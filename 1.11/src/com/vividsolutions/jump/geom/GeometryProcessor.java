package com.vividsolutions.jump.geom;

import com.vividsolutions.jts.geom.Geometry;


/**
 * An interface to process geometries.
 */
public interface GeometryProcessor {
    Geometry process(Geometry geometry);
}
