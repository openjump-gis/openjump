package org.openjump.core.ui.plugin.tools.geometrychange;

import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.function.Function;

import static com.vividsolutions.jump.workbench.ui.AttributeTypeFilter.*;

public class PointsToPathsPlugIn extends AbstractPlugIn implements ThreadedPlugIn {

    private static final String KEY = PointsToPathsPlugIn.class.getName();

    // Parameter names used for macro persistence
    // Mandatory
    private static final String P_LAYER_NAME          = "LayerName";
    private static final String P_USE_SELECTION       = "UseSelection";
    private static final String P_ORDER_BY_ATTRIBUTE  = "OrderBy";
    // Optional (default value provided)
    private static final String P_GROUP_BY_ATTRIBUTE  = "GroupBy";

    private static String UI_LAYER;
    private static String UI_USE_SELECTION;
    private static String UI_ORDER_BY_ATTRIBUTE;
    private static String UI_ORDER_BY_ATTRIBUTE_TT;
    private static String UI_GROUP_BY;
    private static String UI_GROUP_BY_ATTRIBUTE;
    private static String UI_GROUP_BY_ATTRIBUTE_TT;

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[]{MenuNames.TOOLS,MenuNames.TOOLS_EDIT_GEOMETRY}, getName(),
                false, null, createEnableCheck(context.getWorkbenchContext()));
    }


    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    public boolean execute(PlugInContext context) throws Exception {
        UI_USE_SELECTION = I18N.getInstance().get(KEY + ".use-selection");
        UI_LAYER = I18N.getInstance().get(KEY + ".layer");
        UI_ORDER_BY_ATTRIBUTE = I18N.getInstance().get(KEY + ".order-by-attribute");
        UI_ORDER_BY_ATTRIBUTE_TT = I18N.getInstance().get(KEY + ".order-by-attribute-tooltip");
        UI_GROUP_BY = I18N.getInstance().get(KEY + ".group-by-option");
        UI_GROUP_BY_ATTRIBUTE = I18N.getInstance().get(KEY + ".group-by-attribute");
        UI_GROUP_BY_ATTRIBUTE_TT = I18N.getInstance().get(KEY + ".group-by-attribute-tooltip");
        MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), I18N.getInstance().get(KEY), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{
        try {
            Layer layer = context.getLayerManager().getLayer((String)getParameter(P_LAYER_NAME));
            String orderByAttribute = getStringParam(P_ORDER_BY_ATTRIBUTE);

            FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            FeatureDataset dataset = new FeatureDataset(featureSchema);
            Collection<Feature> input = getBooleanParam(P_USE_SELECTION) ?
                    new ArrayList<>(
                            context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer)
                    ): layer.getFeatureCollectionWrapper().getFeatures();
            Map<Object,List<Feature>> map = new HashMap<>();
            String keyName = getStringParam(P_GROUP_BY_ATTRIBUTE);
            int countNonPoint = 0;
            for (Feature feature : input) {
                if (feature.getGeometry() instanceof Point) {
                    Object key = (keyName == null) ? "" : feature.getAttribute(keyName);
                    List<Feature> list = map.get(key);
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(key, list);
                    }
                    list.add(feature);
                } else { countNonPoint++; }
            }
            if (countNonPoint > 0) {
                context.getWorkbenchFrame().warnUser(I18N.getInstance().get(KEY + ".non-point-warning"));
            }
            Comparator<Feature> comparator = Comparator.comparing(new Function<Feature,Comparable>() {
                public Comparable apply(Feature f) {
                    if (f.getSchema().hasAttribute(orderByAttribute)) {
                        return (Comparable) f.getAttribute(orderByAttribute);
                    } else {
                        return f.getID();
                    }
                }
            });
            for (Map.Entry<Object,List<Feature>> entry : map.entrySet()) {
                List<Feature> list = entry.getValue();
                list.sort(comparator);
                Feature f = list.get(0).clone(false);
                CoordinateList coords = new CoordinateList();
                for (Feature p : list) coords.add(p.getGeometry().getCoordinate(), false);
                if (coords.size() > 1) {
                    f.setGeometry(list.get(0).getGeometry().getFactory().createLineString(coords.toCoordinateArray()));
                } else {
                    context.getWorkbenchFrame().warnUser(I18N.getInstance().get(KEY + ".invalid-path"));
                    f.setGeometry(list.get(0).getGeometry().getFactory().createPoint(coords.getCoordinate(0)));
                }
                dataset.add(f);
            }

            if (dataset.isEmpty()) {
                context.getWorkbenchFrame()
                        .warnUser(I18N.getInstance().get("ui.plugin.analysis.BufferPlugIn.empty-result-set"));
                return;
            }
            context.addLayer(StandardCategoryNames.RESULT, layer.getName() + "-paths", dataset);

        } catch(Exception e) {
            throw e;
        }
        if (context.getWorkbenchContext().getBlackboard().get(MacroManager.MACRO_STARTED, false)) {
            ((Macro)context.getWorkbenchContext().getBlackboard().get("Macro")).addProcess(this);
        }
    }

    private void setDialogValues(final MultiInputDialog dialog, final PlugInContext context) {

        final JComboBox layerComboBox = dialog.addLayerComboBox(UI_LAYER, context.getCandidateLayer(0), context.getLayerManager());
        boolean hasSelection = !context.getLayerViewPanel().getSelectionManager().getSelectedItems(context.getCandidateLayer(0)).isEmpty();
        final JCheckBox useSelectionCheckBox = dialog.addCheckBox(UI_USE_SELECTION, hasSelection);
        useSelectionCheckBox.setEnabled(hasSelection);

        dialog.addSeparator();
        final JComboBox orderByAttributeComboBox = dialog.addAttributeComboBox(UI_ORDER_BY_ATTRIBUTE, UI_LAYER,
                new AttributeTypeFilter(INTEGER + LONG + DOUBLE + STRING + DATE), UI_ORDER_BY_ATTRIBUTE_TT);

        dialog.addSeparator();
        final JCheckBox groupByCheckBox = dialog.addCheckBox(UI_GROUP_BY, false);
        final JComboBox groupByAttributeComboBox = dialog.addAttributeComboBox(UI_GROUP_BY_ATTRIBUTE, UI_LAYER,
                new AttributeTypeFilter(INTEGER + LONG + STRING), UI_GROUP_BY_ATTRIBUTE_TT);
        groupByAttributeComboBox.setEnabled(groupByCheckBox.isSelected());

        layerComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean hasSelection = !context.getLayerViewPanel().getSelectionManager().getSelectedItems(dialog.getLayer(UI_LAYER)).isEmpty();
                useSelectionCheckBox.setEnabled(hasSelection);
                if (!hasSelection) useSelectionCheckBox.setSelected(hasSelection);
                groupByCheckBox.setEnabled(AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(dialog.getLayer(UI_LAYER)).size()>1);
                groupByAttributeComboBox.setEnabled(AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(dialog.getLayer(UI_LAYER)).size()>1);
                dialog.getLayer(UI_LAYER).getFeatureCollectionWrapper().getFeatureSchema();
            }
        });

        groupByCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                groupByAttributeComboBox.setEnabled(groupByCheckBox.isSelected());
            }
        });
    }

    private void getDialogValues(final MultiInputDialog dialog) {
        addParameter(P_USE_SELECTION, dialog.getBoolean(UI_USE_SELECTION));
        addParameter(P_LAYER_NAME, dialog.getLayer(UI_LAYER).getName());
        addParameter(P_ORDER_BY_ATTRIBUTE, dialog.getValue(UI_ORDER_BY_ATTRIBUTE));
        if (dialog.getBoolean(UI_GROUP_BY)) {
            addParameter(P_GROUP_BY_ATTRIBUTE, dialog.getValue(UI_GROUP_BY_ATTRIBUTE));
        } else {
            addParameter(P_GROUP_BY_ATTRIBUTE, null);
        }
    }

}
