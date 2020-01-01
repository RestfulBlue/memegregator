package org.memegregator.core.api;

import org.memegregator.core.collector.HtmlScrapper;
import org.memegregator.core.extractors.DebastoExtractor;
import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.ExternalImageContent;
import org.memegregator.entity.content.MemeContent;
import org.memegregator.entity.content.S3ImageContent;
import org.memegregator.push.file.ContentPusher;
import org.memegregator.push.meta.MetaPusher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Objects;


@RestController
public class MemeCollectorApi {


    private class PageReaderContext {
        private final int endId;

        private int currPage = 1;
        private int maxId = Integer.MAX_VALUE;

        public PageReaderContext(int endId) {
            this.endId = endId;
        }

        public void collectPages(FluxSink<MemeInfo> fluxSink, long elementToRead) {

            if (elementToRead <= 0) {
                return;
            }

            scrapper
                    .collectMemes(currPage++)
                    .map(DebastoExtractor::extractMemesFromHtml)
                    .subscribe(list -> {

                        for (MemeInfo info : list) {
                            if (info.getId() > endId) {
                                fluxSink.next(info);
                            }
                            this.maxId = Math.max(this.maxId, info.getId());
                        }

                        if (this.maxId > endId) {
                            this.maxId = maxId;
                            collectPages(fluxSink, elementToRead - list.size());
                        } else {
                            fluxSink.complete();
                        }
                    });
        }
    }

    private final HtmlScrapper scrapper;
    private final ContentPusher contentPusher;
    private final MetaPusher metaPusher;

    private final WebClient webClient;

    @Autowired
    public MemeCollectorApi(HtmlScrapper scrapper, ContentPusher contentPusher, MetaPusher metaPusher) {
        this.scrapper = scrapper;
        this.contentPusher = contentPusher;
        this.metaPusher = metaPusher;

        webClient = WebClient.builder()
                .baseUrl("http://debeste.de")
                .build();
    }

    @PostMapping("/collect")
    public Mono<Void> collect(@RequestParam("endOffset") int endOffset, int collectorNumber, int totalCollectors) {
        return Flux
                .<MemeInfo>create(fluxSink -> {
                    PageReaderContext context = new PageReaderContext(endOffset);
                    fluxSink.onRequest(n -> {
                        context.collectPages(fluxSink, n);
                    });
                })
                .filter(memeInfo -> {
                    int hash = Objects.hash(memeInfo.getId());
                    return hash % totalCollectors == collectorNumber;
                })
                .flatMap(memeInfo -> {
                    MemeContent content = memeInfo.getContent();
                    if (content instanceof ExternalImageContent) {
                        ExternalImageContent imageContent = (ExternalImageContent) content;
                        Flux<DataBuffer> buffer = webClient.get()
                                .uri(imageContent.getImageUrl())
                                .exchange()
                                .flatMapMany(response -> response.bodyToFlux(DataBuffer.class));

                        String[] splittedKey = imageContent.getImageUrl().split("/");
                        String key = splittedKey[splittedKey.length - 1];


                        S3ImageContent s3ImageContent = new S3ImageContent(key);
                        MemeInfo scrappedMemeInfo = new MemeInfo(memeInfo.getId(), memeInfo.getTitle(),
                                s3ImageContent, memeInfo.getRating());

                        ByteBuffer buffer1 = ByteBuffer.allocate(10000);

                        return contentPusher
                                .pushData(key, buffer)
                                .then(Mono.just(scrappedMemeInfo));
                    }

                    return Mono.empty();
                })
                .doOnNext(metaPusher::push)
                .then();
    }


}
