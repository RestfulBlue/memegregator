package org.memegregator.puller;

import java.util.Objects;
import reactor.core.publisher.Flux;

public class StreamWithLength {
  private final long length;
  private final String range;
  private final Flux<byte[]> stream;

  public StreamWithLength(long length, String range, Flux<byte[]> stream) {
    this.length = length;
    this.range = range;
    this.stream = stream;
  }

  public StreamWithLength(long length, Flux<byte[]> stream) {
    this.length = length;
    this.stream = stream;
    this.range = null;
  }

  public long getLength() {
    return length;
  }

  public String getRange() {
    return range;
  }

  public Flux<byte[]> getStream() {
    return stream;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StreamWithLength that = (StreamWithLength) o;
    return length == that.length &&
        Objects.equals(range, that.range) &&
        Objects.equals(stream, that.stream);
  }

  @Override
  public int hashCode() {
    return Objects.hash(length, range, stream);
  }
}
