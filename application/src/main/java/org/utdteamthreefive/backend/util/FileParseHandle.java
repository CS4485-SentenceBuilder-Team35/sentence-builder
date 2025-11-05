package org.utdteamthreefive.backend.util;

import javafx.application.Platform;
import javafx.concurrent.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.service.BackendService;
import org.utdteamthreefive.ui.Table;
import org.utdteamthreefive.ui.FileTab;

/**
 * @author Aiden Martinez
 */
public class FileParseHandle {
    private static final Logger logger = Logger.getLogger(FileParseHandle.class.getName());
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
                // BackendService.processFile(path, fileTab);
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
                    BackendService.processFile(chunkFile.getAbsolutePath(), fileTab);
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
                    fileTab.done();
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
