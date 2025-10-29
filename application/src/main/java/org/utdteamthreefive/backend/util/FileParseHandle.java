package org.utdteamthreefive.backend.util;

import javafx.application.Platform;
import javafx.concurrent.*;
import org.utdteamthreefive.backend.service.BackendService;
import org.utdteamthreefive.ui.Table;

/**
 * @author Aiden Martinez
 */
public class FileParseHandle {
    public static void ParseFile(String path, Table table) {
        Task<Void> fileParseTask = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                BackendService.processFile(path);
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
                // TODO Auto-generated method stub
                super.done();

                Platform.runLater(() -> {
                    table.syncTableWithDatabase();
                    System.out.println("Table update completed.");
                });
            }
        };

        Thread fileParseThread = new Thread(fileParseTask);
        fileParseThread.start();

    }
}
