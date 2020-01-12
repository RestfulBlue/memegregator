package org.memegregator.entity.info;

import java.util.Objects;
import org.memegregator.entity.content.MemeContent;

public class ApiMemeInfo extends MemeInfo {

  private final String id;

  public ApiMemeInfo(String id, String title, MemeContent content, int rating) {
    super(title, content, rating);
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ApiMemeInfo that = (ApiMemeInfo) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), id);
  }
}
