package org.memegregator.publishers;

import org.memegregator.entity.MemeInfo;
import org.memegregator.service.MemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class MongoPublisher implements Publisher {

  private final MemeService memeService;

  @Autowired
  public MongoPublisher(MemeService memeService) {
    this.memeService = memeService;
  }

  @Override
  public void publishMemes(Flux<MemeInfo> memeStream) {
    memeStream
        .flatMap(memeService::saveMeme)
        .subscribe();
  }
}
