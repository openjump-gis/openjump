package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.tools.aggregate.*;

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

    private final static String SOURCE_LAYER              = I18N.get(KEY + ".source-layer");
    private final static String DESCRIPTION               = I18N.get(KEY + ".description");
    private final static String KEY_ATTRIBUTES            = I18N.get(KEY + ".key-attributes");
    private final static String ADD_KEY_ATTRIBUTE         = I18N.get(KEY + ".add-key-attribute");
    private final static String AGGREGATORS               = I18N.get(KEY + ".aggregators");
    private final static String AGGREGATE_FUNCTION        = I18N.get(KEY + ".aggregate-function");
    private final static String AGGREGATE_FUNCTIONS       = I18N.get(KEY + ".aggregate-functions");
    private final static String ADD_AGGREGATE_FUNCTION    = I18N.get(KEY + ".add-aggregate-function");
    private final static String REMOVE_AGGREGATE_FUNCTION = I18N.get(KEY + ".remove-aggregate-function");
    private final static String IGNORE_NULL               = I18N.get(KEY + ".ignore-null");
    private final static String PARAMETER                 = I18N.get(KEY + ".parameter");
    private final static String OUTPUT_NAME               = I18N.get(KEY + ".output-name");
    private final static String INPUT_ATTRIBUTE           = I18N.get(KEY + ".input-attribute");
    private final static String GEOMETRY_AGGREGATOR       = I18N.get(KEY + ".geometry-aggregator");


    private Layer layer;
    private FeatureCollectionAggregator fca;

    public Dissolve2PlugIn() {
    }

    public String getName() {
        return I18N.get(KEY);
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
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createTaskWindowMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1))
                .add(new EnableCheck() {
                    public String check(JComponent component) {
                        return workbenchContext
                                .getLayerableNamePanel()
                                .getSelectedLayers()[0]
                                .getFeatureCollectionWrapper()
                                .getFeatureSchema()
                                .getAttributeCount() > 1 ? null : I18N.get(KEY + ".dataset-must-have-attributes");
                    }
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

        //dialog.setSideBarImage(IconLoader.icon("dissolve_layer_icon.gif"));
        dialog.setSideBarDescription(DESCRIPTION);

        dialog.addSeparator();

        // Filter out layers without attribute
        final JComboBox layerComboBox = dialog.addLayerComboBox(SOURCE_LAYER, "", context.getLayerManager(), AttributeTypeFilter.NO_GEOMETRY_FILTER);
        if (context.getCandidateLayer(0).getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount() > 1) {
            layerComboBox.setSelectedItem(context.getCandidateLayer(0));
        }

        dialog.addSeparator();

        keyOptionPanel.setSchema(dialog.getLayer(SOURCE_LAYER).getFeatureCollectionWrapper().getFeatureSchema());
        dialog.addRow(KEY_ATTRIBUTES, new JLabel(KEY_ATTRIBUTES), keyOptionPanel, null, "",
                MultiInputDialog.LEFT_LABEL, MultiInputDialog.HORIZONTAL);

        dialog.addSeparator();

        Aggregator union = new Aggregators.Union();
        Aggregator collect = new Aggregators.Collect();
        dialog.addComboBox(GEOMETRY_AGGREGATOR, union, Arrays.asList(union, collect), "");

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
                    FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
                    try {
                        keyOptionPanel.setSchema(schema);
                        aggregateOptionPanel.setSchema(schema);
                        updateControls(dialog, keyOptionPanel, aggregateOptionPanel);
                    } catch(Exception ex) {
                        throw new RuntimeException(ex);
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
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        List<String> keyAttributes = new ArrayList<>(keyOptionPanel.getKeyAttributes());
        List<AttributeAggregator> aggregators = new ArrayList<AttributeAggregator>();
        aggregators.add(new AttributeAggregator(
                schema.getAttributeName(schema.getGeometryIndex()),
                (Aggregator)dialog.getComboBox(GEOMETRY_AGGREGATOR).getSelectedItem(),
                schema.getAttributeName(schema.getGeometryIndex())));
        aggregators.addAll(aggregateOptionPanel.getAttributeAggregators());
        int geometryTypeCount = 0;
        for (AttributeAggregator agg : aggregators) {
            if (agg.getAggregator().getOutputAttributeType() == AttributeType.GEOMETRY) {
                geometryTypeCount++;
            }
        }
        try {
            if (geometryTypeCount != 1) {
                throw new Exception(I18N.get(KEY + ".exactly-one-geometry-attribute-is-required"));
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

            jbPlus.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        keyAttributesPanel.add(new KeyAttributePanel(KeyOptionPanel.this, schema));
                        SwingUtilities.getWindowAncestor(keyAttributesPanel).pack();
                    } catch(Exception ex) {
                        throw new RuntimeException(ex);
                    }
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
                throw new Exception(I18N.get(KEY + ".no-available-attribute-to-group-by"));
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
        final JComboBox jcbInputAttributeName;
        final JButton jbRemove;

        KeyAttributePanel(final KeyOptionPanel keyOptionPanel,
                                final FeatureSchema schema) throws Exception {
            super();
            setLayout(new FlowLayout());
            this.keyOptionPanel = keyOptionPanel;
            this.schema = schema;
            jcbInputAttributeName = new JComboBox();
            for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                jcbInputAttributeName.addItem(schema.getAttributeName(i));
            }
            jcbInputAttributeName.setSelectedItem(pickKeyAttribute(schema, keyOptionPanel.getKeyAttributes()));
            jcbInputAttributeName.setPreferredSize(LARGE);
            jbRemove = new JButton();
            jbRemove.setIcon(IconLoader.icon("remove.gif"));
            jbRemove.setPreferredSize(NARROW);
            jbRemove.setToolTipText(REMOVE_AGGREGATE_FUNCTION);

            jbRemove.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (keyOptionPanel.getKeyAttributes().size() > 1) {
                            keyOptionPanel.getKeyAttributesPanel().remove(KeyAttributePanel.this);
                            SwingUtilities.getWindowAncestor(keyOptionPanel).pack();
                        } else {
                            //TODO throw message
                        }
                    }
            });

            add(jcbInputAttributeName);
            add(jbRemove);
        }

        public String getAttribute() {
            return jcbInputAttributeName.getSelectedItem().toString();
        }

        private String pickKeyAttribute(FeatureSchema schema, Set<String> set) throws Exception {
            for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                if (schema.getGeometryIndex() == i) continue;
                if (schema.getExternalPrimaryKeyIndex() == i) continue;
                if (set.contains(schema.getAttributeName(i))) continue;
                return schema.getAttributeName(i);
            }
            throw new Exception(I18N.get(KEY + ".no-more-candidate-attribute"));
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

            jbPlus.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    aggregatorsPanel.add(new AttributeAggregatePanel(
                            AggregateOptionPanel.this,
                            schema));
                    SwingUtilities.getWindowAncestor(aggregatorsPanel).pack();
                }
            });
            setLayout(new BorderLayout());
            add(northPanel, BorderLayout.NORTH);
            add(aggregatorsPanel, BorderLayout.CENTER);
        }

        public void setSchema(final FeatureSchema schema) {
            this.schema = schema;
            aggregatorsPanel.removeAll();
        }

        public JPanel getAggregatorsPanel() {
            return aggregatorsPanel;
        }

        public List<AttributeAggregator> getAttributeAggregators() {
            List<AttributeAggregator> aggregators = new ArrayList<>();
            if (aggregatorsPanel != null) {
                Component[] components = aggregatorsPanel.getComponents();
                for (Component component : components) {
                    if (component instanceof AttributeAggregatePanel) {
                        AttributeAggregatePanel aap = (AttributeAggregatePanel) component;
                        String inputName = aap.jcbInputAttributeName.getSelectedItem().toString();
                        String outputName = aap.jtfOutputAttributeName.getText();
                        Aggregator agg = ((Aggregator) aap.jcbAggregators.getSelectedItem()).clone();
                        agg.setIgnoreNull(aap.jcbIgnoreNull.isSelected());
                        if (aap.jtfParameter.isEnabled() && agg.getParameters().size() > 0) {
                            agg.setParameter(agg.getParameters().iterator().next().toString(), aap.jtfParameter.getText());
                        }
                        AttributeAggregator aggregator = new AttributeAggregator(inputName, agg, outputName);
                        aggregators.add(aggregator);
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
        JComboBox jcbInputAttributeName;
        JComboBox jcbAggregators;
        JCheckBox jcbIgnoreNull;
        JTextField jtfParameter;
        JButton jbRemove;


        AttributeAggregatePanel(final AggregateOptionPanel aggregatePanel,
                                final FeatureSchema schema) {
            this.aggregatePanel = aggregatePanel;
            this.schema = schema;

            jtfOutputAttributeName = new JTextField();
            jtfOutputAttributeName.setPreferredSize(LARGE);

            jcbInputAttributeName = new JComboBox();
            for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
                if (i == schema.getGeometryIndex()) continue;
                jcbInputAttributeName.addItem(schema.getAttributeName(i));
            }
            String defaultAttribute = jcbInputAttributeName.getSelectedItem().toString();
            jtfOutputAttributeName.setText(defaultAttribute);
            jcbInputAttributeName.setPreferredSize(LARGE);

            jcbAggregators = new JComboBox(Aggregators.getAggregators(schema.getAttributeType(defaultAttribute)).values().toArray());
            jcbAggregators.setPreferredSize(LARGE);

            Aggregator aggregator = (Aggregator)jcbAggregators.getSelectedItem();
            jcbIgnoreNull = new JCheckBox();
            jcbIgnoreNull.setPreferredSize(MEDIUM);
            jcbIgnoreNull.setSelected(aggregator.ignoreNull());
            jtfParameter = new JTextField(",");
            jtfParameter.setEditable(aggregator.getParameters().size() > 0);
            jtfParameter.setPreferredSize(NARROW);
            jbRemove = new JButton();
            jbRemove.setIcon(IconLoader.icon("remove.gif"));
            jbRemove.setToolTipText(REMOVE_AGGREGATE_FUNCTION);
            jbRemove.setPreferredSize(NARROW);

            jcbInputAttributeName.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AttributeType type = schema.getAttributeType(jcbInputAttributeName.getSelectedItem().toString());
                    jcbAggregators.setModel(new DefaultComboBoxModel(Aggregators.getAggregators(type).values().toArray()));
                    Aggregator agg = (Aggregator)jcbAggregators.getSelectedItem();
                    jcbIgnoreNull.setSelected(agg.ignoreNull());
                    jtfParameter.setEditable(agg.getParameters().size() > 0);
                    jtfOutputAttributeName.setText(jcbInputAttributeName.getSelectedItem().toString());
                }
            });

            jcbAggregators.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Aggregator agg = (Aggregator)jcbAggregators.getSelectedItem();
                    jcbIgnoreNull.setSelected(agg.ignoreNull());
                    jtfParameter.setEditable(agg.getParameters().size() > 0);
                }
            });

            jbRemove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    aggregatePanel.getAggregatorsPanel().remove(AttributeAggregatePanel.this);
                    SwingUtilities.getWindowAncestor(aggregatePanel).pack();
                }
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
