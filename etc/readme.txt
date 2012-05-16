OpenJUMP ReadMe file
--------------------
Version ${version.number} ${version.release} rev.${version.revision}

${version.buildDate}

Contents
--------
1. Licensing
2. Installation instructions
3. Running OpenJUMP
4. Support
5. OpenJUMP history
6. Credits


1. Licensing
------------
OpenJUMP is distributed under the GPL2 license. A description of this license
can be found in the "gpl-2.txt" (GPL2) file in the "licenses/" folder.

OpenJUMP uses and distributes the following (in alphabetical order) formatted
as ( Library name - License name - License file in "licenses/" folder )
 Apache Tools TAR, BZIP - Apache License Version 2.0 - apache_license-2.0.txt
 BeanShell - LGPL2.1 - lgpl-2.1.txt
 Buoy Framework - Public Domain
 Code2000 Unicode font - GPL2 - gpl-2.txt
 JAI - Java Advanced Imaging Distribution License - jdl-jai.pdf
 JAMA - Public Domain
 Jdom - Apache-style open source jdom license, 
       with the acknowledgment clause removed - jdom_license.txt
 JMath and its successor projects JMathTools (IO,Plot,Array) - BSD license 
     - jmath_license.txt
 Javascript library RHINO - GPL2 - gpl-2.txt
 JTS Topology Suite - LGPL2 - lgpl-2.1.txt
 Junit - Common Public License v1.0 (CPL1) - cpl-1.0.txt
 Jython - Jython license - jython_license.txt
 Log4J - Apache License Version 2.0 - apache_license-2.0.txt
 Postgresql JDBC driver - Postgresql BSD license - postgresql-BSD_license.txt
 Xerces2 Java Parser - Apache License Version 2.0 - apache_license-2.0.txt

