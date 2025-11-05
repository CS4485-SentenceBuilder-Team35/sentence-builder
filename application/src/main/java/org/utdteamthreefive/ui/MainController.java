package org.utdteamthreefive.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.utdteamthreefive.backend.util.FileParseHandle;
import org.utdteamthreefive.backend.SampleClass;
import org.utdteamthreefive.backend.service.BackendService;
import org.utdteamthreefive.backend.service.SentenceGenerator;

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
    private List<File> filesToUpload;
    private ObservableList<Node> filesToUploadUI;

    @FXML
    private HBox fileRow; // fx:id="fileRow" in FXML
    @FXML
    private ProgressBar progressBar; // fx:id="progressBar" in FXML
    @FXML
    private VBox uploadFileListContainer; // fx:id="uploadFileListContainer" in FXML
    @FXML
    private TabPane tabPane; // fx:id="tabPane" in FXML
    @FXML
    private Button uploadFilesButton; // fx:id="uploadFilesButton" in FXML

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

        try
        {
            Parent root = FXMLLoader.load(getClass().getResource("upload-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onSelectFilesButtonClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File(s) to Upload"); // Nice to have
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir"))); // Nice to have so we don't have to keep navigating from root
        filesToUpload = fileChooser.showOpenMultipleDialog((Stage) ((Node) event.getSource()).getScene().getWindow());

        for (File file : filesToUpload) {
            addFileTab(file.getName());
        }

        // Enable upload button only if files are selected
        uploadFilesButton.setDisable(filesToUpload == null || filesToUpload.isEmpty());
    }

    /**
     * Click event is called when the user confirms upload of selected files
     * 
     * @param event
     * @author Rommel Isaac Baldivas
     */
    @FXML
    protected void onUploadFilesButtonClick(ActionEvent event) {
        if (filesToUpload == null || filesToUpload.isEmpty())
            return;

        for (File file : filesToUpload) {
            // Find the corresponding FileTab for this file
            FileTab fileTab = filesToUploadUI.stream().filter(node -> node instanceof FileTab).map(node -> (FileTab) node).filter(tab -> tab.getFileNameLabel().getText().equals(file.getName())).findFirst().orElse(null);

            // Start parsing the file and updating the progress bar in the FileTab
            if (fileTab != null) {
                // FileParseHandle.ParseFile(file.getAbsolutePath(), table, fileTab);
                FileParseHandle.ParseFile(file, table, fileTab);
            }
        }
    }


    /**
     * Adds a new FileTab to the UI.
     * @param fileName
     * @author Justin Yao and Rommel Isaac Baldivas
     */
    public FileTab addFileTab(String fileName) {
        FileTab fileTab = new FileTab(fileName);
        filesToUploadUI.add(fileTab); // Add to observable list since it is bound to the UI container
        return fileTab;
    }

    /**
     * The initialize method is called automatically after all FXML
     * members have been injected. It is used to perform any necessary setup for the
     * controller such as responsive widths, initializing UI components, and adding
     * the Table to the FlowPane.
     *
     * @author Rommel Isaac Baldivas
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (tabPane == null) {
            System.err.println("Cannot find TabPane with fx:id 'tabPane'");
            return;
        }

        // Set up responsive tab widths if TabPane exists
        setupResponsiveTabWidths();

        // Use Platform.runLater to ensure the TabPane is in the scene graph
        Platform.runLater(() -> {
            scene = tabPane.getScene();
            if (scene == null) {
                System.err.println("TabPane is not yet part of a Scene.");
                return;
            }

            // Query the FlowPane Node so that the Table can be added to it
            table = new Table();
            FlowPane flowPane = (FlowPane) scene.lookup(".table-flow-pane");
            if (flowPane == null) {
                System.err.println("FlowPane is not found.");
                return;
            }

            flowPane.getChildren().add(table.getTableView());
            FlowPane.setMargin(table.getTableView(), new Insets(16.0, 0, 16.0, 0));

            // Upload File Button is initially disabled until files are selected
            uploadFilesButton.setDisable(true);

            // Initialize the observable list for file tabs
            filesToUploadUI = BackendService.getFilesFromDatabase();

            // Bind the uploadFileListContainer to the observable list
            Bindings.bindContentBidirectional(uploadFileListContainer.getChildren(), filesToUploadUI);
        });

    }

    /**
     * The event listener will listen for width changes and update tab
     * widths accordingly
     * 
     * @author Rommel Isaac Baldivas
     */
    private void setupResponsiveTabWidths() {
        tabPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            double availableWidth = newValue.doubleValue() - tabPane.getInsets().getLeft() - tabPane.getInsets().getRight();
            int tabCount = tabPane.getTabs().size();

            if (tabCount > 0 && availableWidth > 0) {
                double tabWidth = availableWidth / tabCount;
                tabPane.setTabMinWidth(tabWidth);
                tabPane.setTabMaxWidth(Double.MAX_VALUE); // No max width limit
            }
        });
    }
}
