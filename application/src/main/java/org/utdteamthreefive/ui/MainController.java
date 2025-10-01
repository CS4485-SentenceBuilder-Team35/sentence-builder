package org.utdteamthreefive.ui;

import org.utdteamthreefive.backend.SampleClass;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        SampleClass sampleClass = new SampleClass();
        welcomeText.setText(sampleClass.getGreeting());
    }
}