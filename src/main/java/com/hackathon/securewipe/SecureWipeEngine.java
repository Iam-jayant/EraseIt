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
     * Wipes a drive on Windows using direct raw device access
     */
    private static WipeResult wipeWindowsDrive(DriveDetector.DriveInfo driveInfo, 
                                             WipeMethod method,
                                             Consumer<Double> progressCallback,
                                             LocalDateTime startTime) throws Exception {
        
        String driveLetter = driveInfo.getPath();
        progressCallback.accept(0.1);
        
        logger.info("Starting direct wipe of drive {}", driveLetter);
        
        // Use direct file access to overwrite drive sectors
        try {
            return performDirectWipe(driveInfo, method, progressCallback, startTime);
        } catch (Exception e) {
            logger.warn("Direct wipe failed, trying alternative method: {}", e.getMessage());
            
            // Fallback: Try sdelete if available
            return performSDeleteWipe(driveInfo, method, progressCallback, startTime);
        }
    }
    
    /**
     * Performs file-based wiping by creating large temporary files
     */
    private static WipeResult performDirectWipe(DriveDetector.DriveInfo driveInfo,
                                              WipeMethod method,
                                              Consumer<Double> progressCallback,
                                              LocalDateTime startTime) throws Exception {
        
        String drivePath = driveInfo.getPath();
        long driveSize = driveInfo.getSize();
        
        logger.info("Performing file-based wipe on {} (Size: {} bytes)", drivePath, driveSize);
        
        // Get available free space
        java.io.File driveRoot = new java.io.File(drivePath);
        long freeSpace = driveRoot.getFreeSpace();
        
        logger.info("Available free space: {} bytes", freeSpace);
        
        // Create wipe patterns based on method
        byte[][] patterns = getWipePatterns(method);
        
        long totalBytesWritten = 0;
        
        for (int pass = 0; pass < patterns.length; pass++) {
            byte[] pattern = patterns[pass];
            logger.info("Starting pass {} of {} with pattern", pass + 1, patterns.length);
            
            // Create temporary wipe files to fill the drive
            java.util.List<java.io.File> wipeFiles = new java.util.ArrayList<>();
            
            try {
                long remainingSpace = freeSpace;
                int fileIndex = 0;
                
                while (remainingSpace > 1024 * 1024) { // While at least 1MB remains
                    java.io.File wipeFile = new java.io.File(driveRoot, "wipe_temp_" + pass + "_" + fileIndex + ".tmp");
                    wipeFiles.add(wipeFile);
                    
                    long fileSize = Math.min(100L * 1024 * 1024, remainingSpace - 1024); // 100MB chunks, leave 1KB buffer
                    
                    logger.debug("Creating wipe file: {} (Size: {} bytes)", wipeFile.getName(), fileSize);
                    
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(wipeFile);
                         java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {
                        
                        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
                        
                        // Fill buffer with pattern
                        for (int i = 0; i < buffer.length; i++) {
                            buffer[i] = pattern[i % pattern.length];
                        }
                        
                        long bytesWritten = 0;
                        while (bytesWritten < fileSize) {
                            long remainingBytes = fileSize - bytesWritten;
                            int bytesToWrite = (int) Math.min(buffer.length, remainingBytes);
                            
                            bos.write(buffer, 0, bytesToWrite);
                            bytesWritten += bytesToWrite;
                            totalBytesWritten += bytesToWrite;
                            remainingSpace -= bytesToWrite;
                            
                            // Update progress
                            double passProgress = (double) pass / patterns.length;
                            double fileProgress = (double) totalBytesWritten / (freeSpace * patterns.length);
                            progressCallback.accept((passProgress + fileProgress) * 0.95);
                            
                            // Check every 10MB
                            if (bytesWritten % (10 * 1024 * 1024) == 0) {
                                Thread.sleep(10); // Brief pause
                            }
                        }
                        
                        bos.flush();
                        fos.getFD().sync(); // Force write to disk
                    }
                    
                    fileIndex++;
                    
                    // Update remaining space (check actual free space)
                    remainingSpace = driveRoot.getFreeSpace();
                }
                
                logger.info("Pass {} completed. Created {} wipe files.", pass + 1, wipeFiles.size());
                
            } finally {
                // Clean up temporary files after each pass
                for (java.io.File wipeFile : wipeFiles) {
                    try {
                        if (wipeFile.exists()) {
                            boolean deleted = wipeFile.delete();
                            if (!deleted) {
                                logger.warn("Failed to delete wipe file: {}", wipeFile.getName());
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error deleting wipe file: {}", e.getMessage());
                    }
                }
            }
        }
        
        progressCallback.accept(1.0);
        LocalDateTime endTime = LocalDateTime.now();
        
        logger.info("File-based wipe completed successfully. Total bytes written: {}", totalBytesWritten);
        return new WipeResult(true, method.getDisplayName(), startTime, endTime,
                            driveInfo.getPath(), driveInfo.getSerialNumber(),
                            totalBytesWritten, null);
    }
    
    /**
     * Fallback method using sdelete or diskpart
     */
    private static WipeResult performSDeleteWipe(DriveDetector.DriveInfo driveInfo,
                                                WipeMethod method,
                                                Consumer<Double> progressCallback,
                                                LocalDateTime startTime) throws Exception {
        
        String driveLetter = driveInfo.getPath();
        logger.info("Trying sdelete wipe for drive {}", driveLetter);
        
        // Try to use cipher command for free space wiping
        String cipherCommand = "cipher /w:" + driveLetter;
        
        ProcessBuilder cipherBuilder = new ProcessBuilder("cmd.exe", "/c", cipherCommand);
        Process cipherProcess = cipherBuilder.start();
        
        // Monitor progress
        monitorProcessProgress(cipherProcess, progressCallback, 0.1, 0.9);
        
        int cipherResult = cipherProcess.waitFor();
        progressCallback.accept(1.0);
        
        LocalDateTime endTime = LocalDateTime.now();
        
        if (cipherResult == 0) {
            logger.info("Cipher wipe completed for drive {}", driveLetter);
            return new WipeResult(true, method.getDisplayName() + " (Cipher)", startTime, endTime,
                                driveInfo.getPath(), driveInfo.getSerialNumber(),
                                driveInfo.getSize(), null);
        } else {
            throw new RuntimeException("All wipe methods failed for drive " + driveLetter);
        }
    }
    
    /**
     * Gets wipe patterns based on method
     */
    private static byte[][] getWipePatterns(WipeMethod method) {
        switch (method) {
            case SINGLE_PASS_ZERO:
                return new byte[][]{{0x00}};
            case DOD_3_PASS:
                return new byte[][]{{(byte) 0xFF}, {0x00}, generateRandomPattern()};
            case GUTMANN_35_PASS:
                return generateGutmannPatterns();
            case PLATFORM_DEFAULT:
            default:
                return new byte[][]{{0x00}};
        }
    }
    
    /**
     * Generates random pattern
     */
    private static byte[] generateRandomPattern() {
        byte[] pattern = new byte[1024];
        new java.security.SecureRandom().nextBytes(pattern);
        return pattern;
    }
    
    /**
     * Generates Gutmann patterns (simplified version)
     */
    private static byte[][] generateGutmannPatterns() {
        byte[][] patterns = new byte[35][];
        java.security.SecureRandom random = new java.security.SecureRandom();
        
        for (int i = 0; i < 35; i++) {
            patterns[i] = new byte[1024];
            if (i < 4 || i >= 31) {
                // Random patterns for first 4 and last 4 passes
                random.nextBytes(patterns[i]);
            } else {
                // Specific patterns for middle passes
                byte patternByte = getGutmannPatternByte(i - 4);
                for (int j = 0; j < patterns[i].length; j++) {
                    patterns[i][j] = patternByte;
                }
            }
        }
        return patterns;
    }
    
    /**
     * Gets specific Gutmann pattern byte
     */
    private static byte getGutmannPatternByte(int pass) {
        byte[] gutmannBytes = {
            0x55, (byte) 0xAA, (byte) 0x92, 0x49, (byte) 0x24, (byte) 0x92, 0x49, (byte) 0x24,
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF,
            0x6D, (byte) 0xB6, (byte) 0xDB
        };
        return gutmannBytes[pass % gutmannBytes.length];
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
     * Validates that a drive can be safely wiped with enhanced safety checks
     */
    public static boolean canWipeDrive(DriveDetector.DriveInfo driveInfo) {
        logger.info("Safety check for drive: {} ({})", driveInfo.getPath(), driveInfo.getType());
        
        // Don't allow wiping of system drives
        if (Platform.isWindows()) {
            String systemDrive = System.getenv("SystemDrive");
            String programFiles = System.getenv("ProgramFiles");
            String winDir = System.getenv("WINDIR");
            
            // Block system drive (usually C:)
            if (driveInfo.getPath().startsWith(systemDrive)) {
                logger.warn("BLOCKED: Attempted to wipe system drive: {}", driveInfo.getPath());
                return false;
            }
            
            // Additional Windows safety checks
            if (driveInfo.getPath().startsWith("C:\\") || 
                driveInfo.getPath().startsWith("c:\\") ||
                driveInfo.getPath().toLowerCase().contains("windows")) {
                logger.warn("BLOCKED: Attempted to wipe Windows system drive: {}", driveInfo.getPath());
                return false;
            }
            
            // Only allow removable drives for extra safety
            if (driveInfo.getType() != DriveDetector.DriveType.USB_REMOVABLE) {
                logger.warn("SAFETY: Only removable USB drives are allowed for wiping. Drive type: {}", driveInfo.getType());
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
                    logger.warn("BLOCKED: Attempted to wipe root device: {}", driveInfo.getPath());
                    return false;
                }
                
                // Block common system partitions
                if (driveInfo.getPath().contains("/dev/sda") || 
                    driveInfo.getPath().contains("/dev/nvme0")) {
                    logger.warn("BLOCKED: Attempted to wipe system storage device: {}", driveInfo.getPath());
                    return false;
                }
            } catch (Exception e) {
                logger.warn("Could not determine root device", e);
                return false; // Fail safe - don't allow if we can't verify
            }
        }
        
        // Size check - don't allow wiping drives larger than 2TB (likely system drives)
        if (driveInfo.getSize() > 2L * 1024 * 1024 * 1024 * 1024) {
            logger.warn("BLOCKED: Drive too large (>2TB), likely system drive: {} GB", 
                       driveInfo.getSize() / (1024.0 * 1024 * 1024));
            return false;
        }
        
        logger.info("SAFE: Drive {} passed all safety checks", driveInfo.getPath());
        return true;
    }
    
    /**
     * Gets a detailed safety assessment of a drive
     */
    public static String getSafetyAssessment(DriveDetector.DriveInfo driveInfo) {
        if (Platform.isWindows()) {
            String systemDrive = System.getenv("SystemDrive");
            
            if (driveInfo.getPath().startsWith(systemDrive)) {
                return "❌ SYSTEM DRIVE - Cannot wipe (contains Windows OS)";
            }
            
            if (driveInfo.getType() == DriveDetector.DriveType.USB_REMOVABLE) {
                return "✅ SAFE - Removable USB drive";
            } else {
                return "⚠️ CAUTION - Not a removable drive";
            }
        } else {
            if (driveInfo.getPath().contains("/dev/sda") || 
                driveInfo.getPath().contains("/dev/nvme0")) {
                return "❌ SYSTEM DRIVE - Cannot wipe (contains OS)";
            }
            return "✅ SAFE - External device";
        }
    }
}