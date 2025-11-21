package org.utdteamthreefive.backend.service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.function.DoubleConsumer;

import org.utdteamthreefive.backend.models.Batch;

/**
 * The {@code Parser} is a producer class is responsible for reading and analyzing a text file,
 * breaking it down into words and bigrams, classifying each token, and batching
 * the results for further processing by a consumer.
 *
 * It operates as a producer in a producer-consumer setup, reading the file line by line,
 * tracking word occurrences, sentence boundaries, and generating batches of parsed data
 * that are placed into a shared {@link BlockingQueue}.
 *
 * This class implements {@link Runnable} so it can be executed on a separate thread
 * to enable concurrent file parsing.
 *
 * @author Zaeem Rashid
 */
public class Parser implements Runnable {
    private static final Logger logger = Logger.getLogger(Parser.class.getName());
    private static final Pattern WORD_WITH_PUNCTUATION_PATTERN = Pattern.compile("\\p{L}+[,.!?]?[\"')\\]]*", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern END_OF_SENTENCE_PATTERN = Pattern.compile(".*[.!?][\"')\\]]*$");

    private final Path path;
    private final BlockingQueue<Batch> batchQueue;

    private final DoubleConsumer progressCallback;
    
    /**
     * Internal helper class for tracking counts and classification type
     * for individual tokens.
     * Keeping stats together for each word.
     */
    private static final class Counts {
        int total, begin, end;
        String type;
    }

    private final Map<String, Counts> wordCounts = new HashMap<>();
    private final Map<Batch.BigramKey, Integer> bigramCounts = new HashMap<>();


    /**
     * Constructs a parser for a given text file and output queue.
     *
     * @param path the path to the text file to be parsed
     * @param queue the shared queue between producer and consumer used to store generated {@link Batch}
     * @param flushEveryTokens the maximum number of tokens to process before flushing a batch
     * @param flushUniqueThreshold the maximum number of unique tokens or bigrams before flushing
     */
    public Parser(Path path, BlockingQueue<Batch> queue, DoubleConsumer progressCallback) throws Exception {
        this.path = path;
        this.batchQueue = queue;
        this.progressCallback = progressCallback;
    }

    /**
     * Each token is classified as a word.
     * Word stats and bigram counter are tracked and periodically flushed
     * into {@link Batch} for consumer to process.
     */
    @Override
    public void run() {
        logger.info("Starting chunk parsing for: " + path.toString());

        long processed = 0L;
        String prevWord = null;
        boolean atSentenceStart = true;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            // Get file size once at the beginning for progress calculation
            final long fileSize = Files.size(path);

            // Read each line from the chunk file
            String line;
            while ((line = reader.readLine()) != null) {
                processed += line.getBytes(StandardCharsets.UTF_8).length + 2; // +2 for \r\n

                // Only update the progress every 8KB to reduce overhead on UI thread
                if (processed % 8192 == 0 || processed >= fileSize) { // Every 8KB or at end
                    final double progressValue = Math.min(1.0, (double) processed / fileSize);
                    progressCallback.accept(progressValue);
                }

                String[] tokens = line.split("\\s+");
                for (String rawToken : tokens) {
                    if (rawToken.isEmpty()) 
                        continue;

                    if (NUMBER_PATTERN.matcher(rawToken).matches()) 
                        continue;

                    // Preserve raw token for sentence-end detection, then normalize
                    boolean isEndOfSentence = END_OF_SENTENCE_PATTERN.matcher(rawToken).matches();
                    String token = normalizeToken(rawToken);
                    if (token == null || token.isEmpty()) {
                        if (isEndOfSentence) {
                            prevWord = null;
                            atSentenceStart = true;
                        }
                        continue;
                    }

                    String type = classifyType(token);
                    if (type.equals("alpha")) {
                        // add end count if token ends the sentence, add begin count if at sentence start
                        recordToken(token, type, 1, atSentenceStart ? 1 : 0, isEndOfSentence ? 1 : 0);

                        // Add bigram if previous word exists
                        if (prevWord != null) {
                            bigramCounts.merge(new Batch.BigramKey(prevWord, token), 1, Integer::sum);
                        }

                        prevWord = token;
                        atSentenceStart = false;
                    }

                    if (isEndOfSentence) {
                        prevWord = null;
                        atSentenceStart = true;
                    }
                }
            }

            // Flush after finishing the chunk
            flush(processed, false);

            // Final progress update to ensure 100%
            progressCallback.accept(1.0);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Send one final batch with any remaining data and mark it as the end
                flush(processed, true);
                logger.info("Completed chunk parsing for: " + path.toString());
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }


    /**
     * Determines the classification type of a token alpha or misc.
     *
     * @param token the token to classify
     * @return a string label representing the token type
     */
    private static String classifyType(String token) {
        if (WORD_WITH_PUNCTUATION_PATTERN.matcher(token).matches())
            return "alpha";
        return "misc";
    }

    /**
     * Normalize a raw token by lower-casing and stripping leading/trailing punctuation
     * and surrounding quotes/brackets. Returns null or empty string when nothing
     * remains after normalization.
     */
    private static String normalizeToken(String raw) {
        if (raw == null) return null;
        String s = raw.toLowerCase(Locale.ROOT).strip();
        // strip leading non-alphanumeric (quotes, brackets, etc.)
        s = s.replaceAll("^[^\\p{Alnum}]+", "");
        // strip trailing non-alphanumeric (punctuation, quotes, brackets, etc.)
        s = s.replaceAll("[^\\p{Alnum}]+$", "");
        return s;
    }

    /**
     * Updates count statistics for the given token.
     *
     * @param token the word or symbol being recorded
     * @param type the token type classification
     * @param addTotal increment to total count
     * @param addBegin increment to sentence start count
     * @param addEnd increment to sentence end count
     */
    private void recordToken(String token, String type, int addTotal, int addBegin, int addEnd) {
        Counts stats = wordCounts.get(token);
        if (stats == null) {
            stats = new Counts();
            stats.type = type;
            wordCounts.put(token, stats);
        }
        stats.total += addTotal;
        stats.begin += addBegin;
        stats.end += addEnd;
    }


    /**
     * Flushes the word and bigram counts into a {@link Batch}
     * and places it in the output queue {@link BlockingQueue}.
     *
     * @param processed the number of bytes processed so far
     * @param end whether this is the final batch
     * @throws InterruptedException if the queue operation is interrupted
     */
    private void flush(long processed, boolean end) throws InterruptedException {
        if (wordCounts.isEmpty() && bigramCounts.isEmpty() && !end) return;

        logger.info("🚚 Sending batch with " + wordCounts.size() + " words and " + bigramCounts.size() + " bigrams");

        var words = new ArrayList<Batch.WordDelta>(wordCounts.size());
        for (var entry : wordCounts.entrySet()) {
            String token = entry.getKey();
            Counts stats = entry.getValue();
            words.add(new Batch.WordDelta(token, stats.total, stats.begin, stats.end, stats.type));
        }
        var bigrams = new ArrayList<Batch.BigramDelta>(bigramCounts.size());
        for (var entry : bigramCounts.entrySet()) {
            Batch.BigramKey key = entry.getKey();
            int count = entry.getValue();
            bigrams.add(new Batch.BigramDelta(key, count));
        }

        batchQueue.put(new Batch(words, bigrams, processed, end, path));
        wordCounts.clear();
        bigramCounts.clear();
    }
}
