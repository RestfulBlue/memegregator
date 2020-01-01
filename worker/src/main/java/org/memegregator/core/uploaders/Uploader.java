package org.memegregator.core.uploaders;

import org.springframework.web.reactive.function.client.WebClient;

public class Uploader {

    private final WebClient webClient;

    public Uploader() {
        webClient = WebClient.builder()
                .baseUrl("http://debeste.de")
                .build();
    }

}
