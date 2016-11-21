@echo off
cd /d "%~dp0%"

rem %* gives us '%0 %1 ...' but we only want '%1 %2 ...'
set params=%1
:loop
shift
if [%1]==[] goto afterloop
set params=%params% %1
goto loop
:afterloop

@echo on
.\oj_windows.bat %params%