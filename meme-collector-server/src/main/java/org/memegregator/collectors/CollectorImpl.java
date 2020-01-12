package org.memegregator.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.atomic.AtomicInteger;
import org.memegregator.entity.info.MemeInfo;
import org.memegregator.entity.content.ExternalMemeContent;
import org.memegregator.entity.content.InternalMemeContent;
import org.memegregator.entity.content.MemeContent;
import org.memegregator.puller.WebClientPuller;
import org.memegregator.storage.ContentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CollectorImpl implements Collector {

  private static final Logger LOGGER = LoggerFactory.getLogger(CollectorImpl.class);
  private final ContentStorage contentStorage;
  private final WebClientPuller puller;

  private static final String METRIC_NAME = "CollectorImpl";

  private final AtomicInteger inProgressGauge;
  private final Counter receivedCounter;
  private final Counter processedCounter;
  private final Counter errorsCounter;

  @Autowired
  public CollectorImpl(ContentStorage contentStorage, MeterRegistry registry) {
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
    return memesStream.flatMap(meme -> {
      inProgressGauge.incrementAndGet();
      receivedCounter.increment();

      MemeContent content = meme.getContent();
      if (content instanceof InternalMemeContent) {
        return Mono.just(meme);
      }

      if (!(content instanceof ExternalMemeContent)) {
        return Mono.empty();
      }

      ExternalMemeContent externalContent = (ExternalMemeContent) content;

      return externalContent
          .convertToInternal(puller::pullRaw, contentStorage)
          .map(internalContent -> {
            inProgressGauge.decrementAndGet();
            processedCounter.increment();
            return new MemeInfo(meme.getTitle(), internalContent, meme.getRating());
          })
          .onErrorResume(error -> {
            return Mono.empty();
          });
    });
  }

}
