@echo off
setlocal
echo ===================================================================
echo   CODE VISUALIZATION PLAYGROUND - AUTOMATED SELENIUM TEST SUITE
echo ===================================================================

cd /d "%~dp0"

echo Compiling test suite...
javac -cp "Selenium\lib\*" -d "Selenium" "Selenium\com\codevisualizer\test\CodeVisualizationTest.java"

echo Running 18 End-to-End Test Scenarios...
java -Dheadless=true -cp "Selenium;Selenium\lib\*" com.codevisualizer.test.CodeVisualizationTest

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] All 18 automated test scenarios passed successfully!
) else (
    echo.
    echo [FAILURE] Test suite encountered errors.
)
