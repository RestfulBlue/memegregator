package org.memegregator.storage;

import org.memegregator.puller.StreamWithLength;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface ContentStorage {

  Mono<String> pushData(String key, Mono<ClientResponse> response);

  Mono<StreamWithLength> pullData(String key);

  Mono<StreamWithLength> pullData(String key, String range);

  Mono<Void> dropData(String key);
}
