package org.memegregator.entity.content;

import java.util.Objects;

public class ApiVideoContent implements ApiMemeContent {

  private final String posterUrl;
  private final String videoUrl;

  public ApiVideoContent(String posterUrl, String videoUrl) {
    this.posterUrl = posterUrl;
    this.videoUrl = videoUrl;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public String getVideoUrl() {
    return videoUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiVideoContent that = (ApiVideoContent) o;
    return Objects.equals(posterUrl, that.posterUrl) &&
        Objects.equals(videoUrl, that.videoUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(posterUrl, videoUrl);
  }
}
