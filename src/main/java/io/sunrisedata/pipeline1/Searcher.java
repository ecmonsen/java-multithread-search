package io.sunrisedata.pipeline1;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

public class Searcher {
    private final String wordToSearch;

    public Searcher(String wordToSearch) {

        this.wordToSearch = wordToSearch;
    }

    public int countMatches(byte[] utf8Bytes) {
        String wordsAsString = new String(utf8Bytes, StandardCharsets.UTF_8);
        String[] allWords = wordsAsString.split("\n");
        return Arrays.stream(allWords).mapToInt(s -> Objects.equals(s, wordToSearch) ? 1 : 0).sum();
    }
}
