@echo off
echo [BedwarsAC] Installing v1_8_R3 NMS compile-time stubs...
call mvn -B -f nms-stubs/pom.xml clean install
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo [BedwarsAC] Building BedwarsAC-1.0.0-RELEASE.jar...
call mvn -B clean test package
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo ================================================================
echo Build complete! Plugin JAR: target\BedwarsAC-1.0.0-RELEASE.jar
echo ================================================================
