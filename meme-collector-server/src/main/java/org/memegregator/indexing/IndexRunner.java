package org.memegregator.indexing;

import org.memegregator.collectors.Collector;
import org.memegregator.entity.MemeInfo;
import org.memegregator.publishers.Publisher;
import org.memegregator.scrappers.Scrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Component
public class IndexRunner {


  @Autowired
  public IndexRunner(
      Scrapper scrapper,
      Collector collector,
      Publisher publisher
  ) {
    Schedulers.enableMetrics();
    Flux<MemeInfo> memeStream = scrapper.getScrappingStream().metrics();
    Flux<MemeInfo> collectedMemes = collector.collectMemes(memeStream);
    publisher.publishMemes(collectedMemes);
  }

}
