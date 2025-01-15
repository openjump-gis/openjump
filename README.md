# OpenJUMP ![OpenJUMP logo](https://raw.githubusercontent.com/openjump-gis/openjump/refs/heads/main/icon//pngs/oj_48.png)

OpenJUMP (OJ) - the cross-platform open source Desktop-GIS software provides 
a swiss-knife of tools to analyze and edit huge GIS-datasets.

Taking the _leap_ from _sourceforge_ to _github_, from _svn_ to _git_, 
us few volunteers of the _jump pilot project_ hope that maybe **you might 
join our struggle** to keep OpenJUMP not only alive but contribute to it's 
improvement and adaptation of new and exciting standards.

**NOTE:** The [OJ github repository](https://github.com/openjump-gis/openjump) 
and several of the extension repositories are mirrored at https://gitlab.com/openjump-gis/ . 

## Introduction

OpenJUMP was born from JUMP, an open source GIS originally developed in Java 
by [Vividsolutions](https://www.vividsolutions.com/open-source) and funded by 
the Ministry of Natural Resources of British Columbia (Canada).

In 2007, when the state subsidies stopped, a handful of developers from different 
countries decided to internationalize the software's user interface and uploaded 
the sources to the Sourceforge platform to give it a future.

In 2017, JTS, the geometric engine that made JUMP successful continues to evolve 
thanks to its author Martin Davis, also one of the original authors of JUMP, 
but the project is now developed under the umbrella of 
[Apache Foundation](https://projects.eclipse.org/projects/locationtech) and its
whole namespace changed, breaking the compatibility between JTS and OpenJUMP.

In 2021, OpenJUMP also moves to Github and in order to re-synchronize it with JTS 
a major refactoring is undertaken.

The migration of OpenJUMP and all its extensions may take some time and this page 
will be updated regularly until the first OpenJUMP2 official release.


## Development

OJ development utilizes Maven build automation and can be done with established 
Java IDEs like **Eclipse** or **IntelliJ IDEA** or others supporting mavenized 
java projects. For a quick introduction on how to set up **OJ Core** or the 
**HelloWorld Extension** have a look at this wiki article

https://ojwiki.soldin.de/index.php?title=Eclipse:_Set_up_project_and_example_extension_from_git_sources


## Documentation

To learn more on OpenJUMP you can check the official 
[website](http://www.openjump.org/) or read the 
[wiki documentation](http://ojwiki.soldin.de)


## Downloads

The latest and archived releases for Windows, Linux, macOS can be downloaded from [OpenJUMP project's sf.net files area](https://sourceforge.net/projects/jump-pilot/files/OpenJUMP/).

Current development snapshots are available too at
https://sourceforge.net/projects/jump-pilot/files/OpenJUMP2_snapshots/ .


## Extensions

OJ makes use of **Extensions** and **Drivers** based on a fairly simple **PlugIn API**.

For an example you may have a look at the [**HelloWorldExtension**](https://github.com/openjump-gis/helloworld-extension).

Migration of extensions from OJ 1.x happened as documented below.
advantage was taken of the opportunity to mavenize and modernize
the migrated code.

**Extensions** hosted on openjump-gis group will be named 
xxx-extension (ex. skydriver-extension). An extension
is a plugin or a set of plugins that OpenJUMP can 
discover automatically while loading the jar file.

**Drivers** are special extensions which will be named 
xxx-driver (ex. dxf-driver).

### Extension migration status

#### Column description
- GITHUB : migration from sourceforge to Github
- JTS : refactoring from com.vividsolutions.jts to org.locationtech.jts
- I18N : refactoring to use new I18N and new FeatureInstaller methods
- POM : refactoring to automatize extension distribution
- Status : status of extensions when they are not part of OpenJUMP PLUS

#### Non-driver extensions and libraries supporting one or several extensions

| Extension              |GITHUB| JTS | I18N | POM |                           |
| ---------------------- |:----:|:---:|:----:|:---:| ------------------------- |
| BshEditor              |   x  |  x  |  x   |  x  |                           |
| CadTools               |   x  |  x  |  x   |  x  |                           |
| CadPlan Jump-Chart     |   x  |  x  |  x   |  x  |                           |
| CadPlan Fill-Pattern   |   x  |  x  |  x   |  x  |                           |
| CadPlan Printer        |   x  |  x  |  x   |  x  |                           |
| CadPlan Symbols        |   x  |  x  |  x   |  x  |                           |
| CadPlan Update Project |      |     |      |     | Abandoned                 |
| Color Chooser          |   x  |  x  |  x   |  x  |                           |
| ConcaveHull            |   x  |  x  |  x   |  x  |                           |
| CTS                    |   x  |  x  |  x   |     | Included in CORE          |
| de.soldin.jump         |      |     |      |     | To be done                |
| Extension Manager      |      |     |      |     | Abandoned                 |
| Five Colors            |   x  |  x  |  x   |  x  |                           |
| Graph Toolbox          |   x  |  x  |  x   |  x  |                           |
| Jump-JgraphT (lib)     |   x  |  x  |  x   |  x  | Support for Graph Toolbox |
| Horae                  |      |     |      |     | To be done                |
| Lansdscape Analysis    |      |     |      |     | To be done                |
| MapGenToolbox          |   x  |  x  |  x   |  x  | Not included in PLUS      |
| Matching               |   x  |  x  |  x   |  x  |                           |
| Measure Toolbox        |   x  |  x  |  x   |  x  |                           |
| OjWorldWind            |      |     |      |     | Unmaintained              |
| OpenKLEM               |   x  |  x  |  x   |  x  |                           |
| PgRouting              |      |     |      |     | Unmaintained              |
| PrintLayout            |      |     |      |     | Unmaintained (?)          |
| RoadMatcher            |      |     |      |     | Unmaintained (hard work)  |
| SetAttributes          |   x  |  x  |  x   |  x  |                           |
| Sextante               |   x  |  x  |  x   |  x  |                           |
| SISJUMP                |      |     |      |     | Unmaintained              |
| SkyPrinter             |   x  |  x  |  x   |  x  | Included in CORE          |
| Text-utils (lib)       |   x  |  x  |  x   |     | Support for Matching ext. |
| Topology               |   x  |  x  |  x   |  x  |                           |
| ViewManager            |   x  |  x  |  x   |  x  |                           |

#### Driver extensions

| Extension              |GITHUB| JTS | I18N | POM |                               |
| ---------------------- |:----:|:---:|:----:|:---:| ----------------------------- |
| CSV driver             |   x  |  x  |  x   |  x  |                               |
| DXF driver             |   x  |  x  |  x   |  x  |                               |
| KML driver             |   x  |  x  |  x   |  x  |                               |
| OsmFileReader          |      |     |      |     | Unmaintained                  |
| PostGIS                |      |     |      |     | Deprecated (included in CORE) |
| RasterLayer Export     |      |     |      |     | Unmaintained                  |
| SpatialDatabases       |      |     |      |     | Unmaintained                  |
| Spatialite driver      |   x  |     |      |     | Unmaintained                  |

#### Other known extension/forks non hosted on Github openjump-gis repository 

| Extension                                      | HOST        | REPOSITORY      | URL | STATUS |
|------------------------------------------------|-------------|-----------------| --- | ------ |
| RoadMatcher                                    | GITHUB      | ssinger         | https://github.com/ssinger/roadmatcher | 2009: Not ready for OJ2 |
| LogisticTools                                  | GITHUB      | fduque          | https://github.com/fduque/LogisticTools-Plugin-OpenJUMP | 2019 : Not ready for OJ2 |
| Morphometric analysis                          | GITHUB      | burakbeyhan     | https://github.com/burakbeyhan/morphometric-analysis | 2021 : Not ready for OJ2 |
| Maximum Inscribed Circle                       | GITHUB      | burakbeyhan     | https://github.com/burakbeyhan/maximum-inscribed-circle | 2020 : Not ready for OJ2 |
| Delineation of functional and planning regions | GITHUB      | burakbeyhan     | https://github.com/burakbeyhan/delineation-of-functional-and-planning-regions | 2020 : Not ready for OJ2 |
| GeOxygen                                       | Sourceforge | oxygene-project | https://sourceforge.net/projects/oxygene-project/files/ | 2014 : Not Ready for OJ2|
| SMT:SAR Management Toolkit                     | Sourceforge | sarmanager      | https://sourceforge.net/projects/sarmanager/ | 2016 : not ready for OJ2|
| Geo Arbores Raster Tools                       | Sourceforge | GiuseppeAruta   | https://sourceforge.net/p/opensit/wiki/Geo%20Arbores%20Raster%20Tools/ | 2022 : ported to OJ2 not integrated|

#### Other known drivers/forks non hosted on Github openjump-gis repository 

| Extension          | HOST | REPOSITORY  | URL | STATUS |
| ------------------ | ---- | ----------- | --- | ------ |
| OJ+ocient driver   |GITHUB|Xeograph     | https://github.com/Xeograph/openjump | 2021 : OpenJUMP 2 compatible |
| OSMM-GML Mod       |GITHUB|szhou68      | https://github.com/szhou68/OpenJump_OSMM_GML_Mod | 2018 : Not ready for OJ2 |
| Drillgis driver    |GITHUB|k255         | https://github.com/k255/openjump-drillgis-plugin | 2015 : Not ready for OJ2 |
| MonetDB driver     |GITHUB|DennisPallett| https://github.com/DennisPallett/openjump-monetdb-plugin | 2013 : Not ready for OJ2 |
