package org.utdteamthreefive.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.utdteamthreefive.backend.util.FileParseHandle;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("tab-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called automatically when the JavaFX application is shutting down.
     * This is the perfect place to clean up resources like thread pools.
     */
    @Override
    public void stop() throws Exception {
        System.out.println("Application shutting down - cleaning up thread pools...");
        FileParseHandle.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}