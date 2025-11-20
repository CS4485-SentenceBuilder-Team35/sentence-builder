package org.utdteamthreefive.backend.util;

import javafx.application.Platform;
import javafx.concurrent.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import org.utdteamthreefive.ui.Table;
import org.utdteamthreefive.backend.models.Batch;
import org.utdteamthreefive.backend.service.BackendService;
import org.utdteamthreefive.ui.FileTab;

/**
 * Utility class to handle parsing multiple files in the background
 * @author Aiden Martinez and Rommel Isaac Baldivas
 */
public class FileParseHandle {
    private static final Logger logger = Logger.getLogger(FileParseHandle.class.getName());
    private static LinkedBlockingQueue<Batch> batchQueue = new LinkedBlockingQueue<>();
    private static Thread dbInserterThread = null;

    /**
     * Called from the UI thread to start parsing multiple files
     * @param files
     * @param table
     * @param fileTabMap
     * @author Rommel Isaac Baldivas
     */
    @SuppressWarnings("resource") // Executor is properly shutdown in background thread
    public static void ParseFiles(List<File> files, Table table, HashMap<File, FileTab> fileTabMap) {
        // Start DatabaseInserter only if not already running
        if (dbInserterThread == null || !dbInserterThread.isAlive()) {
            dbInserterThread = BackendService.startDBInserter(batchQueue, table);
            logger.info("Started new DatabaseInserter thread: " + dbInserterThread.getName());
        } else {
            logger.info("Reusing existing DatabaseInserter thread: " + dbInserterThread.getName());
        }

        // The executor limits the number of concurrent file parsing tasks to prevent us from using too much resources on different laptops
        ThreadPoolExecutor filePoolExecutor = new ThreadPoolExecutor(
                8,
                Runtime.getRuntime().availableProcessors(), // get the most available threads on the system
                60,
                java.util.concurrent.TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                // This is just a way to name the threads for debugging
                new ThreadFactory() {
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "FileParseTask-" + count++);
                        t.setDaemon(true);
                        return t;
                    }
                }
        );

        // Set TableView status
        Platform.runLater(() -> {
            table.getTableStatusLabel().setText("Files are being processed...");
        });

        // For each file, submit a task to the executor
        for (File file : files) {
            // Find the file tab
            FileTab fileTab = fileTabMap.get(file);

            if (fileTab != null) {
                // Make sure the progress bar is blue if re-uploading
                fileTab.startStyle();

                FileParseTask task = new FileParseTask(file, fileTab, batchQueue);
                
                // Bind progress bar to task progress on JavaFX Application Thread
                Platform.runLater(() -> {
                    fileTab.getProgressBar().progressProperty().bind(task.progressProperty());
                });
                
                filePoolExecutor.submit(task);
            } else {
                logger.warning("FileTab not found for file: " + file.getName());
            }
        }
        
        // Shutdown file thread executor gracefully
        filePoolExecutor.shutdown();
        
        /**
         * Wait for tasks to complete in a background thread to avoid blocking the UI thread
         * This is a thread that continuously monitors the batchQueue
         * Once the batchQueue is empty and has not changed size for a certain period,
         * then it's assumed that the DatabaseInserter has finished processing all items and is shut down.
         */
        new Thread(() -> {
            try {
                // First wait for all file parsing tasks to complete
                logger.info("Waiting for all file parsing tasks to complete...");
                
                if (!filePoolExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.MINUTES)) {
                    logger.warning("File parsing tasks did not complete within 5 minutes, forcing shutdown");
                    filePoolExecutor.shutdownNow();
                }
                
                logger.info("All file parsing tasks completed. Waiting for DatabaseInserter to process remaining queue...");
                
                // Monitor queue size changes to provide progress updates
                int previousQueueSize = batchQueue.size();
                int stableCount = 0;
                final int requiredStableChecks = 5; // Queue must be stable for 5 checks to ensure it's truly empty
                final int checkInterval = 1000; // Check every second
                int totalWaitTime = 0;
                
                logger.info("Initial queue size: " + previousQueueSize);
                
                while (true) {
                    Thread.sleep(checkInterval);
                    totalWaitTime += checkInterval;
                    
                    int currentQueueSize = batchQueue.size();
                    
                    if (currentQueueSize == previousQueueSize) {
                        stableCount++;
                        if (currentQueueSize == 0 && stableCount >= requiredStableChecks) {
                            logger.info("Queue is empty and stable for " + (stableCount * checkInterval / 1000) + " seconds. DatabaseInserter processing completed.");
                            // Stop the DatabaseInserter thread
                            dbInserterThread.interrupt();   
                            Platform.runLater(() -> {
                                table.getTableStatusLabel().setText("All files have been processed.");
                            });
                            break;
                        }
                        if (currentQueueSize > 0 && stableCount >= requiredStableChecks * 3) {
                            logger.warning("Queue size (" + currentQueueSize + ") has been stable for " + 
                                         (stableCount * checkInterval / 1000) + " seconds. DatabaseInserter may be processing slowly.");
                            // Reset counter to continue waiting instead of timing out
                            stableCount = 0;
                        }
                    } else {
                        // Queue size changed, reset stability counter
                        stableCount = 0;
                        if (totalWaitTime % 5000 == 0) { // Log every 5 seconds when active
                            logger.info("Queue size: " + currentQueueSize + " (was " + previousQueueSize + "), still processing... (waited " + (totalWaitTime / 1000) + "s)");
                        }
                    }
                    
                    previousQueueSize = currentQueueSize;
                }
                
                logger.info("DatabaseInserter finished processing all files. Total wait time: " + (totalWaitTime / 1000) + " seconds");
                logger.info("All file parsing tasks completed successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                filePoolExecutor.shutdownNow();
                logger.warning("File parsing was interrupted");
            }
        }, "FileParseCleanup").start();
    }

    /**
     * 
     * @param file
     * @param table
     * @param fileTab
     * @author Aiden Martinez
     */
    public static void ParseFile(File file, Table table, FileTab fileTab) {

        Task<Void> fileParseTask = new Task<Void>() {
            List<File> chunkFiles = new ArrayList<>();
            Path filePath = file.toPath();
            long fileSize = -1L;
            {
                try {
                    fileSize = Files.size(filePath);
                } catch (Exception e) {
                    logger.severe("❌ Failed to get file size for progress tracking: " + e.getMessage());
                }
            }

            @Override
            protected Void call() throws Exception {
                // Split file into chunks
                int bytesRead;
                int chunkIndex = 0;
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[8192]; // 8KB buffer
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        File chunkFile = new File(file.getParent(), file.getName() + ".part" + chunkIndex++);
                        try (FileOutputStream fos = new FileOutputStream(chunkFile)){
                            fos.write(buffer, 0, bytesRead);
                        }
                        chunkFiles.add(chunkFile);
                    }
                }
                // Process the chunk (e.g., send to server)
                for (File chunkFile : chunkFiles) {
                    // BackendService.processFile(chunkFile.getAbsolutePath(), fileTab);
                    // Update progress
                    if (fileSize > 0) {
                        long processedSize = Files.size(chunkFile.toPath());
                        updateProgress(Math.min(processedSize, fileSize), fileSize);
                    }
                }
                return null;
            }

            /**
             * done is called when the call method is finished. This allows the Task thread
             * to update the UI thread safely.
             * 
             * @author Rommel Isaac Baldivas
             */
            @Override
            protected void done() {
                super.done();

                // Clean up chunk files
                for (File chunkFile : chunkFiles) {
                    if (chunkFile.exists()) {
                        chunkFile.delete();
                    }
                }

                Platform.runLater(() -> {
                    fileTab.doneStyle();
                    table.syncTableWithDatabase();
                    logger.info("Table update completed.");
                });
            }
        };

        fileTab.getProgressBar().progressProperty().bind(fileParseTask.progressProperty());
        Thread fileParseThread = new Thread(fileParseTask);
        fileParseThread.start();
    }

    /**
     * Gracefully shutdown the DatabaseInserter when the application is closing.
     * This method should only be called on application shutdown.
     */
    public static void shutdown() {
        if (dbInserterThread != null && dbInserterThread.isAlive()) {
            logger.info("Shutting down DatabaseInserter thread: " + dbInserterThread.getName());
            dbInserterThread.interrupt();
            try {
                // Give it up to 10 seconds to finish current work
                dbInserterThread.join(10000);
                if (dbInserterThread.isAlive()) {
                    logger.warning("DatabaseInserter did not shut down gracefully within 30 seconds");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Interrupted while waiting for DatabaseInserter to shut down");
            }
        }
    }
}