Additionally PLUS distributions contain
( Component name - License name - License file in "licenses/" folder
   list of files and dependencies with license if any )
 Batik SVG Toolkit - Apache License Version 2.0 - apache_license-2.0.txt
 Geoarbores Vertex Symbols Collection (Giuseppe Aruta) - GPL2 - gpl-2.txt
  lib/ext/VertexImages/*.wkt
 ECW and JPEG2000 Read Support based on ECW SDK 3.3 for windows x86/x64, 
  linux x86 - ERDAS ECW JPEG2000 SDK license - ecw license.txt
  lib/native/[os/]{jecw-*.jar,NCS*.dll,ermapper.jar}
  
and the following plugins
 Aggregation PlugIn (Michaël Michaud) - GPL2 - gpl-2.txt
  aggregation-*.jar
 BeanShell Editor (Michaël Michaud) - GPL2 - gpl-2.txt
  bsheditor4jump-*.jar, buoy.jar(see above)
 DXF driver (Michaël Michaud) - GPL2 - gpl-2.txt
  driver-dxf-*.jar
 Graph Extension (Michaël Michaud) - GPL2 - gpl-2.txt
  graph-toolbox-*.jar, jump-jgrapht-*.jar, 
  jgrapht-*.jar - LGPL2 - lgpl-2.1.txt
 Jump Chart (com.cadplan.jump) - GPL2 - gpl-2.txt
  JumpChart.jar
 Jump Fill Pattern (com.cadplan.jump) - GPL2 - gpl-2.txt
  JumpFillPattern.jar
 Printer (com.cadplan.jump) - GPL2 - gpl-2.txt
  jumpPrinter.jar
 Pirol Csv Dataset 
  (de.fhOsnabrueck.jump.pirol.datasources.pirolCSV) - GPL2 - gpl-2.txt
  PirolCsv.jar, pbaseClasses.jar
 PostGIS Driver (Refractions Research/HCU Hamburg, RZCN/E. Lemesre)
  - GPL2 - gpl-2.txt
  PostGISPlugIn-*.jar
 Sextante Tools (es.unex.sextante.openjump.extensions) - GPL3 - gpl-3.0.txt
  sextante-binding-*.jar, sextante/*.* 
  TableLayout-*.jar - Clearthought License - clearthought-2.0.txt
  xbean-*.jar - Apache License Version 2.0 - apache_license-2.0.txt
 Topology Extension (Michaël Michaud) - GPL2 - gpl-2.txt
  topology-*.jar
 Text Driver (Michaël Michaud) - GPL2 - gpl-2.txt
  txt-driver-*.jar
 Vertex Note (com.cadplan.jump) - GPL2 - gpl-2.txt
 Vertex Symbols (com.cadplan.jump) - GPL2 - gpl-2.txt
  VertexSymbols.jar
  iText-2.1.5.jar - LGPL2.1 - lgpl-2.1.txt


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
- On windows, double-click on oj_windows.bat or OpenJUMP.exe
- On Linux/Unix, launch oj_linux.sh
- On Mac OSX, launch oj_mac.command or OpenJUMP.app

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
-Dproperty=value set a jvm system property. 


OpenJUMP command line options
-default-plugins <file.xml>
  Specifies the configuration file of a standard set of functions realized
  as plugins. For example almost all functions of the "Tools" menu.
  This is configured as
    -default-plugins bin\default-plugins.xml 
-properties <file.xml>
  specifies the file where OpenJUMP persistent properties are stored.
  See Wiki article "How to use a plugin with a properties file in ECLIPSE".
  Default: JUMP_HOME\bin\workbench-properties.xml
-state <some/folder>
  specifies the folder where OpenJUMP stores data between executions
  (workbench-state.xml).
  Default: JUMP_HOME or SETTINGS_HOME
-plug-in-directory <path> 
  Sets the location of the plugin directory.
  Default: JUMP_HOME/lib/ext
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
    ta_IN (indian tamil)
    te (telugu)
    zh_CN (chinese simplified)
    zh_HK (chinese Hong Kong)
  ATTENTION: If the specified language is not available then
             the language used is english (en).

  
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

OpenJUMP continuous contributors (non exhaustive list in alphabetical order):
  Edgar Soldin (edso, soldin.de)
  Giuseppe Aruta
  Jukka Rahkonen
  Matthias Scholz
  Michaël Michaud
  Stefan Steiniger

Past contributors (in alphabetical order):
  Alberto de Luca (geomaticaeambiente.it)
  Andreas Schmitz (lat-lon.de)
  Axel Orth
  Basile Chandesris
  Bing Ran
  Eric Lemesre
  Erwan Bocher
  Ezequias Rodrigues da Rocha
  Fco Lavin
  Geoffrey G Roy
  Hisaji Ono
  Jaakko Ruutiainen
  Jan Ruzicka
  Joe Desbonet
  John Clark
  Jonathan Aquino
  Kevin Neufeld
  Landon Blake (Sunburned Surveyor)
  Larry Becker (ISA.com)
  Larry Reeder
  Martin Davis (refractions.net)
  Mohammed Rashad
  Ole Rahn
  Paolo Rizzi
  Paul Austin
  Pedro Doria Meunier
  Sascha Teichmann (intevation.de)
  Stephan Holl
  Steve Tanner
  Ugo Taddei
  Uwe Dallüge

Translation contributors are
  Chinese: Elton Chan
  Czech: Jan Ruzicka
  English: Landon Blake
  Finnish: Jukka Rahkonnen
  French: Basile Chandesris, Erwan Bocher, Steve Tanner, Michaël Michaud
  German: Florian Rengers, Stefan Steiniger, Edgar Soldin
  Hungarian: Zoltan Siki
  Italian: Giuseppe Aruta
  Japanese: Hisaji Ono
  Malayalam : Mohammed Rashad
  Portuguese (brazilian):
    Ezequias Rodrigues da Rocha, 
    Cristiano das Neves Almeida
  Spanish:
    Giuseppe Aruta, Steve Tanner, Fco Lavin, 
    Nacho Uve, Agustin Diez-Castillo
  Tamil: Vikram Santhanam
  Telugu: Ravi Vundavalli

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
