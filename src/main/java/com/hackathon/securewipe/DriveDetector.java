package com.hackathon.securewipe;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cross-platform drive detection utility using JNA for native system calls
 * Detects USB drives, hard drives, and other storage devices on Windows and Linux
 */
public class DriveDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(DriveDetector.class);
    
    /**
     * Represents a detected storage drive
     */
    public static class DriveInfo {
        private final String path;
        private final String label;
        private final long size;
        private final DriveType type;
        private final String serialNumber;
        
        public DriveInfo(String path, String label, long size, DriveType type, String serialNumber) {
            this.path = path;
            this.label = label;
            this.size = size;
            this.type = type;
            this.serialNumber = serialNumber;
        }
        
        public String getPath() { return path; }
        public String getLabel() { return label; }
        public long getSize() { return size; }
        public DriveType getType() { return type; }
        public String getSerialNumber() { return serialNumber; }
        
        @Override
        public String toString() {
            return String.format("%s (%s) - %s - %.2f GB", 
                label.isEmpty() ? "Unnamed" : label, 
                path, 
                type, 
                size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    public enum DriveType {
        USB_REMOVABLE,
        HARD_DISK,
        SSD,
        OPTICAL,
        NETWORK,
        UNKNOWN
    }
    
    /**
     * Detects all available drives on the system
     * @return List of detected drives
     */
    public static List<DriveInfo> detectDrives() {
        logger.info("Detecting drives on {} platform", Platform.getOSType());
        
        if (Platform.isWindows()) {
            return detectWindowsDrives();
        } else if (Platform.isLinux() || Platform.isMac()) {
            return detectLinuxDrives();
        } else {
            logger.warn("Unsupported platform: {}", Platform.getOSType());
            return new ArrayList<>();
        }
    }
    
    /**
     * Detects drives on Windows using Win32 API
     */
    private static List<DriveInfo> detectWindowsDrives() {
        List<DriveInfo> drives = new ArrayList<>();
        
        try {
            char[] buffer = new char[256];
            Kernel32.INSTANCE.GetLogicalDriveStrings(buffer.length, buffer);
            
            String driveStrings = new String(buffer);
            String[] driveLetters = driveStrings.split("\0");
            
            for (String driveLetter : driveLetters) {
                if (driveLetter.isEmpty()) continue;
                
                int driveType = Kernel32.INSTANCE.GetDriveType(driveLetter);
                DriveType type = mapWindowsDriveType(driveType);
                
                // Get drive info
                IntByReference sectorsPerCluster = new IntByReference();
                IntByReference bytesPerSector = new IntByReference();
                IntByReference freeClusters = new IntByReference();
                IntByReference totalClusters = new IntByReference();
                
                boolean success = Kernel32.INSTANCE.GetDiskFreeSpace(
                    driveLetter, sectorsPerCluster, bytesPerSector, freeClusters, totalClusters
                );
                
                long size = 0;
                if (success) {
                    size = (long) totalClusters.getValue() * 
                           sectorsPerCluster.getValue() * 
                           bytesPerSector.getValue();
                }
                
                // Get volume information
                char[] volumeName = new char[256];
                char[] fileSystemName = new char[256];
                IntByReference serialNumber = new IntByReference();
                IntByReference maxComponentLength = new IntByReference();
                IntByReference fileSystemFlags = new IntByReference();
                
                Kernel32.INSTANCE.GetVolumeInformation(
                    driveLetter, volumeName, volumeName.length,
                    serialNumber, maxComponentLength, fileSystemFlags,
                    fileSystemName, fileSystemName.length
                );
                
                String label = new String(volumeName).trim();
                String serial = String.format("%08X", serialNumber.getValue());
                
                drives.add(new DriveInfo(driveLetter, label, size, type, serial));
            }
            
        } catch (Exception e) {
            logger.error("Error detecting Windows drives", e);
        }
        
        return drives;
    }
    
    /**
     * Detects drives on Linux by parsing /proc and /sys
     */
    private static List<DriveInfo> detectLinuxDrives() {
        List<DriveInfo> drives = new ArrayList<>();
        
        try {
            // Read block devices from /proc/partitions
            Path partitionsPath = Paths.get("/proc/partitions");
            if (Files.exists(partitionsPath)) {
                List<String> lines = Files.readAllLines(partitionsPath);
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("major")) continue;
                    
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        String deviceName = parts[3];
                        
                        // Skip partitions (look for whole devices)
                        if (Pattern.matches(".*\\d+$", deviceName)) continue;
                        
                        long sizeKB = Long.parseLong(parts[2]);
                        long sizeBytes = sizeKB * 1024;
                        
                        String devicePath = "/dev/" + deviceName;
                        DriveType type = determineLinuxDriveType(deviceName);
                        String serial = getLinuxDeviceSerial(deviceName);
                        String label = getLinuxDeviceLabel(deviceName);
                        
                        drives.add(new DriveInfo(devicePath, label, sizeBytes, type, serial));
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error detecting Linux drives", e);
        }
        
        return drives;
    }
    
    private static DriveType mapWindowsDriveType(int windowsType) {
        switch (windowsType) {
            case WinBase.DRIVE_REMOVABLE:
                return DriveType.USB_REMOVABLE;
            case WinBase.DRIVE_FIXED:
                return DriveType.HARD_DISK;
            case WinBase.DRIVE_CDROM:
                return DriveType.OPTICAL;
            case WinBase.DRIVE_REMOTE:
                return DriveType.NETWORK;
            default:
                return DriveType.UNKNOWN;
        }
    }
    
    private static DriveType determineLinuxDriveType(String deviceName) {
        if (deviceName.startsWith("sd")) {
            // Check if it's a USB device
            Path usbPath = Paths.get("/sys/block/" + deviceName + "/removable");
            try {
                if (Files.exists(usbPath)) {
                    String removable = Files.readString(usbPath).trim();
                    if ("1".equals(removable)) {
                        return DriveType.USB_REMOVABLE;
                    }
                }
            } catch (IOException e) {
                logger.debug("Could not determine if device is removable: {}", deviceName);
            }
            
            // Check if it's an SSD
            Path rotationalPath = Paths.get("/sys/block/" + deviceName + "/queue/rotational");
            try {
                if (Files.exists(rotationalPath)) {
                    String rotational = Files.readString(rotationalPath).trim();
                    if ("0".equals(rotational)) {
                        return DriveType.SSD;
                    }
                }
            } catch (IOException e) {
                logger.debug("Could not determine if device is SSD: {}", deviceName);
            }
            
            return DriveType.HARD_DISK;
        } else if (deviceName.startsWith("sr") || deviceName.startsWith("cdrom")) {
            return DriveType.OPTICAL;
        }
        
        return DriveType.UNKNOWN;
    }
    
    private static String getLinuxDeviceSerial(String deviceName) {
        try {
            Path serialPath = Paths.get("/sys/block/" + deviceName + "/serial");
            if (Files.exists(serialPath)) {
                return Files.readString(serialPath).trim();
            }
            
            // Alternative: try udev info
            Process process = Runtime.getRuntime().exec(new String[]{"udevadm", "info", "-q", "property", "-n", "/dev/" + deviceName});
            // Parse output for ID_SERIAL_SHORT or ID_SERIAL
            // Implementation would go here
            
        } catch (Exception e) {
            logger.debug("Could not get serial for device: {}", deviceName);
        }
        
        return "Unknown";
    }
    
    private static String getLinuxDeviceLabel(String deviceName) {
        try {
            // Try to get filesystem label
            Process process = Runtime.getRuntime().exec(new String[]{"blkid", "/dev/" + deviceName});
            // Parse output for LABEL
            // Implementation would go here
            
        } catch (Exception e) {
            logger.debug("Could not get label for device: {}", deviceName);
        }
        
        return deviceName;
    }
}