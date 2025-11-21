package org.utdteamthreefive.backend.service;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.models.Batch;
import org.utdteamthreefive.backend.models.Batch.BigramDelta;
import org.utdteamthreefive.backend.models.Batch.WordDelta;
import org.utdteamthreefive.backend.util.DatabaseManager;
import org.utdteamthreefive.ui.Table;

/**
 * DBInserter consumes parsed batches from a queue and inserts
 * their contents into the database.
 *
 * Updated for the new global SentenceBuilder schema:
 * - WORD and WORD_FOLLOW are global (no per-file tables)
 * - FILES still logs file metadata (path, word count, date)
 * @author Aisha Qureshi, Zaeem Rashid, and Rommel Isaac Baldivas
 */
public class DBInserter implements Runnable {
    private static final Logger logger = Logger.getLogger(DBInserter.class.getName());
    private Table table;
    private final BlockingQueue<Batch> batchQueue;
    private Connection conn;
    private final Map<Path, Long> fileIds = new HashMap<>();
    private final Map<Path, Long> wordsPerFile = new HashMap<>();
    private int batchesProcessed = 0;
    private int endMarkersProcessed = 0;

    public DBInserter(BlockingQueue<Batch> queue, Table table) {
        this.batchQueue = queue;
        this.table = table;
    }

    /**
     * @author Aisha Qureshi and Rommel Isaac Baldivas
     */
    @Override
    public void run() {
        try {
            conn = DatabaseManager.open();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // Continuously consume batches from the queue and insert/update DB
            logger.info("DatabaseInserter started and waiting for batches...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Batch batch = batchQueue.take();

                    // Skip batches with null file path
                    if (batch.filePath == null && !batch.end) {
                        logger.warning("Skipping batch with null file path");
                        continue;
                    }

                    // Initialize file processing if not already done (for both regular and end batches)
                    if (batch.filePath != null && !fileIds.containsKey(batch.filePath)) {
                        Long fileId = ensureFileId(batch.filePath);
                        if (fileId == null) {
                            logger.severe("Could not create file record for " + batch.filePath.toString());
                            continue; // Skip this batch but continue processing others
                        }
                        fileIds.put(batch.filePath, fileId);
                        wordsPerFile.put(batch.filePath, 0L);
                        logger.info("DatabaseInserter initialized for file: " + batch.filePath.toString());
                    }

                    // Handle regular batches
                    if (batch.filePath != null && !batch.end) {
                        batchesProcessed++;
                        // Process the batch with retry logic
                        boolean success = processBatchWithRetry(batch);
                        if (!success) {
                            logger.severe("Failed to process batch after all retries for file: " + batch.filePath.toString());
                            // will continue processing other files even if current file fails
                        }
                    }

                    // Handle end markers
                    if (batch.end && batch.filePath != null) {
                        endMarkersProcessed++;
                        logger.info("🏁 Processing end marker for file: " + batch.filePath.getFileName() + " (end marker " + endMarkersProcessed + " of " + fileIds.size() + " files)");
                        // Update file totals with retry logic for this specific file
                        if (updateFileTotalsWithRetry(batch.filePath)) {
                            logger.info("DatabaseInserter completed file: " + batch.filePath.toString() + " with " + wordsPerFile.get(batch.filePath) + " words");
                        }
                        else {
                            logger.severe("Failed to update file totals after all retries for file: " + batch.filePath.toString());
                        }

                        logger.info("DatabaseInserter progress: " + endMarkersProcessed + "/" + fileIds.size() + " files completed");

                        // Finally update the TableView
                        table.syncTableWithDatabase();
                    }
                } catch (InterruptedException e) {
                    // Interrupted - time to shutdown
                    Thread.currentThread().interrupt();
                    logger.info("DatabaseInserter was interrupted, shutting down...");
                    break;
                }
            }

        } catch (Exception e) {
            logger.severe("❌ DatabaseInserter failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup resources
            if (conn != null) {
                DatabaseManager.close(conn);
                logger.info("DatabaseInserter completed processing " + fileIds.size() + " files" + " (processed " + batchesProcessed + " batches, " + endMarkersProcessed + " end markers)");
            }
        }
    }

    ////#region HELPER METHODS
    
    /**
     * Process a batch with proper retry logic for deadlock handling.
     * @author Rommel Isaac Baldivas
     */
    private boolean processBatchWithRetry(Batch batch) {
        final int MAX_RETRIES = 5; // Increased for deadlock scenarios
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                insertOrUpdateWords(batch);
                insertOrUpdateBigrams(batch);
                conn.commit(); // Commit the transaction after successful batch processing
                return true; // Success
                
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.severe("Failed to rollback transaction: " + rollbackEx.getMessage());
                }

