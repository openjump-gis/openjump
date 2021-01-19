package com.vividsolutions.jump.geom;

import org.locationtech.jts.geom.Geometry;


/**
 * An interface to process geometries.
 */
public interface GeometryProcessor {
    Geometry process(Geometry geometry);
}
