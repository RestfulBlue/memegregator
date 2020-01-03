package org.memegregator.entity.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExternalImageContent.class, name = "extImage"),
        @JsonSubTypes.Type(value = ExternalVideoContent.class, name = "extVideo"),
        @JsonSubTypes.Type(value = S3ImageContent.class, name = "s3Image"),
        @JsonSubTypes.Type(value = S3VideoContent.class, name = "s3Video")
})
public interface MemeContent {
}
