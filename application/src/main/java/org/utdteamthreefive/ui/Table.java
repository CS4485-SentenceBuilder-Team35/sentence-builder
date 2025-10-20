/**
 * @author Rommel Isaac Baldivas
 */

package org.utdteamthreefive.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;

import org.utdteamthreefive.backend.models.Word;
import org.utdteamthreefive.backend.models.enums.WordType;
import org.utdteamthreefive.backend.util.DatabaseManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

public class Table {
    TableView<Word> table;

    // Sample data for now
    ObservableList<Word> data = FXCollections.observableArrayList();

    ObservableList<Word> selectedItems;

    private Connection conn;

    public Table() {
        table = new TableView<Word>();
        table.setItems(data);
        table.getStyleClass().add("main-table");
        table.getColumns().setAll(setupColumns());
        table.setEditable(true);
        table.setMinWidth(TableView.USE_COMPUTED_SIZE);

        // Allow table to expand when columns are resized
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // Open DB connection and query for words
        conn = DatabaseManager.open();
        if (conn == null) {
            System.err.println("❌ Failed to open database connection");
            return;
        }
        String wordQuery = "SELECT word_id, word_token, total_count, start_count, end_count, type_ FROM WORD";
        try {
            PreparedStatement selectStatement = conn.prepareStatement(wordQuery);
            var rs = selectStatement.executeQuery();
            data.clear();
            while (rs.next()) {
                WordType type = WordType.valueOf(rs.getString("type_").toLowerCase());
                Word word = new Word(rs.getInt("word_id"), rs.getString("word_token"), rs.getInt("total_count"),
                        rs.getInt("start_count"), rs.getInt("end_count"), type);
                data.add(word);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseManager.close(conn);
        }
    }

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
}
