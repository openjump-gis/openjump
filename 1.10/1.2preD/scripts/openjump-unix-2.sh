#!/bin/sh

#    A more sophisticated startup script contributed by Djun M. Kim of
# Cielo Systems Inc. Advantages:

#   - doesn't depend on jar names, 
#   - works properly under both MacOS X and Linux, 
#   - can be placed anywhere on $PATH
#   - supports (?) multiple JUMP applications from a single JUMP installation.

#    This is a Bourne shell/Bash script which has been tested under Mandrake
# Linux 9.1 and MacOS X 10.2.8.  It should work under pretty much any
# Unix-like OS.

#    The behaviour of the script is controlled by three
# environment variables - it's perhaps easier to tweak 
# things this way than having people edit shell scripts.

# ENVIRONMENT VARIABLES

#    JUMPHOME - this is the directory where JUMP is installed.  So, 
# for example, the JUMP jar would be in $JUMPHOME/lib.
#    Default value: 
#        MacOS X: /Library/JUMP
#        Linux:   /usr/local/jump
#        Other:   /usr/local/jump

#    JUMP_PLUGIN_DIR - the directory where JUMP plugins are found.
# Having a separate directory for these should allow one JUMP installation
# to support multiple installations.
#    Default value:
#        MacOS X: $JUMPHOME/lib/ext
#        Linux:   $JUMPHOME/lib/ext
#        Other:   $JUMPHOME/lib/ext

#    PROPS - the path to the workbench-properties.xml file. 
#    Default value:
#        MacOS X: $JUMPHOME/bin/workbench-properties.xml
#        Linux:   $JUMPHOME/bin/workbench-properties.xml
#        Other:   $JUMPHOME/bin/workbench-properties.xml

  
#    A couple of notes:  

#    This script can be placed anywhere on $PATH.

#    The script looks for jar files first in the lib
# directory of $JUMPHOME and then the $JUMP_PLUGIN_DIR and 
# *prepends* them to the CLASSPATH in the order that shell 
# wildcard expansion returns them.  This may have subtle 
# effects if there are multiply defined classes.  The first 
# class found will be loaded, subsecquent classes ignored.

#    This file needs to keep the Unix LF line terminators
# if it is to work correctly :)

#    This script is based on earlier scripts contributed to the
# JUMP discussion list by Raj Singh (Mac OS X) and Cameron Shorter
# (Linux), and perhaps others...
# Original version: Djun M. Kim
# Last modifs by S. Tanner, 2006 January 10th:
# - added $LIB/batik/*.jar
# - replaced -Xmx512M by -Xmx256M (safer, not everyone has 1 Go of memory onboard...)




MAIN=com.vividsolutions.jump.workbench.JUMPWorkbench
SAXDRIVER=org.apache.xerces.parsers.SAXParser 

# This next line isn't terribly safe... but no worse than
# asking the shell to find a 'java' executable.
# If you know what version of Java you want to use and 
# where is lives, enter the absolute path to your java JVM 
# instead.
JAVA=`which java`;

OS=`uname -s`;

# Comment the next line out to turn off verbose messages
VERBOSE=1 

if [ "x$OS" = "xDarwin" ]
then
    if [ $VERBOSE ]
      then 
      echo "Setting up for Mac OS X/Darwin";
    fi

    # The next li/ne sets JUMPHOME to be the value of the Environment
    # variable JUMPHOME, or the given directory if $JUMPHOME is null/undefined
    JUMPHOME=${JUMPHOME:=/Library/JUMP}
    JUMP_PLUGIN_DIR=${JUMP_PLUG_DIR:=$JUMPHOME/lib/ext}
    PROPS=${PROPS:=$JUMPHOME/bin/workbench-properties.xml}

    LIB=$JUMPHOME/lib

    MACSTUFF="-Xdock:name=JUMP -Dapple.laf.useScreenMenuBar=true \
          -Dapple.awt.showGrowBox=true" 

    for jarfile in $LIB/*.jar $LIB/batik/*.jar $JUMP_PLUGIN_DIR/*.jar
      do
      CLASSPATH=$jarfile:$CLASSPATH;
      if [ $VERBOSE ]
      then
         echo adding $jarfile to CLASSPATH;
      fi
    done
    export CLASSPATH;
    
    if [ $VERBOSE ]
      then 
      echo "CLASSPATH is: ";
      echo $CLASSPATH | tr ':' '\n';
    fi

    $JAVA -cp $CLASSPATH $MACSTUFF -Xmx256M -Dorg.xml.sax.driver=$SAXDRIVER \
      $MAIN -properties $PROPS -plug-in-directory $JUMP_PLUGIN_DIR
    
    exit 0;
    
elif [ "x$OS" == "xLinux" ]
then
    if [ $VERBOSE ]
      then 
      echo "Setting up for Linux";
    fi

    # The next line takes JUMPHOME to be the value of the Environment
    # variable JUMPHOME, or the given directory if $JUMPHOME is null/undefined
    JUMPHOME=${JUMPHOME:=/usr/local/jump}
    JUMP_PLUGIN_DIR=${JUMP_PLUGIN_DIR:=$JUMPHOME/lib/ext}
    PROPS=${PROPS:=$JUMPHOME/bin/workbench-properties.xml}

    LIB=$JUMPHOME/lib

    for jarfile in $LIB/*.jar $LIB/batik/*.jar $JUMP_PLUGIN_DIR/*.jar
      do
      CLASSPATH=$jarfile:$CLASSPATH;
      echo adding $jarfile to CLASSPATH
    done
    export CLASSPATH;

    if [ $VERBOSE ]
      then 
      echo "CLASSPATH is: ";
      echo $CLASSPATH | tr ':' '\n';
    fi
    
    $JAVA -cp $CLASSPATH -Xmx256M -Dorg.xml.sax.driver=$SAXDRIVER $MAIN \
      -properties $PROPS \
      -plug-in-directory $JUMP_PLUGIN_DIR
    
    exit 0;
else # Some other OS / let's pretend it's fairly standard Unix type system
    
    # The next line takes JUMPHOME to be the value of the Environment
    # variable JUMPHOME, or the given directory if $JUMPHOME is null/undefined
    JUMPHOME=${JUMPHOME:=/usr/local/jump}
    JUMP_PLUGIN_DIR=${JUMP_PLUGIN_DIR:=$JUMPHOME/lib/ext}
    PROPS=${PROPS:=$JUMPHOME/bin/workbench-properties.xml}

    LIB=$JUMPHOME/lib

    for jarfile in $LIB/*.jar $LIB/batik/*.jar $JUMP_PLUGIN_DIR/*.jar
      do
      CLASSPATH=$jarfile:$CLASSPATH;
      echo adding $jarfile to CLASSPATH
    done
    export CLASSPATH;

    if [ $VERBOSE ]
      then 
      echo "CLASSPATH is: ";
      echo $CLASSPATH | tr ':' '\n';
    fi
    
    $JAVA -cp $CLASSPATH -Xmx256M -Dorg.xml.sax.driver=$SAXDRIVER $MAIN \
      -properties $PROPS \
      -plug-in-directory $JUMP_PLUGIN_DIR
    
    exit 0;
fi
