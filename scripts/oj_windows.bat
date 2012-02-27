@echo off
rem -- Detect current dir and OJ home --
set OLD_DIR=%CD%
set JUMP_HOME=%~dp0..%

rem -- uncomment to save settings and log to user profile ---
rem -- defaults to 'JUMP_HOME', if former is not writable 'userprofile/.openjump' --
rem set SETTINGS_HOME="%HOMEDRIVE%%HOMEPATH%"\.openjump

rem -- uncomment to manually set java home --
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

rem --- uncomment and change your http proxy settings here
rem set JAVA_OPTS=%JAVA_OPTS% -Dhttp.proxyHost=myproxyserver.com -Dhttp.proxyPort=80 -Dhttp.noProxyHosts="localhost|host.mydomain.com"

rem --- if the proxy server requires authentication uncomment and edit also these
rem set JAVA_OPTS=%JAVA_OPTS% -Dhttp.proxyUser=username -Dhttp.proxyPass=password

rem -- dequote path entries, just to be sure --
call :dequote %PATH%
set "PATH=%unquoted%"

rem -- dequote java_home, later on we assume it's unquoted --
call :dequote %JAVA_HOME%
set "JAVA_HOME=%unquoted%"

rem -- find java runtime --
  set JAVA=
  rem --- default to javaw ---
  if "%JAVA_BIN%"=="" set JAVA_BIN=javaw

  rem --- search binary in path ---
  @for %%i in (%JAVA_BIN%.exe) do @if NOT "%%~$PATH:i"=="" set JAVA=%%~$PATH:i

  rem --- we might be on amd64 having only x86 jre installed ---
  if "%JAVA%"=="" if DEFINED ProgramFiles(x86) if NOT "%PROCESSOR_ARCHITECTURE%"=="x86" (
    rem --- restart the batch in x86 mode---
    echo Warning: No java interpreter found in path.
    echo Retry using Wow64 filesystem [32bit environment] redirection.
    %SystemRoot%\SysWOW64\cmd.exe /c %0 %*
    exit /b %ERRORLEVEL%
  )

  rem --- if unset fall back to plain bin name, just in case ---
  if "%JAVA%"=="" set JAVA=%JAVA_BIN%
  
  rem --- java home definition overwrites all ---
  if NOT "%JAVA_HOME%"=="" set "JAVA=%JAVA_HOME%\bin\%JAVA_BIN%"

rem -- show java version (for debugging) --
for %%F in ("%JAVA%") do set "dirname=%%~dpF"
echo Using '%JAVA_BIN%' found in '%dirname%'
"%dirname%java" -version

rem -- Change to jump home dir --
rem -- NOTE: mount UNC paths to a local drive for this --
cd /D %JUMP_HOME%

rem -- Uninstall if asked nicely ---
if "%1"=="--uninstall" ( 
  "%JAVA%" -jar .\uninstall\uninstaller.jar
  goto :eof
)

set LIB=lib

rem -- setup native lib paths
set NATIVE=%LIB%\native
if DEFINED ProgramFiles(x86) set X64=64
rem --- XP Version 5.x ---
for /f "delims=" %%v in ('ver^|findstr /I /C:"Version 5"') do (
  set "ID=xp"
)
rem --- Vista Version 6.0 ---
for /f "delims=" %%v in ('ver^|findstr /I /C:"Version 6.0"') do (
  set "ID=vista"
)
rem --- 7 Version 6.1 ---
for /f "delims=" %%v in ('ver^|findstr /I /C:"Version 6.1"') do (
  set "ID=seven"
)
rem -- add native as fallthrough and lib\ext the legacy value --
set "NATIVEPATH=%NATIVE%\%ID%%X64%;%NATIVE%\%ID%;%NATIVE%"
set "PATH=%NATIVEPATH%;%LIB%\ext;%PATH%"

rem -- debug info --
if /i NOT "%JAVA_BIN%"=="javaw" echo ---PATH--- & echo %PATH%

