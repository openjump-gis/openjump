#!/bin/sh

## uncomment and put in the path where oj log, settings should end up
## if unset defaults to
##   JUMP_HOME (oj app folder) if writable or $HOME/.openjump (user home)
#JUMP_SETTINGS="/tmp/foobar"

## uncomment and put the path to your jre here
#JAVA_HOME="/home/ed/jre1.6.0_21"

## uncomment and change your memory configuration here 
## Xms is initial size, Xmx is maximum size
## values are ##M for ## Megabytes, ##G for ## Gigabytes
#JAVA_MAXMEM="-Xmx512M"

## uncomment and change your language/country here 
## to overwrite OS default locale setting
#JAVA_LANG="-Duser.language=de -Duser.country=DE"

## set some defaults (as macosx.command uses this script, it might define other defaults)
MAIN="com.vividsolutions.jump.workbench.JUMPWorkbench"
JAVA_SAXDRIVER=${JAVA_SAXDRIVER-org.apache.xerces.parsers.SAXParser}
JAVA_LOOKANDFEEL=${JAVA_LOOKANDFEEL-javax.swing.plaf.metal.MetalLookAndFeel}

## check if $1 is a number utility function
is_number(){
  expr "$1" : '^[0-9][0-9]*$' > /dev/null 2>&1
}

## check if $1 is a decimal utility function
is_decimal(){
  expr "$1" : '^[0-9][0-9]*\.[0-9][0-9]*$' > /dev/null 2>&1
}

## check if parameter is an absolute path
relPath(){ echo "$@" | awk '/^\//{exit 1}'; }

## end function, delays closing of terminal
end(){
  # show error for some time to prohibit window closing on X
  if [ "$ERROR" != "0" ]; then
    read -p "press Enter to finish" foobar
    exit 1
  else
    exit 0
  fi
}

extract_libs(){
  [ ! -d "$1" ] && echo Missing extraction folder && return 1
  # extract zipped files in native dir (our way to ship symlinks to desktops)
  for filepath in $(find "$1/" -name '*.tgz' -o -name '*.tar.gz')
  do
    file=$(basename "$filepath")
    folder=$(dirname "$filepath")
    done=".$file.unzipped"

    # we create a marker file symbolizing previous successful extraction
    ( cd "$folder/"; [ -f "$file" ] && [ ! -f "$done" ] && tar -xvf "$file" && touch "$done" );
  done
}

postinstall(){
  [ ! -d "$1" ] && echo Missing app folder && exit 1
  # fix permissions
  find "$1" -type f -exec chmod 644 {} \; &&\
  find "$1" -type d -exec chmod 755 {} \; &&\
  find "$1" -type f \( -name \*.sh -o -name \*.command -o -name script -o -name OpenJUMP \) -print -exec chmod 755 {} \; &&\
  echo permissions fixed
  extract_libs "$1/lib/native" &&\
  echo native libs extracted
  which xdg-desktop-menu && xdg-desktop-menu forceupdate && echo reloaded desktop
}

macinstall(){
  # create app package
  cp -R -a "$1"/bin/OpenJUMP.app/Contents "$1" &&\
  awk '{sub(/..\/oj_/,"bin/oj_",$0)}1' "$1"/bin/OpenJUMP.app/Contents/Resources/script > "$1"/Contents/Resources/script &&\
  echo patched oj.app
  # do postinstall
  postinstall "$1"
}

ME="$0"
## absolutify home folder
relPath "$ME" && ME="$(pwd)/$ME"
# filter multiple // and ./ and replace ..
ME=$(echo "$ME"|awk '{gsub(/\/+/,"/",$0);gsub(/\/\.\//,"/",$0);while($0~/\/\.\.\//&&last!=$0){last=$0;gsub(/\/([\.][^\.]|[^\.][\.]|[^\.][^\.]|[^\/][^\/][^\/]+)\/\.\.\//,"/",$0);};print}')
# resolv possible link to our actual location 
while [ -L "$ME" ]; do
  MEBASE=$(dirname "$ME")
  ME=$(readlink -n "$ME")
  relPath "$ME" && ME="$MEBASE/$ME"
