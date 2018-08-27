@echo off & setlocal
rem call :init

rem -- Save current working dir and OJ home --
set OLD_DIR=%CD%
set JUMP_HOME=%~dp0..%

rem -- uncomment to save settings and log to user profile ---
rem -- defaults to 'JUMP_HOME', if former is not writable 'userprofile/.openjump' --
rem set SETTINGS_HOME="%HOMEDRIVE%%HOMEPATH%"\.openjump

rem -- uncomment to manually set java home, wrap in double quotes to protect special chars --
rem set JAVA_HOME="C:\Program Files (x86)\Java\jre1.8.0_xx"

rem -- uncomment to use 'java' and enable debug console output, if unset defaults to 'javaw' for background jre  --
rem set JAVA_BIN=java

rem -- set some default OJ options here (eg. -v debug), initialize empty --
set JUMP_OPTS=

rem -- set some java runtime options here, initialize empty --
set JAVA_OPTS=

rem --- uncomment and change your language/country here to overwrite OS locale setting ---
rem set JAVA_OPTS=%JAVA_OPTS% -Duser.language=de -Duser.country=DE

rem --- enforce a memory configuration here, default value is the ---
rem --- size of ram or 1GB for 32bit jre's, whichever is bigger   ---
rem --- Xms is initial size, Xmx is maximum size, values          ---
rem --- are ##M for ## Megabytes, ##G for ## Gigabytes            ---
rem set JAVA_MEM=-Xms64M -Xmx512M

rem --- uncomment and change your http proxy settings here
rem set JAVA_OPTS=%JAVA_OPTS% -Dhttp.proxyHost=myproxyserver.com -Dhttp.proxyPort=80 -Dhttp.nonProxyHosts="localhost|host.mydomain.com"

rem --- if the proxy server requires authentication uncomment and edit also these
rem set JAVA_OPTS=%JAVA_OPTS% -Dhttp.proxyUser=username -Dhttp.proxyPass=password

rem -- dequote path entries, just to be sure --
call :dequote %PATH%
set "PATH=%unquoted%"

rem -- dequote java_home, later on we assume it's unquoted --
call :dequote %JAVA_HOME%
set "JAVA_HOME=%unquoted%"

rem -- reset vars --
set ERROR=

rem -- find java runtime --
  set JAVA=
  rem --- default to javaw ---
  if "%JAVA_BIN%"=="" set JAVA_BIN=javaw

  rem --- if JAVA_HOME is defined and valid, use it ---
  if DEFINED JAVA_HOME set "JAVA_HOME_BIN=%JAVA_HOME%\bin\%JAVA_BIN%.exe"
  if DEFINED JAVA_HOME_BIN if EXIST "%JAVA_HOME_BIN%" (
      echo Using set "JAVA_HOME=%JAVA_HOME%" .
      set "JAVA=%JAVA_HOME_BIN%"
      goto java_is_set
    ) else (
      echo WARNING: Your JAVA_HOME env variable is stale and does not contain a java interpreter.
      echo   "JAVA_HOME=%JAVA_HOME%"
    )
  )

  rem --- otherwise, search binary in path ---
  @for %%i in (%JAVA_BIN%.exe) do @if NOT "%%~$PATH:i"=="" set JAVA=%%~$PATH:i

  rem --- we might be on amd64 having only x86 jre installed ---
  if "%JAVA%"=="" if DEFINED ProgramFiles(x86) if NOT "%PROCESSOR_ARCHITECTURE%"=="x86" (
    rem --- restart the batch in x86 mode---
    echo WARNING: No java interpreter found in path.
    echo   Retry using Wow64 filesystem [32bit environment] redirection.
    %SystemRoot%\SysWOW64\cmd.exe /c %0 %*
    goto:eof
  )

  rem --- if unset fall back to plain bin name, just in case ---
  if "%JAVA%"=="" set JAVA=%JAVA_BIN%

  rem --- if %JAVA% is still not a valid java path, print an informative warning ---
  if NOT EXIST "%JAVA%" (
    echo ERROR: JAVA can not be found on your system!
    echo   Check that you have a valid JRE or JDK accessible from the system PATH 
    echo   or from the environment variable JAVA_HOME .
    set "ERROR=1"
    goto :end
  )

