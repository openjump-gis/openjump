// license Licence CeCILL http://www.cecill.info/

package org.openjump.core.ui.plugin.edittoolbox.cursortools;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.PolygonTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
* This cursor tool is installed by CutPoygonSIGLEPlugIn
* To use it, select one or several Polygon a in layer A, draw a Polygon b by
* hand, every selected polygon a will be changed into a - b.
*
* @author ERWAN BOCHER Laboratoire RESO UMR CNRS 6590 et Olivier Bonnefont
* @see <a href="http://url www.projet-sigle.org">projet-sigle</a>
* @version 2006-05-18
*/
public class CutPolygonTool extends PolygonTool {
		  
    final static String sCookieCut = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.CutPolygonTool.Create-Cookie-Cut");
    		
	Geometry geomSelected = null;
	Geometry geomDraw = null;
	Geometry newGeomIntersect = null;
	Geometry newGeomDiff = null;
	
	private FeatureDrawingUtil featureDrawingUtil;
					
	protected CutPolygonTool(FeatureDrawingUtil featureDrawingUtil) {
	    this.featureDrawingUtil = featureDrawingUtil;
	}

	public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy) {
		FeatureDrawingUtil featureDrawingUtil =
				new FeatureDrawingUtil(layerNamePanelProxy);

			return featureDrawingUtil.prepare(
				new CutPolygonTool(featureDrawingUtil),
				true);
	}

	// Get the icon for the plugin. The image is located in the same directory
	// as the class
	public Icon getIcon() {
	    return new ImageIcon(getClass().getResource("CutPolygon.gif"));
    }

	public String getName(){
	    return sCookieCut;
	}

	// The user finishes to draw the polygon with the mouse
	protected void gestureFinished() throws Exception {
	    //Ici on va chercher la couche
	    WorkbenchContext context = getWorkbench().getContext();
	    this.reportNothingToUndoYet();
	    LayerNamePanel layernamepanel = context.getLayerNamePanel();
		
	    Layer[] selectedLayers = layernamepanel.getSelectedLayers();
	    
	    // Conditions to use the CutPolygon function
	    
	    if (selectedLayers.length == 0){
			JOptionPane.showMessageDialog(null, I18N.getMessage("com.vividsolutions.jump.workbench.plugin.At-least-one-layer-must-be-selected", new Object[]{1}),  I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), JOptionPane.INFORMATION_MESSAGE);
	    }
	    
	    else if (selectedLayers.length > 1) {
	        JOptionPane.showMessageDialog(null,  I18N.getMessage("com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-have-selected-items", new Object[]{1}),  I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), JOptionPane.INFORMATION_MESSAGE);
	    }
	    
	    else {
	        Layer activeLayer = (Layer) selectedLayers[0];
	    	
	        Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager() .getFeaturesWithSelectedItems(activeLayer);
	        if (activeLayer.isEditable()) {
	            if (!checkPolygon()) {
				    return;
	            }
	            else  {
	    		    for (Iterator k = selectedFeatures.iterator(); k.hasNext();) {
						Feature featureSelected = (Feature) k.next();
						geomSelected = featureSelected.getGeometry();
						geomDraw = getPolygon();
                        
						if(!getPolygon().intersects(geomSelected)) {
						}
	    			    else {
							if ((geomSelected instanceof Polygon)||(geomSelected instanceof MultiPolygon)){
								newGeomIntersect = geomSelected.intersection(geomDraw);
								newGeomDiff = geomSelected.difference(newGeomIntersect);
								BasicFeature featureIntersect = new BasicFeature(activeLayer.getFeatureCollectionWrapper().getFeatureSchema());
								BasicFeature featureDiff = new BasicFeature(activeLayer.getFeatureCollectionWrapper().getFeatureSchema());
								FeatureUtil.copyAttributes(featureSelected, featureIntersect);
								featureIntersect.setGeometry(newGeomIntersect);
								FeatureUtil.copyAttributes(featureSelected, featureDiff);	    		        	
								featureDiff.setGeometry(newGeomDiff);
								
						        FeatureCollection features = activeLayer.getFeatureCollectionWrapper().getUltimateWrappee();
						        EditTransaction edtr = new EditTransaction(new ArrayList(), "cut polygon", activeLayer, true, true, context.getLayerViewPanel());
						        edtr.deleteFeature(featureSelected);
						        edtr.createFeature(featureIntersect);
						        edtr.createFeature(featureDiff);
						        edtr.commit();
					            edtr.clearEnvelopeCaches();
							}
	    			    }
					}
	            }
			}
	        else {
	           	JOptionPane.showMessageDialog(null,  I18N.get("ui.SchemaPanel.layer-must-be-editable"),  I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), JOptionPane.INFORMATION_MESSAGE);
	        }
	    }
	}
	
	protected boolean isRollingBackInvalidEdits(WorkbenchContext context) {
		return context.getWorkbench().getBlackboard()
				.get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
	}
}