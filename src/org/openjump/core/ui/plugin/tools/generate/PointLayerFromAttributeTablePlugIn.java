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

/*****************************************************
 * created:  		4.Sept.2012
 * last modified:			
 * 					
 * 
 * @author sstein
 * 
 * description: Plugin that extends line segments by closing the line towards a geometry 
 *              of another layer with polygons or lines using the shortest distance 
 *              connection (i.e. point-line distance)
 * 	
 *****************************************************/

package org.openjump.core.ui.plugin.tools.generate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;

import org.openjump.core.apitools.FeatureSchemaTools;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
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
 * @description: creates a new point layer from a attribute table where 
 * two attributes are used as coordinates.   
 *	
 * @author sstein
 *
 **/
public class PointLayerFromAttributeTablePlugIn extends AbstractThreadedUiPlugIn{

    private String sSidebar =				I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.descriptiontext");   
    private final String sLAYER = 			I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.Layer-with-attribute-table");
    private final String sXCoordAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.select-attribute-with-East-coordinate");
    private final String sYCoordAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.select-attribute-with-North-coordinate");
    private final String sZCoordAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.select-attribute-with-Height");
    private final String sHasZCoord = 		I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.data-have-a-z-coordinate-/-height-value");
    private final String sPointsFrom =		I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.Points-from");
    
    private FeatureCollection inputFC = null;
    private Layer inputLayer= null;

    private String selXAttribute = "";
    private String selYAttribute = "";
    private String selZAttribute = "";
    private boolean hasHeight = false;
    
    private MultiInputDialog dialog;
    private JCheckBox zCheckBox = null;
    private JComboBox Z_attributeBox = null;
    
    private PlugInContext context = null;
        
	public String getName() {
		return I18N
				.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn") + "...";
	}
	
