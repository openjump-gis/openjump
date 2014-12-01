/*
 * Created on 13.07.2005 for PIROL
 *
 * SVN header information:
 *  $Author: michaudm $
 *  $Rev: 1559 $
 *  $Date: 2008-10-05 16:54:14 -0600 (So, 05 Okt 2008) $
 *  $Id: MetaInformationHandler.java 1559 2008-10-05 22:54:14Z michaudm $
 */
package org.openjump.util.metaData;

import java.util.HashMap;
import java.util.Set;

import org.openjump.core.apitools.HandlerToMakeYourLifeEasier;
import org.openjump.core.apitools.objecttyperoles.FeatureCollectionRole;
import org.openjump.core.apitools.objecttyperoles.FeatureCollectionRoleTypes;
import org.openjump.core.apitools.objecttyperoles.PirolFeatureCollection;
import org.openjump.core.apitools.objecttyperoles.RoleStandardFeatureCollection;
import org.openjump.util.metaData.ObjectContainingMetaInformation;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.model.Layer;

import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;

/**
 * Tool class for easier handling of meta information on a layer basis.<br>
 * - objects will be created, if neccessary<br>
 * - you don't need to access the properties map of the data source (where the meta information is stored) yourself<br>
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 1559 $
 * 
 */
public class MetaInformationHandler implements HandlerToMakeYourLifeEasier {
    protected ObjectContainingMetaInformation objectWithMetaInformation = null;


    protected PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);
    /**
     * 
     *@param layerWithMetaInformation the layer you want the meta information of (has to have a DataSource!!)
     */
    public MetaInformationHandler(Layer layerWithMetaInformation) {
        super();
        if (layerWithMetaInformation!=null) {
            FeatureCollection fc = layerWithMetaInformation.getFeatureCollectionWrapper().getUltimateWrappee();
            
            if (!PirolFeatureCollection.class.isInstance(fc)){
                fc = createPirolFeatureCollection(fc);
                layerWithMetaInformation.setFeatureCollection(fc);
            }
            
            this.objectWithMetaInformation = (PirolFeatureCollection)fc;
        } else {
            throw new RuntimeException("given layer is null.");
        }
    }
    
    /**
     * 
     *@param objectWithMetaInformation the object you want the meta information of
     */
    public MetaInformationHandler(ObjectContainingMetaInformation objectWithMetaInformation) {
        super();
        if (objectWithMetaInformation!=null) {
            this.objectWithMetaInformation = objectWithMetaInformation;
        } else {
            throw new RuntimeException("given layer is null.");
        }
    }

    
    /**
     * creates a PirolFeatureCollection out of a regular FeatureCollection
     *@param fc regular FeatureCollection
     *@return PirolFeatureCollection
     */
    public static final PirolFeatureCollection createPirolFeatureCollection(FeatureCollection fc){
        return MetaInformationHandler.createPirolFeatureCollection(fc, new RoleStandardFeatureCollection());
    }
    
    /**
     * creates a PirolFeatureCollection out of a regular FeatureCollection
     *@param fc regular FeatureCollection
     *@return PirolFeatureCollection
     */
    public static final PirolFeatureCollection createPirolFeatureCollection(FeatureCollection fc, FeatureCollectionRole role){
        PirolFeatureCollection pfc = null;
        
        if (!PirolFeatureCollection.class.isInstance(fc)){
            pfc = new PirolFeatureCollection(fc, role); 
        } else {
            pfc = (PirolFeatureCollection)fc;
            
            if (!role.equalsRole(FeatureCollectionRoleTypes.STANDARD)){
                pfc.addRole(role);
            }
            
        }
        
        return pfc;
    }
    
    /**
     * Retrieve the existent meta information map. 
     *@return the existent meta information map or null, if there is none
     */
    public MetaDataMap getExistentMetaInformationMap(){
        if (this.containsMetaInformation()){
            return this.getMetaInformationMap();
        }
        return null;
    }
    
    /**
     * Retrieve the existent meta information map or create one. 
     *@return the existent meta information map or an empty meta information map (that is now attached to the DataSource)
     *@throws RuntimeException, if the given DataSource doesn't even have properties (<code>getProperties()</code>)
     */
    public MetaDataMap getMetaInformationMap(){
        if (!this.containsMetaInformation()){
            MetaDataMap newMap = new MetaDataMap();
            
            if (this.objectWithMetaInformation!=null){
                this.logger.printDebug("creating new meta map for " + this.objectWithMetaInformation);
                this.objectWithMetaInformation.setMetaInformation(newMap);
            } else
                return null;
            
            return newMap;
        } else if (this.objectWithMetaInformation!=null){
            return this.objectWithMetaInformation.getMetaInformation();
        } 
        
        return null;
    }
    
    /**
     * @return true if the given layer already contains meta information, false if not
     */
    public boolean containsMetaInformation(){
        return (this.objectWithMetaInformation!=null && this.objectWithMetaInformation.getMetaInformation()!=null);
    }

    /**
     * Adds a new meta information key-value-pair to the meta information map, replaces
     * an existing pair with the same key.
     *@param key
     *@param value
     */
    public void addMetaInformation(String key, Object value) {
        MetaDataMap metaMap = this.getMetaInformationMap();
        if (metaMap.containsKey(key)) metaMap.remove(key);        
        metaMap.addMetaInformation(key, value);
    }

    public HashMap getMetaData() {
        MetaDataMap metaMap = this.getMetaInformationMap();
        return metaMap.getMetaData();
    }

    public void setMetaData(HashMap<Object,Object> metaData) {
        MetaDataMap metaMap = this.getMetaInformationMap();
        metaMap.setMetaData(metaData);
    }
    
    public void clear() {
        MetaDataMap metaMap = this.getMetaInformationMap();
        metaMap.clear();
    }

    public boolean containsKey(String key) {
        MetaDataMap metaMap = this.getMetaInformationMap();
        return metaMap.containsKey(key);
    }
    
    public Object getMetaInformation(String key) {
        MetaDataMap metaMap = this.getMetaInformationMap();
        return metaMap.get(key);
    }

    public boolean containsValue(Object value) {
        MetaDataMap metaMap = this.getMetaInformationMap();
        return metaMap.containsValue(value);
    }

    public Set keySet() {
        MetaDataMap metaMap = this.getMetaInformationMap();
        return metaMap.keySet();
    }

    public Object remove(String key) {
        MetaDataMap metaMap = this.getMetaInformationMap();
        return metaMap.remove(key);
    }
    
    // Specific information
    
    /**
     * @return the Attribute2UnitMap of the given DataSource or null, if there is none
     */
    public Attribute2UnitMap getAttribute2UnitMap(){
        if (this.containsAttribute2UnitMap())
            return (Attribute2UnitMap)this.getMetaInformation(Attribute2UnitMap.KEY_ATTRIBUTE2UNIT);
        return null;        
    }
    
    public void putAttribute2UnitMap(Attribute2UnitMap attribute2UnitMap){
        this.addMetaInformation(Attribute2UnitMap.KEY_ATTRIBUTE2UNIT,attribute2UnitMap);
    }
    
    public boolean containsAttribute2UnitMap() {
        return this.containsKey(Attribute2UnitMap.KEY_ATTRIBUTE2UNIT);
    }
    
    
}
