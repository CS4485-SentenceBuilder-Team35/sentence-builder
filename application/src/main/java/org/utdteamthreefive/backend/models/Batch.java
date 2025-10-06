package org.utdteamthreefive.backend.models;

import java.util.List;

/**
 * Represents a batch of parsed word and bigram data processed from a text file.
 * Each batch contains lists of word and bigram deltas along with
 * metadata about processing progress (in case we need to track progress).
 *
 * This class is immutable and used to transfer parsed data between
 * the file parsing and database insertion.
 * @author Zaeem Rashid
 */
public final class Batch {

    /**
     * Represents a single word and its occurrence statistics within the batch.
     *
     * @param token the actual word or token
     * @param total total occurrences in the file
     * @param begin occurrences at the start of a sentence
     * @param end occurrences at the end of a sentence
     * @param type type classification (e.g., alpha, numeric, punctuation)
     */
    public static record WordDelta(String token, int total, int begin, int end, String type) {
    }

    /**
     * Represents a bigram, which is a pair of consecutive words.
     *
     * @param first  the first word in the bigram
     * @param second the second word in the bigram
     */
    public static record BigramKey(String first, String second) {
    }

    /**
     * Represents a bigram and its occurrence count within the batch.
     *
     * @param key   the bigram key (pair of words)
     * @param count the number of occurrences of this bigram in the file
     */
    public static record BigramDelta(BigramKey key, int count) {
    }

    public final List<WordDelta> wordDeltas;
    public final List<BigramDelta> bigramDeltas;
    public final long processedBytes;
    public final boolean end;

    /**
     * Constructs a batch with word and bigram deltas.
     * This Batch is used to send parsed data to the DB insertion.
     *
     * @param w list of word deltas
     * @param b list of bigram deltas
     * @param processedBytes number of bytes processed so far
     * @param end indicates if this is the final batch
     */
    public Batch(List<WordDelta> w, List<BigramDelta> b, long processedBytes, boolean end) {
        this.wordDeltas = w;
        this.bigramDeltas = b;
        this.processedBytes = processedBytes;
        this.end = end;
    }

    /**
     * Creates a final batch marker indicating no more data remains.
     *
     * @param processedBytes total bytes processed
     * @return an empty, end-of-stream batch
     */
    public static Batch end(long processedBytes) {
        return new Batch(List.of(), List.of(), processedBytes, true);
    }
}