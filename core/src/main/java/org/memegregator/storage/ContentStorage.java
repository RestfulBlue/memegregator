package org.memegregator.storage;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface ContentStorage {

  Mono<String> pushData(String key, Mono<ClientResponse> response);

  Mono<Void> dropData(String key);
}
