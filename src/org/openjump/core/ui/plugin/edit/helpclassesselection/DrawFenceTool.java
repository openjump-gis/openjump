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
 * created:  		by Vivid Solutions
 * last modified:  	18.05.2005 by sstein
 * 
 * description:
 *		starts the drawing by cursor and selects the 
 *		features in the fence <p>
 *		uses FenceDrawingUtil class 
 * 
 *****************************************************/
package org.openjump.core.ui.plugin.edit.helpclassesselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.Icon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.PolygonTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class DrawFenceTool extends PolygonTool {
	private FenceDrawingUtil myfDrawingUtil;
	private PlugInContext context;

	//sst: change: PlugInContext added
	protected DrawFenceTool(FenceDrawingUtil featureDrawingUtil, PlugInContext context) {
		this.myfDrawingUtil = featureDrawingUtil;
		this.context = context;
	}
	
	//static method to use class (sst: change: PlugInContext added)
	public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy, PlugInContext context) {
		FenceDrawingUtil fDUtil = new FenceDrawingUtil(layerNamePanelProxy);
		//return fDUtil.prepare(new DrawFenceTool(fDUtil, context),true);
		return fDUtil.prepare(new DrawFenceTool(fDUtil, context),false); // no snap!
	}

	public Icon getIcon() {
		//return IconLoader.icon("DrawPolygon.gif");
		return IconLoader.icon("");
	}

	protected void gestureFinished() throws Exception {
		reportNothingToUndoYet();

		if (!checkPolygon()){
			return;
			}
		/** ============ sst: begin change =======
		this.myfDrawingUtil.drawRing(
			getPolygon(),
			isRollingBackInvalidEdits(),
			this,
			getPanel());
        **/
		int count = 0;
		Layer[] selectedLayers = context.getLayerNamePanel().getSelectedLayers();
		for (int i = 0; i < selectedLayers.length; i++) {
			Layer actualLayer = selectedLayers[i]; 		
			FeatureCollection fc = context.getSelectedLayer(i).getFeatureCollectionWrapper().getWrappee();
			Collection features = new ArrayList();
			for (Iterator iter = fc.iterator(); iter.hasNext();) {
				Feature element = (Feature) iter.next();
				if(!this.getPolygon().disjoint(element.getGeometry())){
					features.add(element);
					count++;
				}
			}
			context.getLayerViewPanel().getSelectionManager().getFeatureSelection().selectItems(actualLayer, features);			
		}		
	    final Collection myf = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
		//context.getWorkbenchFrame().setTimeMessage("Items: " + count + ", selected items:" + myf.size());
		context.getWorkbenchFrame().setTimeMessage(
				I18N.get("org.openjump.core.ui.plugin.edit.helpclassesselection.DrawFenceTool.layer-items") + ": " + 
				count + 
				", " +
				I18N.get("org.openjump.core.ui.plugin.edit.helpclassesselection.DrawFenceTool.selected-items") +
				": " +
				myf.size());
		
		//-- end mouse gesture by using a dirty trick
        Enumeration buttons = context.getWorkbenchContext().getWorkbench().
   		getFrame().getToolBar().getButtonGroup().getElements();
   
        AbstractButton myButton = (AbstractButton)buttons.nextElement();
        //System.out.println(myButton.getClass());
        int j = 0;
        while(buttons.hasMoreElements()){
        	j++;
        	myButton = (AbstractButton)buttons.nextElement();
        	if (j ==1){ //j==1 is move button
        		myButton.doClick();
        	}
        	//System.out.println(myButton.getClass());
        }

		//======== sst: end change ==========	
		
	}

}