done
JUMP_HOME=$(dirname "$(dirname "$ME")")
echo ---JUMP_HOME---
echo $JUMP_HOME

## run postinstalls only, if requested
case "$1" in
 --post-install)
  postinstall "$JUMP_HOME" 2>&1 | tee "$JUMP_HOME"/postinstall.log
  exit
 ;;
 --mac-install) 
  macinstall "$JUMP_HOME" 2>&1 | tee "$JUMP_HOME"/macinstall.log
  exit
 ;;
esac

## cd into jump home
OLD_DIR=`pwd`
cd "$JUMP_HOME"

## determine where to place settings, if no path given
[ -z "$JUMP_SETTINGS" ] && \
JUMP_SETTINGS="$JUMP_HOME"; \
if [ -d "$JUMP_SETTINGS" ]; then
  if [ ! -w "$JUMP_SETTINGS" ]; then
    # try users home dir
    JUMP_SETTINGS="$HOME/.openjump"
    # create if missing
    [ ! -e "$JUMP_SETTINGS" ] && mkdir "$JUMP_SETTINGS"
    # try to make it writable
    [ ! -w "$JUMP_SETTINGS" ] && chmod u+wX -R "$JUMP_SETTINGS"
    # check availability and issue warning in case
  fi
fi
if [ ! -d "$JUMP_SETTINGS" ] || [ ! -w "$JUMP_SETTINGS" ]; then 
  echo "Warning: Cannot access settings folder '$JUMP_SETTINGS' for writing."
fi

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
add the location of java to your PATH environment variable." && ERROR=1 && end

# resolve recursive links to java binary
relPath "$JAVA" && JAVA="$(pwd)/$JAVA"
while [ -L "${JAVA}" ]; do
  JDIR=$(dirname "$JAVA")
  JAVA_CANDIDATE=$(readlink -n "${JAVA}")
  # protect against Gentoo's run-java-tool.bash wrapper
  if [ $(basename "$JAVA") != $(basename "$JAVA_CANDIDATE") ]; then
    break
  fi
  JAVA="$JAVA_CANDIDATE"
  relPath "$JAVA" && JAVA="${JDIR}/${JAVA}"
done
# java executable file?
[ ! -x "$JAVA" ] && \
 echo "The found java binary '$JAVA' is no executable file." && ERROR=1 && end

# java version check
JAVA_VERSIONSTRING="$("$JAVA" -version 2>&1)"
JAVA_VERSION=$( echo $JAVA_VERSIONSTRING | awk 'BEGIN{done=0}{gsub(/["\047]+/,"")}/[a-zA-Z]+ version [0-9]+/{split($3,a,"[^0-9]"); if(match(a[2],/^[0-9]+$/)){print a[1]"."a[2]}else{print a[1]".0"}; done=1}END{if(!done)exit 1}' ) 
JAVA_ARCH=$(echo $JAVA_VERSIONSTRING | grep -q -i 64-bit && echo x64 || echo x86)
JAVA_NEEDED="1.6"
if ! is_decimal "$JAVA_VERSION"; then
  echo "Warning! Your java version could not be detected properly. Please report this issue to the OJ developer list. The result was
---JAVA_VERSIONSTRING---
$JAVA_VERSIONSTRING
---JAVA_VERSION---
$JAVA_VERSION
---"
elif ! awk "BEGIN{if($JAVA_VERSION < $JAVA_NEEDED)exit 1}"; then
  echo "Your java version '$JAVA_VERSION' is insufficient to run OpenJUMP.
Please provide an at least a version '$JAVA_NEEDED' Java Runtime."
  ERROR=1
  end
fi

