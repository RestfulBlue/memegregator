package org.memegregator.core.extractors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.ExternalImageContent;
import org.memegregator.entity.content.ExternalVideoContent;
import org.memegregator.entity.content.MemeContent;

import java.util.ArrayList;
import java.util.List;

public class DebastoExtractor {

    public static List<MemeInfo> extractMemesFromHtml(String html) {

        Document doc = Jsoup.parse(html);
        Elements boxs = doc.getElementById("content").getElementsByClass("box");

        List<MemeInfo> infos = new ArrayList<>();

        for (Element element : boxs) {

            String id = null;
            String title = null;
            MemeContent content = null;
            Integer rating = null;


            Element headerLink = element.getElementsByTag("h2").get(0).children().first();
            String text = headerLink.text();
            if (text == null || text.isEmpty() || text.equals("Warum ?")) {
                continue;
            }

            title = text;
            String href = headerLink.attr("href");
            String[] parts = href.split("/");
            id = href.charAt(href.length()-1) == '/' ? parts[parts.length - 1] : parts[parts.length - 2];

            Element wrapper = element.getElementsByClass("objectWrapper").first();

            if (wrapper == null) {
                continue;
            }


            Element imageBox = wrapper.getElementsByClass("box-img").first();
            Element videoBox = wrapper.getElementsByClass("box-video").first();

            if (imageBox != null) {
                String url = imageBox.getElementsByTag("a").first()
                        .getElementsByTag("img").first()
                        .attr("src");
                content = new ExternalImageContent(url);
            } else if (videoBox != null) {
                Element videoElement = videoBox.children().first().getElementsByTag("video").first();
                String videoUrl = videoElement.attr("src");
                String posterUrl = videoElement.attr("poster");
                content = new ExternalVideoContent(videoUrl, posterUrl);
            }

            Element voteElement = element.getElementsByClass("vote").first();
            if (voteElement != null) {
                try {
                    rating = Integer.parseInt(voteElement.getElementsByClass("rate").text());
                } catch (Exception e) {
                    rating = 0;
                }
            }

            infos.add(new MemeInfo(Integer.parseInt(id), title, content, rating));
        }

        return infos;
    }


}
