
package org.openjump.sigle.utilities.geom;

import java.util.ArrayList;
import java.util.Iterator;

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * @author Erwan Bocher 
 * @author Olivier Bedel
 * Created on 10 août 2005
 * Bassin Versant du Jaudy-Guindy-Bizien, 
 * Laboratoire RESO UMR ESO 6590 CNRS, Université de Rennes 2
 * licence Cecill
 * 
 * Cette classe permet de savoir si la feature collection est composée d'une seule géometrie et dans ce cas le type de géometrie
 *
 */

public class FeatureCollectionUtil {

    
    	// renvoie la dimension des entites de la featureCollection fc :
    	// 0 pour si fc ne contient que des entites ponctuelles
		// 1 pour si fc ne contient que des entites lineaires
    	// 2 pour si fc ne contient que des entites surfaciques
    	// -1 si fc ne contient aucune entite, ou si elle contient des entites 
    	// ayant des dimensions differentes
	
    	public static int getFeatureCollectionDimension(FeatureCollection fc) {
    	    int type = -1;  // type des geometry des entites de la featurecollection
    	    				// -1 correspond au type complexe 
    	    				// (plusieurs dimensions de geometry dans la meme featureCollection) 
    	    
    	    if (fc.getFeatures().size()>0) {
    	        Iterator i = fc.getFeatures().iterator();
    	        
    	        // initialisation des la variable type
    	        Feature f = (Feature) i.next();
    	        type = f.getGeometry().getDimension();
    	        // cas particulier des geometryCollection
    	        if (f.getGeometry() instanceof GeometryCollection) {
	                GeometryCollection geomCol = (GeometryCollection) f.getGeometry();
	                // on ne prend en compte que les geometryCollection non specialisees, ie pas les
	                // multipoint, multilinstring ou multipolygon
	                if (geomCol.getGeometryType().equalsIgnoreCase("GeometryCollection"))
	                    type = -1; 
	            }
    	            
    	        
    	        // on parcourt le reste des entites de la featureCollection
    	        while (i.hasNext() && type !=-1) {
    	            f = (Feature) i.next();
    	            // si la geometrie de f est complexe, on marque le type comme complexe
    	            if (f.getGeometry() instanceof GeometryCollection) {
    	                GeometryCollection geomCol = (GeometryCollection) f.getGeometry();
    	                if (geomCol.getGeometryType().equalsIgnoreCase("GeometryCollection"))
    	                    type = -1; 
    	            }
    	            // si sa dimension ne correspond pas au
    	            // type precedent, on marque le type comme complexe
    	            if  (f.getGeometry().getDimension() != type) 
    	                type = -1;
    	        }
    	    }
    	    
    	    return type;
    	}
    	
    	public static ArrayList getAttributesList(FeatureCollection fc) {
			    		
    		ArrayList AttributesList = new ArrayList();
    		FeatureSchema fs = fc.getFeatureSchema();
    		
    		
    		for (int i=0; i<fs.getAttributeCount()-1;i++){
    			
    			AttributesList.add(fs.getAttributeName(i));

    		}
    		
			return AttributesList;
    	
}
}
