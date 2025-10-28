package org.utdteamthreefive.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class FileTab extends HBox {

    private final Label fileNameLabel;
    private final ProgressBar progressBar;

    public FileTab(String fileName) {
        // Layout and style
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #EAF4FF; "
               + "-fx-border-color: #B8CDE0; "
               + "-fx-border-radius: 6; "
               + "-fx-background-radius: 6;");

        // File name label
        fileNameLabel = new Label(fileName);

        // Fixed left spacer
        Region leftSpacer = new Region();
        leftSpacer.setPrefWidth(30);

        // Progress bar
        progressBar = new ProgressBar(0.5);
        progressBar.setPrefHeight(18);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        // Flexible spacer to make layout adaptive
        Region flexSpacer = new Region();
        HBox.setHgrow(flexSpacer, Priority.ALWAYS);

        // Bind progress bar width to 30% of this FileTab width
        progressBar.prefWidthProperty().bind(widthProperty().multiply(0.3));

        // Add all elements
        getChildren().addAll(fileNameLabel, leftSpacer, progressBar, flexSpacer);
    }

    // Allow external control
    public void setProgress(double value) {
        progressBar.setProgress(value);
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public Label getFileNameLabel() {
        return fileNameLabel;
    }
}
