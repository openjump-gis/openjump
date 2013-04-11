package com.vividsolutions.jump.datastore;

/**
 * Metadata about a Geometry Column
 */ 
public class GeometryColumn {
    
    private String name;
    private int srid = 0;
    private String type = "Geometry";
    
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
    
    public String getType() {
        return type;
    } 
    
    public String toString() {
        return name + " (" + type + ", srid=" + srid + ")";
    }
}
