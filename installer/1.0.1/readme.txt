OpenJUMP readme file
--------------------
Version 1.0.1
July 14th, 2006

Contents
--------
1. License
2. Installation instructions
3. Running OpenJUMP
4. Support
5. OpenJUMP history
6. Credits


1. License
----------
OpenJUMP is distributed under the GPL license. A description of this license
can be found in the "gpl.txt" file on the same location than this readme file.
OpenJUMP uses the BATIK libraries to write svg format. The BATIK libraries are
used under the terms of the APACHE license, which can be found in the "apache.txt" 
file or on www.apache.org.


2. Installation instructions
----------------------------
OpenJUMP comes in two flavours:
- A win32 installer
- A platform-independent zip archive

To install OpenJUMP from the win32 installer, just launch the program.

The platform-independent version of OpenJUMP comes under the form of a compressed archive.
To install, decompress the archive in your hard drive, for example into c:/OpenJUMP
You will see the following folder structure:
c:/OpenJUMP/
c:/OpenJUMP/bin
c:/OpenJUMP/lib


3. Running OpenJUMP
-------------------
The win32 installer version automatically installs an OpenJUMP menu in the start menu.

For the platform-independent version, run the startup scripts contained in the /bin folder:
- For windows, double-click on "JUMPWorkbench.bat"
- For Linux/Unix, launch JUMPWorkbench-unix.sh
- For Mac, launch JUMPWorkbench-mac.sh
- an improved script for unix exists: JUMPWorkbench-unix2.sh

Further notes can be found on our wiki: www.openjump.org

Users of looks extension should place all the jar files from looks-extension 
directly into /lib/ext.


Startup options
-----------------
Several startup options are available, either for the Java Virtual Machine, or for the
OpenJUMP core. To change them, edit the startup script
accordingly, editing the line beginning by "start javaw".

Java VM options (a complete list can be found in the Java VM documentation)
-Xms defines the allocated memory for the virtual machine at startup. Example: -Xms256M
 will allocate 256M of memory for OpenJUMP
-Xmx defines the maximum allocated memory for the virtual machine. Example: -Xmx256M
-Dproperty=value set a system property. For the moment, these properties are used:
  -Dswing.defaultlaf  for defining the OpenJUMP Look and Feel. Several possibilities:
     -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel for the Metal L&F
     -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel for the Windows L&F
  -Dlog4j.configuration for defining the configuration file for Log4j. Normally:
     Dlog4j.configuration=file:./log4j.xml
     
OpenJUMP command lines
-properties filename : specifies the name of the file where OpenJUMP properties are stored.
 Default is -properties workbench-properties.xml
-plug-in-directory path : defines the location of the plugin directory. 
 Default is %LIB%/ext where %LIB% is defined earlier in the startup script.
-i18n locale : defines the locale (language, etc.) used by OpenJUMP. For example:
  - For starting OpenJUMP in French: use -i18n fr
  - Other languages available: de (german), es (spanish), pt_BR (brazilian portuguese)
  - Default is english if the specified language is not implemented.

  
4. Support
----------
General questions regarding OpenJUMP can be found in:
- www.jump-project.org  the original JUMP site
- www.openjump.org the OpenJump website
- jump-pilot.sourceforge.net the old OpenJUMP developper site


5. OpenJUMP history
-------------------
OpenJUMP is a "fork" of the JUMP "Java Unified Mapping Platform" software, developed
by Vividsolutions and released in 2003.
During 2004, some enthusiastic developers joined together to enhance further the 
features of JUMP. They launched an independent development branch called OpenJUMP.
This name gives credit to the original JUMP development, and at the same time 
describes the particularity of this project to be fully open to anyone wanting
to contribute.
Since May 2005 a complete development environment is available at:
www.sourceforge.net/projects/jump-pilot
And a website for OpenJUMP is under construction at: www.openjump.org


6. Credits
----------
Many thanks to all the contributors of OpenJUMP for their time and efforts:

Original development team of JUMP was:
- Martin Davis, Jon Aquino, Alan Chang from Vividsolutions (www.vividsolutions.com)
- David Blasby and Paul Ramsey from Refractions Research Inc (www.refractions.net) 

OpenJUMP regular (and past) contributors are (non exaustive list!):
- Jonathan Aquino, 
- Landon Blake (Sunburned Surveyor),
- Erwan Bocher,
- Basile Chandesris,
- Joe Desbonet,
- Michael Michaud,
- Axel Orth,
- Ole Rahn,
- Ezequias Rodrigues da Rocha
- Stefan Steiniger,
- Ugo Taddei, 
- Steve Tanner,
and
- Larry Becker and Integrated Systems Analysts, Inc.
  for providing their Jump ISA tools code.


Translation contributors are:
- French: Basile Chandesris, Erwan Bocher, Steve Tanner
- Spanish: Steve Tanner
- German: Florian Rengers, Stefan Steiniger
- Portuguese (brazilian): Ezequias Rodrigues da Rocha

others:
- L. Paul Chew for providing the Delauney triangulation algorithm to create the Voronoi edges

