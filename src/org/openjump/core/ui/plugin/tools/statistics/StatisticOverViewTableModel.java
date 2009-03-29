/*
 * Created on 30.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2451 $
 *  $Date: 2006-09-12 15:07:53 +0200 (Di, 12 Sep 2006) $
 *  $Id: StatisticOverViewTableModel.java 2451 2006-09-12 13:07:53Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.tools.statistics;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.core.apitools.comparisonandsorting.ObjectComparator;
import org.openjump.core.apitools.tables.StandardPirolTableModel;
import org.openjump.core.attributeoperations.AttributeOp;
import org.openjump.core.attributeoperations.statistics.CorrelationCoefficients;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;

import de.fho.jump.pirol.utilities.attributes.AttributeInfo;

/**
 * Table model to show a quick, statistical overview for a layer (or selection)
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2451 $
 * modified: [sstein] 16.Feb.2009
 */
public class StatisticOverViewTableModel  extends StandardPirolTableModel {
    
    private static final long serialVersionUID = -4961732734422876267L;
    
    protected Class[] colClasses = new Class[]{String.class, String.class, Double.class, String.class, Double.class, Double.class, Double.class};
    protected Feature[] features = null;

    public StatisticOverViewTableModel(Feature[] features) {
        super(new String[]{ 
        		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.attribute"), 
        		I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-type"), 
        		I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum"), 
        		I18N.get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewTableModel.mean-mode"), 
        		I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum"), 
        		I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.standard-dev"),
        		I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.sum")});
        this.features = features;
        
        this.setupTable();
    }
    
    protected void setupTable(){
        Feature feat = this.features[0];
        FeatureSchema fs = feat.getSchema();
        
        AttributeInfo[] attrInfos = AttributeInfo.schema2AttributeInfoArray(fs);
        
        // we don't use the Geometry
        String[] attrToWorkWith = new String[attrInfos.length-1];
        int saveAttrIndex = 0;
        
        for (int i=0; i<attrInfos.length; i++){
            if (!attrInfos[i].getAttributeType().equals(AttributeType.GEOMETRY)){
                attrToWorkWith[saveAttrIndex] = attrInfos[i].getAttributeName();
                saveAttrIndex++;
            }
        }
        
        Object[] meansModes = FeatureCollectionTools.getMeanOrModeForAttributes(features, attrToWorkWith);
        double[] minMax;
        double deviation, sum;
        saveAttrIndex = 0;
        
        for (int i=0; i<attrInfos.length; i++){
            if (attrInfos[i].getAttributeType().equals(AttributeType.GEOMETRY)) continue;
            if (FeatureCollectionTools.isAttributeTypeNumeric(attrInfos[i].getAttributeType())){
                // numeric
                minMax = FeatureCollectionTools.getMinMaxAttributeValue(this.features, fs, attrInfos[i].getAttributeName());
                deviation = CorrelationCoefficients.getDeviation(this.features, attrInfos[i].getAttributeName(), ObjectComparator.getDoubleValue(meansModes[saveAttrIndex]));
                //sum = FeatureCollectionTools.getSumAttributeValue(this.features, fs, attrInfos[i].getAttributeName());
                sum = AttributeOp.evaluateAttributes(AttributeOp.SUM, this.features, attrInfos[i].getAttributeName());
                this.addRow(attrInfos[i].getAttributeName(), attrInfos[i].getAttributeType(), new Double(minMax[0]), meansModes[saveAttrIndex], new Double(minMax[1]), new Double(deviation), new Double(sum) );
            } else {
                // non numeric
                this.addRow(attrInfos[i].getAttributeName(), attrInfos[i].getAttributeType(), null, meansModes[saveAttrIndex], null, null, null );
            }
            saveAttrIndex++;
        }

    }

    protected void addRow(String attrName, AttributeType type, Double minVal, Object mean, Double maxVal, Double deviation, Double sum){
        this.addRow(new Object[]{attrName, type, minVal, mean, maxVal, deviation, sum});
    }
    

    /**
     *@param rowIndex row index for cell
     *@param columnIndex column index for cell
     *@return always false, since we just want to show information
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Class getColumnClass(int columnIndex) {
        return this.colClasses[columnIndex];
    }
}
