OpenJUMP, the Open Source GIS which has more than one trick in its kangaroo pocket, 
"jumps" from sourceforge to github to help you as never.

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

## Documentation

To learn more on OpenJUMP you can check the official 
[website](http://www.openjump.org/) or read the 
[wiki documentation](http://ojwiki.soldin.de/index.php?title=Main_Page) 

## Downloads

The last official OpenJUMP release is OpenJUMP 1.16.
You can download it from [Sourceforge - OpenJUMP](https://sourceforge.net/projects/jump-pilot/files/OpenJUMP/)

OpenJUMP 2 snapshots can be downloaded from
https://sourceforge.net/projects/jump-pilot/files/OpenJUMP2_snapshots/


## Migration to OpenJUMP 2

OpenJUMP 2 - CORE is already compilable with maven

If you have cloned the project, and you have maven installed,
you can package the core distribution with

`mvn package -P core,portable,snapshot
`

## Extensions

Migration of all extensions may take some time as we take 
advantage of the opportunity to mavenize and modernize
the migrated code.

**Extensions** hosted on openjump-gis group will be named 
xxx-extension (ex. skydriver-extension). An extension
is a plugin or a set of plugins that OpenJUMP can 
discover automatically while loading the jar file.

**Drivers** are special etensions which will be named 
xxx-driver (ex. dxf-driver).

### Extension migration status
- [x] Beanshell Editor
- [x] CADTools ****Extension
- [x] CadPlan JumpChart
- [x] CadPlan JumpFillPattern
- [x] CadPlan JumpPrinter
- [ ] CadPlan UpdateProject
- [x] CadPlan VertexSymbols
- [x] Color_chooser
- [x] ConcaveHullPlugin
- [x] CsvDriver
- [x] ~~CTSPlugin~~ Now included in OpenJUMP CORE
- [ ] de.soldin.jump
- [x] DxfDriver
- [ ] ExtensionManagerPlugin
- [x] GraphToolboxPlugin
- [ ] Horae
- [x] JumpJGraphT
- [x] KmlDriver
- [ ] LandscapeAnalysis
- [x] MapGenToolboxPlugin (not included in PLUS)
- [x] MatchingPlugIn
- [x] Measure Toolbox 
- [ ] ojmapcoloring
- [ ] OjWorldWind
- [x] OpenKLEM
- [ ] OsmFileReaderPlugin
- [ ] PgRoutingPlugin
- [ ] PostGISPlugin
- [ ] PrintLayoutPlugin
- [ ] RasterLayerExport
- [ ] RoadMatcher
- [x] SetAttributesPlugin
- [x] SextantePlugIn
- [ ] SISJUMP
- [x] SkyPrinterPlugIn
- [ ] SpatialDatabasesPlugin
- [ ] SpatialitePlugin
- [x] TopologyPlugin
- [x] ViewManagerPlugin