# always print java infos
echo ---JAVA---
echo "Using '$(basename "${JAVA}")' found in '$(dirname "${JAVA}")'"
"$JAVA" -version 2>&1|awk 'BEGIN{ORS=""}{print $0"; "}END{print "\n"}'

JUMP_PROFILE=~/.jump/openjump.profile
if [ -f "$JUMP_PROFILE" ]; then
  source $JUMP_PROFILE
fi

# setup some lib paths
if [ -z "$JUMP_LIB" ]; then
  JUMP_LIB="./lib"
fi
JUMP_NATIVE_DIR="$JUMP_LIB/native"
JUMP_PLUGIN_DIR="${JUMP_PLUGIN_DIR:=$JUMP_LIB/ext}"

JUMP_PLUGINS=./bin/default-plugins.xml
if [ -z "$JUMP_PLUGINS" ] || [ ! -f "$JUMP_PLUGINS" ]; then
  JUMP_PLUGINS="./bin/default-plugins.xml"
  if [ ! -f "$JUMP_PLUGINS" ]; then
    JUMP_PLUGINS="./scripts/default-plugins.xml"
  fi
fi

# include every jar/zip in lib and native dir
for libfile in "$JUMP_LIB/"*.zip "$JUMP_LIB/"*.jar "$JUMP_NATIVE_DIR/$JAVA_ARCH/"*.jar "$JUMP_NATIVE_DIR/"*.jar
do
  CLASSPATH="$libfile":"$CLASSPATH";
done
CLASSPATH=.:./bin:./conf:$CLASSPATH
export CLASSPATH;

## compile jump opts
#
JUMP_OPTS="-plug-in-directory $JUMP_PLUGIN_DIR"
if [ -f "$JUMP_PLUGINS" ]; then
  JUMP_OPTS="$JUMP_OPTS -default-plugins $JUMP_PLUGINS"
fi
# workbench-properties.xml is used to manually load plugins (ISA uses this)
JUMP_PROPERTIES=./bin/workbench-properties.xml
if [ -n "$JUMP_PROPERTIES" ] && [ -f "$JUMP_PROPERTIES" ]; then
  JUMP_OPTS="$JUMP_OPTS -properties $JUMP_PROPERTIES"
fi

# compile jre opts, respect already set ones from e.g. mac
JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS $JAVA_MAXMEM $JAVA_LANG"
JAVA_OPTS="$JAVA_OPTS -Djump.home=."
[ -n "JAVA_SAXDRIVER"    ] && JAVA_OPTS="$JAVA_OPTS -Dorg.xml.sax.driver=$JAVA_SAXDRIVER"
[ -n "$JAVA_LOOKANDFEEL" ] && JAVA_OPTS="$JAVA_OPTS -Dswing.defaultlaf=$JAVA_LOOKANDFEEL"
JAVA_OPTS="$JAVA_OPTS $JAVA_OPTS_OVERRIDE"

# java9 needs some packages explicitly added/exported
if awk "BEGIN{if($JAVA_VERSION >= 9)exit 0;else exit 1}"; then
  JAVA_OPTS="--add-exports java.base/jdk.internal.loader=ALL-UNNAMED \
--add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED \
--add-exports java.desktop/com.sun.java.swing.plaf.motif=ALL-UNNAMED \
--add-exports java.desktop/com.sun.imageio.spi=ALL-UNNAMED \
$JAVA_OPTS"
fi
# java ee was removed from jdk in java 11
if awk "BEGIN{if( $JAVA_VERSION >= 9 && $JAVA_VERSION < 11 )exit 0;else exit 1}"; then
  JAVA_OPTS="--add-modules java.se.ee $JAVA_OPTS"
fi

# in case some additional archives were placed in native dir inbetween
extract_libs "$JUMP_NATIVE_DIR"

