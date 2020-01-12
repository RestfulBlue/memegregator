package org.memegregator.entity.content;

import java.util.Objects;

public class ApiImageContent implements ApiMemeContent{

  private final String imageUrl;

  public ApiImageContent(String imageUrl) {
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
    ApiImageContent that = (ApiImageContent) o;
    return Objects.equals(imageUrl, that.imageUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imageUrl);
  }
}
