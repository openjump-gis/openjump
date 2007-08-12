set LIB=../lib
set CLASSPATH=../conf;%LIB%/bsh-2.0b4.jar;%LIB%/Buoy.jar;%LIB%/Jama-1.0.1.jar;%LIB%/jdom.jar;%LIB%/jts-1.7.2.jar;%LIB%/jump-workbench-@VERSION@.jar;%LIB%/jump-api-@VERSION@.jar;%LIB%/xercesImpl.jar;%LIB%/xml-apis.jar;%LIB%/xml-apis-ext.jar;%LIB%/log4j-1.2.8.jar;%LIB%/batik-all.jar;%LIB%/jmat_5.0m.jar;%LIB%/ermapper.jar;%LIB%/jai_codec.jar;%LIB%/jai_core.jar;%LIB%/ext
REM Add extension directory to path, so extensions can put DLL's there [Jon Aquino 2005-03-18]
set PATH=%PATH%;%LIB%/ext
start javaw -Dlog4j.configuration=file:./log4j.xml -Xms256M -Xmx256M -cp %CLASSPATH% com.vividsolutions.jump.workbench.JUMPWorkbench -properties workbench-properties.xml -plug-in-directory %LIB%/ext
