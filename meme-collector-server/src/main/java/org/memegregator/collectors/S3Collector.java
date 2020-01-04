package org.memegregator.collectors;

import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.ExternalImageContent;
import org.memegregator.entity.content.ExternalVideoContent;
import org.memegregator.entity.content.InternalMemeContent;
import org.memegregator.entity.content.MemeContent;
import org.memegregator.entity.content.S3ImageContent;
import org.memegregator.entity.content.S3VideoContent;
import org.memegregator.puller.HttpPuller;
import org.memegregator.storage.ContentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class S3Collector implements Collector {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3Collector.class);
  private final ContentStorage contentStorage;
  private final HttpPuller puller;

  @Autowired
  public S3Collector(HttpPuller puller, ContentStorage contentStorage) {
    this.contentStorage = contentStorage;
    this.puller = puller;
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

        String s3Key = String.format("image/%s/%s.%s", meme.getProvider(), meme.getId(),
            getExtension(imageContent.getImageUrl()));

        return contentStorage
            .pushData(s3Key, buffer)
            .then(Mono.fromCallable(() -> {
              S3ImageContent s3ImageContent = new S3ImageContent(s3Key);
              return new MemeInfo(meme.getId(), meme.getProvider(), meme.getTitle(), s3ImageContent,
                  meme.getRating());
            }))
            .retry(1)
            .onErrorResume(e -> Mono.empty());
      }

      if (content instanceof ExternalVideoContent) {
        ExternalVideoContent videoContent = (ExternalVideoContent) content;

        String posterUrl = videoContent.getPosterUrl();
        String videoUrl = videoContent.getVideoUrl();

        Flux<byte[]> posterBuffer = streamFileByUrl(posterUrl);
        Flux<byte[]> videoBuffer = streamFileByUrl(videoUrl);

        String posterKey = String
            .format("poster/%s/%s.%s", meme.getProvider(), meme.getId(), getExtension(posterUrl));
        String videoKey = String
            .format("video/%s/%s.%s", meme.getProvider(), meme.getId(), getExtension(videoUrl));

        Mono<Void> posterUpload = contentStorage.pushData(posterKey, posterBuffer);
        Mono<Void> videoUpload = contentStorage.pushData(videoKey, videoBuffer);

        return Mono
            .when(posterUpload, videoUpload)
            .then(Mono.fromCallable(() -> {
              S3VideoContent s3VideoContent = new S3VideoContent(videoKey, posterKey);
              return new MemeInfo(meme.getId(), meme.getProvider(), meme.getTitle(), s3VideoContent,
                  meme.getRating());
            }))
            .onErrorResume(e -> {
              System.out.println("123");
              return Mono.empty();
            });
      }

      LOGGER.warn("Received unknown content in meme, do nothing");
      return Mono.empty();

    });
  }

  private String getExtension(String url) {
    String[] parts = url.split("\\.");
    return parts[parts.length - 1];
  }

  private Flux<byte[]> streamFileByUrl(String url) {
    return puller.pullAsFlux(url);
  }
}
