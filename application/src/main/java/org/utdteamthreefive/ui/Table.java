/**
 * @author Rommel Isaac Baldivas
 */

package org.utdteamthreefive.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.utdteamthreefive.backend.models.Word;
import org.utdteamthreefive.backend.models.enums.WordType;
import org.utdteamthreefive.backend.util.DatabaseManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

/**
 * This is a class that will initialize a TableView and
 * populate it with the WORD table from the database
 */
public class Table {
    TableView<Word> table; // The TableView UI component
    Label tableStatusLabel; // Label to show status of the table
    private ObservableList<Word> data = FXCollections.observableArrayList(); // Holds Word objects for the TableView
    private Connection conn; // DB connection
    private static final Logger logger = Logger.getLogger(Table.class.getName());

    public Table() {
        // Establish the TableView
        table = new TableView<Word>();
        table.setItems(data);
        table.getStyleClass().add("main-table");
        table.getColumns().setAll(setupColumns());
        table.setEditable(true);
        table.setMinWidth(TableView.USE_COMPUTED_SIZE);
        tableStatusLabel = new Label("Table loaded");
        tableStatusLabel.setMinWidth(TableView.USE_COMPUTED_SIZE);
        tableStatusLabel.setMaxWidth(Double.MAX_VALUE);
        tableStatusLabel.setAlignment(javafx.geometry.Pos.CENTER);

        // Allow table to expand when columns are resized
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // Populate the table
        syncTableWithDatabase();
    }

    /**
     * Uses a prepared statement to query the WORD table
     * and populate the TableView with the results
     */
    public void syncTableWithDatabase() {
        // Open DB connection and query for wordss
        Platform.runLater(() -> {
            conn = DatabaseManager.open();

            String wordQuery = "SELECT word_id, word_token, total_count, start_count, end_count, type_ FROM WORD";
            try {
                PreparedStatement selectStatement = conn.prepareStatement(wordQuery);
                var rs = selectStatement.executeQuery();

                logger.info("Refreshing TableView data. Current size: " + data.size());
                data.clear();

                while (rs.next()) {
                    // Make sure that type maps to WordType enum
                    WordType type = null;
                    String typeStr = rs.getString("type_");
                    if (typeStr != null && typeStr.equalsIgnoreCase("alpha")) {
                        type = WordType.alpha;
                    }
                    else if (typeStr != null && typeStr.equalsIgnoreCase("misc")) {
                        type = WordType.misc;
                    }
                    else {
                        throw new IllegalArgumentException("Unknown word type: " + typeStr);
                    }

                    // Create Word object and add to data
                    Word word = new Word(rs.getInt("word_id"), rs.getString("word_token"), rs.getInt("total_count"), rs.getInt("start_count"), rs.getInt("end_count"), type);
                    data.add(word);
                }

                logger.info("Updated TableView from database. New size: " + data.size());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                table.refresh();
                DatabaseManager.close(conn);
            }
        });
    }

    /**
     * This programmatically fills out the columns for the TableView
     * 
     * @return List of TableColumns for the TableView
     */
    public ArrayList<TableColumn<Word, ?>> setupColumns() {
        // Setup the columns
        TableColumn<Word, String> wordTokenCol = new TableColumn<Word, String>("Text");
        wordTokenCol.setPrefWidth(400);
        wordTokenCol.setMinWidth(200);
        wordTokenCol.setCellValueFactory(new PropertyValueFactory<>("wordToken"));
        wordTokenCol.setEditable(false);

        TableColumn<Word, Integer> totalCountCol = new TableColumn<Word, Integer>("Total Count");
        totalCountCol.setPrefWidth(90);
        totalCountCol.setMinWidth(70);
        totalCountCol.setCellValueFactory(new PropertyValueFactory<>("totalCount"));
        totalCountCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        totalCountCol.setEditable(true);

        TableColumn<Word, Integer> startCountCol = new TableColumn<Word, Integer>("Start Count");
        startCountCol.setPrefWidth(90);
        startCountCol.setMinWidth(70);
        startCountCol.setCellValueFactory(new PropertyValueFactory<>("startCount"));
        startCountCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        startCountCol.setEditable(true);

        TableColumn<Word, Integer> endCountCol = new TableColumn<Word, Integer>("End Count");
        endCountCol.setPrefWidth(90);
        endCountCol.setMinWidth(70);
        endCountCol.setCellValueFactory(new PropertyValueFactory<>("endCount"));
        endCountCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        endCountCol.setEditable(true);

        TableColumn<Word, String> typeCol = new TableColumn<Word, String>("Type");
        typeCol.setPrefWidth(200);
        typeCol.setMinWidth(100);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setEditable(false);

        return new ArrayList<TableColumn<Word, ?>>(
                Arrays.asList(wordTokenCol, totalCountCol, startCountCol, endCountCol, typeCol));
    }

    public TableView<Word> getTableView() {
        return table;
    }

    public Label getTableStatusLabel() {
        return tableStatusLabel;
    }
}
