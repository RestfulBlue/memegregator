package org.memegregator.entity.info;

import java.util.Objects;
import org.memegregator.entity.content.MemeContent;

public class MemeInfo {

  private final String title;
  private final MemeContent content;
  private final int rating;

  public MemeInfo(String title, MemeContent content, int rating) {
    this.title = title;
    this.content = content;
    this.rating = rating;
  }

  public String getTitle() {
    return title;
  }

  public MemeContent getContent() {
    return content;
  }

  public int getRating() {
    return rating;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MemeInfo memeInfo = (MemeInfo) o;
    return rating == memeInfo.rating &&
        Objects.equals(title, memeInfo.title) &&
        Objects.equals(content, memeInfo.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, content, rating);
  }
}
