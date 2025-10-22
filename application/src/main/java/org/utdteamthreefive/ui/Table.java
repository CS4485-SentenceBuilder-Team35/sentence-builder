/**
 * @author Rommel Isaac Baldivas
 */

package org.utdteamthreefive.ui;

import java.util.ArrayList;
import java.util.Arrays;

import org.utdteamthreefive.backend.models.Word;
import org.utdteamthreefive.backend.models.enums.WordType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class Table {
    TableView<Word> table;

    // Sample data for now
    ObservableList<Word> data = FXCollections.observableArrayList(new Word(1, "hello", 25, 5, 3, WordType.alpha),
            new Word(2, "world", 30, 8, 4, WordType.alpha), new Word(3, "the", 150, 45, 12, WordType.alpha),
            new Word(4, "quick", 15, 2, 1, WordType.alpha), new Word(5, "brown", 12, 3, 2, WordType.alpha),
            new Word(6, "fox", 8, 1, 1, WordType.alpha), new Word(7, "jumps", 6, 2, 0, WordType.alpha),
            new Word(8, "over", 20, 4, 2, WordType.alpha), new Word(9, "lazy", 10, 1, 1, WordType.alpha),
            new Word(10, "dog", 18, 3, 2, WordType.alpha), new Word(11, "!", 75, 0, 25, WordType.misc),
            new Word(12, ".", 200, 0, 80, WordType.misc), new Word(13, "?", 35, 0, 15, WordType.misc));

    public Table() {
        table = new TableView<Word>();
        table.setItems(data);
        table.getStyleClass().add("main-table");
        table.getColumns().setAll(setupColumns());
        // Prevent the table from being editable
        table.setEditable(false);
    }

    public ArrayList<TableColumn<Word, ?>> setupColumns() {
        // Setup the columns
        TableColumn<Word, String> wordTokenCol = new TableColumn<Word, String>("Text");
        wordTokenCol.setPrefWidth(400);
        wordTokenCol.setCellValueFactory(new PropertyValueFactory<>("wordToken"));
        wordTokenCol.getStyleClass().add("column-primary");

        TableColumn<Word, Integer> totalCountCol = new TableColumn<Word, Integer>("Total Count");
        totalCountCol.setPrefWidth(90);
        totalCountCol.setCellValueFactory(new PropertyValueFactory<>("totalCount"));
        totalCountCol.getStyleClass().add("column-secondary");

        TableColumn<Word, Integer> startCountCol = new TableColumn<Word, Integer>("Start Count");
        startCountCol.setPrefWidth(90);
        startCountCol.setCellValueFactory(new PropertyValueFactory<>("startCount"));
        startCountCol.getStyleClass().add("column-primary");

        TableColumn<Word, Integer> endCountCol = new TableColumn<Word, Integer>("End Count");
        endCountCol.setPrefWidth(90);
        endCountCol.setCellValueFactory(new PropertyValueFactory<>("endCount"));
        endCountCol.getStyleClass().add("column-secondary");

        TableColumn<Word, String> typeCol = new TableColumn<Word, String>("Type");
        typeCol.setPrefWidth(200);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.getStyleClass().add("column-primary");

        return new ArrayList<TableColumn<Word, ?>>(
                Arrays.asList(wordTokenCol, totalCountCol, startCountCol, endCountCol, typeCol));
    }

    public TableView<Word> getTableView() {
        return table;
    }
}
