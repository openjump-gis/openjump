/*
 * Created on 28.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package org.openjump.core.apitools;

import java.util.ArrayList;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;

import de.fho.jump.pirol.utilities.attributes.AttributeInfo;

/**
 * Class for easier handling of featureSchema objects.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 */
public class FeatureSchemaTools extends ToolToMakeYourLifeEasier {

    /**
     * Extracts information of all attributes with one of the given types from the given FeatureSchema
     *@param fs FeatureSchema to get information from
     *@param allowedTypes array with AttributeTypes, that specify which attribute to get information about
     *@return array with information on matching attributes
     */
    public static AttributeInfo[] getAttributesWithTypes(FeatureSchema fs, AttributeType[] allowedTypes){
        ArrayList<AttributeInfo> attrInfosList = new ArrayList<AttributeInfo>();
        AttributeInfo[] attrInfoArray = AttributeInfo.schema2AttributeInfoArray(fs);
        int numInfos = attrInfoArray.length;
        
        for ( int i=0; i<numInfos; i++){
            if (FeatureSchemaTools.isAttributeTypeAllowed(attrInfoArray[i].getAttributeType(),allowedTypes)){
                attrInfosList.add(attrInfoArray[i]);
            }
        }
        
        numInfos = attrInfosList.size();
        AttributeInfo[] attrInfoRaw = (AttributeInfo[])attrInfosList.toArray(new AttributeInfo[0]);
        
        return attrInfoRaw;
    }
    
    /**
     * Extracts information on the attribute at the given index from the feature schema.
     *@param fs FeatureSchema to get information from
     *@param attrIndex index of the attribute in the given featureSchema to get information about
     *@return information about the attribute
     */
    public static AttributeInfo getAttributesInfoFor(FeatureSchema fs, int attrIndex){
        AttributeInfo attrInfo = new AttributeInfo(fs.getAttributeName(attrIndex), fs.getAttributeType(attrIndex));
        
        attrInfo.setIndex(attrIndex);
        
        return attrInfo;
    }
    
    /**
     * Extracts information on the attribute with the given name from the feature schema.
     *@param fs FeatureSchema to get information from
     *@param attrName name of the attribute in the given featureSchema to get information about
     *@return information about the attribute
     */
    public static AttributeInfo getAttributesInfoFor(FeatureSchema fs, String attrName){
        return FeatureSchemaTools.getAttributesInfoFor(fs, fs.getAttributeIndex(attrName));
    }
    
    /**
     * Checks if the given attribute type is contained in the given array of allowed attribute types.
     *@param at attribute type to be checked
     *@param allowedTypes array of allowed attribute types
     *@return true if <code>at</code> is contained in <code>allowedTypes</code>, else false
     */
    public static boolean isAttributeTypeAllowed(AttributeType at, AttributeType[] allowedTypes){
        int numTypes = allowedTypes.length;
        for (int i=0; i<numTypes; i++){
            if (allowedTypes[i].equals(at)) return true;
        }
        return false;
    }

}
