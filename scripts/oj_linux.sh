#!/bin/sh

## uncomment and put the path to your jre here
#JAVA_HOME="/home/ed/jre1.6.0_21"

## uncomment and change your memory configuration here 
## Xms is initial size, Xmx is maximum size
## values are ##M for ## Megabytes, ##G for ## Gigabytes
#JAVA_MAXMEM="-Xmx512M"

## set some defaults (as macosx.command uses this script, it might define other defaults)
MAIN="com.vividsolutions.jump.workbench.JUMPWorkbench"
[ -z "$JAVA_SAXDRIVER" ] && JAVA_SAXDRIVER="org.apache.xerces.parsers.SAXParser"
[ -z "$JAVA_LOOKANDFEEL" ] && JAVA_LOOKANDFEEL="javax.swing.plaf.metal.MetalLookAndFeel"
[ -z "$JAVA_MAXMEM" ] && JAVA_MAXMEM="-Xmx512M"

if(test -L $0) then
    	auxlink=`ls -l $0 | sed 's/^[^>]*-> //g'`
    	JUMP_HOME=`dirname $auxlink`/..
else 
    	JUMP_HOME=`dirname $0`/..
fi
JUMP_PROPERTIES=$JUMP_HOME/bin/workbench-properties.xml
JUMP_DEFAULTP=$JUMP_HOME/bin/default-plugins.xml
JUMP_STATE=$JUMP_HOME/bin/

## search java, order is:
# 1. first in oj_home/jre
# 2. in configured java_home
# 3. in path
if [ -f "$JUMP_HOME/jre/bin/java" ]; then
  JAVA="$JUMP_HOME/jre/bin/java"
# is there a jre defined by env var?
elif [ -n "$JAVA_HOME" ]; then
  JAVA=$JAVA_HOME/bin/java
# well, let's look what we've got in the path
else
  JAVA=`which java`
fi

# java available
[ -z "$JAVA" ] && \
 echo "Couldn't find java in your PATH ($PATH). Please install java or 
add the location of java to your PATH environment variable." && ERROR=1

# java executable file?
while [ -L "$JAVA" ]; do
  JAVA=$(readlink -n $JAVA)
done
[ ! -x "$JAVA" ] && \
 echo "The found java binary '$JAVA' is no executable file." && ERROR=1

# java version check
JAVA_VERSION=$($JAVA -version 2>&1 | awk -F'"' '/^java version/{print $2}' | awk -F'.' '{print $1"."$2}')
JAVA_NEEDED="1.5"
if ! awk "BEGIN{if($JAVA_VERSION < $JAVA_NEEDED)exit 1}"; then
  echo "Your java version '$JAVA_VERSION' is insufficient to run openjump.
Please provide an at least version '$JAVA_NEEDED' java runtime."
  ERROR=1
fi

# always print java version info
$JAVA -version

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

if [ -z "$JUMP_PROPERTIES" ] || [ ! -f $JUMP_PROPERTIES ]; then
  JUMP_PROPERTIES=$JUMP_HOME/bin/workbench-properties.xml
fi

if [ -z "$JUMP_DEFAULTP" ] || [ ! -f $JUMP_DEFAULTP ]; then
  JUMP_DEFAULTP=$JUMP_HOME/bin/default-plugins.xml
fi

if [ -z "$JUMP_DEFAULTP" ] || [ ! -f $JUMP_DEFAULTP ]; then
  JUMP_DEFAULTP=$JUMP_HOME/scripts/default-plugins.xml
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

if [ -f "$JUMP_DEFAULTP" ]; then
  JUMP_OPTS="$JUMP_OPTS -default-plugins $JUMP_DEFAULTP"
fi

if ( test -d "$JUMP_STATE" || test -f "$JUMP_STATE") then
  JUMP_OPTS="$JUMP_OPTS -state $JUMP_STATE"
fi
JAVA_OPTS="$JAVA_MAXMEM"
JAVA_OPTS="$JAVA_OPTS -Djump.home=$JUMP_HOME"
JAVA_OPTS="$JAVA_OPTS -Dorg.xml.sax.driver=$JAVA_SAXDRIVER"
JAVA_OPTS="$JAVA_OPTS -Dswing.defaultlaf=$JAVA_LOOKANDFEEL"

# try to start if no errors so far
if [ -z "$ERROR" ]; then
  $JAVA -cp $CLASSPATH:$JUMP_HOME/bin $JAVA_OPTS  $MAIN $JUMP_OPTS $*
  # result of java call
  ERROR=$?
fi

# show error for some time to prohibit window closing on X
if [ "$ERROR" != "0" ]; then
  sleep 15
fi
