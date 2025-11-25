@echo off
cd /d "C:\Users\Fatima\Documents\NetBeansProjects\ProjectoPCyPoto2025"
title 🚀 Proyecto PCyP - NetBeans

echo ==================================================
echo    EJECUCION PROYECTO NETBEANS
echo ==================================================
echo.

echo 🔍 Estructura del proyecto:
dir src\main\java\com\mycompany\projectopcypoto2025\*.java

echo.
echo 🔨 Compilando...
javac -cp ".;lib/mpj.jar" -d target/classes src/main/java/com/mycompany/projectopcypoto2025/*.java

if errorlevel 1 (
    echo ❌ Error de compilación
    echo 💡 Probando compilación paso a paso...
    javac -cp ".;lib/mpj.jar" -d target/classes src/main/java/com/mycompany/projectopcypoto2025/SyncMetricsMPJ.java
    javac -cp ".;lib/mpj.jar" -d target/classes src/main/java/com/mycompany/projectopcypoto2025/ProyectoPCyPoto2025.java
)

echo.
echo 🚀 Ejecutando SyncMetricsMPJ...
java -cp "target/classes;lib/mpj.jar" com.mycompany.projectopcypoto2025.SyncMetricsMPJ

echo.
pause