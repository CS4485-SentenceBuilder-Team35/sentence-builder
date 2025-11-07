package org.utdteamthreefive.backend.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.models.Batch;
import org.utdteamthreefive.backend.service.BackendService;
import org.utdteamthreefive.backend.service.ChunkParser;
import org.utdteamthreefive.ui.FileTab;
import org.utdteamthreefive.ui.Table;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Task for parsing a single file in chunks and updating progress.
 * 
 * @author Rommel Isaac Baldivas
 */
public class FileParseTask extends Task<Void> {
    private static final Logger logger = Logger.getLogger(FileParseTask.class.getName());
    private File file;
    private Table table;
    private FileTab fileTab;
    private List<File> chunkFiles;
    private LinkedBlockingQueue<Batch> chunkQueue = new LinkedBlockingQueue<>();
    private final int CHUNK_SIZE = 16 * 1024; // 16k tokens per chunk
    private ThreadPoolExecutor chunkProcessorPool;

    public FileParseTask(File file, Table table, FileTab fileTab, ThreadPoolExecutor chunkProcessorPool) {
        this.file = file;
        this.table = table;
        this.fileTab = fileTab;
        this.chunkProcessorPool = chunkProcessorPool;
        this.chunkFiles = new ArrayList<>();
    }

    @Override
    protected Void call() throws Exception {
        Path filePath = file.toPath();
        long fileSize = -1L;
        try {
            fileSize = Files.size(filePath);
        } catch (Exception e) {
            logger.severe("❌ Failed to get file size for progress tracking: " + e.getMessage());
            return null;
        }

        // Split file into chunks of tokens
        int bytesRead;
        int chunkIndex = 0;
        long totalProcessed = 0;

        // Update progress for file reading phase (0-25% of total progress)
        // Create chunks based on number of tokens so that words are not split in between chunks
        try (BufferedReader br = Files.newBufferedReader(filePath)){
            StringBuilder chunkBuilder = new StringBuilder();
            int tokenCount = 0;
            List<String> lines = br.readAllLines();
            for (String line : lines) {
                String[] tokens = line.split("\\s+");
                
                // Process each token in the line
                for (String token : tokens) {
                    if (token.trim().isEmpty()) continue; // Skip empty tokens
                    tokenCount++;
                }
                
                // Add the complete line to the current chunk
                chunkBuilder.append(line).append(System.lineSeparator());
                
                // Check if we should create a new chunk after adding this complete line
                if (tokenCount >= CHUNK_SIZE) {
                    // Write current chunk to file
                    File chunkFile = new File(file.getParent(), file.getName() + ".part" + chunkIndex++);
                    try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                        fos.write(chunkBuilder.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    chunkFiles.add(chunkFile);

                    // Reset for next chunk
                    chunkBuilder.setLength(0);
                    tokenCount = 0;

                    // Update progress as we read the file (0-25% range)
                    totalProcessed += chunkFile.length();
                    if (fileSize > 0) {
                        double readProgress = (double) totalProcessed / fileSize * 0.25; // First quarter of progress
                        updateProgress(readProgress, 1.0);
                    }
                }
            }

            // Write any remaining tokens as the last chunk
            if (chunkBuilder.length() > 0) {
                File chunkFile = new File(file.getParent(), file.getName() + ".part" + chunkIndex++);
                try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                    fos.write(chunkBuilder.toString().getBytes());
                }
                chunkFiles.add(chunkFile);

                // Update progress for the last chunk
                totalProcessed += chunkFile.length();
                if (fileSize > 0) {
                    double readProgress = (double) totalProcessed / fileSize * 0.25; // First quarter of progress
                    updateProgress(readProgress, 1.0);
                }
            }
        }

        // OLD CODE FOR BYTE-BASED CHUNKING
        // try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
        //     byte[] buffer = new byte[CHUNK_SIZE];
        //     while ((bytesRead = bis.read(buffer)) != -1) {
        //         File chunkFile = new File(file.getParent(), file.getName() + ".part" + chunkIndex++);
        //         try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
        //             fos.write(buffer, 0, bytesRead);
        //         }
        //         chunkFiles.add(chunkFile);

        //         // Update progress as we read the file (0-25% range)
        //         totalProcessed += bytesRead;
        //         if (fileSize > 0) {
        //             double readProgress = (double) totalProcessed / fileSize * 0.25; // First quarter of progress
        //             updateProgress(readProgress, 1.0);
        //         }
        //     }
        // }

        // Start DBInserter to consume from the shared chunk queue BEFORE submitting chunk tasks
        Thread dbInserterThread = BackendService.startDBInserter(filePath, chunkQueue);

        // Submit chunk processing tasks to the shared thread pool
        List<java.util.concurrent.Future<?>> chunkFutures = new ArrayList<>();

        // Multiple Chunks
        // for (int iFile = 0; iFile < chunkFiles.size(); iFile++) {
        //     File chunkFile = chunkFiles.get(iFile);

        //     Future<?> future = chunkProcessorPool.submit(() -> {
        //         ChunkParser chunkParser = new ChunkParser(chunkFile.toPath(), chunkQueue);
        //         chunkParser.run(); // Run synchronously in the thread pool thread
        //     });
        //     chunkFutures.add(future);
        // }

        // One Chunk 
        Future<?> future = chunkProcessorPool.submit(() -> {
            ChunkParser chunkParser = new ChunkParser(file.toPath(), chunkQueue);
            chunkParser.run();
        });
        chunkFutures.add(future);

        // Wait for all chunk processing futures to complete
        for (int iChunk = 0; iChunk < chunkFutures.size(); iChunk++) {
            try {
                chunkFutures.get(iChunk).get(); // Wait for completion

                // Update progress for chunk processing phase after chunk completes (25-75% range)
                if (chunkFiles.size() > 0) {
                    double processProgress = 0.25 + ((double) (iChunk + 1) / chunkFiles.size()) * 0.5; // Second part of progress
                    updateProgress(processProgress, 1.0);
                }
            } catch (Exception e) {
                logger.severe("❌ Chunk processing failed: " + e.getMessage());
                // Continue processing other chunks even if one fails
            }
        }

        // After all chunk parsers finish, send a single END marker to signal DatabaseInserter to finish
        try {
            chunkQueue.put(Batch.end(0)); // Signal end of all chunks for this file
        } catch (InterruptedException e) {
            logger.severe("❌ Failed to send end marker: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        // Now wait for DBInserter to finish processing all batches
        dbInserterThread.join();

        // Final progress update to 100%
        updateProgress(1.0, 1.0);
        return null;
    }

    /**
     * Done is called automatically when the call method is finished, so
     * we can do cleanup work and update the UI here
     */
    @Override
    protected void done() {
        super.done();

        // Clean up chunk files
        for (File chunkFile : chunkFiles) {
            if (chunkFile.exists()) {
                boolean deleted = chunkFile.delete();
                if (!deleted) {
                    logger.warning("Failed to delete chunk file: " + chunkFile.getAbsolutePath());
                }
            }
        }

        Platform.runLater(() -> {
            try {
                // Check if task completed successfully or with error
                if (getException() != null) {
                    logger.severe("File parsing failed: " + getException().getMessage());
                    fileTab.errorStyle();
                    return;
                }

                // Mark as completed
                fileTab.doneStyle();
                table.syncTableWithDatabase();
                logger.info("File parsing completed successfully for: " + file.getName());
            } catch (Exception e) {
                logger.severe("Error in done() callback: " + e.getMessage());
            }
        });
    }
}
