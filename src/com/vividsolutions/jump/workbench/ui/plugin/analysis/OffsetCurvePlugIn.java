/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.Exception;
import java.lang.Integer;
import java.util.*;

import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.algorithm.distance.DistanceToPoint;
import com.vividsolutions.jts.algorithm.distance.PointPairDistance;
import com.vividsolutions.jts.geomgraph.Position;
import com.vividsolutions.jts.math.Vector2D;
import com.vividsolutions.jts.noding.SegmentString;
import com.vividsolutions.jts.operation.buffer.OffsetCurveBuilder;
import com.vividsolutions.jts.operation.buffer.OffsetCurveSetBuilder;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

/**
 * PlugIn creating an offset linestring from a linear geometry.
 * It takes a number of options :
 * <ul>
 * <li>offset size</li>
 * <li>keep attributes of source features</li>
 * <li>choose join style</li>
 * <li>number of segments by quadrant</li>
 * <li>fix a distance limit for mitre join style</li>
 * </ul>
 * @author Micha&euml;l Michaud
 */
public class OffsetCurvePlugIn extends AbstractThreadedUiPlugIn {
	
  private String MAIN_OPTIONS;
  private String PROCESSED_DATA;
  private String LAYER;
  private String SELECTION;
  private String SELECTION_HELP;
  
  private String DISTANCE;
  private String FIXED_DISTANCE;
  private String ATTRIBUTE;
  private String FROM_ATTRIBUTE;
  private String ATTRIBUTE_TOOLTIP;
  private String ROUGH_OFFSET_CURVE;
  private String ROUGH_OFFSET_CURVE_TOOLTIP;
  private String OFFSET;
  
  private String OTHER_OPTIONS;
  private String QUADRANT_SEGMENTS;
  
  private String ADVANCED_OPTIONS;
  
  private String JOIN_STYLE_TITLE;
  private String JOIN_STYLE;
  private String JOIN_BEVEL;
  private String JOIN_MITRE;
  private String JOIN_ROUND;
  private String MITRE_LIMIT;
  
  private List joinStyles;

  private Layer layer;
  private double offsetDistance    = 1.0;
  private boolean roughOffsetCurve = false;
  private int joinStyleCode        = BufferParameters.JOIN_ROUND;;
  private double mitreLimit        = 10.0;
  private boolean useSelected      = false;
  private int quadrantSegments     = 8;
  private String sideBarText       = "";
  private boolean fromAttribute    = false;
  private int attributeIndex       = 0;

    public OffsetCurvePlugIn() {
        super(
            I18N.get("com.vividsolutions.jump.workbench.ui.plugin.analysis.OffsetCurvePlugIn") + "...",
            IconLoader.icon("offset.png")
        );
    }

    private String categoryName = StandardCategoryNames.RESULT;

