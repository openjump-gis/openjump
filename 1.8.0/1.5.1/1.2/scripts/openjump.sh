#!/bin/sh

JAVA_OPTS=-Xmx256M
MAIN=com.vividsolutions.jump.workbench.JUMPWorkbench
SAXDRIVER=org.apache.xerces.parsers.SAXParser
if(test -z $JAVA_HOME) then
  JAVA=`which java`
else
  JAVA=$JAVA_HOME/bin/java
fi
JUMP_HOME=`dirname $0`/..
JUMP_PROPERTIES=~/.jump/workbench-properties.xml
JUMP_STATE=~/.jump/

JUMP_PROFILE=~/.jump/openjump.profile
if [ -f "$JUMP_PROFILE" ]; then
  source $JUMP_PROFILE
fi

if [ -z "$JUMP_LIB" ]; then
  JUMP_LIB=$JUMP_HOME/lib
fi

if [ -z "$JUMP_PLUGIN_DIR" ]; then
  JUMP_PLUGIN_DIR=${JUMP_PLUGIN_DIR:=$JUMP_LIB/ext}
fi

if [ -z "$JUMP_PROPERTIES" -o ! -f $JUMP_PROPERTIES ]; then
  JUMP_PROPERTIES=~/.jump/workbench-properties.xml
fi

for libfile in $JUMP_LIB/*.jar $JUMP_LIB/*.zip
do
  CLASSPATH=$libfile:$CLASSPATH;
done
CLASSPATH=$JUMP_HOME:$JUMP_HOME/conf:$CLASSPATH
export CLASSPATH;

JUMP_OPTS="-plug-in-directory $JUMP_PLUGIN_DIR"
if [ -f "$JUMP_PROPERTIES" ]; then
  JUMP_OPTS="$JUMP_OPTS -properties $JUMP_PROPERTIES"
fi

if ( test -d "$JUMP_STATE" || test -f "$JUMP_STATE") then
  JUMP_OPTS="$JUMP_OPTS -state $JUMP_STATE"
fi
JAVA_OPTS="$JAVA_OPTS -Djump.home=$JUMP_HOME"
JAVA_OPTS="$JAVA_OPTS -Dorg.xml.sax.driver=$SAXDRIVER"
JAVA_OPTS="$JAVA_OPTS -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel"

$JAVA -cp $CLASSPATH:$JUMP_HOME/bin $JAVA_OPTS $MAIN $JUMP_OPTS $*
