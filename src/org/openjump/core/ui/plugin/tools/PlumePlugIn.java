/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 *
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.tools;
import java.util.Collection;

import javax.swing.JComponent;

import org.openjump.core.geomutils.GeoUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;

public class PlumePlugIn extends AbstractPlugIn {
    
	private WorkbenchContext workbenchContext;

	private final static String sNew = I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.New");
	private final static String sTheradius = I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.The-arc-radius");
	private final static String selectLineStrings = I18N.get("ui.cursortool.SelectLineStringsTool.select-linestrings");
	private final static String RADIUS = I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.Radius");
	private final static String RADIUS1 = RADIUS + " 1";
	private final static String RADIUS2 = RADIUS + " 2";
	private double radius1 = 5.0;
	private double radius2 = 50.0;

	public void initialize(PlugInContext context) throws Exception {     
		workbenchContext = context.getWorkbenchContext();
		context.getFeatureInstaller().addMainMenuItem(
		    this, new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
		    getName() + "...",
		    false,
		    null,
		    this.createEnableCheck(workbenchContext));
	}

	public boolean execute(final PlugInContext context) throws Exception {
		reportNothingToUndoYet(context);
		Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();

		//get the radii
		MultiInputDialog dialog = new MultiInputDialog(
				context.getWorkbenchFrame(), getName(), true);
		setDialogValues(dialog, context);
		dialog.setVisible(true);
		if (! dialog.wasOKPressed())
		{ return false; }
		getDialogValues(dialog);

		Geometry plume = null;
		if (selectedFeatures.size() != 1)
			return false;
		Geometry geo = ((Feature) selectedFeatures.iterator().next()).getGeometry();
		if (geo instanceof LineString) {
			plume = GeoUtils.createPlume(geo.getCoordinates(), radius1, radius2);
		}

		if (plume != null) {
			Feature currFeature = (Feature) selectedFeatures.iterator().next();
			Feature newFeature = (Feature) currFeature.clone();
			newFeature.setGeometry(plume);
			Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
			LayerManager layerManager = context.getLayerManager();
			FeatureDataset newFeatures = new FeatureDataset(currFeature.getSchema());
			newFeatures.add(newFeature);

			layerManager.addLayer(selectedCategories.isEmpty()
					? StandardCategoryNames.WORKING
							: selectedCategories.iterator().next().toString(),
							layerManager.uniqueLayerName(sNew),
							newFeatures);

			layerManager.getLayer(0).setFeatureCollectionModified(true);
			layerManager.getLayer(0).setEditable(true);
		}
		return true;
	}


	private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
		dialog.addDoubleField(RADIUS1, radius1, 6, sTheradius);
		dialog.addDoubleField(RADIUS2, radius2, 6, sTheradius);
	}

	private void getDialogValues(MultiInputDialog dialog) {
		radius1 = dialog.getDouble(RADIUS1);
		radius2 = dialog.getDouble(RADIUS2);
	}

	public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
		return new MultiEnableCheck()
		.add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
		.add(checkFactory.createOnlyOneLayerMayHaveSelectedFeaturesCheck())
		.add(checkFactory.createExactlyNFeaturesMustHaveSelectedItemsCheck(1))
		.add(onlyOneLinestringMayBeSelected(workbenchContext));
	}

	public EnableCheck onlyOneLinestringMayBeSelected(final WorkbenchContext workbenchContext) {
	    return new EnableCheck() {
	        public String check(JComponent component) {
		        Collection selectedItems = ((SelectionManagerProxy) workbenchContext
	                            .getWorkbench()
	                            .getFrame()
	                            .getActiveInternalFrame())
	                            .getSelectionManager()
	                            .getSelectedItems();	            
	            if ((Geometry) selectedItems.iterator().next() instanceof LineString) return null;
	            return selectLineStrings;
	        }
	    };
	}

}
