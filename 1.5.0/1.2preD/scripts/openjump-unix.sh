#!/bin/sh
#To make this script executable, use chmod a+x JUMPWorkbench-unix.sh

# You may need to uncomment the next three lines on some linux systems (Fedora?)

# JUMPHOME=/usr/local/jump/bin
# JAVA_HOME=/usr/java/jre1.5.0_04/bin
# cd $JUMPHOME 

# The next lines are required in any case
LIB=../lib
CLASSPATH=$LIB/bsh-2.0b4.jar:$LIB/Buoy.jar:$LIB/Jama-1.0.1.jar:$LIB/jdom.jar:$LIB/jts-1.7.2.jar:$LIB/jump-workbench-@VERSION@.jar:$LIB/jump-api-@VERSION@.jar:$LIB/xercesImpl.jar:$LIB/xml-apis.jar:$LIB/xml-apis-ext.jar:$LIB/log4j-1.2.8.jar:$LIB/batik-all.jar:$LIB/jmat_5.0m.jar:$LIB/ermapper.jar:$LIB/jai_core.jar:$LIB/jai_codec.jar:$LIB/ext
java -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel -Dlog4j.configuration=file:./log4j.xml -Xms256M -Xmx256M -cp $CLASSPATH com.vividsolutions.jump.workbench.JUMPWorkbench -properties workbench-properties.xml -plug-in-directory $LIB/ext

