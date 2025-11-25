@echo off
title 🖥️ Proyecto PCyP Otoño 2025 - GUI
echo ==================================================
echo    PROYECTO PCyP OTOÑO 2025 - INTERFAZ GRAFICA
echo ==================================================
echo.

:: Verificar si está compilado
if not exist "target\classes\com\mycompany\proyectopcypoto2025\ProyectoPCyPoto2025.class" (
    echo ❌ ERROR: Primero compila el proyecto con ejecutar_mpj.bat
    pause
    exit /b 1
)

echo 🖥️ Iniciando interfaz gráfica...
echo.

java -cp "target\classes;lib\mpj.jar" com.mycompany.proyectopcypoto2025.ProyectoPCyPoto2025

pause