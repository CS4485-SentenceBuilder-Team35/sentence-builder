package org.utdteamthreefive.backend.service;

import java.nio.file.Path;


import java.util.concurrent.BlockingQueue;
import org.utdteamthreefive.backend.models.Batch;

public class DBInserter implements Runnable{

    private final Path path;
    private final BlockingQueue<Batch> batchQueue;

    public DBInserter(Path path, BlockingQueue<Batch> queue) {
        this.path = path;
        this.batchQueue = queue;
    }

    @Override
    public void run() {
        // TODO

    }
}

// DBInserter – TODO (short)
// 1) Open DB connection via DatabaseManager.open() ONLY.
// 2) Insert filepath into files (or get existing) and save the generated
// file_id.
// 3) For each Batch:
// - Insert/UPSERT into file_word using saved file_id.
// - Insert/UPSERT into file_word_follow using saved file_id.
// 4) After END batch:
// - SELECT SUM(total_count) from file_word for this file_id (words and/or all
// types).
// - UPDATE files with those totals.
// 5) Keep each step as its own function:
// - ensureFileId(), insertWordDeltas(), insertBigramDeltas(),
// updateFileTotals().
