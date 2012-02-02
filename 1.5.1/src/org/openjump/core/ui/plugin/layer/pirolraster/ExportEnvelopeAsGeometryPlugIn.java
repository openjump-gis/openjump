/*
 * Created on 09.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: ExportEnvelopeAsGeometryPlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.apitools.objecttyperoles.RoleOutline;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * PlugIn to export the bounding box of the RasterImageLayer as a geometry layer, so 
 * it can be changed, transformed to a fence and be re-applied to the RasterImage.<br>
 * This enables all geometry operations for RasterImages. 
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class ExportEnvelopeAsGeometryPlugIn extends AbstractPlugIn {
    
    protected static FeatureSchema defaultSchema = null;

    public ExportEnvelopeAsGeometryPlugIn() {
        //super(new PersonalLogger(DebugUserIds.OLE));
        
        if (ExportEnvelopeAsGeometryPlugIn.defaultSchema==null){
            ExportEnvelopeAsGeometryPlugIn.defaultSchema = new FeatureSchema();
            
            ExportEnvelopeAsGeometryPlugIn.defaultSchema.addAttribute("geometry", AttributeType.GEOMETRY);
        }
    }

    /**
     *@inheritDoc
     */
    public String getIconString() {
        return null;
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExportEnvelopeAsGeometryPlugIn.Export-Envelope-As-Geometry");
    }
    
    /**
     *@inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);
        
        if (rLayer==null){
            context.getWorkbenchFrame().warnUser(I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return false;
        }
        
        Geometry geom = rLayer.getEnvelopeAsGeometry();
        
        if (geom==null){
            return false;
        }
        
        FeatureCollection newFeaturecollection = new FeatureDataset((FeatureSchema)ExportEnvelopeAsGeometryPlugIn.defaultSchema.clone());
        
        BasicFeature feature = new BasicFeature((FeatureSchema)ExportEnvelopeAsGeometryPlugIn.defaultSchema.clone());
        
        feature.setAttribute("geometry", geom);
        
        newFeaturecollection.add(feature);
        
        LayerTools.addStandardResultLayer(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExportEnvelopeAsGeometryPlugIn.Geometry") + "-" + rLayer.getName(), newFeaturecollection, context, new RoleOutline() );
        
        return false;
    }

}
