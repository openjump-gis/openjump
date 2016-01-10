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

import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;


import java.util.HashMap;
import java.util.Map;
/**
 * A file, database, web service, or other source of data. To be savable to a
 * project file, a DataSource must not be an anonymous class (because the class
 * name is recorded in the project file) and it must have a parameterless
 * constructor (so it can be reconstructed by simply being instantiated and
 * having #setProperties called).
 */
public abstract class DataSource {

    private Map properties;

    /**
     * Sets properties required to open a DataSource, such as username, password,
     * filename, coordinate system, etc. Called by DataSourceQueryChoosers.
     */
    public void setProperties(Map properties) {
        this.properties = new HashMap(properties);
    }

    public Map getProperties() {
        //This method needs to be public because it is called by Java2XML [Jon Aquino 11/13/2003]
        
        //I was returning a Collections.unmodifiableMap before, but
        //Java2XML couldn't open it after saving it (can't instantiate
        //java.util.Collections$UnmodifiableMap). [Jon Aquino]
        return properties;
    }

    /**
     * Creates a new Connection to this DataSource.
     */
    public abstract Connection getConnection();

    /**
     * Filename property, used for file-based DataSources
     */
    public static final String FILE_KEY = "File";
    
	/**
	 * Coordinate-system property, used for files and other DataSources that
	 * have a single CoordinateSystem
	 */    
	public static final String COORDINATE_SYSTEM_KEY = "Coordinate System";
    
    public boolean isReadable() { 
        return true;
    }
    
    public boolean isWritable() {
        return true;
    }
    
    public FeatureCollection installCoordinateSystem(FeatureCollection queryResult, 
            										CoordinateSystemRegistry registry) {
        if (queryResult == null) { return queryResult; }
        String coordinateSystemName;
        try {
            coordinateSystemName = (String) getProperties().get(COORDINATE_SYSTEM_KEY);
        } catch (NullPointerException e){
            return queryResult;
        }
        if (coordinateSystemName == null) { return queryResult; }
        CoordinateSystem coordinateSystem = registry.get(coordinateSystemName);
        if (coordinateSystem == null) { return queryResult; }
        queryResult.getFeatureSchema().setCoordinateSystem(coordinateSystem);
        return queryResult;
    }

}
