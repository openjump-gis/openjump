  package org.openjump.core.ui.plugin.edittoolbox.cursortools;
  
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.cursortool.CoordinateListMetrics;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JOptionPane;

  
  /*
   * Giuseppe Aruta Dec 4 2015
   * This tool partially derives from SplitPolygonPlugIn.class from Kosmo SAIG
   * It has been modified to be used both with selected Linestrings and Polygons.
   * It works only with one editable/selectable layer, already selected on layer list
   * A warning message will points, multigeometries or geometry collections are 
   */
  
  
  
  public class CutFeaturesTool
    extends MultiClickTool
  {
    static final String sCookieCut = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.CutPolygonTool.Create-Cookie-Cut");
    
    Geometry geomSelected = null;
    Geometry geomDraw = null;
    Geometry newGeomIntersect = null;
    Geometry newGeomIntersect1 = null;
    Geometry newGeomDiff = null;
    public CutFeaturesTool(EnableCheckFactory checkFactory) {
    }
    
    PlugInContext  context;
    public CutFeaturesTool(PlugInContext context)
    {
    	this.context = context;
    	setColor(Color.red);
    	setStroke(new BasicStroke(1.5f,                      // Width
                BasicStroke.CAP_SQUARE,    // End cap
                BasicStroke.JOIN_ROUND,    // Join style
                10.0f,                     // Miter limit
                new float[] {10.0f,5.0f}, // Dash pattern
                0.0f));
      allowSnapping();
      setMetricsDisplay(new CoordinateListMetrics());
    }
    
    
  
    public Icon getIcon()
    {
      return IconLoader.icon("splitPolygon.png");
    }
    
    public String getName() {
      return I18N.get("org.openjump.core.ui.plugin.tools.CutFeaturesTool");
    }
    
    public Cursor getCursor() {
      return createCursor(IconLoader.icon("splitPolygonCursor.png").getImage());
    }
    
  
    protected void gestureFinished()
      throws Exception
    {
      WorkbenchContext context = getWorkbench().getContext();
      reportNothingToUndoYet();
      LayerNamePanel layernamepanel = context.getLayerNamePanel();
      Layer[] selectedLayers = layernamepanel.getSelectedLayers();
     
 
      if (selectedLayers.length == 0){
        JOptionPane.showMessageDialog(null, 
          I18N.getMessage("com.vividsolutions.jump.workbench.plugin.At-least-one-layer-must-be-selected", 
          new Object[] { Integer.valueOf(1) }), I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), 
          1);
  
      }
      else if (selectedLayers.length > 1) {
        JOptionPane.showMessageDialog(null, 
          I18N.getMessage( "com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-have-selected-items", 
          new Object[] { Integer.valueOf(1) }), I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), 
          1);
      } else {
    	  Layer activeLayer = selectedLayers[0];
          if (activeLayer.isEditable()) {
             Collection selectedFeatures = context.getLayerViewPanel()
        	            .getSelectionManager().getFeaturesWithSelectedItems(activeLayer);
             for (Iterator k = selectedFeatures.iterator(); k.hasNext();) {
            	 Feature featureSelected = (Feature)k.next();
            	 this.geomSelected = featureSelected.getGeometry();
            	 this.geomDraw = getLineString();
            	 SelectionManager selectionManager = getPanel().getSelectionManager();
            	 Layer editableLayer = getSelectedLayer();
            	 		if ((this.geomSelected.isEmpty())){
            	 				return;
            	 			}
            	 		if (this.geomSelected.contains(this.geomDraw)){
            	 				return;
            	 			}
            	 		if (this.geomDraw.intersects(this.geomSelected)){
            	 			if (this.geomSelected instanceof Polygon ||  this.geomSelected instanceof MultiPolygon) {
            	 				List<Geometry> div = splitPoligon(this.geomDraw, this.geomSelected);
            	 				Geometry newGeomIntersect = (Geometry)div.get(0);
            	 				Feature featureIntersect = featureSelected.clone(true);
            	 				FeatureUtil.copyAttributes(featureSelected,featureIntersect);
            	 				featureIntersect.setGeometry(newGeomIntersect);
            	 				this.newGeomIntersect1 = this.geomSelected.difference(newGeomIntersect);
            	 				BasicFeature featureIntersect1 = new BasicFeature(activeLayer.getFeatureCollectionWrapper().getFeatureSchema());
	            	 			FeatureUtil.copyAttributes(featureSelected,featureIntersect1);
	            	 			featureIntersect1.setGeometry(this.newGeomIntersect1);
	            	 			EditTransaction edtr = new EditTransaction(new ArrayList(), "cut polygon", activeLayer, true, true, context.getLayerViewPanel());
	            	 			edtr.deleteFeature(featureSelected);
	            	 			edtr.createFeature(featureIntersect);
	            	 			edtr.createFeature(featureIntersect1);
	            	 			edtr.commit();
	            	 			edtr.clearEnvelopeCaches();
	            	 		//	selectionManager.getFeatureSelection().selectItems(editableLayer, featureIntersect);
	            	 		//	selectionManager.getFeatureSelection().selectItems(editableLayer, featureIntersect1);
            	 			}else 
            	 				if (this.geomSelected instanceof LineString ||this.geomSelected instanceof MultiLineString ){
            	 					List<Geometry> div = splitLines(this.geomDraw, this.geomSelected);
            	 					Geometry newGeomIntersect = (Geometry)div.get(0);
            	 					Geometry newGeomIntersect1 = (Geometry)div.get(1);
            	 					Feature featureIntersect = featureSelected.clone(true);
            	 					FeatureUtil.copyAttributes(featureSelected,featureIntersect);
            	 					featureIntersect.setGeometry(newGeomIntersect);
            	 					BasicFeature featureIntersect1 = new BasicFeature(activeLayer.getFeatureCollectionWrapper().getFeatureSchema());
            	 					FeatureUtil.copyAttributes(featureSelected,featureIntersect1);
            	 					featureIntersect1.setGeometry(newGeomIntersect1);
            	 					EditTransaction edtr = new EditTransaction(new ArrayList(), "cut polygon", activeLayer,true, true, context.getLayerViewPanel());
            	 					edtr.deleteFeature(featureSelected);
            	 					edtr.createFeature(featureIntersect);
            	 					edtr.createFeature(featureIntersect1);
            	 					edtr.commit();
            	 					edtr.clearEnvelopeCaches();
            	 			//		selectionManager.getFeatureSelection().selectItems(editableLayer, featureIntersect);
            	 			//		selectionManager.getFeatureSelection().selectItems(editableLayer, featureIntersect1);
            	 			} else 
            	 				{
            	 				return;
            	 			} 
        	           } else {
	            	JOptionPane.showMessageDialog(null,  I18N.get("ui.SchemaPanel.layer-must-be-editable"),  I18N.get("org.openjump.core.ui.plugin.edittoolbox.Information"), JOptionPane.INFORMATION_MESSAGE);
	           }
            }
          }
       }
    }
    
    protected boolean isRollingBackInvalidEdits(WorkbenchContext context)
    {
      return 
        context.getWorkbench().getBlackboard().get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
    }
    
    protected boolean checkLineString() throws NoninvertibleTransformException {
      if (getCoordinates().size() < 2)
      {
  
        getPanel().getContext().warnUser(
          I18N.get("ui.cursortool.editing.DrawLineString.the-linestring-must-have-at-least-2-points"));
        
        return false;
      }
      
      IsValidOp isValidOp = new IsValidOp(getLineString());
      
      if (!isValidOp.isValid()) {
        getPanel().getContext().warnUser(
          isValidOp.getValidationError().getMessage());
        
  
        if (getWorkbench().getBlackboard().get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)) {
          return false;
        }
      }
      
      return true;
    }
    
    private LineString getLineString() throws NoninvertibleTransformException {
      return 
        new GeometryFactory().createLineString(toArray(getCoordinates()));
    }
    
   public static List<Geometry> splitPoligon(Geometry digitizedGeometry, Geometry geomSel)
      throws Exception
    {
      List<Geometry> intersectingParts = new ArrayList();
      for (int i = 0; i < digitizedGeometry.getNumGeometries(); i++) {
        if (digitizedGeometry.getGeometryN(i).intersects(geomSel)) {
          intersectingParts.add(digitizedGeometry.getGeometryN(i));
        }
      }
      List<Geometry> geometries = new ArrayList();
      try
      {
        Iterator<Geometry> itParts = intersectingParts.iterator();
        while (itParts.hasNext()) {
          Geometry currentPart = (Geometry)itParts.next();
          Geometry intersections = currentPart.intersection(geomSel);
          if ((intersections != null) && (!intersections.isEmpty()))
          {
         Geometry boundIntersection = geomSel.getBoundary()
              .intersection(currentPart);
            if ((boundIntersection != null) && (
              (boundIntersection.getDimension() != 0) || 
              (boundIntersection.getNumGeometries() != 1)))
            {
               geometries.addAll(
                LinearComponentExtracter.getLines(currentPart)); }
          } }
        Polygonizer polygonizer = new Polygonizer();
        List lines = LinearComponentExtracter.getLines(geomSel);
        MultiLineString mls = new GeometryFactory()
          .createMultiLineString(
          GeometryFactory.toLineStringArray(lines));
        Geometry unionGeom = mls;
        Iterator<Geometry> iterator = geometries.iterator();
        while (iterator.hasNext()) {
          Geometry geom = (Geometry)iterator.next();
          unionGeom = unionGeom.union(geom);
        }
        geometries.clear();
        polygonizer.add(unionGeom);
        Collection<Geometry> geomCol = polygonizer.getPolygons();
        Iterator<Geometry> iter = geomCol.iterator();
        Geometry bufferedGeomSel = geomSel.buffer(0.001D);
        while (iter.hasNext()) {
          Geometry currentGeom = (Geometry)iter.next();
           if ((bufferedGeomSel.contains(currentGeom)) && 
            (!currentGeom.equals(geomSel))) {
            geometries.add(currentGeom);
          }
        }
      }
      catch (TopologyException ex)
      {
        return new ArrayList();
      }
      return geometries;
    }
  
  
    public static List<Geometry> splitLines(Geometry digitizedGeometry, Geometry geomSel)
      throws Exception
    {
      List<Geometry> intersectingParts = new ArrayList();
      for (int i = 0; i < digitizedGeometry.getNumGeometries(); i++) {
        if (digitizedGeometry.getGeometryN(i).intersects(geomSel)) {
          intersectingParts.add(digitizedGeometry.getGeometryN(i));
        }
      }
      List<Geometry> geometries = new ArrayList();
      try
      {
        Iterator<Geometry> itParts = intersectingParts.iterator();
        while (itParts.hasNext()) {
          Geometry currentPart = (Geometry)itParts.next();
          Geometry intersections = currentPart.intersection(geomSel);
          if ((intersections != null) && (!intersections.isEmpty()))
          {
        geometries.addAll(
              LinearComponentExtracter.getLines(currentPart)); }
        }
        LineMerger linemerger = new LineMerger();
        List lines = LinearComponentExtracter.getLines(geomSel);
        MultiLineString mls = new GeometryFactory()
          .createMultiLineString(
          GeometryFactory.toLineStringArray(lines));
        Geometry unionGeom = mls;
        Iterator<Geometry> iterator = geometries.iterator();
        while (iterator.hasNext()) {
          Geometry geom = (Geometry)iterator.next();
          unionGeom = unionGeom.union(geom);
        }
        geometries.clear();
        linemerger.add(unionGeom);
        Collection<Geometry> geomCol = linemerger.getMergedLineStrings();
        Iterator<Geometry> iter = geomCol.iterator();
        Geometry bufferedGeomSel = geomSel.buffer(0.001D);
        while (iter.hasNext()) {
          Geometry currentGeom = (Geometry)iter.next();
         if ((bufferedGeomSel.contains(currentGeom)) && 
            (!currentGeom.equals(geomSel))) {
            geometries.add(currentGeom);
          }
        }
      }
      catch (TopologyException ex) {
        return new ArrayList();
      }
      return geometries;
    }
    
  
  
  
  
  
    public Layer getSelectedLayer()
    {
      Collection<Layer> editableLayers = getPanel().getLayerManager()
        .getEditableLayers();
      
      if (editableLayers.isEmpty()) {
        return null;
      }
      return (Layer)editableLayers.iterator().next();
    }
  }

