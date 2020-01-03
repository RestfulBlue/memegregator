package org.memegregator.collectors;

import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.*;
import org.memegregator.push.file.ContentPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class S3Collector implements Collector {

    private final ContentPusher contentPusher;
    private final WebClient webClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Collector.class);

    @Autowired
    public S3Collector(ContentPusher contentPusher) {
        this.contentPusher = contentPusher;
        this.webClient = WebClient.builder()
                .baseUrl("http://debeste.de")
                .build();

    }

    @Override
    public Flux<MemeInfo> collectMemes(Flux<MemeInfo> memesStream) {
        return memesStream.flatMap(meme -> {

            MemeContent content = meme.getContent();
            if (content instanceof InternalMemeContent) {
                return Mono.just(meme);
            }

            if (content instanceof ExternalImageContent) {
                ExternalImageContent imageContent = (ExternalImageContent) content;

                Flux<byte[]> buffer = streamFileByUrl(imageContent.getImageUrl());

                return contentPusher
                        .pushData(imageContent.getImageUrl(), buffer)
                        .then(Mono.fromCallable(() -> {
                            S3ImageContent s3ImageContent = new S3ImageContent(imageContent.getImageUrl());
                            return new MemeInfo(meme.getId(), meme.getTitle(), s3ImageContent, meme.getRating());
                        }));

            }

//            if (content instanceof ExternalVideoContent) {
//                ExternalVideoContent videoContent = (ExternalVideoContent) content;
//
//                String posterUrl = videoContent.getPosterUrl();
//                String videoUrl = videoContent.getVideoUrl();
//
//                Flux<byte[]> posterBuffer = streamFileByUrl(posterUrl);
//                Flux<byte[]> videoBuffer = streamFileByUrl(videoUrl);
//
//                Mono<Void> posterUpload = contentPusher.pushData(posterUrl, posterBuffer);
//                Mono<Void> videoUpload = contentPusher.pushData(videoUrl, videoBuffer);
//
//                return Mono
//                        .when(posterUpload, videoUpload)
//                        .then(Mono.fromCallable(() -> {
//                            S3VideoContent s3VideoContent = new S3VideoContent(videoUrl, posterUrl);
//                            return new MemeInfo(meme.getId(), meme.getTitle(), s3VideoContent, meme.getRating());
//                        }));
//            }


            LOGGER.warn("Received unknown content in meme, do nothing");
            return Mono.empty();

        });
    }


    private Flux<byte[]> streamFileByUrl(String url) {
        return webClient.get()
                .uri(url)
                .exchange()
                .flatMapMany(response -> response.bodyToFlux(byte[].class));
    }
}
