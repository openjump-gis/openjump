/*
 * Created on 13.07.2005 for PIROL
 *
 * SVN header information:
 *  $Author: javamap $
 *  $Rev: 856 $
 *  $Date: 2007-06-18 22:15:27 -0600 (Mo, 18 Jun 2007) $
 *  $Id: Attribute2UnitMap.java 856 2007-06-19 04:15:27Z javamap $
 */
package org.openjump.util.metaData;

import java.util.Collection;
import java.util.HashMap;

/**
 * Meta information object to store the units for the attributes in a layer.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 856 $
 * 
 */
public class Attribute2UnitMap {

    protected HashMap<String, String> attribute2unit = new HashMap<String, String>();
    protected boolean useHTMLLineBreaks = false;
    /**
     * key to be used in the meta information map
     */
    protected static final String KEY_ATTRIBUTE2UNIT = "attribute2unit";
    
    public void clear() {
        attribute2unit.clear();
    }
    public boolean containsAttribute(String attributeName) {
        return attribute2unit.containsKey(attributeName);
    }
    public boolean containsUnit(String unitString) {
        return attribute2unit.containsValue(unitString);
    }
    public String getUnitString(String attributeName) {
        return attribute2unit.get(attributeName);
    }
    public String put(String attributeName, String unitString) {
        return attribute2unit.put(attributeName, unitString);
    }
    public String removeAttribute(String attributeName) {
        return attribute2unit.remove(attributeName);
    }
    public Collection values() {
        return attribute2unit.values();
    }
    public String toString() {
        
        Object[] keys = this.attribute2unit.keySet().toArray();
        String result = this.getClass().getName() + (useHTMLLineBreaks?":<br>\n":":\n");
        
        for (int i=0; i<this.attribute2unit.size(); i++){
            result += keys[i].toString() + " - "+ this.attribute2unit.get(keys[i]).toString() + (useHTMLLineBreaks?"<br>\n":"\n");
        }
        
        return result;
    }
    /**
     * tells you if @link{Attribute2UnitMap#toString()} uses &lt;br&gt; or just backslash+n to begin a new line.
     *@return value of useHTMLLineBreaks
     */
    public boolean isUseHTMLLineBreaks() {
        return useHTMLLineBreaks;
    }
    /**
     * Controlls if @link{Attribute2UnitMap#toString()} uses &lt;br&gt; or just backslash+n to begin a new line.
     */
    public void setUseHTMLLineBreaks(boolean useHTMLLineBreaks) {
        this.useHTMLLineBreaks = useHTMLLineBreaks;
    }
    
    /**
     * for java2xml
     *@return attribute2unit map
     */
    public HashMap getAttribute2unit() {
        return attribute2unit;
    }
    /**
     * for java2xml
     *@param attribute2unit map
     */
    public void setAttribute2unit(HashMap<String,String> attribute2unit) {
        this.attribute2unit = attribute2unit;
    }

}
