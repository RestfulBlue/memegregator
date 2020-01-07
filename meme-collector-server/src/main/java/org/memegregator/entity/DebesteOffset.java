package org.memegregator.entity;

import java.util.Objects;

public class DebesteOffset implements ScrappingOffset {

  private int checkpointedPage;
  private int checkpointOffset;
  private int committedOffset;
  private boolean committed;

  public DebesteOffset() {
  }

  public DebesteOffset(int checkpointedPage, int checkpointOffset, int committedOffset,
      boolean committed) {
    this.checkpointedPage = checkpointedPage;
    this.checkpointOffset = checkpointOffset;
    this.committedOffset = committedOffset;
    this.committed = committed;
  }

  public int getCheckpointedPage() {
    return checkpointedPage;
  }

  public void setCheckpointedPage(int checkpointedPage) {
    this.checkpointedPage = checkpointedPage;
  }

  public int getCheckpointOffset() {
    return checkpointOffset;
  }

  public void setCheckpointOffset(int checkpointOffset) {
    this.checkpointOffset = checkpointOffset;
  }

  public int getCommittedOffset() {
    return committedOffset;
  }

  public void setCommittedOffset(int committedOffset) {
    this.committedOffset = committedOffset;
  }

  public boolean isCommitted() {
    return committed;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DebesteOffset offset = (DebesteOffset) o;
    return checkpointedPage == offset.checkpointedPage &&
        checkpointOffset == offset.checkpointOffset &&
        committedOffset == offset.committedOffset &&
        committed == offset.committed;
  }

  @Override
  public int hashCode() {
    return Objects.hash(checkpointedPage, checkpointOffset, committedOffset, committed);
  }
}
