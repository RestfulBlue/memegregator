package org.memegregator.core.collector;

import org.memegregator.core.entity.ScrappedMemeInfo;
import org.memegregator.core.extractors.DebastoExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class HtmlScrapper {

    private final String host;
    private final int endOffset;

    public HtmlScrapper(@Value("${host:debasto.de}") String host, @Value("${endOffset:0}") int endOffset) {
        this.host = host;
        this.endOffset = endOffset;
    }


    public List<ScrappedMemeInfo> collectMemes() {

        WebClient client = WebClient.builder()
                .baseUrl("http://debeste.de")
                .build();

        String data = client.method(HttpMethod.GET)
                .exchange()
                .block()
                .bodyToMono(String.class)
                .block();

        return new DebastoExtractor().extractMemesFromHtml(data);
    }
}