    public void setCategoryName(String value) {
        categoryName = value;
    }
  
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(
            new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
            this,
            createEnableCheck(context.getWorkbenchContext())
        );
    }
  
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
  
    public boolean execute(PlugInContext context) throws Exception {

	    MAIN_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.main-options");
	    PROCESSED_DATA = I18N.get("ui.plugin.analysis.BufferPlugIn.processed-data");
	    LAYER = I18N.get("ui.plugin.analysis.BufferPlugIn.layer");
        SELECTION = I18N.get("ui.plugin.analysis.BufferPlugIn.selection");
        SELECTION_HELP = I18N.get("ui.plugin.analysis.BufferPlugIn.selection-help");
  
        DISTANCE = I18N.get("ui.plugin.analysis.OffsetCurvePlugIn.distance");
	    FIXED_DISTANCE = I18N.get("ui.plugin.analysis.OffsetCurvePlugIn.fixed-distance");
	    FROM_ATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.get-distance-from-attribute-value");
	    ATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.attribute-to-use");
	    ATTRIBUTE_TOOLTIP = I18N.get("ui.plugin.analysis.BufferPlugIn.attribute-to-use-tooltip");
	    ROUGH_OFFSET_CURVE = I18N.get("ui.plugin.analysis.OffsetCurvePlugIn.rough-offset-curve");
	    ROUGH_OFFSET_CURVE_TOOLTIP = I18N.get("ui.plugin.analysis.OffsetCurvePlugIn.rough-offset-curve-tooltip");
	    OFFSET = I18N.get("ui.plugin.analysis.OffsetCurvePlugIn.offset");;
	    
	    OTHER_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.other-options");
        QUADRANT_SEGMENTS = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Number-of-segments-per-circle-quarter");
	    
	    ADVANCED_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.advanced-options");
	    
	    JOIN_STYLE_TITLE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-style-subtitle");
	    JOIN_STYLE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-style");
        JOIN_BEVEL = I18N.get("ui.plugin.analysis.BufferPlugIn.join-bevel");
        JOIN_MITRE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-mitre");
        JOIN_ROUND = I18N.get("ui.plugin.analysis.BufferPlugIn.join-round");
        MITRE_LIMIT = I18N.get("ui.plugin.analysis.BufferPlugIn.mitre-join-limit");
        
	    joinStyles = new ArrayList();
	    joinStyles.add(JOIN_BEVEL);
	    joinStyles.add(JOIN_MITRE);
	    joinStyles.add(JOIN_ROUND);    
	  
	    MultiTabInputDialog dialog = new MultiTabInputDialog(
	        context.getWorkbenchFrame(), getName(), MAIN_OPTIONS, true);
	    int n = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size();
	    useSelected = (n > 0);
		sideBarText = I18N.get("ui.plugin.analysis.OffsetCurvePlugIn.description");
	    setDialogValues(dialog, context);
	    updateControls(dialog);
	    GUIUtil.centreOnWindow(dialog);
	    dialog.setVisible(true);
	    if (! dialog.wasOKPressed()) { return false; }
	    getDialogValues(dialog);
	    return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{
	    monitor.allowCancellationRequests();
        FeatureSchema featureSchema = new FeatureSchema();
        featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        FeatureCollection resultFC = new FeatureDataset(featureSchema);
        // Fill inputC with features to be processed
        Collection inputC;
        if (useSelected) {
        	inputC = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
        	Feature feature = (Feature) inputC.iterator().next();
        	featureSchema = feature.getSchema();
        	inputC = PasteItemsPlugIn.conform(inputC,featureSchema);
        } else {
        	inputC = layer.getFeatureCollectionWrapper().getFeatures();
        	featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        	resultFC = new FeatureDataset(featureSchema);
        }
        // Short-circuit if input is empty
        FeatureDataset inputFD = new FeatureDataset(inputC, featureSchema);
        if (inputFD.isEmpty()) {
	    	context.getWorkbenchFrame()
	    	       .warnUser(I18N.get("ui.plugin.analysis.BufferPlugIn.empty-result-set"));
	    	return;
	    }
	    // Create offsets for each input feature
        Collection resultGeomColl = runOffset(monitor, context, inputFD);
        FeatureCollection resultFeatureColl = new FeatureDataset(featureSchema);
        Iterator iResult = resultGeomColl.iterator();
        for (Iterator iSource = inputFD.iterator(); iSource.hasNext(); ) {
        	Feature sourceFeature = (Feature) iSource.next();
        	Geometry gResult = (Geometry) iResult.next();
        	if (!(gResult == null || gResult.isEmpty())) {
        		Feature newFeature = sourceFeature.clone(true);
        		newFeature.setGeometry(gResult);
        		resultFeatureColl.add(newFeature);
        	}
        	if (monitor.isCancelRequested()) break;
        }
        resultFC = resultFeatureColl;
	    if (resultFC.isEmpty()) {
	    	context.getWorkbenchFrame()
	    	       .warnUser(I18N.get("ui.plugin.analysis.BufferPlugIn.empty-result-set"));
	    	return;
	    }
        context.getLayerManager().addCategory(categoryName);
        String name;
        if (!useSelected)
        	name = layer.getName();
        else
        	name = I18N.get("ui.MenuNames.SELECTION");
        name = name + "-" + OFFSET + "-" + offsetDistance;
        context.addLayer(categoryName, name, resultFC);
    }

    private Collection runOffset(TaskMonitor monitor, PlugInContext context, FeatureCollection fcA) throws Exception {
        int total = fcA.size();
        int count = 0;
        Collection resultColl = new ArrayList();
        for (Iterator ia = fcA.iterator(); ia.hasNext(); ) {
            monitor.report(count++, total, I18N.get("com.vividsolutions.jump.qa.diff.DiffGeometry.features"));
            if (monitor.isCancelRequested()) break;
            Feature fa = (Feature) ia.next();
            Geometry ga = fa.getGeometry();
            if (fromAttribute) {
        	    Object o = fa.getAttribute(attributeIndex);
        	    if (o instanceof Double)     		  
        		    offsetDistance = ((Double) o).doubleValue();
        	    else if (o instanceof Integer)
           		    offsetDistance = ((Integer) o).doubleValue();
            }
            // Create one offset curve per feature
            try {
                Geometry result = runOffset(ga);
                resultColl.add(result);
            } catch (Exception e) {
                String errorMessage = I18N.getMessage(
                    "ui.plugin.analysis.BufferPlugIn.error-found",
                    new Object[]{fa.getID(), ga.getCoordinate().x, ga.getCoordinate().x});
                context.getWorkbenchFrame().warnUser(errorMessage);
                throw new Exception(errorMessage, e);
            }
        }
        return resultColl;
    }

    private Geometry runOffset(Geometry a) throws TopologyException, Exception {
        GeometryFactory gf = a.getFactory();
        // If "a" is a surface, process its boundary
        if (a.getDimension() == 2) a = a.getBoundary();
        BufferParameters parameters =
            new BufferParameters(quadrantSegments, BufferParameters.CAP_FLAT, joinStyleCode, mitreLimit);
                
        Collection offsetCurves = new ArrayList();
        if (roughOffsetCurve) {
            addRoughOffsetCurves(offsetCurves, a, parameters);
            
        } else {
            addCleanOffsetCurves(offsetCurves, a, parameters);
        }
        return gf.buildGeometry(offsetCurves);
    }
    
    
    private Collection merge(Collection linestrings) {
        LineMerger merger = new LineMerger();
        merger.add(linestrings);
        return merger.getMergedLineStrings();
    }
    
    private void addCleanOffsetCurves(Collection offsetCurves, 
            Geometry sourceCurve, BufferParameters parameters) {
        parameters.setSingleSided(true);
        parameters.setQuadrantSegments(quadrantSegments);
        Geometry sidedBuffer = new BufferOp(sourceCurve, parameters)
            .getResultGeometry(offsetDistance)
            .getBoundary();
        Collection offsetSegments = new ArrayList();
        // Segments located entirely under this distance are excluded
        double lowerBound = Math.abs(offsetDistance)*Math.sin(Math.PI/(4*quadrantSegments));
        // Segments located entirely over this distance are included
        // note that the theoretical approximation made with quadrantSegments
        // is offset*cos(PI/(4*quadrantSegments) but offset*cos(PI/(2*quadrantSegments)
        // is used to make sure to include segments located on the boundary
        double upperBound = Math.abs(offsetDistance)*Math.cos(Math.PI/(2*quadrantSegments));
        for (int i = 0 ; i < sidedBuffer.getNumGeometries() ; i++) {
            Coordinate[] cc = sidedBuffer.getGeometryN(i).getCoordinates();
            PointPairDistance ppd = new PointPairDistance();
            DistanceToPoint.computeDistance(sourceCurve, cc[0], ppd);
            double dj = ppd.getDistance();
            for (int j = 1 ; j < cc.length ; j++) {
                double di = dj;
                ppd = new PointPairDistance();
                DistanceToPoint.computeDistance(sourceCurve, cc[j], ppd);
                dj = ppd.getDistance();
                // segment along or touching the source geometry : eclude it
                if (Math.max(di, dj) < lowerBound || di == 0 || dj == 0) {
                    continue;
                }
                // segment along the buffer boundary : include it
                else if (Math.min(di, dj) > upperBound) {
                    LineString segment = sourceCurve.getFactory().createLineString(
                        new Coordinate[]{cc[j-1], cc[j]});
                    offsetSegments.add(segment);
                }
                // segment entirely located inside the buffer : exclude it
                else if (Math.min(di, dj) > lowerBound && Math.max(di, dj) < upperBound) {
                    continue;
                }
                // segment with a end at the offset distance and the other
                // located within the buffer : divide it
                else {
                    // One of the coordinates is closed to but not on the source
                    // curve and the other is more or less closed to offset distance
                    divide(offsetSegments, sourceCurve, cc[j-1], cc[j], di, dj, lowerBound, upperBound);
                }
            }
        }
        offsetCurves.addAll(merge(offsetSegments));
    }
    
    
    // Recursive function to split segments located on the single-side buffer
    // boundary, but having a part of them inside the full buffer.
    private void divide(Collection offsetSegments, Geometry sourceCurve,
            Coordinate c1, Coordinate c2, double d1, double d2, double lb, double ub) {
        // I stop recursion for segment < 2*lb to exclude small segments
        // perpendicular but very close to the boundary
        if (c1.distance(c2) < 2*lb) return;
        
        Coordinate c = new Coordinate((c1.x+c2.x)/2.0, (c1.y+c2.y)/2.0);
        PointPairDistance ppd = new PointPairDistance();
        DistanceToPoint.computeDistance(sourceCurve, c, ppd);
        double d = ppd.getDistance();
        if (Math.max(d1, d) < lb) {}
        else if (Math.min(d1, d) > lb && Math.max(d1, d) < ub) {}
        else if (Math.min(d1, d) > ub) {
            LineString segment = sourceCurve.getFactory().createLineString(
                        new Coordinate[]{c1, c});
            offsetSegments.add(segment);
        }
        else {
            divide(offsetSegments, sourceCurve, c1, c, d1, d, lb, ub);
        }
        if (Math.max(d, d2) < lb) {}
        else if (Math.min(d, d2) > lb && Math.max(d, d2) < ub) {}
        else if (Math.min(d, d2) > ub) {
            LineString segment = sourceCurve.getFactory().createLineString(
                        new Coordinate[]{c, c2});
            offsetSegments.add(segment);
        }
        else {
            divide(offsetSegments, sourceCurve, c, c2, d, d2, lb, ub);
        }
    }
    
    private void addRoughOffsetCurves(Collection offsetCurves, 
            Geometry sourceCurve, BufferParameters parameters) {
        
        OffsetCurveBuilder builder = new OffsetCurveBuilder(
            sourceCurve.getFactory().getPrecisionModel(), parameters);
        
        for (int i = 0 ; i < sourceCurve.getNumGeometries() ; i++) {
            if (sourceCurve.getGeometryN(i) instanceof LineString) {
                LineString lineString = (LineString)sourceCurve.getGeometryN(i);
                Coordinate[] cc = lineString.getCoordinates();
                if (lineString.isClosed()) {
                    offsetCurves.add(lineString.getFactory().createLineString(
                        builder.getRingCurve(cc, 
                            offsetDistance>0?Position.LEFT:Position.RIGHT, 
                            Math.abs(offsetDistance))));
                }
                else {
                    offsetCurves.add(lineString.getFactory().createLineString(
                        builder.getOffsetCurve(cc, offsetDistance)));
                }
            }
        }
    }
    

    private void setDialogValues(final MultiTabInputDialog dialog, PlugInContext context) {
	    
        dialog.setSideBarDescription(sideBarText);
        
        dialog.addSubTitle(PROCESSED_DATA);
        final JComboBox layerComboBox = dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());
        dialog.addLabel(SELECTION);
        dialog.addLabel(SELECTION_HELP);
        
        dialog.addSeparator();
        dialog.addSubTitle(DISTANCE);
        final JTextField offsetDistanceTextField = dialog.addDoubleField(FIXED_DISTANCE, offsetDistance, 10, null);	    	
	    final JCheckBox fromAttributeCheckBox = dialog.addCheckBox(FROM_ATTRIBUTE, false, ATTRIBUTE_TOOLTIP);
	    final JComboBox attributeComboBox = dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NUMERIC_FILTER, ATTRIBUTE_TOOLTIP);
	    final JCheckBox roughOffsetCurveCheckBox = dialog.addCheckBox(ROUGH_OFFSET_CURVE, roughOffsetCurve, ROUGH_OFFSET_CURVE_TOOLTIP);
        
        dialog.addSeparator();
        dialog.addSubTitle(OTHER_OPTIONS);
        final JTextField quadrantSegmentsIntegerField = dialog.addIntegerField(QUADRANT_SEGMENTS, quadrantSegments, 3, null);
        final JComboBox joinStyleComboBox = dialog.addComboBox(JOIN_STYLE, joinStyle(joinStyleCode), joinStyles, null);
        final JTextField mitreLimitTextField = dialog.addDoubleField(MITRE_LIMIT, mitreLimit, 10, null);
        
        mitreLimitTextField.setEnabled(joinStyleCode == BufferParameters.JOIN_MITRE);
        quadrantSegmentsIntegerField.setEnabled(joinStyleCode == BufferParameters.JOIN_ROUND);
        //updateIcon(dialog);
        layerComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (ActionListener listener : layerComboBox.getActionListeners()) {
                    // execute other ActionListener methods before this one
                    if (listener != this) listener.actionPerformed(e);
                }
                updateControls(dialog);
            }
        });
        fromAttributeCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        joinStyleComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
    }

    private void getDialogValues(MultiInputDialog dialog) {
	    if (!useSelected) {
		    layer = dialog.getLayer(LAYER);
        }
	    offsetDistance = dialog.getDouble(FIXED_DISTANCE);
	    roughOffsetCurve = dialog.getBoolean(ROUGH_OFFSET_CURVE);
	    quadrantSegments = dialog.getInteger(QUADRANT_SEGMENTS);
	    joinStyleCode = joinStyleCode(dialog.getText(JOIN_STYLE));
	    mitreLimit = dialog.getDouble(MITRE_LIMIT);
	    if (!useSelected) {
	        boolean hasNumericAttributes = AttributeTypeFilter.NUMERIC_FILTER
	            .filter(layer.getFeatureCollectionWrapper().getFeatureSchema()).size() > 0;
	        fromAttribute = dialog.getBoolean(FROM_ATTRIBUTE);
	        if (fromAttribute && dialog.getCheckBox(FROM_ATTRIBUTE).isEnabled() && hasNumericAttributes) {
			    FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
			    String attributeName = dialog.getText(ATTRIBUTE);
			    attributeIndex = schema.getAttributeIndex(attributeName);
		    } else {
		        dialog.getCheckBox(FROM_ATTRIBUTE).setSelected(false);
			    fromAttribute = false;
		    }
	    }
    }
    
    private int joinStyleCode(String joinStyle) {
        if (joinStyle == JOIN_BEVEL) return BufferParameters.JOIN_BEVEL;
        if (joinStyle == JOIN_MITRE) return BufferParameters.JOIN_MITRE;
        return BufferParameters.JOIN_ROUND;
    }
    
    private String joinStyle(int joinStyleCode) {
        if (joinStyleCode == BufferParameters.JOIN_BEVEL) return JOIN_BEVEL;
        if (joinStyleCode == BufferParameters.JOIN_MITRE) return JOIN_MITRE;
        return JOIN_ROUND;
    }

    protected void updateControls(final MultiInputDialog dialog) {
	    getDialogValues(dialog);
	    boolean hasNumericAttributes = !useSelected && AttributeTypeFilter.NUMERIC_FILTER
	        .filter(layer.getFeatureCollectionWrapper().getFeatureSchema()).size() > 0;
	    dialog.setFieldVisible(LAYER, !useSelected);
	    dialog.setFieldVisible(SELECTION, useSelected);
	    dialog.setFieldVisible(SELECTION_HELP, useSelected);
	    dialog.setFieldEnabled(FIXED_DISTANCE, useSelected || !fromAttribute || !hasNumericAttributes);
	    dialog.setFieldEnabled(FROM_ATTRIBUTE, !useSelected && hasNumericAttributes);
	    dialog.setFieldEnabled(ATTRIBUTE, !useSelected && fromAttribute && hasNumericAttributes);
	    dialog.setFieldEnabled(QUADRANT_SEGMENTS, joinStyleCode == BufferParameters.JOIN_ROUND);
        dialog.setFieldEnabled(MITRE_LIMIT, 
                     joinStyleCode == BufferParameters.JOIN_MITRE);
    }

}
