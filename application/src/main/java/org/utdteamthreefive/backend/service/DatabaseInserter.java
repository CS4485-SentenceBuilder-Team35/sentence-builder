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

public class DatabaseInserter implements Runnable {
    private static final Logger logger = Logger.getLogger(DatabaseInserter.class.getName());
    private final BlockingQueue<Batch> batchQueue;
    private Connection conn;
    private Long fileId;
    private final Path filePath;
    private long wordsThisFile = 0L;

    public DatabaseInserter(Path filePath, BlockingQueue<Batch> queue) {
        this.filePath = filePath;
        this.batchQueue = queue;
    }

    @Override
    public void run() {
        try {
            conn = DatabaseManager.open();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            
            // Set lock wait timeout to fail fast on deadlocks
            try (PreparedStatement stmt = conn.prepareStatement("SET SESSION innodb_lock_wait_timeout = 5")) {
                stmt.execute();
            } catch (SQLException e) {
                logger.warning("Could not set lock wait timeout: " + e.getMessage());
            }

            // 1. Get file id from FILES table and create if not exists
            fileId = ensureFileId();
            if (fileId == null) {
                logger.severe("❌ Could not create file record for " + filePath.toString());
                return;
            }

            // 2. Consume batches from the queue and insert/update DB
            logger.info("DatabaseInserter started for file: " + filePath.toString());
            while(!Thread.currentThread().isInterrupted()) {
                Batch batch = batchQueue.take();

                // Process the batch with retry logic
                boolean success = processBatchWithRetry(batch);
                if (!success) {
                    logger.severe("❌ Failed to process batch after all retries for file: " + filePath.toString());
                    return;
                }
                
                if (batch.end) {
                    // Update file totals with retry logic
                    if (updateFileTotalsWithRetry()) {
                        logger.info("✅ DatabaseInserter received end marker, finishing for file: " + filePath.toString());
                        return; // exit run method
                    } else {
                        logger.severe("❌ Failed to update file totals after all retries for file: " + filePath.toString());
                        return;
                    }
                }
            }

        } catch (Exception e) {
            logger.severe("❌ DatabaseInserter failed for " + filePath.toString() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup resources
            if (conn != null) {
                try {
                    conn.close();
                    logger.info("✅ DatabaseInserter completed for file: " + filePath.toString());
                } catch (SQLException e) {
                    logger.severe("Failed to close database connection: " + e.getMessage());
                }
            }
        }
    }

    //#region HELPER METHODS
    
    /**
     * Process a batch with proper retry logic for deadlock handling.
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
                             filePath.toString() + ": " + e.getMessage());
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
     * Update file totals with retry logic.
     */
    private boolean updateFileTotalsWithRetry() {
        final int MAX_RETRIES = 3;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                updateFileTotals();
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

    private static boolean isTransient(SQLException e) {
        int code = e.getErrorCode(); // MySQL vendor code
        return code == 1213 || code == 1205 || e instanceof SQLTransientException;
    }

    private static long backoff(int attempt) {
        return Math.min(2000L, (long) (50 * Math.pow(2, attempt)));
    }

     /**
     * Ensure file record exists, or insert a new one.
     */
    private Long ensureFileId() throws SQLException {
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
        wordsThisFile += batchTotal;
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
                Long toId   = idMap.get(bd.key().second());
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
     * After all batches are processed, compute total words for the file.
     */
    private void updateFileTotals() throws SQLException {
        String updateSql = "UPDATE files SET word_count = ? WHERE file_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setLong(1, wordsThisFile);
            stmt.setLong(2, fileId);
            stmt.executeUpdate();
        }

        logger.info("📊 Updated totals → Words (this file): " + wordsThisFile);
    }
    //#endregion
}
