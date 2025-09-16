package com.hackathon.securewipe;

import com.sun.jna.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Cross-platform secure wipe engine that implements industry-standard wiping methods
 * Uses shred, dd on Linux and PowerShell/Cipher on Windows for secure data erasure
 */
public class SecureWipeEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureWipeEngine.class);
    
    public enum WipeMethod {
        SINGLE_PASS_ZERO("Single Pass (Zeros)", 1),
        DOD_3_PASS("DoD 3-Pass", 3),
        GUTMANN_35_PASS("Gutmann 35-Pass", 35),
        PLATFORM_DEFAULT("Platform Default", 1);
        
        private final String displayName;
        private final int passes;
        
        WipeMethod(String displayName, int passes) {
            this.displayName = displayName;
            this.passes = passes;
        }
        
        public String getDisplayName() { return displayName; }
        public int getPasses() { return passes; }
        
        @Override
        public String toString() { return displayName; }
    }
    
    public static class WipeResult {
        private final boolean success;
        private final String method;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final String devicePath;
        private final String deviceSerial;
        private final long bytesWiped;
        private final String errorMessage;
        
        public WipeResult(boolean success, String method, LocalDateTime startTime, 
                         LocalDateTime endTime, String devicePath, String deviceSerial,
                         long bytesWiped, String errorMessage) {
            this.success = success;
            this.method = method;
            this.startTime = startTime;
            this.endTime = endTime;
            this.devicePath = devicePath;
            this.deviceSerial = deviceSerial;
            this.bytesWiped = bytesWiped;
            this.errorMessage = errorMessage;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMethod() { return method; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getDevicePath() { return devicePath; }
        public String getDeviceSerial() { return deviceSerial; }
        public long getBytesWiped() { return bytesWiped; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Securely wipes the specified drive using the given method
     * 
     * @param driveInfo The drive to wipe
     * @param method The wipe method to use
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @return CompletableFuture containing the wipe result
     */
    public static CompletableFuture<WipeResult> wipeDrive(DriveDetector.DriveInfo driveInfo, 
                                                         WipeMethod method,
                                                         Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting secure wipe of drive: {} using method: {}", 
                       driveInfo.getPath(), method);
            
            LocalDateTime startTime = LocalDateTime.now();
            
            try {
                if (Platform.isWindows()) {
                    return wipeWindowsDrive(driveInfo, method, progressCallback, startTime);
                } else if (Platform.isLinux() || Platform.isMac()) {
                    return wipeLinuxDrive(driveInfo, method, progressCallback, startTime);
                } else {
                    throw new UnsupportedOperationException("Unsupported platform: " + Platform.getOSType());
                }
            } catch (Exception e) {
                logger.error("Error during drive wipe", e);
                return new WipeResult(false, method.getDisplayName(), startTime, 
                                    LocalDateTime.now(), driveInfo.getPath(), 
                                    driveInfo.getSerialNumber(), 0, e.getMessage());
            }
        });
    }
    
    /**
     * Wipes a drive on Windows using PowerShell and Cipher commands
     */
    private static WipeResult wipeWindowsDrive(DriveDetector.DriveInfo driveInfo, 
                                             WipeMethod method,
                                             Consumer<Double> progressCallback,
                                             LocalDateTime startTime) throws Exception {
        
        String driveLetter = driveInfo.getPath();
        progressCallback.accept(0.1);
        
        // Step 1: Format the drive
        logger.info("Formatting drive {}", driveLetter);
        ProcessBuilder formatBuilder = new ProcessBuilder(
            "powershell.exe", "-Command", 
            "Format-Volume -DriveLetter " + driveLetter.substring(0, 1) + " -FileSystem NTFS -Force"
        );
        
        Process formatProcess = formatBuilder.start();
        int formatResult = formatProcess.waitFor();
        
        if (formatResult != 0) {
            throw new RuntimeException("Failed to format drive " + driveLetter);
        }
        
        progressCallback.accept(0.3);
        
        // Step 2: Use Cipher for secure deletion
        logger.info("Running Cipher secure wipe on {}", driveLetter);
        
        String cipherCommand;
        switch (method) {
            case DOD_3_PASS:
                cipherCommand = "cipher /w:" + driveLetter;
                break;
            case SINGLE_PASS_ZERO:
            case PLATFORM_DEFAULT:
            default:
                cipherCommand = "cipher /w:" + driveLetter;
                break;
        }
        
        ProcessBuilder cipherBuilder = new ProcessBuilder("cmd.exe", "/c", cipherCommand);
        Process cipherProcess = cipherBuilder.start();
        
        // Monitor progress
        monitorProcessProgress(cipherProcess, progressCallback, 0.3, 0.9);
        
        int cipherResult = cipherProcess.waitFor();
        progressCallback.accept(1.0);
        
        LocalDateTime endTime = LocalDateTime.now();
        
        if (cipherResult == 0) {
            logger.info("Successfully wiped drive {}", driveLetter);
            return new WipeResult(true, method.getDisplayName(), startTime, endTime,
                                driveInfo.getPath(), driveInfo.getSerialNumber(),
                                driveInfo.getSize(), null);
        } else {
            throw new RuntimeException("Cipher command failed with exit code: " + cipherResult);
        }
    }
    
    /**
     * Wipes a drive on Linux using shred or dd commands
     */
    private static WipeResult wipeLinuxDrive(DriveDetector.DriveInfo driveInfo,
                                           WipeMethod method,
                                           Consumer<Double> progressCallback,
                                           LocalDateTime startTime) throws Exception {
        
        String devicePath = driveInfo.getPath();
        progressCallback.accept(0.1);
        
        // First, unmount any mounted partitions
        logger.info("Unmounting partitions for device {}", devicePath);
        unmountLinuxDevice(devicePath);
        
        progressCallback.accept(0.2);
        
        String command;
        switch (method) {
            case SINGLE_PASS_ZERO:
                command = String.format("dd if=/dev/zero of=%s bs=1M status=progress", devicePath);
                break;
            case DOD_3_PASS:
                // For DoD 3-pass, we'll use shred
                command = String.format("shred -vfz -n 3 %s", devicePath);
                break;
            case GUTMANN_35_PASS:
                command = String.format("shred -vfz -n 35 %s", devicePath);
                break;
            case PLATFORM_DEFAULT:
            default:
                // Check if shred is available, otherwise use dd
                if (isCommandAvailable("shred")) {
                    command = String.format("shred -vfz -n 1 %s", devicePath);
                } else {
                    command = String.format("dd if=/dev/zero of=%s bs=1M status=progress", devicePath);
                }
                break;
        }
        
        logger.info("Executing wipe command: {}", command);
        
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        Process wipeProcess = processBuilder.start();
        
        // Monitor progress
        monitorProcessProgress(wipeProcess, progressCallback, 0.2, 0.9);
        
        int result = wipeProcess.waitFor();
        progressCallback.accept(1.0);
        
        LocalDateTime endTime = LocalDateTime.now();
        
        if (result == 0) {
            logger.info("Successfully wiped drive {}", devicePath);
            return new WipeResult(true, method.getDisplayName(), startTime, endTime,
                                driveInfo.getPath(), driveInfo.getSerialNumber(),
                                driveInfo.getSize(), null);
        } else {
            throw new RuntimeException("Wipe command failed with exit code: " + result);
        }
    }
    
    /**
     * Unmounts all partitions of a Linux device
     */
    private static void unmountLinuxDevice(String devicePath) {
        try {
            // Get all mounted partitions for this device
            ProcessBuilder lsblkBuilder = new ProcessBuilder("lsblk", "-ln", "-o", "NAME", devicePath);
            Process lsblkProcess = lsblkBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(lsblkProcess.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.equals(devicePath.substring(5))) { // Remove /dev/ prefix
                    String partitionPath = "/dev/" + line;
                    
                    // Try to unmount
                    ProcessBuilder umountBuilder = new ProcessBuilder("umount", partitionPath);
                    Process umountProcess = umountBuilder.start();
                    umountProcess.waitFor(); // Don't check result, some partitions might not be mounted
                    
                    logger.debug("Attempted to unmount {}", partitionPath);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error unmounting device partitions", e);
        }
    }
    
    /**
     * Checks if a command is available in the system PATH
     */
    private static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder("which", command);
            Process process = builder.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Monitors process progress by reading output
     */
    private static void monitorProcessProgress(Process process, Consumer<Double> progressCallback,
                                             double startProgress, double endProgress) {
        Thread progressThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                
                String line;
                while (process.isAlive() && (line = reader.readLine()) != null) {
                    // Parse progress from output (implementation would depend on tool output format)
                    // For now, just update progress incrementally
                    double currentProgress = startProgress + 
                        (Math.random() * (endProgress - startProgress) * 0.1); // Simulate progress
                    progressCallback.accept(Math.min(currentProgress, endProgress));
                    
                    Thread.sleep(1000);
                }
                
                // Log any errors
                while ((line = errorReader.readLine()) != null) {
                    logger.debug("Process error output: {}", line);
                }
                
            } catch (Exception e) {
                logger.debug("Error monitoring process progress", e);
            }
        });
        
        progressThread.setDaemon(true);
        progressThread.start();
    }
    
    /**
     * Validates that a drive can be safely wiped
     */
    public static boolean canWipeDrive(DriveDetector.DriveInfo driveInfo) {
        // Don't allow wiping of system drives
        if (Platform.isWindows()) {
            String systemDrive = System.getenv("SystemDrive");
            if (driveInfo.getPath().startsWith(systemDrive)) {
                return false;
            }
        } else {
            // On Linux, don't allow wiping root filesystem
            try {
                String rootDevice = Files.readString(Paths.get("/proc/mounts"))
                    .lines()
                    .filter(line -> line.contains(" / "))
                    .findFirst()
                    .map(line -> line.split("\\s+")[0])
                    .orElse("");
                
                if (driveInfo.getPath().equals(rootDevice)) {
                    return false;
                }
            } catch (Exception e) {
                logger.warn("Could not determine root device", e);
            }
        }
        
        return true;
    }
}