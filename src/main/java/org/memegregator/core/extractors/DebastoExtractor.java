package org.memegregator.core.extractors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.memegregator.core.entity.ImageContent;
import org.memegregator.core.entity.ScrappedContent;
import org.memegregator.core.entity.ScrappedMemeInfo;
import org.memegregator.core.entity.VideoContent;

import java.util.ArrayList;
import java.util.List;

public class DebastoExtractor {

    public List<ScrappedMemeInfo> extractMemesFromHtml(String html){

        Document doc = Jsoup.parse(html);
        Elements boxs = doc.getElementById("content").getElementsByClass("box");

        List<ScrappedMemeInfo> infos = new ArrayList<>();

        for (Element element : boxs) {

            String id = null;
            String title = null;
            ScrappedContent content = null;
            Integer rating = null;

            for (Element headerLink : element.getElementsByTag("h2").get(0).children()) {
                String text = headerLink.text();
                if (text == null || text.isEmpty()) {
                    continue;
                }

                title = text;
                String[] parts = headerLink.attr("href").split("/");
                id = parts[parts.length-2];
            }

            Element wrapper = element.getElementsByClass("objectWrapper").first();

            if(wrapper == null){
                continue;
            }


            Element imageBox = wrapper.getElementsByClass("box-image").first();
            Element videoBox = wrapper.getElementsByClass("box-video").first();

            if(imageBox != null){
                String url = imageBox.getElementsByTag("a").first()
                        .getElementsByTag("img").first()
                        .attr("src");
                content = new ImageContent(url);
            }else if(videoBox != null){
                Element videoElement = videoBox.children().first().getElementsByTag("video").first();
                String videoUrl = videoElement.attr("src");
                String posterUrl = videoElement.attr("poster");
                content = new VideoContent(videoUrl, posterUrl);
            }

            Element voteElement = element.getElementsByClass("vote").first();
            if(voteElement != null){
                try {
                    rating = Integer.parseInt(voteElement.getElementsByClass("rate").text());
                }catch (Exception e){
                    rating = 0;
                }
            }

            infos.add(new ScrappedMemeInfo(id, title, content, rating));
        }

        return infos;
    }



}
