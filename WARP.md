# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Repository Overview

This is a cross-platform JavaFX application for securely wiping storage drives with digital proof of erasure, developed for Smart India Hackathon 2025. It provides industry-standard data wiping capabilities with certificate generation.

## Common Development Commands

### Build & Package
```bash
# Clean and compile with tests
mvn clean compile test

# Full build with packaging
mvn clean package

# Quick build using provided scripts
# On Windows:
.\build.bat
# On Linux/Mac:
./build.sh

# Build platform-specific executables
mvn clean package -Pwindows  # Creates .exe on Windows
mvn clean package -Plinux    # Linux-specific builds
```

### Development & Testing
```bash
# Run the application in development mode
mvn javafx:run

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DriveDetectorTest

# Run with test coverage
mvn test jacoco:report

# Run integration tests (use with extreme caution!)
mvn test -Dtest=*IntegrationTest
```

### Running the Application
```bash
# Run the packaged JAR
java -jar target/secure-data-wipe-tool-1.0.0.jar

# Run with debug logging
java -Dlogging.level.com.hackathon.securewipe=DEBUG -jar target/secure-data-wipe-tool-1.0.0.jar

# Run with custom logback config
java -Dlogback.configurationFile=debug-logback.xml -jar target/secure-data-wipe-tool-1.0.0.jar
```

## High-Level Architecture

### Core Components Architecture

The application follows a layered architecture with four main components:

#### 1. SecureDataWipeApplication (Entry Point)
- JavaFX Application class that initializes the UI
- Loads FXML layout and CSS styling
- Entry point: `com.hackathon.securewipe.SecureDataWipeApplication`

#### 2. MainController (UI Layer)
- JavaFX controller managing the one-click interface
- Orchestrates the entire wipe workflow using JavaFX Tasks for threading
- Handles drive selection, method choice, progress tracking, and certificate saving
- Uses asynchronous operations to keep UI responsive during long-running wipe operations

#### 3. DriveDetector (Platform Abstraction)
- Cross-platform drive detection using JNA (Java Native Access)
- **Windows**: Uses Win32 API calls (`Kernel32.INSTANCE`) for drive enumeration and volume information
- **Linux**: Parses `/proc/partitions` and `/sys` filesystem for block device detection
- Provides safety checks to prevent system drive wiping
- Returns `DriveInfo` objects with path, label, size, type, and serial number

#### 4. SecureWipeEngine (Core Engine)
- Platform-specific secure wiping implementations
- **Windows**: Uses PowerShell `Format-Volume` + `cipher /w:` commands
- **Linux**: Uses `shred`, `dd`, and `umount` commands
- Supports multiple wipe methods: Single Pass, DoD 3-Pass, Gutmann 35-Pass
- Returns detailed `WipeResult` with timestamps, bytes wiped, and success status

#### 5. CertificateGenerator (Proof Generation)
- Generates cryptographically signed certificates using RSA-2048 keys
- Creates both PDF (with QR codes) and JSON certificates
- Uses Apache PDFBox for PDF generation and ZXing for QR codes
- Provides tamper-evident digital signatures for legal compliance

### Cross-Platform Strategy

The application handles platform differences through:
- **JNA Integration**: Direct native system calls for accurate drive detection
- **Command Dispatch**: Platform-specific command execution (PowerShell vs bash)
- **Path Handling**: Windows drive letters (`C:\`) vs Linux device paths (`/dev/sdb`)
- **Permission Model**: Windows UAC vs Linux sudo requirements

### Threading & UI Responsiveness

- All long-running operations (drive detection, wiping, certificate generation) use JavaFX `Task` objects
- Progress updates are pushed to UI thread via `Platform.runLater()`
- Prevents UI freezing during potentially hour-long wipe operations
- Error handling propagates through Task failure callbacks

### Security Considerations

- **Drive Safety**: System drive detection prevents accidental OS wipe
- **Confirmation Dialogs**: Multiple user confirmations before destructive operations  
- **Certificate Integrity**: Private keys generated locally, never transmitted
- **Audit Trail**: Comprehensive logging with SLF4J/Logback

## Key File Locations

### Source Structure
- Main classes: `src/main/java/com/hackathon/securewipe/`
- UI resources: `src/main/resources/com/hackathon/securewipe/`
  - `main-view.fxml` - UI layout
  - `application.css` - Styling
- Configuration: `src/main/resources/logback.xml`
- Tests: `src/test/java/com/hackathon/securewipe/`

### Build Configuration
- `pom.xml` - Maven configuration with JavaFX, JNA, PDFBox, ZXing dependencies
- `build.bat` / `build.sh` - Cross-platform build scripts
- Maven profiles for Windows (Launch4j) and Linux packaging

## Dependencies & Technology Stack

### Core Dependencies
- **JavaFX 17**: UI framework with FXML support
- **JNA 5.12.1**: Native system calls (Windows Win32 API, Linux system calls)
- **Apache PDFBox 2.0.27**: PDF certificate generation
- **ZXing 3.5.1**: QR code generation for mobile verification
- **Jackson 2.15.2**: JSON certificate serialization
- **SLF4J + Logback**: Structured logging

### Build Tools
- **Maven**: Build automation with platform-specific profiles
- **Maven Shade Plugin**: Fat JAR creation with dependency bundling
- **Launch4j** (Windows): Native executable generation
- **JUnit 5 + Mockito**: Testing framework

## Development Notes

### Platform Requirements
- **Java 11+** required (uses var keyword and modern APIs)
- **Administrator/root privileges** required for drive access
- **Windows**: PowerShell and Cipher.exe availability
- **Linux**: shred, dd, umount utilities (typically pre-installed)

### Testing Strategy
- Unit tests focus on data structures and logic (not requiring elevated privileges)
- Integration tests require actual hardware and elevated privileges
- Mock-based testing for platform-specific components
- Manual testing checklist includes cross-platform drive detection and UI responsiveness

### Certificate Generation Details
- RSA-2048 key pairs generated at runtime
- SHA-256 signing algorithm for future-proof security
- QR codes contain certificate ID and public key hash for mobile verification
- JSON format enables programmatic certificate validation

### Safety & Compliance Features
- System drive detection prevents accidental OS destruction
- Multiple confirmation dialogs with detailed drive information
- Certificate generation provides legal proof of secure data destruction
- Comprehensive audit logging for compliance requirements

## Troubleshooting Common Issues

### Drive Detection Issues
- Ensure elevated privileges (Administrator/root)
- Check if drives are unmounted and not in use
- Verify JNA platform libraries are included in classpath

### JavaFX Runtime Issues  
- JavaFX is bundled via Maven dependencies for Java 11+
- Check `module-info.java` is not conflicting with classpath setup

### Certificate Generation Failures
- Verify write permissions to output directory
- Ensure adequate disk space for large certificates
- Check antivirus software is not interfering with PDF generation

### Cross-Platform Path Issues
- Windows uses drive letters (`C:\`) while Linux uses device paths (`/dev/sdb`)
- Path separator differences handled by `File.separator`
- Use `Paths.get()` for robust path construction