@echo off
cd /d "%~dp0"
title 🔧 REPARANDO PROYECTO PCyP

echo ==================================================
echo    REPARADOR PROYECTO PCyP OTOÑO 2025
echo ==================================================
echo.

echo 🔍 Verificando estructura...
if not exist "src" (
    echo ❌ ERROR: No hay carpeta src/
    echo    Ejecuta este script desde la carpeta del proyecto
    pause
    exit /b 1
)

echo 📁 Creando directorios necesarios...
if not exist "lib" mkdir "lib"
if not exist "target" mkdir "target"
if not exist "target\classes" mkdir "target\classes"

echo 🔎 Buscando archivos Java...
dir src\*.java /s 2>nul
dir src\com\mycompany\proyectopcypoto2025\*.java /s 2>nul

echo.
echo 📥 Descargando MPJ Express...
if not exist "lib\mpj.jar" (
    echo Descargando MPJ Express...
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/mpj-express/mpj-express/releases/download/v0.44/mpj-v0_44.zip' -OutFile 'mpj-temp.zip'"
    powershell -Command "Expand-Archive -Path 'mpj-temp.zip' -DestinationPath '.' -Force"
    copy "mpj-v0_44\lib\mpj.jar" "lib\" >nul
    del "mpj-temp.zip" >nul
    rmdir /s /q "mpj-v0_44" >nul
    echo ✅ MPJ Express descargado
) else (
    echo ✅ MPJ Express ya existe
)

echo.
echo 🔨 Compilando proyecto...
javac -cp ".;lib\mpj.jar" -d "target\classes" src\com\mycompany\proyectopcypoto2025\*.java

if errorlevel 1 (
    echo ❌ ERROR de compilación
    echo Probando compilación individual...
    javac -cp ".;lib\mpj.jar" -d "target\classes" src\com\mycompany\proyectopcypoto2025\SyncMetricsMPJ.java
)

echo.
echo 📋 Verificando clases compiladas...
dir target\classes\com\mycompany\proyectopcypoto2025\*.class

echo.
echo 🧪 Probando ejecución...
java -cp "target\classes;lib\mpj.jar" com.mycompany.proyectopcypoto2025.SyncMetricsMPJ

if errorlevel 1 (
    echo ❌ Error en ejecución
    echo 💡 Probando sin MPJ...
    java -cp "target\classes" com.mycompany.proyectopcypoto2025.SyncMetricsMPJ
)

echo.
echo ✅ Proceso completado!
pause