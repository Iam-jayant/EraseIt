package com.hackathon.securewipe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX Application class for the Secure Data Wipe Tool
 * Cross-platform MVP application to securely wipe USB drives and laptop hard drives
 * with digital proof of erasure.
 */
public class SecureDataWipeApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureDataWipeApplication.class);
    
    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting Secure Data Wipe Tool application");
        
        FXMLLoader fxmlLoader = new FXMLLoader(
            SecureDataWipeApplication.class.getResource("/com/hackathon/securewipe/main-view.fxml")
        );
        
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        
        // Load CSS stylesheet
        String css = SecureDataWipeApplication.class.getResource("/com/hackathon/securewipe/application.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        stage.setTitle("Secure Data Wipe Tool - Smart India Hackathon MVP");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        
        logger.info("Application window displayed successfully");
    }

    public static void main(String[] args) {
        logger.info("Launching Secure Data Wipe Tool...");
        launch();
    }
}