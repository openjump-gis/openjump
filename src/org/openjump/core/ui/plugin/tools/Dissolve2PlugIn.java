package org.openjump.core.ui.plugin.tools;

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
import org.locationtech.jts.geom.Geometry;
import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.tools.aggregate.*;
import static com.vividsolutions.jump.workbench.ui.AttributeTypeFilter.NO_GEOMETRY_FILTER;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

/**
 * Plugin to mimic a SQL GROUP BY operation.
 */
public class Dissolve2PlugIn extends AbstractThreadedUiPlugIn {

    private final static String KEY = Dissolve2PlugIn.class.getName();

    private final static Dimension LARGE = new Dimension(110,22);
    private final static Dimension MEDIUM = new Dimension(22,22);
    private final static Dimension NARROW = new Dimension(22,22);

    private final static String SOURCE_LAYER              = I18N.JUMP.get(KEY + ".source-layer");
    private final static String DESCRIPTION               = I18N.JUMP.get(KEY + ".description");
    private final static String KEY_ATTRIBUTES            = I18N.JUMP.get(KEY + ".key-attributes");
    private final static String ADD_KEY_ATTRIBUTE         = I18N.JUMP.get(KEY + ".add-key-attribute");
    private final static String AGGREGATORS               = I18N.JUMP.get(KEY + ".aggregators");
    private final static String AGGREGATE_FUNCTION        = I18N.JUMP.get(KEY + ".aggregate-function");
    private final static String AGGREGATE_FUNCTIONS       = I18N.JUMP.get(KEY + ".aggregate-functions");
    private final static String ADD_AGGREGATE_FUNCTION    = I18N.JUMP.get(KEY + ".add-aggregate-function");
    private final static String REMOVE_AGGREGATE_FUNCTION = I18N.JUMP.get(KEY + ".remove-aggregate-function");
    private final static String IGNORE_NULL               = I18N.JUMP.get(KEY + ".ignore-null");
    private final static String PARAMETER                 = I18N.JUMP.get(KEY + ".parameter");
    private final static String OUTPUT_NAME               = I18N.JUMP.get(KEY + ".output-name");
    private final static String INPUT_ATTRIBUTE           = I18N.JUMP.get(KEY + ".input-attribute");
    private final static String GEOMETRY_AGGREGATOR       = I18N.JUMP.get(KEY + ".geometry-aggregator");

    private final static String FLOATING_PRECISION_MODEL    = I18N.JUMP.get("jts.use-floating-point-precision-model");
    private final static String FLOATING_PRECISION_MODEL_TT = I18N.JUMP.get("jts.use-floating-point-precision-model-tt");
    private final static String FIXED_PRECISION_MODEL       = I18N.JUMP.get("jts.use-fixed-precision-model");
    private final static String FIXED_PRECISION_MODEL_TT    = I18N.JUMP.get("jts.use-fixed-precision-model-tt");
    private final static String PRECISION                   = I18N.JUMP.get("jts.fixed-precision");
    private final static String PRECISION_TT                = I18N.JUMP.get("jts.fixed-precision-tt");


    private Layer layer;
    private FeatureCollectionAggregator fca;
    private boolean floatingPrecision = true;
    private boolean fixedPrecision = false;
    private double precision = 1000.0;

    public Dissolve2PlugIn() {
    }

