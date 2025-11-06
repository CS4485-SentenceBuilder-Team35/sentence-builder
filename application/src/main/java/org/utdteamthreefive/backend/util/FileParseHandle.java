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
import org.utdteamthreefive.ui.FileTab;

/**
 * @author Aiden Martinez
 */
public class FileParseHandle {
    private static final Logger logger = Logger.getLogger(FileParseHandle.class.getName());
    private static final int maxThreads = Math.max(8, Runtime.getRuntime().availableProcessors()); // max threads based on CPU cores
    // Shared thread pool for chunk processing to prevent thread explosion
    private static final ThreadPoolExecutor chunkProcessorPool = new ThreadPoolExecutor(
        4,
        maxThreads,
        60L, 
        java.util.concurrent.TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ChunkProcessor-" + count++);
                t.setDaemon(true);
                return t;
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * Called from the UI thread to start parsing multiple files
     * @param files
     * @param table
     * @param fileTabMap
     * @author Rommel Isaac Baldivas
     */
    @SuppressWarnings("resource") // Executor is properly shutdown in background thread
    public static void ParseFiles(List<File> files, Table table, HashMap<File, FileTab> fileTabMap) {
        // The executor limits the number of concurrent file parsing tasks to prevent us from using too much resources on different laptops
        ThreadPoolExecutor filePoolExecutor = new ThreadPoolExecutor(
                8, 
                16, // Does not compute available processors to allow more for chunks
                60, 
                java.util.concurrent.TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
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

        // For each file, submit a task to the executor
        for (File file : files) {
            // Find the file tab
            FileTab fileTab = fileTabMap.get(file);

            if (fileTab != null) {
                // Make sure the progress bar is blue if re-uploaded
                fileTab.startStyle();

                FileParseTask task = new FileParseTask(file, table, fileTab, chunkProcessorPool);
                
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
        
        // Wait for tasks to complete in a background thread to avoid blocking the UI thread
        new Thread(() -> {
            try {
                if (!filePoolExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.MINUTES)) {
                    logger.warning("Executor did not terminate within 5 minutes, forcing shutdown");
                    filePoolExecutor.shutdownNow();
                }
                logger.info("All file parsing tasks completed successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                filePoolExecutor.shutdownNow();
                logger.warning("File parsing was interrupted");
            }
        }, "FileParseCleanup").start();
    }

    /**
     * Gracefully shutdown the shared chunk processor pool.
     * Call this when the application is shutting down.
     */
    public static void shutdown() {
        logger.info("Shutting down chunk processor pool");
        chunkProcessorPool.shutdown();
        try {
            if (!chunkProcessorPool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warning("Chunk processor pool did not terminate gracefully, forcing shutdown");
                chunkProcessorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            chunkProcessorPool.shutdownNow();
        }
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
}
