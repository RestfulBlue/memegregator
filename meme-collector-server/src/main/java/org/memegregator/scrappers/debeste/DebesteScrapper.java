package org.memegregator.scrappers.debeste;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.memegregator.entity.MemeInfo;
import org.memegregator.puller.WebClientPuller;
import org.memegregator.scrappers.Scrapper;
import org.memegregator.service.OffsetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Import(OffsetService.class)
public class DebesteScrapper implements Scrapper {

  private static final String PROVIDER_KEY = "DEBESTE";
  private static final Logger LOGGER = LoggerFactory.getLogger(DebesteScrapper.class);
  private final WebClientPuller puller;

  private final OffsetService offsetService;
  private final String host;

  private ReentrantLock lock = new ReentrantLock(true);
  private volatile int scheduledDrains = 0;
  private volatile long demand = 0;

  private final BlockingQueue<Mono<Void>> chain = new ArrayBlockingQueue<>(300);
  private final BlockingQueue<MemeInfo> blockingQueue = new ArrayBlockingQueue(1000);
  private volatile int currentPage = 1;
  private volatile FluxSink<MemeInfo> memeSink;

  @Autowired
  public DebesteScrapper(OffsetService offsetService,
      @Value("${host:http://debeste.de}") String host) {
    this.host = host;
    this.offsetService = offsetService;
    this.puller = new WebClientPuller();
  }


  private void addToChain(Mono<Void> mono) {
    lock.lock();
    chain.add(mono);
    if(chain.size() == 1){
      dispatchNext();
    }
    lock.unlock();
  }

  private void dispatchNext() {
    lock.lock();
    if (!chain.isEmpty()) {
      chain
          .peek()
          .publishOn(Schedulers.parallel())
          .doFinally(signalType -> {
            lock.lock();
            chain.poll();
            lock.unlock();
            dispatchNext();
          }).subscribe();
    }
    lock.unlock();
  }

  private void schedulePageFetch() {
    addToChain(puller.pullAsString(host + "/" + currentPage++)
        .map((page) -> {
          return DebesteHtmlParser.parseMemesFromPage(host, page);
        })
        .handle((list, sink) -> {
          blockingQueue.addAll(list);
          sink.complete();
        }));
  }

  private void scheduleDrain() {
    lock.lock();
    scheduledDrains++;
    addToChain(Mono.fromRunnable(() -> {

      lock.lock();
      while (demand > 0 && !blockingQueue.isEmpty()) {
        memeSink.next(blockingQueue.poll());
        demand--;
      }

      if (demand > 0) {
        schedulePageFetch();
        scheduleDrain();
      }

      scheduledDrains--;
      lock.unlock();
    }));
    lock.unlock();
  }


  @Override
  public Flux<MemeInfo> getScrappingStream() {
    return Flux.<MemeInfo>create(sink -> {
      this.memeSink = sink;
      sink.onRequest(n -> {
        addToChain(Mono.fromRunnable(() -> {
          lock.lock();
          demand += n;
          if (scheduledDrains == 0) {
            scheduleDrain();
          }
          lock.unlock();
        }));
      });
    });
  }
}
