package com.asciidocvault;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController();

        Scene scene = new Scene(controller.buildUI(), 1280, 800);
        // Load stylesheet if present on the classpath
        var css = getClass().getResource("/styles/editor.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        primaryStage.setTitle("AsciiDoc Workspace");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
