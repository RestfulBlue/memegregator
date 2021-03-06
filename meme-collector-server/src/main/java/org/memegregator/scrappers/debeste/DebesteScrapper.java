package org.memegregator.scrappers.debeste;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.memegregator.entity.info.MemeInfo;
import org.memegregator.entity.offsets.DebesteOffset;
import org.memegregator.puller.HttpPuller;
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
import reactor.util.function.Tuple2;

@Component
@Import(OffsetService.class)
public class DebesteScrapper implements Scrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DebesteScrapper.class);
  private final HttpPuller puller;

  private final OffsetService offsetService;
  private final String host;

  private ReentrantLock lock = new ReentrantLock(true);
  private volatile int scheduledDrains = 0;
  private volatile long demand = 0;

  // scheduling context
  private final BlockingQueue<Mono<Void>> chain = new ArrayBlockingQueue<>(300);
  private final BlockingQueue<MemeInfo> blockingQueue = new ArrayBlockingQueue(1000);
  private volatile FluxSink<MemeInfo> memeSink;


  private volatile int currentPage = 1;
  private volatile int endOffset = 0;

  private volatile int checkpointOffset;
  private volatile int maxPageOffset = Integer.MIN_VALUE;
  private volatile boolean endReached = false;

  @Autowired
  public DebesteScrapper(HttpPuller httpPuller,
      OffsetService offsetService,
      @Value("${host:http://debeste.de}") String host) {
    this.host = host;
    this.offsetService = offsetService;
    this.puller = httpPuller;

    DebesteOffset defaultOffset = new DebesteOffset();
    defaultOffset.setCheckpointOffset(0);
    defaultOffset.setCommittedOffset(0);
    defaultOffset.setCheckpointedPage(1);
    defaultOffset.setCommitted(false);

    addToChain(offsetService
        .findOffset("debeste")
        .defaultIfEmpty(defaultOffset)
        .flatMap((offset) -> {
          return initFromOffset((DebesteOffset) offset);
        })
        .then()
    );
  }


  private void addToChain(Mono<Void> mono) {
    lock.lock();
    chain.add(mono);
    if (chain.size() == 1) {
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

  private Mono<Void> initFromOffset(DebesteOffset offset) {
    return Mono.fromRunnable(() -> {
      lock.lock();
      currentPage = offset.isCommitted() ? 1 : offset.getCheckpointedPage();
      checkpointOffset = offset.getCheckpointOffset();
      endOffset = offset.getCommittedOffset();
      lock.unlock();
    });
  }

  private void schedulePageFetch() {
    addToChain(puller.pullRaw(host + "/" + currentPage)
        .<Tuple2<Integer, List<MemeInfo>>>flatMap(clientResponse -> {
          if (clientResponse.statusCode().is3xxRedirection()) {
            endReached = true;
            return Mono.empty();
          }

          return clientResponse
              .bodyToMono(String.class)
              .handle((page, sink) -> {
                Tuple2<Integer, List<MemeInfo>> parseResult = DebesteHtmlParser
                    .parseMemesFromPage(host, page);


                blockingQueue.addAll(parseResult.getT2());
                maxPageOffset = Math.max(maxPageOffset, parseResult.getT1());
                sink.complete();
              });
        })
        .then(processScrappingStep())
    );
  }

  private Mono<Void> processScrappingStep() {
    return Mono.create((sink) -> {
      DebesteOffset offset = new DebesteOffset();
      offset.setCheckpointedPage(currentPage);

      if (endReached || maxPageOffset < endOffset) {
        offset.setCommitted(true);
        offset.setCommittedOffset(checkpointOffset);
        offset.setCheckpointOffset(checkpointOffset);

        endOffset = checkpointOffset;
        currentPage = 1;
        offsetService
            .saveOffset("debeste", offset)
            .delayElement(Duration.ofSeconds(10))
            .subscribe(sink::success);
      } else {
        checkpointOffset = Math.max(checkpointOffset, maxPageOffset);
        offset.setCommitted(false);
        offset.setCommittedOffset(endOffset);
        offset.setCheckpointOffset(checkpointOffset);
        currentPage++;
        offsetService
            .saveOffset("debeste", offset)
            .subscribe(sink::success);
      }
      maxPageOffset = Integer.MIN_VALUE;
    }).then();
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
    return Flux.create(sink -> {
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
