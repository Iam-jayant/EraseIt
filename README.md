# EraseIt - Ultra-Fast USB Drive Wiper

**High-Performance Secure Data Destruction Tool**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-11%2B-blue)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-orange)](https://openjfx.io/)

> **Made by Jayant**

> **⚠️ WARNING**: This tool PERMANENTLY destroys data. Always verify you're targeting the correct device before wiping.

---

## 📝 Description

EraseIt is a powerful, lightning-fast USB drive wiping tool that securely erases all data from removable storage devices in just **2-5 minutes** (compared to traditional methods that take 1+ hours). Built with JavaFX and optimized for Windows, this tool provides a user-friendly interface for permanent data destruction.

### Key Features:

- **⚡ Ultra-Fast**: Wipes a 16GB USB drive in 2-5 minutes
- **🔒 Secure**: Deletes all files and overwrites data with zeros
- **🛡️ Safe**: Built-in protections prevent accidental system drive wipes
- **📊 Real-Time Monitoring**: Live progress, speed (MB/s), and ETA display
- **📜 Digital Certificates**: Generates proof of data destruction
- **🔧 Auto-Format**: Drive is immediately usable after wipe

---

## 🛠️ How It Works

EraseIt uses a **4-step optimized wiping process**:

### Step 1: File Deletion
- Recursively scans and deletes ALL files and folders on the USB drive
- Force-deletes locked or read-only files
- Reports progress: "Deleted X items (Y MB)"

### Step 2: Quick Format
- Uses Windows `Format-Volume` command to reset the filesystem
- Wipes file allocation tables and directory structures
- Takes only 5-10 seconds

### Step 3: Zero-Fill Overwrite
- Writes zeros to the entire drive using **64MB buffers**
- Uses Direct ByteBuffer for maximum I/O performance
- Overwrites all data sectors to prevent recovery
- Real-time speed monitoring (MB/s)

### Step 4: Final Format
- Formats the drive again to ensure it's immediately usable
- Creates a clean NTFS filesystem
- Drive is ready for new files without manual formatting

### Technical Optimizations:

- **64MB Buffer Size**: 4x larger than standard (16MB) for faster writes
- **Direct I/O**: Uses `ByteBuffer.allocateDirect()` for native memory access
- **Single File Strategy**: Writes one large file instead of many small ones
- **Force Sync**: Ensures all data is written to physical media
- **Error Handling**: Handles locked files gracefully

**Result**: All data is permanently destroyed and unrecoverable.

---

## 🚀 How to Use

### Prerequisites:

- **Java 11 or higher** installed
- **Maven** installed (for building from source)
- **Windows OS** (Windows 10/11)
- **Administrator privileges** (required for drive access)

### Quick Start:

#### Option 1: Run with Maven (Recommended)

```bash
# Navigate to project directory
cd EraseIt

# Run the application
mvn javafx:run
```

#### Option 2: Run the Batch File

```bash
# Double-click or run from command line
launch-optimized.bat
```

#### Option 3: Build JAR and Run

```bash
# Build the project
mvn clean package

# Run the JAR
java -jar target/secure-data-wipe-tool-1.0.0.jar
```

### Step-by-Step Usage:

1. **Launch the Application**
   - Run using one of the methods above
   - The application window will open

2. **Select Your USB Drive**
   - The app will auto-detect all drives
   - Select your USB drive from the dropdown
   - Look for the green "✅ SAFE - Removable USB drive" indicator
   - **WARNING**: System drives (C:\) are blocked and cannot be selected

3. **Choose Wipe Method**
   - **⚡ Ultra Fast Format + Zero** (RECOMMENDED - 2-5 min)
   - Or: 🚀 Quick Secure Wipe (2-5 min)
   - Or: NIST DoD 3-Pass (15-30 min for extra security)

4. **Start the Wipe**
   - Click the **"Start Wipe"** button
   - Type `OPTIMIZE` in the confirmation dialog (case-sensitive)
   - Click OK to confirm

5. **Monitor Progress**
   - Watch real-time progress percentage
   - View current speed (MB/s)
   - See estimated time remaining (ETA)
   - Read step-by-step status updates

6. **Completion**
   - Status will show: "✅ Ultra-fast wipe completed! Drive is ready to use."
   - A digital certificate is automatically generated
   - Click **"Save Certificate"** to save the proof (optional)
   - Your USB drive is now empty and ready for use!

### Expected Performance:

| USB Drive | Size | Wipe Time | Speed |
|-----------|------|-----------|-------|
| USB 2.0 | 16GB | 3-5 min | ~50-80 MB/s |
| USB 3.0 | 16GB | 1-3 min | ~100-200 MB/s |
| USB 3.0 | 32GB | 3-7 min | ~100-200 MB/s |

---

## 🛡️ Safety Features

- **System Drive Protection**: Cannot wipe C:\ or Windows system drives
- **USB-Only Mode**: Only removable USB drives can be wiped (for extra safety)
- **Confirmation Required**: Must type "OPTIMIZE" to proceed
- **Visual Indicators**: 
  - ✅ SAFE - Removable USB drive
  - ❌ SYSTEM DRIVE - Cannot wipe (contains Windows OS)
- **Error Reporting**: Clear error messages for locked files

---

## 📚 Project Structure

```
EraseIt/
├── src/
│   ├── main/java/com/hackathon/securewipe/
│   │   ├── OptimizedSecureDataWipeApplication.java  # Main entry point
│   │   ├── OptimizedMainController.java            # UI controller
│   │   ├── OptimizedSecureWipeEngine.java           # Core wiping logic
│   │   ├── DriveDetector.java                      # Drive detection
│   │   └── CertificateGenerator.java               # Certificate generation
│   └── main/resources/
│       ├── com/hackathon/securewipe/
│       │   └── optimized-styles.css                # UI styling
│       ├── application.properties
│       └── logback.xml                             # Logging config
├── scripts/
│   ├── launch-optimized.ps1                      # PowerShell launcher
│   └── build.bat                                 # Build script
├── launch-optimized.bat                          # Quick launcher
├── pom.xml                                       # Maven configuration
└── README.md                                     # This file
```

---

## ⚙️ Technical Details

### Technology Stack:

- **Java 11+**: Core programming language
- **JavaFX 17**: User interface framework
- **Maven**: Build and dependency management
- **JNA**: Native system calls for drive access
- **Apache PDFBox**: PDF certificate generation
- **ZXing**: QR code generation
- **Jackson**: JSON processing
- **Logback**: Logging framework

### Wipe Methods Available:

1. **⚡ Ultra Fast Format + Zero** (RECOMMENDED)
   - 1 pass
   - Deletes all files + Format + Zero-fill + Final format
   - Time: 2-5 minutes
   - Security: High (data unrecoverable)

2. **🚀 Quick Secure Wipe**
   - 1 pass
   - Deletes all files + Zero-fill + Format
   - Time: 2-5 minutes
   - Security: High (data unrecoverable)

3. **NIST DoD 3-Pass**
   - 3 passes (0xFF, 0x00, Random)
   - Time: 15-30 minutes
   - Security: Very High (military standard)

4. **NIST Gutmann 35-Pass**
   - 35 passes with specific patterns
   - Time: 2-4 hours
   - Security: Maximum (overkill for most users)

---

## 📝 License

MIT License - Free to use, modify, and distribute.

---

## ⚠️ Important Notes

- **Data Loss**: This tool PERMANENTLY destroys data. There is NO undo.
- **Backups**: Always have backups before wiping any drive.
- **Target Drive**: Double-check you've selected the correct drive.
- **Locked Files**: Some Windows system files may not delete (this is normal).
- **Performance**: Actual speed depends on USB drive quality and USB port type (2.0 vs 3.0).

---

## 👤 Author

**Made by Jayant**

---

## 🚀 Quick Example

```bash
# 1. Navigate to project
cd EraseIt

# 2. Run the application
mvn javafx:run

# 3. In the UI:
#    - Select USB drive from dropdown
#    - Choose "Ultra Fast Format + Zero"
#    - Click "Start Wipe"
#    - Type "OPTIMIZE" to confirm
#    - Wait 2-5 minutes
#    - Done! Drive is wiped and ready to use
```

---

**Enjoy fast and secure USB drive wiping!** ⚡
