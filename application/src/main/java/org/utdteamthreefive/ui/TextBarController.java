package org.utdteamthreefive.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import org.utdteamthreefive.backend.service.SentenceGenerator;

public class TextBarController {

    @FXML
    private TextField inputField;

    @FXML
    private Button generateButton;

    @FXML
    private ChoiceBox<String> algoChoice;

    @FXML
    private TextField sentenceField;

    @FXML
    private void initialize() {
        if (algoChoice != null) {
            algoChoice.setItems(FXCollections.observableArrayList(
                    "Markov Chain",
                    "N-gram",
                    "Neural Model"));
            algoChoice.getSelectionModel().clearSelection();
            algoChoice.setValue("Choose Algorithm");
        }
    }

    /**
     * Button to generate a sentence from the given text field.
     * TODO: implement algorithm selection.
     *
     * @author Aiden Martinez
     */
    @FXML
    protected void onGenerateClick(ActionEvent event) {

      /*   String userInput = inputField.getText();

        if (userInput == null || userInput.trim().isEmpty()) {
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
                System.out.println("Input field is empty.");
            }
            return;
        }

        if (sentenceField != null) {
            sentenceField.setText(SentenceGenerator.GenerateFromMostFrequent(inputField.getText().trim()));
            System.out.println("Generated sentence for input: " + inputField.getText().trim());
        }
             
    }
}
