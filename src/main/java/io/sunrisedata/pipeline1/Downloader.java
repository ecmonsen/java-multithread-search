package io.sunrisedata.pipeline1;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.logging.Logger;

public class Downloader {
    private final S3Client s3Client;
    private final String bucketName;
    private final String bucketPrefix;
    private final Logger logger;

    public Downloader(S3Client s3Client, String bucketName, String bucketPrefix) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.bucketPrefix = bucketPrefix;
        logger = Logger.getLogger("s3test.downloader");
    }

    public byte[] download(int fileIndex) throws IOException {
        String key = String.format("%s/words_%d", bucketPrefix, fileIndex);
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key).build());
            byte[] b = response.asByteArray();
            logger.fine(String.format("Downloaded %d bytes for %s%n", b.length, key));
            return b;
        } catch (RuntimeException re) {
            throw new RuntimeException(String.format("Index %d: %s", fileIndex, re.getMessage()), re);
        }
    }

}
