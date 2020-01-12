package org.memegregator;

import org.memegregator.collectors.Collector;
import org.memegregator.entity.info.MemeInfo;
import org.memegregator.publishers.Publisher;
import org.memegregator.puller.WebClientPuller;
import org.memegregator.scrappers.Scrapper;
import org.memegregator.storage.S3ContentStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan
@EnableWebFlux
@Import({S3ContentStorage.class, WebClientPuller.class})
public class CollectorServer {


  @Autowired
  public CollectorServer(
      Scrapper scrapper,
      Collector collector,
      Publisher publisher
  ) {
    Schedulers.enableMetrics();
    Flux<MemeInfo> memeStream = scrapper.getScrappingStream().metrics();
    Flux<MemeInfo> collectedMemes = collector.collectMemes(memeStream);
    publisher.publishMemes(collectedMemes);
  }

  public static void main(String[] args) {
    SpringApplication.run(CollectorServer.class, args);
  }
}
