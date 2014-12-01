/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.io.datasource;

/**
 * A wrapper for a query string that attributes it with the DataSource
 * to apply it against.
 */
public class DataSourceQuery {
    
    private String name;
    
	/**
	 * Constructs a DataSourceQuery that wraps a query string
	 * (implementation-dependent) and a DataSource to apply it against.
	 * 
	 * @param query
	 *                  identifies the dataset; may take the form of a SQL statement,
	 *                  a table name, null (if there is only one dataset), or other
	 *                  format
	 * @param name
	 *                  will be used for the layer name
	 */
    public DataSourceQuery(DataSource dataSource, String query, String name) {
        this.dataSource = dataSource;
        this.query = query;
        this.name = name;
    }
    
    /**
     * Parameterless constructor called by Java2XML
     */
    public DataSourceQuery() {        
    }

    private DataSource dataSource;
    private String query;

    

	/**
	 * Returns the DataSource against which to apply the
	 * (implementation-dependent) query string.
	 */
    public DataSource getDataSource() {
        return dataSource;
    }
    
	/**
	 * Returns the implementation-dependent query string wrapped by this
	 * DataSourceQuery
	 */
    public String getQuery() {
        return query;
    }

    /**
     * Returns the name of this DataSourceQuery, suitable for use as a layer name.
     */
    public String toString() {
        return name;
    }

    /**
     * Called by Java2XML
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Called by Java2XML
     */    
    public void setQuery(String query) {
        this.query = query;
    }

}
