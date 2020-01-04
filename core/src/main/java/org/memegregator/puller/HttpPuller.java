package org.memegregator.puller;

import com.sun.javafx.webkit.WebConsoleListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class HttpPuller {

    private final WebClient webClient;

    public HttpPuller() {
        this.webClient = WebClient.builder().build();
    }

    public <T> Mono<T> pullAsMono(String url, Class<? extends T> elementClass) {
        return webClient
                .get()
                .uri(URI.create(url))
                .exchange()
                .flatMap(response -> response.bodyToMono(elementClass));
    }

    public <T> Flux<T> pullAsFlux(String url, Class<? extends T> elementClass) {
        return webClient
                .get()
                .uri(URI.create(url))
                .exchange()
                .flatMapMany(response -> response.bodyToFlux(elementClass));
    }


}
