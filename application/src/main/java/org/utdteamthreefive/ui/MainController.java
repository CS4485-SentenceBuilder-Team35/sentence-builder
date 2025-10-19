package org.utdteamthreefive.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

public class MainController {

    @FXML
    private HBox fileRow; // fx:id="fileRow" in FXML

    @FXML
    private ProgressBar progressBar; // fx:id="progressBar" in FXML

    @FXML
    public void initialize() {
        // Bind progress bar width to 30% of the HBox width
        progressBar.prefWidthProperty().bind(fileRow.widthProperty().multiply(0.3));
    }
}
