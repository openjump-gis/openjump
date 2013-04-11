@echo off
rem -- Detect current dir and OJ home --
set OLD_DIR=%CD%
set JUMP_HOME=%~dp0..%

rem -- uncomment to manually set java home, don't use quotes --
rem set JAVA_HOME=G:\path\to\a\specific\<jre|jdk>-1.<5|6>

rem -- uncomment to use 'java' for console output, if unset defaults to 'javaw' for background jre  --
rem set JAVA_BIN=java

rem -- set some java runtime options here, initialize empty --
set JAVA_OPTS=

rem --- uncomment and change your language/country here to overwrite OS locale setting ---
rem set JAVA_OPTS=%JAVA_OPTS% -Duser.language=de -Duser.country=DE

rem --- change your memory configuration here - Xms is initial size, Xmx is maximum size, ---
rem --- values are ##M for ## Megabytes, ##G for ## Gigabytes ---
set JAVA_OPTS=%JAVA_OPTS% -Xms64M -Xmx512M

rem --- essential options, don't change unless you know what you're doing ---
set JAVA_OPTS=%JAVA_OPTS% -Dlog4j.configuration="bin\log4j.xml" -Djump.home="%JUMP_HOME%"

rem -- find java runtime --
  rem --- default to javaw ---
  if "%JAVA_BIN%"=="" set JAVA_BIN=javaw

  rem --- fallback to plain bin name, just in case ---
  set JAVA=%JAVA_BIN%

  rem --- if no java home & java bin in path, replace fallback entry ---
  if "%JAVA_HOME%"=="" (
      @for %%i in (%JAVA_BIN%.exe) do @if NOT "%%~$PATH:i"=="" set JAVA=%%~$PATH:i
  )

  rem --- java home definition overwrites all ---
  if NOT "%JAVA_HOME%"=="" set JAVA=%JAVA_HOME%\bin\%JAVA_BIN%

rem -- show java version (for debugging) --
rem for %%F in ("%JAVA%") do set dirname=%%~dpF
rem %dirname%java -version

rem -- Change to jump home dir --
rem -- NOTE: mount UNC paths to a local drive for this --
cd /D %JUMP_HOME%

set LIB=lib
set CLASSPATH=.;bin;conf

for %%i in ("%LIB%\*.jar" "%LIB%\*.zip") do (
  set jarfile=%%i

  rem If we append to a variable inside the for, only the last entry will
  rem be kept. So append to the variable outside the for.
  rem See http://www.experts-exchange.com/Operating_Systems/MSDOS/Q_20561701.html.
  rem [Jon Aquino]

  call :setclass
) 

set PATH=%PATH%;%LIB%\ext

set JUMP_OPTS=-default-plugins bin\default-plugins.xml -properties bin\workbench-properties.xml -plug-in-directory "%LIB%\ext"

rem -- note: title is needed or start won't accept quoted path to java binary (enables spaces in path)
if /i "%JAVA_BIN%"=="javaw" ( set START=start "OpenJUMP console" ) else ( set START= )
%START% "%JAVA%" -cp "%CLASSPATH%" %JAVA_OPTS% com.vividsolutions.jump.workbench.JUMPWorkbench %JUMP_OPTS%

cd /D %OLD_DIR%

goto :eof

:setclass
set CLASSPATH=%CLASSPATH%;%jarfile%
set jarfile=
goto :eof

:eof
