package org.memegregator.storage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface ContentStorage {

  Mono<Void> pushData(String key, Flux<byte[]> stream);

}
