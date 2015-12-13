package com.vividsolutions.jump.datastore.spatialite;

/**
 * An Enum representing Spatialite Geometry_column layout:
 * either a FDO layout, an OGR OGC layout, and OGR Spatialite layout, or no layout (no geometry_column found)
 * @author nicolas
 */
public enum GeometryColumnsLayout {
    FDO_LAYOUT, OGC_OGR_LAYOUT,OGC_SPATIALITE_LAYOUT, NO_LAYOUT;
}
