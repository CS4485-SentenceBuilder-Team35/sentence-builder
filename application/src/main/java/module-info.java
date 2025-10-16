module org.utdteamthreefive.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.github.cdimascio.dotenv.java;
    requires java.logging;
    requires java.sql;

    opens org.utdteamthreefive.ui to javafx.fxml;

    exports org.utdteamthreefive.backend.models;
    exports org.utdteamthreefive.ui;
}