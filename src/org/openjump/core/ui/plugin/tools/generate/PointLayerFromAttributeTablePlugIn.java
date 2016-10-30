/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This class implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 * Stefan Steiniger
 * perriger@gmx.de
 */
package org.openjump.core.ui.plugin.tools.generate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/**
 * Creates a new point layer from a attribute table where
 * two attributes are used as coordinates.   
 *	
 * @author sstein
 */
public class PointLayerFromAttributeTablePlugIn extends AbstractThreadedUiPlugIn{

    private final String sSidebar =		    I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.descriptiontext");
    private final String sLAYER = 			I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.Layer-with-attribute-table");
    private final String sXCoordAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.select-attribute-with-East-coordinate");
    private final String sYCoordAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.select-attribute-with-North-coordinate");
    private final String sZCoordAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.select-attribute-with-Height");
    private final String sHasZCoord = 		I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.data-have-a-z-coordinate-/-height-value");
    private final String sPointsFrom =		I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.Points-from");
    
    private Layer inputLayer = null;

    private String selXAttribute = "";
    private String selYAttribute = "";
    private String selZAttribute = "";
    private boolean hasHeight = false;

	public String getName() {
		return I18N
				.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn") + "...";
	}
	
    public void initialize(PlugInContext context) throws Exception {
	        context.getFeatureInstaller().addMainMenuPlugin(
	        		this,
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE},
	                this.getName() + "...", false, null,
	                createEnableCheck(context.getWorkbenchContext()),-1);
	}
	        
	public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
	            EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
	            return new MultiEnableCheck()
	                .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())
	                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
	}
    
	public boolean execute(PlugInContext context) throws Exception{
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes.
		MultiInputDialog dialog = initDialog(context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        else{
        	this.getDialogValues(dialog); 
        }
        return true;	    
	}
    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{            		
		monitor.allowCancellationRequests();
		FeatureCollection inputFC = inputLayer.getFeatureCollectionWrapper();

	    int dXCoord = inputFC.getFeatureSchema().getAttributeIndex(selXAttribute);
	    int dYCoord = inputFC.getFeatureSchema().getAttributeIndex(selYAttribute);
	    int dZCoord = 0;
	    if(hasHeight){
			dZCoord = inputFC.getFeatureSchema().getAttributeIndex(selZAttribute);
		}
	    	
		FeatureCollection resultFC = new FeatureDataset(inputFC.getFeatureSchema().clone());
		GeometryFactory gf = new GeometryFactory();

		int i = 0;
		int numTransfers = inputFC.size();
		for (Feature origFeature : inputFC.getFeatures()) {
			monitor.report(i, numTransfers, I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.items-processed"));
			if (origFeature.getAttribute(dXCoord) == null || origFeature.getAttribute(dYCoord) == null) {
				continue;
			}
			double xc = ((Number)origFeature.getAttribute(dXCoord)).doubleValue();
			double yc = ((Number)origFeature.getAttribute(dYCoord)).doubleValue();
			Coordinate coord = new Coordinate(xc, yc);
			if (hasHeight && origFeature.getAttribute(dZCoord) != null) {
				coord.z = ((Number)origFeature.getAttribute(dZCoord)).doubleValue();
			}
			Geometry pt = gf.createPoint(coord);
			Feature newFeature = origFeature.clone(true);
			newFeature.setGeometry(pt);
			resultFC.add(newFeature);
			i++; //count feature processing (next transfer)
		}

		// show results
		if(resultFC.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, sPointsFrom + " " + inputLayer.getName(), resultFC);
		}
	}

	private MultiInputDialog initDialog(final PlugInContext context) {

		MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(),
				I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.Create-Point-Layer"), true);
        dialog.setSideBarDescription(sSidebar);

		dialog.addLayerComboBox(sLAYER, context.getCandidateLayer(0), null, context.getLayerManager());
          	
		List<String> attributes = AttributeTypeFilter.NUMERIC_FILTER.filter(context.getCandidateLayer(0));
		String valAttribute = attributes.size()>0?attributes.get(0):null;
		final JComboBox<String> X_attributeBox = dialog.addComboBox(this.sXCoordAttrib, valAttribute, attributes, this.sXCoordAttrib);
		if (attributes.size() == 0) X_attributeBox.setEnabled(false);

		final JComboBox<String> Y_attributeBox = dialog.addComboBox(this.sYCoordAttrib, valAttribute, attributes, this.sYCoordAttrib);
		if (attributes.size() == 0) Y_attributeBox.setEnabled(false);

		dialog.addSeparator();
            
		final JCheckBox zCheckBox = dialog.addCheckBox(sHasZCoord, hasHeight);
            
		final JComboBox<String> Z_attributeBox = dialog.addComboBox(this.sZCoordAttrib, valAttribute, attributes, this.sZCoordAttrib);
		if (attributes.size() == 0) Z_attributeBox.setEnabled(false);
		if (!zCheckBox.isSelected()) Z_attributeBox.setEnabled(false);
        
        zCheckBox.addActionListener(new ActionListener() {
 	        public void actionPerformed(ActionEvent e) {
				Z_attributeBox.setEnabled(zCheckBox.isSelected());
				hasHeight = zCheckBox.isSelected();
 	        }});
            
        dialog.getComboBox(this.sLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				List<String> list = AttributeTypeFilter.NUMERIC_FILTER.filter(context.getCandidateLayer(0));
                if (list.size() == 0) {
                	X_attributeBox.setModel(new DefaultComboBoxModel<>(new String[0]));
                	X_attributeBox.setEnabled(false);
                	Y_attributeBox.setModel(new DefaultComboBoxModel<>(new String[0]));
                	Y_attributeBox.setEnabled(false);
                	Z_attributeBox.setModel(new DefaultComboBoxModel<>(new String[0]));
                	Z_attributeBox.setEnabled(false);
                }
                else {
                	X_attributeBox.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
                	X_attributeBox.setEnabled(true);
                	Y_attributeBox.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
                	Y_attributeBox.setEnabled(true);
                	Z_attributeBox.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
                	Z_attributeBox.setEnabled(true);
                	if(!hasHeight){
                    	Z_attributeBox.setEnabled(false);
                	}
                }
            }
        });        
       
        GUIUtil.centreOnWindow(dialog);
		return dialog;
    }

    private void getDialogValues(MultiInputDialog dialog) {
		inputLayer    =  dialog.getLayer(sLAYER);
		selXAttribute = dialog.getText(sXCoordAttrib);
        selYAttribute = dialog.getText(sYCoordAttrib);
        hasHeight = dialog.getBoolean(sHasZCoord);
        if(hasHeight){
        	selZAttribute = dialog.getText(sZCoordAttrib);
        }
	}

}