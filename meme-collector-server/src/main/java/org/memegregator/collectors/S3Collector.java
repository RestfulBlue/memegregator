package org.memegregator.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.atomic.AtomicInteger;
import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.ExternalImageContent;
import org.memegregator.entity.content.ExternalVideoContent;
import org.memegregator.entity.content.InternalMemeContent;
import org.memegregator.entity.content.MemeContent;
import org.memegregator.entity.content.S3ImageContent;
import org.memegregator.entity.content.S3VideoContent;
import org.memegregator.puller.WebClientPuller;
import org.memegregator.storage.ContentStorage;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class S3Collector implements Collector {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3Collector.class);
  private final ContentStorage contentStorage;
  private final WebClientPuller puller;

  private static final String METRIC_NAME = "S3Collector";

  private final AtomicInteger inProgressGauge;
  private final Counter receivedCounter;
  private final Counter processedCounter;
  private final Counter errorsCounter;

  @Autowired
  public S3Collector(ContentStorage contentStorage, MeterRegistry registry) {
    this.contentStorage = contentStorage;
    this.puller = new WebClientPuller();

    this.inProgressGauge = registry
        .gauge(METRIC_NAME, Tags.of("type", "current"), new AtomicInteger());
    this.receivedCounter = registry.counter(METRIC_NAME, Tags.of("type", "received"));
    this.processedCounter = registry.counter(METRIC_NAME, Tags.of("type", "processed"));
    this.errorsCounter = registry.counter(METRIC_NAME, Tags.of("type", "processedWithErrors"));

  }

  @Override
  public Flux<MemeInfo> collectMemes(Flux<MemeInfo> memesStream) {

    return Flux.create(sink -> {
      memesStream.subscribe(new BaseSubscriber<MemeInfo>() {

        Subscription subscription;

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
          this.subscription = subscription;
          this.subscription.request(256);
        }

        @Override
        protected void hookOnNext(MemeInfo value) {
          processMeme(value)
              .handle((memeInfo, synchronousSink) -> {
                sink.next(memeInfo);
              })
              .doFinally((signalType) -> {
                subscription.request(1);
              })
              .subscribe();

        }

        @Override
        protected void hookOnCancel() {

        }
      });
    });

  }

  private Mono<MemeInfo> processMeme(MemeInfo meme) {
    Mono<MemeInfo> result = Mono.empty();
    inProgressGauge.incrementAndGet();
    receivedCounter.increment();

    MemeContent content = meme.getContent();
    if (content instanceof InternalMemeContent) {
      return Mono.just(meme);
    }

    if (content instanceof ExternalImageContent) {
      ExternalImageContent imageContent = (ExternalImageContent) content;

      Mono<ClientResponse> buffer = streamFileByUrl(imageContent.getImageUrl());

      String s3Key = String.format("image/%s/%s.%s", meme.getProvider(), meme.getId(),
          getExtension(imageContent.getImageUrl()));

      result = contentStorage
          .pushData(s3Key, buffer)
          .then(Mono.fromCallable(() -> {
            S3ImageContent s3ImageContent = new S3ImageContent(s3Key);
            return new MemeInfo(meme.getId(), meme.getProvider(), meme.getTitle(), s3ImageContent,
                meme.getRating());
          }));
    }

    if (content instanceof ExternalVideoContent) {
      ExternalVideoContent videoContent = (ExternalVideoContent) content;

      String posterUrl = videoContent.getPosterUrl();
      String videoUrl = videoContent.getVideoUrl();

      Mono<ClientResponse> posterBuffer = streamFileByUrl(posterUrl);
      Mono<ClientResponse> videoBuffer = streamFileByUrl(videoUrl);

      String posterKey = String
          .format("poster/%s/%s.%s", meme.getProvider(), meme.getId(), getExtension(posterUrl));
      String videoKey = String
          .format("video/%s/%s.%s", meme.getProvider(), meme.getId(), getExtension(videoUrl));

      Mono<Void> posterUpload = contentStorage.pushData(posterKey, posterBuffer);
      Mono<Void> videoUpload = contentStorage.pushData(videoKey, videoBuffer);

      result = Mono
          .when(posterUpload, videoUpload)
          .then(Mono.fromCallable(() -> {
            S3VideoContent s3VideoContent = new S3VideoContent(videoKey, posterKey);
            return new MemeInfo(meme.getId(), meme.getProvider(), meme.getTitle(), s3VideoContent,
                meme.getRating());
          }));
    }

    return result
        .map(info -> {
          inProgressGauge.decrementAndGet();
          processedCounter.increment();
          return info;
        })
        .onErrorResume(e -> {
          LOGGER.error("Error while trying to upload meme : {}", e);
          inProgressGauge.decrementAndGet();
          errorsCounter.increment();
          return Mono.empty();
        });

  }

  private String getExtension(String url) {
    String[] parts = url.split("\\.");
    return parts[parts.length - 1];
  }

  private Mono<ClientResponse> streamFileByUrl(String url) {
    return puller.pullRaw(url);
  }
}
