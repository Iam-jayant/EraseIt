package com.hackathon.securewipe;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * JavaFX Main Controller for the Secure Data Wipe Tool
 * Handles the one-click user interface and orchestrates the wipe process
 */
public class MainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    @FXML private ComboBox<DriveDetector.DriveInfo> driveComboBox;
    @FXML private ComboBox<SecureWipeEngine.WipeMethod> methodComboBox;
    @FXML private Button refreshDrivesButton;
    @FXML private Button wipeButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TextArea logTextArea;
    @FXML private Button saveCertificateButton;
    
    private CertificateGenerator certificateGenerator;
    private CertificateGenerator.WipeCertificate currentCertificate;
    private DriveDetector.DriveInfo currentDriveInfo;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing main controller");
        
        try {
            // Initialize certificate generator
            certificateGenerator = new CertificateGenerator();
            
            // Setup combo boxes
            methodComboBox.getItems().addAll(SecureWipeEngine.WipeMethod.values());
            methodComboBox.setValue(SecureWipeEngine.WipeMethod.PLATFORM_DEFAULT);
            
            // Setup initial state
            progressBar.setVisible(false);
            saveCertificateButton.setDisable(true);
            updateStatus("Ready - Select a drive to wipe");
            
            // Load available drives
            refreshDrives();
            
        } catch (Exception e) {
            logger.error("Error initializing controller", e);
            showErrorDialog("Initialization Error", "Failed to initialize the application: " + e.getMessage());
        }
    }
    
    @FXML
    private void refreshDrives() {
        logger.info("Refreshing drive list");
        
        Task<List<DriveDetector.DriveInfo>> refreshTask = new Task<List<DriveDetector.DriveInfo>>() {
            @Override
            protected List<DriveDetector.DriveInfo> call() throws Exception {
                updateMessage("Detecting drives...");
                return DriveDetector.detectDrives();
            }
            
            @Override
            protected void succeeded() {
                List<DriveDetector.DriveInfo> drives = getValue();
                Platform.runLater(() -> {
                    driveComboBox.getItems().clear();
                    driveComboBox.getItems().addAll(drives);
                    
                    if (!drives.isEmpty()) {
                        driveComboBox.setValue(drives.get(0));
                        updateStatus(String.format("Found %d drive(s)", drives.size()));
                    } else {
                        updateStatus("No drives detected");
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("Error detecting drives");
                    showErrorDialog("Drive Detection Error", "Failed to detect drives: " + getException().getMessage());
                });
            }
        };
        
        refreshTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> updateStatus(newMsg));
        });
        
        new Thread(refreshTask).start();
    }
    
    @FXML
    private void startWipe() {
        DriveDetector.DriveInfo selectedDrive = driveComboBox.getValue();
        SecureWipeEngine.WipeMethod selectedMethod = methodComboBox.getValue();
        
        if (selectedDrive == null) {
            showWarningDialog("No Drive Selected", "Please select a drive to wipe.");
            return;
        }
        
        // Safety check
        if (!SecureWipeEngine.canWipeDrive(selectedDrive)) {
            showErrorDialog("Cannot Wipe Drive", 
                "The selected drive cannot be safely wiped. It may be a system drive.");
            return;
        }
        
        // Confirmation dialog
        String confirmationMessage = String.format(
            "This will permanently erase ALL DATA on the following drive:\\n\\n" +
            "Drive: %s\\n" +
            "Size: %.2f GB\\n" +
            "Method: %s\\n\\n" +
            "This action CANNOT be undone. Are you absolutely sure you want to continue?",
            selectedDrive.toString(),
            selectedDrive.getSize() / (1024.0 * 1024.0 * 1024.0),
            selectedMethod.getDisplayName()
        );
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Data Wipe");
        confirmDialog.setHeaderText("WARNING: This will permanently destroy all data!");
        confirmDialog.setContentText(confirmationMessage);
        
        ButtonType yesButton = new ButtonType("Yes, Wipe Drive", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(yesButton, noButton);
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            performWipe(selectedDrive, selectedMethod);
        }
    }
    
    private void performWipe(DriveDetector.DriveInfo driveInfo, SecureWipeEngine.WipeMethod method) {
        logger.info("Starting wipe operation for drive: {}", driveInfo.getPath());
        
        currentDriveInfo = driveInfo;
        
        // Update UI state
        wipeButton.setDisable(true);
        refreshDrivesButton.setDisable(true);
        driveComboBox.setDisable(true);
        methodComboBox.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        saveCertificateButton.setDisable(true);
        
        // Clear previous logs
        logTextArea.clear();
        
        Task<SecureWipeEngine.WipeResult> wipeTask = new Task<SecureWipeEngine.WipeResult>() {
            @Override
            protected SecureWipeEngine.WipeResult call() throws Exception {
                return SecureWipeEngine.wipeDrive(
                    driveInfo, 
                    method, 
                    progress -> {
                        Platform.runLater(() -> {
                            progressBar.setProgress(progress);
                            updateStatus(String.format("Wiping... %.0f%%", progress * 100));
                        });
                    }
                ).get(); // Block until completion
            }
            
            @Override
            protected void succeeded() {
                SecureWipeEngine.WipeResult result = getValue();
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    
                    if (result.isSuccess()) {
                        updateStatus("Wipe completed successfully!");
                        logTextArea.appendText("✓ Wipe operation completed successfully\\n");
                        logTextArea.appendText(String.format("✓ Method: %s\\n", result.getMethod()));
                        logTextArea.appendText(String.format("✓ Bytes wiped: %,d\\n", result.getBytesWiped()));
                        logTextArea.appendText(String.format("✓ Duration: %s\\n", 
                            java.time.Duration.between(result.getStartTime(), result.getEndTime())));
                        
                        // Generate certificate
                        generateCertificate(result);
                    } else {
                        updateStatus("Wipe failed: " + result.getErrorMessage());
                        logTextArea.appendText("✗ Wipe operation failed\\n");
                        logTextArea.appendText("✗ Error: " + result.getErrorMessage() + "\\n");
                        showErrorDialog("Wipe Failed", "The wipe operation failed: " + result.getErrorMessage());
                    }
                    
                    // Reset UI state
                    resetUIState();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("Wipe operation failed");
                    logTextArea.appendText("✗ Wipe operation failed with exception\\n");
                    logTextArea.appendText("✗ Error: " + getException().getMessage() + "\\n");
                    showErrorDialog("Wipe Error", "Wipe operation failed: " + getException().getMessage());
                    resetUIState();
                });
            }
        };
        
        new Thread(wipeTask).start();
    }
    
    private void generateCertificate(SecureWipeEngine.WipeResult wipeResult) {
        try {
            currentCertificate = certificateGenerator.generateCertificate(wipeResult, currentDriveInfo);
            saveCertificateButton.setDisable(false);
            
            logTextArea.appendText("✓ Digital certificate generated\\n");
            logTextArea.appendText(String.format("✓ Certificate ID: %s\\n", currentCertificate.getCertificateId()));
            logTextArea.appendText(String.format("✓ Public Key Hash: %s\\n", currentCertificate.getPublicKeyHash()));
            
        } catch (Exception e) {
            logger.error("Error generating certificate", e);
            logTextArea.appendText("✗ Failed to generate certificate: " + e.getMessage() + "\\n");
            showErrorDialog("Certificate Error", "Failed to generate certificate: " + e.getMessage());
        }
    }
    
    @FXML
    private void saveCertificate() {
        if (currentCertificate == null) {
            showWarningDialog("No Certificate", "No certificate available to save.");
            return;
        }
        
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Save Certificate");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        Stage stage = (Stage) saveCertificateButton.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);
        
        if (selectedDirectory != null) {
            Task<Void> saveTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String baseName = "wipe_certificate_" + currentCertificate.getCertificateId();
                    
                    // Save as PDF
                    File pdfFile = new File(selectedDirectory, baseName + ".pdf");
                    certificateGenerator.saveCertificateAsPdf(currentCertificate, pdfFile);
                    
                    // Save as JSON
                    File jsonFile = new File(selectedDirectory, baseName + ".json");
                    certificateGenerator.saveCertificateAsJson(currentCertificate, jsonFile);
                    
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        logTextArea.appendText("✓ Certificates saved successfully\\n");
                        showInfoDialog("Certificates Saved", 
                            "Certificates have been saved to:\\n" + selectedDirectory.getAbsolutePath());
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        logTextArea.appendText("✗ Failed to save certificates: " + getException().getMessage() + "\\n");
                        showErrorDialog("Save Error", "Failed to save certificates: " + getException().getMessage());
                    });
                }
            };
            
            new Thread(saveTask).start();
        }
    }
    
    private void resetUIState() {
        wipeButton.setDisable(false);
        refreshDrivesButton.setDisable(false);
        driveComboBox.setDisable(false);
        methodComboBox.setDisable(false);
    }
    
    private void updateStatus(String status) {
        statusLabel.setText(status);
        logger.debug("Status: {}", status);
    }
    
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarningDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}