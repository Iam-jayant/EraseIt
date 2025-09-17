# EraseIt - Secure Data Wipe Tool

## Smart India Hackathon 2025 - MVP Submission

> **Repository**: This project is hosted on GitHub as "EraseIt" - a professional data erasure solution.

A cross-platform application for securely wiping USB drives and laptop hard drives with digital proof of erasure. This tool provides industry-standard data wiping capabilities with an intuitive one-click interface and generates digitally signed certificates as proof of successful erasure.

## 🚀 Features

### Core Functionality
- **Cross-Platform Support**: Works on both Windows and Linux
- **Multiple Drive Types**: Supports USB drives, hard drives, and SSDs
- **Industry-Standard Wiping**: Uses `shred`, `dd` on Linux and PowerShell/Cipher on Windows
- **One-Click Interface**: Simple, intuitive JavaFX GUI
- **Digital Certificates**: Generates signed PDF and JSON certificates with QR codes
- **Offline Operation**: No internet or cloud dependencies required
- **Portable Deployment**: Can be packaged as standalone JAR or installed application

### Security Features
- **Multiple Wipe Methods**:
  - Single Pass (Zeros)
  - DoD 3-Pass Standard
  - Gutmann 35-Pass (for maximum security)
  - Platform Default (optimized for each OS)
- **Digital Signatures**: RSA-2048 signing for certificate authenticity
- **QR Code Verification**: Quick verification of certificate integrity
- **Safety Checks**: Prevents accidental system drive wipes

### Certificate Generation
- **PDF Certificates**: Professional-looking certificates with all operation details
- **JSON Export**: Machine-readable certificate data
- **Digital Signatures**: Cryptographically signed with RSA-2048
- **Device Information**: Serial numbers, paths, sizes, and timestamps
- **QR Codes**: For quick mobile verification

## 🛠️ Technology Stack

- **Backend**: Java 11+
- **UI Framework**: JavaFX 17
- **Native Integration**: JNA (Java Native Access)
- **PDF Generation**: Apache PDFBox
- **QR Codes**: ZXing (Zebra Crossing)
- **JSON Processing**: Jackson
- **Logging**: SLF4J + Logback
- **Build Tool**: Maven
- **Packaging**: Maven Shade Plugin, Launch4j (Windows)

## 📋 Requirements

### System Requirements
- **Java**: JDK/JRE 11 or higher
- **Memory**: Minimum 512MB RAM
- **Storage**: 100MB free space
- **Privileges**: Administrator/root access for drive operations

### Platform-Specific Requirements

#### Windows
- Windows 10/11 (recommended)
- PowerShell (for secure wipe operations)
- Administrator privileges

#### Linux
- Modern Linux distribution (Ubuntu 18.04+, CentOS 7+, etc.)
- `shred` or `dd` utilities (usually pre-installed)
- Root privileges

## 🚀 Installation & Setup

### Option 1: Download Pre-built Release
1. Download the latest release from the releases section
2. For Windows: Run `SecureDataWipeTool.exe`
3. For Linux: Run `java -jar secure-data-wipe-tool.jar`

### Option 2: Build from Source

#### Prerequisites
```bash
# Install Java 11+ and Maven
# Windows (using Chocolatey)
choco install openjdk11 maven

# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk maven

# CentOS/RHEL
sudo yum install java-11-openjdk-devel maven
```

#### Build Steps
```bash
# Clone the repository
git clone <repository-url>
cd SecureDataWipeTool

# Build the project
mvn clean package

# Run the application
java -jar target/secure-data-wipe-tool-1.0.0.jar
```

### Option 3: Development Setup
```bash
# Clone and setup for development
git clone <repository-url>
cd SecureDataWipeTool

# Install dependencies
mvn dependency:resolve

# Run in development mode
mvn javafx:run

# Or run with IDE
# Import as Maven project in IntelliJ IDEA, Eclipse, or VS Code
```

## 📖 Usage Guide

### Basic Operation

1. **Launch the Application**
   - Windows: Double-click the executable or run from command line
   - Linux: Run with `java -jar` command or use the desktop launcher

2. **Select Target Drive**
   - Click "Refresh" to detect available drives
   - Select the drive you want to wipe from the dropdown
   - **⚠️ WARNING**: Double-check the selection as this operation is irreversible

3. **Choose Wipe Method**
   - **Single Pass (Zeros)**: Fast, suitable for modern SSDs
   - **DoD 3-Pass**: Department of Defense standard
   - **Gutmann 35-Pass**: Maximum security for sensitive data
   - **Platform Default**: Recommended for general use

4. **Start Wiping Process**
   - Click "Start Secure Wipe"
   - Confirm the operation in the warning dialog
   - Monitor progress in the progress bar and log area

5. **Save Certificate**
   - After successful completion, click "Save Certificate"
   - Choose a location to save the PDF and JSON certificates
   - Certificates include QR codes for mobile verification

### Command Line Usage (Advanced)
```bash
# Run with specific parameters
java -jar secure-data-wipe-tool.jar --cli --device /dev/sdb --method dod3

# View help
java -jar secure-data-wipe-tool.jar --help
```

## 🔧 Configuration

### Logging Configuration
The application uses Logback for logging. Configuration is in `src/main/resources/logback.xml`:
- Console output for immediate feedback
- Rolling file logs in `logs/` directory
- Adjustable log levels per package

### Certificate Configuration
Certificates are generated with:
- RSA-2048 bit keys for signing
- SHA-256 hashing algorithm
- PDF with embedded QR codes
- JSON with full metadata

## 🏗️ Architecture

