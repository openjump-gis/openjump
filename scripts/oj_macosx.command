#/bin/sh
##
# See oj_linux.sh for some parameters you might want to tune
# e.g. max memory, language, jre to use
##
CDIR=`dirname "$0"`
## define a default look&feel(laf)
#  "" - empty for system default
#  "javax.swing.plaf.metal" - for a cross platform ui, if problems occure
JAVA_LOOKANDFEEL=""
## some parameters to make OJ shiny ;)
JAVA_OPTS_OVERRIDE="-Xdock:name=OpenJUMP -Xdock:icon=./bin/OpenJUMP.app/Contents/Resources/oj.icns"
## run the real magic now
. "$CDIR/oj_linux.sh"
