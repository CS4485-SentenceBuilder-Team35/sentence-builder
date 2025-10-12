package org.utdteamthreefive.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import org.utdteamthreefive.backend.SampleClass;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MainController {
    private Stage stage;
    private Scene scene;
    private Parent root;

//    @FXML
//    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        SampleClass sampleClass = new SampleClass();
//        welcomeText.setText(sampleClass.getGreeting());
    }

    @FXML
    protected void onSwitchToTableView(ActionEvent event) {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("table-view.fxml"));
        try {
            root = fxmlLoader.load();
            stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}