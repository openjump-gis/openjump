package com.vividsolutions.jump.datastore;

/**
 * Metadata about a Geometry Column
 */ 
public class GeometryColumn {
    
    private String name;
    private int srid = 0;
    private String type = "Geometry";
    private boolean indexed = false;
    private int coordDimension = 2;
    
    public GeometryColumn(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public GeometryColumn(String name, int srid) {
        this(name);
        this.srid = srid;
    }
    
    public int getSRID() {
        return srid;
    }
    
    public GeometryColumn(String name, int srid, String type) {
        this(name, srid);
        this.type = type;
    }

    public GeometryColumn(String name, int coordDimension, int srid, String type) {
        this(name, srid);
        this.coordDimension = coordDimension;
        this.type = type;
    }
    
    /**
     * Ctor with boolean parameter telling if column is indexed
     * @param name the name of the geometry column
     * @param srid the SRID of the geometry column
     * @param type the geometric native type of the geometry column (GEOMETRY, SDO_GEOMETRY...)
     * @param indexed true if the geometry column is indexed
     */
    public GeometryColumn(String name, int srid, String type, boolean indexed) {
        this(name, srid, type);
        this.indexed = indexed;
    }
    
    public String getType() {
        return type;
    } 
    
    /**
     * Sets the type of this GeometryColumn
     * @param type 
     */
    public void setType(String type) {
        this.type = type;
    }

    public int getCoordDimension() {
        return coordDimension;
    }

    public void setCoordDimension(int coordDimension) {
        this.coordDimension = coordDimension;
    }

    public String toString() {
        return name + " (" + type + ", srid=" + srid + ")";
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }
}
