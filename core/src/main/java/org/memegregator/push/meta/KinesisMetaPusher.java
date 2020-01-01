package org.memegregator.push.meta;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.memegregator.entity.MemeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
public class KinesisMetaPusher implements MetaPusher {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AmazonKinesis kinesis;
    private final String streamName;

    @Autowired
    public KinesisMetaPusher(
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.accessKey}") String accessKey,
            @Value("${kinesis.streamName}") String streamName
    ) {
        this.streamName = streamName;
        kinesis = AmazonKinesisClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(Regions.EU_CENTRAL_1)
                .build();
    }

    @Override
    public void push(MemeInfo memeInfo) {
        try {
            PutRecordRequest request = new PutRecordRequest();
            request.setPartitionKey(Integer.toString(memeInfo.getId()));
            request.setData(ByteBuffer.wrap(mapper.writeValueAsBytes(memeInfo)));
            request.setStreamName(streamName);
            kinesis.putRecord(request);
        } catch (JsonProcessingException e) {
            // probably we'll never come here
            throw new IllegalStateException(e);
        }
    }
}
