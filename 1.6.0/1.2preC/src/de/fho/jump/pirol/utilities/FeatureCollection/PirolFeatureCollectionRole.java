/*
 * Created on 09.11.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.FeatureCollection;

/**
 * Base class for different roles of a PirolFeatureCollection, like RasterImage, Grid, Outline, etc.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * @see de.fhOsnabrueck.jump.pirol.utilities.FeatureCollection.PirolFeatureCollection
 * 
 */
public abstract class PirolFeatureCollectionRole {

    protected PirolFeatureCollectionRoleTypes type = null;
    
    /** Number of existent roles roles */
    public static final int numOfExistentRoles = PirolFeatureCollectionRoleTypes.getNumOfTypes();
    
    
    public PirolFeatureCollectionRole(PirolFeatureCollectionRoleTypes type){
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
    
    public PirolFeatureCollectionRoleTypes getType(){
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
    public boolean equalsRole(PirolFeatureCollectionRole role) {
        return this.getRoleId() == role.getRoleId();
    }
    
    /**
     * Check if this role is the same type of role as the given one.
     * Caution: If this role contains specific information (like RasterImage role), this information
     * is not checked for equality - Only the type of the role is checked!
     *@param role role to check for type equality
     *@return true if this role is the same type of role as the given one, else false
     */
    public boolean equalsRole(PirolFeatureCollectionRoleTypes roleType) {
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
