package org.memegregator.publishers;

import org.memegregator.entity.MemeInfo;
import org.memegregator.service.MemeService;
import org.memegregator.storage.ContentStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MongoPublisher implements Publisher {

  private final MemeService memeService;
  private final ContentStorage storage;

  @Autowired
  public MongoPublisher(MemeService memeService, ContentStorage storage) {
    this.memeService = memeService;
    this.storage = storage;
  }

  @Override
  public void publishMemes(Flux<MemeInfo> memeStream) {
    memeStream
        .flatMap(memeInfo -> {
          return memeService
              .saveMeme(memeInfo);
//              .onErrorResume(storage.dropData())
        })
        .onErrorResume(throwable -> {
          return Mono.empty();
        })
        .subscribe();
  }
}
