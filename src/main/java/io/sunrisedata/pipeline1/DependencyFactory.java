
package io.sunrisedata.pipeline1;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {
    private static final Logger logger = Logger.getLogger("s3test");

    private DependencyFactory() {
    }

    /**
     * @return an instance of S3Client
     */
    public static S3Client s3Client(String s3Endpoint, String awsProfileName) throws URISyntaxException {
        logger.fine(String.format("S3 client using endpoint %s, profile %s", s3Endpoint, awsProfileName));
            S3ClientBuilder builder = S3Client.builder()
                    .forcePathStyle(true)
                    .region(Region.US_WEST_2)
                    .httpClientBuilder(UrlConnectionHttpClient.builder());
            if (s3Endpoint != null) {
                builder = builder.endpointOverride(new URI(s3Endpoint));
            }
            if (awsProfileName != null) {
                builder = builder.credentialsProvider(ProfileCredentialsProvider.create(awsProfileName));
            }
            return builder.build();

    }
}
