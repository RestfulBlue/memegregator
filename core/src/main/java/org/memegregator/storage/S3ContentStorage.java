package org.memegregator.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Component
public class S3ContentStorage implements ContentStorage {

  private final String secretKey;
  private final String accessKey;
  private final String bucketName;

  private volatile long started = 0;
  private volatile long finished = 0;

  private final S3AsyncClient s3AsyncClient;

  @Autowired
  public S3ContentStorage(
      @Value("${aws.secretKey}") String secretKey,
      @Value("${aws.accessKey}") String accessKey,
      @Value("${s3.bucketName}") String bucketName
  ) {
    this.secretKey = secretKey;
    this.accessKey = accessKey;
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
  public Mono<Void> pushData(String filename, Flux<byte[]> data) {
    started++;
    S3UploadContext uploadContext = new S3UploadContext(s3AsyncClient, bucketName, filename);
    return data
        .handle((next, sink) -> {
          uploadContext.processPart(next).subscribe();
        })
        .then(Mono.create((sink) -> {
          finished++;
          uploadContext
              .finishUpload()
              .subscribe(sink::success, sink::error, sink::success);
        }));
  }

}
