/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore;

import com.vividsolutions.jump.I18N;

/**
 * Metadata about a Datastore geo layer: a dataset name with a geo column (one table 
 * with several geo columns generates several DataStoreLayers, one for each geo column
 */ 
public class DataStoreLayer {
    private String fullName;
    private String name;
    private GeometryColumn geoCol;
    private String where;
    private String schema;
    private int limit;
    private Boolean caching;

    public DataStoreLayer(String fName, GeometryColumn geoCol) {
        this.fullName = fName;
        this.geoCol = geoCol;
        this.limit = 0;
        this.caching = true;
        this.where = "";
        this.schema = I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Default");
        
        // find schema from name: schema.name.
        // If no schema found -> Default
        String[] a = this.fullName.split("\\.");
        if (a.length == 1) {
            this.name = this.fullName;
        } else {
            this.schema = a[0];
            this.name = a[1];
        }
    }
    
    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeometryColumn getGeoCol() {
        return geoCol;
    }

    public void setGeoCol(GeometryColumn geoCol) {
        this.geoCol = geoCol;
    }

    public String getWhere() {
        return where;
    }
    
    /**
     * Returns the where clause with WHERE keyword removed
     * @return 
     */
    public String getWhereClause() {
        return where.trim().toLowerCase().startsWith("where")
                ? where.trim().substring("where".length()).trim() : where.trim();
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Boolean isCaching() {
        return caching;
    }

    public void setCaching(Boolean caching) {
        this.caching = caching;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    @Override
    public String toString() {
        return this.getFullName();
    }
    
    
}
