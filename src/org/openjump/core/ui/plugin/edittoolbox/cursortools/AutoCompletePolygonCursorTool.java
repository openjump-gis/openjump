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

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

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

   final static String sAutoComplete = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.AutoCompletePolygonCursorTool.Auto-Complete-Polygon");	   
   final static String sCanNotAdd = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.AutoCompletePolygonCursorTool.Can-not-add-polygon");	   
	   
   private static WorkbenchContext context;
   private EnableCheckFactory checkFactory;

   private FeatureDrawingUtil featureDrawingUtil;

   protected AutoCompletePolygonCursorTool(FeatureDrawingUtil featureDrawingUtil)
   {
       this.featureDrawingUtil = featureDrawingUtil;
   }
   
   @Override
   public String getName() {
      return sAutoComplete;
   }

   public AutoCompletePolygonCursorTool(EnableCheckFactory checkFactory) {
       this.checkFactory = checkFactory;
       allowSnapping();
   }

   public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy)
   {
       FeatureDrawingUtil featureDrawingUtil =
       new FeatureDrawingUtil(layerNamePanelProxy);
       
       return featureDrawingUtil.prepare(
       new AutoCompletePolygonCursorTool(featureDrawingUtil),
       true);
   }
   
   protected void gestureFinished() throws Exception {
      reportNothingToUndoYet();
      context = getWorkbench().getContext();
      if (!checkPolygon()) {
         return;
      }

      Layer lay = org.openjump.core.apitools.LayerTools.getSelectedLayer(context.createPlugInContext());

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
