#!/bin/bash
# Build script for Secure Data Wipe Tool
# Smart India Hackathon 2025

echo "================================"
echo "Secure Data Wipe Tool - Build"
echo "================================"

echo "Checking Java version..."
java -version
if [ $? -ne 0 ]; then
    echo "ERROR: Java is not installed or not in PATH"
    exit 1
fi

echo "Checking Maven..."
mvn -version
if [ $? -ne 0 ]; then
    echo "ERROR: Maven is not installed or not in PATH"
    exit 1
fi

echo
echo "Cleaning previous builds..."
mvn clean

echo
echo "Compiling and running tests..."
mvn compile test

if [ $? -ne 0 ]; then
    echo "ERROR: Build or tests failed"
    exit 1
fi

echo
echo "Packaging application..."
mvn package

if [ $? -ne 0 ]; then
    echo "ERROR: Packaging failed"
    exit 1
fi

echo
echo "Build completed successfully!"
echo
echo "Generated files:"
ls -la target/*.jar

echo
echo "To run the application:"
echo "java -jar target/secure-data-wipe-tool-1.0.0.jar"
echo
echo "Or for development:"
echo "mvn javafx:run"

echo
echo "================================"
echo "Build Complete!"
echo "================================"