package org.memegregator.entity.content;

import java.util.Objects;

public class ExternalVideoContent implements ExternalMemeContent {
    private final String videoUrl;
    private final String posterUrl;

    public ExternalVideoContent(String videoUrl, String posterUrl) {
        this.videoUrl = videoUrl;
        this.posterUrl = posterUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getPosterUrl() {
        return posterUrl;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalVideoContent that = (ExternalVideoContent) o;
        return Objects.equals(videoUrl, that.videoUrl) &&
                Objects.equals(posterUrl, that.posterUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(videoUrl, posterUrl);
    }

}
