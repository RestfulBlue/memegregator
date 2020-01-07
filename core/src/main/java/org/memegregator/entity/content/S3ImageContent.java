package org.memegregator.entity.content;

import org.memegregator.storage.ContentStorage;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class S3ImageContent implements InternalMemeContent {

  private final String hash;
  private final String imageKey;

  public S3ImageContent(String hash, String imageKey) {
    this.imageKey = imageKey;
    this.hash = hash;
  }

  public String getHash() {
    return hash;
  }

  public String getImageKey() {
    return imageKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    S3ImageContent that = (S3ImageContent) o;
    return Objects.equals(hash, that.hash) &&
        Objects.equals(imageKey, that.imageKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash, imageKey);
  }

  @Override
  public Mono<Void> dropFromStorage(ContentStorage storage) {
    return storage.dropData(imageKey);
  }
}
