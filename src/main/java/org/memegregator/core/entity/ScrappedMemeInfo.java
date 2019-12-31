package org.memegregator.core.entity;

import java.util.Objects;

public class ScrappedMemeInfo {

    private final String id;
    private final String picId;

    public ScrappedMemeInfo(String id, String picId) {
        this.id = id;
        this.picId = picId;
    }

    public String getId() {
        return id;
    }

    public String getPicId() {
        return picId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScrappedMemeInfo that = (ScrappedMemeInfo) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(picId, that.picId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, picId);
    }
}
