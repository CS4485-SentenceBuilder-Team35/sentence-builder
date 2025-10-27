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
        fileNameLabel.setMinWidth(Region.USE_PREF_SIZE); // Ensure label doesn't shrink below preferred size
        fileNameLabel.setMaxWidth(Double.MAX_VALUE); // Allow label to grow if needed

        // Spacer to push progress bar to middle
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        // Progress bar
        progressBar = new ProgressBar(0.5);
        progressBar.setPrefHeight(18);
        progressBar.prefWidthProperty().bind(this.widthProperty().multiply(0.5)); // 50% of HBox width
        progressBar.setMaxWidth(Region.USE_PREF_SIZE); // Don't let it grow beyond preferred size

        // Add all elements
        getChildren().addAll(fileNameLabel, leftSpacer, progressBar);
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
