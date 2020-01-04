package org.memegregator.entity;

import org.memegregator.entity.content.MemeContent;

import java.util.Objects;

public class MemeInfo {

    private final int id;
    private final String provider;
    private final String title;
    private final MemeContent content;
    private final int rating;

    public MemeInfo(int id, String provider, String title, MemeContent content, int rating) {
        this.id = id;
        this.provider = provider;
        this.title = title;
        this.content = content;
        this.rating = rating;
    }

    public int getId() {
        return id;
    }

    public String getProvider() {
        return provider;
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
        MemeInfo memeInfo = (MemeInfo) o;
        return id == memeInfo.id &&
                rating == memeInfo.rating &&
                Objects.equals(provider, memeInfo.provider) &&
                Objects.equals(title, memeInfo.title) &&
                Objects.equals(content, memeInfo.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, provider, title, content, rating);
    }
}
