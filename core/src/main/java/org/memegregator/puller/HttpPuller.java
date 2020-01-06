package org.memegregator.puller;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HttpPuller {

  Mono<String> pullAsString(String url);

  Flux<byte[]> pullAsFlux(String url);

  Mono<ClientResponse> pullRaw(String url);

}
