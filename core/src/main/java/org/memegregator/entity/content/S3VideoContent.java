package org.memegregator.entity.content;

import org.memegregator.storage.ContentStorage;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class S3VideoContent implements InternalMemeContent {

  private final String hash;
  private final String fileKey;
  private final String posterKey;

  public S3VideoContent(String hash, String fileKey, String posterKey) {
    this.hash = hash;
    this.fileKey = fileKey;
    this.posterKey = posterKey;
  }

  public String getHash() {
    return hash;
  }

  public String getFileKey() {
    return fileKey;
  }

  public String getPosterKey() {
    return posterKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    S3VideoContent that = (S3VideoContent) o;
    return Objects.equals(hash, that.hash) &&
        Objects.equals(fileKey, that.fileKey) &&
        Objects.equals(posterKey, that.posterKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash, fileKey, posterKey);
  }

  @Override
  public Mono<Void> dropFromStorage(ContentStorage contentStorage) {
    return Mono.when(
            contentStorage.dropData(fileKey),
            contentStorage.dropData(posterKey)
    );
  }
}
