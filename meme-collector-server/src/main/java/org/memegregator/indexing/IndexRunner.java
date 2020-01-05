package org.memegregator.indexing;

import org.memegregator.collectors.Collector;
import org.memegregator.entity.MemeInfo;
import org.memegregator.publishers.Publisher;
import org.memegregator.scrappers.Scrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class IndexRunner {

    private final Logger LOGGER = LoggerFactory.getLogger(IndexRunner.class);
    @Autowired
    public IndexRunner(
            Scrapper scrapper,
            Collector collector,
            Publisher publisher
    ) {
        Flux<MemeInfo> memeStream = scrapper.getScrappingStream();
        Flux<MemeInfo> collectedMemes = collector.collectMemes(memeStream).onErrorResume(e -> {
            LOGGER.error("Error encountered while collecting memes : {} " , e);
            return Mono.empty();
        });
        publisher.publishMemes(collectedMemes);
    }

}
