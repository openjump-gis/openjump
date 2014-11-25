Put native libraries (e.g. *.dll,*.so) into this folder.


Notes for Mac, Linux

Currently native libs are searched in the following paths and order
lib/native/linux-$JAVA_ARCH ( JAVA_ARCH being x86 [32bit] or x64 [64bit] )
lib/native/
lib/ext
$LD_LIBRARY_PATH

as file extensions for native jre libs differ on OSX (*.dylib) and Linux (*.so) you may place 
e.g. mac 32bit java libraries under lib/native/linux-x32 and they will be 
found.


Notes for Windows

The native library include path generation is more elaborate on this platform.
That's because different windows version may run specific native library 
versions more or less successful. Therefor the path is generated more 
finegrained.
Native libs are being searched in the following paths and order
lib\native\%ID%-%JAVA_ARCH% 
  ( ID being either xp, xp64, vista, vista64, seven, seven64, 
                    eight, eight64, eightone, eightone64; 
    JAVA_ARCH being x86 [32bit] or x64 [64bit] )
lib\native\%ID%
lib\native\win-%JAVA_ARCH%
lib\native
%LIB%\ext
%PATH%


Conclusion

If you don't care and run OJ only with one specific java version simply drop
the fitting native libs into 'lib/native'.

If you run both, e.g. in a corporate environment from a network share, while
having different java versions installed on different Windows workstations, 
then place 32bit libraries into 
 lib/native/win-x86
and 64bit libraries into 
 lib/native/win-x86
.
Create the folders if necessary.

Same goes for Linux or OSX workstations. Use
 lib/native/linux-x64
and
 lib/native/linux-x86
analogously.

If you find some libs only working on a specific windows, like e.g. 
Windows Vista 64bit with 32bit java then you may place these in
 lib\native\vista64-x86
. Keep versions working on the other platforms in the above mentioned folders.