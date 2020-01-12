package org.memegregator.scrappers.debeste;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.memegregator.entity.info.MemeInfo;
import org.memegregator.entity.content.ExternalImageContent;
import org.memegregator.entity.content.ExternalVideoContent;
import org.memegregator.entity.content.MemeContent;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class DebesteHtmlParser {

  public static Tuple2<Integer, List<MemeInfo>> parseMemesFromPage(String host, String html) {
    Document doc = Jsoup.parse(html);
    Elements boxs = doc.getElementById("content").getElementsByClass("box");

    Integer maxId = 0;
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
      id =
          href.charAt(href.length() - 1) == '/' ? parts[parts.length - 1] : parts[parts.length - 2];

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
        content = new ExternalImageContent(host + url);
      } else if (videoBox != null) {
        Element videoElement = videoBox.children().first().getElementsByTag("video").first();
        String videoUrl = host + videoElement.children().first().attr("src");
        String posterUrl = host + videoElement.attr("poster");
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

      infos.add(new MemeInfo(title, content, rating));
      maxId = Math.max(maxId, Integer.parseInt(id));
    }

    return Tuples.of(maxId, infos);
  }

}
