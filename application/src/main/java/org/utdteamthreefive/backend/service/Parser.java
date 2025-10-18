package org.utdteamthreefive.backend.service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;
import java.util.logging.Logger;

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

    private static final Pattern WORD_WITH_OPTIONAL_COMMA = Pattern.compile("\\w+,?");
    private static final Pattern ENDS_SENT = Pattern.compile(".*[.!?][\"')\\]]*$");

    private final Path path;
    private final BlockingQueue<Batch> batchQueue;
    private final int flushEveryTokens;
    private final int flushUniqueThreshold;

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
    public Parser(Path path, BlockingQueue<Batch> queue, int flushEveryTokens, int flushUniqueThreshold) {
        this.path = path;
        this.batchQueue = queue;
        this.flushEveryTokens = flushEveryTokens;
        this.flushUniqueThreshold = flushUniqueThreshold;
    }

    /**
     * Each token is classified as a word.
     * Word stats and bigram counter are tracked and periodically flushed
     * into {@link Batch} for consumer to process.
     */
    @Override
    public void run() {
        logger.info("📝 Parsing Text File: " + path);

        long processed = 0L, seenTokens = 0L;
        String prevWord = null, lastWord = null;
        boolean atSentenceStart = true;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                processed += line.getBytes(StandardCharsets.UTF_8).length + 1;
                String[] tokens = line.split("\\s+");
                for (String token : tokens) {
                    if (token.isEmpty())
                        continue;
                    
                    token = token.toLowerCase(Locale.ROOT);

                    String type = classifyType(token);
                    if ("alpha".equals(type)) {
                        seenTokens++;
                        recordToken(token, type, 1, atSentenceStart ? 1 : 0, 0);
                        if (prevWord != null)
                            bigramCounts.merge(new Batch.BigramKey(prevWord, token), 1, Integer::sum);
                        prevWord = token;
                        lastWord = token;
                        atSentenceStart = false;
                    }

                    if (ENDS_SENT.matcher(token).matches()) {
                        if (lastWord != null)
                            wordCounts.get(lastWord).end += 1;
                        prevWord = null;
                        lastWord = null;
                        atSentenceStart = true;
                    }

                    // Flush when:
                    // - processed tokens reach the flush interval
                    // - OR unique word count exceeds threshold
                    // - OR unique bigram count exceeds threshold
                    if (seenTokens % flushEveryTokens == 0 || wordCounts.size() > flushUniqueThreshold
                            || bigramCounts.size() > flushUniqueThreshold) {
                        flush(processed, false);
                    }
                }
            }
            flush(processed, false);
            batchQueue.put(Batch.end(processed));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                batchQueue.put(Batch.end(processed));
            } catch (InterruptedException ignored) {
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
        if (WORD_WITH_OPTIONAL_COMMA.matcher(token).matches())
            return "alpha";
        return "misc";
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

        logger.info("🚚 Sending batch with " + wordCounts.size() + " words and " + bigramCounts.size() + " bigrams");
        if (wordCounts.isEmpty() && bigramCounts.isEmpty() && !end)
            return;

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

        batchQueue.put(new Batch(words, bigrams, processed, end));
        wordCounts.clear();
        bigramCounts.clear();
    }
}