    public void initialize(PlugInContext context) throws Exception {
	        context.getFeatureInstaller().addMainMenuItem(
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE}, 	//menu path
	                this,
	                new JMenuItem( this.getName(), null),
	                createEnableCheck(context.getWorkbenchContext()), -1);
	}
	        
	public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
	            EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
	            return new MultiEnableCheck()
	                .add(checkFactory.createTaskWindowMustBeActiveCheck())
	                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
	}
    
	public boolean execute(PlugInContext context) throws Exception{
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes.
        initDialog(context);
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
	    	System.gc(); //flush garbage collector
	    	this.context = context;
	    	monitor.allowCancellationRequests();
	    	int numTransfers = this.inputFC.size();
	    	int i = 0;
	    	int dXCoord = inputFC.getFeatureSchema().getAttributeIndex(this.selXAttribute);
	    	int dYCoord = inputFC.getFeatureSchema().getAttributeIndex(this.selYAttribute);
	    	int dZCoord = 0;
	    	if(hasHeight){
	    		dZCoord = inputFC.getFeatureSchema().getAttributeIndex(this.selZAttribute);
	    	}
	    	
	    	FeatureCollection resultFC = new FeatureDataset((FeatureSchema)inputFC.getFeatureSchema().clone());
	    	GeometryFactory gf = new GeometryFactory();
	    	
	    	for (Iterator iterator = inputFC.iterator(); iterator.hasNext();) {
				Feature origFeature = (Feature) iterator.next();
		    	monitor.report(i, numTransfers, I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.items-processed"));	

		    	double xc = origFeature.getDouble(dXCoord);
		    	double yc = origFeature.getDouble(dYCoord);
		    	
		    	Geometry pt = gf.createGeometryCollection(null);
		    	if(hasHeight == false){
		    		pt = gf.createPoint(new Coordinate(xc,yc));
		    	}
		    	else{
			    	double zc = origFeature.getDouble(dZCoord);
		    		pt = gf.createPoint(new Coordinate(xc,yc,zc));
		    	}
		    	Feature newFeature = origFeature.clone(true);
		    	newFeature.setGeometry(pt);
		    	resultFC.add(newFeature);
		    	i++; //count feature processing (next transfer)
			}
	    	
	    	// show results
	        if(resultFC.size() > 0){
	        	context.addLayer(StandardCategoryNames.RESULT, sPointsFrom + " " + inputLayer.getName(), resultFC);
	        }
	    	//--
	        System.gc();    		
    	}

	private void initDialog(PlugInContext context) {
		JComboBox layerComboBoxLayerSelection = null;
		
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.Create-Point-Layer"), true);
        dialog.setSideBarDescription(sSidebar);
        
        	layerComboBoxLayerSelection = dialog.addLayerComboBox(this.sLAYER, context.getCandidateLayer(0), null, context.getLayerManager());
          	
            List listNumAttributesTransfers = FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(context.getCandidateLayer(0));
            Object valAttribute = listNumAttributesTransfers.size()>0?listNumAttributesTransfers.iterator().next():null;
            final JComboBox X_attributeBox = dialog.addComboBox(this.sXCoordAttrib, valAttribute, listNumAttributesTransfers, this.sXCoordAttrib);
            if (listNumAttributesTransfers.size() == 0) X_attributeBox.setEnabled(false);

            final JComboBox Y_attributeBox = dialog.addComboBox(this.sYCoordAttrib, valAttribute, listNumAttributesTransfers, this.sYCoordAttrib);
            if (listNumAttributesTransfers.size() == 0) Y_attributeBox.setEnabled(false);

            dialog.addSeparator();
            
            zCheckBox = dialog.addCheckBox(sHasZCoord, hasHeight);
            
            Z_attributeBox = dialog.addComboBox(this.sZCoordAttrib, valAttribute, listNumAttributesTransfers, this.sZCoordAttrib);
            if (listNumAttributesTransfers.size() == 0) Z_attributeBox.setEnabled(false);
            if (this.zCheckBox.isSelected() == false) Z_attributeBox.setEnabled(false);
        
        zCheckBox.addActionListener(new ActionListener() {
 	        public void actionPerformed(ActionEvent e) {
 	            updateControls();
 	        }}); 
            
        dialog.getComboBox(this.sLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List list = getFieldsFromLayerWithoutGeometryAndStringTransfer();
                if (list.size() == 0) {
                	X_attributeBox.setModel(new DefaultComboBoxModel(new String[0]));
                	X_attributeBox.setEnabled(false);
                	Y_attributeBox.setModel(new DefaultComboBoxModel(new String[0]));
                	Y_attributeBox.setEnabled(false);
                	Z_attributeBox.setModel(new DefaultComboBoxModel(new String[0]));
                	Z_attributeBox.setEnabled(false);
                }
                else {
                	X_attributeBox.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
                	X_attributeBox.setEnabled(true);
                	Y_attributeBox.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
                	Y_attributeBox.setEnabled(true);
                	Z_attributeBox.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
                	Z_attributeBox.setEnabled(true);
                	if(hasHeight == false){
                    	Z_attributeBox.setEnabled(false);
                	}
                }
            }
        });        
       
        GUIUtil.centreOnWindow(dialog);
    }
	
	private void updateControls() {
		//System.out.print("process update method: ");
		if (this.zCheckBox.isSelected()){
			this.Z_attributeBox.setEnabled(true);
			this.hasHeight = true;
		}
		else{
			this.Z_attributeBox.setEnabled(false);
			this.hasHeight = false;
		}
	}
    private void getDialogValues(MultiInputDialog dialog) {
    	this.inputLayer =  dialog.getLayer(this.sLAYER);
        this.selXAttribute = dialog.getText(this.sXCoordAttrib);
        this.selYAttribute = dialog.getText(this.sYCoordAttrib);
        this.hasHeight = dialog.getBoolean(sHasZCoord);
        if(this.hasHeight){
        	this.selZAttribute = dialog.getText(this.sZCoordAttrib);
        }
        
    	this.inputFC= this.inputLayer.getFeatureCollectionWrapper(); 
      }

    private List getFieldsFromLayerWithoutGeometryAndStringTransfer() {
        return FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(dialog.getLayer(this.sLAYER));
    }
	
}