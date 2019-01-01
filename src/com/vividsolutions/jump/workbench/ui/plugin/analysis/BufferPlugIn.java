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
import java.lang.Exception;
import java.util.*;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.plugin.StartMacroPlugIn;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
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

    private static final int LEFT  = 1;
    private static final int RIGHT = 2;

    private String MAIN_OPTIONS;
    private String PROCESSED_DATA;
    private String LAYER;
    private String SELECTION;
    private String SELECTION_HELP;
    private String UPDATE_SOURCE;
    private String UPDATE_SOURCE_HELP;

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
    private String S_CAP_FLAT   = I18N.get("ui.plugin.analysis.BufferPlugIn.cap-flat");
    private String S_CAP_ROUND  = I18N.get("ui.plugin.analysis.BufferPlugIn.cap-round");
    private String S_CAP_SQUARE = I18N.get("ui.plugin.analysis.BufferPlugIn.cap-square");

    private String JOIN_STYLE;
    private String S_JOIN_BEVEL = I18N.get("ui.plugin.analysis.BufferPlugIn.join-bevel");
    private String S_JOIN_MITRE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-mitre");
    private String S_JOIN_ROUND = I18N.get("ui.plugin.analysis.BufferPlugIn.join-round");
    private String MITRE_LIMIT;

    private String LEFT_SINGLE_SIDED;
    private String RIGHT_SINGLE_SIDED;

    private List endCapStyles;
    private List joinStyles;

    // Parameter names used for macro persistence
    // Mandatory
    private static final String P_LAYER_NAME          = "LayerName";
    // Optional (default value provided)
    private static final String P_USE_SELECTION       = "UseSelection";
    private static final String P_UPDATE_SOURCE       = "UpdateSource";
    private static final String P_DISTANCE            = "Distance";
    private static final String P_QUADRANT_SEGMENTS   = "QuadrantSegments";
    private static final String P_END_CAP_STYLE       = "EndCapStyle";
    private static final String P_JOIN_STYLE          = "JoinStyle";
    private static final String P_MITRE_LIMIT         = "MitreLimit";
    private static final String P_LEFT_SINGLE_SIDED   = "LeftSingleSided";
    private static final String P_RIGHT_SINGLE_SIDED  = "RightSingleSided";
    private static final String P_UNION_RESULT        = "UnionResult";
    private static final String P_COPY_ATTRIBUTE      = "CopyAttribute";
    private static final String P_FROM_ATTRIBUTE      = "FromAttribute";
    private static final String P_ATTRIBUTE_INDEX     = "AttributeIndex";

    {
        addParameter(P_USE_SELECTION,     false);
        addParameter(P_UPDATE_SOURCE,     false);
        addParameter(P_DISTANCE,          1.0);
        addParameter(P_QUADRANT_SEGMENTS, 8);
        addParameter(P_END_CAP_STYLE,     BufferParameters.CAP_ROUND);
        addParameter(P_JOIN_STYLE,        BufferParameters.JOIN_ROUND);
        addParameter(P_MITRE_LIMIT,       1.0);
        addParameter(P_LEFT_SINGLE_SIDED, false);
        addParameter(P_RIGHT_SINGLE_SIDED,false);
        addParameter(P_UNION_RESULT,      false);
        addParameter(P_COPY_ATTRIBUTE,    true);
        addParameter(P_FROM_ATTRIBUTE,    false);
        addParameter(P_ATTRIBUTE_INDEX,   -1);
    }

    private int encodeCapStyle(String value) {
        if (value.equalsIgnoreCase(S_CAP_ROUND)) return BufferParameters.CAP_ROUND;
        else if (value.equalsIgnoreCase(S_CAP_FLAT)) return BufferParameters.CAP_FLAT;
        else if (value.equalsIgnoreCase(S_CAP_SQUARE)) return BufferParameters.CAP_SQUARE;
        else return -1;
    }
    private String decodeCapStyle(int value) {
        if (value == BufferParameters.CAP_ROUND) return S_CAP_ROUND;
        else if (value == BufferParameters.CAP_FLAT) return S_CAP_FLAT;
        else if (value == BufferParameters.CAP_SQUARE) return S_CAP_SQUARE;
        else return "Unknown";
    }
    private int encodeJoinStyle(String value) {
        if (value.equalsIgnoreCase(S_JOIN_ROUND)) return BufferParameters.JOIN_ROUND;
        else if (value.equalsIgnoreCase(S_JOIN_MITRE)) return BufferParameters.JOIN_MITRE;
        else if (value.equalsIgnoreCase(S_JOIN_BEVEL)) return BufferParameters.JOIN_BEVEL;
        else return -1;
    }
    private String decodeJoinStyle(int value) {
        if (value == BufferParameters.JOIN_ROUND) return S_JOIN_ROUND;
        else if (value == BufferParameters.JOIN_MITRE) return S_JOIN_MITRE;
        else if (value == BufferParameters.JOIN_BEVEL) return S_JOIN_BEVEL;
        else return "Unknown";
    }

    public BufferPlugIn() {
        super(
            I18N.get("com.vividsolutions.jump.workbench.ui.plugin.analysis.BufferPlugIn") + "...",
            IconLoader.icon("buffer.gif")
        );
    }

  
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[]{MenuNames.TOOLS,MenuNames.TOOLS_ANALYSIS}, getName(),
                false, getIcon(), createEnableCheck(context.getWorkbenchContext()));
    }


    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }


    public boolean execute(PlugInContext context) throws Exception {
        // [sstein, 16.07.2006] set again to obtain correct language
        // [LDB: 31.08.2007] moved all initialization of strings here
        MAIN_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.main-options");
        PROCESSED_DATA = I18N.get("ui.plugin.analysis.BufferPlugIn.processed-data");
        LAYER = I18N.get("ui.plugin.analysis.BufferPlugIn.layer");
        SELECTION = I18N.get("ui.plugin.analysis.BufferPlugIn.selection");
        SELECTION_HELP = I18N.get("ui.plugin.analysis.BufferPlugIn.selection-help");
        UPDATE_SOURCE = I18N.get("ui.plugin.analysis.BufferPlugIn.update-source");
        UPDATE_SOURCE_HELP = I18N.get("ui.plugin.analysis.BufferPlugIn.update-source-help");

        DISTANCE = I18N.get("ui.plugin.analysis.BufferPlugIn.distance");
        FIXED_DISTANCE = I18N.get("ui.plugin.analysis.BufferPlugIn.fixed-distance");
        FROM_ATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.get-distance-from-attribute-value");
        ATTRIBUTE = I18N.get("ui.plugin.analysis.BufferPlugIn.attribute-to-use");
        ATTRIBUTE_TOOLTIP = I18N.get("ui.plugin.analysis.BufferPlugIn.attribute-to-use-tooltip");

        OTHER_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.other-options");
        QUADRANT_SEGMENTS = I18N.get(
                "org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Number-of-segments-per-circle-quarter");
        UNION_RESULT = I18N.get("ui.plugin.analysis.UnionPlugIn.union");
        COPY_ATTRIBUTES = I18N.get("ui.plugin.analysis.BufferPlugIn.preserve-attributes");

        ADVANCED_OPTIONS = I18N.get("ui.plugin.analysis.BufferPlugIn.advanced-options");
        END_CAP_STYLE = I18N.get("ui.plugin.analysis.BufferPlugIn.end-cap-style");
        JOIN_STYLE = I18N.get("ui.plugin.analysis.BufferPlugIn.join-style");
        MITRE_LIMIT = I18N.get("ui.plugin.analysis.BufferPlugIn.mitre-join-limit");
        LEFT_SINGLE_SIDED = I18N.get("ui.plugin.analysis.BufferPlugIn.left-single-sided");
        RIGHT_SINGLE_SIDED = I18N.get("ui.plugin.analysis.BufferPlugIn.right-single-sided");

        endCapStyles = new ArrayList();
        endCapStyles.add(S_CAP_FLAT);
        endCapStyles.add(S_CAP_ROUND);
        endCapStyles.add(S_CAP_SQUARE);

        joinStyles = new ArrayList();
        joinStyles.add(S_JOIN_BEVEL);
        joinStyles.add(S_JOIN_MITRE);
        joinStyles.add(S_JOIN_ROUND);

        MultiTabInputDialog dialog = new MultiTabInputDialog(context.getWorkbenchFrame(), getName(), MAIN_OPTIONS,
                true);
        int n = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size();
        boolean useSelection = (n > 0);
        String sideBarText;
        if (useSelection) {
            sideBarText = SELECTION;
        } else {
            sideBarText = I18N.get("ui.plugin.analysis.BufferPlugIn.buffers-all-geometries-in-the-input-layer");
        }
        dialog.setSideBarDescription(sideBarText);
        addParameter(P_USE_SELECTION, useSelection);
        setDialogValues(dialog, context, useSelection);
        updateControls(context, dialog, useSelection);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog, useSelection);
        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{
        try {
            //PlugInContext context = getPlugInContext();
            Layer layer = context.getLayerManager().getLayer((String)getParameter(P_LAYER_NAME));
            // To make the macro more useful, the macro will run on the first selected layer
            // or on the first project layer if P_LAYER_NAME is not found in the project
            if (layer == null && context.getLayerNamePanel().getSelectedLayers().length > 0) {
                layer = context.getLayerNamePanel().getSelectedLayers()[0];
            }
            if (layer == null && context.getLayerManager().getLayers().size() > 0) {
                layer = context.getLayerManager().getLayer(0);
            }

            //monitor.allowCancellationRequests();
            FeatureSchema featureSchema = new FeatureSchema();
            featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
            //FeatureCollection resultFC = new FeatureDataset(featureSchema);

            // Fill inputC with features to be processed
            Collection inputC;
            if ((Boolean) getParameter(P_USE_SELECTION)) {
                inputC = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
                if (!(Boolean)getParameter(P_UPDATE_SOURCE)) {
                    Feature feature = (Feature) inputC.iterator().next();
                    featureSchema = feature.getSchema();
                    inputC = PasteItemsPlugIn.conform(inputC, featureSchema);
                }
            } else {
                inputC = layer.getFeatureCollectionWrapper().getFeatures();
                featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            }

            // Short-circuit if input is empty
            FeatureDataset inputFD = new FeatureDataset(inputC, featureSchema);
            if (inputFD.isEmpty()) {
                context.getWorkbenchFrame()
                        .warnUser(I18N.get("ui.plugin.analysis.BufferPlugIn.empty-result-set"));
                return;
            }

            Map<Integer,Geometry> resultMap = runBuffer(monitor, context, inputFD);

            if ((Boolean)getParameters().get(P_UPDATE_SOURCE)) {
                updateSourceLayer(monitor, context, layer, inputFD, resultMap);
            }
            else {
                createNewLayer(monitor, context, inputFD, resultMap, featureSchema);
            }
        } catch(Exception e) {
            throw e;
        }
        if (context.getWorkbenchContext().getBlackboard().get(MacroManager.MACRO_STARTED, false)) {
            ((Macro)context.getWorkbenchContext().getBlackboard().get("Macro")).addProcess(this);
        }
    }

    private void createNewLayer(TaskMonitor monitor, PlugInContext context, FeatureCollection inputFD,
                                Map<Integer,Geometry> resultMap, FeatureSchema featureSchema) {
        FeatureCollection resultFC = new FeatureDataset(featureSchema);
        if ((Boolean) getParameter(P_COPY_ATTRIBUTE)) {
            FeatureCollection resultFeatureColl = new FeatureDataset(featureSchema);
            for (Iterator iSource = inputFD.iterator(); iSource.hasNext(); ) {
                Feature sourceFeature = (Feature) iSource.next();
                Geometry gResult = resultMap.get(sourceFeature.getID());
                if (!(gResult == null || gResult.isEmpty())) {
                    Feature newFeature = sourceFeature.clone(false);
                    newFeature.setGeometry(gResult);
                    resultFeatureColl.add(newFeature);
                }
                if (monitor.isCancelRequested()) break;
            }
            resultFC = resultFeatureColl;
        } else {
            resultFC = FeatureDatasetFactory.createFromGeometry(resultMap.values());
        }
        if ((Boolean)getParameter(P_UNION_RESULT)) {
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
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        String name;
        if (!(Boolean) getParameter("UseSelection"))
            name = (String)getParameter(P_LAYER_NAME);
        else
            name = I18N.get("ui.MenuNames.SELECTION");
        name = I18N.get("com.vividsolutions.jump.workbench.ui.plugin.analysis.BufferPlugIn") + "-" + name;
        //if (endCapStyleCode != BufferParameters.CAP_ROUND) {
        //    name = name + "-" + endCapStyle(endCapStyleCode);
        //}
        context.addLayer(StandardCategoryNames.RESULT, name, resultFC);
    }

    private void updateSourceLayer(TaskMonitor monitor, PlugInContext context,
                                   Layer layer, FeatureCollection input, Map<Integer,Geometry> map) {
        EditTransaction transaction = new EditTransaction(new LinkedHashSet<Feature>(),
                "Buffer", layer, true, true, context.getLayerViewPanel().getContext());
        for (Iterator it = input.iterator() ; it.hasNext(); ) {
            Feature feature = (Feature)it.next();
            Geometry newGeometry = map.get(feature.getID());
            transaction.modifyFeatureGeometry(feature, newGeometry);
            //if (newGeometry != null) feature.setGeometry(newGeometry);
        }
        transaction.commit();
    }


    private Map<Integer,Geometry> runBuffer(TaskMonitor monitor, PlugInContext context, FeatureCollection fcA) throws Exception {
        int quadrantSegments = (Integer)getParameter(P_QUADRANT_SEGMENTS);
        int endCapStyleCode = (Integer)getParameter(P_END_CAP_STYLE);
        int joinStyleCode = (Integer)getParameter(P_JOIN_STYLE);
        double mitreLimit = (Double)getParameter(P_MITRE_LIMIT);
        boolean leftSingleSided = (Boolean)getParameter(P_LEFT_SINGLE_SIDED);
        boolean rightSingleSided = (Boolean)getParameter(P_RIGHT_SINGLE_SIDED);
        double bufferDistance = (Double)getParameter(P_DISTANCE);
        boolean fromAttribute = (Boolean)getParameter(P_FROM_ATTRIBUTE);
        int attributeIndex = (Integer)getParameter(P_ATTRIBUTE_INDEX);
        int total = fcA.size();
        int count = 0;
        //Collection resultColl = new ArrayList();
        Map<Integer,Geometry> map = new HashMap<Integer, Geometry>(fcA.size());
        BufferParameters bufferParameters = 
            new BufferParameters(quadrantSegments, endCapStyleCode, joinStyleCode, mitreLimit);
        bufferParameters.setSingleSided(leftSingleSided || rightSingleSided);
        int side = 0;
        if (leftSingleSided) side += LEFT;
        if (rightSingleSided) side += RIGHT;
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
                if (side == 0) {
                    Geometry result = runBuffer(ga, bufferParameters, 0, bufferDistance);
                    map.put(fa.getID(), result);
                }
                if (side == LEFT) {
                    Geometry result = runBuffer(ga, bufferParameters, LEFT, bufferDistance);
                    map.put(fa.getID(), result);
                }
                if (side == RIGHT) {
                    Geometry result = runBuffer(ga, bufferParameters, RIGHT, bufferDistance);
                    map.put(fa.getID(), result);
                }
                if (side == LEFT + RIGHT) {
                    Geometry left = runBuffer(ga, bufferParameters, LEFT, bufferDistance);
                    Geometry right = runBuffer(ga, bufferParameters, RIGHT, bufferDistance);
                    map.put(fa.getID(), left.getFactory().createGeometryCollection(new Geometry[]{left,right}));
                }
            } catch (Exception e) {
                String errorMessage = I18N.getMessage(
                    "ui.plugin.analysis.BufferPlugIn.error-found",
                    new Object[]{fa.getID(), ga.getCoordinate().x, ga.getCoordinate().x});
                context.getWorkbenchFrame().warnUser(errorMessage);
                throw new Exception(errorMessage, e);
            }
        }
        return map;
    }

    private Geometry runBuffer(Geometry a, BufferParameters param, int side, double bufferDistance) throws Exception {
        Geometry result;
        // Simplifying a with a Douglas-Peucker simplifier can eliminate
        // useless aligned vertices which can cause exceptions
        // see Bugs item #3488976
        // Hopefully, the overhead is small compared to buffer computation
        a = DouglasPeuckerSimplifier.simplify(a, 0.0);
        BufferOp bufOp = new BufferOp(a, param);
        if ((side == LEFT && bufferDistance < 0) || (side == RIGHT && bufferDistance > 0)) {
            result = bufOp.getResultGeometry(-bufferDistance);
        } else{
            result = bufOp.getResultGeometry(bufferDistance);
        }
        return result;
    }

    private void setDialogValues(final MultiTabInputDialog dialog,
                                 final PlugInContext context,
                                 final boolean useSelection) {
        try{
            updateIcon(dialog);
        }
        catch (Exception ex){}
        
        dialog.addSubTitle(PROCESSED_DATA);
        final JComboBox layerComboBox = dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());
        dialog.addLabel(SELECTION);
        dialog.addLabel(SELECTION_HELP);
        final JCheckBox updateCheckBox = dialog.addCheckBox(UPDATE_SOURCE, (Boolean)getParameter(P_UPDATE_SOURCE), UPDATE_SOURCE_HELP);
        
        dialog.addSeparator();
        dialog.addSubTitle(DISTANCE);
        final JTextField bufferDistanceTextField = dialog.addDoubleField(FIXED_DISTANCE, (Double)getParameter(P_DISTANCE), 10, null);
        final JCheckBox fromAttributeCheckBox = dialog.addCheckBox(FROM_ATTRIBUTE, false, ATTRIBUTE_TOOLTIP);
        final JComboBox attributeComboBox = dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NUMERIC_FILTER, ATTRIBUTE_TOOLTIP);
        
        dialog.addSeparator();
        dialog.addSubTitle(OTHER_OPTIONS);
        final JTextField quadrantSegmentsIntegerField = dialog.addIntegerField(QUADRANT_SEGMENTS, (Integer)getParameter(P_QUADRANT_SEGMENTS), 3, null);
        final JCheckBox unionCheckBox = dialog.addCheckBox(UNION_RESULT, (Boolean)getParameter(P_UNION_RESULT));
        final JCheckBox copyAttributesCheckBox = dialog.addCheckBox(COPY_ATTRIBUTES, (Boolean)getParameter(P_COPY_ATTRIBUTE));
        
        dialog.addPane(ADVANCED_OPTIONS);
        
        //dialog.addSubTitle(END_CAP_STYLE);
        final JComboBox endCapComboBox = dialog.addComboBox(END_CAP_STYLE,
                decodeCapStyle((Integer)getParameter(P_END_CAP_STYLE)), endCapStyles, null);
        
        dialog.addSeparator();
        //dialog.addSubTitle(JOIN_STYLE);
        final JComboBox joinStyleComboBox = dialog.addComboBox(JOIN_STYLE,
                decodeJoinStyle((Integer)getParameter(P_JOIN_STYLE)), joinStyles, null);
        final JTextField mitreLimitTextField = dialog.addDoubleField(MITRE_LIMIT, (Double)getParameter(P_MITRE_LIMIT), 10, null);
        
        dialog.addSeparator();
        //dialog.addSubTitle(SINGLE_SIDED);
        final JCheckBox leftSingleSidedCheckBox = dialog.addCheckBox(LEFT_SINGLE_SIDED, (Boolean)getParameter(P_LEFT_SINGLE_SIDED));
        final JCheckBox rightSingleSidedCheckBox = dialog.addCheckBox(RIGHT_SINGLE_SIDED, (Boolean)getParameter(P_RIGHT_SINGLE_SIDED));
        dialog.addRow(new javax.swing.JPanel());
        
        mitreLimitTextField.setEnabled((Integer)getParameter(P_JOIN_STYLE) == BufferParameters.JOIN_MITRE);
        quadrantSegmentsIntegerField.setEnabled(
                    ((Integer)getParameter(P_END_CAP_STYLE) == BufferParameters.CAP_ROUND) ||
                    ((Integer)getParameter(P_JOIN_STYLE) == BufferParameters.JOIN_ROUND));
        endCapComboBox.setEnabled(!((Boolean)getParameter(P_LEFT_SINGLE_SIDED) ||
                                    (Boolean)getParameter(P_RIGHT_SINGLE_SIDED)));
        copyAttributesCheckBox.setEnabled(!(Boolean)getParameter(P_UNION_RESULT));
        
        updateIcon(dialog);
        
        layerComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (ActionListener listener : layerComboBox.getActionListeners()) {
                    // execute other ActionListener methods before this one
                    if (listener != this) listener.actionPerformed(e);
                }
                updateControls(context, dialog, useSelection);
            }
        });
        updateCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(context, dialog, useSelection);
            }
        });
        fromAttributeCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(context, dialog, useSelection);
            }
        });
        unionCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(context, dialog, useSelection);
            }
        });
        endCapComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(context, dialog, useSelection);
            }
        });
        joinStyleComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(context, dialog, useSelection);
            }
        });
        leftSingleSidedCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(context, dialog, useSelection);
            }
        });
        rightSingleSidedCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls(context, dialog, useSelection);
            }
        });
        
    }

    private void getDialogValues(final MultiInputDialog dialog, final boolean useSelection) {
        Layer layer = null;
        if (!useSelection) {
            layer = dialog.getLayer(LAYER);
        }
        boolean updateSource = dialog.getBoolean(UPDATE_SOURCE);
        double bufferDistance = dialog.getDouble(FIXED_DISTANCE);
        int endCapStyleCode = encodeCapStyle(dialog.getText(END_CAP_STYLE));
        int quadrantSegments = dialog.getInteger(QUADRANT_SEGMENTS);
        int joinStyleCode = encodeJoinStyle(dialog.getText(JOIN_STYLE));
        double mitreLimit = dialog.getDouble(MITRE_LIMIT);
        boolean leftSingleSided = dialog.getBoolean(LEFT_SINGLE_SIDED);
        boolean rightSingleSided = dialog.getBoolean(RIGHT_SINGLE_SIDED);
        boolean unionResult = dialog.getBoolean(UNION_RESULT);
        boolean copyAttributes = dialog.getBoolean(COPY_ATTRIBUTES);
        boolean fromAttribute = getParameter(P_FROM_ATTRIBUTE) != null ? (Boolean) getParameter(P_FROM_ATTRIBUTE)
                : false;
        int attributeIndex = getParameter(P_ATTRIBUTE_INDEX) != null ? (Integer) getParameter(P_ATTRIBUTE_INDEX) : -1;
        if (!useSelection) {
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
        addParameter(P_USE_SELECTION, useSelection);
        addParameter(P_UPDATE_SOURCE, updateSource);
        addParameter(P_DISTANCE, bufferDistance);
        addParameter(P_LAYER_NAME, useSelection ? null : layer.getName());
        addParameter(P_QUADRANT_SEGMENTS, quadrantSegments);
        addParameter(P_END_CAP_STYLE, endCapStyleCode);
        addParameter(P_JOIN_STYLE, joinStyleCode);
        addParameter(P_MITRE_LIMIT, mitreLimit);
        addParameter(P_LEFT_SINGLE_SIDED, leftSingleSided);
        addParameter(P_RIGHT_SINGLE_SIDED, rightSingleSided);
        addParameter(P_UNION_RESULT, unionResult);
        addParameter(P_COPY_ATTRIBUTE, copyAttributes);
        addParameter(P_FROM_ATTRIBUTE, fromAttribute);
        addParameter(P_ATTRIBUTE_INDEX, attributeIndex);
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

    protected void updateControls(PlugInContext context, final MultiInputDialog dialog, boolean useSelection) {
        getDialogValues(dialog, useSelection);
        updateIcon(dialog);
        boolean hasNumericAttributes = !useSelection
                && AttributeTypeFilter.NUMERIC_FILTER.filter(context.getLayerManager()
                        .getLayer((String) getParameter(P_LAYER_NAME)).getFeatureCollectionWrapper().getFeatureSchema())
                        .size() > 0;
        dialog.setFieldVisible(LAYER, !useSelection);
        dialog.setFieldVisible(SELECTION, useSelection);
        dialog.setFieldVisible(UPDATE_SOURCE, useSelection);
        dialog.setFieldVisible(SELECTION_HELP, useSelection);
        dialog.setFieldEnabled(FIXED_DISTANCE,
                useSelection || !(Boolean) getParameter(P_FROM_ATTRIBUTE) || !hasNumericAttributes);
        dialog.setFieldEnabled(FROM_ATTRIBUTE, !useSelection && hasNumericAttributes);
        dialog.setFieldEnabled(ATTRIBUTE,
                !useSelection && (Boolean) getParameter(P_FROM_ATTRIBUTE) && hasNumericAttributes);
        dialog.setFieldEnabled(COPY_ATTRIBUTES,
                !(Boolean) getParameter(P_UNION_RESULT) && !(Boolean) getParameter(P_UPDATE_SOURCE));
        dialog.setFieldEnabled(UNION_RESULT, !(Boolean) getParameter(P_UPDATE_SOURCE));
        dialog.setFieldEnabled(QUADRANT_SEGMENTS,
                ((Integer) getParameter(P_END_CAP_STYLE) == BufferParameters.CAP_ROUND)
                        || ((Integer) getParameter(P_JOIN_STYLE) == BufferParameters.JOIN_ROUND));
        dialog.setFieldEnabled(MITRE_LIMIT, (Integer) getParameter(P_JOIN_STYLE) == BufferParameters.JOIN_MITRE);
        dialog.setFieldEnabled(END_CAP_STYLE,
                !((Boolean) getParameter(P_LEFT_SINGLE_SIDED) || (Boolean) getParameter(P_RIGHT_SINGLE_SIDED)));
    }
    
    private void updateIcon(MultiInputDialog dialog) {
        StringBuffer fileName = new StringBuffer("Buffer");
        if ((Boolean)getParameter(P_UNION_RESULT)) fileName.append("Union");
        if ((Boolean)getParameter(P_LEFT_SINGLE_SIDED) || (Boolean)getParameter(P_RIGHT_SINGLE_SIDED)) fileName.append("SingleSided");
        boolean notSingleSided = !(Boolean)getParameter(P_LEFT_SINGLE_SIDED) && !(Boolean)getParameter(P_RIGHT_SINGLE_SIDED);
        if (notSingleSided && (Integer)getParameter(P_END_CAP_STYLE) == BufferParameters.CAP_FLAT) fileName.append("Flat");
        else if (notSingleSided && (Integer)getParameter(P_END_CAP_STYLE) == BufferParameters.CAP_ROUND) fileName.append("Round");
        else if (notSingleSided && (Integer)getParameter(P_END_CAP_STYLE) == BufferParameters.CAP_SQUARE) fileName.append("Square");
        if ((Integer)getParameter(P_JOIN_STYLE) == BufferParameters.JOIN_BEVEL) fileName.append("Bevel");
        else if ((Integer)getParameter(P_JOIN_STYLE) == BufferParameters.JOIN_ROUND) fileName.append("Round");
        else if ((Integer)getParameter(P_JOIN_STYLE) == BufferParameters.JOIN_MITRE) fileName.append("Mitre");
        dialog.setSideBarImage(
            new javax.swing.ImageIcon(IconLoader.image(fileName.toString()+".png")
                .getScaledInstance((int)(216.0*0.8), (int)(159.0*0.8), java.awt.Image.SCALE_SMOOTH))
        );
    }

}
