@echo off
title 🚀 Proyecto PCyP Otoño 2025 - MPJ Express
echo ==================================================
echo    PROYECTO PCyP OTOÑO 2025 - MPJ EXPRESS
echo ==================================================
echo.

:: Verificar si Java está instalado
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Java no encontrado. Instala JDK 8+ primero.
    pause
    exit /b 1
)

:: Verificar si MPJ está en la ruta correcta
if not exist "lib\mpj.jar" (
    echo ❌ ERROR: No se encuentra lib\mpj.jar
    echo.
    echo 📥 Descarga MPJ Express de:
    echo    http://mpj-express.org/
    echo.
    echo 📁 Coloca mpj.jar en la carpeta lib\
    pause
    exit /b 1
)

echo 🚀 Compilando proyecto...
if not exist "target\classes" mkdir "target\classes"

:: Compilar todos los archivos Java
javac -cp ".;lib\mpj.jar" -d "target\classes" src\com\mycompany\proyectopcypoto2025\*.java

if errorlevel 1 (
    echo ❌ ERROR: Fallo en la compilación. Revisa el código.
    pause
    exit /b 1
)

echo ✅ Compilación exitosa!

echo.
echo 📦 Creando JAR ejecutable...
cd target\classes
jar cfv ..\..\proyecto-pcyp.jar com\mycompany\proyectopcypoto2025\*.class
cd ..\..

echo.
echo 🔧 Ejecutando MPJ Express con 5 cores...
echo    • Core 0: Semáforos
echo    • Core 1: Variables de Condición  
echo    • Core 2: Monitores
echo    • Core 3: Mutex
echo    • Core 4: Barreras
echo.

:: Ejecutar MPJ Express
mpjrun.bat -np 5 -cp "target\classes;lib\mpj.jar" com.mycompany.proyectopcypoto2025.SyncMetricsMPJ

if errorlevel 1 (
    echo.
    echo ❌ ERROR: MPJ Express falló. Posibles soluciones:
    echo    1. Verifica que MPJ esté instalado
    echo    2. Ejecuta como administrador
    echo    3. Revisa la configuración de red
    pause
    exit /b 1
)

echo.
echo ✅ Ejecución MPJ completada!
echo.
echo 📊 Archivos CSV generados:
if exist "mpj_tiempos.csv" (
    echo    ✓ mpj_tiempos.csv
) else (
    echo    ✗ mpj_tiempos.csv (no generado)
)

if exist "mpj_operaciones.csv" (
    echo    ✓ mpj_operaciones.csv
) else (
    echo    ✗ mpj_operaciones.csv (no generado)
)

echo.
echo 🎯 Ahora puedes ejecutar la GUI para ver los resultados.
echo.
pause