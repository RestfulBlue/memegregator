package org.memegregator.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.*;
import org.memegregator.puller.HttpPuller;
import org.memegregator.storage.ContentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class S3Collector implements Collector {

    private final ContentStorage contentStorage;
    private final HttpPuller puller;

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Collector.class);

    private final AtomicLong inProgressGauge;
    private final Counter memesReceived;
    private final Counter memesProcessed;



    @Autowired
    public S3Collector(HttpPuller puller, ContentStorage contentStorage, MeterRegistry registry) {
        this.contentStorage = contentStorage;
        this.puller = puller;

        this.inProgressGauge = registry.gauge("S3Collector", Tags.of("type","current"),new AtomicLong());
        this.memesReceived = registry.counter("S3Collector", Tags.of("type", "received"));
        this.memesProcessed = registry.counter("S3Collector", Tags.of("type", "finished"));
    }

    @Override
    public Flux<MemeInfo> collectMemes(Flux<MemeInfo> memesStream) {
        return memesStream.flatMap(meme -> {

            memesReceived.increment();
            inProgressGauge.incrementAndGet();

            MemeContent content = meme.getContent();
            if (content instanceof InternalMemeContent) {
                return Mono.just(meme);
            }

            if (content instanceof ExternalImageContent) {
                ExternalImageContent imageContent = (ExternalImageContent) content;

                Flux<byte[]> buffer = streamFileByUrl(imageContent.getImageUrl());

                String s3Key = String.format("image/%s/%s.%s", meme.getProvider(), meme.getId(), getExtension(imageContent.getImageUrl()));

                return contentStorage
                        .pushData(s3Key, buffer)
                        .then(Mono.fromCallable(() -> {
                            memesProcessed.increment();
                            inProgressGauge.decrementAndGet();
                            S3ImageContent s3ImageContent = new S3ImageContent(s3Key);
                            return new MemeInfo(meme.getId(), meme.getProvider(), meme.getTitle(), s3ImageContent, meme.getRating());
                        }));

            }

            if (content instanceof ExternalVideoContent) {
                ExternalVideoContent videoContent = (ExternalVideoContent) content;

                String posterUrl = videoContent.getPosterUrl();
                String videoUrl = videoContent.getVideoUrl();

                Flux<byte[]> posterBuffer = streamFileByUrl(posterUrl);
                Flux<byte[]> videoBuffer = streamFileByUrl(videoUrl);

                String posterKey = String.format("poster/%s/%s.%s", meme.getProvider(), meme.getId(), getExtension(posterUrl));
                String videoKey = String.format("video/%s/%s.%s", meme.getProvider(), meme.getId(), getExtension(videoUrl));

                Mono<Void> posterUpload = contentStorage.pushData(posterKey, posterBuffer);
                Mono<Void> videoUpload = contentStorage.pushData(videoKey, videoBuffer);


                return Mono
                        .when(posterUpload, videoUpload)
                        .then(Mono.fromCallable(() -> {
                            memesProcessed.increment();
                            inProgressGauge.decrementAndGet();
                            S3VideoContent s3VideoContent = new S3VideoContent(videoKey, posterKey);
                            return new MemeInfo(meme.getId(), meme.getProvider(), meme.getTitle(), s3VideoContent, meme.getRating());
                        }));
            }

            LOGGER.warn("Received unknown content in meme, do nothing");
            return Mono.empty();

        });
    }

    private String getExtension(String url){
        String[] parts = url.split("\\.");
        return parts[parts.length-1];
    }

    private Flux<byte[]> streamFileByUrl(String url) {
        return puller.pullAsFlux(url, byte[].class);
    }
}
