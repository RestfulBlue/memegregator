package org.memegregator.entity.content;

import java.util.Objects;

public class S3VideoContent implements MemeContent {

    private final String fileKey;
    private final String posterKey;

    public S3VideoContent(String fileKey, String posterKey) {
        this.fileKey = fileKey;
        this.posterKey = posterKey;
    }

    public String getFileKey() {
        return fileKey;
    }

    public String getPosterKey() {
        return posterKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S3VideoContent that = (S3VideoContent) o;
        return Objects.equals(fileKey, that.fileKey) &&
                Objects.equals(posterKey, that.posterKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileKey, posterKey);
    }
}
