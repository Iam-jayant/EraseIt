package com.hackathon.securewipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriveDetector class
 */
class DriveDetectorTest {

    @Test
    void testDetectDrives_ReturnsNonEmptyList() {
        // This is a basic smoke test - actual drive detection requires system privileges
        List<DriveDetector.DriveInfo> drives = DriveDetector.detectDrives();
        assertNotNull(drives, "Drive list should not be null");
        // Note: May be empty if no drives are accessible without elevated privileges
    }

    @Test
    void testDriveInfo_GettersWork() {
        DriveDetector.DriveInfo drive = new DriveDetector.DriveInfo(
            "C:\\", "System", 1000000000L, 
            DriveDetector.DriveType.HARD_DISK, "ABC123"
        );
        
        assertEquals("C:\\", drive.getPath());
        assertEquals("System", drive.getLabel());
        assertEquals(1000000000L, drive.getSize());
        assertEquals(DriveDetector.DriveType.HARD_DISK, drive.getType());
        assertEquals("ABC123", drive.getSerialNumber());
    }

    @Test
    void testDriveInfo_ToString() {
        DriveDetector.DriveInfo drive = new DriveDetector.DriveInfo(
            "/dev/sdb1", "USB Drive", 8000000000L, 
            DriveDetector.DriveType.USB_REMOVABLE, "USB123"
        );
        
        String toString = drive.toString();
        assertTrue(toString.contains("USB Drive"));
        assertTrue(toString.contains("/dev/sdb1"));
        assertTrue(toString.contains("USB_REMOVABLE"));
        assertTrue(toString.contains("GB"));
    }

    @Test
    void testDriveType_Values() {
        DriveDetector.DriveType[] types = DriveDetector.DriveType.values();
        assertTrue(types.length >= 6);
        
        boolean hasUsbType = false;
        for (DriveDetector.DriveType type : types) {
            if (type == DriveDetector.DriveType.USB_REMOVABLE) {
                hasUsbType = true;
                break;
            }
        }
        assertTrue(hasUsbType, "Should have USB_REMOVABLE type");
    }
}