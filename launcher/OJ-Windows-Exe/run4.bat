@echo off
cd /d "%~dp0%"

rem Bat_To_Exe_Converter.exe wrapper %* gives us '%0 %1 ...' but we only want '%1 %2 ...'
for /f "tokens=1,* delims= " %%a in ("%*") do set params=%%b

@echo on
.\oj_windows.bat %params%

