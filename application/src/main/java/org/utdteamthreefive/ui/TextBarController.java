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
                    "Most Frequent", "Least Frequent", "Complete Random", "Weighted Random"));
            algoChoice.getSelectionModel().clearSelection();
            algoChoice.setValue("Choose Algorithm");
        }
    }

    /**
     * Button to generate a sentence from the given text field.
     * using the selected algorithm.
     * 
     * @author Aiden Martinez
     * @author Justin Yao
     * @author Zaeem Rashid
     */
    @FXML
    protected void onGenerateClick(ActionEvent event) {
        if (sentenceField == null) {
            logger.severe("sentenceField label is null.");
            return;
        }

        String selectedAlgo = (algoChoice != null) ? algoChoice.getValue() : null;
        String userInput = (inputField != null && inputField.getText() != null)
                ? inputField.getText().trim()
                : "";

        // Checking an algorithm is selected
        if (selectedAlgo == null || "Choose Algorithm".equals(selectedAlgo)) {
            sentenceField.setText("Please choose an algorithm first.");
            sentenceField.getStyleClass().add("sentence-output-label-error");
            logger.warning("No algorithm selected.");
            return;
        }

        // Checking if the input field is empty
        boolean requiresInput = "Most Frequent".equals(selectedAlgo)
                || "Weighted Random".equals(selectedAlgo);

        if (requiresInput && userInput.isEmpty()) {
            sentenceField.setText("Please enter a word to begin the sentence.");
            sentenceField.getStyleClass().add("sentence-output-label-error");
            logger.warning("Input field is empty for algorithm: " + selectedAlgo);
            return;
        }

        String result;

        switch (selectedAlgo) {
            case "Most Frequent":
                result = SentenceGenerator.GenerateFromMostFrequent(userInput);
                break;

            case "Least Frequent":
                result = SentenceGenerator.GenerateFromLeastFrequent(userInput);
                break;

            case "Complete Random":
                result = SentenceGenerator.GenerateFromRandomWord(10);
                break;

            case "Weighted Random":
                result = SentenceGenerator.GenerateFromRandomFollow(userInput, 10);
                break;

            default:
                result = "Unknown algorithm selected.";
                logger.warning("Unknown algorithm: " + selectedAlgo);
                break;
        }

        sentenceField.setText(result);
        sentenceField.setWrapText(true);

        logger.info("Generated sentence using algorithm '" + selectedAlgo
                + "' for input: '" + userInput + "'");
    }
}
