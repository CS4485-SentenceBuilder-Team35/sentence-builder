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

                // Retry loop for transient errors
                for (int attempt = 0;; attempt++) {
                    try {
                        insertOrUpdateWords(batch);
                        insertOrUpdateBigrams(batch);
                        conn.commit(); // Commit the transaction after successful batch processing
                        if (batch.end) {
                            updateFileTotals();
                            conn.commit(); // Final commit for file totals
                            logger.info("✅ DatabaseInserter received end marker, finishing for file: " + filePath.toString());
                            return; // exit run method
                        }
                        break; // success, exit retry loop
                    } catch (SQLException e) {
                        try {
                            conn.rollback();
                        } catch (SQLException rollbackEx) {
                            logger.severe("❌ Failed to rollback transaction: " + rollbackEx.getMessage());
                        }

                        if (isTransient(e) && attempt < 2) {
                            long sleep = backoff(attempt);
                            logger.warning("🔁 Deadlock/timeout, retrying batch in " + sleep + "ms: " + e.getMessage());
                            Thread.sleep(sleep);
                            continue;
                        }

                        logger.severe("❌ Fatal SQL after retries in " + filePath.toString() + ": " + e.getMessage());
                        throw e;
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
            for (WordDelta wd : batch.wordDeltas) {
                stmt.setString(1, wd.token());
                stmt.setInt(2, wd.total());
                stmt.setInt(3, wd.begin());
                stmt.setInt(4, wd.end());

                // 🧠 Normalize type to match ENUM('alpha', 'misc')
                String type = wd.type().toLowerCase();
                if (!type.equals("alpha")) {
                    type = "misc";
                }
                stmt.setString(5, type);

                batchTotal += wd.total();

                stmt.addBatch();
            }
            stmt.executeBatch();
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

        // 3) Batch insert/merge
        String upsert = """
            INSERT INTO word_follow (from_word_id, to_word_id, total_count)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE total_count = total_count + VALUES(total_count)
            """;

        try (PreparedStatement ps = conn.prepareStatement(upsert)) {
            int batchSize = 0;
            for (BigramDelta bd : batch.bigramDeltas) {
                Long fromId = idMap.get(bd.key().first());
                Long toId   = idMap.get(bd.key().second());
                if (fromId == null || toId == null) continue; // should be rare if words inserted first

                ps.setLong(1, fromId);
                ps.setLong(2, toId);
                ps.setInt(3, bd.count());
                ps.addBatch();

                if (++batchSize % 1000 == 0) ps.executeBatch(); // reasonable chunk
            }
            ps.executeBatch();
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
