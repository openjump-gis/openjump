@echo off

set IZPACK_HOME=%~dp0..%

rem -- set classpath --
set CLASSPATH=

for %%i in ("%IZPACK_HOME%\lib\*.jar") do (
  set jarfile=%%i

  rem If we append to a variable inside the for, only the last entry will
  rem be kept. So append to the variable outside the for.
  rem See http://www.experts-exchange.com/Operating_Systems/MSDOS/Q_20561701.html.
  rem [Jon Aquino]

  call :setclass
)

java -Xmx512m %JAVA_OPTS% -classpath "%CLASSPATH%" com.izforge.izpack.compiler.Compiler %1 -h "%IZPACK_HOME%" %2 %3 %4

goto :eof

:setclass
set CLASSPATH=%CLASSPATH%;%jarfile%
set jarfile=
goto :eof

:eof