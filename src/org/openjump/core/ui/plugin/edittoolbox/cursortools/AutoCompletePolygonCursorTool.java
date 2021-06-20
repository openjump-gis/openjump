package org.openjump.core.ui.plugin.edittoolbox.cursortools;

/*
 * Copyright (C) 2007 Leibniz Universitaet Hannover  Institute of Environmetal Planning
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
 */
import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.PolygonTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;

/*
 * @author Daniel K&uuml;mper
 */
public class AutoCompletePolygonCursorTool extends PolygonTool {

   final static String sAutoComplete = I18N.getInstance().get("org.openjump.core.ui.plugin.edittoolbox.cursortools.AutoCompletePolygonCursorTool.Auto-Complete-Polygon");	   
   final static String sCanNotAdd = I18N.getInstance().get("org.openjump.core.ui.plugin.edittoolbox.cursortools.AutoCompletePolygonCursorTool.Can-not-add-polygon");	   
	   
   private static WorkbenchContext context;
   private EnableCheckFactory checkFactory;

   private FeatureDrawingUtil featureDrawingUtil;

   protected AutoCompletePolygonCursorTool(WorkbenchContext context, FeatureDrawingUtil featureDrawingUtil)
   {
       super(context);
       this.featureDrawingUtil = featureDrawingUtil;
   }
   
   @Override
   public String getName() {
      return sAutoComplete;
   }

   public AutoCompletePolygonCursorTool(WorkbenchContext context) {
       super(context);
       this.checkFactory = EnableCheckFactory.getInstance(context);
       allowSnapping();
   }

   public static CursorTool create(WorkbenchContext context)
   {
       FeatureDrawingUtil featureDrawingUtil =
       new FeatureDrawingUtil(context);
       
       return featureDrawingUtil.prepare(
       new AutoCompletePolygonCursorTool(context, featureDrawingUtil),
       true);
   }
   
	@Override
	protected void gestureFinished() throws Exception {
		reportNothingToUndoYet();
		context = getWorkbench().getContext();
		if (!checkPolygon()) {
			return;
		}
		Collection<Layer> layers = getPanel().getLayerManager().getEditableLayers();
		if(layers.size()==0 || layers.size()>1) {
			getPanel()
			.getContext()
			.warnUser(

					I18N.getInstance().get("com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-be-selected"));

			return;
		}
		Layer lay = org.openjump.core.apitools.LayerTools.getSelectedLayer(context.createPlugInContext());
		if (!lay.isEditable()) {
			getPanel()
			.getContext()
			.warnUser(

					I18N.getInstance().get("com.vividsolutions.jump.workbench.plugin.Selected-items-layers-must-be-editable"));

			return;

		}
		Feature[] featuresInFence = de.fho.jump.pirol.utilities.plugIns.StandardPirolPlugIn.getFeaturesInFenceOrInLayer(context.createPlugInContext(), lay);

		Polygon poly = this.getPolygon();

		Geometry newGeo = poly.getGeometryN(0);

		Geometry diffGeo = newGeo;

		try {
			for (int i = 0; i < featuresInFence.length; i++) {
				diffGeo = diffGeo.difference(featuresInFence[i].getGeometry());
			}
			if (diffGeo instanceof Polygon){
				featureDrawingUtil.drawRing(
						(Polygon)diffGeo,
						isRollingBackInvalidEdits(),
						this,
						getPanel()); 
			}
			//System.out.println("Polygon added");

		} catch (Exception e) {
			//System.out.println("Unknown Exception" + e);
			getPanel().getContext().warnUser(sCanNotAdd + " " + e);
		}
	}
   public Icon getIcon() {
	   return new ImageIcon(getClass().getResource("AutoCompletePoly.gif"));
   }

}
