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
import java.util.*;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
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
 * PlugIn performing buffer operations authorized by JTS BufferOp class.
 * It takes a number of options :
 * <ul>
 * <li>union selected features or all features of a selected layers</li>
 * <li>buffer size</li>
 * <li>number of segments by quadrant</li>
 * <li>union the buffered features</li>
 * <li>keep attributes of source features</li>
 * </ul>
 * Advanced options can be choosen from the second panel :
 * <ul> 
 * <li>choose end cap style</li>
 * <li>choose join style</li>
 * <li>fix a distance limit for mitre join style</li>
 * <li>one side buffer</li>
 * </ul>
 * @author vividsolutions
 * @author Micha&euml;l Michaud (refactoring, adding options of JTS 1.12)
 */
public class BufferPlugIn extends AbstractThreadedUiPlugIn {
	
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
  
  private String OTHER_OPTIONS;
  private String QUADRANT_SEGMENTS;
  private String UNION_RESULT;
  private String COPY_ATTRIBUTES;
  
  private String ADVANCED_OPTIONS;
  private String END_CAP_STYLE;
  private String CAP_FLAT;
  private String CAP_ROUND;
  private String CAP_SQUARE;
  
  private String JOIN_STYLE_TITLE;
  private String JOIN_STYLE;
  private String JOIN_BEVEL;
  private String JOIN_MITRE;
  private String JOIN_ROUND;
  private String MITRE_LIMIT;
  
  private String SINGLE_SIDED;

  private List endCapStyles;
  private List joinStyles;

  private Layer layer;
  private double bufferDistance   = 1.0;
  private int endCapStyleCode     = BufferParameters.CAP_ROUND;
  private int joinStyleCode       = BufferParameters.JOIN_ROUND;;
  private double mitreLimit       = 10.0;
  private boolean singleSided     = false;
  //private boolean exceptionThrown = false;
  private int exceptionNumber     = 0;
  private boolean useSelected     = false;
  private int quadrantSegments    = 8;
  private boolean unionResult     = false;
  private String sideBarText      = "";
  private boolean copyAttributes  = true;
  private boolean fromAttribute   = false;
  private int attributeIndex      = 0;

