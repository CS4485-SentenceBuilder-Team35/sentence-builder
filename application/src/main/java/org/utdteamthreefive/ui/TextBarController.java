package org.utdteamthreefive.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.utdteamthreefive.backend.service.SentenceGenerator;

public class TextBarController {

    @FXML
    private TextField inputField;

    @FXML
    private Button generateButton;

    /**
     Button to generate a sentence from the given text field
     TODO: implement algorithm selection
     @author Aiden Martinez
     */
    @FXML
    protected void onGenerateClick(ActionEvent event)
    {
        if(inputField == null || inputField.getText().equals(" "))
        {
            sentenceField.setText("Please Enter a Word to Begin the Sentence.");
            return;
        }

        sentenceField.setText(SentenceGenerator.GenerateFromMostFrequent(inputField.getText()));
    }
}
