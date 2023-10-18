package io.sunrisedata.pipeline1;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;

public class Downloader {
    private final S3Client s3Client;
    private final String bucketName;
    private final String bucketPrefix;

    public Downloader(S3Client s3Client, String bucketName, String bucketPrefix) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.bucketPrefix = bucketPrefix;
    }

    public byte[] download(int fileIndex) throws IOException {
        String key = String.format("%s/words_%d", bucketPrefix, fileIndex);
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key).build());
        byte[] b = response.asByteArray();
//        System.out.printf("Downloaded %d bytes for %s%n", b.length, key);
        return b;
    }

}
