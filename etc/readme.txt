OpenJUMP readme file
--------------------
Version 1.4.2

September 11th, 2011

Contents
--------
1. License
2. Installation instructions
3. Running OpenJUMP
4. Support
5. OpenJUMP history
6. Credits


1. Licensing
------------
OpenJUMP is distributed under the GPL2 license. A description of this license
can be found in the "gpl2_license.txt" (GPL2) file in the "licenses" folder.

OpenJUMP uses and distributes the following:
BATIK libraries to write svg format are used under the terms of the 
    Apache License Version 2.0, found in the file "apache_license.txt".
BeanShell distributed under LGPL2.1 ("lgpl-2.1.txt").
Buoy Framework as Public Domain software.
Code2000 Unicode font licensed under GPL2.
JAI, licensed under Java Advanced Imaging Distribution License ("jdl-jai.pdf").
JAMA as Public Domain software.
Jdom, licensed under Apache-style open source jdom license, with the 
    acknowledgment clause removed ("jdom_license.txt").
JMath and its successor projects JMathTools (IO,Plot,Array) are distributed 
    under BSD license, to be found in "jmath_license.txt".
Javascript library RHINO licensed under GPL2 (gpl2.txt).
JTS Topology Suite licensed under LGPL2 (lgpl-2.1.txt).
Junit licensed under Common Public License - v 1.0 (CPL1) ("cpl-1.0.txt").
Jython distributed under Jython license ("jython_license.txt").
Log4J distributed under Apache License Version 2.0 (apache_license.txt).
Postgresql JDBC driver under the Postgresql BSD license 
    ("postgresql-BSD_license.txt").
Xerces2 Java Parser under Apache License Version 2.0 (apache_license).


2. Installation instructions
----------------------------
OpenJUMP comes in the form of a compressed zip archive file.
To install, decompress the archive in your hard drive, for example into 
c:/OpenJUMP
You will end up with the following folder structure:
c:/OpenJUMP/
c:/OpenJUMP/bin
c:/OpenJUMP/lib
c:/OpenJUMP/licenses


3. Running OpenJUMP
-------------------
Run the startup scripts contained in the /bin folder:
- For windows, double-click on "oj_windows.bat" or "OpenJUMP.exe"
- For Linux/Unix, launch oj_linux.sh
- For Mac, launch oj_mac.command

Further notes can be found on our wiki:
http://sourceforge.net/apps/mediawiki/jump-pilot/
and on 
http://www.openjump.org


Startup options
-----------------
Several startup options are available, either for the Java Virtual Machine, 
or for the OpenJUMP core. To change them, edit the startup script accordingly.
The scripts contain documentation comments, don't be afraid.

Note, that Windows users that like to start OpenJUMP with the OpenJUMP.exe 
launcher will need to modify OpenJUMP.ini.  

Java VM options (a complete list can be found in the Java VM documentation)
-Xms defines the allocated memory for the virtual machine at startup.
Example: -Xms256M will allocate 256M of memory for OpenJUMP
-Xmx defines the maximum allocated memory for the virtual machine.
Example: -Xmx256M
-Dproperty=value set a system property. 
At the moment, the following properties are used:
  -Dswing.defaultlaf for enforcing a specific Look and Feel. 
    Several possibilities:
    Metal L&F
     -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel
    Windows L&F
     -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel
  -Dlog4j.configuration for defining the configuration file for Log4j. 
    e.g. by default:
     Dlog4j.configuration=file:./log4j.xml
     
OpenJUMP command line options
-default-plugins <file.xml>
  Specifies the configuration file of a standard set of functions realized
  as plugins. For example almost all functions of the "Tools" menu.
  This is configured as
    -default-plugins bin\default-plugins.xml 
-properties <file.xml> 
  specifies the file where OpenJUMP persistent properties are stored.
  Default setting is 
    -properties bin\workbench-properties.xml
-plug-in-directory <path> 
  Sets the location of the plugin directory. 
  Default setting is
    -plug-in-directory lib/ext
-i18n <locale>
  Overrides the operating systems default locale setting (language, 
  number format etc.) For example:
  - For starting OpenJUMP in French: use -i18n fr
  - languages available (09/2011): 
    cz (czech)
    de (german)
    en (english)
    es (spanish)
    fi (finnish)
    fr (french)
    hu (hungarian)
    it (italian)
    ja_JP (japanese)
    ml (malayalam)
    pt (portuguese)
    pt_BR (brazilian portuguese)
    zh_CN (chinese simplified)
    zh_HK (chinese Hong Kong)
  ATTENTION: If the specified language is not available then
             the locale used is english (en).

  
