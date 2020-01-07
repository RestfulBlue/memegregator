package org.memegregator.entity.content;

import org.memegregator.entity.MemeInfo;
import org.memegregator.storage.ContentStorage;
import org.memegregator.util.MemegregatorUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class ExternalVideoContent implements ExternalMemeContent {

  private final String videoUrl;
  private final String posterUrl;

  public ExternalVideoContent(String videoUrl, String posterUrl) {
    this.videoUrl = videoUrl;
    this.posterUrl = posterUrl;
  }

  public String getVideoUrl() {
    return videoUrl;
  }

  public String getPosterUrl() {
    return posterUrl;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalVideoContent that = (ExternalVideoContent) o;
    return Objects.equals(videoUrl, that.videoUrl) &&
        Objects.equals(posterUrl, that.posterUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(videoUrl, posterUrl);
  }

  @Override
  public Mono<InternalMemeContent> convertToInternal(Function<String, Mono<ClientResponse>> fileStreamFunction, ContentStorage contentStorage) {


    Mono<ClientResponse> posterBuffer = fileStreamFunction.apply(posterUrl);
    Mono<ClientResponse> videoBuffer = fileStreamFunction.apply(videoUrl);

    String uid = MemegregatorUtils.getUid();

    String posterKey = String.format("%s.%s", uid, MemegregatorUtils.getExtension(posterUrl));
    String videoKey = String.format("%s.%s", uid, MemegregatorUtils.getExtension(videoUrl));

    Mono<String> posterUpload = contentStorage.pushData(posterKey, posterBuffer);
    Mono<String> videoUpload = contentStorage.pushData(videoKey, videoBuffer);

    return Mono
            .zip(posterUpload, videoUpload)
            .map(tuple -> {
              S3VideoContent s3VideoContent = new S3VideoContent(tuple.getT2(), videoKey, posterKey);
              return s3VideoContent;
            });
  }
}
