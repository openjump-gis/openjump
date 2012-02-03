#!/bin/sh

JAVA_OPTS=-Xmx256M
MAIN=com.vividsolutions.jump.workbench.JUMPWorkbench
SAXDRIVER=org.apache.xerces.parsers.SAXParser 
JAVA=`which java`;
JUMP_HOME=`dirname $0`/..
JUMP_PROPERTIES=~/.jump/workbench-properties.xml

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
  JUMP_PROPERTIES=$OPENJUMP_HOME/bin/workbench-properties.xml
fi

for libfile in $JUMP_LIB/*.jar $JUMP_LIB/*.zip $JUMP_LIB/batik/*.jar
do
  CLASSPATH=$libfile:$CLASSPATH;
done
CLASSPATH=$OPENJUMP_HOME/conf:$CLASSPATH
export CLASSPATH;

JUMP_OPTS="-plug-in-directory $JUMP_PLUGIN_DIR"
if [ -f "$JUMP_PROPERTIES" ]; then
  JUMP_OPTS=$JUMP_OPTS -properties $JUMP_PROPERTIES
fi

$JAVA -cp $CLASSPATH:$JUMP_HOME/bin $JAVA_OPTS -Dorg.xml.sax.driver=$SAXDRIVER $MAIN $JUMP_OPTS
    