rem -- we now have a valid java executable
:java_is_set
rem -- show java version (for debugging) --
for %%F in ("%JAVA%") do set "dirname=%%~dpF"
echo Using '%JAVA_BIN%' found in '%dirname%'
rem "%dirname%java" -version
SET concat=
for /f "tokens=* delims=" %%i in ('"%dirname%java" -version 2^>^&1') do (
    call :concat "; " %%i
    for /F "tokens=1-3 delims= " %%a in ("%%i") do (
       rem -- memorize version number string --
       if "%%a"=="java" if "%%b"=="version" ( 
               set JAVAVER=%%c
           )
       if /I "%%a"=="openjdk" if "%%b"=="version" ( 
               set JAVAVER=%%c
           )
    )
)
set "JAVA_VERSIONSTRING=%concat%"
rem -- print java version string all in one line --
echo %JAVA_VERSIONSTRING%

rem -- strip doublequotes from version number --
set JAVAVER=%JAVAVER:"=%

rem -- split java version (for processing) --

rem @echo Output: %JAVAVER%

for /f "delims=. tokens=1-3" %%v in ("%JAVAVER%") do (
    if [%%v] neq [] call :extractLeadingNumbers "%%v" major
    rem @echo Major: %%v
    if [%%w] neq [] call :extractLeadingNumbers "%%w" minor
    rem @echo Minor: %%w
    if [%%x] neq [] call :extractLeadingNumbers "%%x" patch
    rem @echo Patch: %%x
)
if [%major%] neq [] ( set "JAVAVER_MAJOR=%major%" ) else (
    echo ERROR: Could not detect java version number.
    set "ERROR=1"
    goto :end
)
if [%minor%] neq [] ( set "JAVAVER_MINOR=%minor%" ) else ( set "JAVAVER_MINOR=0" )
if [%patch%] neq [] ( set "JAVAVER_PATCH=%patch%" ) else ( set "JAVAVER_PATCH=0" )


rem -- java9-java11 need some packages explicitly added/exported --
if %JAVAVER_MAJOR% geq 9 if %JAVAVER_MAJOR% lss 12 (
  set JAVA_OPTS=%JAVA_OPTS% --add-exports java.base/jdk.internal.loader=ALL-UNNAMED ^
--add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED ^
--add-exports java.desktop/com.sun.java.swing.plaf.motif=ALL-UNNAMED ^
--add-exports java.desktop/com.sun.imageio.spi=ALL-UNNAMED --add-modules java.se.ee
)

rem -- detect if java is 64bit --
for /f "delims=" %%v in ('echo "%JAVA_VERSIONSTRING%"^|findstr /I "64-Bit"') do (
  set JAVA_X64=64
)

rem -- Change to jump home dir --
rem -- NOTE: mount UNC paths to a local drive for this --
cd /D "%JUMP_HOME%"

rem -- Uninstall if asked nicely ---
if [%1] == [--uninstall] ( 
  "%JAVA%" -jar .\uninstall\uninstaller.jar
  goto:eof
)

set LIB=lib

rem -- setup native lib paths
set NATIVE=%LIB%\native
if DEFINED ProgramFiles(x86) set X64=64
if DEFINED JAVA_X64 (
  set "JAVA_ARCH=x64"
) else (
  set "JAVA_ARCH=x86"
)
rem command ver example outputs
rem  german win7 "Microsoft Windows [Version 6.1.7601]"
rem  finnish xp  "Microsoft Windows XP [versio 5.1.2600]"
rem  french  xp  "Microsoft Windows XP [version 5.1.2600]"
set "ID=unknown"
rem --- XP Version 5.x ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 5[0-9\.]*]"') do (
  set "ID=xp"
)
rem --- Vista Version 6.0 ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 6.0.[0-9\.]*]"') do (
  set "ID=vista"
)
rem --- 7 Version 6.1 ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 6.1.[0-9\.]*]"') do (
  set "ID=seven"
)
rem --- 8 Version 6.2 ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 6.2.[0-9\.]*]"') do (
  set "ID=eight"
)
rem --- 8.1 Version 6.3 ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 6.3.[0-9\.]*]"') do (
  set "ID=eightone"
)
rem --- 10 Version 10.x ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 10.[0-9\.]*]"') do (
  set "ID=ten"
)
rem -- add native as fallthrough and lib\ext the legacy value and default system path --
if DEFINED X64 (
  set "NATIVEPATH=%NATIVE%\%ID%%X64%-%JAVA_ARCH%;%NATIVE%\%ID%%X64%"
  set NATIVE64CLASSPATHS="%NATIVE%\%ID%%X64%-%JAVA_ARCH%\*.jar" "%NATIVE%\%ID%%X64%\*.jar"
)
set "NATIVEPATH=%NATIVEPATH%;%NATIVE%\%ID%-%JAVA_ARCH%;%NATIVE%\%ID%"
set "NATIVEPATH=%NATIVEPATH%;%NATIVE%\%JAVA_ARCH%"
set "PATH=%NATIVEPATH%;%NATIVE%;%LIB%\ext;%PATH%"

