package org.memegregator.puller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class HttpPuller {

  private final int MAX_CHUNK_SIZE = 6096;
  private final CloseableHttpAsyncClient httpClient;

  public HttpPuller() {
    httpClient = HttpAsyncClients.custom()
    .setMaxConnTotal(10000)
    .setMaxConnPerRoute(10000)
    .build();
    httpClient.start();
  }

  public Mono<String> pullAsString(String url) {
    HttpGet request = new HttpGet(url);
    return Mono.create(sink -> {

      Future<HttpResponse> responseFuture = httpClient
          .execute(request, new FutureCallback<HttpResponse>() {

            @Override
            public void completed(HttpResponse result) {
              try {
                sink.success(EntityUtils.toString(result.getEntity(), "UTF-8"));
              } catch (IOException e) {
                sink.error(e);
              }
            }

            @Override
            public void failed(Exception ex) {
              sink.error(ex);
            }

            @Override
            public void cancelled() {
              sink.error(new IllegalAccessError("request was cancelled"));
            }
          });

      sink.onCancel(() -> responseFuture.cancel(true));
      sink.onDispose(() -> responseFuture.cancel(true));
    });
  }

  public Flux<byte[]> pullAsFlux(String url) {

    return Flux.create(sink -> {
      Future<Boolean> responseFuture = httpClient
          .execute(HttpAsyncMethods.createGet(url), new AsyncByteConsumer<Boolean>() {

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

            }

            @Override
            protected Boolean buildResult(HttpContext context) throws Exception {
              sink.complete();
              return true;
            }

          }, null);

      sink.onCancel(() -> responseFuture.cancel(true));
      sink.onDispose(() -> responseFuture.cancel(true));
    });
  }

}
