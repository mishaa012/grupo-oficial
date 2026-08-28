@echo off
title Hospital PRO Java V3

cd /d "%~dp0"

if not exist "HospitalProApp.java" (
    echo ERROR: No se encuentra HospitalProApp.java.
    echo Ejecute este archivo dentro de la carpeta JAVA_PRO_FINAL.
    pause
    exit /b 1
)

echo Compilando HospitalProApp.java (UTF-8)...
javac -encoding UTF-8 HospitalProApp.java

if errorlevel 1 (
    echo.
    echo ERROR: No se pudo compilar. Instale Java JDK 17 o superior y agreguelo al PATH.
    pause
    exit /b 1
)

echo.
echo Compilacion correcta. Iniciando Sistema Hospitalario PRO V3...
echo Ejecutando...
java HospitalProApp

pause
