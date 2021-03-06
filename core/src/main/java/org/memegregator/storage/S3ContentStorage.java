package org.memegregator.storage;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import org.memegregator.puller.StreamWithLength;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Component
public class S3ContentStorage implements ContentStorage {

  private final String bucketName;
  private final S3AsyncClient s3AsyncClient;

  @Autowired
  public S3ContentStorage(
      @Value("${aws.secretKey}") String secretKey,
      @Value("${aws.accessKey}") String accessKey,
      @Value("${s3.bucketName}") String bucketName
  ) {
    this.bucketName = bucketName;
    AwsCredentialsProvider creds = StaticCredentialsProvider.create(new AwsCredentials() {
      @Override
      public String accessKeyId() {
        return accessKey;
      }

      @Override
      public String secretAccessKey() {
        return secretKey;
      }
    });

    s3AsyncClient = S3AsyncClient
        .builder()
        .credentialsProvider(creds)
        .region(Region.EU_CENTRAL_1)
        .build();
  }


  @Override
  public Mono<String> pushData(String filename, Mono<ClientResponse> responseMono) {

    return responseMono.flatMap(clientResponse -> {
      OptionalLong optionalLong = clientResponse.headers().contentLength();
      if (!optionalLong.isPresent()) {
        throw new IllegalStateException(
            "Response from server don't contain length information, streaming isn't possible");
      }

      MessageDigest messageDigest = getMessageDigest();
      ReentrantLock reentrantLock = new ReentrantLock(true);

      return Mono.fromFuture(s3AsyncClient.putObject(
          builder -> builder.bucket(bucketName).key(filename).build(),
          new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
              return Optional.of(optionalLong.getAsLong());
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> producerBodySubscriber) {
              clientResponse.bodyToFlux(DataBuffer.class)
                  .subscribe(new BaseSubscriber<DataBuffer>() {

                    Subscription httpBodySubscription;

                    @Override
                    protected void hookOnSubscribe(Subscription httpBodySubscription) {
                      this.httpBodySubscription = httpBodySubscription;
                      producerBodySubscriber.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                          httpBodySubscription.request(n);
                        }

                        @Override
                        public void cancel() {
                          httpBodySubscription.cancel();
                        }
                      });


                    }

                    @Override
                    protected void hookOnNext(DataBuffer value) {
                      reentrantLock.lock();
                      byte[] data = new byte[value.readableByteCount()];
                      value.read(data);
                      messageDigest.update(data, 0, data.length);
                      producerBodySubscriber.onNext(ByteBuffer.wrap(data));
                      DataBufferUtils.release(value);
                      reentrantLock.unlock();
                    }

                    @Override
                    protected void hookFinally(SignalType type) {
                      switch (type) {
                        case ON_COMPLETE:
                          producerBodySubscriber.onComplete();
                          return;
                        case CANCEL:
                          this.dispose();
                          return;
                        default:
                          producerBodySubscriber.onError(
                              new IllegalStateException("Processing was finished with error"));
                      }
                    }
                  });
            }
          }
      )).then(
          Mono.fromCallable(() -> new String(Base64.getEncoder().encode(messageDigest.digest()))));
    });
  }

  @Override
  public Mono<StreamWithLength> pullData(String key) {
    return pullData(key, null);
  }

  @Override
  public Mono<StreamWithLength> pullData(String key, String range) {
    return Mono.create(sink -> {
      s3AsyncClient.getObject(builder -> {
        builder.bucket(bucketName).key(key);
        if (range != null) {
          builder.range(range);
        }
      }, new AsyncResponseTransformer<GetObjectResponse, Object>() {

        private CompletableFuture<Object> completableFuture;
        private long contentLength;
        private String contentRange;

        @Override
        public CompletableFuture<Object> prepare() {
          this.completableFuture = new CompletableFuture<>();
          return completableFuture;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
          this.contentLength = response.contentLength();
          Map<String, List<String>> metadata = response.sdkHttpResponse().headers();
          contentRange = metadata.get("x-amz-content-range").get(0);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
          sink.success(new StreamWithLength(contentLength,contentRange, Flux.create(sink -> {

            publisher.subscribe(new BaseSubscriber<ByteBuffer>() {

              private Subscription subscription;

              @Override
              protected void hookOnSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(16);
                sink.onCancel(this);
                sink.onDispose(this);
              }

              @Override
              protected void hookOnNext(ByteBuffer value) {
                sink.next(value.array());
                subscription.request(1);
              }

              @Override
              protected void hookFinally(SignalType type) {
                if (type.equals(SignalType.ON_COMPLETE)) {
                  sink.complete();
                  completableFuture.complete(new Object());
                } else {
                  sink.error(new IllegalStateException("test"));
                  completableFuture.completeExceptionally(new IllegalStateException("test"));
                }
              }
            });
          })));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
          sink.error(error);
        }
      });
    });
  }

  @Override
  public Mono<Void> dropData(String key) {
    return Mono.fromFuture(s3AsyncClient.deleteObject(builder -> {
      builder.bucket(bucketName).key(key).build();
    })).then();
  }

  private MessageDigest getMessageDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
