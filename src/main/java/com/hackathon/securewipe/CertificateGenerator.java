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
</function_calls>

<function_results>{"file_content_after_create":{"path":"C:\\Users\\jayan\\Documents\\SecureDataWipeTool\\src\\main\\java\\com\\hackathon\\securewipe\\CertificateGenerator.java","content":"1|package com.hackathon.securewipe;\n2|\n3|import com.fasterxml.jackson.databind.ObjectMapper;\n4|import com.fasterxml.jackson.databind.SerializationFeature;\n5|import com.google.zxing.BarcodeFormat;\n6|import com.google.zxing.EncodeHintType;\n7|import com.google.zxing.WriterException;\n8|import com.google.zxing.common.BitMatrix;\n9|import com.google.zxing.qrcode.QRCodeWriter;\n10|import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;\n11|import org.apache.pdfbox.pdmodel.PDDocument;\n12|import org.apache.pdfbox.pdmodel.PDPage;\n13|import org.apache.pdfbox.pdmodel.PDPageContentStream;\n14|import org.apache.pdfbox.pdmodel.common.PDRectangle;\n15|import org.apache.pdfbox.pdmodel.font.PDType1Font;\n16|import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;\n17|import org.slf4j.Logger;\n18|import org.slf4j.LoggerFactory;\n19|\n20|import javax.imageio.ImageIO;\n21|import java.awt.*;\n22|import java.awt.image.BufferedImage;\n23|import java.io.ByteArrayOutputStream;\n24|import java.io.File;\n25|import java.io.IOException;\n26|import java.security.*;\n27|import java.security.spec.PKCS8EncodedKeySpec;\n28|import java.security.spec.X509EncodedKeySpec;\n29|import java.time.LocalDateTime;\n30|import java.time.format.DateTimeFormatter;\n31|import java.util.Base64;\n32|import java.util.HashMap;\n33|import java.util.Map;\n34|\n35|/**\n36| * Certificate generator that creates digitally signed PDF and JSON certificates\n37| * proving successful completion of secure data wipe operations\n38| */\n39|public class CertificateGenerator {\n40|    \n41|    private static final Logger logger = LoggerFactory.getLogger(CertificateGenerator.class);\n42|    private static final String ALGORITHM = \"RSA\";\n43|    private static final String SIGNATURE_ALGORITHM = \"SHA256withRSA\";\n44|    private static final int KEY_SIZE = 2048;\n45|    \n46|    /**\n47|     * Certificate data structure\n48|     */\n49|    public static class WipeCertificate {\n50|        private String devicePath;\n51|        private String deviceSerial;\n52|        private String deviceLabel;\n53|        private long deviceSize;\n54|        private String wipeMethod;\n55|        private LocalDateTime startTime;\n56|        private LocalDateTime endTime;\n57|        private long bytesWiped;\n58|        private String certificateId;\n59|        private String digitalSignature;\n60|        private String publicKeyHash;\n61|        private String toolVersion;\n62|        private String platform;\n63|        \n64|        // Constructors, getters, and setters\n65|        public WipeCertificate() {}\n66|        \n67|        public WipeCertificate(SecureWipeEngine.WipeResult wipeResult, \n68|                              DriveDetector.DriveInfo driveInfo) {\n69|            this.devicePath = wipeResult.getDevicePath();\n70|            this.deviceSerial = wipeResult.getDeviceSerial();\n71|            this.deviceLabel = driveInfo.getLabel();\n72|            this.deviceSize = driveInfo.getSize();\n73|            this.wipeMethod = wipeResult.getMethod();\n74|            this.startTime = wipeResult.getStartTime();\n75|            this.endTime = wipeResult.getEndTime();\n76|            this.bytesWiped = wipeResult.getBytesWiped();\n77|            this.certificateId = generateCertificateId();\n78|            this.toolVersion = \"1.0.0\";\n79|            this.platform = System.getProperty(\"os.name\");\n80|        }\n81|        \n82|        private String generateCertificateId() {\n83|            return \"CERT-\" + System.currentTimeMillis() + \"-\" + \n84|                   Integer.toHexString(hashCode()).toUpperCase();\n85|        }\n86|        \n87|        // Getters and setters\n88|        public String getDevicePath() { return devicePath; }\n89|        public void setDevicePath(String devicePath) { this.devicePath = devicePath; }\n90|        \n91|        public String getDeviceSerial() { return deviceSerial; }\n92|        public void setDeviceSerial(String deviceSerial) { this.deviceSerial = deviceSerial; }\n93|        \n94|        public String getDeviceLabel() { return deviceLabel; }\n95|        public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }\n96|        \n97|        public long getDeviceSize() { return deviceSize; }\n98|        public void setDeviceSize(long deviceSize) { this.deviceSize = deviceSize; }\n99|        \n100|        public String getWipeMethod() { return wipeMethod; }\n101|        public void setWipeMethod(String wipeMethod) { this.wipeMethod = wipeMethod; }\n102|        \n103|        public LocalDateTime getStartTime() { return startTime; }\n104|        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }\n105|        \n106|        public LocalDateTime getEndTime() { return endTime; }\n107|        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }\n108|        \n109|        public long getBytesWiped() { return bytesWiped; }\n110|        public void setBytesWiped(long bytesWiped) { this.bytesWiped = bytesWiped; }\n111|        \n112|        public String getCertificateId() { return certificateId; }\n113|        public void setCertificateId(String certificateId) { this.certificateId = certificateId; }\n114|        \n115|        public String getDigitalSignature() { return digitalSignature; }\n116|        public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }\n117|        \n118|        public String getPublicKeyHash() { return publicKeyHash; }\n119|        public void setPublicKeyHash(String publicKeyHash) { this.publicKeyHash = publicKeyHash; }\n120|        \n121|        public String getToolVersion() { return toolVersion; }\n122|        public void setToolVersion(String toolVersion) { this.toolVersion = toolVersion; }\n123|        \n124|        public String getPlatform() { return platform; }\n125|        public void setPlatform(String platform) { this.platform = platform; }\n126|    }\n127|    \n128|    private final KeyPair keyPair;\n129|    \n130|    /**\n131|     * Constructor that generates or loads a key pair for signing\n132|     */\n133|    public CertificateGenerator() throws Exception {\n134|        this.keyPair = generateOrLoadKeyPair();\n135|        logger.info(\"Certificate generator initialized with RSA key pair\");\n136|    }\n137|    \n138|    /**\n139|     * Generates a digitally signed certificate for the wipe operation\n140|     */\n141|    public WipeCertificate generateCertificate(SecureWipeEngine.WipeResult wipeResult,\n142|                                             DriveDetector.DriveInfo driveInfo) throws Exception {\n143|        \n144|        logger.info(\"Generating certificate for wipe operation: {}\", wipeResult.getDevicePath());\n145|        \n146|        WipeCertificate certificate = new WipeCertificate(wipeResult, driveInfo);\n147|        \n148|        // Generate public key hash for verification\n149|        String publicKeyHash = generatePublicKeyHash();\n150|        certificate.setPublicKeyHash(publicKeyHash);\n151|        \n152|        // Sign the certificate\n153|        String signature = signCertificate(certificate);\n154|        certificate.setDigitalSignature(signature);\n155|        \n156|        logger.info(\"Certificate generated successfully with ID: {}\", certificate.getCertificateId());\n157|        return certificate;\n158|    }\n159|    \n160|    /**\n161|     * Saves the certificate as a JSON file\n162|     */\n163|    public void saveCertificateAsJson(WipeCertificate certificate, File outputFile) throws IOException {\n164|        logger.info(\"Saving certificate as JSON: {}\", outputFile.getAbsolutePath());\n165|        \n166|        ObjectMapper mapper = new ObjectMapper();\n167|        mapper.enable(SerializationFeature.INDENT_OUTPUT);\n168|        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());\n169|        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);\n170|        \n171|        mapper.writeValue(outputFile, certificate);\n172|        logger.info(\"JSON certificate saved successfully\");\n173|    }\n174|    \n175|    /**\n176|     * Saves the certificate as a PDF file with QR code\n177|     */\n178|    public void saveCertificateAsPdf(WipeCertificate certificate, File outputFile) throws Exception {\n179|        logger.info(\"Saving certificate as PDF: {}\", outputFile.getAbsolutePath());\n180|        \n181|        try (PDDocument document = new PDDocument()) {\n182|            PDPage page = new PDPage(PDRectangle.A4);\n183|            document.addPage(page);\n184|            \n185|            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {\n186|                // Set up fonts and layout\n187|                float margin = 50;\n188|                float yPosition = page.getMediaBox().getHeight() - margin;\n189|                float lineHeight = 20;\n190|                \n191|                // Title\n192|                contentStream.beginText();\n193|                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);\n194|                contentStream.newLineAtOffset(margin, yPosition);\n195|                contentStream.showText(\"Secure Data Wipe Certificate\");\n196|                contentStream.endText();\n197|                yPosition -= lineHeight * 2;\n198|                \n199|                // Certificate ID\n200|                contentStream.beginText();\n201|                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);\n202|                contentStream.newLineAtOffset(margin, yPosition);\n203|                contentStream.showText(\"Certificate ID: \" + certificate.getCertificateId());\n204|                contentStream.endText();\n205|                yPosition -= lineHeight * 2;\n206|                \n207|                // Device information\n208|                contentStream.beginText();\n209|                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);\n210|                contentStream.newLineAtOffset(margin, yPosition);\n211|                contentStream.showText(\"Device Information\");\n212|                contentStream.endText();\n213|                yPosition -= lineHeight;\n214|                \n215|                String[] deviceInfo = {\n216|                    \"Path: \" + certificate.getDevicePath(),\n217|                    \"Serial Number: \" + certificate.getDeviceSerial(),\n218|                    \"Label: \" + certificate.getDeviceLabel(),\n219|                    \"Size: \" + String.format(\"%.2f GB\", certificate.getDeviceSize() / (1024.0 * 1024.0 * 1024.0))\n220|                };\n221|                \n222|                contentStream.setFont(PDType1Font.HELVETICA, 12);\n223|                for (String info : deviceInfo) {\n224|                    contentStream.beginText();\n225|                    contentStream.newLineAtOffset(margin + 20, yPosition);\n226|                    contentStream.showText(info);\n227|                    contentStream.endText();\n228|                    yPosition -= lineHeight;\n229|                }\n230|                yPosition -= lineHeight;\n231|                \n232|                // Wipe information\n233|                contentStream.beginText();\n234|                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);\n235|                contentStream.newLineAtOffset(margin, yPosition);\n236|                contentStream.showText(\"Wipe Operation Details\");\n237|                contentStream.endText();\n238|                yPosition -= lineHeight;\n239|                \n240|                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(\"yyyy-MM-dd HH:mm:ss\");\n241|                String[] wipeInfo = {\n242|                    \"Method: \" + certificate.getWipeMethod(),\n243|                    \"Start Time: \" + certificate.getStartTime().format(formatter),\n244|                    \"End Time: \" + certificate.getEndTime().format(formatter),\n245|                    \"Bytes Wiped: \" + String.format(\"%,d bytes\", certificate.getBytesWiped())\n246|                };\n247|                \n248|                contentStream.setFont(PDType1Font.HELVETICA, 12);\n249|                for (String info : wipeInfo) {\n250|                    contentStream.beginText();\n251|                    contentStream.newLineAtOffset(margin + 20, yPosition);\n252|                    contentStream.showText(info);\n253|                    contentStream.endText();\n254|                    yPosition -= lineHeight;\n255|                }\n256|                yPosition -= lineHeight;\n257|                \n258|                // Digital signature information\n259|                contentStream.beginText();\n260|                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);\n261|                contentStream.newLineAtOffset(margin, yPosition);\n262|                contentStream.showText(\"Digital Signature\");\n263|                contentStream.endText();\n264|                yPosition -= lineHeight;\n265|                \n266|                contentStream.beginText();\n267|                contentStream.setFont(PDType1Font.HELVETICA, 10);\n268|                contentStream.newLineAtOffset(margin + 20, yPosition);\n269|                contentStream.showText(\"Public Key Hash: \" + certificate.getPublicKeyHash());\n270|                contentStream.endText();\n271|                yPosition -= lineHeight;\n272|                \n273|                contentStream.beginText();\n274|                contentStream.setFont(PDType1Font.HELVETICA, 8);\n275|                contentStream.newLineAtOffset(margin + 20, yPosition);\n276|                String signature = certificate.getDigitalSignature();\n277|                if (signature.length() > 80) {\n278|                    contentStream.showText(\"Signature: \" + signature.substring(0, 80));\n279|                    contentStream.endText();\n280|                    yPosition -= lineHeight;\n281|                    contentStream.beginText();\n282|                    contentStream.newLineAtOffset(margin + 20, yPosition);\n283|                    contentStream.showText(signature.substring(80));\n284|                }\n285|                contentStream.endText();\n286|                yPosition -= lineHeight * 2;\n287|                \n288|                // Generate and add QR code\n289|                BufferedImage qrImage = generateQRCode(certificate);\n290|                ByteArrayOutputStream baos = new ByteArrayOutputStream();\n291|                ImageIO.write(qrImage, \"PNG\", baos);\n292|                \n293|                PDImageXObject qrImageXObject = PDImageXObject.createFromByteArray(\n294|                    document, baos.toByteArray(), \"QRCode\");\n295|                \n296|                float qrSize = 100;\n297|                contentStream.drawImage(qrImageXObject, \n298|                    page.getMediaBox().getWidth() - margin - qrSize, \n299|                    yPosition - qrSize, qrSize, qrSize);\n300|                \n301|                // Add QR code label\n302|                contentStream.beginText();\n303|                contentStream.setFont(PDType1Font.HELVETICA, 10);\n304|                contentStream.newLineAtOffset(\n305|                    page.getMediaBox().getWidth() - margin - qrSize, \n306|                    yPosition - qrSize - 15);\n307|                contentStream.showText(\"Scan to verify certificate\");\n308|                contentStream.endText();\n309|            }\n310|            \n311|            document.save(outputFile);\n312|            logger.info(\"PDF certificate saved successfully\");\n313|        }\n314|    }\n315|    \n316|    /**\n317|     * Generates or loads an RSA key pair for signing\n318|     */\n319|    private KeyPair generateOrLoadKeyPair() throws Exception {\n320|        // For MVP, generate a new key pair each time\n321|        // In production, you would want to load from a secure keystore\n322|        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);\n323|        keyGen.initialize(KEY_SIZE);\n324|        return keyGen.generateKeyPair();\n325|    }\n326|    \n327|    /**\n328|     * Generates a hash of the public key for verification\n329|     */\n330|    private String generatePublicKeyHash() throws Exception {\n331|        MessageDigest digest = MessageDigest.getInstance(\"SHA-256\");\n332|        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();\n333|        byte[] hash = digest.digest(publicKeyBytes);\n334|        return Base64.getEncoder().encodeToString(hash).substring(0, 16); // First 16 chars\n335|    }\n336|    \n337|    /**\n338|     * Signs the certificate data\n339|     */\n340|    private String signCertificate(WipeCertificate certificate) throws Exception {\n341|        // Create a string representation of the certificate data for signing\n342|        StringBuilder dataToSign = new StringBuilder();\n343|        dataToSign.append(certificate.getDevicePath());\n344|        dataToSign.append(certificate.getDeviceSerial());\n345|        dataToSign.append(certificate.getWipeMethod());\n346|        dataToSign.append(certificate.getStartTime().toString());\n347|        dataToSign.append(certificate.getEndTime().toString());\n348|        dataToSign.append(certificate.getBytesWiped());\n349|        dataToSign.append(certificate.getCertificateId());\n350|        \n351|        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);\n352|        signature.initSign(keyPair.getPrivate());\n353|        signature.update(dataToSign.toString().getBytes());\n354|        \n355|        byte[] digitalSignature = signature.sign();\n356|        return Base64.getEncoder().encodeToString(digitalSignature);\n357|    }\n358|    \n359|    /**\n360|     * Generates a QR code containing certificate verification data\n361|     */\n362|    private BufferedImage generateQRCode(WipeCertificate certificate) throws WriterException {\n363|        // Create verification data for QR code\n364|        String qrData = String.format(\n365|            \"ID:%s|Serial:%s|Method:%s|Hash:%s\", \n366|            certificate.getCertificateId(),\n367|            certificate.getDeviceSerial(),\n368|            certificate.getWipeMethod(),\n369|            certificate.getPublicKeyHash()\n370|        );\n371|        \n372|        QRCodeWriter qrCodeWriter = new QRCodeWriter();\n373|        Map<EncodeHintType, Object> hints = new HashMap<>();\n374|        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);\n375|        hints.put(EncodeHintType.MARGIN, 1);\n376|        \n377|        BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 200, 200, hints);\n378|        \n379|        int width = bitMatrix.getWidth();\n380|        int height = bitMatrix.getHeight();\n381|        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);\n382|        \n383|        for (int x = 0; x < width; x++) {\n384|            for (int y = 0; y < height; y++) {\n385|                image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());\n386|            }\n387|        }\n388|        \n389|        return image;\n390|    }\n391|    \n392|    /**\n393|     * Verifies a digital signature\n394|     */\n395|    public boolean verifyCertificate(WipeCertificate certificate, String publicKeyBase64) throws Exception {\n396|        try {\n397|            // Decode the public key\n398|            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);\n399|            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);\n400|            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);\n401|            PublicKey publicKey = keyFactory.generatePublic(keySpec);\n402|            \n403|            // Recreate the signed data\n404|            StringBuilder dataToVerify = new StringBuilder();\n405|            dataToVerify.append(certificate.getDevicePath());\n406|            dataToVerify.append(certificate.getDeviceSerial());\n407|            dataToVerify.append(certificate.getWipeMethod());\n408|            dataToVerify.append(certificate.getStartTime().toString());\n409|            dataToVerify.append(certificate.getEndTime().toString());\n410|            dataToVerify.append(certificate.getBytesWiped());\n411|            dataToVerify.append(certificate.getCertificateId());\n412|            \n413|            // Verify the signature\n414|            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);\n415|            signature.initVerify(publicKey);\n416|            signature.update(dataToVerify.toString().getBytes());\n417|            \n418|            byte[] signatureBytes = Base64.getDecoder().decode(certificate.getDigitalSignature());\n419|            return signature.verify(signatureBytes);\n420|            \n421|        } catch (Exception e) {\n422|            logger.error(\"Error verifying certificate signature\", e);\n423|            return false;\n424|        }\n425|    }\n426|}","line_range_start":1,"line_range_end":426}}
</function_calls>

<function_calls>
<invoke name="mark_todo_as_done">
<parameter name="todo_ids">["2a8be0be-d242-47a4-9054-bf641f5e6784", "45d5a519-8dfe-40ff-9042-cf06d0e15b4b", "f686febd-d168-46f4-8d7d-f178ff45d154", "fbfcd134-4930-494e-9e3f-3ac5853355a2"]