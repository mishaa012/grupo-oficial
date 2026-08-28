@echo off
title Hospital PRO Java V3

cd /d "%~dp0"

if not exist "SistemaHospitalCamillasJavaPro.jar" (
    echo ERROR: No se encuentra el archivo ejecutable .jar.
    pause
    exit /b 1
)

java -jar SistemaHospitalCamillasJavaPro.jar

pause
