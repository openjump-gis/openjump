package com.vividsolutions.jump.datastore.spatialite;

/**
 * An Enum representing Spatialite Geometry_column layout:
 * either a FDO layout, an OGR OGC layout, an OGR Spatialite layout, an GeoPackage layout
 * or no layout (no *_geometry_columns MD table found)
 * @author nicolas Ribot
 */
public enum GeometryColumnsLayout {
    FDO_LAYOUT, OGC_OGR_LAYOUT,OGC_SPATIALITE_LAYOUT, OGC_GEOPACKAGE_LAYOUT, NO_LAYOUT;
}
