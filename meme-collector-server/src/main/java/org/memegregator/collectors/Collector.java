package org.memegregator.collectors;

import org.memegregator.entity.info.MemeInfo;
import reactor.core.publisher.Flux;


public interface Collector {

  Flux<MemeInfo> collectMemes(Flux<MemeInfo> memesStream);

}
