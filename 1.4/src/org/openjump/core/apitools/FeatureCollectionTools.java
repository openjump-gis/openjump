/*
 * Created on 12.01.2005
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$s
 */
package org.openjump.core.apitools;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjump.core.apitools.comparisonandsorting.ObjectComparator;
import org.openjump.core.apitools.objecttyperoles.PirolFeatureCollection;
import org.openjump.util.metaData.MetaInformationHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;
import de.fho.jump.pirol.utilities.attributes.AttributeInfo;
import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;

/**
 * Class to speed up handling of FeatureCollections (or lists of features) during progamming by implementing
 * a set of common tasks.
 * Most functions can be used in a static way, but on the other hand for each FeatureCollection a instance of this class can be invoked.
 * This might be more convenient, if there is more than one thing to do with the same feature collection. 
 * 
 * @author Ole Rahn, Stefan Steiniger
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */

public class FeatureCollectionTools extends ToolToMakeYourLifeEasier {
    
    protected FeatureCollection fc = null;
    protected List<Feature> featureList;
    private HashMap<Integer, Feature> fid2Object = new HashMap<Integer, Feature>();
    
    protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL); 
    
    public FeatureCollectionTools( FeatureCollection fc ){
        super(); 
        this.fc = fc;
        this.featureList = this.fc.getFeatures();
    }
    
    public FeatureCollectionTools( List<Feature> fcl ){
        super();
        this.featureList = fcl;
    }

    /**
     * gets the Feature with the given FID.
     *@param fid FID to look for
     *@return the feature
     */
    public Feature getFeature( int fid ){
        Integer FID = new Integer(fid);
		if (!this.fid2Object.containsKey(FID)){
		    this.fid2Object.put(FID,FeatureCollectionTools.getFeatureFromCollection(this.featureList, fid));
		}
		
		return (Feature)this.fid2Object.get(FID);
	}

    
    /**
     * Get the feature with the specified ID from the List 
     *@param features array with Feature objects
     *@param fid the feature ID we are looking for
     *@return feature with specified FID if exists, else null
     */
    public static Feature getFeatureFromCollection( List<Feature> features, int fid ){
		return FeatureCollectionTools.getFeatureFromCollection((Feature[])features.toArray(new Feature[0]), fid);		
	}
    
    /**
     * Get the feature with the specified ID from the array 
     *@param features array with Feature objects
     *@param fid the feature ID we are looking for
     *@return feature with specified FID if exists, else null
     */
    public static Feature getFeatureFromCollection( Feature[] features, int fid ){
		Feature feat;
		
		for ( int i=0; i < features.length; i++ ){
			feat = features[i];
			
			if (feat.getID() == fid){
				return feat;
			}
		}
		return null;		
	}
    
    /**
     * deep copies a the FeatureSchema, the Features and their Geometries
     * @param fc the FeatureCollection to copy
     * @return the new copied FeatureCollection
     */
    public final static PirolFeatureCollection cloneFeatureCollection(FeatureCollection fc){
    	FeatureSchema clonedSchema = (FeatureSchema)fc.getFeatureSchema().clone();
    	FeatureDataset newFc = new FeatureDataset(clonedSchema);
    	
    	Feature[] featuresToCopy = FeatureCollection2FeatureArray(fc);
    	Feature newFeat = null;
    	AttributeType attrType = null;
    	
    	for (int i=0; i<featuresToCopy.length; i++){
    		newFeat = new BasicFeature(clonedSchema);
    		
    		for (int attr=0; attr<clonedSchema.getAttributeCount(); attr++){
    			attrType = clonedSchema.getAttributeType(attr);
    			/**
			     * FIX for Null Values.
			     * Added by mweller 24.07.2007
			     */
		      if(featuresToCopy[i].getAttribute(attr) != null){
      			if (attrType.equals(AttributeType.DOUBLE)){
      				newFeat.setAttribute(attr, new Double(((Double)featuresToCopy[i].getAttribute(attr)).doubleValue()) );
      			} else if (attrType.equals(AttributeType.INTEGER)){
      				newFeat.setAttribute(attr, new Integer(((Integer)featuresToCopy[i].getAttribute(attr)).intValue()) );
      			} else if (attrType.equals(AttributeType.STRING)){
      				newFeat.setAttribute(attr, ((String)featuresToCopy[i].getAttribute(attr)).trim() );
      			} else if (attrType.equals(AttributeType.GEOMETRY)){
      				newFeat.setAttribute(attr, ((Geometry)featuresToCopy[i].getAttribute(attr)).clone() );
      			} else if (attrType.equals(AttributeType.DATE)){
      				newFeat.setAttribute(attr, ((Date)featuresToCopy[i].getAttribute(attr)).clone() );
      			} else if (attrType.equals(AttributeType.OBJECT)){
      				logger.printError("not implemented!");
      				newFeat.setAttribute(attr, (featuresToCopy[i].getAttribute(attr)) );
      			}
      		}
      		else{
      		  newFeat.setAttribute(attr, null );
          }
    		}
    		
    		newFc.add(newFeat);
    	}
    	return MetaInformationHandler.createPirolFeatureCollection(newFc);
    }
    
    /**
     * Get the feature with the specified ID from the FeatureCollection 
     *@param features array with Feature objects
     *@param fid the feature ID we are looking for
     *@return feature with specified FID if exists, else null
     */
    public static Feature getFeatureFromCollection( FeatureCollection features, int fid ){
        return FeatureCollectionTools.getFeatureFromCollection(features.getFeatures(), fid);		
	}
    
    /**
     *@param features list of features to calculate the mean for
     *@param attr name of attribute to calculate the mean for
     *@return the mean (double because a mean of integers will very likely be a double) 
     */
    public static double getAritmeticMiddleForAttribute( Feature[] features, String attr ){
        
        if (features == null || features.length==0) return Double.NaN;
        
        Feature f = features[0];
        FeatureSchema fs = f.getSchema();
        int attrInd = fs.getAttributeIndex(attr);
        return FeatureCollectionTools.getAritmeticMiddleForAttribute( features, attrInd );
    }
    
    /**
     * Method to calculate means (or modes) for the attributes given. If means or modes 
     * are calulated depends on the attribute type of each given attribute.
     * This method is hopefully faster, than calculation each mean (mode) in an extra loop,
     * if there are more means (modes) to calculate than one...
     *@param features list of features to calculate mean/modes for
     *@param attrs array of attribute names to calculate mean/modes for
     *@return array of objects, representing means (or modes) --> e.g. Double, Integer or String objects.
     */
    public static Object[] getMeanOrModeForAttributes( Feature[] features, String[] attrs ){
        if (features==null || features.length==0) return null;

        int numVals = features.length;
        int numAttrs = attrs.length;
        
        FeatureSchema fs = features[0].getSchema();
        Object[] meansOrModes = new Object[numAttrs];
        AttributeType[] ats = new AttributeType[numAttrs];

        Object[] sumsOrMaps = new Object[numAttrs];
        boolean[] atIsNumeric = new boolean[numAttrs];
        
        for (int i=0; i<numAttrs; i++){
            ats[i] = fs.getAttributeType(attrs[i]);
            atIsNumeric[i] = FeatureCollectionTools.isAttributeTypeNumeric(ats[i]);
            if (atIsNumeric[i]){
                sumsOrMaps[i] = new Double(0);
            } else {
                // Map; no. of max. occurances; modus value
                sumsOrMaps[i] = new Object[]{ new HashMap(), new Integer(0), null };
            }
        }
        
        Feature currFeat;
        Double sum;
        Object value, modus;
        Map<Object,Object> map;
        
        Feature[] featArray = features;
        
        Integer maxOcc;
        int occ = 0;
        
        for (int i=0; i<featArray.length; i++){
            currFeat = featArray[i];
            for (int j=0; j<numAttrs; j++){
                if (currFeat.getAttribute(attrs[j]) == null){
                    // value is skipped
                    FeatureCollectionTools.logger.printMinorError("skipped a value (NULL), when calculating mean for " + attrs[j]);
                }
                else if (atIsNumeric[j]){
                    sum = ((Double)sumsOrMaps[j]);
                    sumsOrMaps[j] = new Double(sum.doubleValue() + ObjectComparator.getDoubleValue(currFeat.getAttribute(attrs[j])));
                }
                else {
                    value = currFeat.getAttribute(attrs[j]);
                    
                    if (value.getClass().equals(String.class)){
                        ((String)value).trim();
                    }
                    
                    map = (Map)((Object[])sumsOrMaps[j])[0];
                    maxOcc = (Integer)((Object[])sumsOrMaps[j])[1];
                    modus = ((Object[])sumsOrMaps[j])[2];
                    
                    if ( map.containsKey(value) ){
                        occ = ((Integer)map.get(value)).intValue();
                        occ += 1;
                        map.remove(value);
                    } else {
                        occ = 1;
                    }
                    map.put(value,new Integer(occ));
                    
                    if (occ > maxOcc.intValue()){
                        maxOcc = new Integer(occ);
                        modus = value;
                    }
                    
                    sumsOrMaps[j] = new Object[]{map,maxOcc,modus};
                    
                }
            }
        }
        
        for ( int i=0; i<meansOrModes.length; i++ ){
            if (atIsNumeric[i]){
                meansOrModes[i] = new Double( ((Double)sumsOrMaps[i]).doubleValue()/((double)numVals) );
            } else {
                meansOrModes[i] = ((Object[])sumsOrMaps[i])[2];
            }
        }
        
        return meansOrModes;
    }
    
    /**
     * Creates an envelope object for the features in the given array. This is an easy way
     * to get an envelope for a subset of a layer's features.
     *@param features array of Feature object to create an envelope for
     *@return the envelope containing the given features
     */
    public static Envelope getEnvelopeForFeatures(Feature[] features){
        Envelope env = null;
        Feature feat;
        
        for (int i=0; i<features.length; i++){
            feat = features[i];
            if (env==null){
                env = new Envelope(feat.getGeometry().getCoordinate());
            } else {
                env.expandToInclude(feat.getGeometry().getCoordinate());
            }
        }
        
        return env;
    }

    /**
     *@param features list of features to calculate the mean for
     *@param attr index of attribute to calculate the mean for
     *@return the mean (double because a mean of integers will very likely be a double) 
     */
    public static double getAritmeticMiddleForAttribute( List<Feature> features, int attr ){
        return FeatureCollectionTools.getAritmeticMiddleForAttribute((Feature[])features.toArray(new Feature[0]), attr);
    }
    
    /**
     *@param featArray array of features to calculate the mean for
     *@param attr index of attribute to calculate the mean for
     *@return the mean (double because a mean of integers will very likely be a double) 
     */
    public static double getAritmeticMiddleForAttribute( Feature[] featArray, int attr ){
        double sumVals = 0;
        int numVals = 0;
        
        if (featArray.length==0) {
            logger.printWarning("no features in list - return value will be NAN!");
            return Double.NaN;
        }
        
        Feature f = featArray[0];
        FeatureSchema fs = f.getSchema();
        
        if ( FeatureCollectionTools.isAttributeTypeNumeric(fs.getAttributeType(attr)) ){
           
            //Iterator iter = features.iterator();
            Feature feat;
            double value;
            
            for (int i=0; i<featArray.length; i++){
                feat = featArray[i];
                value = ObjectComparator.getDoubleValue(feat.getAttribute(attr));
                
                sumVals += value;
                numVals ++;
            }
        }
        
        return (sumVals/(double)numVals);
    }
    
    /**
     * Calculates the center of mass for the gives features' geometries.
     * It's like getCentroid(), but puts out one center of mass for N features instead of N centers for N features.
     *@param features the features
     *@return the point, representing the center of mass
     */
    public static Geometry getCenterOfMass( Feature[] features ){
        double sumX = 0, sumY = 0;
        GeometryFactory gf = new GeometryFactory();  
        
        if (features==null || features.length==0) return null;
        
        Feature feat;
        
        for (int i=0; i<features.length; i++){
            feat = features[i];
            sumX += feat.getGeometry().getCoordinate().x;
            sumY += feat.getGeometry().getCoordinate().y;
        }
        
        double newX = sumX / (double)features.length;
        double newY = sumY / (double)features.length;
        
        return gf.createPoint(new Coordinate(newX, newY));
    }
    
    /**
     *@param features list of features
     *@param attr name of attribute
     *@return number of differnt values in the given features attributes or -1 if an error occurs
     */
    public static Set getSetOfDifferentAttributeValues( Feature[] features, String attr ){
        Feature f = features[0];
        FeatureSchema fs = f.getSchema();
        int attrInd = fs.getAttributeIndex(attr);
        return FeatureCollectionTools.getSetOfDifferentAttributeValues( features, attrInd );
    }
    
    /**
     *@param features list of features
     *@param attr index of attribute
     *@return number of differnt values in the given features attributes or -1 if an error occurs
     */
    public static Set<Object> getSetOfDifferentAttributeValues( Feature[] features, int attr ){
        Feature[] featArray = features;
        int numFeats = featArray.length;
        
        HashSet<Object> differentValues = new HashSet<Object>();
        Object val;
        
        for (int i=numFeats-1; i>=0; i--){
            
            val = featArray[i].getAttribute(attr);
            //if (!differentValues.contains(val))
                differentValues.add(val);
        }
        
        return differentValues;
    }
    
    /**
     *@param features list of features
     *@param attr name of attribute
     *@return number of differnt values in the given features attributes or -1 if an error occurs
     */
    public static int getNumOfDifferentAttributeValues( Feature[] features, String attr ){
        Feature f = features[0];
        FeatureSchema fs = f.getSchema();
        int attrInd = fs.getAttributeIndex(attr);
        return FeatureCollectionTools.getNumOfDifferentAttributeValues( features, attrInd );
    }
    
    /**
     *@param features list of features
     *@param attr index of attribute
     *@return number of differnt values in the given features attributes or -1 if an error occurs
     */
    public static int getNumOfDifferentAttributeValues( Feature[] features, int attr ){
        return FeatureCollectionTools.getSetOfDifferentAttributeValues(features,attr).size();
    }

    /**
     *@param features list of features to calculate the mode for
     *@param attr name of attribute to calculate the mode for
     *@return the mode 
     */
    public static Object getModusForAttribute( Feature[] features, String attr ){
        Feature f = features[0];
        FeatureSchema fs = f.getSchema();
        int attrInd = fs.getAttributeIndex(attr);
        return FeatureCollectionTools.getModusForAttribute( features, attrInd );
    }
    

    /**
     * Counts the number of appearances of each value of the given attributes within the given features.
     * (Somewhat like a histogram for each given attribute, but each histogram class is just a single value.)
     * @param features array of Features to count value appearances for 
     * @param attrs array of attribute indices of attributes to count value appearances for
     * @return Array of mappings of values to number of appearances 
     */
    public final static HashMap<Object,Integer>[] getValueAppearancesCount( Feature[] features, int[] attrs ){
        HashMap<Object,Integer>[] value2NumAppearanceMaps = new HashMap[attrs.length];
        
        for (int i=0; i<attrs.length; i++){
        	value2NumAppearanceMaps[i] = new HashMap<Object,Integer>();
        }
        
        Feature feat;
        Object value;
        
        for (int i=0; i<features.length; i++){
            feat = features[i];
            
            for (int attInd=0; attInd<attrs.length; attInd++){
            	value = feat.getAttribute(attrs[attInd]);
            	
            	if (!value2NumAppearanceMaps[attInd].containsKey(value)){
            		value2NumAppearanceMaps[attInd].put(value, 1);
            	} else {
            		value2NumAppearanceMaps[attInd].put(value, value2NumAppearanceMaps[attInd].get(value).intValue()+1);
            	}
            	
            }

        }
        
        return value2NumAppearanceMaps;
    }
    
    /**
     *@param features list of features to calculate the mode for
     *@param attr index of attribute to calculate the mode for
     *@return the mode 
     */
    public static Object getModusForAttribute( Feature[] features, int attr ){
        HashMap<Object,Integer> map = new HashMap<Object,Integer>();
        Object modus = null;
        int maxNr = 0;
        
        if (features==null || features.length==0) return null;
        
        Feature[] featArray = features;
        Feature feat;
        Object value;
        int occ, numFeats = featArray.length;
        
        for (int i=0; i<numFeats; i++){
            feat = featArray[i];

            value = feat.getAttribute(attr);
            
            if (value.getClass().getName().equals(String.class.getName())){
                ((String)value).trim();
            }
            
            if ( map.containsKey(value) ){
                occ = ((Integer)map.get(value)).intValue();
                occ += 1;
                map.remove(value);
            } else {
                occ = 1;
            }
            map.put(value,new Integer(occ));
            
            if (occ > maxNr){
                maxNr = occ;
                modus = value;
            }
        }
        
        return modus;
    }
    
    /**
     * deletes the given features from the map. It thereby creates an EditTransaction that
     * enables the user to undo the deletion.
     *@param features features to be deleted
     *@param context curr. PlugIn context
     */
    public static void deleteFeatures( List<Feature> features, PlugInContext context ){
        Map layer2FeatList = LayerTools.getLayer2FeatureMap(features, context);
        
        Layer[] layersWithFeatures = (Layer[])layer2FeatList.keySet().toArray(new Layer[0]);
        Feature[] selFeatsOfLayer;
        
        for ( int i=0; i<layersWithFeatures.length; i++ ){
            EditTransaction edtr = new EditTransaction(new ArrayList(), "delete features", layersWithFeatures[i], true, true, context.getLayerViewPanel());
            
            selFeatsOfLayer = (Feature[])((List)layer2FeatList.get(layersWithFeatures[i])).toArray(new Feature[0]);
            
            for (int j=0; j<selFeatsOfLayer.length; j++){
                edtr.deleteFeature(selFeatsOfLayer[j]);
            }
            
            edtr.commit();
            edtr.clearEnvelopeCaches();
        }
    }
    
    /**
     * "deep copys" the given Feature
     *@param feat the feature to be copied
     *@return copy of the feature
     */
    public static Feature copyFeature( Feature feat ){
        Feature newFeat = new BasicFeature(feat.getSchema());
        
        int numAttr = feat.getSchema().getAttributeCount();
        
        for ( int i=0; i<numAttr; i++ ){
            newFeat.setAttribute( feat.getSchema().getAttributeName(i), feat.getAttribute(i) );
        }
        
        newFeat.setGeometry((Geometry)feat.getGeometry());
        
        return newFeat;
    }
    
    /**
     * "deep copys" the given Feature and thereby sets the given feature schema.
     * The new FeatureSchema should have the same attribute names for copying. 
     * The new FeatureSchmema can have less or more attributes. 
     *@param feat the feature to be copied
     *@param newFs the new feature schema
     *@return copy of the feature
     */
    public static Feature copyFeatureAndSetFeatureSchema( Feature feat, FeatureSchema newFs ){
    	feat = feat.clone(true);
    	FeatureSchema fs = feat.getSchema();
        
        Feature newFeat = new BasicFeature(newFs);
        int numAttr = feat.getSchema().getAttributeCount();
        
        for ( int i=0; i<numAttr; i++ ){
        	String attrName = fs.getAttributeName(i);
        	if (newFs.hasAttribute(attrName)){
        		newFeat.setAttribute( fs.getAttributeName(i), feat.getAttribute(fs.getAttributeName(i)) );
        	}           
        }
        
        newFeat.setGeometry((Geometry)feat.getGeometry());
        
        return newFeat;
    }
    
    public String getUniqueAttributeName(String attr){
        return FeatureCollectionTools.getUniqueAttributeName( this.fc, attr );
    }
    
    public static String getUniqueAttributeName(FeatureCollection fc, String attr){
        FeatureSchema fs = fc.getFeatureSchema();
        String newName = new String(attr);
        String suffix = "";
        
        for ( int i=2; FeatureCollectionTools.attributeExistsInSchema(fs, newName+suffix); i++ ){
            suffix = " ("+i+")";
        }
        
        return newName+suffix;
    }
    
    protected static boolean attributeExistsInSchema(FeatureSchema fs, String attr){
        try {
            int index = fs.getAttributeIndex(attr);
            return (index>=0);
        } catch (RuntimeException e) {
            return false;
        }
    }
    
    public PirolFeatureCollection addAttributeToFeatureCollection( String attr, AttributeType at, Object defaultVal ){
        if (this.fc != null)
            return FeatureCollectionTools.addAttributeToFeatureCollection( this.fc, attr, at, defaultVal, true );
        return null;
    }
    
    public static boolean isAttributeTypeNumeric( AttributeType at ){
        return ( at.equals(AttributeType.DOUBLE) || at.equals(AttributeType.INTEGER) );
    }
    
    /**
     * Method to apply a given formula to the given, new attribute of the given featureCollection
     *@param oldFc old FeatureCollection that will be replaced by the new one
     *@param attrInfo information for the new attribute
     *@param formula the parsed formula
     *@return FeatureCollection containing the new attribute
     */
    public static PirolFeatureCollection applyFormulaToFeatureCollection( FeatureCollection oldFc, AttributeInfo attrInfo, FormulaValue formula, boolean clearOldFeatureCollection ){
        
        PirolFeatureCollection newFc = FeatureCollectionTools.addAttributeToFeatureCollection( oldFc, attrInfo, clearOldFeatureCollection );
        
        Feature[] features = (Feature[])newFc.getFeatures().toArray(new Feature[0]);
        Feature feat;
        int numFeats = features.length, attrInd = newFc.getFeatureSchema().getAttributeIndex(attrInfo.getUniqueAttributeName());
        
        for (int i=0; i<numFeats; i++){
            feat = features[i];
            feat.setAttribute(attrInd, new Double(formula.getValue(feat)));
            
            if (i%500 == 0)
                logger.printDebug("done: " + i);
        }
        return newFc;
    }
    
    public static PirolFeatureCollection addAttributeToFeatureCollection( FeatureCollection fc, AttributeInfo attrInfo ){
        return FeatureCollectionTools.addAttributeToFeatureCollection(fc, attrInfo.getUniqueAttributeName(), attrInfo.getAttributeType(), attrInfo.getNullValue(), true);
    }
    
    public static PirolFeatureCollection addAttributeToFeatureCollection( FeatureCollection fc, AttributeInfo attrInfo, boolean clearOldFeatureCollection ){
        return FeatureCollectionTools.addAttributeToFeatureCollection(fc, attrInfo.getUniqueAttributeName(), attrInfo.getAttributeType(), attrInfo.getNullValue(), clearOldFeatureCollection);
    }
    
    /**
     * Method to add a new attribute to an existing FeatureCollection. Since there is no "legal" way to
     * add an attribute to a FeatureCollection, this method creates (and returns) a new FeatureCollection with a new
     * FeatureSchema to do this operation. If a layer is to be manipulated the new FeatureCollection has to be set
     * as THE FeatureCollection for the layer...
     *@param fc the old feature collection
     *@param attr name of the new attribute
     *@param at type of the new attribute
     *@param defaultVal the initial value for the attribute, since we do not want NULL values in the attribute table
     *@param clearOldFeatureCollection if true the old feature collection will be erased to save RAM
     *@return new FeatureCollection with a new FeatureSchema including the new attribute
     */
    public static PirolFeatureCollection addAttributeToFeatureCollection( FeatureCollection fc, String attr, AttributeType at, Object defaultVal, boolean clearOldFeatureCollection ){
        FeatureSchema fs = (FeatureSchema)fc.getFeatureSchema().clone();
        fs.addAttribute(attr,at);
        
        MetaInformationHandler mih = new MetaInformationHandler(MetaInformationHandler.createPirolFeatureCollection(fc));
        
        PirolFeatureCollection newFc = MetaInformationHandler.createPirolFeatureCollection(new FeatureDataset(fs));
        
        if (mih.containsMetaInformation()){
            newFc.setMetaInformation(mih.getExistentMetaInformationMap());
        }
        mih = null;
        
        Feature feat, newFeat;
        
        if (at.equals(AttributeType.INTEGER)){
            if (Double.class.isInstance(defaultVal)){
                defaultVal = new Integer( ((Double)defaultVal).intValue() );
            }
        }
        
        Feature[] featArray = (Feature[])fc.getFeatures().toArray(new Feature[0]);
        
        if (clearOldFeatureCollection)
            fc.clear();        
        
        int featuresDone = 0;
        
        for ( int i=featArray.length-1; i>=0; i--, featuresDone++ ){
            feat = featArray[i];
            
            newFeat = FeatureCollectionTools.copyFeatureAndSetFeatureSchema(feat,fs);
            newFeat.setAttribute(attr, defaultVal);
            newFc.add(newFeat);
            
            if (i%10000 == 0 && i!=0){
                featArray = (Feature[])FeatureCollectionTools.resizeArray(featArray, featArray.length - featuresDone);
                featuresDone = 0;
                if (i%50000 == 0){
                    System.gc();
                }
            }
        }
        
        featArray = null;
        
        return newFc;
    }
    
    /**
     * Method to add a new attribute to an existing FeatureCollection. Since there is no "legal" way to
     * add an attribute to a FeatureCollection, this method creates (and returns) a new FeatureCollection with a new
     * FeatureSchema to do this operation. If a layer is to be manipulated the new FeatureCollection has to be set
     * as THE FeatureCollection for the layer...
     *@param fc the old feature collection
     *@param attr name of the new attribute
     *@param at type of the new attribute
     *@param defaultVal the initial value for the attribute, since we do not want NULL values in the attribute table
     *@return new FeatureCollection with a new FeatureSchema including the new attribute
     */
    public static PirolFeatureCollection addAttributeToFeatureCollection( FeatureCollection fc, String attr, AttributeType at, Object defaultVal ){
        return FeatureCollectionTools.addAttributeToFeatureCollection(fc, attr, at, defaultVal, true);
    }
    
    /**
     * Adds multiple attributes to the FeatureCollection
     *@param attributeInfos list containing the attributes (as AttributeInfo objects) to be added
     *@return a new FeatureCollection containing the old and the new attributes
     *
     *@see AttributeInfo
     */
    public PirolFeatureCollection addAttributesToFeatureCollection( List attributeInfos ){
        return FeatureCollectionTools.addAttributesToFeatureCollection(this.fc, attributeInfos);
    }
    
    /**
     * Adds multiple attributes to a FeatureCollection
     *@param fc the source feature collection
     *@param attributeInfos list containing the attributes (as AttributeInfo objects) to be added
     *@return a new FeatureCollection containing the old and the new attributes
     *
     *@see AttributeInfo
     */
    public static PirolFeatureCollection addAttributesToFeatureCollection( FeatureCollection fc, List<AttributeInfo> attributeInfos ){
        return FeatureCollectionTools.addAttributesToFeatureCollection(fc, (AttributeInfo[])attributeInfos.toArray(new AttributeInfo[0]));
    }
    
    public static PirolFeatureCollection addAttributesToFeatureCollection( FeatureCollection fc, AttributeInfo[] attributeInfos ){
    	return FeatureCollectionTools.addAttributesToFeatureCollection(fc, attributeInfos, true);
    }
    
    /**
     * Adds multiple attributes to a FeatureCollection
     *@param fc the source feature collection
     *@param attributeInfos array containing the attributes to be added
     *@param clearOriginalFeatureCollection set true, if you want to save RAM by 
     *clearing the original FeatureCollection, false if you still want to use it.
     *@return a new FeatureCollection containing the old and the new attributes
     */
    public static PirolFeatureCollection addAttributesToFeatureCollection( FeatureCollection fc, AttributeInfo[] attributeInfos, boolean clearOriginalFeatureCollection ){
        FeatureSchema fs = (FeatureSchema)fc.getFeatureSchema().clone();
        
        MetaInformationHandler mih = new MetaInformationHandler(MetaInformationHandler.createPirolFeatureCollection(fc));
        
        
        AttributeInfo attrInfo;
        
        for (int i=0; i<attributeInfos.length; i++){
            attrInfo = attributeInfos[i]; 
            fs.addAttribute(attrInfo.getUniqueAttributeName(), attrInfo.getAttributeType());
        }
        
        PirolFeatureCollection newFc = MetaInformationHandler.createPirolFeatureCollection(new FeatureDataset(fs));
        
        if (mih.containsMetaInformation()){
            newFc.setMetaInformation(mih.getExistentMetaInformationMap());
        }
        mih = null;
        
        Feature feat, newFeat;
        
        Feature[] featArray = (Feature[])fc.getFeatures().toArray(new Feature[0]);
        
        if (clearOriginalFeatureCollection)
        	fc.clear();
        
        int featuresDone = 0;
        
        for ( int i=featArray.length-1; i>=0; i--, featuresDone++ ){
            feat = featArray[i];
            
            newFeat = FeatureCollectionTools.copyFeatureAndSetFeatureSchema(feat,fs);
            

            for (int j=0; j<attributeInfos.length; j++){
                attrInfo = attributeInfos[j]; 
                newFeat.setAttribute(attrInfo.getUniqueAttributeName(), attrInfo.getNullValue());
            }
            
            newFc.add(newFeat);

            if (i%10000 == 0 && i!=0){
                featArray = (Feature[])FeatureCollectionTools.resizeArray(featArray, featArray.length - featuresDone);
                featuresDone = 0;
                if (i%50000 == 0){
                    System.gc();
                }
                logger.printDebug("adding attribute, features left to do " + i);
            }
        }
        featArray = null;
        
        return newFc;
    }

    /**
    * Reallocates an array with a new size, and copies the contents
    * of the old array to the new array.
    * @param oldArray  the old array, to be reallocated.
    * @param newSize   the new array size.
    * @return          A new array with the same contents.
    */
    public static Object resizeArray (Object oldArray, int newSize) {
       int oldSize = java.lang.reflect.Array.getLength(oldArray);
       Class elementType = oldArray.getClass().getComponentType();
       Object newArray = java.lang.reflect.Array.newInstance(
             elementType,newSize);
       int preserveLength = Math.min(oldSize,newSize);
       if (preserveLength > 0)
          System.arraycopy (oldArray,0,newArray,0,preserveLength);
       return newArray; 
    }
    
    public static double[] getMinMaxAttributeValue( Feature[] featArray, FeatureSchema fs, String attr ){
        double[] minmax = new double[]{ Double.MAX_VALUE, -Double.MAX_VALUE };
        
        
        if (fs.getAttributeType(attr) == AttributeType.INTEGER || fs.getAttributeType(attr) == AttributeType.DOUBLE){

            Feature feat;
            double value;
            
            for ( int i=featArray.length-1; i>=0; i-- ){
                feat = featArray[i];
                
                if (feat.getAttribute(attr) != null){
	                value = ObjectComparator.getDoubleValue(feat.getAttribute(attr));
	                
	                if (value<minmax[0]){
	                    minmax[0] = value;
	                }
	                if (value>minmax[1]){
	                    minmax[1] = value;
	                }
                } else {
                    FeatureCollectionTools.logger.printMinorError("skipped value (NULL), when checking min./max. values for Attribute " + attr);
                }
            }
        }
        
        return minmax;
    }
    
    
    public static double getSumAttributeValue( Feature[] featArray, FeatureSchema fs, String attr ){
        double sum = 0;
        
        if (fs.getAttributeType(attr) == AttributeType.INTEGER || fs.getAttributeType(attr) == AttributeType.DOUBLE){
            Feature feat;
            double value;
            
            for ( int i=featArray.length-1; i>=0; i-- ){
                feat = featArray[i];
                
                if (feat.getAttribute(attr) != null){
	                value = ObjectComparator.getDoubleValue(feat.getAttribute(attr));
	                sum = sum + value;
                } else {
                    FeatureCollectionTools.logger.printMinorError("skipped value (NULL), when checking sum values for Attribute " + attr);
                }
            }
        }
        
        return sum;
    }
    
    public static double[] getMinMaxAttributeValue( FeatureCollection features, String attr ){
        return FeatureCollectionTools.getMinMaxAttributeValue(FeatureCollectionTools.FeatureCollection2FeatureArray(features), features.getFeatureSchema(), attr);
    }
    
    
    /**
     * Converts a given FeatureCollection into an array of Feature, that can - by far - be faster iterated.
     *@param fc the feature Collection
     *@return an array of the features of the feature collection
     */
    public static Feature[] FeatureCollection2FeatureArray( FeatureCollection fc ){
        return (Feature[])fc.getFeatures().toArray(new Feature[0]);
    }
    
    /**
     * Converts a given list of features into an array of Feature, that can - by far - be faster iterated.
     *@param features list of features
     *@return an array of the features of the feature list
     */
    public static Feature[] FeatureCollection2FeatureArray( List<Feature> features ){
        return (Feature[])features.toArray(new Feature[0]);
    }
    
    /**
     * Extracts all points from an input feature and returns them as list of point features.
     * Note: for closed Geometry objects the start and end point are extracted - 
     * i.e. the points may be overlap.   
     * @param f
     * @param accountForRings doesn't add a point for the last coordinate if the geometry is a ring (i.e. first point equals last point) 
     * @return list of point features
     */
    public static ArrayList<Feature> convertToPointFeature(Feature f, boolean accountForRings){
    	GeometryFactory gf = new GeometryFactory(); 
    	ArrayList<Feature> points = new ArrayList<Feature>();    	
    	Coordinate[] coords = f.getGeometry().getCoordinates();  
    	int lastCoord = coords.length;
    	if (accountForRings){ 
    		if(coords[lastCoord-1].equals2D(coords[0])){
    			lastCoord = coords.length-1;
    		}
    	}
    	for (int i=0; i < lastCoord; i++) {
    			Point pt = gf.createPoint(coords[i]);
    			Feature fNew = copyFeature(f);
    			fNew.setGeometry(pt);
    		    points.add(fNew);
    		 }
    	return points;
    }
    
	/**
     * Sorts features according to unique attribute values into different lists (on list for each unique attribute). 
     * TODO: enable sorting for String values
     * @param features
     * @param idAttribute (must be Double or Integer)
     * @return Object[0]: an Array of ArrayLists containing features, Object[1]: an Array of int values containing the unique values 
     * 			used for sorting. Can return null if wrong AttributeType. E.g. use<br>
     * 			Object[] myReturns = FeatureCollectionTools.sortFeaturesIntoListsByAttributeValue(pointFeatures, idAttribute); <br>
	 *		    individualPts = (ArrayList[])myReturns[0]; <br>
	 *		    int[] uniqueValues = (int[])myReturns[1]; <br>
	 *		    Note that the order in which the lists are returned is not sorted!
     */
	public static Object[] sortFeaturesIntoListsByAttributeValue(FeatureCollection features,
			String idAttribute) {
		Object[] returnObject = new Object[2];
		ArrayList<Feature>[] objectsInClass = null;
		AttributeType atype = features.getFeatureSchema().getAttributeType(idAttribute);
		//-- only proceed if Id attribute has correct datatype
		if ((atype == AttributeType.DOUBLE) || (atype == AttributeType.INTEGER)){
			Feature[] fArray = FeatureCollectionTools.FeatureCollection2FeatureArray(features); 
			//-- retrieve the set of different individuals
			Set individuals = FeatureCollectionTools.getSetOfDifferentAttributeValues(fArray, idAttribute);
			int[] individualValues = new int[individuals.size()];
			//System.out.print("unique values: ");
			int i=0;
			for (Iterator iterator = individuals.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				int idval = -9998;
				if (atype == AttributeType.DOUBLE){
					idval = ((Double)object).intValue();
				}
				else if (atype == AttributeType.INTEGER){
					idval = ((Integer)object).intValue();
				}
				individualValues[i] = idval;
				i++;
				//System.out.print(idval + ", ");
			}
			System.out.println();
			returnObject[1] = individualValues;
			objectsInClass = new ArrayList[individuals.size()]; 
			//-- create the lists
			for (int j = 0; j < objectsInClass.length; j++) {
				objectsInClass[j] = new ArrayList<Feature>();
			}
			//-- sort all objects f
			for (Iterator iterator = features.iterator(); iterator.hasNext();) {
				Feature f = (Feature) iterator.next();
					int  id = -9999;
					if (atype == AttributeType.DOUBLE){
						Double val = (Double)f.getAttribute(idAttribute);
						id = val.intValue();
					}
					else if (atype == AttributeType.INTEGER){
						Integer val = (Integer)f.getAttribute(idAttribute);
						id = val.intValue();
					}
					//-- search if the ID fits to one of the values
					boolean found = false; int j =0;
					while(found == false){
						if(id == individualValues[j]){
							objectsInClass[j].add(f.clone(true));
							found = true;
						}
						//-- stop if all values have been compared
						j++;
						if((j == individualValues.length) && (found == false)){
							found = true;
							System.out.println("sortFeaturesIntoListsByAttributeValue: could not assign value to class; value: " + id);
						}
					}
			}
		}
		else{
			System.out.println("sortFeaturesIntoListsByAttributeValue: id AttributeType neither double nor int");
		}
		returnObject[0] = objectsInClass;
		return returnObject;
	}
	
	/**
	 * gets the value of an attribute; it checks if the attribute is of double or int type, otherwise NaN is returned. 
	 * @param f
	 * @param attributeName
	 * @return value as double
	 */
	public static double getNumericalAttributeValue(Feature f, String attributeName){
    	AttributeType atype = f.getSchema().getAttributeType(attributeName);
		if ((atype == AttributeType.DOUBLE) || (atype == AttributeType.INTEGER)){
			double value = 0;
	    	if (atype == AttributeType.DOUBLE){
				Double val = (Double)f.getAttribute(attributeName);
				value = val.doubleValue();
	    	}
	    	else if(atype == AttributeType.INTEGER){
				Integer val = (Integer)f.getAttribute(attributeName);
				value = val.doubleValue();
	    	}	
			return value;
		}
		else{
			return Double.NaN;	
		}
	}
	
    /**
     * Sorts a list of features according to the values of a attribute. If values are similar the feature
     * are ordered the same way as in the input list.
     * TODO: this method has been tested only briefly (not exhaustively)
     * @param features
     * @param attributeNameForSorting, attribute needs to be either Integer or Double 
     * @return list of sorted features; the smallest value will be first in the list.
     */
    public static ArrayList<Feature> sortFeatureListByAttributeBeginWithSmallest(
			List<Feature> features, String attributeNameForSorting) {
    	ArrayList<Feature> sortedFeatureList = new ArrayList<Feature>();
    	ArrayList<Double> sortedValueList = new ArrayList<Double>(); // used to speed up sorting so attribute values 
    																 // need not to be derived every time
    	int i = 0;
    	boolean first = true; 
    	for (Iterator iterator = features.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			if(first){
				//-- just add the first feature
				sortedFeatureList.add(f.clone(true));
				double valuef = FeatureCollectionTools.getNumericalAttributeValue(f, attributeNameForSorting);
				sortedValueList.add(valuef);
				first = false;
			}
			else{
				//-- get value
				double valuef = FeatureCollectionTools.getNumericalAttributeValue(f, attributeNameForSorting);
				//-- parse the existing list
				boolean isLarger = true;
				int j = 0;
				while(isLarger){
					//-- to speed up things (i.e. avoid cumbersome value derivation), use the sortedValueList instead
					double valueFtemp = sortedValueList.get(j);
					if(valuef >= valueFtemp){
						//-- everything is fine, keep searching
					}
					else{
						//-- valuef is now smaller, squeeze it in right before ftemp (i.e. the same position)
						sortedFeatureList.add(j,f.clone(true));
						sortedValueList.add(j,valuef);
						//-- end the search, and sort the next item from "features"
						isLarger = false;
					}
					//-- if we are already at the end, then we need to insert the feature too
					if ( j+1 == sortedFeatureList.size()){
						sortedFeatureList.add(f.clone(true));
						sortedValueList.add(valuef);
						//-- end the search, and sort the next item from "features"
						isLarger = false;
					}
					j++;
				}//-- end while
			}
			i++;
		}//-- end for
		return sortedFeatureList;
	}
}
