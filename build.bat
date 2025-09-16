@echo off
REM Build script for Secure Data Wipe Tool
REM Smart India Hackathon 2025

echo ================================
echo Secure Data Wipe Tool - Build
echo ================================

echo Checking Java version...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    exit /b 1
)

echo Checking Maven...
mvn -version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH
    exit /b 1
)

echo.
echo Cleaning previous builds...
mvn clean

echo.
echo Compiling and running tests...
mvn compile test

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build or tests failed
    exit /b 1
)

echo.
echo Packaging application...
mvn package

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Packaging failed
    exit /b 1
)

echo.
echo Build completed successfully!
echo.
echo Generated files:
dir target\*.jar

echo.
echo To run the application:
echo java -jar target\secure-data-wipe-tool-1.0.0.jar
echo.
echo Or for development:
echo mvn javafx:run

echo.
echo ================================
echo Build Complete!
echo ================================

pause