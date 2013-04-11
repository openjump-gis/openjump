/*
 * Created on 22.09.2005 for PIROL
 *
 * SVN header information:
 *  $Author: mentaer $
 *  $Rev: 1654 $
 *  $Date: 2009-02-16 21:32:04 -0700 (Mo, 16 Feb 2009) $
 *  $Id: PirolFeatureCollection.java 1654 2009-02-17 04:32:04Z mentaer $
 */
package org.openjump.core.apitools.objecttyperoles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openjump.core.apitools.CollectionsTools;
import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.util.metaData.MetaDataMap;
import org.openjump.util.metaData.ObjectContainingMetaInformation;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * 
 * Class that wraps a FeatureDataset and adds methods to get and set meta information objects. 
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 1654 $
 * @see org.openjump.util.metaData.MetaInformationHandler#createPirolFeatureCollection(FeatureCollection)
 *
 */
public class PirolFeatureCollection implements FeatureCollection, ObjectContainingMetaInformation {

    private static final long serialVersionUID = 1997134887214940597L;
    
    protected FeatureCollection featureDataSet = null;
    protected MetaDataMap metaInformation = null;
    
    protected FeatureCollectionRole[] roles = null;
    
    public PirolFeatureCollection(FeatureCollection featureDataSet, FeatureCollectionRole initRole){
        this.featureDataSet = featureDataSet;
        
        this.roles = new FeatureCollectionRole[FeatureCollectionRole.numOfExistentRoles];
        this.roles[0] = initRole;
    }

    /**
     * Constructor - for java2xml, only!!
     *
     */
    public PirolFeatureCollection(){ }
    
    /**
     * for java2xml
     */
    public Collection getXmlRoles(){
        ArrayList rolesForXml = new ArrayList();
        CollectionsTools.addArrayToList(rolesForXml, this.roles);
        return rolesForXml;
    }
    
    /**
     * for java2xml
     */
    public void addXmlRole(FeatureCollectionRole role){
        this.addRole(role);
    }
    
    /**
     * for java2xml
     */
    public FeatureCollection getFeatureDataSet() {
        return featureDataSet;
    }

    /**
     * for java2xml
     */
    public void setFeatureDataSet(FeatureCollection featureDataSet) {
        this.featureDataSet = featureDataSet;
    }

    public void addRole(FeatureCollectionRole role){
        for (int i=0; i<this.roles.length; i++){
            if (this.roles[i]==null){
                this.roles[i] = role;
                return;
            } else if (this.roles[i].equalsRole(role)){
                return;
            }
        }
    }
    
    public void removeRole(FeatureCollectionRole role){
        int removedRoleIndex = -1;
        for (int i=0; i<this.roles.length; i++){
            if (this.roles[i]==null){
                return;
            } else if (this.roles[i].equalsRole(role)){
                this.roles[i] = null;
                removedRoleIndex = i;
                return;
            } else {
                if (removedRoleIndex>-1 && i > removedRoleIndex){
                    this.roles[i-1] = this.roles[i];
                    this.roles[i] = null;
                }
            }
        }
    }

    public FeatureCollectionRole[] getRoles() {
        return roles;
    }

    /**
     * Check if this FeatureCollection has a role like the given one
     *@param role the role to check for
     *@return the role if this FeatureCollection has a role like the given one, else null
     *
     *@see org.openjump.core.apitools.objecttyperoles.FeatureCollectionRoleTypes
     */
    public FeatureCollectionRole getRole(FeatureCollectionRole role) {
        for (int i=0; i<this.roles.length && this.roles[i]!=null; i++){
            if (this.roles[i].equalsRole(role)){
                return this.roles[i];
            }
        }
        return null;
    }
    
