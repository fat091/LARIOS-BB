@echo off
title 🔨 Proyecto PCyP - Compilación
echo ==================================================
echo    COMPILACIÓN PROYECTO PCyP OTOÑO 2025
echo ==================================================
echo.

:: Crear directorios necesarios
if not exist "target\classes" mkdir "target\classes"
if not exist "lib" mkdir "lib"

echo 🔨 Compilando todos los archivos Java...
javac -cp ".;lib\mpj.jar" -d "target\classes" src\com\mycompany\proyectopcypoto2025\*.java

if errorlevel 1 (
    echo ❌ ERROR: Fallo en la compilación
    echo.
    echo 📋 Posibles soluciones:
    echo    1. Verifica que todos los archivos .java estén en src\
    echo    2. Asegúrate de tener lib\mpj.jar
    echo    3. Revisa errores de sintaxis en el código
    pause
    exit /b 1
)

echo ✅ Compilación exitosa!
echo 📁 Archivos compilados en: target\classes\
echo.
pause