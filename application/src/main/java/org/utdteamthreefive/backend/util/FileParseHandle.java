package org.utdteamthreefive.backend;

import javafx.concurrent.*;
import org.utdteamthreefive.backend.service.BackendService;

public class FileParseHandle
{
    public static void ParseFile(String path)
    {
        Task<Void> fileParseTask = new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    BackendService.processFile(path);
                    return null;
                }
            };
        
        Thread fileParseThread = new Thread(fileParseTask);
        fileParseThread.start();
    }

    
}