                if (isTransient(e) && attempt < MAX_RETRIES - 1) {
                    long sleep = backoff(attempt);
                    logger.warning("Deadlock/timeout detected (attempt " + (attempt + 1) + "/" + MAX_RETRIES + 
                                 "), retrying batch in " + sleep + "ms: " + e.getMessage());
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.severe("Thread interrupted during retry backoff");
                        return false;
                    }
                    
                    // Add jitter to reduce collision probability
                    try {
                        Thread.sleep((long)(Math.random() * 100));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    continue;
                }

                logger.severe("Fatal SQL error after " + MAX_RETRIES + " retries in " + 
                             (batch.filePath != null ? batch.filePath.toString() : "unknown file") + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                logger.severe("Unexpected error processing batch: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * Update file totals with retry logic for a specific file.
     * @author Rommel Isaac Baldivas
     */
    private boolean updateFileTotalsWithRetry(Path filePath) {
        final int MAX_RETRIES = 3;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                updateFileTotals(filePath);
                conn.commit(); // Final commit for file totals
                return true;

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.severe("Failed to rollback file totals transaction: " + rollbackEx.getMessage());
                }

                if (isTransient(e) && attempt < MAX_RETRIES - 1) {
                    long sleep = backoff(attempt);
                    logger.warning("Retrying file totals update in " + sleep + "ms: " + e.getMessage());
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    continue;
                }

                logger.severe("Failed to update file totals after " + MAX_RETRIES + " retries: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * 
     * @param e
     * @return
     * @author Zaeem Rashid
     */
    private static boolean isTransient(SQLException e) {
        int code = e.getErrorCode(); // MySQL vendor code
        return code == 1213 || code == 1205 || e instanceof SQLTransientException;
    }

    /**
     * 
     * @param attempt
     * @return
     * @author Zaeem Rashid
     */
    private static long backoff(int attempt) {
        return Math.min(2000L, (long) (50 * Math.pow(2, attempt)));
    }
    /**
     * Ensure file record exists, or insert a new one.
     */
    private Long ensureFileId(Path filePath) throws SQLException {
        String selectSql = "SELECT file_id FROM files WHERE file_path = ?";
        String insertSql = "INSERT INTO files (file_path, word_count, date_imported) VALUES (?, 0, CURDATE())";

        // Query for existing record
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, filePath.toString());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) return rs.getLong("file_id");
            }
        }

