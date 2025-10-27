package org.utdteamthreefive.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.utdteamthreefive.backend.FileParseHandle;
import org.utdteamthreefive.backend.SampleClass;

import javax.swing.*;
import java.io.File;
import java.util.List;
import javafx.scene.layout.VBox;

public class MainController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private HBox fileRow; // fx:id="fileRow" in FXML
    private ProgressBar progressBar; // fx:id="progressBar" in FXML
    private VBox uploadContainer;

    /**
     * @author Rommel Isaac Baldivas
     * @param event
     */
    @FXML
    protected void onSwitchToTableView(ActionEvent event) {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("table-view.fxml"));
        try {
            root = fxmlLoader.load();
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);

            Table table = new Table();

            // Query the FlowPane Node so that the Table can be added to it
            FlowPane flowPane = (FlowPane) scene.lookup(".main-flow-pane");
            flowPane.getChildren().add(table.getTableView());
            FlowPane.setMargin(table.getTableView(), new Insets(16.0, 0, 16.0, 0));

            stage.show();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onSwitchToUploadClick(ActionEvent event) {
        SampleClass sampleClass = new SampleClass();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("upload-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onUploadFileButtonClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        List<File> files = fileChooser
                .showOpenMultipleDialog((Stage) ((Node) event.getSource()).getScene().getWindow());

        for (int i = 0; i < files.size(); i++) {
            FileParseHandle.ParseFile(files.get(i).getAbsolutePath());
        }
    }

    @FXML
    protected void onGenerateSentenceButtonClick(ActionEvent event) {
    }

    public void initialize() {
        // Add sample file tabs for testing
        addFileTab("example.txt");
        addFileTab("report.pdf");
    }

    public void addFileTab(String fileName) {
        FileTab fileTab = new FileTab(fileName);
        uploadContainer.getChildren().add(fileTab);
    }
}