rem -- set classpath --
set CLASSPATH=.;bin;conf

for %%i in ("%LIB%\*.jar" "%LIB%\*.zip" "%NATIVE%\%ID%%X64%\*.jar" "%NATIVE%\%ID%\*.jar" "%NATIVE%\*.jar") do (
  set jarfile=%%i

  rem If we append to a variable inside the for, only the last entry will
  rem be kept. So append to the variable outside the for.
  rem See http://www.experts-exchange.com/Operating_Systems/MSDOS/Q_20561701.html.
  rem [Jon Aquino]

  call :setclass
)

rem -- debug info --
if /i NOT "%JAVA_BIN%"=="javaw" echo ---CLASSPATH--- & echo %CLASSPATH%

rem -- set settings home/log dir if none given --
  rem --- dequote settings_home, later on we assume it's unquoted ---
  call :dequote %SETTINGS_HOME%
  set "SETTINGS_HOME=%unquoted%"

  rem --- set default or create missing folder ---
  rem --- ATTENTION: logdir requires a trailing backslash for concatenation in log4j.xml ---
  if NOT DEFINED SETTINGS_HOME (
    rem ---- check if jumphome is writable ----
    copy /Y NUL "%JUMP_HOME%\.writable" > NUL 2>&1 && set WRITEOK=1
    IF DEFINED WRITEOK ( 
      rem ---- an absolute settings_home allows file:/// for log4j conf ----
      set "SETTINGS_HOME=%JUMP_HOME%"
     ) else (
      set "SETTINGS_HOME=%HOMEDRIVE%%HOMEPATH%\.openjump"
    )
  )
  set "LOG_DIR=%SETTINGS_HOME%/"
  rem -- debug info --
  if /i NOT "%JAVA_BIN%"=="javaw" echo ---Save logs ^& state to--- & echo %SETTINGS_HOME%
  rem --- create folder if not existing ---
  if NOT EXIST "%SETTINGS_HOME%" mkdir "%SETTINGS_HOME%"

rem -- look if we have a custom logging configuration in settings --
if EXIST "%SETTINGS_HOME%\log4j.xml" (
  rem --- log4j can't seem to find absolute path without the file:/// prefix ---
  set "LOG4J_CONF=file:///%SETTINGS_HOME%\log4j.xml"
) else (
  set LOG4J_CONF=.\bin\log4j.xml
)

rem -- essential options, don't change unless you know what you're doing --
set JAVA_OPTS=%JAVA_OPTS% -Dlog4j.configuration="%LOG4J_CONF%" -Dlog.dir="%LOG_DIR%" -Djump.home="%JUMP_HOME%"

rem -- set default app options --
set JUMP_OPTS=-default-plugins bin\default-plugins.xml -state "%SETTINGS_HOME%" -plug-in-directory "%LIB%\ext"
rem --- workbench-properties.xml is used to manually load plugins (ISA uses this) ---
if EXIST "bin\workbench-properties.xml" set "JUMP_OPTS=%JUMP_OPTS% -properties bin\workbench-properties.xml"

rem -- disconnect javaw from console by using start --
rem -- note: title is needed or start won't accept quoted path to java binary (protect spaces in javapath) --
if /i "%JAVA_BIN%"=="javaw" ( set START=start "" ) else ( set START= )
%START% "%JAVA%" -cp "%CLASSPATH%" %JAVA_OPTS% com.vividsolutions.jump.workbench.JUMPWorkbench %JUMP_OPTS% %*

cd /D %OLD_DIR%

rem -- give user a chance to see console output if we are in console mode but the app finished already
if /i NOT "%JAVA_BIN%"=="javaw" pause

goto :eof

:setclass
set CLASSPATH=%CLASSPATH%;%jarfile%
set jarfile=
goto :eof

:dequote
SETLOCAL enabledelayedexpansion
set string=%*
if DEFINED string set string=!string:"=!
ENDLOCAL & set unquoted=%string%
goto :eof

:eof
