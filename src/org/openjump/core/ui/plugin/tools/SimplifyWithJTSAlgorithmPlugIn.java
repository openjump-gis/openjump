/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
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
 * created:  		30.05.2005
 * last modified:  	31.05.2005
 * 					
 * 
 * @author sstein
 * 
 * description:
 * 		simplifies a selected line, criterion is a maximal line displacement <p>
 * 		It is used the JTS 1.5 douglas peucker simplification with topology 
 * 		preservation for polygons and DouglasPeuckerSimplifier for linestrings.<p>
 * 		n.b.: the jts-algorithm handles all geometry types
 *   
 *****************************************************/

package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

import java.util.Iterator;
import com.vividsolutions.jump.workbench.model.Layer;
import java.util.Collection;

/**
 * Simplifies a selected line, criterion is a maximal line displacement <p>
 * it uses JTS TopologyPreservingSimplifier.
 * n.b.: since version > 1.9.1 the plugin handles all geometry types
 *
 * @author sstein, mmichaud
 *
 **/
public class SimplifyWithJTSAlgorithmPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

    private final static String sSimplifyJTSAlgorithm = I18N.get("org.openjump.core.ui.plugin.tools.SimplifyWithJTSAlgorithmPlugIn");
    private final static String sidebarText=I18N.get("org.openjump.core.ui.plugin.tools.SimplifyWithJTSAlgorithmPlugIn.Line-simplification-for-a-selected-line-or-polygon");
    private final static String sItem=I18N.get("org.openjump.core.ui.plugin.tools.SimplifyWithJTSAlgorithmPlugIn.Item");
    private final static String sSimplificationFinalized=I18N.get("org.openjump.core.ui.plugin.tools.SimplifyWithJTSAlgorithmPlugIn.simplification-finalized");
    private static String T3=I18N.get("org.openjump.core.ui.plugin.tools.SimplifyWithJTSAlgorithmPlugIn.Maximum-point-displacement-in-model-units");
    double maxPDisp = 0;

    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuPlugin(
    	        this,								//exe
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERALIZATION }, 	//menu path
                sSimplifyJTSAlgorithm , //name methode .getName recieved by AbstractPlugIn 
                false,			//checkbox
                null,			//icon
                createEnableCheck(context.getWorkbenchContext())); //enable check        
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1))
						.add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());
    }
    
	public boolean execute(PlugInContext context) throws Exception{
	    this.reportNothingToUndoYet(context);
	    MultiInputDialog dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), getName(), true);
	        setDialogValues(dialog, context);
	        GUIUtil.centreOnWindow(dialog);
	        dialog.setVisible(true);
	        if (! dialog.wasOKPressed()) { return false; }
	        getDialogValues(dialog);
	        return true;
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {
	    dialog.setSideBarDescription(sidebarText);
	    dialog.addDoubleField(T3,1.0,5);
	  }

	private void getDialogValues(MultiInputDialog dialog) {
	    this.maxPDisp = dialog.getDouble(T3);

	  }

	protected Layer layer(PlugInContext context) {
		return (Layer) context.getLayerViewPanel().getSelectionManager()
				.getLayersWithSelectedItems().iterator().next();
	}
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{
        
    		monitor.allowCancellationRequests();
    	    this.simplify(context, this.maxPDisp, monitor);
    	    System.gc();    		
    	}
	

	private boolean simplify(PlugInContext context, double maxDisp, TaskMonitor monitor) throws Exception{

	    //-- get selected items
	    final Collection features = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();

		EditTransaction transaction = new EditTransaction(features, this.getName(), layer(context),
						this.isRollingBackInvalidEdits(context), false, context.getWorkbenchFrame());
	    
	    int count=0; 
	    int noItems = features.size(); 
	    Geometry resultgeom = null;
	    //--get single object in selection to analyse
      	for (Iterator iter = features.iterator(); iter.hasNext();) {
      		count++;
      		Feature f = (Feature)iter.next();
			resultgeom = TopologyPreservingSimplifier.simplify(f.getGeometry(), Math.abs(maxDisp));
			String mytext = sItem + ": " + count + " / " + noItems + " : " + sSimplificationFinalized;
			monitor.report(mytext);
			//-- commit changes to undo history
			transaction.setGeometry(f, resultgeom);
      	}
		transaction.commit();
        return true;        
	}
	  
}
