package org.utdteamthreefive.backend.util;

import javafx.concurrent.*;
import org.utdteamthreefive.backend.service.BackendService;
import org.utdteamthreefive.ui.Table;

public class FileParseHandle {
    private static Table table;

    public static void setTableInstance(Table _table) {
        table = _table;
    }

    public static void ParseFile(String path) {
        Task<Void> fileParseTask = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                BackendService.processFile(path);
                // if (table != null)
                table.syncTableWithDatabase();
                return null;
            }
        };

        Thread fileParseThread = new Thread(fileParseTask);
        fileParseThread.start();
    }
}
