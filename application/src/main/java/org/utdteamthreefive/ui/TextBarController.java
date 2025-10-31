package org.utdteamthreefive.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class TextBarController {

    @FXML
    private TextField inputField;

    @FXML
    private Button generateButton;

    /**
     * Called when user clicks the "Generate Sentence" button.
     * For now, just logs the input text to console.
     */
    @FXML
    private void onGenerateClick() {
        String userInput = inputField.getText();

        if (userInput == null || userInput.trim().isEmpty()) {
            System.out.println("[TextBar] No input entered.");
        } else {
            System.out.println("[TextBar] User entered: " + userInput);
            // TODO: later integrate with the actual sentence generator
        }

        // Optional: clear the text field after submit
        inputField.clear();
    }
}
