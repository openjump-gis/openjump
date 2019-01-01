package com.vividsolutions.jump.workbench.ui.style;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openjump.core.ui.util.LayerableUtil;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.TextEditor;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

public class LegendPlugIn extends AbstractPlugIn {
    /**
     * Plugin to display the legend of correct symbology used into a view
     * 
     * @author Giuseppe Aruta
     */
    private JScrollPane scrollPane = new JScrollPane();

    String taskString = I18N.get("ui.WorkbenchFrame.task");
    String layerString = I18N.get("ui.plugin.analysis.BufferPlugIn.layer");
    String labelString = I18N.get("ui.renderer.style.LabelStyle.Label");

    @Override
    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);

    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createTaskWindowMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
    }

    //   @SuppressWarnings("deprecation")
    public ImageIcon getIcon() {
        return IconLoader.icon("saig/addLegend.gif");
        // return org.openjump.core.ui.images.IconLoader.icon("save_legend.png");
    }

    WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();
    AdditionalResults frames;

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        if (getVectorLayers(context).size() == 0) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            I18N.get("org.openjump.core.ui.plugin.wms.WMSLegendPlugIn.message")
                                    + " ("
                                    + I18N.get("com.vividsolutions.jump.workbench.imagery.ReferencedImagesLayer")
                                    + ", "
                                    + I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Null-Geometries")
                                    + ",...)", I18N
                                    .get("ui.WorkbenchFrame.warning"),
                            JOptionPane.WARNING_MESSAGE);
        } else {
            AdditionalResults.addAdditionalResultAndShow(
                    getName() + "-" + I18N.get("ui.WorkbenchFrame.task") + ": "
                            + context.getTask().getName(), legend(context));
        }

        return true;

    }

    private final static String EMPTY = I18N
            .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.empty");
    private final static String POINT = I18N
            .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.point");
    private final static String POLYLINE = I18N
            .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.polyline");
    private final static String POLYGON = I18N
            .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.polygon");
    private final static String GEOMETRYCOLLECTION = I18N
            .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.geometrycollection");

    private LegendPanel legendPanel(final Layer layer, final BasicStyle style,
            FeatureCollection featureCollection) {
        final LegendPanel previewPanel = new LegendPanel(layer, style,
                featureCollection);
        previewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {

            }
        });
        return previewPanel;
    }

    private JLabel label(final String text, Font font, int increaseSize) {
        final JLabel labelValue = new JLabel();
        labelValue.setToolTipText("");

        labelValue.setBorder(BorderFactory.createEmptyBorder());
        font = new Font(labelValue.getFont().getName(), labelValue.getFont()
                .getStyle(), labelValue.getFont().getSize() + increaseSize);
        labelValue.setFont(font);
        labelValue.setText(text);
        // labelValue.setEnabled(editable);
        labelValue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    final TextEditor fc = new TextEditor();

                    fc.setSelectedFont(labelValue.getFont());
                    fc.setSelectedFontSize(labelValue.getFont().getSize());
                    fc.setSelectedFontStyle(labelValue.getFont().getStyle());
                    fc.setSelectedFontFamily(labelValue.getFont().getFamily());
                    fc.setSampleTextField(labelValue.getText());

                    fc.showDialog(
                            labelValue.getParent(),
                            I18N.get("org.openjump.core.ui.plugin.style.LegendPlugIn.modify-label"));
                    final Font labelFont = fc.getSelectedFont();
                    if (fc.wasOKPressed()) {
                        labelValue.setFont(labelFont);
                        labelValue.setText(fc.getSampleTextField().getText());

                    } else {
                        reportNothingToUndoYet(null);
                    }

                }
            }
        });
        labelValue.setForeground(new Color(20, 24, 25));
        return labelValue;
    }

    private void mapByGeomType(Feature feature,
            Map<String, FeatureCollection> map) {
        final Geometry geometry = feature.getGeometry();
        final Feature f = feature.clone(false, true);
        f.setGeometry(geometry);
        if (geometry.getGeometryType().equals("Point")) {
            map.get(POINT).add(f);
        } else if (geometry.getGeometryType().equals("MultiPoint")) {
            map.get(POINT).add(f);
        } else if (geometry.getGeometryType().equals("LineString")) {
            map.get(POLYLINE).add(f);
        } else if (geometry.getGeometryType().equals("MultiLineString")) {
            map.get(POLYLINE).add(f);
        } else if (geometry.getGeometryType().equals("Polygon")) {
            map.get(POLYGON).add(f);
        } else if (geometry.getGeometryType().equals("MultiPolygon")) {
            map.get(POLYGON).add(f);
        } else {
            map.get(GEOMETRYCOLLECTION).add(f);
        }
    }

    public JPanel layersPanel(List<Layer> layers) {
        final JPanel mainPanel = new JPanel();
        final Font plainFont = new Font(mainPanel.getFont().getName(),
                Font.PLAIN, mainPanel.getFont().getSize());
        mainPanel.setLayout(new GridBagLayout());
        final int gridx = 0;
        int gridy = 0;
        final String projectTitle = taskString + ": "
                + JUMPWorkbench.getInstance().getContext().getTask().getName();
        final JLabel projectName = label(projectTitle, plainFont, 6);
        projectName.setToolTipText(projectTitle);
        FormUtils.addRowInGBL(mainPanel, gridy, gridx + 1, projectName, false,
                true);
        gridy++;
        FormUtils.addSpacerInGBL(mainPanel, gridy++, gridx, 80, Color.white);
        gridy++;
        final Iterator<Layer> it = layers.iterator();
        while (it.hasNext()) {
            final Layer layer = it.next();
            LegendPanel previewPanel = null;
            final FeatureSchema schema = layer.getFeatureCollectionWrapper()
                    .getFeatureSchema();
            final FeatureCollection emptyFeatures = new FeatureDataset(schema);
            final FeatureCollection pointFeatures = new FeatureDataset(schema);
            final FeatureCollection polyLineFeatures = new FeatureDataset(
                    schema);
            final FeatureCollection polygonFeatures = new FeatureDataset(schema);
            final FeatureCollection geometryCollectionFeatures = new FeatureDataset(
                    schema);
            final Map<String, FeatureCollection> mapfeat = new HashMap<>();
            mapfeat.put(EMPTY, emptyFeatures);
            mapfeat.put(POINT, pointFeatures);
            mapfeat.put(POLYLINE, polyLineFeatures);
            mapfeat.put(POLYGON, polygonFeatures);
            mapfeat.put(GEOMETRYCOLLECTION, geometryCollectionFeatures);
            final FeatureCollection featureCollection = layer
                    .getFeatureCollectionWrapper();
            for (final Feature feature : featureCollection.getFeatures()) {
                mapByGeomType(feature, mapfeat);
            }
            String text;
            if (ColorThemingStyle.get(layer).isEnabled()) {
                final JLabel layerName = label(layer.getName(), plainFont, 4);
                layerName.setToolTipText(taskString
                        + " :"
                        + JUMPWorkbench.getInstance().getContext().getTask()
                                .getName() + " - " + layerString + " :"
                        + layer.getName());
                FormUtils.addRowInGBL(mainPanel, gridy++, gridx, layerName,
                        true, true);
                final Map<Object, BasicStyle> attributeValueToBasicStyleMap = ColorThemingStyle
                        .get(layer).getAttributeValueToBasicStyleMap();
                final Map<Object, String> attributeValueToLabelMap = ColorThemingStyle
                        .get(layer).getAttributeValueToLabelMap();
                final List<ColorThemingValue> colorThemingValues = new ArrayList<>();
                for (final Map.Entry<Object, BasicStyle> entry : attributeValueToBasicStyleMap
                        .entrySet()) {
                    final Object key = entry.getKey();
                    for (final Map.Entry<String, FeatureCollection> entryFeatCol : mapfeat
                            .entrySet()) {
                        if (entryFeatCol.getValue().size() != 0) {
                            if (isEsriType(layer)) {
                                colorThemingValues.add(new ColorThemingValue(
                                        key, entry.getValue(),
                                        attributeValueToLabelMap.get(key),
                                        entryFeatCol.getValue()));
                            } else {
                                final Set<String> listValues = getAvailableValues(
                                        ColorThemingStyle.get(layer),
                                        entryFeatCol.getValue());
                                for (final Object stock : listValues) {
                                    if (stock.toString().matches(
                                            entry.getKey().toString())) {
                                        colorThemingValues
                                                .add(new ColorThemingValue(key,
                                                        entry.getValue(),
                                                        attributeValueToLabelMap
                                                                .get(key),
                                                        entryFeatCol.getValue()));
                                    }
                                }
                            }
                        }
                    }
                }
                for (final ColorThemingValue entry : colorThemingValues) {
                    previewPanel = legendPanel(layer, entry.getStyle(),
                            entry.getFeatureCollection());

                    text = entry.toString();
                    final JLabel entryName = label(entry.toString(), plainFont,
                            2);
                    entryName.setToolTipText(taskString
                            + " :"
                            + JUMPWorkbench.getInstance().getContext()
                                    .getTask().getName() + " - " + layerString
                            + " :" + layer.getName() + " - " + labelString
                            + " :" + text);
                    FormUtils.addRowInGBL(mainPanel, gridy++, gridx,
                            previewPanel, entryName);
                    gridy++;
                }
            } else {
                for (final Map.Entry<String, FeatureCollection> entryFeatCol : mapfeat
                        .entrySet()) {
                    if (entryFeatCol.getValue().size() == 0) {
                        continue;
                    }
                    previewPanel = legendPanel(layer, layer.getBasicStyle(),
                            entryFeatCol.getValue());
                    previewPanel.setToolTipText(taskString
                            + " :"
                            + JUMPWorkbench.getInstance().getContext()
                                    .getTask().getName() + " - " + layerString
                            + " :" + layer.getName());

                    FormUtils.addRowInGBL(mainPanel, gridy++, gridx,
                            previewPanel, label(layer.getName(), plainFont, 2));
                    gridy++;
                }
            }
            FormUtils
                    .addSpacerInGBL(mainPanel, gridy++, gridx, 80, Color.white);
            gridy++;
        }

        mainPanel.repaint();
        return mainPanel;
    }

    @SuppressWarnings("unchecked")
    private List<Layer> getVectorLayers(PlugInContext context) {
        final List<Layer> ListVectorLayerNames = new ArrayList<Layer>();
        final Collection<Layer> layers1 = context.getLayerNamePanel()
                .selectedNodes(Layer.class);
        for (final Layer layer : layers1) {
            final Envelope env = layer.getFeatureCollectionWrapper()
                    .getEnvelope();
            if (env.isNull() | LayerableUtil.isImage(layer)) {
                // Remove Images, empty vector layers and tables
                ListVectorLayerNames.remove(layer);
            } else {
                ListVectorLayerNames.add(layer);
            }
        }
        return ListVectorLayerNames;
    }

    private JScrollPane legend(PlugInContext context) throws IOException {
        final JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel mPanel = new JPanel();

        mPanel = layersPanel(getVectorLayers(context));

        scrollPane.setBackground(Color.WHITE);
        scrollPane = new JScrollPane(mPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().getView().setBackground(Color.WHITE);
        scrollPane.getViewport().getView().setForeground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(400, context
                .getLayerViewPanel().getHeight() - 20));
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return scrollPane;
    }

    private boolean isEsriType(Layer layer) {
        if (LayerableUtil.isLinealLayer(layer)
                || LayerableUtil.isPointLayer(layer)
                || LayerableUtil.isPolygonalLayer(layer)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.style.LegendPlugIn");
    }

    public static class ColorThemingValue {
        private final Object value;
        private final BasicStyle style;
        private final String label;
        private final FeatureCollection featureCollection;

        ColorThemingValue(Object value, BasicStyle style, String label,
                FeatureCollection featureCollection) {
            this.value = value;
            this.style = style;
            Assert.isTrue(label != null);
            this.label = label;
            this.featureCollection = featureCollection;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ColorThemingValue
                    && LangUtil.bothNullOrEqual(value,
                            ((ColorThemingValue) other).value)
                    && style == ((ColorThemingValue) other).style;
        }

        public BasicStyle getStyle() {
            return style;
        }

        public FeatureCollection getFeatureCollection() {
            return featureCollection;
        }
    }

    public static Set<String> getAvailableValues(ColorThemingStyle style,
            FeatureCollection fc) {
        final Set<String> set = new TreeSet<>();
        set.add("");
        final Iterator<Feature> it = fc.iterator();
        while (it.hasNext()) {
            final Feature f = it.next();

            if (style.isEnabled()) {
                try {
                    set.add(f.getAttribute(style.getAttributeName()).toString());
                } catch (final Throwable t) {
                }

            }
        }
        return set;
    }

}
