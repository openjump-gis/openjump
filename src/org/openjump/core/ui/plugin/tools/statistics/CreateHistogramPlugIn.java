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
package org.openjump.core.ui.plugin.tools.statistics;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;

import com.vividsolutions.jump.workbench.ui.*;
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
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 *
 * created on 		19.10.2007
 * @author 			sstein
 */
public class CreateHistogramPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

    private String sHistogram = "Histogram";
    private String sCount = "count";

    private String T2 ="number of ranges";
    private String CLAYER = "select layer";
    private String ATTRIBUTE = "select attribute";
    private Layer selLayer = null;
    private int ranges = 7; 
    private String selAttribute = null;
    private String sName = "Create Histogram Plot";
    private String sWrongDataType = "Wrong datatype of chosen attribute";

    /**
     * this method is called on the startup by JUMP/OpenJUMP.
     * We set here the menu entry for calling the function.
     */
    public void initialize(PlugInContext context) throws Exception {
       	
    	
        ATTRIBUTE = GenericNames.SELECT_ATTRIBUTE;
        T2 = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.Number-of-ranges");
        CLAYER = GenericNames.SELECT_LAYER;
        sHistogram = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.Histogram-Plot");
        sCount = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.count");
        sName = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn");
		sWrongDataType = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Wrong-datatype-of-chosen-attribute");	
    	
    	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuPlugin(
    			this,
    			new String[] {MenuNames.TOOLS, MenuNames.STATISTICS, MenuNames.PLOT },
    			this.sName + "...",
    			false,          //checkbox
    			null,           //icon
    			createEnableCheck(context.getWorkbenchContext()));
    }
 
    
    /**
     * This method is used to define when the menu entry is activated or
     * disabled. In this example we allow the menu entry to be usable only
     * if one layer exists.
     */
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                        .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck());
    }
    
    /**
     * this function is called by JUMP/OpenJUMP if one clicks on the menu entry.
     * It is called before the "run" method and useful to do all the GUI /user-input things
     * In this example we call two additional methods {@link #setDialogValues(MultiInputDialog, PlugInContext)}
     * and {@link #getDialogValues(MultiInputDialog)} to obtain the Layer and the buffer radius by the user. 
     */
    public boolean execute(PlugInContext context) throws Exception{
    	
        this.reportNothingToUndoYet(context);         
        
            MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(),sName, true);
            this.setDialogValues(dialog, context);
            GUIUtil.centreOnWindow(dialog);
            dialog.setVisible(true);
            if (! dialog.wasOKPressed()) { return false; }
            this.getDialogValues(dialog);
            
        return true;
    }
    
	public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        createHistogram(context, this.selLayer);
		
	}
    
    private void setDialogValues(final MultiInputDialog dialog, PlugInContext context)
      {
        dialog.addLayerComboBox(CLAYER, context.getCandidateLayer(0), context.getLayerManager());
        
        List<String> list = AttributeTypeFilter.NUMERIC_FILTER.filter(context.getCandidateLayer(0));
        Object val = list.size()>0?list.iterator().next():null;
        final JComboBox<String> jcb_attribute = dialog.addComboBox(ATTRIBUTE, val, list, ATTRIBUTE);
        if (list.size() == 0) jcb_attribute.setEnabled(false);        
        dialog.addIntegerField(T2, this.ranges, 6, T2);
        
        dialog.getComboBox(CLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List<String> list = AttributeTypeFilter.NUMERIC_FILTER.filter(dialog.getLayer(CLAYER));
                if (list.size() == 0) {
                    jcb_attribute.setModel(new DefaultComboBoxModel<>(new String[0]));
                    jcb_attribute.setEnabled(false);
                }
                jcb_attribute.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
            }
        });        
      }

    private void getDialogValues(MultiInputDialog dialog) {
        this.ranges = dialog.getInteger(T2);
        this.selLayer = dialog.getLayer(CLAYER);
        this.selAttribute = dialog.getText(ATTRIBUTE);
      }
    
    private boolean createHistogram(final PlugInContext context, Layer selLayer) throws Exception {

        FeatureCollection fc = selLayer.getFeatureCollectionWrapper();
        FeatureSchema fs = fc.getFeatureSchema();
        AttributeType type = fs.getAttributeType(selAttribute);
        if (type != AttributeType.DOUBLE && type != AttributeType.INTEGER && type != AttributeType.LONG) {
			context.getWorkbenchFrame().warnUser(sWrongDataType);
            return false;
        }
        
        double[] data = new double[fc.size()];
        int i=0;
        for (Feature f : fc.getFeatures()) {
            Object val = f.getAttribute(this.selAttribute);
            if (val instanceof Number) {
                data[i] = ((Number) val).doubleValue();
                i++;
            }
        }
        double[] data2 = new double[i];
        System.arraycopy(data,0,data2,0,i);

        final Plot2DPanelOJ plot = new Plot2DPanelOJ();
        plot.addHistogramPlotOJ(selAttribute, data2, ranges, context, selLayer, selAttribute);
        plot.plotToolBar.setVisible(true);
        plot.setAxisLabel(0, selAttribute);
        plot.setAxisLabel(1, sCount);
        
        // FrameView fv = new FrameView(plot);
        // -- replace the upper line by:
        JInternalFrame frame = new JInternalFrame(sHistogram);
        frame.setLayout(new BorderLayout());
        frame.add(plot, BorderLayout.CENTER);
        frame.setClosable(true);
        frame.setResizable(true);
        frame.setMaximizable(true);
        frame.setSize(450, 450);
        frame.setVisible(true);

        context.getWorkbenchFrame().addInternalFrame(frame);
        return true;
    }

}
