package org.utdteamthreefive.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.logging.Logger;

import org.utdteamthreefive.backend.service.SentenceGenerator;

public class TextBarController {
    private final Logger logger = Logger.getLogger(TextBarController.class.getName());

    @FXML
    private TextField inputField;

    @FXML
    private Button generateButton;

    @FXML
    private ChoiceBox<String> algoChoice;

    @FXML
    private Label sentenceField;

    /**
     * @author Justin Yao
     */
    @FXML
    private void initialize() {
        if (algoChoice != null) {
            algoChoice.setItems(FXCollections.observableArrayList(
                    "Most Frequent"));
            algoChoice.getSelectionModel().clearSelection();
            algoChoice.setValue("Choose Algorithm");
        }
    }

    /**
     * Button to generate a sentence from the given text field.
     *
     * @author Aiden Martinez and Justin Yao
     */
    @FXML
    protected void onGenerateClick(ActionEvent event) {
        /*if (userInput == null || userInput.trim().isEmpty()) {
            System.out.println("[TextBar] No input entered.");
        } else {
            System.out.println("[TextBar] User entered: " + userInput);
            sentenceField.setText("Generated sentence based on: " + userInput);
            // TODO: later integrate with the actual sentence generator
        }

        // Optional: clear the text field after submit
        inputField.clear();
          */
         if (inputField == null || inputField.getText() == null || inputField.getText().trim().isEmpty()) {
            if (sentenceField != null) {
                sentenceField.setText("Please enter a word to begin the sentence.");
                sentenceField.getStyleClass().add("sentence-output-label-error");
                logger.warning("Input field is empty.");
            }
            return;
        }

        if (sentenceField != null) {
            sentenceField.setText(SentenceGenerator.GenerateFromMostFrequent(inputField.getText().trim()));
            try {
                sentenceField.getStyleClass().remove("sentence-output-label-error");
            } catch (Exception e) {
                // Ignore if the style class was not present
            }
            logger.info("Generated sentence for input: " + inputField.getText().trim());
        }
             
    }
}
