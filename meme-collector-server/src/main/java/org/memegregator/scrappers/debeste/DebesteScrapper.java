package org.memegregator.scrappers.debeste;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.ExternalImageContent;
import org.memegregator.entity.content.ExternalVideoContent;
import org.memegregator.entity.content.MemeContent;
import org.memegregator.puller.HttpPuller;
import org.memegregator.scrappers.Scrapper;
import org.memegregator.service.OffsetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Import(OffsetService.class)
public class DebesteScrapper implements Scrapper {

    private static final String PROVIDER_KEY = "DEBESTE";
    private final HttpPuller httpPuller;

    private final OffsetService offsetService;
    private final String host;

    @Autowired
    public DebesteScrapper(OffsetService offsetService,
                           HttpPuller httpPuller,
                           @Value("${host:http://debeste.de}") String host) {
        this.host = host;
        this.offsetService = offsetService;
        this.httpPuller = httpPuller;
    }


    public class DebestoScrapperContext implements Runnable {
        private final FluxSink<MemeInfo> sink;
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        private final ReentrantLock reentrantLock = new ReentrantLock();


        private volatile int currentPage = 1;
        private volatile long memesToCollect = 0;

        private volatile boolean offsetScheduled = false;
        private volatile boolean collectInProgress = false;

        private volatile int endOfsset = Integer.MIN_VALUE;

        public DebestoScrapperContext(FluxSink<MemeInfo> sink) {
            this.sink = sink;
            scheduler.scheduleWithFixedDelay(this, 0, 10, TimeUnit.SECONDS);
        }

        private void runScrapping() {

            reentrantLock.lock();
            if (collectInProgress) {
                return;
            }
            reentrantLock.unlock();

            collectPage(currentPage++)
                    .map(DebesteScrapper.this::extractMemesFromHtml)
                    .subscribe(list -> {
                        int pageMax = Integer.MIN_VALUE;
                        for (MemeInfo info : list) {
                            if (info.getId() > endOfsset) {
                                sink.next(info);
                            }
                            pageMax = Math.max(pageMax, info.getId());
                        }

                        reentrantLock.lock();
                        memesToCollect = memesToCollect > list.size() ? memesToCollect - list.size() : 0;
                        if (memesToCollect == 0) {
                            collectInProgress = false;
                            reentrantLock.unlock();
                            return;
                        }
                        reentrantLock.unlock();


                        if (pageMax > endOfsset) {
                            runScrapping();
                        } else {
                            commitOffsets();
                        }
                    });
        }

        private Mono<Void> commitOffsets() {
//            reentrantLock.lock();
            return offsetService
                    .saveOffset("debeste", endOfsset)
                    .doFinally((signal) -> {
                        offsetScheduled = false;
                        collectInProgress = false;
//                        reentrantLock.unlock();
                    })
                    .then();
        }

        public void processMemeRequest(long n) {
            reentrantLock.lock();
            memesToCollect += n;
            reentrantLock.unlock();
            runScrapping();
        }

        @Override
        public void run() {
            try {
                reentrantLock.lock();
                if (offsetScheduled) {
                    reentrantLock.unlock();
                    return;
                }

                offsetService.findOffset("debeste")
                        .defaultIfEmpty(0)
                        .handle((targetOffset, sink) -> {
                            this.endOfsset = targetOffset;
                            offsetScheduled = true;
                            currentPage = 1;

                            sink.complete();
                        })
                        .block();
            } finally {
                reentrantLock.unlock();
            }

        }
    }

    @Override
    public Flux<MemeInfo> getScrappingStream() {
        return Flux.create(sink -> {
            DebestoScrapperContext scrapper = new DebestoScrapperContext(sink);
            sink.onRequest(scrapper::processMemeRequest);
        });
    }

    private Mono<String> collectPage(int page) {
        return httpPuller.pullAsMono(host + "/" + page, String.class);
    }

    private List<MemeInfo> extractMemesFromHtml(String html) {

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
            id = href.charAt(href.length() - 1) == '/' ? parts[parts.length - 1] : parts[parts.length - 2];

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
                content = new ExternalImageContent(host + "/" + url);
            } else if (videoBox != null) {
                Element videoElement = videoBox.children().first().getElementsByTag("video").first();
                String videoUrl = host + "/" + videoElement.children().first().attr("src");
                String posterUrl = host + "/" + videoElement.attr("poster");
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

            infos.add(new MemeInfo(Integer.parseInt(id), PROVIDER_KEY, title, content, rating));
        }

        return infos;
    }
}
