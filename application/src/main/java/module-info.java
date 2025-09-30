module org.utdteamthreefive.ui {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.utdteamthreefive.ui to javafx.fxml;
    exports org.utdteamthreefive.ui;
}