package org.memegregator.core.collector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.memegregator.core.entity.ScrappedMemeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
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

        String data = client.method(HttpMethod.GET).exchange().block().bodyToMono(String.class).block();

        Document doc = Jsoup.parse(data);

        Elements boxs = doc.getElementById("content")
                .getElementsByClass("box");

        String title = null;
        String link = null;
        String src = null;
        Integer rating = null;


        for (Element element : boxs) {

            for (Element headerLink : element.getElementsByTag("h2").get(0).children()) {
                String text = headerLink.text();
                if (text == null || text.isEmpty()) {
                    continue;
                }

                title = text;
                link = headerLink.attr("href");
            }

            Element image = element
                    .getElementsByClass("objectMapper").first()
                    .getElementsByClass("box-img").first()
                    .getElementsByTag("a").first()
                    .getElementsByTag("img").first();

            src = image.attr("src");




        }
        System.out.println(321);
        return Collections.emptyList();
    }
}
