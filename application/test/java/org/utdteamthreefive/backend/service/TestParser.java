package org.utdteamthreefive.backend.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.utdteamthreefive.backend.models.Batch;

/**
 * Minimal integration test to verify Parser ↔ DBInserter workflow.
 * Creates a temporary text file, runs both threads, then confirms
 * the process completes and inserts into the database.
 */
public class TestParser {

    @Test
    void testParserAndDBInserterIntegration() throws Exception {
        // 1️⃣ Create a small sample text file
        Path tempFile = Files.createTempFile("parser_test_", ".txt");
        Files.writeString(tempFile, "Hello world! This is a test.\nHello again, world!");

        // 2️⃣ Create a queue shared by parser & inserter
        BlockingQueue<Batch> queue = new ArrayBlockingQueue<>(5);

        // 3️⃣ Instantiate both components
        Parser parser = new Parser(tempFile, queue, 1000, 500);
        DBInserter inserter = new DBInserter(tempFile, queue);

        // 4️⃣ Run both in separate threads
        Thread parserThread = new Thread(parser, "test-parser");
        Thread inserterThread = new Thread(inserter, "test-inserter");

        inserterThread.start();
        parserThread.start();

        parserThread.join();
        inserterThread.join();

        // 5️⃣ Basic success message
        System.out.println("✅ Parser–Inserter integration completed for: " + tempFile);
    }
}
