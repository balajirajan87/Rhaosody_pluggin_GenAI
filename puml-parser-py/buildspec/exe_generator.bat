@echo off
echo *******************************************************************************
echo *                     Python File to Exe Converter                            *
echo *******************************************************************************

Rem User Inputs

set FileName=pumlparser
set RunFile=../src/main.py

echo.
echo Converted File name  : %RunFile%
echo Executable File name : %FileName%.exe
echo.

call conda activate myvenv

Rem Set the directory as required directory if the batch file is executed elsewhere
call cd %cd%

echo Starting conversion...
echo.

Rem Command to call the pyinstaller for the conversion of python file to executable file
call nuitka --standalone --onefile --output-filename=%FileName% %RunFile% --enable-plugin=tk-inter --include-data-files=../src/grammar/classdiagram.lark=grammar\classdiagram.lark --include-data-files=../src/grammar/activitydiagram.lark=grammar\activitydiagram.lark

echo.
echo Conversion completed!...
echo.
echo *******************************************************************************
echo *******************************************************************************
pause