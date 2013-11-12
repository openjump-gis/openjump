package org.openjump.core.ui.plugin.tools.generate;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import org.openjump.core.ui.images.IconLoader;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Create point along a selected linear geometry from a distance
 * and an offset values.
 * One can also repeat points at regular interval.
 */
public class LinearReferencingOnLayerPlugIn extends AbstractLinearReferencingPlugIn {

    private static final String KEY = LinearReferencingOnLayerPlugIn.class.getName();

    private String MAIN_OPTIONS;
    private String SOURCE_LAYER;

    private String ATTRIBUTES;
    private String USE_ATTRIBUTE_AS_PATH_IDENTIFIER;
    private String PATH_IDENTIFIER_TOOLTIP;
    private String PATH_IDENTIFIER_ATTRIBUTE;
    private String USE_ATTRIBUTE_TO_ORDER_PATH_SECTIONS;
    private String PATH_SECTION_TOOLTIP;
    private String PATH_SECTION_ATTRIBUTE;

    String layer_name;
    boolean use_attribute_as_path_identifier = false;
    String path_identifier_attribute;
    boolean use_attribute_to_order_path_sections = false;
    String path_section_attribute;

    public LinearReferencingOnLayerPlugIn() {
        super(I18N.get(KEY), IconLoader.icon("linearref_layer.png"));
    }

    private String categoryName = StandardCategoryNames.RESULT;

