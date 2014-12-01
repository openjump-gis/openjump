/*
 * Created on 09.11.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2434 $
 *  $Date: 2006-09-12 12:31:50 +0200 (Di, 12 Sep 2006) $
 *  $Id: PirolFeatureCollectionRole.java 2434 2006-09-12 10:31:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.apitools.objecttyperoles;

/**
 * Base class for different roles of a PirolFeatureCollection, like RasterImage, Grid, Outline, etc.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2434 $
 * @see de.fhOsnabrueck.jump.pirol.utilities.FeatureCollection.PirolFeatureCollection
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public abstract class FeatureCollectionRole {

    protected FeatureCollectionRoleTypes type = null;
    
    /** Number of existent roles roles */
    public static final int numOfExistentRoles = FeatureCollectionRoleTypes.getNumOfTypes();
    
    
    public FeatureCollectionRole(FeatureCollectionRoleTypes type){
        this.type = type;
    }
    
    /**
     * 
     *@return an integer that specifies the role type of the derived role object
     *
     *@see PirolFeatureCollectionRoleTypes
     */
    public int getRoleId(){
        return this.type.getType();
    }
    
    public FeatureCollectionRoleTypes getType(){
        return this.type;
    }
    
    /**
     * 
     *@return true if it contains gridded data (e.g. grid layer), else false 
     */
    public boolean containsGrid(){
        return false;
    }
    
    /**
     * 
     *@return true if it contains raster data (e.g. raster image layer), else false 
     */
    public boolean containsImage(){
        return false;
    }

    /**
     * Check if this role is the same type of role as the given one.
     * Caution: If this role contains specific information (like RasterImage role), this information
     * is not checked for equality - Only the type of the role is checked!
     *@param role role to check for type equality
     *@return true if this role is the same type of role as the given one, else false
     */
    public boolean equalsRole(FeatureCollectionRole role) {
        return this.getRoleId() == role.getRoleId();
    }
    
    /**
     * Check if this role is the same type of role as the given one.
     * Caution: If this role contains specific information (like RasterImage role), this information
     * is not checked for equality - Only the type of the role is checked!
     *@param role role to check for type equality
     *@return true if this role is the same type of role as the given one, else false
     */
    public boolean equalsRole(FeatureCollectionRoleTypes roleType) {
        return this.getRoleId() == roleType.getType();
    }
    
    /**
     * Check if this role is the same type of role as the given role id.
     *@param roleID id of the role type to check for type equality
     *@return true if this role is the same type of role as the given ID, else false
     *
     *@see PirolFeatureCollectionRoleTypes
     */
    public boolean equalsRole(int roleID) {
        return this.getRoleId() == roleID;
    }
    
    /**
     * Method to be called e.g. by a FeatureCollection, when it's disposed
     * to free RAM that may be bound in references to objects, that are still
     * referenced by other objects, but won't be used without the FeatureCollection.
     * 
     */
    public void clearRam(){
        return;
    }
    
}
