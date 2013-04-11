/*
 * Olivier BEDEL 
 * Bassin Versant du Jaudy-Guindy-Bizien, 
 * Laboratoire RESO UMR ESO 6590 CNRS, Universit&euml; de Rennes 2
 * 
 */

package org.openjump.sigle.utilities.gui;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.*;

/**
 * Utility class to select attributes from a layer.
 * @author Olivier
 * @version 2005-08-14
 */

public class DialogUtil {

    //  renvoie la liste des champs de la table attributaire d'une couche
    public static List getFieldsFromLayer(Layer l) {
        List fields = new LinkedList();
        for (int i=0; i< l.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount(); i++) {
            fields.add(l.getFeatureCollectionWrapper().getFeatureSchema().getAttributeName(i));  
        }
        return fields;
    }
    
    // update the combobox with layer 1 attributes
    public static void updateFieldComboBox(JComboBox cb, Layer l) {
        cb.removeAllItems();
        for (Iterator i = getFieldsFromLayer(l).iterator(); i.hasNext();)
            cb.addItem(i.next());
    }
        
    //  renvoie la liste des champs de la table attributaire d'une couche sans le champ geometry
    public static List getFieldsFromLayerWithoutGeometry(Layer l) {
        List fields = new LinkedList();
        AttributeType type;
        for (int i=0; i< l.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount(); i++) {
        	
        	type =  l.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(i);
                    	
        	if(type == AttributeType.GEOMETRY){
        			
           }
           else {
        	fields.add(l.getFeatureCollectionWrapper().getFeatureSchema().getAttributeName(i));  
           }
           }
        return fields;
    }
    
    // returns a Double computed from attributeName value of feature f
    // if the value is not parsable as a double, returns NaN (Not a Number)  
    public static Double getDoubleFromAttribute(Feature f, String attributeName) {
        Double d = new Double(Double.NaN);
        AttributeType type =  f.getSchema().getAttributeType(attributeName);
        if (type == AttributeType.DOUBLE)
            d = (Double) f.getAttribute(attributeName); 
        else if (type == AttributeType.INTEGER)
            d = new Double(((Integer) f.getAttribute(attributeName)).doubleValue());
        else  if (type == AttributeType.STRING)
            d = Double.valueOf((String) f.getAttribute(attributeName));
        return d;
    }
    
    // update a JLabel with the AttributeType of the selected attribute
    public static void updateFieldTypeLabel(JLabel label, Layer l, String attributeName) {
                
        AttributeType nameType = l.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(attributeName);
        label.setText(nameType.toString());
    }
    
    public static void setLayerNamesAsListData(LayerManager argManager, JList argList)
    {
    	List layers = argManager.getLayers();
    	List<String> layerNames = new LinkedList<String>();
    	
    	Iterator goOverEach = layers.iterator();
    	
    	while(goOverEach.hasNext())
    	{
    		Layer thisElement = (Layer) goOverEach.next();
    		String layerName = thisElement.getName();
    		layerNames.add(layerName);
    	}
    	Object[] arrayForJList = layerNames.toArray();
    	argList.setListData(arrayForJList);
    }
}
