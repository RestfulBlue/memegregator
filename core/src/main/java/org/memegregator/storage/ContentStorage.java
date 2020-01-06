package org.memegregator.storage;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface ContentStorage {

  Mono<Void> pushData(String key, Mono<ClientResponse> response);

}
