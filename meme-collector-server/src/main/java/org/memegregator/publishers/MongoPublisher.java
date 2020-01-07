package org.memegregator.publishers;

import org.memegregator.entity.MemeInfo;
import org.memegregator.entity.content.InternalMemeContent;
import org.memegregator.service.MemeService;
import org.memegregator.storage.ContentStorage;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

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

    memeStream.subscribe(new BaseSubscriber<MemeInfo>() {

      private Subscription subscription;

      @Override
      protected void hookOnSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(256);
      }

      @Override
      protected void hookOnNext(MemeInfo memeInfo) {
        memeService
            .saveMeme(memeInfo)
            .then()
            .onErrorResume(throwable -> {
              if (memeInfo.getContent() instanceof InternalMemeContent) {
                InternalMemeContent memeContent = (InternalMemeContent) memeInfo.getContent();
                return memeContent.dropFromStorage(storage).then();
              }
              return Mono.empty();
            })
            .doFinally(signalType -> subscription.request(1))
            .subscribe();
      }

      @Override
      protected void hookFinally(SignalType type) {
        super.hookFinally(type);
      }
    });
  }
}
