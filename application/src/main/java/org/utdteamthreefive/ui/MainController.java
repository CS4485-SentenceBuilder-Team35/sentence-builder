package org.utdteamthreefive.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.utdteamthreefive.backend.util.FileParseHandle;
import org.utdteamthreefive.backend.SampleClass;

// import javax.swing.*;
import java.io.File;
import java.util.List;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private Table table;

    @FXML
    private HBox fileRow; // fx:id="fileRow" in FXML
    @FXML
    private ProgressBar progressBar; // fx:id="progressBar" in FXML
    @FXML
    private VBox uploadContainer;
    @FXML
    private TabPane tabPane;

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
            FileTab fileTab = addFileTab(files.get(i).getName());
            FileParseHandle.ParseFile(files.get(i).getAbsolutePath(), table);
        }
    }

    @FXML
    protected void onGenerateSentenceButtonClick(ActionEvent event) {
    }

    public FileTab addFileTab(String fileName) {
        FileTab fileTab = new FileTab(fileName);
        uploadContainer.getChildren().add(fileTab);
        return fileTab;
    }

    /**
     * @author Rommel Isaac Baldivas
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up responsive tab widths if TabPane exists
        if (tabPane != null) {
            setupResponsiveTabWidths();

            // Use Platform.runLater to ensure the TabPane is in the scene graph
            Platform.runLater(() -> {
                scene = tabPane.getScene();
                if (scene != null) {
                    // Query the FlowPane Node so that the Table can be added to it
                    table = new Table();
                    FlowPane flowPane = (FlowPane) scene.lookup(".table-flow-pane");
                    if (flowPane != null) {
                        flowPane.getChildren().add(table.getTableView());
                        FlowPane.setMargin(table.getTableView(), new Insets(16.0, 0, 16.0, 0));
                    }
                }
            });
        }
    }

    /**
     * @author Rommel Isaac Baldivas
     *         The event listener will listen for width changes and update tab
     *         widths accordingly
     */
    private void setupResponsiveTabWidths() {
        tabPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            double availableWidth = newValue.doubleValue() - tabPane.getInsets().getLeft()
                    - tabPane.getInsets().getRight();
            int tabCount = tabPane.getTabs().size();

            if (tabCount > 0 && availableWidth > 0) {
                double tabWidth = availableWidth / tabCount;
                tabPane.setTabMinWidth(tabWidth);
                tabPane.setTabMaxWidth(Double.MAX_VALUE); // No max width limit
            }
        });
    }
}
