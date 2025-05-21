@echo off
REM Check if Python 3 is installed
set PYTHON_VERSION=3.10.9
set PYTHON_DIR=C:\Python\%PYTHON_VERSION%
python --version 2>NUL 
if %ERRORLEVEL% NEQ 0 (
    echo Python 3 is not installed. Downloading and installing Python 3...
    REM Download Python 3 installer
    powershell -Command "Invoke-WebRequest -Uri https://www.python.org/ftp/python/%PYTHON_VERSION%/python-%PYTHON_VERSION%-amd64.exe -OutFile python_installer.exe"
    REM Install Python 3 silently
    start /wait "" python_installer.exe /quiet InstallAllUsers=1 PrependPath=1 TargetDir="%PYTHON_DIR%"
    REM Clean up installer
    REM del python_installer.exe
    echo Python 3 installation completed.
) else (
    echo Python 3 is already installed.
)

REM Check if OPENAI_API_KEY is set
echo Checking for OPENAI_API_KEY...
set "OPENAI_API_KEY" >NUL
if "%OPENAI_API_KEY%"=="" (
    echo OPENAI_API_KEY is not set. Setting it now...
    set /p OPENAI_API_KEY="Enter your OpenAI API key: "
    setx OPENAI_API_KEY "%OPENAI_API_KEY%"
    if %ERRORLEVEL% EQU 0 (
        echo OPENAI_API_KEY has been set successfully.
    ) else (
        echo Failed to set OPENAI_API_KEY. Exiting script.
        exit /b 1
    )
) else (
    echo OPENAI_API_KEY is already set.
)

echo %PATH% | findstr /C:%PYTHON_DIR%
if not %ERRORLEVEL%==0 (
    echo Setting Python environment variables...
    setx /M PATH "%PYTHON_DIR%;%PYTHON_DIR%\Scripts;%PATH%"
    set "PATH=%PYTHON_DIR%;%PYTHON_DIR%\Scripts;%PATH%"
    echo Python environment variables set.
) else (
    echo Python environment variables are already set.
)

if exist requirements.txt (
    echo Installing required packages from requirements.txt...
    pip install -r requirements.txt
    if %ERRORLEVEL% EQU 0 (
        echo All required packages installed successfully.
    ) else (
        echo Failed to install some packages. Check the errors above.
    )
) else (
    echo No requirements.txt file found. Skipping package installation.
)