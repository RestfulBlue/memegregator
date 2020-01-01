package org.memegregator.push.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class S3UploadContext {

    private static final int MAX_BYTES_TO_BUFFER = 6291456;

    // used  to preserve send order
    private final ReentrantLock reentrantLock = new ReentrantLock(true);
    private final AmazonS3 s3Client;

    private final String bucket;
    private final String key;

    private List<DataBuffer> buffers = new ArrayList<>();
    private InputStream inputStream;
    private int currentSize;

    private List<PartETag> tags;
    private InitiateMultipartUploadRequest initialRequest;
    private InitiateMultipartUploadResult initialResult;

    private int position = 1;

    public S3UploadContext(AmazonS3 s3Client, String bucket, String key) {
        this.s3Client = s3Client;

        this.bucket = bucket;
        this.key = key;
    }


    public void processPart(DataBuffer buffer) {
        try {
            reentrantLock.lock();
            buffers.add(buffer);
            if (inputStream == null) {
                inputStream = buffer.asInputStream();
            } else {
                inputStream = new SequenceInputStream(inputStream, buffer.asInputStream());
            }

            currentSize += buffer.readableByteCount();

            if (currentSize > MAX_BYTES_TO_BUFFER) {
                if (initialRequest == null) {
                    initiateMultipartUpload();
                }
                sendCurrentAsMultipart();
                inputStream = null;
                currentSize = 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            reentrantLock.unlock();
        }
    }

    public void finishUpload() {
        try {
            reentrantLock.lock();
            // if request initialized it means we are using a multipart upload
            if (initialRequest != null) {
                if (inputStream != null) {
                    sendCurrentAsMultipart();
                }
                finalizeMultipartUpload();
                return;
            }

            sendCurrentAsSingle();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            for (DataBuffer buffer : buffers) {
                DataBufferUtils.releaseConsumer().accept(buffer);
            }
            reentrantLock.unlock();
        }
    }

    private void initiateMultipartUpload() {
        initialRequest = new InitiateMultipartUploadRequest(bucket, key);
        initialResult = s3Client.initiateMultipartUpload(initialRequest);
        tags = new ArrayList<>();
    }

    private void sendCurrentAsMultipart() throws IOException {
        UploadPartRequest request = new UploadPartRequest()
                .withBucketName(bucket)
                .withKey(key)
                .withUploadId(initialResult.getUploadId())
                .withPartSize(currentSize)
                .withPartNumber(position++)
                .withInputStream(inputStream);

        UploadPartResult result = s3Client.uploadPart(request);
        tags.add(result.getPartETag());
    }

    private void finalizeMultipartUpload() {
        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, initialResult.getUploadId(), tags);
        s3Client.completeMultipartUpload(compRequest);
    }

    private void sendCurrentAsSingle() {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(currentSize);
        s3Client.putObject(bucket, key, inputStream, metadata);
    }
}
