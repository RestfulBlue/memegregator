package org.memegregator.scrappers;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.memegregator.entity.info.MemeInfo;
import org.memegregator.scrappers.debeste.DebesteScrapper;
import reactor.core.publisher.Flux;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DebesteScrapper.class, name = "debeste"),
})
public interface Scrapper {

  Flux<MemeInfo> getScrappingStream();
}
