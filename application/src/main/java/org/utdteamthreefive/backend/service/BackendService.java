package org.utdteamthreefive.backend.service;

import java.nio.file.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.models.Batch;

/**
 * PRODUCER - CONSUMER pattern with a BlockingQueue between Parser and DBInserter.
 * Provides backend processing functionality for handling text files.
 * This service coordinates the parsing of large text files and the writing of
 * processed data to a Database. It initializes worker threads for both parsing 
 * and writing, enabling asynchronous data handling.
 *
 * PRODUCER: Parser reads and processes the text file in 'batches' and puts
 * these batches into a BlockingQueue.
 * 
 * CONSUMER: DBInserter takes these batches and writes them to the database.
 * 
 * @author Zaeem Rashid
 */
public class BackendService {

    private static final Logger logger = Logger.getLogger(BackendService.class.getName());

    // call this from JavaFX controller (no progress bar)
    public static void processFile(String filePath) {

        logger.info("✅ Starting processing for file: " + filePath);

        try {
            Path path = Paths.get(filePath);
            BlockingQueue<Batch> queue = new ArrayBlockingQueue<>(5); // bounded = back-pressure

            Parser parser = new Parser(path, queue, 10_000, 5_000); // tune thresholds
            DBInserter dbWriter = new DBInserter(path, queue);

            Thread parserThread = new Thread(parser, "parser-producer");
            Thread writerThread = new Thread(dbWriter, "db-consumer");

            writerThread.start();
            parserThread.start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
