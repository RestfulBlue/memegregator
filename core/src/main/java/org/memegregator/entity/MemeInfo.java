package org.memegregator.entity;

import org.memegregator.entity.content.MemeContent;

import java.util.Objects;

public class MemeInfo {

    private final int id;
    private final String title;
    private final MemeContent content;
    private final int rating;

    public MemeInfo(int id, String title, MemeContent content, int rating) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.rating = rating;
    }

    public int getId() {
        return id;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemeInfo that = (MemeInfo) o;
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