    public BufferPlugIn() {
        super(
            I18N.get("com.vividsolutions.jump.workbench.ui.plugin.analysis.BufferPlugIn") + "...",
            IconLoader.icon("buffer.gif")
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
        
  	    //[sstein, 16.07.2006] set again to obtain correct language
	    //[LDB: 31.08.2007] moved all initialization of strings here
	    MAIN_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.main-options");
	    PROCESSED_DATA = I18N.get("ui.plugin.analysis.BufferPlugIn.processed-data");
	    LAYER = I18N.get("ui.plugin.analysis.BufferPlugIn.layer");
        SELECTION = I18N.get("ui.plugin.analysis.BufferPlugIn.selection");
        SELECTION_HELP = I18N.get("ui.plugin.analysis.BufferPlugIn.selection-help");
  
        DISTANCE = I18N.get("ui.plugin.analysis.BufferPlugIn.distance");
	    FIXED_DISTANCE = I18N.get("ui.plugin.analysis.BufferPlugIn.fixed-distance");
	    FROM_ATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.get-distance-from-attribute-value");
	    ATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.attribute-to-use");
	    ATTRIBUTE_TOOLTIP = I18N.get("ui.plugin.analysis.BufferPlugIn.attribute-to-use-tooltip");
	    
	    
	    OTHER_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.other-options");
        QUADRANT_SEGMENTS = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Number-of-segments-per-circle-quarter");
	    UNION_RESULT = I18N.get("ui.plugin.analysis.UnionPlugIn.union");
	    COPY_ATTRIBUTES = I18N.get("ui.plugin.analysis.BufferPlugIn.preserve-attributes");
	    
	    ADVANCED_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.advanced-options");
	    END_CAP_STYLE = I18N.get("ui.plugin.analysis.BufferPlugIn.end-cap-style");
	    CAP_FLAT = I18N.get("ui.plugin.analysis.BufferPlugIn.cap-flat");
	    CAP_ROUND = I18N.get("ui.plugin.analysis.BufferPlugIn.cap-round");
	    CAP_SQUARE = I18N.get("ui.plugin.analysis.BufferPlugIn.cap-square");
	    
	    JOIN_STYLE_TITLE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-style-subtitle");
	    JOIN_STYLE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-style");
        JOIN_BEVEL = I18N.get("ui.plugin.analysis.BufferPlugIn.join-bevel");
        JOIN_MITRE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-mitre");
        JOIN_ROUND = I18N.get("ui.plugin.analysis.BufferPlugIn.join-round");
        MITRE_LIMIT = I18N.get("ui.plugin.analysis.BufferPlugIn.mitre-join-limit");
        
        SINGLE_SIDED = I18N.get("ui.plugin.analysis.BufferPlugIn.single-sided");

	  
	    endCapStyles = new ArrayList();
	    endCapStyles.add(CAP_FLAT);
	    endCapStyles.add(CAP_ROUND);
	    endCapStyles.add(CAP_SQUARE);
	  
	    joinStyles = new ArrayList();
	    joinStyles.add(JOIN_BEVEL);
	    joinStyles.add(JOIN_MITRE);
	    joinStyles.add(JOIN_ROUND);    
	  
	    MultiTabInputDialog dialog = new MultiTabInputDialog(
	        context.getWorkbenchFrame(), getName(), MAIN_OPTIONS, true);
	    int n = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size();
	    useSelected = (n > 0);
	    if (useSelected) {
		  sideBarText = SELECTION;
		}
	    else {
		  sideBarText = I18N.get("ui.plugin.analysis.BufferPlugIn.buffers-all-geometries-in-the-input-layer");
		}
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
	    // Create buffers for each input feature
        Collection resultGeomColl = runBuffer(monitor, context, inputFD);
        // Post-process result
        if (copyAttributes) {
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
        } else {
        	resultFC = FeatureDatasetFactory.createFromGeometry(resultGeomColl);
        }
	    if (unionResult) {
	        monitor.report(I18N.get("ui.plugin.analysis.BufferPlugIn.union-buffered-features"));
	    	Collection geoms = FeatureUtil.toGeometries(resultFC.getFeatures());
	    	Geometry g = UnaryUnionOp.union(geoms);
	    	geoms.clear();
	    	if (!(g == null || g.isEmpty())) geoms.add(g);
	    	resultFC = FeatureDatasetFactory.createFromGeometry(geoms);
	    }
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
        name = I18N.get("com.vividsolutions.jump.workbench.ui.plugin.analysis.BufferPlugIn") + "-" + name;
        if (endCapStyleCode != BufferParameters.CAP_ROUND) {
        	name = name + "-" + endCapStyle(endCapStyleCode);
        }
        context.addLayer(categoryName, name, resultFC);
        //if (exceptionNumber > 0) {
        //    context.getWorkbenchFrame().warnUser(
        //        I18N.get("ui.plugin.analysis.BufferPlugIn.errors-found-while-executing-buffer") + 
        //        ": " + exceptionNumber);
        //}
    }

    private Collection runBuffer(TaskMonitor monitor, PlugInContext context, FeatureCollection fcA) throws Exception {
        exceptionNumber = 0;
        int total = fcA.size();
        int count = 0;
        Collection resultColl = new ArrayList();
        BufferParameters bufferParameters = 
            new BufferParameters(quadrantSegments, endCapStyleCode, joinStyleCode, mitreLimit);
        bufferParameters.setSingleSided(singleSided);
        for (Iterator ia = fcA.iterator(); ia.hasNext(); ) {
            monitor.report(count++, total, I18N.get("com.vividsolutions.jump.qa.diff.DiffGeometry.features"));
            if (monitor.isCancelRequested()) break;
            Feature fa = (Feature) ia.next();
            Geometry ga = fa.getGeometry();
            if (fromAttribute) {
        	    Object o = fa.getAttribute(attributeIndex);
        	    if (o instanceof Double)     		  
        		    bufferDistance = ((Double) o).doubleValue();
        	    else if (o instanceof Integer)
           		    bufferDistance = ((Integer) o).doubleValue();
            }
            try {
                Geometry result = runBuffer(ga, bufferParameters);
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

    private Geometry runBuffer(Geometry a, BufferParameters param) 
                                           throws TopologyException, Exception {
        Geometry result = null;
        // Simplifying a with a Douglas-Peucker simplifier can eliminate
        // useless aligned vertices which can cause exceptions
        // see Bugs item #3488976
        // Hopefully, the overhead is be small compared to buffer computation
        a = DouglasPeuckerSimplifier.simplify(a, 0.0);
        BufferOp bufOp = new BufferOp(a, param);
        result = bufOp.getResultGeometry(bufferDistance);
        return result;
    }

    private void setDialogValues(final MultiTabInputDialog dialog, PlugInContext context) {
	    
        dialog.setSideBarDescription(sideBarText);
	    
        try{
	    	updateIcon(dialog);
        }
        catch (Exception ex){}
        
        dialog.addSubTitle(PROCESSED_DATA);
        final JComboBox layerComboBox = dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());
        dialog.addLabel(SELECTION);
        dialog.addLabel(SELECTION_HELP);
        
        dialog.addSeparator();
        dialog.addSubTitle(DISTANCE);
        final JTextField bufferDistanceTextField = dialog.addDoubleField(FIXED_DISTANCE, bufferDistance, 10, null);	    	
	    final JCheckBox fromAttributeCheckBox = dialog.addCheckBox(FROM_ATTRIBUTE, false, ATTRIBUTE_TOOLTIP);
	    final JComboBox attributeComboBox = dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NUMERIC_FILTER, ATTRIBUTE_TOOLTIP);
        
        dialog.addSeparator();
        dialog.addSubTitle(OTHER_OPTIONS);
        final JTextField quadrantSegmentsIntegerField = dialog.addIntegerField(QUADRANT_SEGMENTS, quadrantSegments, 3, null);
        final JCheckBox unionCheckBox = dialog.addCheckBox(UNION_RESULT, unionResult);
        final JCheckBox copyAttributesCheckBox = dialog.addCheckBox(COPY_ATTRIBUTES, copyAttributes);
        
        dialog.addPane(ADVANCED_OPTIONS);
        
        //dialog.addSubTitle(END_CAP_STYLE);
        final JComboBox endCapComboBox = dialog.addComboBox(END_CAP_STYLE, endCapStyle(endCapStyleCode), endCapStyles, null);
        
        dialog.addSeparator();
        //dialog.addSubTitle(JOIN_STYLE);
        final JComboBox joinStyleComboBox = dialog.addComboBox(JOIN_STYLE, joinStyle(joinStyleCode), joinStyles, null);
        final JTextField mitreLimitTextField = dialog.addDoubleField(MITRE_LIMIT, mitreLimit, 10, null);
        
        dialog.addSeparator();
        //dialog.addSubTitle(SINGLE_SIDED);
        final JCheckBox singleSidedCheckBox = dialog.addCheckBox(SINGLE_SIDED, singleSided);
        dialog.addRow(new javax.swing.JPanel());
        
        mitreLimitTextField.setEnabled(joinStyleCode == BufferParameters.JOIN_MITRE);
        quadrantSegmentsIntegerField.setEnabled(
                    (endCapStyleCode == BufferParameters.CAP_ROUND) ||
                    (joinStyleCode == BufferParameters.JOIN_ROUND));
        endCapComboBox.setEnabled(!singleSided);
        copyAttributesCheckBox.setEnabled(!unionResult);
        
        updateIcon(dialog);
        
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
        unionCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        endCapComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        joinStyleComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        singleSidedCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(dialog);
            }
        });
        
    }

    private void getDialogValues(MultiInputDialog dialog) {
	    if (!useSelected) {
		    layer = dialog.getLayer(LAYER);
        }
	    bufferDistance = dialog.getDouble(FIXED_DISTANCE);
	    endCapStyleCode = endCapStyleCode(dialog.getText(END_CAP_STYLE));
	    quadrantSegments = dialog.getInteger(QUADRANT_SEGMENTS);
	    joinStyleCode = joinStyleCode(dialog.getText(JOIN_STYLE));
	    mitreLimit = dialog.getDouble(MITRE_LIMIT);
	    singleSided = dialog.getBoolean(SINGLE_SIDED);
	    unionResult = dialog.getBoolean(UNION_RESULT);
	    copyAttributes = dialog.getBoolean(COPY_ATTRIBUTES);
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

    private int endCapStyleCode(String capStyle) {
        if (capStyle == CAP_FLAT) return BufferParameters.CAP_FLAT;
        if (capStyle == CAP_SQUARE) return BufferParameters.CAP_SQUARE;
        return BufferParameters.CAP_ROUND;
    }
    
    private String endCapStyle(int capStyleCode) {
        if (capStyleCode == BufferParameters.CAP_FLAT) return CAP_FLAT;
        if (capStyleCode == BufferParameters.CAP_SQUARE) return CAP_SQUARE;
        return CAP_ROUND;
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
    
    private Feature combine(Collection originalFeatures) {
        GeometryFactory factory = new GeometryFactory();
        Feature feature = (Feature) ((Feature) originalFeatures.iterator().next()).clone();
        feature.setGeometry(factory.createGeometryCollection(
              (Geometry[])FeatureUtil.toGeometries(originalFeatures).toArray(
                  new Geometry[originalFeatures.size()]))
        );
        return feature;
    }

    protected void updateControls(final MultiInputDialog dialog) {
	    getDialogValues(dialog);
	    updateIcon(dialog);
	    boolean hasNumericAttributes = !useSelected && AttributeTypeFilter.NUMERIC_FILTER
	        .filter(layer.getFeatureCollectionWrapper().getFeatureSchema()).size() > 0;
	    dialog.setFieldVisible(LAYER, !useSelected);
	    dialog.setFieldVisible(SELECTION, useSelected);
	    dialog.setFieldVisible(SELECTION_HELP, useSelected);
	    dialog.setFieldEnabled(FIXED_DISTANCE, useSelected || !fromAttribute || !hasNumericAttributes);
	    dialog.setFieldEnabled(FROM_ATTRIBUTE, !useSelected && hasNumericAttributes);
	    dialog.setFieldEnabled(ATTRIBUTE, !useSelected && fromAttribute && hasNumericAttributes);
	    dialog.setFieldEnabled(COPY_ATTRIBUTES, !unionResult);
	    dialog.setFieldEnabled(QUADRANT_SEGMENTS,
                    (endCapStyleCode == BufferParameters.CAP_ROUND) ||
                    (joinStyleCode == BufferParameters.JOIN_ROUND));
        dialog.setFieldEnabled(MITRE_LIMIT, 
                     joinStyleCode == BufferParameters.JOIN_MITRE);
        dialog.setFieldEnabled(END_CAP_STYLE, !singleSided);
    }
    
    private void updateIcon(MultiInputDialog dialog) {
        StringBuffer fileName = new StringBuffer("Buffer");
        if (unionResult) fileName.append("Union");
        if (singleSided) fileName.append("SingleSided");
        if (!singleSided && endCapStyleCode == BufferParameters.CAP_FLAT) fileName.append("Flat");
        else if (!singleSided && endCapStyleCode == BufferParameters.CAP_ROUND) fileName.append("Round");
        else if (!singleSided && endCapStyleCode == BufferParameters.CAP_SQUARE) fileName.append("Square");
        if (joinStyleCode == BufferParameters.JOIN_BEVEL) fileName.append("Bevel");
        else if (joinStyleCode == BufferParameters.JOIN_ROUND) fileName.append("Round");
        else if (joinStyleCode == BufferParameters.JOIN_MITRE) fileName.append("Mitre");
        dialog.setSideBarImage(
            new javax.swing.ImageIcon(IconLoader.image(fileName.toString()+".png")
                .getScaledInstance((int)(216.0*0.8), (int)(159.0*0.8), java.awt.Image.SCALE_SMOOTH))
        );
    }

}
