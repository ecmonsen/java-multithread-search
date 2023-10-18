package io.sunrisedata.pipeline1;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DownloaderTest {
    @Test
    public void searcher_shouldFind() throws URISyntaxException {
        S3Client s3Client = DependencyFactory.s3Client(
                "http://localhost:9444",
                "ninja"
        );

        Downloader d = new Downloader(
                s3Client, "local-test", "p2"
        );
        byte[] b = new byte[0];
        try {
            b = d.download(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
//            fail("IOException during download");
        }

        assertEquals(1048584, b.length);

    }

}
