package org.memegregator.storage;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;

public class S3UploadContext {

  private static final int MAX_BYTES_TO_BUFFER = 6291456;

  // used  to preserve send order
  private final ReentrantLock reentrantLock = new ReentrantLock(true);
  private final S3AsyncClient s3Client;

  private final String bucket;
  private final String key;

  private volatile ByteArrayOutputStream outputStream;
  private volatile int currentSize;

  private volatile List<CompletedPart> tags;
  private volatile Mono<CreateMultipartUploadResponse> initialResult;

  private volatile Mono<Void> chain = Mono.empty();

  private volatile int position = 1;

  public S3UploadContext(S3AsyncClient s3Client, String bucket, String key) {
    this.s3Client = s3Client;

    this.bucket = bucket;
    this.key = key;
  }


  public Mono<Void> processPart(byte[] buffer) {
    try {
      reentrantLock.lock();

      if (outputStream == null) {
        outputStream = new ByteArrayOutputStream(buffer.length);
      }
      outputStream.write(buffer);
      currentSize += buffer.length;

      if (currentSize > MAX_BYTES_TO_BUFFER) {
        if (initialResult == null) {
          initiateMultipartUpload();
        }
        sendCurrentAsMultipart().subscribe();
        outputStream = null;
        currentSize = 0;
      }

      return chain;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      reentrantLock.unlock();
    }
  }

  public Mono<Void> finishUpload() {

    reentrantLock.lock();
    if (outputStream == null && initialResult == null) {
      reentrantLock.unlock();
      return Mono.empty();
    }
    try {
      // if request initialized it means we are using a multipart upload
      if (initialResult != null) {
        if (outputStream != null) {
          sendCurrentAsMultipart();
        }

        return finalizeMultipartUpload();
      }

      return sendCurrentAsSingle();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      reentrantLock.unlock();
    }
  }

  private Mono<Void> initiateMultipartUpload() {
    reentrantLock.lock();

    this.initialResult = Mono.fromFuture(
        s3Client.createMultipartUpload(builder -> {
          builder.bucket(bucket).key(key);
        })
    );

    chain = chain.then(initialResult).then().cache();
    tags = new ArrayList<>();

    reentrantLock.unlock();
    return chain;
  }

  private Mono<Void> sendCurrentAsMultipart() {
    reentrantLock.lock();
    int partNumber = position++;

    AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytes(outputStream.toByteArray());
    Mono<Void> mono = initialResult
        .flatMap(result -> {
          return Mono.fromFuture(
              s3Client.uploadPart(builder -> {
                    builder.bucket(bucket)
                        .key(key)
                        .uploadId(result.uploadId())
                        .partNumber(partNumber);

                  }, asyncRequestBody
              )
          );
        })
        .map(result -> {
          tags.add(CompletedPart.builder().eTag(result.eTag()).partNumber(partNumber).build());
          return result;
        }).then();

    chain = chain.then(mono).cache();

    reentrantLock.unlock();
    return chain;

  }

  private Mono<Void> finalizeMultipartUpload() {
    reentrantLock.lock();
    chain = chain.then(
        initialResult.flatMap(result -> {
          return Mono.create(sink -> {
            CompletableFuture<CompleteMultipartUploadResponse> future = s3Client
                .completeMultipartUpload(builder -> {
                  builder
                      .bucket(bucket)
                      .key(key)
                      .uploadId(result.uploadId())
                      .multipartUpload(upload -> {
                        upload.parts(tags);
                      });

                });
            future.handle((completeMultipartUploadResponse, throwable) -> {
//                            sink.success(completeMultipartUploadResponse);
              if (completeMultipartUploadResponse != null) {
                sink.success(completeMultipartUploadResponse);
              } else {
                sink.error(throwable);
              }
              return null;
            });
          }).then();
        })
    ).cache();
    reentrantLock.unlock();
    return chain;
  }

  private Mono<Void> sendCurrentAsSingle() {
    return Mono.fromFuture(s3Client.putObject(
        builder -> builder.bucket(bucket).key(key).build(),
        AsyncRequestBody.fromBytes(outputStream.toByteArray())
    )).then();
  }
}
