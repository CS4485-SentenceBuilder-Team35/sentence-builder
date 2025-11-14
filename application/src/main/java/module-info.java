module org.utdteamthreefive.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.github.cdimascio.dotenv.java;
    requires java.logging;
    requires java.sql;

    requires kotlin.stdlib;
    requires openai.java.core;
    requires openai.java.client.okhttp;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    opens org.utdteamthreefive.ui to javafx.fxml;

    exports org.utdteamthreefive.backend.models;
    exports org.utdteamthreefive.ui;
}