package com.vividsolutions.jump.datastore.mariadb;

/**
 * An Enum representing MySQL/MariaDB Geometry_column layout:
 * either an OGR layout, or no layout (no geometry_columns MD table found)
 * @author nicolas Ribot
 */
public enum GeometryColumnsLayout {
    OGC_LAYOUT, NO_LAYOUT;
}
