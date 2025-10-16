package org.utdteamthreefive.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class MainController {
    private Stage stage;
    private Scene scene;
    private Parent root;

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
            // flowPane.getChildren().add(table.getTableView());
            // FlowPane.setMargin(table.getTableView(), new Insets(16.0, 0, 16.0, 0));

            stage.show();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}