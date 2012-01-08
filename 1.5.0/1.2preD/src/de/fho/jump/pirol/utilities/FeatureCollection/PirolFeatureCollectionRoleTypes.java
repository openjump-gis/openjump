/*
 * Created on 21.08.2006
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.FeatureCollection;

/**
 * TODO: comment class
 *
 * <br><br><b>Last change by $Author$ on $Date$</b>
 *
 * @author Ole Rahn
 * 
 * @version $Rev$
 * 
 */
public enum PirolFeatureCollectionRoleTypes {
    STANDARD(0),
    GRID(1),
    OUTLINE(2),
    POINTLAYER(3),
    TIN(4);
    
    int type = -1;
    
    PirolFeatureCollectionRoleTypes(int type){
        this.type = type;
    }
    
    public static int getNumOfTypes(){
        return PirolFeatureCollectionRoleTypes.values().length;
    }
    
    public int getType(){
        return this.type;
    }
    
}
