/**
 * @author Olivier BEDEL
 * 	Laboratoire RESO UMR 6590 CNRS
 * 	Bassin Versant du Jaudy-Guindy-Bizien
 * 	26 oct. 2004
 * 
 */
package org.openjump.sigle.plugin.joinTable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;

/**
 * @author Olivier BEDEL
 * Laboratoire RESO UMR 6590 CNRS
 * Bassin Versant du Jaudy-Guindy-Bizien
 * 26 oct. 2004
 * license Licence CeCILL http://www.cecill.info/
 * 
 */
public class JoinTable {
	// TODO: obedel voir pour la definition d'un type enumere
	//static Object DataSourceType =  CSV ;
	
	private JoinTableDataSource dataSource = null;
	private ArrayList fieldNames = null;
	private ArrayList fieldTypes = null;
	private Hashtable table = null;
	private int keyIndex = -1;
	private int fieldCount = 0;

	//	TODO: obedel voir pour la prise en compte du type de source de donnee
	public JoinTable(String filePath ) { 
		dataSource = new JoinTableDataSourceCSV(filePath);
		fieldNames = dataSource.getFieldNames();
		fieldCount = fieldNames.size();
	}
	
	public List getFieldNames()
	{
		return fieldNames;
	}
	public String getFieldName(int indice)
	{
		return (String) fieldNames.get(indice);
	}
	
	public AttributeType getFieldType(int indice)
	{
		return (AttributeType) fieldTypes.get(indice);
	}
	
	public int getFieldCount()
	{
		return fieldCount;
	}
	
	public void setKeyIndex(int keyIndex)
	{
		this.keyIndex =keyIndex;
	}
	
	public int getKeyIndex() {
		return keyIndex;
	}
	
	public void build()
	{
		if (keyIndex>-1)
		{
			table = dataSource.buildTable(keyIndex);
			fieldTypes = dataSource.getFieldTypes();
		}
			 
	}
	
	public void join(Layer layer, int attributeIndex) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		layer.setEditable(true);
		
		// rajout des attributs de la table dans la couche
		FeatureSchema schema;
		String nomChamp;
		String suffixe="";
		int nbOldAttributes;
		
		schema = (FeatureSchema) layer.getFeatureCollectionWrapper().getFeatureSchema().clone();
		nbOldAttributes = schema.getAttributeCount();
		for (int i=0; i<fieldNames.size();i++) {
			if (i!=keyIndex) {
				nomChamp = (String) fieldNames.get(i);
				suffixe="";
				int j = 0;
				while (schema.hasAttribute(nomChamp + suffixe)) {
						j++;
						suffixe = String.valueOf(j);
				} 
				nomChamp = nomChamp + suffixe;
				fieldNames.set(i,nomChamp);
				
				AttributeType t = (AttributeType) fieldTypes.get(i);
				schema.addAttribute(nomChamp, t);
			}
		}
		
		// parcours des entites de la couche et remplissage des nouveaux champs
		Feature f, fNew;
		String keyValue;
		Object value;
		ArrayList newFeatures;
		String[] valeurs;
		
		FeatureCollection fc = layer.getFeatureCollectionWrapper();
		List features = fc.getFeatures(); 
		
		
		newFeatures = new ArrayList(features.size());
		
		for (Iterator i = features.iterator(); i.hasNext();) {
			f = (Feature) i.next();
			
			// probleme de restitution de la classe de l'objet f dans fNew --> voir methode clone
			//Class [] parameterType = { FeatureSchema.class };
			//Object [] parameter = { schema };
			//Constructor c = f.getClass().getConstructor(parameterType); 
			//fNew = (Feature) c.newInstance(parameter);
			
			fNew = f.clone(true);
			fNew.setSchema(schema);
			fNew.setAttributes(new Object[schema.getAttributeCount()]);
			int j=0;
			while(j<nbOldAttributes) {
				fNew.setAttribute(j, f.getAttribute(j));
				j++;
			}
					
			newFeatures.add(fNew);			
				
			keyValue = fNew.getString(attributeIndex).trim();
			valeurs = (String[]) table.get(keyValue);
			for (j=0; j<fieldCount; j++){
				if (j!=keyIndex) {
					if (valeurs != null)
						value = castValue((String) valeurs[j], (AttributeType) fieldTypes.get(j));
					else 
						value =null;					
					fNew.setAttribute((String) fieldNames.get(j), value);
				}
			}
					
		}
		
		// mise a jour de la couche
		layer.setFeatureCollection(new FeatureDataset(newFeatures, schema));
		
		layer.setEditable(false);
		

	}
	
	// liberation memoire organisee
	public void dispose() {
		if (table!=null) table.clear();
		if (fieldTypes!=null) fieldTypes.clear();
		if (fieldNames!=null) fieldNames.clear();
		keyIndex = -1;
		fieldCount = 0;
		table=null;
		fieldTypes=null;
		fieldNames=null;
		dataSource = null;
	}
	
	private Object castValue(String s, AttributeType t) {
		try 
		{
			Object res;
			if (t == AttributeType.DOUBLE){
				s = s.replace(',','.').replaceAll(" ","");
				res = s.length() == 0 ? null : Double.valueOf(s); // uniformisation du format numerique
			}
			else if (t == AttributeType.INTEGER) {
				s = s.replaceAll(" ","");
				res = s.length() == 0 ? null : Integer.valueOf(s);
			}
			else
				res = s.toString();
			return res;
		}
		catch (Exception e)
		{
			// pour eviter les mauvaises surprises
			return null;
		}
	}
}