    public void setCategoryName(String value) {
        categoryName = value;
    }

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE, MenuNames.TOOLS_LINEARREFERENCING},
                getName() + "...", false, getIcon(),
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

        MAIN_OPTIONS = I18N.get(KEY + ".main-options");
        SOURCE_LAYER = I18N.get(KEY + ".source-layer");

        ATTRIBUTES                              = I18N.get(KEY + ".attributes");
        USE_ATTRIBUTE_AS_PATH_IDENTIFIER        = I18N.get(KEY + ".use-attribute-as-path-identifier");
        PATH_IDENTIFIER_TOOLTIP                 = I18N.get(KEY + ".path-identifier-tooltip");
        PATH_IDENTIFIER_ATTRIBUTE               = I18N.get(KEY + ".path-identifier-attribute");
        USE_ATTRIBUTE_TO_ORDER_PATH_SECTIONS    = I18N.get(KEY + ".use-attribute-to-order-path-sections");
        PATH_SECTION_TOOLTIP                    = I18N.get(KEY + ".path-section-tooltip");
        PATH_SECTION_ATTRIBUTE                  = I18N.get(KEY + ".path-section-attribute");

        super.execute(context);

        MultiTabInputDialog dialog = new MultiTabInputDialog(
                context.getWorkbenchFrame(), getName(), MAIN_OPTIONS, true);
        setDialogValues(dialog, context);
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
        featureSchema.addAttribute("LAYER", AttributeType.STRING);
        featureSchema.addAttribute("PATH", AttributeType.STRING);
        featureSchema.addAttribute("NUM", AttributeType.INTEGER);
        featureSchema.addAttribute("DISTANCE", AttributeType.DOUBLE);
        featureSchema.addAttribute("OFFSET", AttributeType.DOUBLE);
        FeatureCollection resultFC = new FeatureDataset(featureSchema);

        Layer layer = context.getLayerManager().getLayer(layer_name);
        List<Geometry> geometries = prepareGeometries(layer.getFeatureCollectionWrapper().getFeatures());

        for (Geometry geometry : geometries) {
            if (geometry.getDimension() == 1) {
                setPointsAlong(resultFC, layer.getName(), geometry.getUserData().toString(), geometry);
            }
            else if (geometry.getDimension() == 2) {
                setPointsAlong(resultFC, layer.getName(), geometry.getUserData().toString(), geometry.getBoundary());
            }
            // do nothing if geometry is a point
        }
        if (resultFC.size() > 0) {
            context.addLayer(categoryName, "Linear-Referencing", resultFC);
        } else {
            context.getWorkbenchFrame().warnUser(EMPTY_RESULT);
        }
    }

    private List<Geometry> prepareGeometries(List<Feature> features) {
        List<Geometry> geometries = new ArrayList<Geometry>();
        if (features.size()==0) return geometries;

        // Put all features in a map
        Map<Object,TreeMap<Object,List<Geometry>>> map = new HashMap<Object,TreeMap<Object,List<Geometry>>>();
        for (Feature feature : features) {
            Object key1 = use_attribute_as_path_identifier ?
                    feature.getAttribute(path_identifier_attribute) : feature.getID();
            if (use_attribute_as_path_identifier && key1 == null) continue;
            else if (key1 == null) key1 = "null";
            Object key2 = use_attribute_to_order_path_sections ?
                    feature.getAttribute(path_section_attribute) : feature.getID();
            if (use_attribute_as_path_identifier && key2 == null) continue;
            else if (key2 == null) key2 = "null";
            TreeMap<Object,List<Geometry>> k = map.get(key1);
            if (k == null) {
                List<Geometry> list = new ArrayList<Geometry>();
                list.add(feature.getGeometry());
                TreeMap tm = new TreeMap<Object,List<Geometry>>();
                tm.put(key2, list);
                map.put(key1, tm);
            } else {
                List<Geometry> list = k.get(key2);
                if (list == null) {
                    list = new ArrayList<Geometry>();
                }
                list.add(feature.getGeometry());
                k.put(key2,list);
            }
        }
        GeometryFactory factory = ((Feature)features.iterator().next()).getGeometry().getFactory();
        for (Map.Entry<Object,TreeMap<Object,List<Geometry>>> entry : map.entrySet()) {
            TreeMap<Object,List<Geometry>> tm = entry.getValue();
            List<LineString> sections = new ArrayList<LineString>();
            for (Map.Entry<Object,List<Geometry>> entry2 : tm.entrySet()) {
                List<Geometry> geoms = entry2.getValue();
                LineMerger merger = new LineMerger();
                merger.add(geoms);
                Collection coll = merger.getMergedLineStrings();
                for (Object obj : coll) {
                    sections.add((LineString)obj);
                }
            }
            Geometry newGeometry = factory.createMultiLineString(sections.toArray(new LineString[sections.size()]));
            newGeometry.setUserData(entry.getKey());
            geometries.add(newGeometry);
        }
        return geometries;
    }

    private void setDialogValues(final MultiTabInputDialog dialog, PlugInContext context) {

        // first pane
        Layer layer = context.getLayerManager().getLayer(layer_name);
        if (layer == null) layer = context.getCandidateLayer(0);
        dialog.setSideBarDescription(DESCRIPTION);
        dialog.addLayerComboBox(SOURCE_LAYER, layer, null, context.getLayerManager().getLayers());
        dialog.addSubTitle(DISTANCE_UNIT);
        dialog.addRadioButton(MAP_UNIT, "UNIT", map_unit, MAP_UNIT_TOOLTIP);
        dialog.addRadioButton(LINESTRING_FRACTION, "UNIT", linestring_fraction, LINESTRING_FRACTION_TOOLTIP);
        dialog.addSubTitle(DISTANCE_AND_OFFSET);
        dialog.addDoubleField(DISTANCE, distance, 6, DISTANCE_TOOLTIP);
        dialog.addDoubleField(OFFSET, offset, 6, OFFSET_TOOLTIP);
        dialog.addSeparator();
        final JCheckBox repeatCheckBox = dialog.addCheckBox(REPEAT, repeat, null);
        final JTextField repeatDistanceTextField = dialog.addDoubleField(REPEAT_DISTANCE, repeat_distance, 6, null);
        final JCheckBox addEndPointCheckBox = dialog.addCheckBox(ADD_END_POINT, add_end_point, null);
        dialog.addSeparator();
        //dialog.addCheckBox(MULTILINESTRING_SMART_ORDERING,
        //        multilinestring_smart_ordering, MULTILINESTRING_SMART_ORDERING_TOOLTIP);

        // second pane
        dialog.addPane(ATTRIBUTES);
        final JCheckBox useAttributeAsPathCheckBox = dialog.addCheckBox(
                USE_ATTRIBUTE_AS_PATH_IDENTIFIER, use_attribute_as_path_identifier, PATH_IDENTIFIER_TOOLTIP);
        final JComboBox pathIdentifierComboBox = dialog.addAttributeComboBox(
                PATH_IDENTIFIER_ATTRIBUTE, SOURCE_LAYER, AttributeTypeFilter.NUMSTRING_FILTER,null);
        pathIdentifierComboBox.setEnabled(use_attribute_as_path_identifier);
        if (use_attribute_as_path_identifier) pathIdentifierComboBox.setSelectedItem(path_identifier_attribute);

        final JCheckBox orderSectionsCheckBox = dialog.addCheckBox(
                USE_ATTRIBUTE_TO_ORDER_PATH_SECTIONS, use_attribute_to_order_path_sections, PATH_SECTION_TOOLTIP);
        final JComboBox orderSectionsComboBox = dialog.addAttributeComboBox(
                PATH_SECTION_ATTRIBUTE, SOURCE_LAYER, AttributeTypeFilter.NUMSTRING_FILTER, null);
        orderSectionsComboBox.setEnabled(use_attribute_to_order_path_sections);
        if (use_attribute_to_order_path_sections) orderSectionsComboBox.setSelectedItem(path_section_attribute);

        // components interactions
        repeatCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repeatDistanceTextField.setEnabled(repeatCheckBox.isSelected());
                addEndPointCheckBox.setEnabled(repeatCheckBox.isSelected());
            }
        });
        useAttributeAsPathCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pathIdentifierComboBox.setEnabled(useAttributeAsPathCheckBox.isSelected());
            }
        });
        orderSectionsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                orderSectionsComboBox.setEnabled(orderSectionsCheckBox.isSelected());
            }
        });

    }

    private void getDialogValues(MultiInputDialog dialog) {
        layer_name = dialog.getLayer(SOURCE_LAYER).getName();
        map_unit = dialog.getBoolean(MAP_UNIT);
        linestring_fraction = dialog.getBoolean(LINESTRING_FRACTION);
        distance = dialog.getDouble(DISTANCE);
        offset = dialog.getDouble(OFFSET);
        repeat = dialog.getBoolean(REPEAT);
        repeat_distance = dialog.getDouble(REPEAT_DISTANCE);
        if (repeat_distance == 0) repeat = false;
        add_end_point = dialog.getBoolean(ADD_END_POINT);
        //multilinestring_smart_ordering = dialog.getBoolean(MULTILINESTRING_SMART_ORDERING);

        use_attribute_as_path_identifier = dialog.getBoolean(USE_ATTRIBUTE_AS_PATH_IDENTIFIER);
        path_identifier_attribute = dialog.getText(PATH_IDENTIFIER_ATTRIBUTE);
        use_attribute_to_order_path_sections = dialog.getBoolean(USE_ATTRIBUTE_TO_ORDER_PATH_SECTIONS);
        path_section_attribute = dialog.getText(PATH_SECTION_ATTRIBUTE);
    }

}
