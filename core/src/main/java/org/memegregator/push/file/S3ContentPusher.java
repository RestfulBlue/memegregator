package org.memegregator.push.file;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class S3ContentPusher implements ContentPusher {

    private final String secretKey;
    private final String accessKey;
    private final String bucketName;

    private final AmazonS3 s3;

    @Autowired
    public S3ContentPusher(
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.accessKey}") String accessKey,
            @Value("${s3.bucketName}") String bucketName
    ) {
        this.secretKey = secretKey;
        this.accessKey = accessKey;
        this.bucketName = bucketName;

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        s3 = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.EU_CENTRAL_1)
                .build();
    }


    @Override
    public Mono<Void> pushData(String filename, Flux<byte[]> data) {
        S3UploadContext uploadContext = new S3UploadContext(s3, bucketName, filename);
        return data
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(uploadContext::processPart)
                .then(Mono.fromRunnable(uploadContext::finishUpload))
                .then();
    }

}