        // If it doesn't exist, insert new record
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, filePath.toString());
            insertStmt.executeUpdate();
            try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return null;
    }

    /**
     * Insert or update global WORD table.
     */
    private void insertOrUpdateWords(Batch batch) throws SQLException {
        if (batch.wordDeltas.isEmpty()) return;

        String upsertSql = """
                INSERT INTO word (word_token, total_count, start_count, end_count, type_)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    total_count = total_count + VALUES(total_count),
                    start_count = start_count + VALUES(start_count),
                    end_count = end_count + VALUES(end_count),
                    type_ = VALUES(type_)
                """;

        long batchTotal = 0L;

        try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
            // Sort word deltas by token to reduce deadlock probability
            List<WordDelta> sortedDeltas = new ArrayList<>(batch.wordDeltas);
            sortedDeltas.sort((a, b) -> a.token().compareTo(b.token()));

            int batchCount = 0;
            final int BATCH_SIZE = 500; // Smaller batches to reduce deadlock window

            for (WordDelta wd : sortedDeltas) {
                stmt.setString(1, wd.token());
                stmt.setInt(2, wd.total());
                stmt.setInt(3, wd.begin());
                stmt.setInt(4, wd.end());

                // Normalize type to match ENUM('alpha', 'misc')
                String type = wd.type().toLowerCase();
                if (!type.equals("alpha")) {
                    type = "misc";
                }
                stmt.setString(5, type);

                batchTotal += wd.total();
                stmt.addBatch();
                batchCount++;

                // Execute in smaller chunks to reduce deadlock window
                if (batchCount >= BATCH_SIZE) {
                    stmt.executeBatch();
                    batchCount = 0;
                }
            }

            // Execute remaining statements
            if (batchCount > 0) {
                stmt.executeBatch();
            }
        }

        // Update word count for this specific file
        if (batch.filePath != null) {
            wordsPerFile.merge(batch.filePath, batchTotal, Long::sum);
        }
    }
    
    private Map<String, Long> fetchWordIdsBulk(Connection conn, Set<String> tokens) throws SQLException {
        Map<String, Long> out = new HashMap<>(tokens.size() * 2);
        if (tokens.isEmpty()) return out;
        
        // MySQL IN list practical chunking (avoid huge packets)
        final int CHUNK = 1000;
        List<String> list = new ArrayList<>(tokens);
        for (int i = 0; i < list.size(); i += CHUNK) {
            int end = Math.min(i + CHUNK, list.size());
            List<String> sub = list.subList(i, end);
            
            String placeholders = String.join(",", Collections.nCopies(sub.size(), "?"));
            String sql = "SELECT word_id, word_token FROM word WHERE word_token IN (" + placeholders + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (String t : sub) ps.setString(idx++, t);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.put(rs.getString("word_token"), rs.getLong("word_id"));
                    }
                }
            }
        }
        return out;
    }
    
    
    /**
     * Insert or update global WORD_FOLLOW table.
     * @author Aisha Qureshi and Rommel Isaac Baldivas
     */
    private void insertOrUpdateBigrams(Batch batch) throws SQLException {
        if (batch.bigramDeltas.isEmpty()) return;

        // 1) Collect all unique tokens we need IDs for (from + to)
        Set<String> needed = new HashSet<>();
        for (BigramDelta bd : batch.bigramDeltas) {
            needed.add(bd.key().first());
            needed.add(bd.key().second());
        }

        // 2) Resolve IDs in bulk (words should already exist from insertOrUpdateWords in this same transaction)
        Map<String, Long> idMap = fetchWordIdsBulk(conn, needed);

        // 3) Sort bigrams by from_word_id, then to_word_id to reduce deadlock probability
        List<BigramDelta> sortedBigrams = new ArrayList<>(batch.bigramDeltas);
        sortedBigrams.sort((a, b) -> {
            Long aFromId = idMap.get(a.key().first());
            Long bFromId = idMap.get(b.key().first());
            if (aFromId == null && bFromId == null) return 0;
            if (aFromId == null) return 1;
            if (bFromId == null) return -1;

            int fromCompare = aFromId.compareTo(bFromId);
            if (fromCompare != 0) return fromCompare;

            Long aToId = idMap.get(a.key().second());
            Long bToId = idMap.get(b.key().second());
            if (aToId == null && bToId == null) return 0;
            if (aToId == null) return 1;
            if (bToId == null) return -1;

            return aToId.compareTo(bToId);
        });

        // 4) Batch insert/merge with smaller batch sizes
        String upsert = """
                INSERT INTO word_follow (from_word_id, to_word_id, total_count)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE total_count = total_count + VALUES(total_count)
                """;

        try (PreparedStatement ps = conn.prepareStatement(upsert)) {
            int batchSize = 0;
            final int MAX_BATCH_SIZE = 500; // Smaller batches to reduce deadlock window

            for (BigramDelta bd : sortedBigrams) {
                Long fromId = idMap.get(bd.key().first());
                Long toId = idMap.get(bd.key().second());
                if (fromId == null || toId == null) continue; // should be rare if words inserted first

                ps.setLong(1, fromId);
                ps.setLong(2, toId);
                ps.setInt(3, bd.count());
                ps.addBatch();
                batchSize++;

                // Execute in smaller chunks to reduce deadlock window
                if (batchSize >= MAX_BATCH_SIZE) {
                    ps.executeBatch();
                    batchSize = 0;
                }
            }

            // Execute remaining statements
            if (batchSize > 0) {
                ps.executeBatch();
            }
        }
    }

    
    /**
     * After all batches are processed, compute total words for the specific file.
     * @author Aisha Qureshi and Rommel Isaac Baldivas
    */
   private void updateFileTotals(Path filePath) throws SQLException {
       Long fileId = fileIds.get(filePath);
       Long wordCount = wordsPerFile.get(filePath);
       
       if (fileId == null || wordCount == null) {
           logger.warning("Cannot update totals for file: " + filePath + " - missing data");
           return;
        }
        
        String updateSql = "UPDATE files SET word_count = ? WHERE file_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setLong(1, wordCount);
            stmt.setLong(2, fileId);
            stmt.executeUpdate();
        }
        
        logger.info("Updated totals for " + filePath.getFileName() + " → Words: " + wordCount);
    }
    ////#endregion
}
