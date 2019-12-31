package org.memegregator.core.blob;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class S3FilePusher implements FilePusher {

    private final String secretKey;
    private final String accessKey;
    private final String bucketName;

    private final AmazonS3 s3;

    @Autowired
    public S3FilePusher(
            @Value("${s3.secretKey}") String secretKey,
            @Value("${s3.accessKey}") String accessKey,
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

        List<Bucket> buckets = s3.listBuckets();
        System.out.printf("123");

    }


    @Override
    public String pushFile(String filename, Flux<DataBuffer> data) {
        return null;
    }

}
