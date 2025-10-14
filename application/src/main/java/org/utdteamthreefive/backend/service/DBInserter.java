package org.utdteamthreefive.backend.service;

import java.nio.file.Path;
import java.sql.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.models.Batch;
import org.utdteamthreefive.backend.models.Batch.WordDelta;
import org.utdteamthreefive.backend.models.Batch.BigramDelta;
import org.utdteamthreefive.backend.util.DatabaseManager;

/**
 * DBInserter consumes parsed batches from a queue and inserts
 * their contents into the database.
 *
 * Updated for the new global SentenceBuilder schema:
 * - WORD and WORD_FOLLOW are global (no per-file tables)
 * - FILES still logs file metadata (path, word count, date)
 * @Author Aisha Qureshi
 */
public class DBInserter implements Runnable {
    private static final Logger logger = Logger.getLogger(DBInserter.class.getName());

    private final Path path;
    private final BlockingQueue<Batch> batchQueue;
    private Connection conn;
    private Long fileId;

    public DBInserter(Path path, BlockingQueue<Batch> queue) {
        this.path = path;
        this.batchQueue = queue;
    }

    @Override
    public void run() {
        try {
            conn = DatabaseManager.open();
            if (conn == null) {
                logger.severe("❌ Failed to open database connection");
                return;
            }

            // Step 1: Ensure file record exists
            fileId = ensureFileId();
            if (fileId == null) {
                logger.severe("❌ Could not create file record for " + path);
                return;
            }

            logger.info("✅ DBInserter started for file: " + path);

            // Step 2: Consume batches until the END batch is received
            boolean running = true;
            while (running) {
                Batch batch = batchQueue.take(); // blocks until next batch

                logger.info("📦 Got batch with " + batch.wordDeltas.size() +
                        " words and " + batch.bigramDeltas.size() + " bigrams");

                insertOrUpdateWords(batch);
                insertOrUpdateBigrams(batch);

                if (batch.end) {
                    updateFileTotals();
                    logger.info("🏁 Completed all inserts for file: " + path);
                    running = false;
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("⚠️ Inserter interrupted: " + e.getMessage());
        } catch (SQLException e) {
            logger.severe("💥 SQL error: " + e.getMessage());
        } finally {
            DatabaseManager.close(conn);
        }
    }

    /**
     * Ensure file record exists, or insert a new one.
     */
    private Long ensureFileId() throws SQLException {
        String selectSql = "SELECT file_id FROM files WHERE file_path = ?";
        String insertSql = "INSERT INTO files (file_path, word_count, date_imported) VALUES (?, 0, CURDATE())";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, path.toString());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) return rs.getLong("file_id");
            }
        }

        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, path.toString());
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
            INSERT INTO word (word_token, total_count, start_count, endcount, type)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                total_count = total_count + VALUES(total_count),
                start_count = start_count + VALUES(start_count),
                endcount = endcount + VALUES(endcount)
            """;

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

                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Insert or update global WORD_FOLLOW table.
     */
    private void insertOrUpdateBigrams(Batch batch) throws SQLException {
        if (batch.bigramDeltas.isEmpty()) return;

        // We must map token strings to their IDs from WORD table
        String getWordIdSql = "SELECT word_id FROM word WHERE word_token = ?";
        String insertFollowSql = """
            INSERT INTO word_follow (from_word_id, to_word_id, total_count)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                total_count = total_count + VALUES(total_count)
            """;

        try (PreparedStatement findStmt = conn.prepareStatement(getWordIdSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertFollowSql)) {

            for (BigramDelta bd : batch.bigramDeltas) {
                // Look up from_word_id
                Long fromId = getWordId(findStmt, bd.key().first());
                Long toId = getWordId(findStmt, bd.key().second());
                if (fromId == null || toId == null) continue;

                insertStmt.setLong(1, fromId);
                insertStmt.setLong(2, toId);
                insertStmt.setInt(3, bd.count());
                insertStmt.addBatch();
            }

            insertStmt.executeBatch();
        }
    }

    private Long getWordId(PreparedStatement stmt, String token) throws SQLException {
        stmt.setString(1, token);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong("word_id");
        }
        return null;
    }

    /**
     * After all batches are processed, compute total words for the file.
     */
    private void updateFileTotals() throws SQLException {
        String countSql = "SELECT SUM(total_count) AS total_words FROM word";
        int totalWords = 0;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) totalWords = rs.getInt("total_words");
        }

        String updateSql = "UPDATE files SET word_count = ? WHERE file_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, totalWords);
            stmt.setLong(2, fileId);
            stmt.executeUpdate();
        }

        logger.info("📊 Updated totals → Words: " + totalWords);
    }
}
