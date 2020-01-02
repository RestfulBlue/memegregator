package org.memegregator.coordinator.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "offsets")
public class Offset {

    @Id
    private String id;
    private String service;
    private int offset;

    public Offset() {
    }

    public Offset(String service, int offset) {
        this.service = service;
        this.offset = offset;
    }

    public Offset(String id, String service, int offset) {
        this.id = id;
        this.service = service;
        this.offset = offset;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Offset offset1 = (Offset) o;
        return offset == offset1.offset &&
                Objects.equals(id, offset1.id) &&
                Objects.equals(service, offset1.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, service, offset);
    }
}
