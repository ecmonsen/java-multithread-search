package io.sunrisedata.pipeline1;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearcherTest {
    @Test
    public void searcher_shouldFind() {
        Searcher s = new Searcher("bar");
        byte[] b = new String("foo\nbar\nbiz\nbaz\nbar\nbarb").getBytes(StandardCharsets.UTF_8);
        int matchCount = s.countMatches(b);
        assertEquals(2, matchCount);

    }

    @Test
    public void searcher_shouldNotFind() {
        Searcher s = new Searcher("bar");
        byte[] b = new String("foo\nba\nbiz\nbaz\nar").getBytes(StandardCharsets.UTF_8);
        int matchCount = s.countMatches(b);
        assertEquals(0, matchCount);
    }
}