    /**
     * Check if this FeatureCollection has a role like the given one
     *@param role the role to check for
     *@return the role if this FeatureCollection has a role like the given one, else null
     *
     *@see org.openjump.core.apitools.objecttyperoles.FeatureCollectionRoleTypes
     */
    public FeatureCollectionRole getRole(FeatureCollectionRoleTypes role) {
        for (int i=0; i<this.roles.length && this.roles[i]!=null; i++){
            if (this.roles[i].equalsRole(role)){
                return this.roles[i];
            }
        }
        return null;
    }
    
    /**
     * Check if this FeatureCollection has a role with the given ID
     *@param roleId id of the role type to check for
     *@return the role if this FeatureCollection has a role with the given ID, else null
     *
     *@see org.openjump.core.apitools.objecttyperoles.FeatureCollectionRoleTypes
     */
    public FeatureCollectionRole getRole(int roleId) {
        for (int i=0; i<this.roles.length && this.roles[i]!=null; i++){
            if (this.roles[i].equalsRole(roleId)){
                return this.roles[i];
            }
        }
        return null;
    }

    public MetaDataMap getMetaInformation() {
        return metaInformation;
    }

    public void setMetaInformation(MetaDataMap metaInformation) {
        this.metaInformation = metaInformation;
    }



    public void invalidateEnvelope(boolean simpleInvalidation){
        
        if (FeatureDataset.class.isInstance(featureDataSet)){
            ((FeatureDataset)featureDataSet).invalidateEnvelope();
        }
        
        if (simpleInvalidation) return;
        
        this.featureDataSet.getEnvelope().setToNull();
        
        Feature[] features = FeatureCollectionTools.FeatureCollection2FeatureArray(this.featureDataSet);
        for (int i=0; i<features.length; i++){
            features[i].getGeometry().geometryChanged();
        }
        
//        throw new UnsupportedOperationException("the wrapped FeatureCollection does not support this operation: " + this.featureDataSet.getClass().getName());
    }
    

    public void add(Feature feature) {
        this.invalidateEnvelope(true);
        featureDataSet.add(feature);
    }

    public void addAll(Collection features) {
        this.invalidateEnvelope(true);
        featureDataSet.addAll(features);
    }

    public void clear() {
        this.invalidateEnvelope(true);
        featureDataSet.clear();
    }

    public boolean equals(Object arg0) {
        return featureDataSet.equals(arg0);
    }

    public Envelope getEnvelope() {
        this.invalidateEnvelope(true);
        return featureDataSet.getEnvelope();
    }

    public List getFeatures() {
        this.invalidateEnvelope(true);
        return featureDataSet.getFeatures();
    }

    public FeatureSchema getFeatureSchema() {
        return featureDataSet.getFeatureSchema();
    }

    public int hashCode() {
        return featureDataSet.hashCode();
    }

    public boolean isEmpty() {
        return featureDataSet.isEmpty();
    }

    public Iterator iterator() {
        this.invalidateEnvelope(true);
        return featureDataSet.iterator();
    }

    public List query(Envelope envelope) {
        return featureDataSet.query(envelope);
    }

    public Collection remove(Envelope env) {
        this.invalidateEnvelope(true);
        return featureDataSet.remove(env);
    }

    public void remove(Feature feature) {
        this.invalidateEnvelope(true);
        featureDataSet.remove(feature);
    }

    public void removeAll(Collection features) {
        this.invalidateEnvelope(true);
        featureDataSet.removeAll(features);
    }

    public int size() {
        return featureDataSet.size();
    }

    public String toString() {
        return featureDataSet.toString();
    }
    
    public Feature[] toArray(){
        return FeatureCollectionTools.FeatureCollection2FeatureArray(this.featureDataSet);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        
        for (int i=0; i<this.roles.length && this.roles[i]!=null; i++){
            this.roles[i].clearRam();
        }
    }
    
    /**
     * the PirolFeatureCollection is a wrapper for other feature collections
     * in some cases one might want to work with the original FC. Therefor you can use this method.
     *@return the feature collection wrapped by this PirolFeatureCollection
     */
    public FeatureCollection getWrappee(){
        return this.featureDataSet;
    }

    
}
