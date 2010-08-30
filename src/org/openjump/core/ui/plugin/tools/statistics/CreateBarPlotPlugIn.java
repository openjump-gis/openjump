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
/***********************************************
 * created on 		19.10.2007
 * last modified: 	
 * 
 * author:			sstein
 * 
 * description:
 * 
 * 
 ***********************************************/
package org.openjump.core.ui.plugin.tools.statistics;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;

import org.openjump.core.apitools.FeatureSchemaTools;
import org.openjump.core.ui.plot.Plot2DPanelOJ;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class CreateBarPlotPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

    private String sBarPlot = "Bar-Plot";
    private String sNthfeature = "n-th feature";
    
    private MultiInputDialog dialog;
    
    private String CLAYER = "select layer";
    private String ATTRIBUTE = "select attribute";
    private Layer selLayer = null; 
    private FeatureCollection fc = null;
    private String selAttribute = null;
    private String sName = "Create Bar Plot";
    private String sWrongDataType = "Wrong datatype of chosen attribute";
    
	File selFile = null;
	
    /**
     * this method is called on the startup by JUMP/OpenJUMP.
     * We set here the menu entry for calling the function.
     */
	public void initialize(PlugInContext context) throws Exception {
		
		ATTRIBUTE = GenericNames.SELECT_ATTRIBUTE;
		CLAYER = GenericNames.SELECT_LAYER;
		sBarPlot = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Bar-Plot");
		sNthfeature = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.n-th-feature");
		sName = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Create-Bar-Plot");
		sWrongDataType = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Wrong-datatype-of-chosen-attribute");
		
		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
		featureInstaller.addMainMenuItem(
				this,                               //exe
				new String[] {MenuNames.TOOLS, MenuNames.STATISTICS, MenuNames.PLOT },     //menu path
				this.sName + "...", //name methode .getName recieved by AbstractPlugIn 
				false,          //checkbox
				null,           //icon
				createEnableCheck(context.getWorkbenchContext())); //enable check   

	}
    
    /**
     * This method is used to define when the menu entry is activated or
     * disabled. In this example we allow the menu entry to be usable only
     * if one layer exists.
     * @param workbenchContext
     * @return
     */
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                        .add(checkFactory.createTaskWindowMustBeActiveCheck());
    }
    
    /**
     * this function is called by JUMP/OpenJUMP if one clicks on the menu entry.
     * It is called before the "run" method and useful to do all the GUI /user-input things
     * In this example we call two additional methods {@link #setDialogValues(MultiInputDialog, PlugInContext)}
     * and {@link #getDialogValues(MultiInputDialog)} to obtain the Layer and the buffer radius by the user. 
     */
    public boolean execute(PlugInContext context) throws Exception{
    	
        this.reportNothingToUndoYet(context);         
        
            dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), sName, true);
            this.setDialogValues(dialog, context);
            GUIUtil.centreOnWindow(dialog);
            dialog.setVisible(true);
            if (! dialog.wasOKPressed()) { return false; }
            this.getDialogValues(dialog);
            
        return true;
    }
    
	public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        createPlot(context, this.selLayer);
		
	}
    
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
      {
        dialog.addLayerComboBox(CLAYER, context.getCandidateLayer(0), context.getLayerManager());
        
        List list = FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(context.getCandidateLayer(0));
        Object val = list.size()>0?list.iterator().next():null;
        final JComboBox jcb_attribute = dialog.addComboBox(ATTRIBUTE, val, list, ATTRIBUTE);
        if (list.size() == 0) jcb_attribute.setEnabled(false);        
        //dialog.addIntegerField(T2, this.ranges, 6, T2);
        
        dialog.getComboBox(CLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List list = getFieldsFromLayerWithoutGeometryAndString();
                if (list.size() == 0) {
                    jcb_attribute.setModel(new DefaultComboBoxModel(new String[0]));
                    jcb_attribute.setEnabled(false);
                }
                jcb_attribute.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
            }
        });        
      }

    private void getDialogValues(MultiInputDialog dialog) {
        //this.itemlayer = dialog.getLayer(this.CLAYER);
        this.selLayer = dialog.getLayer(CLAYER);
        this.fc = this.selLayer.getFeatureCollectionWrapper();
        this.selAttribute = dialog.getText(ATTRIBUTE);
      }
    
    private boolean createPlot(final PlugInContext context, Layer selLayer) throws Exception {
        
        FeatureSchema fs = this.fc.getFeatureSchema();
        AttributeType type = null;
        if ((fs.getAttributeType(this.selAttribute) == AttributeType.DOUBLE) || 
                (fs.getAttributeType(this.selAttribute) == AttributeType.INTEGER)){
            //-- move on
            type = fs.getAttributeType(this.selAttribute);
        }
        else{
            //System.out.println("CreateBarPlotPlugIn: wrong datatype of chosen attribute");
			context.getWorkbenchFrame().warnUser(sWrongDataType);
            return false;
        }
        
        double[] data = new double[this.fc.size()];
        int[] fID = new int[this.fc.size()];
        int i=0;
        for (Iterator iter = fc.iterator(); iter.hasNext();) {
            Feature f = (Feature) iter.next();
            fID[i] = f.getID();
            Object val = f.getAttribute(this.selAttribute);
            if (type == AttributeType.DOUBLE){
                data[i] = ((Double)val).doubleValue();
            }
            else if (type == AttributeType.INTEGER){
                data[i] = ((Integer)val).intValue();
            }               
            i++;
        } 
        
        //double[] data2 = { 45, 89, 6, 32, 63, 12 };
        
        final Plot2DPanelOJ plot = new Plot2DPanelOJ();                
        plot.addBarPlotOJ(this.selAttribute, data, fID, context, selLayer);
        plot.plotToolBar.setVisible(true);
        plot.setAxisLabel(0, sNthfeature);
        plot.setAxisLabel(1, this.selAttribute);
        
        // FrameView fv = new FrameView(plot);
        // -- replace the upper line by:
        JInternalFrame frame = new JInternalFrame(this.sBarPlot);
        frame.setLayout(new BorderLayout());
        frame.add(plot, BorderLayout.CENTER);
        frame.setClosable(true);
        frame.setResizable(true);
        frame.setMaximizable(true);
        frame.setSize(450, 450);
        frame.setVisible(true);
        
        context.getWorkbenchFrame().addInternalFrame(frame);
        /*
        if (plotDialog==null) {
            plotDialog = new PlotDialog(context, this.selAttribute, data, this.ranges);
        }
        else {plotDialog.setVisible(true);}
        */
        return true;
    }
    
    private List getFieldsFromLayerWithoutGeometryAndString() {
        return FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(dialog.getLayer(CLAYER));
    }
    
}
