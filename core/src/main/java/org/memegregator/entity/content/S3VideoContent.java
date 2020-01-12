package org.memegregator.entity.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.memegregator.storage.ContentStorage;
import reactor.core.publisher.Mono;

public class S3VideoContent implements InternalMemeContent {

  private final String hash;
  private final String fileKey;
  private final String posterKey;

  @JsonCreator
  public S3VideoContent(@JsonProperty("hash") String hash, @JsonProperty("fileKey") String fileKey,
      @JsonProperty("posterKey") String posterKey) {
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

  @Override
  public Mono<ApiMemeContent> convertToApiContent() {
    return Mono.just(new ApiVideoContent("/image/" + posterKey, "/video/" + fileKey));
  }
}
