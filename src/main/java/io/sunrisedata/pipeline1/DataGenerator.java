package io.sunrisedata.pipeline1;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DataGenerator {
    private static final Logger logger = Logger.getLogger("s3test");
    private final S3Client s3Client;
    private final String bucketName;
    private final String bucketPrefix;
    private final String wordListFilePath;
    private final int maxSize;

    public DataGenerator(S3Client s3Client, String bucketName, String bucketPrefix, String wordListFilePath, int maxSize) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.bucketPrefix = bucketPrefix;
        this.wordListFilePath = wordListFilePath;
        this.maxSize = maxSize;
    }

    public void generate(int fileCount) throws Exception {
        if (!s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build()).sdkHttpResponse().isSuccessful()) {
            if (!s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).sdkHttpResponse().isSuccessful()) {
                throw new Exception("could not create bucket");
            }
            ;
        }

        if (s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketName).prefix(bucketPrefix).maxKeys(1).build()).hasContents()) {
            return;
        }
        logger.info("Creating test files. This only happens once");

        try {
            // Read wordlist from file
            List<byte[]> allWords = Files.readAllLines(Paths.get(wordListFilePath))
                    .stream()
                    .map(s -> s.getBytes(StandardCharsets.UTF_8))
                    .collect(Collectors.toList());

            Random random = new Random();
            // Generate 1MB files with random selections of words from the wordlist
            // Upload them to S3
            for (int fileIndex = 0; fileIndex < fileCount; fileIndex++) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int totalBytes = 0;
                while (totalBytes < maxSize) {
                    byte[] toWrite = allWords.get(random.nextInt(allWords.size()));
                    bytes.write(toWrite);
                    bytes.write("\n".getBytes(StandardCharsets.UTF_8));
                    totalBytes += toWrite.length + 1;
                }
                ByteArrayInputStream readBytes = new ByteArrayInputStream(bytes.toByteArray());

                String key = String.format("%s/words_%d", bucketPrefix, fileIndex);
                logger.warning(key);
                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(key)
                                .contentEncoding("utf-8")
                                .build(),
                        RequestBody.fromInputStream(readBytes, bytes.size()));
            }
        } catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }
    }

}
