package org.memegregator.entity;

import java.util.Objects;

public class HttpRange {

  private Long from;
  private Long to;

  public HttpRange(Long from, Long to) {
    this.from = from;
    this.to = to;
  }

  public void setFrom(Long from) {
    this.from = from;
  }

  public void setTo(Long to) {
    this.to = to;
  }

  public Long getFrom() {
    return from;
  }

  public Long getTo() {
    return to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HttpRange httpRange = (HttpRange) o;
    return Objects.equals(from, httpRange.from) &&
        Objects.equals(to, httpRange.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to);
  }
}
