package org.memegregator.puller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncByteConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class AHCPuller implements HttpPuller {

  private final int MAX_CHUNK_SIZE = 6096;
  private final CloseableHttpAsyncClient httpClient;

  private final String METRIC_NAME = "AHC.PULLER";

  private final MeterRegistry meterRegistry;

  private final AtomicInteger inProgressGauge;
  private final Counter receivedCounter;
  private final Counter processedCounter;
  private final Counter errorsCounter;

  private final Timer timer;

  @Autowired
  public AHCPuller(MeterRegistry registry) {
    httpClient = HttpAsyncClients.custom()
        .setMaxConnTotal(200)
        .setMaxConnPerRoute(200)
        .build();

    this.meterRegistry = registry;

    this.inProgressGauge = registry
        .gauge(METRIC_NAME, Tags.of("type", "current"), new AtomicInteger());
    this.receivedCounter = registry.counter(METRIC_NAME, Tags.of("type", "received"));
    this.processedCounter = registry.counter(METRIC_NAME, Tags.of("type", "processed"));
    this.errorsCounter = registry.counter(METRIC_NAME, Tags.of("type", "processedWithErrors"));

    timer = Timer.builder("request.time").tags("type", "duration").register(registry);

    httpClient.start();

  }

  public Mono<String> pullAsString(String url) {

    Timer.Sample sample = started();

    HttpGet request = new HttpGet(url);
    return Mono.<String>create(sink -> {
      Future<HttpResponse> responseFuture = httpClient
          .execute(request, new FutureCallback<HttpResponse>() {

            @Override
            public void completed(HttpResponse result) {
              try {
                sink.success(EntityUtils.toString(result.getEntity(), "UTF-8"));
                finished(sample);
              } catch (IOException e) {
                sink.error(e);
              }
            }

            @Override
            public void failed(Exception ex) {
              sink.error(ex);
              AHCPuller.this.failed(sample);
            }

            @Override
            public void cancelled() {
              sink.error(new IllegalAccessError("request was cancelled"));
              AHCPuller.this.failed(sample);
            }
          });

      sink.onCancel(() -> responseFuture.cancel(true));
      sink.onDispose(() -> responseFuture.cancel(true));
    }).publishOn(Schedulers.parallel());
  }

  public Flux<byte[]> pullAsFlux(String url) {
    Timer.Sample sample = started();
    return Flux.<byte[]>create(sink -> {
      Future<Boolean> responseFuture = httpClient
          .execute(HttpAsyncMethods.createGet(url), new AsyncByteConsumer<Boolean>() {

            private long start = System.currentTimeMillis();

            @Override
            protected void onByteReceived(ByteBuffer buf, IOControl ioctrl) throws IOException {
              while (buf.hasRemaining()) {
                int size = buf.remaining() > MAX_CHUNK_SIZE ? MAX_CHUNK_SIZE : buf.remaining();
                byte[] data = new byte[size];
                buf.get(data);
                sink.next(data);
              }
            }

            @Override
            protected void onResponseReceived(HttpResponse response)
                throws HttpException, IOException {
              long duration = System.currentTimeMillis() - start;
              duration = duration++;
            }

            @Override
            protected Boolean buildResult(HttpContext context) throws Exception {
//              sink.complete();
              return true;
            }

          }, new FutureCallback<Boolean>() {
            @Override
            public void completed(Boolean result) {
              sink.complete();
              finished(sample);
            }

            @Override
            public void failed(Exception ex) {
              sink.error(ex);
              AHCPuller.this.failed(sample);
            }

            @Override
            public void cancelled() {
              sink.error(new IllegalStateException());
              AHCPuller.this.failed(sample);
            }
          });

      sink.onCancel(() -> responseFuture.cancel(true));
      sink.onDispose(() -> responseFuture.cancel(true));
    }).publishOn(Schedulers.parallel());
  }

  @Override
  public Mono<ClientResponse> pullRaw(String url) {
    return null;
  }


  private Timer.Sample started() {
    inProgressGauge.incrementAndGet();
    receivedCounter.increment();
    return Timer.start(meterRegistry);
  }

  private void finished(Timer.Sample sample) {
    inProgressGauge.decrementAndGet();
    processedCounter.increment();
    sample.stop(timer);
  }

  private void failed(Timer.Sample sample) {
    errorsCounter.increment();
    inProgressGauge.decrementAndGet();
    sample.stop(timer);
  }


}