    public String getName() {
        return I18N.JUMP.get(KEY);
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
                getName() + "...", false, IconLoader.icon("groupby.png"),
                createEnableCheck(context.getWorkbenchContext()), -1);
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createTaskWindowMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                .add(component -> {
                    // At least one layer must have a non geometric attribute
                    return NO_GEOMETRY_FILTER.filter(workbenchContext.getLayerManager()).size() > 0 ? null :
                        I18N.JUMP.get(KEY + ".dataset-must-have-attributes");
                });
    }


    @Override
    public boolean execute(PlugInContext context) throws Exception {
        final MultiTabInputDialog dialog = new MultiTabInputDialog(
                context.getWorkbenchFrame(), getName(), KEY_ATTRIBUTES, true);
        final KeyOptionPanel keyOptionPanel = new KeyOptionPanel();
        final AggregateOptionPanel aggregateOptionPanel = new AggregateOptionPanel();
        initDialog(dialog, context, keyOptionPanel, aggregateOptionPanel);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog, keyOptionPanel, aggregateOptionPanel);
        return true;
    }

    private void initDialog(final MultiTabInputDialog dialog, PlugInContext context,
                            final KeyOptionPanel keyOptionPanel,
                            final AggregateOptionPanel aggregateOptionPanel) throws Exception {

        dialog.setSideBarDescription(DESCRIPTION);

        dialog.addSeparator();

        if (layer == null) {
            if (context.getCandidateLayer(0).getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()>1) {
                layer = context.getCandidateLayer(0);
            } else {
                List<Layer> candidates = NO_GEOMETRY_FILTER.filter(workbenchContext.getLayerManager());
                layer = candidates.get(0);
            }
        }

        // Filter out layers without attribute
        final JComboBox<Layer> layerComboBox = dialog.addLayerComboBox(SOURCE_LAYER, layer, "",
            NO_GEOMETRY_FILTER.filter(context.getLayerManager()));

        dialog.addSeparator();

        keyOptionPanel.setSchema(dialog.getLayer(SOURCE_LAYER).getFeatureCollectionWrapper().getFeatureSchema());
        dialog.addRow(KEY_ATTRIBUTES, new JLabel(KEY_ATTRIBUTES), keyOptionPanel, null, "",
                MultiInputDialog.LEFT_LABEL, MultiInputDialog.HORIZONTAL);

        dialog.addSeparator();

        Aggregator<Geometry> union = new Aggregators.Union();
        Aggregator<Geometry> collect = new Aggregators.Collect();
        dialog.addComboBox(GEOMETRY_AGGREGATOR, union, Arrays.asList(union, collect), "");
        JRadioButton floatingPrecisionRB = dialog
            .addRadioButton(FLOATING_PRECISION_MODEL,"MODEL", floatingPrecision, FLOATING_PRECISION_MODEL_TT);
        JRadioButton fixedPrecisionRB = dialog
            .addRadioButton(FIXED_PRECISION_MODEL,"MODEL", fixedPrecision, FIXED_PRECISION_MODEL_TT);
        final JTextField precisionTF = dialog.addDoubleField(PRECISION,precision, 12, PRECISION_TT);
        precisionTF.setEnabled(fixedPrecision);
        floatingPrecisionRB.addActionListener(e -> {
            FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            try {
                keyOptionPanel.setSchema(schema);
                aggregateOptionPanel.setSchema(schema);
                precisionTF.setEnabled(!floatingPrecisionRB.isSelected());
                updateControls(dialog, keyOptionPanel, aggregateOptionPanel);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        fixedPrecisionRB.addActionListener(e -> {
            FeatureSchema schema = dialog.getLayer(SOURCE_LAYER).getFeatureCollectionWrapper().getFeatureSchema();
            try {
                keyOptionPanel.setSchema(schema);
                aggregateOptionPanel.setSchema(schema);
                precisionTF.setEnabled(fixedPrecisionRB.isSelected());
                updateControls(dialog, keyOptionPanel, aggregateOptionPanel);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        dialog.addSeparator();

        // Aggregators definition
        final JPanel aggregationOptionsTab = dialog.addPane(AGGREGATORS);
        aggregationOptionsTab.setLayout(new BorderLayout());
        aggregateOptionPanel.setSchema(dialog.getLayer(SOURCE_LAYER).getFeatureCollectionWrapper().getFeatureSchema());
        aggregationOptionsTab.add(aggregateOptionPanel, BorderLayout.NORTH);

        dialog.pack();

        layerComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    layer = (Layer)layerComboBox.getSelectedItem();
                    if (layer != null) {
                        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
                        try {
                            keyOptionPanel.setSchema(schema);
                            aggregateOptionPanel.setSchema(schema);
                            updateControls(dialog, keyOptionPanel, aggregateOptionPanel);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        });

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog,
                                 KeyOptionPanel keyOptionPanel,
                                 AggregateOptionPanel aggregateOptionPanel) {
        layer = dialog.getLayer(SOURCE_LAYER);
        floatingPrecision = dialog.getBoolean(FLOATING_PRECISION_MODEL);
        fixedPrecision = dialog.getBoolean(FIXED_PRECISION_MODEL);
        precision = dialog.getDouble(PRECISION);
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        List<String> keyAttributes = new ArrayList<>(keyOptionPanel.getKeyAttributes());
        List<AttributeAggregator> aggregators = new ArrayList<>();
        // We use the special geometry aggregator if geometry is not a key attribute
        //System.out.println(keyAttributes);
        if (!keyAttributes.contains(schema.getAttributeName(schema.getGeometryIndex()))) {
            //System.out.println("add");
            Aggregator<?> geomAggregator =
                (Aggregator<?>)dialog.getComboBox(GEOMETRY_AGGREGATOR).getSelectedItem();
            if (geomAggregator instanceof Aggregators.Union && fixedPrecision) {
                geomAggregator.setParameter("precision", precision);
            }
            AttributeAggregator attAggregator = new AttributeAggregator(
                schema.getAttributeName(schema.getGeometryIndex()),
                geomAggregator,
                schema.getAttributeName(schema.getGeometryIndex()));
            aggregators.add(attAggregator);
        }
        aggregators.addAll(aggregateOptionPanel.getAttributeAggregators());
        int geometryTypeCount = 0;
        for (String key : keyAttributes) {
            if (schema.getAttributeType(key) == AttributeType.GEOMETRY) {
                geometryTypeCount++;
            }
        }
        for (AttributeAggregator agg : aggregators) {
            if (agg.getAggregator().getOutputAttributeType() == AttributeType.GEOMETRY) {
                geometryTypeCount++;
            }
        }
        try {
            if (geometryTypeCount != 1) {
                throw new Exception(I18N.getInstance().get(KEY + ".exactly-one-geometry-attribute-is-required"));
            }
            fca = new FeatureCollectionAggregator(layer.getFeatureCollectionWrapper(), keyAttributes, aggregators);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updateControls(MultiInputDialog dialog,
                                KeyOptionPanel keyAttributeOptionPanel,
                                AggregateOptionPanel aggregateOptionPanel) throws Exception {
        getDialogValues(dialog, keyAttributeOptionPanel, aggregateOptionPanel);
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        keyAttributeOptionPanel.setSchema(schema);
        aggregateOptionPanel.setSchema(schema);
        dialog.pack();
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

        monitor.allowCancellationRequests();
        monitor.report(getName());
        FeatureCollection resultfc = fca.getAggregatedFeatureCollection();
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        String newLayerName = layer.getName() + "-grouped";
        context.addLayer(StandardCategoryNames.RESULT, newLayerName, resultfc);
    }

    class KeyOptionPanel extends JPanel {

        final JPanel northPanel;
        final JPanel keyAttributesPanel;

        FeatureSchema schema;

        KeyOptionPanel() {
            super();
            setLayout(new BorderLayout());
            northPanel = new JPanel(new BorderLayout());

            JPanel titleLine = new JPanel();

            JLabel jlInputName = new JLabel(ADD_KEY_ATTRIBUTE);
            jlInputName.setPreferredSize(LARGE);
            titleLine.add(jlInputName);

            JButton jbPlus = new JButton(IconLoader.icon("plus.gif"));
            jbPlus.setToolTipText(ADD_KEY_ATTRIBUTE);
            jbPlus.setPreferredSize(NARROW);
            titleLine.add(jbPlus);
            northPanel.add(titleLine, BorderLayout.CENTER);

            keyAttributesPanel = new JPanel();
            keyAttributesPanel.setLayout(new BoxLayout(keyAttributesPanel, BoxLayout.Y_AXIS));

            jbPlus.addActionListener(e -> {
                try {
                    keyAttributesPanel.add(new KeyAttributePanel(KeyOptionPanel.this, schema));
                    SwingUtilities.getWindowAncestor(keyAttributesPanel).pack();
                } catch(Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            JPanel westPanel = new JPanel(new BorderLayout());
            add(westPanel, BorderLayout.WEST);
            westPanel.add(northPanel, BorderLayout.NORTH);
            westPanel.add(keyAttributesPanel, BorderLayout.CENTER);
        }

        public void setSchema(final FeatureSchema schema) throws Exception {
            this.schema = schema;
            if (schema.getAttributeCount()<2) {
                throw new Exception(I18N.getInstance().get(KEY + ".no-available-attribute-to-group-by"));
            }
            keyAttributesPanel.removeAll();
            keyAttributesPanel.add(new KeyAttributePanel(this, schema));
        }

        public JPanel getKeyAttributesPanel() {
            return keyAttributesPanel;
        }

        public Set<String> getKeyAttributes() {
            Set<String> keyAttributeSet = new LinkedHashSet<>();
            if (keyAttributesPanel != null) {
                Component[] components = keyAttributesPanel.getComponents();
                for (Component component : components) {
                    if (component instanceof KeyAttributePanel) {
                        KeyAttributePanel kap = (KeyAttributePanel) component;
                        keyAttributeSet.add(kap.getAttribute());
                    }
                }
            }
            return keyAttributeSet;
        }
    }

    class KeyAttributePanel extends JPanel {
        final KeyOptionPanel keyOptionPanel;
        final FeatureSchema schema;
        final JComboBox<String> jcbInputAttributeName;
        final JButton jbRemove;

        KeyAttributePanel(final KeyOptionPanel keyOptionPanel,
                                final FeatureSchema schema) throws Exception {
            super();
            setLayout(new FlowLayout());
            this.keyOptionPanel = keyOptionPanel;
            this.schema = schema;
            jcbInputAttributeName = new JComboBox<>();
            for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                jcbInputAttributeName.addItem(schema.getAttributeName(i));
            }
            jcbInputAttributeName.setSelectedItem(pickKeyAttribute(schema, keyOptionPanel.getKeyAttributes()));
            jcbInputAttributeName.setPreferredSize(LARGE);
            jbRemove = new JButton();
            jbRemove.setIcon(IconLoader.icon("remove.gif"));
            jbRemove.setPreferredSize(NARROW);
            jbRemove.setToolTipText(REMOVE_AGGREGATE_FUNCTION);

            jbRemove.addActionListener(e -> {
                if (keyOptionPanel.getKeyAttributes().size() > 1) {
                    keyOptionPanel.getKeyAttributesPanel().remove(KeyAttributePanel.this);
                    SwingUtilities.getWindowAncestor(keyOptionPanel).pack();
                }
            });

            add(jcbInputAttributeName);
            add(jbRemove);
        }

        public String getAttribute() {
            Object selection = jcbInputAttributeName.getSelectedItem();
            if (selection != null) {
                return selection.toString();
            } else {
                return null;
            }
        }

        private String pickKeyAttribute(FeatureSchema schema, Set<String> set) throws Exception {
            for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                if (schema.getGeometryIndex() == i) continue;
                if (schema.getExternalPrimaryKeyIndex() == i) continue;
                if (set.contains(schema.getAttributeName(i))) continue;
                return schema.getAttributeName(i);
            }
            throw new Exception(I18N.getInstance().get(KEY + ".no-more-candidate-attribute"));
        }
    }

    class AggregateOptionPanel extends JPanel {
        final JPanel northPanel;
        final JPanel aggregatorsPanel;
        FeatureSchema schema;

        AggregateOptionPanel() {
            super();
            northPanel = new JPanel(new BorderLayout());
            northPanel.add(new JLabel("<html><b>" + AGGREGATE_FUNCTIONS + "</b></html>"), BorderLayout.NORTH);

            JPanel titleLine = new JPanel();
            JLabel jlOutputName = new JLabel(OUTPUT_NAME);
            jlOutputName.setPreferredSize(LARGE);
            titleLine.add(jlOutputName);

            JLabel jlInputName = new JLabel(INPUT_ATTRIBUTE);
            jlInputName.setPreferredSize(LARGE);
            titleLine.add(jlInputName);

            JLabel jlFunction = new JLabel(AGGREGATE_FUNCTION);
            jlFunction.setPreferredSize(LARGE);
            titleLine.add(jlFunction);

            JLabel jlIgnoreNull = new JLabel(IGNORE_NULL);
            jlIgnoreNull.setPreferredSize(MEDIUM);
            jlIgnoreNull.setToolTipText(IGNORE_NULL);
            titleLine.add(jlIgnoreNull);

            JLabel jlFunctionParameter = new JLabel(PARAMETER);
            jlFunctionParameter.setPreferredSize(NARROW);
            jlFunctionParameter.setToolTipText(PARAMETER);
            titleLine.add(jlFunctionParameter);

            JButton jbPlus = new JButton(org.openjump.core.ui.images.IconLoader.icon("plus.gif"));
            jbPlus.setToolTipText(ADD_AGGREGATE_FUNCTION);
            jbPlus.setPreferredSize(NARROW);
            titleLine.add(jbPlus);
            northPanel.add(titleLine, BorderLayout.CENTER);

            aggregatorsPanel = new JPanel();
            aggregatorsPanel.setLayout(new BoxLayout(aggregatorsPanel, BoxLayout.Y_AXIS));

            jbPlus.addActionListener(e -> {
                aggregatorsPanel.add(new AttributeAggregatePanel(
                        AggregateOptionPanel.this,
                        schema));
                SwingUtilities.getWindowAncestor(aggregatorsPanel).pack();
            });
            setLayout(new BorderLayout());
            add(northPanel, BorderLayout.NORTH);
            add(aggregatorsPanel, BorderLayout.CENTER);
        }

        void setSchema(final FeatureSchema schema) {
            this.schema = schema;
            aggregatorsPanel.removeAll();
        }

        JPanel getAggregatorsPanel() {
            return aggregatorsPanel;
        }

        List<AttributeAggregator> getAttributeAggregators() {
            List<AttributeAggregator> aggregators = new ArrayList<>();
            if (aggregatorsPanel != null) {
                Component[] components = aggregatorsPanel.getComponents();
                for (Component component : components) {
                    if (component instanceof AttributeAggregatePanel) {
                        AttributeAggregatePanel aap = (AttributeAggregatePanel) component;
                        Object selectedAttributeName = aap.jcbInputAttributeName.getSelectedItem();
                        Object selectedAggregator = aap.jcbAggregators.getSelectedItem();
                        if (selectedAttributeName != null && selectedAggregator != null) {
                            String inputName = selectedAttributeName.toString();
                            String outputName = aap.jtfOutputAttributeName.getText();
                            Aggregator<?> agg = ((Aggregator<?>)selectedAggregator).clone();
                            agg.setIgnoreNull(aap.jcbIgnoreNull.isSelected());
                            if (aap.jtfParameter.isEnabled() && agg.getParameters().size() > 0) {
                                agg.setParameter(agg.getParameters().iterator().next(), aap.jtfParameter.getText());
                            }
                            AttributeAggregator aggregator = new AttributeAggregator(inputName, agg, outputName);
                            aggregators.add(aggregator);
                        }
                    }
                }
            }
            return aggregators;
        }
    }

    class AttributeAggregatePanel extends JPanel {

        final AggregateOptionPanel aggregatePanel;
        final FeatureSchema schema;

        JTextField jtfOutputAttributeName;
        JComboBox<String> jcbInputAttributeName;
        JComboBox<Aggregator<?>> jcbAggregators;
        JCheckBox jcbIgnoreNull;
        JTextField jtfParameter;
        JButton jbRemove;


        AttributeAggregatePanel(final AggregateOptionPanel aggregatePanel,
                                final FeatureSchema schema) {
            this.aggregatePanel = aggregatePanel;
            this.schema = schema;

            jtfOutputAttributeName = new JTextField();
            jtfOutputAttributeName.setPreferredSize(LARGE);

            jcbInputAttributeName = new JComboBox<>();
            for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                if (i == schema.getGeometryIndex()) continue;
                jcbInputAttributeName.addItem(schema.getAttributeName(i));
            }
            String defaultAttribute = null;
            if (jcbInputAttributeName.getSelectedItem() != null) {
                defaultAttribute = jcbInputAttributeName.getSelectedItem().toString();
            }
            jtfOutputAttributeName.setText(defaultAttribute);
            jcbInputAttributeName.setPreferredSize(LARGE);

            jcbAggregators = new JComboBox<Aggregator<?>>(
                    Aggregators.getAggregators(schema.getAttributeType(defaultAttribute))
                            .values().toArray(new Aggregator[0]));
            jcbAggregators.setPreferredSize(LARGE);

            Aggregator<?> aggregator = (Aggregator<?>)jcbAggregators.getSelectedItem();
            jcbIgnoreNull = new JCheckBox();
            jcbIgnoreNull.setPreferredSize(MEDIUM);
            jcbIgnoreNull.setSelected(aggregator == null || aggregator.ignoreNull());
            jtfParameter = new JTextField(",");
            jtfParameter.setEditable(aggregator != null && aggregator.getParameters().size() > 0);
            jtfParameter.setPreferredSize(NARROW);
            jbRemove = new JButton();
            jbRemove.setIcon(IconLoader.icon("remove.gif"));
            jbRemove.setToolTipText(REMOVE_AGGREGATE_FUNCTION);
            jbRemove.setPreferredSize(NARROW);

            jcbInputAttributeName.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AttributeType type = schema.getAttributeType(jcbInputAttributeName.getSelectedItem().toString());
                    jcbAggregators.setModel(new DefaultComboBoxModel<Aggregator<?>>(
                            Aggregators.getAggregators(type).values().toArray(new Aggregator[0])));
                    Aggregator<?> agg = (Aggregator<?>)jcbAggregators.getSelectedItem();
                    if (agg != null) {
                        jcbIgnoreNull.setSelected(agg.ignoreNull());
                        jtfParameter.setEditable(agg.getParameters().size() > 0);
                    }
                    jtfOutputAttributeName.setText(jcbInputAttributeName.getSelectedItem().toString());
                }
            });

            jcbAggregators.addActionListener(e -> {
                Aggregator<?> agg = (Aggregator<?>)jcbAggregators.getSelectedItem();
                if (agg != null) {
                    jcbIgnoreNull.setSelected(agg.ignoreNull());
                    jtfParameter.setEditable(agg.getParameters().size() > 0);
                }
            });

            jbRemove.addActionListener(e -> {
                aggregatePanel.getAggregatorsPanel().remove(AttributeAggregatePanel.this);
                SwingUtilities.getWindowAncestor(aggregatePanel).pack();
            });

            add(jtfOutputAttributeName);
            add(jcbInputAttributeName);
            add(jcbAggregators);
            add(jcbIgnoreNull);
            add(jtfParameter);
            add(jbRemove);

        }
    }

}
