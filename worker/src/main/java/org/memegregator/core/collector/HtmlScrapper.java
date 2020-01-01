package org.memegregator.core.collector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class HtmlScrapper {

    private final String host;
    private final WebClient webClient;

    public HtmlScrapper(@Value("${host:debasto.de}") String host) {
        this.host = host;
        webClient = WebClient.builder()
                .baseUrl("http://debeste.de")
                .build();

    }


    public Mono<String> collectMemes(int page) {
        return webClient.method(HttpMethod.GET)
                .uri("/" + page)
                .exchange()
                .flatMap(response -> response.bodyToMono(String.class));
    }
}
