package org.openjump.core.ui.plugin.tools;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.operation.overlayng.UnaryUnionNG;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Dissolve plugin is used to union features with same attribute value(s).
 * It can optionnaly merge unioned LineStrings (union just create MultiLineStrings by default).
 * <br>
 * There are three options available :
 * <ul>
 *      <li>Merge linestring : get simple linestring instead of multilinestrings
 *      for adjacent linestrings having same attribute values</li>
 *      <li>Simple geometries : decompose multi-geometries into several simple geometries</li>
 *      <li>Remove unused attributes : remove unused attributes from the result schema (anyway,
 *      values will be null for these attributes</li>
 * </ul>
 */
public class DissolvePlugIn extends AbstractThreadedUiPlugIn {

    private final String LAYER                       = I18N.JUMP.get("ui.plugin.analysis.DissolvePlugIn.source-layer");
    private final String DESCRIPTION                 = I18N.JUMP.get("ui.plugin.analysis.DissolvePlugIn.description");
    private final String ATTRIBUTES                  = I18N.JUMP.get("ui.plugin.analysis.DissolvePlugIn.attributes");
    private final String MERGE_LINESTRINGS           = I18N.JUMP.get("ui.plugin.analysis.DissolvePlugIn.merge-linestrings");
    private final String SIMPLE_GEOMETRIES           = I18N.JUMP.get("ui.plugin.analysis.DissolvePlugIn.decompose-multi-geometries");
    private final String REMOVE_UNUSED_ATT           = I18N.JUMP.get("ui.plugin.analysis.DissolvePlugIn.remove-unused-attributes");

    private final String FLOATING_PRECISION_MODEL    = I18N.JUMP.get("jts.use-floating-point-precision-model");
    private final String FLOATING_PRECISION_MODEL_TT = I18N.JUMP.get("jts.use-floating-point-precision-model-tt");
    private final String FIXED_PRECISION_MODEL       = I18N.JUMP.get("jts.use-fixed-precision-model");
    private final String FIXED_PRECISION_MODEL_TT    = I18N.JUMP.get("jts.use-fixed-precision-model-tt");
    private final String PRECISION                   = I18N.JUMP.get("jts.fixed-precision");
    private final String PRECISION_TT                = I18N.JUMP.get("jts.fixed-precision-tt");

    private Layer layer;
    private boolean floatingPrecision = true;
    private boolean fixedPrecision = false;
    private double precision = 1000.0;
    private boolean merge_linestrings = true;
    private boolean simple_geometries = false;
    private boolean remove_unused_att = true;
    private final List<String> attributes = new ArrayList<>();

    private JPanel attributePanel;



    public DissolvePlugIn() {
    }

    public String getName() {
        return I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn");
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
                getName() + "...", false, IconLoader.icon("dissolve_layer_icon.gif"),
                createEnableCheck(context.getWorkbenchContext()), -1);
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createTaskWindowMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
    }


    @Override
    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), getName(), true);
        initDialog(dialog, context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }

    private void initDialog(final MultiInputDialog dialog, PlugInContext context) {

        //dialog.setSideBarImage(IconLoader.icon("dissolve_layer_icon.gif"));
        dialog.setSideBarDescription(I18N.getInstance().get(DESCRIPTION));

        final JComboBox<Layer> layerComboBox =
            dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());

        JRadioButton floatingPrecisionRB = dialog
            .addRadioButton(FLOATING_PRECISION_MODEL,"MODEL", floatingPrecision, FLOATING_PRECISION_MODEL_TT);
        JRadioButton fixedPrecisionRB = dialog
            .addRadioButton(FIXED_PRECISION_MODEL,"MODEL", fixedPrecision, FIXED_PRECISION_MODEL_TT);
        dialog.addDoubleField(PRECISION,precision, 12, PRECISION_TT);
        floatingPrecisionRB.addActionListener(e -> updateControls(dialog));
        fixedPrecisionRB.addActionListener(e -> updateControls(dialog));

        dialog.addSubTitle(I18N.getInstance().get(ATTRIBUTES));
        attributePanel = new JPanel();
        attributePanel.setLayout(new BoxLayout(attributePanel, BoxLayout.Y_AXIS));
        final JComponent scrollableAttributeChecks = new JScrollPane(attributePanel);
        scrollableAttributeChecks.setPreferredSize(new Dimension(320,160));
        scrollableAttributeChecks.setMinimumSize(new Dimension(320,160));
        dialog.addRow(scrollableAttributeChecks);

        dialog.addSeparator();

        final JCheckBox mergeLineStringsCheckBox = dialog.addCheckBox(MERGE_LINESTRINGS, merge_linestrings,
                I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn.merge-linestrings-tooltip"));

        final JCheckBox simpleGeometriesCheckBox = dialog.addCheckBox(SIMPLE_GEOMETRIES, simple_geometries,
                I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn.decompose-multi-geometries-tooltip"));

        final JCheckBox removeUnusedAttCheckBox = dialog.addCheckBox(REMOVE_UNUSED_ATT, remove_unused_att,
                I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn.remove-unused-attributes-tooltip"));


        updateControls(dialog);

        layerComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (ActionListener listener : layerComboBox.getActionListeners()) {
                    // execute other ActionListener methods before this one
                    if (listener != this) listener.actionPerformed(e);
                }
                updateControls(dialog);
            }
        });
        //mergeLineStringsCheckBox.addActionListener(e -> updateControls(dialog));

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layer = dialog.getLayer(LAYER);
        attributes.clear();
        floatingPrecision = dialog.getBoolean(FLOATING_PRECISION_MODEL);
        fixedPrecision = dialog.getBoolean(FIXED_PRECISION_MODEL);
        precision = dialog.getDouble(PRECISION);
        for (int i = 0 ; i < attributePanel.getComponentCount() ; i++) {
            if (attributePanel.getComponent(i) instanceof JCheckBox) {
                String name = ((JCheckBox)attributePanel.getComponent(i)).getText();
                boolean checked = ((JCheckBox)attributePanel.getComponent(i)).isSelected();
                if (checked) attributes.add(name);
            }
        }
        merge_linestrings = dialog.getBoolean(MERGE_LINESTRINGS);
        simple_geometries = dialog.getBoolean(SIMPLE_GEOMETRIES);
        remove_unused_att = dialog.getBoolean(REMOVE_UNUSED_ATT);
    }

    private void updateControls(MultiInputDialog dialog) {
        getDialogValues(dialog);
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        attributePanel.removeAll();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++){
            if (schema.getGeometryIndex() == i) continue; // skip geometry attribute
            attributePanel.add(new JCheckBox(schema.getAttributeName(i), false));
        }
        dialog.setFieldEnabled(PRECISION, dialog.getBoolean(FIXED_PRECISION_MODEL));
        dialog.pack();
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

        monitor.allowCancellationRequests();

        Collection<Feature> inputC = layer.getFeatureCollectionWrapper().getFeatures();
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        FeatureDataset inputFC = new FeatureDataset(inputC, schema);

        if (inputFC.getFeatures().size() < 2) {
            context.getWorkbenchFrame().warnUser(
                    I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn.needs-two-features-or-more"));
            return;
        }

        // Create the schema for the output dataset
        FeatureSchema newSchema;
        if (remove_unused_att) {
            newSchema = new FeatureSchema();
            newSchema.addAttribute(schema.getAttributeName(schema.getGeometryIndex()), AttributeType.GEOMETRY);
            for (String name : attributes) {
                newSchema.addAttribute(name, schema.getAttributeType(name));
            }
        }
        else {
            newSchema = schema;
        }

        // Order features by attribute value in a map
        Map<List<Object>,FeatureCollection> map = new HashMap<>();
        monitor.report(I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn"));
        for (Feature f : inputFC.getFeatures()) {
            List<Object> key = computeKeyFromAttributes(f, attributes);
            if (!map.containsKey(key)) {
                FeatureCollection fd = new FeatureDataset(inputFC.getFeatureSchema());
                fd.add(f);
                map.put(key, fd);
            }
            else {
                (map.get(key)).add(f);
            }
        }

        // Computing the result
        int count = 1;
        FeatureCollection resultfc = new FeatureDataset(newSchema);
        for (List<Object> key : map.keySet()) {
            monitor.report(I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn.computing-union") + " (" + count++ + "/" + map.size() + ")");
            FeatureCollection fca = map.get(key);
            if (fca.size() > 0) {
                List<Geometry> geometries = union(context, monitor, fca);
                for (Geometry geom : geometries) {
                    Feature newFeature = new BasicFeature(newSchema);
                    newFeature.setGeometry(geom);
                    for (int i = 0 ; i < attributes.size() ; i++) {
                        newFeature.setAttribute(attributes.get(i), key.get(i));
                    }
                    resultfc.add(newFeature);
                }
            }
        }
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        String newLayerName = layer.getName() + "-dissolve";
        context.addLayer(StandardCategoryNames.RESULT, newLayerName, resultfc);
    }

    private List<Object> computeKeyFromAttributes(Feature feature, List<String> attributes) {
        List<Object> list = new ArrayList<>();
        for (String attribute : attributes) {
            list.add(feature.getAttribute(attribute));
        }
        return list;
    }

    /**
     * New method for union. Uses new UnaryUnionOp which is much more
     * efficient than Geometry.union() for large datasets.
     */
    private List<Geometry> union(PlugInContext context, TaskMonitor monitor, FeatureCollection fc) {
        // Eliminate invalid geometries and log their fid
        List<Geometry> geometries  = new ArrayList<>();
        for (Feature f : fc.getFeatures()) {
            Geometry g = f.getGeometry();
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                Geometry component = g.getGeometryN(i);
                if (component.isValid()) {
                    geometries.add(g.getGeometryN(i));
                }
                else {
                    geometries.add(GeometryFixer.fix(component));
                }
            }
        }
        //Geometry unioned = UnaryUnionOp.union(geometries); // old way
        Geometry unioned;
        //unioned = UnaryUnionOp.union(geometries); // old algorithm (not as robust)
        if (floatingPrecision) {
            unioned = OverlayNGRobust.union(geometries);
        } else {
            unioned = UnaryUnionNG.union(geometries, new PrecisionModel(precision));
        }
        // Post process linestring if merged is wanted
        if (merge_linestrings) {
            geometries.clear();
            List<Geometry> points      = new ArrayList<>();
            List<Geometry> lineStrings = new ArrayList<>();
            List<Geometry> polygons    = new ArrayList<>();
            decompose(unioned, points, lineStrings, polygons);
            LineMerger merger = new LineMerger();
            merger.add(lineStrings);
            geometries.addAll(points);
            geometries.addAll(merger.getMergedLineStrings());
            geometries.addAll(polygons);
            unioned = unioned.getFactory().buildGeometry(geometries);
        }
        geometries.clear();
        if (simple_geometries) {
            for (int i = 0; i < unioned.getNumGeometries(); i++) {
                geometries.add(unioned.getGeometryN(i));
            }
        }
        else {
            geometries.add(unioned);
        }
        return geometries;
    }

    private void decompose(Geometry geometry, List<Geometry> dim0, List<Geometry> dim1, List<Geometry> dim2) {
        if (geometry instanceof GeometryCollection) {
            for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
                decompose(geometry.getGeometryN(i), dim0, dim1, dim2);
            }
        }
        else if (geometry.getDimension() == 2) dim2.add(geometry);
        else if (geometry.getDimension() == 1) dim1.add(geometry);
        else if (geometry.getDimension() == 0) dim0.add(geometry);
        else {
            assert false : "Should never reach here";
        }
    }

}
