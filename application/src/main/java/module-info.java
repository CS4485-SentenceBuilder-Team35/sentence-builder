module org.utdteamthreefive.ui {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.utdteamthreefive.ui to javafx.fxml;

    exports org.utdteamthreefive.backend.models;
    exports org.utdteamthreefive.ui;
}