# allow jre to find native libraries in native dir, lib/ext (backwards compatibility)
NATIVE_PATH="$JUMP_NATIVE_DIR/$JAVA_ARCH:$JUMP_NATIVE_DIR:$JUMP_PLUGIN_DIR"
# allow jre to find executable binaries located under the native folder
export PATH="$JUMP_NATIVE_DIR:$PATH"

# generate gdal settings
export GDAL_DATA="$JUMP_NATIVE_DIR/gdal-linux-data"
GDALPATH="$JUMP_NATIVE_DIR/gdal-linux-$JAVA_ARCH"
NATIVE_PATH="$GDALPATH:$GDALPATH/lib:$GDALPATH/java:$NATIVE_PATH"
CLASSPATH="$GDALPATH/java/gdal.jar:$CLASSPATH"

# export (DY)LD_LIBRARY_PATH depending on platform
if [ "$(basename "$0")" = "oj_macosx.command" ]; then
  ## Export environment variables for C-coded functions.
  export DYLD_LIBRARY_PATH="$NATIVE_PATH:$DYLD_LIBRARY_PATH"
  echo ---DYLD_LIBRARY_PATH---
  echo $DYLD_LIBRARY_PATH
else
  export LD_LIBRARY_PATH="$NATIVE_PATH:$LD_LIBRARY_PATH"
  echo ---LD_LIBRARY_PATH---
  echo $LD_LIBRARY_PATH
fi

echo ---PATH---
echo $PATH

echo ---CLASSPATH---
echo $CLASSPATH

echo ---Save logs \& state to--- 
echo "$JUMP_SETTINGS/"

echo ---Detect maximum memory limit---
# use previously set or detect RAM size in bytes
RAM_SIZE=${RAM_SIZE-$(expr "$(awk '/MemTotal/{print $2}' /proc/meminfo)" \* 1024)}
if [ -n "$JAVA_MAXMEM" ]; then
  echo "max. memory limit defined via JAVA_MAXMEM=$JAVA_MAXMEM"
elif ! is_number "$RAM_SIZE"; then
  echo "failed to detect system RAM size, using default max. memory limit of 512 MiB"
  JAVA_MAXMEM="-Xmx512M"
else
  # calculate 80% RAM (in bytes)
  MEM_80PCT=`expr "$RAM_SIZE" \* 80 / 100`
  # calculate RAM size minus 1GiB, for big RAM machines we protect max. 1GiB
  # e.g. for 80% of 16GiB not to waste 3.2GiB
  MEM_MINUS1GB=`expr "$RAM_SIZE" - \( 1024 \* 1024 \* 1024 \)`
  # use whatever is bigger
  if [ "$MEM_80PCT" -gt "$MEM_MINUS1GB" ]; then
    MEM_MAX="$MEM_80PCT"
  else
    MEM_MAX="$MEM_MINUS1GB"
  fi

  # limit 32bit jre to 2GiB = 2147483648 bytes
  if [ "$JAVA_ARCH" != "x64" ] && [ "$MEM_MAX" -gt "2147483648" ]; then
    MEM_MAX=2147483648
  fi

  MEM_MAX_MB=`expr $MEM_MAX / 1024 / 1024`
  JAVA_MAXMEM="-Xmx${MEM_MAX_MB}M"
  # output info
  echo set max. memory limit to $MEM_MAX_MB MiB
fi

# eventually try to start if no errors so far
if [ -z "$ERROR" ]; then
  echo ---Start OJ---
  # log.dir needs a trailing slash for path concatenation in log4j.xml
"$JAVA" -Djava.system.class.loader=com.vividsolutions.jump.workbench.plugin.PlugInClassLoader -cp "$CLASSPATH" -Dlog.dir="$JUMP_SETTINGS/" $JAVA_OPTS $MAIN -state "$JUMP_SETTINGS/" $JUMP_OPTS "$@"
  # result of jre call
  ERROR=$?
fi

## return to old working dir
cd "$OLD_DIR"

## run end function
end
