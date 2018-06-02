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
import java.util.Iterator;

import javax.swing.JComponent;

import org.openjump.core.geomutils.Arc;
import org.openjump.core.geomutils.MathVector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
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
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

public class JoinWithArcPlugIn extends AbstractPlugIn {

    private WorkbenchContext workbenchContext;
    
    private final static String sJoinWithArc = I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.Join-With-Arc");
    private final static String sNew = I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.New");
    private final static String sTheArcRadius = I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.The-arc-radius");
    private final static String sBetween= I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.Between");
    private final static String sAnd= I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.and");
    private final static String sFeaturesMustBeSelected= I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.features-must-be-selected");
	
    private final static String RADIUS = I18N.get("org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn.Radius");
    private double arcRadius = 50.0;

    public void initialize(PlugInContext context) throws Exception
    {     
        workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY },
                getName(),
                false, null, this.createEnableCheck(workbenchContext));
    }
    
    public String getName() {
        return sJoinWithArc;
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
        reportNothingToUndoYet(context);
        Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
        
        //get the arc radius
        MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        
        Geometry fillet = null;
        
        if (selectedFeatures.size() == 1)
        {
            Geometry geo = ((Feature) selectedFeatures.iterator().next()).getGeometry();
        if (geo instanceof LinearRing)
            {
                fillet = filletLinearRing((LinearRing) geo);
            }
            else if (geo instanceof LineString)
            {
                fillet = filletOneLineString((LineString) geo);
            }
            else if (geo instanceof Polygon)
            {
                fillet = filletPolygon((Polygon) geo);
            }
        }
        else if (selectedFeatures.size() == 2)
        {
            Iterator i = selectedFeatures.iterator();
            Feature feature1 = (Feature) i.next();
            Feature feature2 = (Feature) i.next();
            Geometry geo1 = feature1.getGeometry();
            Geometry geo2 = feature2.getGeometry();
            
            if ((geo1 instanceof LineString) && (geo2 instanceof LineString))
            {
                fillet = filletTwoLineStrings((LineString)geo1, (LineString)geo2);
            }
        }
            
        if (fillet != null) {
            Feature currFeature = (Feature) selectedFeatures.iterator().next();
            Feature newFeature = currFeature.clone(true);
            newFeature.setGeometry(fillet);
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
    
      private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
      {
        dialog.addDoubleField(RADIUS, arcRadius, 6, sTheArcRadius);
      }

      private void getDialogValues(MultiInputDialog dialog) {
        arcRadius = dialog.getDouble(RADIUS);
      }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createOnlyOneLayerMayHaveSelectedFeaturesCheck())
            .add(createBetweenNAndMFeaturesMustBeSelectedCheck(1, 2))
            .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(1));
    }

    private EnableCheck createBetweenNAndMFeaturesMustBeSelectedCheck(final int n, final int m) {
        return new EnableCheck() {
            public String check(JComponent component) {
                int numSelected = workbenchContext.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size(); 
                return (
                    ((numSelected > m) || (numSelected < n))
                    ? (sBetween + " " + n + " " + sAnd+ " " + m + " " + sFeaturesMustBeSelected)
                    : null);
            }
        };
    }
    
    private Coordinate Intersect(Coordinate P1, Coordinate P2, Coordinate P3, Coordinate P4) //find intersection of two lines
    {
        Coordinate V = new Coordinate((P2.x - P1.x), (P2.y - P1.y));
        Coordinate W = new Coordinate((P4.x - P3.x), (P4.y - P3.y));
        double n = W.y * (P3.x - P1.x) - W.x * (P3.y - P1.y);
        double d = W.y * V.x - W.x * V.y;
        
        if (d != 0.0) {
            double t1 = n / d;
            return new Coordinate((P1.x + V.x * t1),(P1.y + V.y * t1));
        }
        else {
            return null;
        }
    }
    
    private LineString MakeRoundCorner(Coordinate A, Coordinate B, Coordinate C, Coordinate D,
                                       double r, boolean arcOnly) {
        MathVector Gv;
        MathVector Hv;
        MathVector Fv;
        Coordinate E = Intersect(A, B, C, D);	//vector solution
        
        if (E != null) { //non-parallel lines

            MathVector Ev = new MathVector(E);
            
            if (E.distance(B) > E.distance(A)) { //find longest distance from intersection
                //these equations assume B and D are closest to the intersection
                //reverse points
                Coordinate temp = A;
                A = B;
                B = temp;
            }
            
            if (E.distance(D) > E.distance(C)) {//find longest distance from intersection
                //these equations assume B and D are closest to the intersection
                //reverse points
                Coordinate temp = C;
                C = D;
                D = temp;
            }
            
            MathVector Av = new MathVector(A);
            MathVector Cv = new MathVector(C);
            double alpha = Av.vectorBetween(Ev).angleRad(Cv.vectorBetween(Ev)) / 2.0; //we only need the half angle
            double h1 = Math.abs(r / Math.sin(alpha));  //from definition of sine solved for h
            
            if ((h1 * h1 - r * r) >= 0) {
                double d1 = Math.sqrt(h1 * h1 - r * r);	//pythagorean theorem}
                double theta = Math.PI / 2.0 - alpha; //sum of triangle interior angles = 180 degrees
                theta = theta * 2.0;		      //we only need the double angle}
                //we now have the angles and distances needed for a vector solution: 
                //we must find the points G and H by vector addition. 
                Gv = Ev.add(Av.vectorBetween(Ev).unit().scale(d1));
                Hv = Ev.add(Cv.vectorBetween(Ev).unit().scale(d1));
                Fv = Ev.add(Gv.vectorBetween(Ev).rotateRad(alpha).unit().scale(h1));
                
                if (Math.abs(Fv.distance(Hv) - Fv.distance(Gv)) > 1.0) {//rotated the wrong dirction
                    Fv = Ev.add(Gv.vectorBetween(Ev).rotateRad(-alpha).unit().scale(h1));
                    theta = -theta;
                }
                
                CoordinateList coordinates = new CoordinateList();
                if (!arcOnly) coordinates.add(C);
                Arc arc = new Arc(Fv.getCoord(), Hv.getCoord(), Math.toDegrees(theta));
                LineString lineString = arc.getLineString();
                coordinates.add(lineString.getCoordinates(), false);
                if (!arcOnly) coordinates.add(A);
                return new GeometryFactory().createLineString(coordinates.toCoordinateArray());
            }
        }
        return null;
    }
        
   
     private LineString filletTwoLineStrings(LineString ls1, LineString ls2) {
         Coordinate A = ls1.getCoordinateN(0);
         Coordinate B = ls1.getCoordinateN(1);
         Coordinate C = ls2.getCoordinateN(0);
         Coordinate D = ls2.getCoordinateN(1);
         LineString lineString = MakeRoundCorner(A, B, C, D, arcRadius, false);
         if (lineString != null) {
             CoordinateList coordinates = new CoordinateList();
             coordinates.add(lineString.getCoordinates(), false);
             return new GeometryFactory().createLineString(coordinates.toCoordinateArray());
         }
         return null;
     }
     
     private LineString filletOneLineString(LineString ls)
     {
         if (ls.getNumPoints() > 2)
         {
             CoordinateList filletCoordinates = new CoordinateList();
             filletCoordinates.add(ls.getCoordinateN(0));
             
             for (int i = 0; i <= ls.getNumPoints() - 3; i++) {
                 Coordinate A = ls.getCoordinateN(i);
                 Coordinate B = ls.getCoordinateN(i+1);
                 Coordinate C = ls.getCoordinateN(i+1); //copy B
                 Coordinate D = ls.getCoordinateN(i+2);
                 LineString lineString = MakeRoundCorner(A, B, C, D, arcRadius, true);
                 if (lineString != null && !lineString.isEmpty()) {
                     filletCoordinates.add(lineString.getCoordinates(), false, false);
                 }
             }
             
             filletCoordinates.add(ls.getCoordinateN(ls.getNumPoints() - 1));
             return new GeometryFactory().createLineString(filletCoordinates.toCoordinateArray());
         }
        return null;
     }
     
     private Polygon filletPolygon(Polygon poly)
     {
         LineString ls = poly.getExteriorRing();
         
         CoordinateList filletCoordinates = new CoordinateList();
         
         for (int i = 0; i <= ls.getNumPoints() - 3; i++) {
             Coordinate A = ls.getCoordinateN(i);
             Coordinate B = ls.getCoordinateN(i+1);
             Coordinate C = ls.getCoordinateN(i+1); //copy B
             Coordinate D = ls.getCoordinateN(i+2);
             LineString lineString = MakeRoundCorner(A, B, C, D, arcRadius, true);
             if (lineString != null) {
                 filletCoordinates.add(lineString.getCoordinates(), false, false);
             }
         }

         Coordinate A = ls.getCoordinateN(ls.getNumPoints() - 2); //second to last
         Coordinate B = ls.getCoordinateN(0); //last == first
         Coordinate C = ls.getCoordinateN(0);
         Coordinate D = ls.getCoordinateN(1);
         LineString lineString = MakeRoundCorner(A, B, C, D, arcRadius, true);
         if (lineString != null) {
             filletCoordinates.add(lineString.getCoordinates(), false, false);
             filletCoordinates.add(filletCoordinates.getCoordinate(0));
         }
         return new GeometryFactory().createPolygon( new GeometryFactory().createLinearRing(filletCoordinates.toCoordinateArray()),null);
     }
     
     private LinearRing filletLinearRing(LinearRing ring)
     {
         CoordinateList filletCoordinates = new CoordinateList();
         
         for (int i = 0; i <= ring.getNumPoints() - 3; i++)
         {
             Coordinate A = ring.getCoordinateN(i);
             Coordinate B = ring.getCoordinateN(i+1);
             Coordinate C = ring.getCoordinateN(i+1); //copy B
             Coordinate D = ring.getCoordinateN(i+2);
             LineString lineString = MakeRoundCorner(A, B, C, D, arcRadius, true);
             if (lineString != null) {
                 filletCoordinates.add(lineString.getCoordinates(), false, false);
             }
         }

         Coordinate A = ring.getCoordinateN(ring.getNumPoints() - 2); //second to last
         Coordinate B = ring.getCoordinateN(0); //last == first
         Coordinate C = ring.getCoordinateN(0);
         Coordinate D = ring.getCoordinateN(1);
         LineString lineString = MakeRoundCorner(A, B, C, D, arcRadius, true);
         if (lineString != null) {
             filletCoordinates.add(lineString.getCoordinates(), false, false);
             filletCoordinates.add(filletCoordinates.getCoordinate(0));
         }
         return new GeometryFactory().createLinearRing(filletCoordinates.toCoordinateArray());
     }     
}
