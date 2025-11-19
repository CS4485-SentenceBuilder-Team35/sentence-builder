package org.utdteamthreefive.backend.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.models.Batch;
import org.utdteamthreefive.backend.service.ChunkParser;
import org.utdteamthreefive.ui.FileTab;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Task for parsing a single file in chunks and updating progress.
 * Tasks are used to run background operations that can update the UI safely.
 * 
 * @author Rommel Isaac Baldivas and Aiden Martinez
 */
public class FileParseTask extends Task<Void> {
    private static final Logger logger = Logger.getLogger(FileParseTask.class.getName());
    private File file;
    private FileTab fileTab;
    private List<File> chunkFiles;
    private LinkedBlockingQueue<Batch> chunkQueue;
    // private ThreadPoolExecutor chunkProcessorPool;

    public FileParseTask(File file, FileTab fileTab, LinkedBlockingQueue<Batch> chunkQueue) {
        this.file = file;
        this.fileTab = fileTab;
        // this.chunkProcessorPool = chunkProcessorPool;
        this.chunkFiles = new ArrayList<>();
        this.chunkQueue = chunkQueue;
    }

    @Override
    protected Void call() throws Exception {
        ChunkParser chunkParser = new ChunkParser(file.toPath(), chunkQueue, progress -> {
            // Ensure progress updates happen on JavaFX Application Thread
            Platform.runLater(() -> updateProgress(progress, 1.0));
        });
        chunkParser.run();

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
                logger.info("File parsing completed successfully for: " + file.getName());
            } catch (Exception e) {
                logger.severe("Error in done() callback: " + e.getMessage());
            }
        });
    }
}