4. Support
----------
General questions regarding OpenJUMP can be found in:
- www.openjump.org the OpenJUMP home
- jump-pilot.sourceforge.net the OpenJUMP developper site

For commerical support, e.g. payed plugin development, see our
www.openjump.org home.


5. OpenJUMP history
-------------------
OpenJUMP is a "fork" of the JUMP "Java Unified Mapping Platform" software,
developed by Vividsolutions and released in 2003.
During 2004, some enthusiastic developers joined together to enhance further 
the features of JUMP. They launched an independent development branch called 
OpenJUMP. This name gives credit to the original JUMP development, and at the 
same time describes the objectives of this project to be fully open to anyone
wanting to contribute.
Since May 2005 a complete development source is available at:
www.sourceforge.net/projects/jump-pilot


6. Credits
----------
Many thanks to all the contributors of OpenJUMP for their time and efforts:

Original development team of JUMP was:
  at Vividsolutions (www.vividsolutions.com)
    Martin Davis
    Jon Aquino
    Alan Chang 
  at Refractions Research Inc (www.refractions.net) 
    David Blasby 
    Paul Ramsey 

OpenJUMP regular contributors are (non exhaustive list!):
  Andreas Schmitz (lat-lon.de)
  Edgar Soldin (edso)
  Geoffrey G Roy
  Giuseppe Aruta
  Jukka Rahkonen
  Larry Becker (ISA.com)
  Larry Reeder
  Matthias Scholz (refractions.net)
  Michaël Michaud
  Mohammed Rashad
  Stefan Steiniger
  Uwe Dallüge

Past contributors:
  Alberto de Luca (geomaticaeambiente.it)
  Axel Orth
  Basile Chandesris
  Bing Ran
  Eric Lemesre
  Erwan Bocher
  Ezequias Rodrigues da Rocha
  Fco Lavin
  Hisaji Ono
  Jaakko Ruutiainen
  Jan Ruzicka
  Joe Desbonet
  John Clark
  Jonathan Aquino
  Kevin Neufeld
  Landon Blake (Sunburned Surveyor)
  Martin Davis (refractions.net)
  Ole Rahn
  Paolo Rizzi
  Paul Austin
  Pedro Doria Meunier
  Sascha Teichmann (intevation.de)
  Stephan Holl
  Steve Tanner
  Ugo Taddei 

Translation contributors are:
- English: Landon Blake
- Finnish: Jukka Rahkonnen
- French: Basile Chandesris, Erwan Bocher, Steve Tanner, Michaël Michaud
- German: Florian Rengers, Stefan Steiniger, Edgar Soldin
- Italian: Giuseppe Aruta
- Japanese: Hisaji Ono
- Malayalam : Mohammed Rashad
- Portuguese (brazilian):
    Ezequias Rodrigues da Rocha, 
    Cristiano das Neves Almeida
- Spanish: Giuseppe Aruta, Steve Tanner, Fco Lavin, 
           Nacho Uve, Agustin Diez-Castillo
- Hungarian: Zoltan Siki
- Czech: Jan Ruzicka
- Chinese: Elton Chan

Contributing projects and companies:
- Intevation GmbH
  Nightly Build process, collaborative PlugIn development (Print Layout PlugIn)
  contact: Jan Oliver Wagner/Stephan Holl
- Larry Becker and Robert Littlefield (SkyJUMP team)
  partly at Integrated Systems Analysts, Inc.
  for providing their Jump ISA tools code and numerous other improvements
- Lat/Lon GmbH (deeJUMP team)
  for providing some plugins and functionality (i.e. WFS and WMS Plugins)
  contact: Markus Müller/Andreas Schmitz
- Pirol Project from University of Applied Sciences Osnabrück
  for providing the attribute editor. Note that the project is finished now.
  (contact: Arnd Kielhorn)
- VividSolutions Inc. & Refractions Inc.
  for support and answering the never ending stream of questions, especially:
  Martin Davis (now at Refractions Inc.)
  David Zwiers

others:
- L. Paul Chew for providing the Delaunay triangulation algorithm to 
  create Voronoi diagrams
