Place native libraries (e.g. *.dll,*.so,*.dylib) into this folder or 
it's subfolders.


Notes for Linux, Mac

Currently native libs are searched in the following paths and order
lib/native/$JAVA_ARCH ( JAVA_ARCH being x86 [32bit] or x64 [64bit] )
lib/native/
lib/ext
$LD_LIBRARY_PATH or $DYLD_LIBRARY_PATH

file extensions for native java library files differ between OSX (*.dylib)
and Linux (*.so). hence you can place Mac and Linux 32bit java libraries under 
 lib/native/x86
and they will be found on each platform.


Notes for Windows

The native library include path generation is more elaborate on this platform.
That's because different windows version may run specific native library 
versions more or less successful. Therefore the path is generated more 
finegrained.
Native libs are being searched in the following paths and order
lib\native\%ID%-%JAVA_ARCH% 
  ( ID being either xp, xp64, vista, vista64, seven, seven64, 
                    eight, eight64, eightone, eightone64; 
    JAVA_ARCH being x86 [32bit] or x64 [64bit] )
lib\native\%ID%
lib\native\%JAVA_ARCH%
lib\native
%LIB%\ext
%PATH%


Conclusion

If you don't care and run OJ only with one specific java version simply drop
the fitting native libs into 'lib/native'.

If you run several, e.g. running OJ from a network share in a corporate 
environment, while having different java versions installed on different 
Windows workstations, then place 32bit java libraries into 
 lib/native/x86
and 64bit libraries into 
 lib/native/x64
.

Same goes for Linux or OSX workstations. Use
 lib/native/x64
and
 lib/native/x86
analogously.

If you find some libs working only on a specific windows, like e.g. 
Windows Vista 64bit with 32bit java then you can create
 lib\native\vista64-x86
and place these in there.
Versions working on all other Windows should go in the above mentioned folders.