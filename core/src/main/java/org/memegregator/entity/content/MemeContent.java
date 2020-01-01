package org.memegregator.entity.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExternalImageContent.class, name = "extImage"),
        @JsonSubTypes.Type(value = ExternalVideoContent.class, name = "extVideo")
})
public interface MemeContent {
}
