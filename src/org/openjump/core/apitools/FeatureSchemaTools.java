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
import java.util.List;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;

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
        ArrayList<AttributeInfo> attrInfosList = new ArrayList<>();
        for (AttributeInfo attrInfo :  AttributeInfo.schema2AttributeInfoArray(fs)){
            if (FeatureSchemaTools.isAttributeTypeAllowed(attrInfo.getAttributeType(),allowedTypes)){
                attrInfosList.add(attrInfo);
            }
        }
        return attrInfosList.toArray(new AttributeInfo[0]);
    }
    
    /**
     * Extracts information on the attribute at the given index from the feature schema.
     *@param fs FeatureSchema to get information from
     *@param attrIndex index of the attribute in the given featureSchema to get information about
     *@return information about the attribute
     */
    private static AttributeInfo getAttributesInfoFor(FeatureSchema fs, int attrIndex){
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
    private static AttributeInfo getAttributesInfoFor(FeatureSchema fs, String attrName){
        return FeatureSchemaTools.getAttributesInfoFor(fs, fs.getAttributeIndex(attrName));
    }
    
    /**
     * Checks if the given attribute type is contained in the given array of allowed attribute types.
     *@param at attribute type to be checked
     *@param allowedTypes array of allowed attribute types
     *@return true if <code>at</code> is contained in <code>allowedTypes</code>, else false
     */
    private static boolean isAttributeTypeAllowed(AttributeType at, AttributeType[] allowedTypes){
        for (AttributeType type : allowedTypes){
            if (type.equals(at)) return true;
        }
        return false;
    }

    /**
     * copy/clone the input featureSchema since it is not proper implemented in Jump
     * Note : FeatureSchema has now a proper deep clone implementation
     * @deprecated
     * @param oldSchema old schema
     * @return a new FeatureSchema cloned from oldSchema
     */
    public static FeatureSchema copyFeatureSchema(FeatureSchema oldSchema){
        FeatureSchema fs = new FeatureSchema();
        for (int i = 0; i < oldSchema.getAttributeCount(); i++) {
            AttributeType at = oldSchema.getAttributeType(i);
            String aname = oldSchema.getAttributeName(i);
            fs.addAttribute(aname,at);
            fs.setAttributeReadOnly(i, oldSchema.isAttributeReadOnly(i));
            fs.setCoordinateSystem(oldSchema.getCoordinateSystem());            
        }       
        return fs;
    }
    
    /**
     * copy the input feature to a new Schema whereby the new 
     * Feature Schema must be an extended or shortened one 
     * @param feature the feature to copy from
     * @param newSchema the schema to copy to
     * @return a new feature which is a copy of feature in the new Schema
     */
    public static Feature copyFeature(Feature feature, FeatureSchema newSchema){
        FeatureSchema oldSchema = feature.getSchema();
        Feature newF = new BasicFeature(newSchema);
        int n;
        if (oldSchema.getAttributeCount() > newSchema.getAttributeCount()){
            //for schema shortening
            n = newSchema.getAttributeCount();
        }
        else{
            //for schema extension
            n = oldSchema.getAttributeCount();
        }
        for (int i = 0; i < n; i++) {           
            String aname = oldSchema.getAttributeName(i);
            Object value = feature.getAttribute(aname);         
            newF.setAttribute(aname,value);                     
        }       
        return newF;
    }
    
    public static List<String> getFieldsFromLayerWithoutGeometryAndString(Layer lyr) {
        List<String> fields = new ArrayList<>();
        FeatureSchema schema = lyr.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            if ((schema.getAttributeType(i) != AttributeType.GEOMETRY) &&
                    (schema.getAttributeType(i) != AttributeType.STRING))
            {
                fields.add(schema.getAttributeName(i));  
           }
        }
        return fields;
    }
    
}
