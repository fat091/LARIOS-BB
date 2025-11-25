@echo off
cd /d "%~dp0"
title 🚀 Proyecto PCyP - MPJ Express

echo ==================================================
echo    PROYECTO PCyP OTOÑO 2025 - EJECUCION COMPLETA
echo ==================================================
echo.

:: Verificar Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Java no encontrado o no configurado
    echo    Instala JDK 8+ y configura JAVA_HOME
    pause
    exit /b 1
)

:: Verificar si estamos en la carpeta correcta
if not exist "src" (
    echo ❌ ERROR: No se encuentra la carpeta src/
    echo    Ejecuta este script desde la carpeta del proyecto
    pause
    exit /b 1
)

:: Verificar MPJ
if not exist "lib\mpj.jar" (
    echo ❌ MPJ Express no encontrado en lib\mpj.jar
    echo 📥 Ejecuta primero: instalar_mpj.bat
    pause
    exit /b 1
)

echo 🔨 Compilando proyecto...
if not exist "target\classes" mkdir "target\classes"

:: Compilar todos los archivos Java
javac -cp ".;lib\mpj.jar" -d "target\classes" src\com\mycompany\proyectopcypoto2025\*.java

if errorlevel 1 (
    echo ❌ ERROR: Fallo en la compilacion
    echo    Revisa los errores arriba
    pause
    exit /b 1
)

echo ✅ Compilacion exitosa!

echo.
echo 🚀 EJECUTANDO MPJ EXPRESS - 5 CORES
echo ====================================
echo    Core 0: Semáforos
echo    Core 1: Variables de Condición
echo    Core 2: Monitores  
echo    Core 3: Mutex
echo    Core 4: Barreras
echo.

:: Método 1: Usando la clase principal directamente
echo 🔧 Ejecutando con Java directamente...
java -cp "target\classes;lib\mpj.jar" com.mycompany.proyectopcypoto2025.SyncMetricsMPJ

if errorlevel 1 (
    echo.
    echo ❌ ERROR en ejecucion MPJ
    echo 📋 Probando metodo alternativo...
    
    :: Método alternativo
    echo 🔄 Intentando con runtime MPJ...
    java -jar "lib\mpj.jar" -np 5 -cp "target\classes" com.mycompany.proyectopcypoto2025.SyncMetricsMPJ
)

if errorlevel 1 (
    echo.
    echo ❌❌ Todos los metodos fallaron
    echo 💡 Soluciones:
    echo    1. Verifica que MPJ este instalado correctamente
    echo    2. Ejecuta como Administrador
    echo    3. Revisa el firewall de Windows
    pause
    exit /b 1
)

echo.
echo ✅ EJECUCION MPJ COMPLETADA!
echo.

:: Verificar archivos generados
echo 📊 ARCHIVOS GENERADOS:
dir *.csv

echo.
echo 🖥️ Para ejecutar la interfaz grafica:
echo    ejecutar_gui.bat
echo.

pause