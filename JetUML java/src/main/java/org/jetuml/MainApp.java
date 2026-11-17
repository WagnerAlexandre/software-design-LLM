package org.jetuml;

import javafx.application.Application;
import javafx.stage.Stage;
import org.jetuml.ui.JetUMLMainWindow;

/**
 * Ponto de entrada principal da aplicacao JetUML (Java 21 + JavaFX 21).
 */
public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        new JetUMLMainWindow(primaryStage);
        primaryStage.show();
    }
}