rem -- debug info --
if /i NOT "%JAVA_BIN%"=="javaw" echo ---PATH--- & echo %PATH%

rem -- set classpath --
set CLASSPATH=.;bin;conf
rem -- add jars to classpath --
for %%i in (
  "%LIB%\*.jar" "%LIB%\*.zip" "%LIB%\imageio-ext\*.jar"
  %NATIVE64CLASSPATHS% 
  "%NATIVE%\%ID%-%JAVA_ARCH%\*.jar" "%NATIVE%\%ID%\*.jar" 
  "%NATIVE%\%JAVA_ARCH%\*.jar" "%NATIVE%\*.jar"
) do (
  set jarfile=%%i

  rem If we append to a variable inside the for, only the last entry will
  rem be kept. So append to the variable outside the for.
  rem See http://www.experts-exchange.com/Operating_Systems/MSDOS/Q_20561701.html.
  rem [Jon Aquino]

  call :setclass
)

rem -- set GDAL vars --
if DEFINED JAVA_X64 (
  set "GDAL_FOLDER=gdal-win-x64"
) else (
  set "GDAL_FOLDER=gdal-win-x86"
)
set "GDAL_FOLDER=lib\native\%GDAL_FOLDER%"
if EXIST "%GDAL_FOLDER%" (
  set "GDAL_DATA=%GDAL_FOLDER%\bin\gdal-data"
  set "GDAL_DRIVER_PATH=%GDAL_FOLDER%\bin\gdal\plugins"
  set "PATH=%GDAL_FOLDER%\bin;%GDAL_FOLDER%\bin\gdal\java;%PATH%"
  rem --- gdal binding is version specific, prioritize the one delivered with native libs ---
  set "CLASSPATH=%GDAL_FOLDER%\bin\gdal\java\gdal.jar;%CLASSPATH%"
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
    if DEFINED WRITEOK ( 
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

rem -- add memory settings --
if NOT DEFINED JAVA_MEM (
  call :memory
)

rem -- essential options, don't change unless you know what you're doing --
set JAVA_OPTS=%JAVA_OPTS% -Dlog4j.configuration="%LOG4J_CONF%" -Dlog.dir="%LOG_DIR%" -Djump.home="%JUMP_HOME%" %JAVA_MEM%

rem -- set default app options --
set JUMP_OPTS=-default-plugins bin\default-plugins.xml -state "%SETTINGS_HOME%" -plug-in-directory "%LIB%\ext" %JUMP_OPTS%
rem --- workbench-properties.xml is used to manually load plugins (ISA uses this) ---
if EXIST "bin\workbench-properties.xml" set "JUMP_OPTS=-properties bin\workbench-properties.xml %JUMP_OPTS%"

rem -- disconnect javaw from console by using start --
rem -- note: title is needed or start won't accept quoted path to java binary (protect spaces in javapath) --
if /i "%JAVA_BIN%"=="javaw" ( set START=start "" ) else ( set START= )
if /i NOT "%JAVA_BIN%"=="javaw" echo ---Start OJ---
 %START% "%JAVA%" -Djava.system.class.loader=com.vividsolutions.jump.workbench.plugin.PlugInClassLoader -cp "%CLASSPATH%" %JAVA_OPTS% com.vividsolutions.jump.workbench.JUMPWorkbench %JUMP_OPTS% %*

:end
cd /D %OLD_DIR%

rem -- give user a chance to see console output if 
rem --  we are in console mode but the app finished already
rem --  or ERROR is defined
set "PAUSE="
if /i NOT "%JAVA_BIN%"=="javaw" set "PAUSE=1"
if NOT [%ERROR%]==[] set "PAUSE=1"
if DEFINED PAUSE pause

goto:eof

:init
rem --- reset variables ---
set JAVA=
set JAVA_MEM=
set JAVA_BIN=
set SETTINGS_HOME=
goto:eof

:setclass
set "CLASSPATH=%CLASSPATH%;%jarfile%"
set jarfile=
goto:eof

:dequote
SETLOCAL enabledelayedexpansion
set string=%*
if DEFINED string set string=!string:"=!
ENDLOCAL & set "unquoted=%string%"
goto:eof

:concat
set "glue=%~1"
shift
set string=
:loop
set glue2=
if DEFINED string set "glue2= "
if NOT [%1]==[] (
  set "string=%string%%glue2%%1"
  shift
  goto :loop
)
if NOT "%concat%" == "" (
  set "string=%glue%%string%"
)
set "concat=%concat%%string%"
goto:eof

:memory
if /i NOT "%JAVA_BIN%"=="javaw" echo ---Detect maximum memory limit---

rem --- default java32 limit is 1GB ---
set /a "JAVA_XMX_X86=1024*1024"

rem --- detect ram size, values are in kB ---
for /f "delims=" %%l in ('wmic os get FreePhysicalMemory^,TotalVisibleMemorySize /format:list') do >nul 2>&1 set "OS_%%l"
if NOT DEFINED OS_TotalVisibleMemorySize goto mem_failed

set /a "JAVA_XMX=%OS_TotalVisibleMemorySize%"
set /a "JAVA_RAM_QUARTER=%OS_TotalVisibleMemorySize%/4"
rem --- a. cap to 1GB for 32bit jre ---
rem --- b. use xmx value if it fits into free space ---
rem --- c. use freemem value if bigger than 1/4 ram (jre default) ---
rem --- d. don't set, use jre default (works even though less than 1/4 ram might be free) ---

if NOT DEFINED JAVA_X64 if %JAVA_XMX% GTR %JAVA_XMX_X86% goto use_cap
goto use_max
rem if %OS_FreePhysicalMemory% GEQ %JAVA_XMX%
rem if %OS_FreePhysicalMemory% GTR %JAVA_RAM_QUARTER% goto :use_free
rem if /i NOT "%JAVA_BIN%"=="javaw" echo Less than 1/4 ram free. Use jre default.
goto:eof

:use_cap
  rem --- if x86 cap is bigger than actual ram, use ram size instead ---
  if %JAVA_XMX_X86% GTR %OS_TotalVisibleMemorySize% call :use_max
  call :xmx %JAVA_XMX_X86%
  if /i NOT "%JAVA_BIN%"=="javaw" echo set %JAVA_MEM_STRING% ^(32 bit jre maximum^)
  goto:eof
:use_max
  call :xmx %JAVA_XMX%
  if /i NOT "%JAVA_BIN%"=="javaw" echo set %JAVA_MEM_STRING% ^(ram maximum^)
  goto:eof
:use_free
  call :xmx %OS_FreePhysicalMemory%
  if /i NOT "%JAVA_BIN%"=="javaw" call echo set %JAVA_MEM_STRING% ^(free memory^)
  goto:eof
:mem_failed
  if /i NOT "%JAVA_BIN%"=="javaw" call echo skipped because: Couldn't determine ram size. Use safe 1GB value.
  call :xmx %JAVA_XMX_X86%
  goto:eof

:xmx
set /a "value=%1/1024"
set JAVA_MEM=-Xmx%value%M
set "JAVA_MEM_STRING=Xmx to %value%M"
goto:eof

rem This extracts the first numerical series in the input string
:extractLeadingNumbers inputString returnVar
setlocal enableextensions disabledelayedexpansion
rem Retrieve the string from arguments
set "string=%~1"

rem Use numbers as delimiters (so they are removed) to retrieve the rest of the string
for /f "tokens=1-2 delims=0123456789 " %%a in ("%string:^"=%") do set "delimiters=%%a%%b"

rem Use the retrieved characters as delimiters to retrieve the first numerical serie
for /f "delims=%delimiters% " %%a in ("%string:^"=%") do set "numbers=%%a"

rem Return the found data to caller and leave
endlocal & set "%~2=%numbers%"
goto :eof

:eof
