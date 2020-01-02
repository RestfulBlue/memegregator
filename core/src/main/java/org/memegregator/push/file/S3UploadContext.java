package org.memegregator.push.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.io.*;
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

    private ByteArrayOutputStream outputStream;
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


    public void processPart(byte[] buffer) {
        try {
            reentrantLock.lock();

            if(outputStream == null){
                outputStream = new ByteArrayOutputStream(buffer.length);
            }
            outputStream.write(buffer);
            currentSize += buffer.length;

            if (currentSize > MAX_BYTES_TO_BUFFER) {
                if (initialRequest == null) {
                    initiateMultipartUpload();
                }
                sendCurrentAsMultipart();
                outputStream = null;
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
                if (outputStream != null) {
                    sendCurrentAsMultipart();
                }
                finalizeMultipartUpload();
                return;
            }

            sendCurrentAsSingle();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
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
                .withInputStream(new ByteArrayInputStream(outputStream.toByteArray()));

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
        s3Client.putObject(bucket, key, new ByteArrayInputStream(outputStream.toByteArray()), metadata);
    }
}
