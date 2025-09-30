module com.utd.team35.ui {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.utd.team35.ui to javafx.fxml;
    exports com.utd.team35.ui;
}