OpenJUMP readme file
--------------------
Version 1.4

October 24th, 2010

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
can be found in the "gpl2_license.txt" file in the "licenses" folder.
OpenJUMP uses the BATIK libraries to write svg format. The BATIK libraries are
used under the terms of the APACHE license, which can be found in the "apache_license.txt" 
file or on www.apache.org. We use also a math library called JMath. JMath and its 
successor projects JMathTools (IO,Plot,Array) are distributed under BSD license, 
to be found in "jmathlicense.txt". Jython is distributed under Jython license that 
can be found in the file: jython_license.txt


2. Installation instructions
----------------------------
The platform-independent version of OpenJUMP comes under the form of a compressed archive.
To install, decompress the archive in your hard drive, for example into c:/OpenJUMP
You will see the following folder structure:
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

Further notes can be found on our wiki: http://sourceforge.net/apps/mediawiki/jump-pilot/index.php?title=Main_Page
and on www.openjump.org

Users of the "looks" extension should place all the jar files from looks-extension 
directly into /lib/ext.


Startup options
-----------------
Several startup options are available, either for the Java Virtual Machine, or for the
OpenJUMP core. To change them, edit the startup script
accordingly, editing the line beginning by "start javaw" or look for similar entries.

Note, that Windows users that like to start OpenJUMP with the OpenJUMP.exe file will need to modify OpenJUMP.ini, or alternatively oj_windows.bat  

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
     
OpenJUMP command line options
-default plugins: specifies the name of the file that loads adavanced OpenJUMP functions.
 I.e. almost all functions of the "Tools" menu.
 Default is "-default-plugins bin\default-plugins.xml"
-properties filename : specifies the name of the file where OpenJUMP properties are stored.
 Default is "-properties bin\workbench-properties.xml"
-plug-in-directory path : defines the location of the plugin directory. 
 Default is %LIB%/ext where %LIB% is defined earlier in the startup script.
-i18n locale : defines the locale (language, etc.) used by OpenJUMP. For example:
  - For starting OpenJUMP in French: use -i18n fr
  - Other languages available: de (german), es (spanish), pt_BR (brazilian portuguese)
  - Default is english if the specified language is not implemented.

  
4. Support
----------
General questions regarding OpenJUMP can be found in:
- www.openjump.org the OpenJUMP home
- jump-pilot.sourceforge.net the OpenJUMP developper site

For commerical support, e.g. payed plugin development, see our www.openjump.org home.


5. OpenJUMP history
-------------------
OpenJUMP is a "fork" of the JUMP "Java Unified Mapping Platform" software, developed
by Vividsolutions and released in 2003.
During 2004, some enthusiastic developers joined together to enhance further the 
features of JUMP. They launched an independent development branch called OpenJUMP.
This name gives credit to the original JUMP development, and at the same time 
describes the objectives of this project to be fully open to anyone wanting
to contribute.
Since May 2005 a complete development source is available at:
www.sourceforge.net/projects/jump-pilot


6. Credits
----------
Many thanks to all the contributors of OpenJUMP for their time and efforts:

Original development team of JUMP was:
- Martin Davis, Jon Aquino, Alan Chang from Vividsolutions (www.vividsolutions.com)
- David Blasby and Paul Ramsey from Refractions Research Inc (www.refractions.net) 

OpenJUMP regular contributors are (non exhaustive list!):
- Alberto de Luca (geomaticaeambiente.it),
- Andreas Schmitz (lat-lon.de),
- Bing Ran,
- Edgar Soldin,
- Giuseppe Aruta, 
- Hisaji Ono,
- Jukka Rahkonen,
- Kevin Neufeld,
- Landon Blake (Sunburned Surveyor),
- Larry Becker (ISA.com),
- Martin Davis (refractions.net),
- Matthias Scholz (refractions.net),
- Michaël Michaud,
- Paolo Rizzi,
- Stefan Steiniger,
- Sascha Teichmann (intevation.de)*
- Uwe Dallüge,

* past contributors:
- Axel Orth*,
- Basile Chandesris*,
- Eric Lemesre*,
- Erwan Bocher*,
- Ezequias Rodrigues da Rocha*,
- Fco Lavin*,
- Jaakko Ruutiainen*,
- Jan Ruzicka*,
- Joe Desbonet*,
- John Clark*,
- Jonathan Aquino*,
- Ole Rahn*,
- Paul Austin*,
- Pedro Doria Meunier*,
- Stephan Holl*
- Steve Tanner*,
- Ugo Taddei* 

Projects and Companies
- Larry Becker and Robert Littlefield (SkyJUMP team)
  partly at Integrated Systems Analysts, Inc.
  for providing their Jump ISA tools code and numerous other improvements
- Pirol Project from University of Applied Sciences Osnabrück
  for providing the attribute editor. Note that the project is finished now.
  (contact: Arnd Kielhorn)
- Lat/Lon GmbH (deeJUMP team)
  for providing some plugins and functionality (i.e. WFS and WMS Plugins)
  contact: Markus Müller/Andreas Schmitz
- VividSolutions Inc. & Refractions Inc.
  for support and answering the never ending stream of questions, especially:
  Martin Davis (now at Refractions Inc.)
  David Zwiers
- Intevation GmbH
  Nighlty Build process, collaborative PlugIn development (Print Layout PlugIn)
  contact: Jan Oliver Wagner/Stephan Holl

Translation contributors are:
- English: Landon Blake
- Finnish: Jukka Rahkonnen
- French: Basile Chandesris, Erwan Bocher, Steve Tanner, Michaël Michaud
- German: Florian Rengers, Stefan Steiniger
- Italian: Giuseppe Aruta
- Japanese: Hisaji Ono
- Portuguese (brazilian): Ezequias Rodrigues da Rocha, Cristiano das Neves Almeida
- Spanish: Giuseppe Aruta, Steve Tanner, Fco Lavin, Nacho Uve
- Hungarian: Zoltan Siki
- Czech: Jan Ruzicka
- Chinese: Elton Chan

others:
- L. Paul Chew for providing the Delauney triangulation algorithm to create Voronoi diagrams

