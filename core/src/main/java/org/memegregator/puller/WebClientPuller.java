package org.memegregator.puller;

import java.net.URI;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebClientPuller implements HttpPuller {

  private final WebClient webClient;

  public WebClientPuller() {
    this.webClient = WebClient.builder().build();
  }

  @Override
  public Mono<String> pullAsString(String url) {
    return webClient.get().uri(URI.create(url)).exchange().flatMap(clientResponse -> {
      return clientResponse.bodyToMono(String.class);
    });
  }

  @Override
  public Flux<byte[]> pullAsFlux(String url) {
    return webClient.get().uri(URI.create(url)).exchange().flatMapMany(clientResponse -> {
      return clientResponse.bodyToFlux(byte[].class);
    });
  }

  @Override
  public Mono<ClientResponse> pullRaw(String url) {
    return webClient.get().uri(URI.create(url)).exchange();
  }
}
