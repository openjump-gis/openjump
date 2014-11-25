Concave hull based on JTS

Download

The distribution of the concave hull can be downloaded here:
  http://sourceforge.net/projects/jump-pilot/files/OpenJUMP_plugins/More%20Plugins/ConcaveHull/

The sources can be found here
  http://sourceforge.net/p/jump-pilot/code/HEAD/tree/plug-ins/ConcaveHullPlugin/trunk/


Installation

Use OJ PLUS, where it is included.
 or
Place it in <OJ_FOLDER>/lib/ext/ .


How to use it?

Tools menu -> Generate -> Concave hull


Licenses

The concave hull implementation is released under a LGPL licence and the 
OpenJUMP plugin implementation under GPL, according to JTS and OpenJUMP 
licences.


Changes

0.2 (25.11.2014)
- added translations
- minor changes to prepare inclusion in OJ PLUS

0.1
- initial release


Concave hull

This concave hull implementation is based on the algorithm developed by 
Duckham et al. (2008) in the paper "Efficient generation of simple polygons 
for characterizing the shape of a set of points in the plane", available here.

This implementation is based on the JTS Delaunay triangulation, so on the 
subjacent QuadEdge model. Nevertheless, using directly the QuadEdge model was 
too complex to obtain optimal performances and a O(n log(n)) algorithm 
complexity (tests have been done and computational times were quite important 
-- between 1min30 and 2min to compute the concave hull of a set of 50.000 
points). Therefore, the first part of the implementation consists in converting
the QuadEdge model into another triangulation model in which relationships 
between vertices, edges and triangles are clearly defined and easily reachable 
using accessors.

This implementation enables to consider all types of geometry, i.e. Point, 
LineString and Polygon, decomposing LineString and Polygon into Point. 
It is a priori robust (many tests have been done) and a priori efficient -- 
around or less than 3 seconds to compute "the" concave hull of 50.000 points 
--. Nevertheless, improvements could still be done (cf. "Some thoughts about 
concave hull computation" below).

To compute "the" concave hull (a concave hull is not uniquely defined) of a 
geometry or geometry collection, users have to define a threshold. 
This threshold is used to remove all the edges which are longer than this 
threshold during the concave hull creation, except if the edge is part of an 
irregular triangle (cf. Duckham et al.'s paper for more information).


Some thoughts about concave hull computation

Which definition of the threshold? Duckham et al. proposed several solution in 
their paper to consider an adapted threshold to a set of points.

This implementation could be improved, e.g.:

- when two sets of points are separated by a distance greater to the treshold,
  a "bridge" is still done between these two sets. It would be better to create   a mulitpolygon or several polygons,
- the constrained Delaunay triangulation could be used when the geometries in 
  input are Polygon,
- the merge of the segments of the concave hull which is done by the JTS line 
  merger but it could be done using the vertex ID,
etc. 

Michael Michaud, OpenJUMP developer, tested this implementation (many thanks!) 
and realised that results were a little bit unexpected when the algorithm is 
used with layers which contain other geometry than Point. Therefore Michael 
suggests to realise a preprocessing to densify lines with a parameter slightly 
smaller than the threshold. With such a preprocessing, the concave hull becomes a better representation of the features.


For more information

Contact
  Eric Grosso <eric.grosso.os (at) gmail.com>

Original Website
  http://www.rotefabrik.free.fr/concave_hull/

