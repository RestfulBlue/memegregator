package org.memegregator.core.entity;

import java.util.Objects;

public class ScrappedMemeInfo {

    private final String id;
    private final String title;
    private final ScrappedContent content;
    private final int rating;

    public ScrappedMemeInfo(String id, String title, ScrappedContent content, int rating) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ScrappedContent getContent() {
        return content;
    }

    public int getRating() {
        return rating;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScrappedMemeInfo that = (ScrappedMemeInfo) o;
        return rating == that.rating &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, rating);
    }
}
