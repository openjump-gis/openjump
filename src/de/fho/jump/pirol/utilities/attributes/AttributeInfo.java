/*
 * Created on 03.03.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.attributes;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;


/**
 * Class to store all information for a given attribute, so it can easily passed
 * to e.g. to methods of the FeatureCollectionTools 
 * 
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 * @see de.fho.jump.pirol.utilities.FeatureCollectionTools
 */
public class AttributeInfo implements Comparable {

    protected AttributeType attributeType = null;
    protected String attributeName = null;
    protected String uniqueAttributeName = null;
    protected String unitIdentifier = null;
    protected Object nullValue = null;
    
    /**
     * For attributes that have been loaded from the DB
     */
    protected int dataBaseId = -1;
    
    /**
     * attribute's index in a (given?) FeatureSchema
     */
    protected int index = -1;
    
    public AttributeInfo(AttributeType attributeType, String attributeName,
            Object nullValue) {
        this(attributeType, attributeName);
        this.nullValue = nullValue;
    }
    
    public AttributeInfo(AttributeType attributeType, String attributeName) {
        super();
        this.attributeType = attributeType;
        this.attributeName = attributeName;
    }
 
    public AttributeInfo(String attributeName, Object nullValue) {
        super();
        this.attributeName = attributeName;
        this.nullValue = nullValue;
    }
    
    public String getAttributeName() {
        return attributeName;
    }
    public AttributeType getAttributeType() {
        return attributeType;
    }
    
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public Object getNullValue() {
        return nullValue;
    }
    public String getUniqueAttributeName() {
        if (uniqueAttributeName==null)
            return this.getAttributeName();
        return uniqueAttributeName;
    }
    
    public void setUniqueAttributeName(String uniqueAttributeName) {
        this.uniqueAttributeName = uniqueAttributeName;
    }
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
    public void setAttributeType(AttributeType attributeType) {
        this.attributeType = attributeType;
    }
    
    public void setNullValue(Object nullValue) {
        if (this.attributeType != null){
            if ( (this.attributeType.equals(AttributeType.DOUBLE) && !Double.class.isInstance(nullValue)) ||
                 (this.attributeType.equals(AttributeType.INTEGER) && !Integer.class.isInstance(nullValue)) )
                throw new IllegalArgumentException("default value is of a wrong type: " + nullValue.getClass().getName());
            else if (this.attributeType.equals(AttributeType.STRING) && !String.class.isInstance(nullValue))
                nullValue = new String(nullValue.toString());
        }

        this.nullValue = nullValue;
    }
    public String getUnitIdentifier() {
        return unitIdentifier;
    }
    public void setUnitIdentifier(String unitIdentifier) {
        this.unitIdentifier = unitIdentifier;
    }

    /**
     * convenient method to convert a FeatureSchema into an AttributeInfo array
     *@param fs the FeatureSchema
     *@return an array of AttributeInfos matching the FeatureSchema
     */
    public static AttributeInfo[] schema2AttributeInfoArray(FeatureSchema fs){
        AttributeInfo[] result = new AttributeInfo[fs.getAttributeCount()];
        
        for (int i=0; i<result.length; i++){
            result[i] = new AttributeInfo(fs.getAttributeType(i), fs.getAttributeName(i));
            result[i].setIndex(i);
        }
        
        return result;
    }
    
    /**
     * convenient method to convert an AttributeInfo array into a FeatureSchema
     *@param attributeInfos an array of AttributeInfos matching wanted in the FeatureSchema
     *@return the desired FeatureSchema
     */
    public static FeatureSchema attributeInfoArray2FeatureSchema(AttributeInfo[] attributeInfos){
        FeatureSchema result = new FeatureSchema();
        
        for ( int i=0; i<attributeInfos.length; i++){
            result.addAttribute( attributeInfos[i].getUniqueAttributeName(), attributeInfos[i].getAttributeType() );
        }
        
        return result;
    }
    
    
    /**
     *@inheritDoc
     */
    public String toString() {
        return this.getUniqueAttributeName();
    }

    /**
     * In order for this to work, the attribute indices of this AttributeInfo object and of the other
     * one has to be set, correctly!
     *@param theOtherObject
     *@return see <code>Comparable</code> for
     */
    public int compareTo(Object theOtherObject) {
        int iTheOtherIndex = ((AttributeInfo)theOtherObject).getIndex();
        int iThisIndex = this.getIndex();
        
        if (Math.min(iThisIndex, iTheOtherIndex) > -1){
            // since both indices may be -1 --> not set...
            Integer theOtherIndex = new Integer(iTheOtherIndex);
            Integer thisIndex = new Integer(iThisIndex);
            return thisIndex.compareTo(theOtherIndex);
        }
        // ... we may want to throw an exception...
        throw new IllegalStateException("at least one index is not set correctly: " + iThisIndex + ", " + iTheOtherIndex);
    }

	public int getDataBaseId() {
		return dataBaseId;
	}

	public void setDataBaseId(int dataBaseId) {
		this.dataBaseId = dataBaseId;
	}
    
    
}
