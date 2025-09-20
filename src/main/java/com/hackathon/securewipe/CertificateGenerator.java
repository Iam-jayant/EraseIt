package com.hackathon.securewipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Certificate generator that creates digitally signed PDF and JSON certificates
 * proving successful completion of secure data wipe operations
 */
public class CertificateGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(CertificateGenerator.class);
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;
    
    /**
     * Certificate data structure
     */
    public static class WipeCertificate {
        private String devicePath;
        private String deviceSerial;
        private String deviceLabel;
        private long deviceSize;
        private String wipeMethod;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long bytesWiped;
        private String certificateId;
        private String digitalSignature;
        private String publicKeyHash;
        private String toolVersion;
        private String platform;
        
        // Constructors, getters, and setters
        public WipeCertificate() {}
        
        public WipeCertificate(SecureWipeEngine.WipeResult wipeResult, 
                              DriveDetector.DriveInfo driveInfo) {
            this.devicePath = wipeResult.getDevicePath();
            this.deviceSerial = wipeResult.getDeviceSerial();
            this.deviceLabel = driveInfo.getLabel();
            this.deviceSize = driveInfo.getSize();
            this.wipeMethod = wipeResult.getMethod();
            this.startTime = wipeResult.getStartTime();
            this.endTime = wipeResult.getEndTime();
            this.bytesWiped = wipeResult.getBytesWiped();
            this.certificateId = generateCertificateId();
            this.toolVersion = "1.0.0";
            this.platform = System.getProperty("os.name");
        }
        
        private String generateCertificateId() {
            return "CERT-" + System.currentTimeMillis() + "-" + 
                   Integer.toHexString(hashCode()).toUpperCase();
        }
        
        // Getters and setters
        public String getDevicePath() { return devicePath; }
        public void setDevicePath(String devicePath) { this.devicePath = devicePath; }
        
        public String getDeviceSerial() { return deviceSerial; }
        public void setDeviceSerial(String deviceSerial) { this.deviceSerial = deviceSerial; }
        
        public String getDeviceLabel() { return deviceLabel; }
        public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
        
        public long getDeviceSize() { return deviceSize; }
        public void setDeviceSize(long deviceSize) { this.deviceSize = deviceSize; }
        
        public String getWipeMethod() { return wipeMethod; }
        public void setWipeMethod(String wipeMethod) { this.wipeMethod = wipeMethod; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public long getBytesWiped() { return bytesWiped; }
        public void setBytesWiped(long bytesWiped) { this.bytesWiped = bytesWiped; }
        
        public String getCertificateId() { return certificateId; }
        public void setCertificateId(String certificateId) { this.certificateId = certificateId; }
        
        public String getDigitalSignature() { return digitalSignature; }
        public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }
        
        public String getPublicKeyHash() { return publicKeyHash; }
        public void setPublicKeyHash(String publicKeyHash) { this.publicKeyHash = publicKeyHash; }
        
        public String getToolVersion() { return toolVersion; }
        public void setToolVersion(String toolVersion) { this.toolVersion = toolVersion; }
        
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
    }
    
    private final KeyPair keyPair;
    
    /**
     * Constructor that generates or loads a key pair for signing
     */
    public CertificateGenerator() throws Exception {
        this.keyPair = generateOrLoadKeyPair();
        logger.info("Certificate generator initialized with RSA key pair");
    }
    
    /**
     * Generates a digitally signed certificate for the wipe operation
     */
    public WipeCertificate generateCertificate(SecureWipeEngine.WipeResult wipeResult,
                                             DriveDetector.DriveInfo driveInfo) throws Exception {
        
        logger.info("Generating certificate for wipe operation: {}", wipeResult.getDevicePath());
        
        WipeCertificate certificate = new WipeCertificate(wipeResult, driveInfo);
        
        // Generate public key hash for verification
        String publicKeyHash = generatePublicKeyHash();
        certificate.setPublicKeyHash(publicKeyHash);
        
        // Sign the certificate
        String signature = signCertificate(certificate);
        certificate.setDigitalSignature(signature);
        
        logger.info("Certificate generated successfully with ID: {}", certificate.getCertificateId());
        return certificate;
    }
    
    /**
     * Saves the certificate as a JSON file
     */
    public void saveCertificateAsJson(WipeCertificate certificate, File outputFile) throws IOException {
        logger.info("Saving certificate as JSON: {}", outputFile.getAbsolutePath());
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        mapper.writeValue(outputFile, certificate);
        logger.info("JSON certificate saved successfully");
    }
    
    /**
     * Saves the certificate as a PDF file with QR code
     */
    public void saveCertificateAsPdf(WipeCertificate certificate, File outputFile) throws Exception {
        logger.info("Saving certificate as PDF: {}", outputFile.getAbsolutePath());
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Set up fonts and layout
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float lineHeight = 20;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Secure Data Wipe Certificate");
                contentStream.endText();
                yPosition -= lineHeight * 2;
                
                // Certificate ID
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Certificate ID: " + certificate.getCertificateId());
                contentStream.endText();
                yPosition -= lineHeight * 2;
                
                // Device information
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Device Information");
                contentStream.endText();
                yPosition -= lineHeight;
                
                String[] deviceInfo = {
                    "Path: " + certificate.getDevicePath(),
                    "Serial Number: " + certificate.getDeviceSerial(),
                    "Label: " + certificate.getDeviceLabel(),
                    "Size: " + String.format("%.2f GB", certificate.getDeviceSize() / (1024.0 * 1024.0 * 1024.0))
                };
                
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                for (String info : deviceInfo) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 20, yPosition);
                    contentStream.showText(info);
                    contentStream.endText();
                    yPosition -= lineHeight;
                }
                yPosition -= lineHeight;
                
                // Wipe information
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Wipe Operation Details");
                contentStream.endText();
                yPosition -= lineHeight;
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String[] wipeInfo = {
                    "Method: " + certificate.getWipeMethod(),
                    "Start Time: " + certificate.getStartTime().format(formatter),
                    "End Time: " + certificate.getEndTime().format(formatter),
                    "Bytes Wiped: " + String.format("%,d bytes", certificate.getBytesWiped())
                };
                
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                for (String info : wipeInfo) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 20, yPosition);
                    contentStream.showText(info);
                    contentStream.endText();
                    yPosition -= lineHeight;
                }
                yPosition -= lineHeight;
                
                // Digital signature information
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Digital Signature");
                contentStream.endText();
                yPosition -= lineHeight;
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(margin + 20, yPosition);
                contentStream.showText("Public Key Hash: " + certificate.getPublicKeyHash());
                contentStream.endText();
                yPosition -= lineHeight;
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 8);
                contentStream.newLineAtOffset(margin + 20, yPosition);
                String signature = certificate.getDigitalSignature();
                if (signature.length() > 80) {
                    contentStream.showText("Signature: " + signature.substring(0, 80));
                    contentStream.endText();
                    yPosition -= lineHeight;
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 20, yPosition);
                    contentStream.showText(signature.substring(80));
                }
                contentStream.endText();
                yPosition -= lineHeight * 2;
                
                // Generate and add QR code
                BufferedImage qrImage = generateQRCode(certificate);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "PNG", baos);
                
                PDImageXObject qrImageXObject = PDImageXObject.createFromByteArray(
                    document, baos.toByteArray(), "QRCode");
                
                float qrSize = 100;
                contentStream.drawImage(qrImageXObject, 
                    page.getMediaBox().getWidth() - margin - qrSize, 
                    yPosition - qrSize, qrSize, qrSize);
                
                // Add QR code label
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(
                    page.getMediaBox().getWidth() - margin - qrSize, 
                    yPosition - qrSize - 15);
                contentStream.showText("Scan to verify certificate");
                contentStream.endText();
            }
            
            document.save(outputFile);
            logger.info("PDF certificate saved successfully");
    }

            logger.info("PDF certificate saved successfully");
    }

    /**
     * Generates or loads an RSA key pair for signing
     */
    private KeyPair generateOrLoadKeyPair() throws Exception {
        // For MVP, generate a new key pair each time
        // In production, you would want to load from a secure keystore
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }
    
    /**
     * Generates a hash of the public key for verification
     */
    private String generatePublicKeyHash() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] hash = digest.digest(publicKeyBytes);
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Signs the certificate with the private key
     */
    private String signCertificate(WipeCertificate certificate) throws Exception {
        // Create signature data from certificate fields
        StringBuilder signatureData = new StringBuilder();
        signatureData.append(certificate.getDevicePath())
                    .append(certificate.getDeviceSerial())
                    .append(certificate.getWipeMethod())
                    .append(certificate.getStartTime())
                    .append(certificate.getEndTime())
                    .append(certificate.getBytesWiped())
                    .append(certificate.getCertificateId());
        
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(signatureData.toString().getBytes("UTF-8"));
        
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
    
    /**
     * Generates a QR code for the certificate
     */
    private BufferedImage generateQRCode(WipeCertificate certificate) throws WriterException {
        // Create QR code content with certificate verification info
        StringBuilder qrContent = new StringBuilder();
        qrContent.append("Certificate ID: ").append(certificate.getCertificateId()).append("\n")
                 .append("Device: ").append(certificate.getDevicePath()).append("\n")
                 .append("Method: ").append(certificate.getWipeMethod()).append("\n")
                 .append("Date: ").append(certificate.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n")
                 .append("Signature: ").append(certificate.getDigitalSignature().substring(0, Math.min(20, certificate.getDigitalSignature().length()))).append("...");
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent.toString(), BarcodeFormat.QR_CODE, 200, 200, hints);
        
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        
        return qrImage;
    }
}
