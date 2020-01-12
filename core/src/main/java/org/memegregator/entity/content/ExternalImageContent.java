package org.memegregator.entity.content;

import org.memegregator.storage.ContentStorage;
import org.memegregator.util.MemegregatorUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class ExternalImageContent implements ExternalMemeContent {

  private final String imageUrl;

  public ExternalImageContent(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalImageContent that = (ExternalImageContent) o;
    return Objects.equals(imageUrl, that.imageUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imageUrl);
  }

  @Override
  public Mono<InternalMemeContent> convertToInternal(
      Function<String, Mono<ClientResponse>> fileStreamFunction, ContentStorage contentStorage) {

    Mono<ClientResponse> stream = fileStreamFunction.apply(imageUrl);
    String uid = UUID.randomUUID().toString().replaceAll("-", "");

    String s3Key = String
        .format("%s.%s", MemegregatorUtils.getUid(), MemegregatorUtils.getExtension(imageUrl));

    return contentStorage
        .pushData(s3Key, stream)
        .map(hash -> {
          S3ImageContent s3ImageContent = new S3ImageContent(hash, s3Key);
          return s3ImageContent;
        });
  }
}