### Project Structure
```
SecureDataWipeTool/
├── src/
│   ├── main/
│   │   ├── java/com/hackathon/securewipe/
│   │   │   ├── SecureDataWipeApplication.java    # Main JavaFX application
│   │   │   ├── MainController.java               # UI controller
│   │   │   ├── DriveDetector.java                # Cross-platform drive detection
│   │   │   ├── SecureWipeEngine.java             # Wipe operation engine
│   │   │   └── CertificateGenerator.java         # Digital certificate generation
│   │   └── resources/
│   │       ├── com/hackathon/securewipe/
│   │       │   ├── main-view.fxml                # UI layout
│   │       │   └── application.css               # Styling
│   │       └── logback.xml                       # Logging configuration
│   └── test/
│       └── java/                                 # Unit tests
├── target/                                       # Build output
├── pom.xml                                       # Maven configuration
└── README.md                                     # This file
```

### Key Components

#### DriveDetector
- Cross-platform drive enumeration
- Uses JNA for native OS integration
- Identifies drive types (USB, HDD, SSD)
- Safety checks for system drives

#### SecureWipeEngine
- Platform-specific wipe implementations
- Progress tracking and reporting
- Error handling and recovery
- Multiple wiping algorithms

#### CertificateGenerator
- Digital signature generation (RSA-2048)
- PDF creation with detailed information
- QR code generation for verification
- JSON export for programmatic access

## 🔒 Security Considerations

### Wipe Effectiveness
- **SSDs**: Single pass sufficient due to wear leveling
- **HDDs**: Multiple passes recommended for sensitive data
- **USB Drives**: Single pass typically adequate
- **Secure Deletion**: Uses cryptographically secure random data

### Certificate Security
- Private keys generated locally (never transmitted)
- RSA-2048 with SHA-256 for future-proof security
- Certificates include tamper-evident signatures
- QR codes enable offline verification

### Safety Measures
- System drive detection and blocking
- Multiple confirmation dialogs
- Operation logging and audit trails
- Recovery warnings and user education

## 🧪 Testing

### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DriveDetectorTest

# Run with coverage
mvn test jacoco:report
```

### Integration Testing
```bash
# Test on actual hardware (use with caution!)
mvn test -Dtest=*IntegrationTest

# Test with mock drives
mvn test -Dprofile=mock-drives
```

### Manual Testing Checklist
- [ ] Drive detection on Windows and Linux
- [ ] Safe mode operation (no actual wiping)
- [ ] Certificate generation and validation
- [ ] UI responsiveness during operations
- [ ] Error handling and recovery

## 📦 Deployment

### Packaging Options

#### Standalone JAR
```bash
mvn clean package
# Output: target/secure-data-wipe-tool-1.0.0.jar
```

#### Windows Executable
```bash
mvn clean package -Pwindows
# Output: target/SecureDataWipeTool.exe
```

#### Linux Package
```bash
# Debian/Ubuntu package
mvn clean package -Pdebian
# Output: target/secure-data-wipe-tool_1.0.0_amd64.deb

# RPM package
mvn clean package -Prpm
# Output: target/secure-data-wipe-tool-1.0.0.x86_64.rpm
```

#### Bootable ISO (Future Enhancement)
```bash
# Create bootable environment with Tiny Core Linux
./scripts/create-bootable-iso.sh
```

## 🐛 Troubleshooting

### Common Issues

#### Drive Not Detected
- **Windows**: Run as Administrator
- **Linux**: Run with `sudo` or as root
- Check if drive is mounted and unmount if necessary
- Verify drive is not in use by other applications

#### Permission Denied
- Ensure application is run with elevated privileges
- On Linux, add user to `disk` group (not recommended for security)
- Check SELinux/AppArmor policies

#### JavaFX Issues
- Ensure JavaFX runtime is available
- For Java 11+, JavaFX is separate and included in the build
- Check Java version compatibility

#### Certificate Generation Fails
- Verify write permissions to output directory
- Check available disk space
- Ensure no antivirus interference

### Debug Mode
```bash
# Enable debug logging
java -Dlogging.level.com.hackathon.securewipe=DEBUG -jar secure-data-wipe-tool.jar

# Full debug mode
java -Dlogback.configurationFile=debug-logback.xml -jar secure-data-wipe-tool.jar
```

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make changes and test thoroughly
4. Commit with clear messages (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

### Code Style
- Follow Oracle Java coding conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Include unit tests for new functionality

### Testing Requirements
- All new code must have unit tests
- Integration tests for platform-specific features
- Manual testing on both Windows and Linux

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚠️ Disclaimer

**IMPORTANT WARNING**: This tool permanently destroys data. Users are solely responsible for:
- Verifying the correct target drive selection
- Ensuring proper backups of important data
- Understanding the irreversible nature of the operation
- Complying with local laws and regulations regarding data destruction

The developers assume no liability for data loss or misuse of this tool.

## 📞 Support

For technical support or questions:
- Create an issue in the GitHub repository
- Email: [your-email@example.com]
- Documentation: [Wiki link]

## 🏆 Smart India Hackathon 2025

This project was developed for the Smart India Hackathon 2025 with the following objectives:
- **Problem Statement**: Secure data destruction with digital proof
- **Innovation**: Cross-platform, offline-capable solution
- **Impact**: Supports data privacy compliance and secure device disposal
- **Technology**: Modern Java stack with native platform integration

### Team Information
- **Team Name**: [Your Team Name]
- **Institution**: [Your Institution]
- **Hackathon Track**: Cybersecurity & Data Privacy

## 🔄 Version History

### v1.0.0 (Current)
- Initial MVP release
- Cross-platform drive detection
- Multiple wipe methods
- Digital certificate generation
- JavaFX GUI with modern design

### Planned Features (v1.1.0)
- Command-line interface
- Bootable ISO creation
- Advanced scheduling
- Remote operation capabilities
- Enhanced certificate formats

---

**Made with ❤️ for Smart India Hackathon 2025**