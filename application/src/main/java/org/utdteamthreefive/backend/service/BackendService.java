package org.utdteamthreefive.backend.service;

import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Observable;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.models.Batch;
import org.utdteamthreefive.backend.util.DatabaseManager;
import org.utdteamthreefive.ui.FileTab;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

public class BackendService {
    
    private static final Logger logger = Logger.getLogger(BackendService.class.getName());
    
    /**
     * PRODUCER - CONSUMER pattern with a BlockingQueue between Parser and
     * DBInserter.
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
    public static void processFile(String filePath, FileTab fileTab) {

        logger.info("✅ Starting processing for file: " + filePath);

        try {
            Path path = Paths.get(filePath);
            BlockingQueue<Batch> queue = new ArrayBlockingQueue<>(5); // bounded = back-pressure

            Parser parser = new Parser(path, queue, 10_000, 5_000, progress -> Platform.runLater(() -> fileTab.setProgress(progress))); // tune thresholds
            DBInserter dbWriter = new DBInserter(path, queue);

            Thread parserThread = new Thread(parser, "parser-producer");
            Thread writerThread = new Thread(dbWriter, "db-consumer");

            writerThread.start();
            parserThread.start();

            parserThread.join(); // Wait for parser to complete
            writerThread.join(); // Wait for writer to complete

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches the list of files previously processed and stored in the database.
     * @return An observable list of Nodes representing the file tabs for each stored file.
     * @author Rommel Isaac Baldivas
     */
    public static ObservableList<Node> getFilesFromDatabase() {
        final Logger logger = Logger.getLogger(BackendService.class.getName());
        Connection conn = DatabaseManager.open();
        ObservableList<Node> fileList = FXCollections.observableArrayList();

        String fileQuery = "SELECT file_id, file_path FROM FILES";

        try {
            PreparedStatement stmt = conn.prepareStatement(fileQuery);
            var rs = stmt.executeQuery();

            while (rs.next()) {
                String filePath = rs.getString("file_path");
                String fileName = Paths.get(filePath).getFileName().toString();
                FileTab fileTab = new FileTab(fileName);
                fileTab.setProgress(100.0); // Mark as completed since it's from the database
                fileList.add(fileTab);
            }

            logger.info("Fetched " + fileList.size() + " files from database.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            DatabaseManager.close(conn);
        }

        return fileList;
    }
}
