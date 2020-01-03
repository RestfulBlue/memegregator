package org.memegregator.collectors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.ExternalImageContent;
import org.memegregator.entity.content.ExternalVideoContent;
import reactor.core.publisher.Flux;


public interface Collector {

    Flux<MemeInfo> collectMemes(Flux<MemeInfo> memesStream);

}
