package com.vividsolutions.jump.datastore.spatialite;

/**
 * DB types for geometric columns
 * Used by spatialite, for other datastore, default to native type.
 * @author nicolas
 */
public enum GeometricColumnType {
    WKT, WKB, SPATIALITE, NATIVE
    // All OGC types can be set as column type
}
