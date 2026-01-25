@echo off
echo 🔧 Генератор конфигураций для Config Server
echo ============================================

REM Проверяем наличие Python
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python не найден! Установите Python 3 и добавьте в PATH
    pause
    exit /b 1
)

REM Запускаем скрипт
python build-config.py

pause