@echo off
echo ==================================================
echo    INSTALADOR MPJ EXPRESS - PROYECTO PCyP
echo ==================================================
echo.

echo 📥 Descargando MPJ Express...
if not exist "lib" mkdir "lib"

:: Descargar MPJ Express
powershell -Command "& {
    try {
        Invoke-WebRequest -Uri 'https://github.com/mpj-express/mpj-express/releases/download/v0.44/mpj-v0_44.zip' -OutFile 'mpj-temp.zip'
        echo ✅ MPJ descargado correctamente
        
        echo 🔄 Descomprimiendo...
        Expand-Archive -Path 'mpj-temp.zip' -DestinationPath '.' -Force
        copy 'mpj-v0_44\lib\*.jar' 'lib\' -Force
        echo ✅ Archivos copiados a lib\
        
        :: Limpiar
        Remove-Item 'mpj-temp.zip' -Force
        Remove-Item 'mpj-v0_44' -Recurse -Force
        
        echo.
        echo 🎉 MPJ Express instalado correctamente!
        echo 📍 Archivos en: lib\mpj.jar
    }
    catch {
        echo ❌ Error descargando MPJ: $_
        echo.
        echo 💡 Solucion alternativa:
        echo 1. Descarga manualmente de: http://mpj-express.org/
        echo 2. Guarda como: lib\mpj.jar
    }
}"

echo.
pause