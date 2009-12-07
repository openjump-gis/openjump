/*
 * Created on 17.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2434 $
 *  $Date: 2006-09-12 12:31:50 +0200 (Di, 12 Sep 2006) $
 *  $Id: MetaDataMap.java 2434 2006-09-12 10:31:50Z LBST-PF-3\orahn $
 */
package org.openjump.util.metaData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class to store meta information of various kinds in a map.
 * By default an object of this class will be added to the properties of a 
 * DataSource (and is hopefully saved, when e.g. the task is saved). 
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2434 $
 * 
 */
public class MetaDataMap {
    
    protected HashMap<Object,Object> metaData = new HashMap<Object,Object>(); 
    
    /**
     * constructor (needs to be parameterless in order for java2xml to be able to load it)
     */
    public MetaDataMap() {
        super();
    }
    
    /**
     * Adds a new meta information to the map
     *@param key the kind of information
     *@param value the information itself
     */
    public void addMetaInformation(Object key, Object value){
        this.metaData.put(key, value);
    }


    /**
     * Gets all meta information in one map object
     *@return all stored meta information
     */
    public HashMap getMetaData() {
        return metaData;
    }
    /**
     * Sets (overwrites) the stored meta information
     *@param metaData
     */
    public void setMetaData(HashMap<Object,Object> metaData) {
        this.metaData = metaData;
    }

    public void clear() {
        metaData.clear();
    }

    public boolean containsKey(Object key) {
        return metaData.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return metaData.containsValue(value);
    }

    public Set keySet() {
        return metaData.keySet();
    }

    public Object remove(Object key) {
        return metaData.remove(key);
    }

    public Object get(String key) {
        return metaData.get(key);
    }

    public void putAll(Map<Object,Object> arg0) {
        metaData.putAll(arg0);
    }
    
}
