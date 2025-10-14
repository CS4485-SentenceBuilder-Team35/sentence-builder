package org.utdteamthreefive.backend;

import org.utdteamthreefive.backend.models.Batch;
import org.utdteamthreefive.backend.service.Parser;
import org.utdteamthreefive.backend.service.DBInserter;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Temporary test runner to validate backend DB integration without UI.
 */
public class TestRun {
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Starting backend parser + inserter test...");

        Path path = Path.of("src/main/resources/test_input.txt"); // your input file
        BlockingQueue<Batch> queue = new ArrayBlockingQueue<>(5);

        Parser parser = new Parser(path, queue, 500, 50);
        DBInserter inserter = new DBInserter(path, queue);

        Thread parserThread = new Thread(parser);
        Thread inserterThread = new Thread(inserter);

        parserThread.start();
        inserterThread.start();

        parserThread.join();
        inserterThread.join();

        System.out.println("✅ Test completed — check MySQL (SentenceBuilder DB)!");
    }
}
