/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2005 Integrated Systems Analysts, Inc.
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
package org.openjump.core.ui.plugin.layer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;
import org.apache.commons.io.FilenameUtils;
import org.openjump.core.ccordsys.utils.ProjUtils;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.openjump.core.ui.swing.DetachableInternalFrame;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.AlphaSetting;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

import de.latlon.deejump.wfs.jump.WFSLayer;

public class NewLayerPropertiesPlugIn extends AbstractPlugIn {
    private static String LAYER_PROPERTIES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Layer-Properties");
    private static String INFO = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Info");
    private static String LAYERS = I18N
            .get("demo.layerviewpanel.MapTab.layers");
    private static String NUMBER_OF_FEATURES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Features");
    private static String NUMBER_OF_POINTS = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Points");
    private static String GEOMETRY_TYPE = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Geometry-Type");
    private static String NUMBER_OF_ATTRIBUTES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Attributes");
    private static String DATASOURCE_CLASS = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.DataSource-Class");
    private static String SOURCE_PATH = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Source-Path");
    private static String NO_FEATURES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.No-Features");
    private static String FEATURES = I18N
            .get("ui.AttributeTablePanel.features");
    private static String TRANSPARENCY = I18N
            .get("ui.renderer.style.ColorThemingPanel.transparency");
    private static String MULTIPLE_GEOMETRY_TYPES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-geometry-types");
    private static String MULTIPLE_SOURCE_TYPES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-Source-Types");
    private static String NULL_GEOMETRIES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Null-Geometries");
    private static String NOT_SAVED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not-Saved");
    private static String AVERAGE_PER_LAYER = " ("
            + I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.average-per-layer")
            + ")";
    private static String MULTIPLE_SOURCES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-Sources");
    private static String STYLES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Styles");
    private static String PROPORTIONAL_TRANSPARENCY_ADJUSTER = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Proportional-Transparency-Adjustment");
    private static String CHARSET = I18N
            .get("org.openjump.core.ui.io.file.DataSourceFileLayerLoader.charset");
    private static String EXTENT = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.extent");
    private static String XMIN = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmin");
    private static String YMIN = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymin");
    private static String XMAX = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmax");
    private static String YMAX = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymax");
    private static String NAME = I18N
            .get("jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel.Name");
    private static String IMAGE = I18N
            .get("ui.plugin.imagery.AddImageLayerPlugIn.Image");
    private static String PROJECTION_UNSPECIFIED = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.unknown_projection");
    private static String GEO_METADATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.geographic_metadata");
    private static String CRS = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.SRS");
    private static String COORDINATE_SYSTEM = I18N
            .get("datasource.FileDataSourceQueryChooser.coordinate-system-of-file");
    private final String NODATASOURCELAYER = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.nodatasourcelayer.message");
    private final String MODIFIED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Modified");
    private static String CANCEL = I18N.get("ui.OKCancelPanel.cancel");
    private static String OK = I18N.get("ui.OKCancelPanel.ok");
    private static String CATALOG_FILE = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Catalog_file");
    private static String SRID_CODE = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.SRID_code");
    private static String WFS_LAYER_NAME = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Local_layer_name");
    private static String WFS_SERVER = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.URL");
    private static String WEB_SERVICE = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Web_Service");
    private static String DATASOURCE = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.DataSource");

    private Layer[] layers;
    private Envelope extent;
    private int[] currTransArray;
    private boolean styleChanged = false;
    public static final String Test = "Test";

    // 2016 April 13th - 0.1 version [Giuseppe Aruta giuseppe_aruta@yahoo.it]

    public ImageIcon getIcon() {
        return IconLoader.icon("information_16x16.png");
    }

    public String getName() {
        return LAYER_PROPERTIES;
    }

    public boolean execute(PlugInContext context) throws Exception {

        styleChanged = false;
        layers = context.getSelectedLayers();
        extent = context.getSelectedLayerEnvelope();
        currTransArray = new int[layers.length];
        for (int i = 0; i < layers.length; i++) {
            currTransArray[i] = (255 - getAlpha(layers[i]));
        }
        final ArrayList<Collection<Style>> oldStyleList = new ArrayList<>(
                layers.length);
        for (Layer layer : layers) {
            oldStyleList.add(layer.cloneStyles());
        }
        InfoPanel infoPanel = new InfoPanel();
        StylePanel stylePanel = new StylePanel();
        infoPanel.setPreferredSize(new Dimension(350, 200));
        JTabbedPane tabbedPane = new JTabbedPane();
        final DetachableInternalFrame frame = new DetachableInternalFrame();
        if (layers.length == 1) {
            frame.setTitle(LAYER_PROPERTIES + ": " + layers[0].getName());
        } else {
            frame.setTitle(LAYER_PROPERTIES + ": " + MULTIPLE_SOURCES);
        }
        frame.setIconifiable(true);
        frame.setFrameIcon(IconLoader.icon("information_16x16.png"));
        Border mainComponentBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabbedPane.setBorder(mainComponentBorder);
        tabbedPane.addTab(INFO, getIcon(), infoPanel, "");
        tabbedPane.addTab(TRANSPARENCY, null, stylePanel, "");
        JButton okButton = new JButton(OK) {
            private static final long serialVersionUID = 1L;

            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        JButton cancelButton = new JButton(CANCEL) {
            private static final long serialVersionUID = 1L;

            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (oldStyleList != null) {
                    int j = 0;
                    for (Layer layer : layers) {
                        layer.setStyles(oldStyleList.get(j++));
                    }
                }
                frame.dispose();
            }
        });
        JPanel okCancelPane = new JPanel();
        okCancelPane.add(okButton);
        okCancelPane.add(cancelButton);
        if (!styleChanged) {
            reportNothingToUndoYet(context);
        }
        frame.add(tabbedPane, "Center");
        frame.add(okCancelPane, "South");
        frame.setClosable(true);
        frame.setResizable(true);
        frame.setMaximizable(true);
        frame.setSize(550, 400);
        frame.setVisible(true);
        frame.setIcon(true);
        context.getWorkbenchFrame().addInternalFrame(frame, true, true);
        return true;
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck().add(
                checkFactory
                        .createWindowWithSelectionManagerMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
    }

    public static ColorThemingStyle getColorThemingStyleIfEnabled(Layer layer) {
        ColorThemingStyle someStyle = null;
        for (Style style : layer.getStyles()) {
            if (style instanceof ColorThemingStyle && style.isEnabled()) {
                someStyle = (ColorThemingStyle) style;
            }
        }
        return someStyle;
    }

    public static int getAlpha(Layer layer) {
        ColorThemingStyle cts = getColorThemingStyleIfEnabled(layer);
        if (cts != null) {
            return cts.getDefaultStyle().getAlpha();
        }
        List<Style> styles = layer.getStylesIfEnabled(AlphaSetting.class);
        Iterator<Style> localIterator = styles.iterator();
        if (localIterator.hasNext()) {
            Style style = localIterator.next();
            return ((AlphaSetting) style).getAlpha();
        }
        return 128;
    }

    public static void setAlpha(Layer layer, int alpha) {
        List<Style> styles = layer.getStyles(AlphaSetting.class);
        for (Style style : styles) {
            ((AlphaSetting) style).setAlpha(alpha);
        }
        ColorThemingStyle cts = getColorThemingStyleIfEnabled(layer);
        if (cts != null) {
            cts.setAlpha(alpha);
        }
    }

    private final String bgColor0 = "\"#FEEDD6\"";
    private final String bgColor1 = "\"#EAEAEA\"";
    private final String bgColor3 = "\"#FBFFE1\"";
    private final String bgColor4 = "\"#CCCCCC\"";

    public String header(String textA, String textB) {
        return "  <tr valign=\"top\">"
                + "     <td width=\"550\" height=\"12\" bgcolor="
                + bgColor3
                + "align=\"center\"><font face=\"Arial\" size=\"3\" align=\"right\"><b>"
                + textA + "</b></font></td>"
                + "     <td width=\"1586\" height=\"12\" bgcolor=" + bgColor3
                + "align=\"center\"><font face=\"Arial\" size=\"3\"><b>"
                + textB + "</b></font></td>" + "  </tr>";
    }

    public String property(String textA, String textB, String color) {
        return "  <tr valign=\"top\">"
                + "     <td width=\"550\" height=\"12\" bgcolor="
                + bgColor4
                + "align=\"right\"><font face=\"Arial\" size=\"3\" align=\"right\">"
                + textA + "</font></td>"
                + "     <td width=\"1586\" height=\"12\" bgcolor=" + color
                + "align=\"left\"><font face=\"Arial\" size=\"3\" >" + textB
                + "</font></td>" + "  </tr>";
    }

    private class InfoPanel extends HTMLPanel implements PropertyPanel {

        private static final long serialVersionUID = 1L;

        private String label_Name_R = "";     // Layer name
        private String label_NumItems_R = ""; // Number of features
        private String label_NumPts_R = "";   // Number of points
        private String label_GeoType_R = "";  // Geometry type
        private String label_NumAtts_R = "";  // Attribute number

        // These are the codes for Database and vector layers
        private String label_DSClass_R = "";  // vector type (SHP, etc)
        private String label_Path_R = "";     // Vector file path
        private String label_Charset_R = "";  // Shapefile charset
        private String label_Coordinate = ""; // Projection description
        private String label_Coordinate_file = "";// Proj. location

        // These are the string codes for ReferencedImage layers
        private String label_DSClass_IR = ""; // Image type (TIF, etc)
        private String label_Path_IR = "";    // Image file path

        private InfoPanel() throws Exception {
            String infotext;
            Locale locale = new Locale("en", "UK");
            String pattern = "###.####";
            DecimalFormat df = (DecimalFormat) NumberFormat
                    .getNumberInstance(locale);
            df.applyPattern(pattern);
            setInfo(layers);
            String info = "";
            info = info + header("", LAYERS + ": " + df.format(layers.length));

            // If only one layer (Layer.class) is selected
            if (layers.length == 1) {

                if (layers[0].isFeatureCollectionModified()) {
                    info = info
                            + property(NAME, label_Name_R + " - " + MODIFIED,
                                    bgColor0);
                } else {
                    info = info + property(NAME, label_Name_R, bgColor0);
                }
                // If it is a WFS
                String sclass = layers[0].getClass().getSimpleName();

                if (sclass.equals("WFSLayer")) {
                    // WFSLayer layer = (WFSLayer) layers[0];
                    String server;
                    String urlLayer;
                    try {
                        WFSLayer layer = (WFSLayer) layers[0];
                        server = layer.getServerURL();
                        urlLayer = layer.getGeoPropertyNameAsString();
                    } catch (Exception ex) {
                        server = "";
                        urlLayer = "";
                    }
                    info = info + header("", WEB_SERVICE);
                    info = info
                            + property(DATASOURCE_CLASS,
                                    "WFS - Web Feature Services", bgColor0);
                    info = info + property(WFS_SERVER, server, bgColor1);
                    info = info + property(WFS_LAYER_NAME, urlLayer, bgColor0);
                    // Other types of layer.class (vector or image files)
                } else {
                    // LAYER Name and check if FeatureCollection was modified
                    info = info + header("", DATASOURCE);
                    // IMAGE layer sub-section
                    if (sclass.equals("ReferencedImagesLayer")) {
                        // if ((layers[0].getStyle(ReferencedImageStyle.class)
                        // != null)) {
                        info = info
                                + property(DATASOURCE_CLASS, label_DSClass_IR,
                                        bgColor0);
                        info = info
                                + property(SOURCE_PATH, label_Path_IR, bgColor1);
                        info = info
                                + property(CATALOG_FILE, label_Path_R, bgColor0);
                        // VECTOR layer sub-section
                    } else {
                        info = info
                                + property(DATASOURCE_CLASS, label_DSClass_R,
                                        bgColor0);
                        info = info
                                + property(SOURCE_PATH, label_Path_R, bgColor1);
                        // Add charset if selected vector layer is a shapefile
                        if (layers[0].getDataSourceQuery() != null) {
                            if (layers[0]
                                    .getDataSourceQuery()
                                    .getDataSource()
                                    .getClass()
                                    .getName()
                                    .equals("com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource$Shapefile")) {
                                info = info
                                        + property(CHARSET, label_Charset_R,
                                                bgColor0);
                            }
                        }

                    }
                }
                // PROJECTION section
                info = info + header("", COORDINATE_SYSTEM);
                setInfoProjection(layers);
                info = info + property(CRS, label_Coordinate, bgColor0);
                info = info + property(SOURCE_PATH, label_Coordinate_file, bgColor1);
            }
            // if more than one layer.class is selected
            else {
                info = info + property(NAME, label_Name_R, bgColor0);
                info = info + header("", DATASOURCE);
                info = info
                        + property(DATASOURCE_CLASS, label_DSClass_R, bgColor0);
                info = info + property(SOURCE_PATH, label_Path_R, bgColor1);
            }
            // LAYER EXTENSION section
            info = info + header("", EXTENT);
            info = info + property(XMIN, df.format(extent.getMinX()), bgColor0);
            info = info + property(XMAX, df.format(extent.getMaxX()), bgColor1);
            info = info + property(YMIN, df.format(extent.getMinY()), bgColor0);
            info = info + property(YMAX, df.format(extent.getMaxY()), bgColor1);
            // FEATURES and ATTRIBUTES section
            info = info + header("", FEATURES);
            info = info
                    + property(NUMBER_OF_FEATURES, label_NumItems_R, bgColor0);
            info = info + property(NUMBER_OF_POINTS, label_NumPts_R, bgColor1);
            info = info + property(GEOMETRY_TYPE, label_GeoType_R, bgColor0);
            info = info
                    + property(NUMBER_OF_ATTRIBUTES, label_NumAtts_R, bgColor1);
            String table = "<table border='0.1'>";
            String table2 = "</table>";
            infotext = "<html>" + table + info + table2 + "</html>";
            getRecordPanel().removeAll();
            createNewDocument();
            append(infotext);
        }

        // Boolean. Selected layer is related to an image file
        private boolean isImageFileLayer(Layer layer) {
            if (layer.getStyle(ReferencedImageStyle.class) != null
                    && (layer.getDescription() != null)) {
                return true;
            } else {
                return false;
            }
        }

        // Boolean. Selected layer is related to a database
        private boolean isDataBaseLayer(Layer layer) {
            DataSourceQuery dsq = layer.getDataSourceQuery();
            if (dsq == null
                    || dsq.getDataSource() instanceof DataStoreQueryDataSource) {
                return true;
            } else {
                return false;
            }
        }

        public String getTitle() {
            return INFO;
        }

        public void updateStyles() {
        }

        public String validateInput() {
            return null;
        }

        // Set Info layer (excluded projection)
        private void setInfo(Layer[] layers) throws IOException, URISyntaxException {
            if (layers.length == 1) {
                // If only one layer is selected
                if (layers[0].getName().startsWith("wfs")) {
                    // if selected layer is WMF it reduces the name to source
                    label_Name_R = layers[0].getName().substring(4);
                } else {
                    // get the name of layer
                    label_Name_R = layers[0].getName();
                }
            } else {
                // Get the list of layers if more than one layer is selected
                for (Layer layer : layers)
                    label_Name_R += layer.getName() + " - ";
            }
            // The following code derives from original LayerPropertyPlugIn
            String sourcePath = NOT_SAVED;
            String geoClass = "";
            String sourceClass = "";
            int numFeatures = 0;
            int numPts = 0;
            int numAtts = 0;
            Geometry geo;
            boolean multipleGeoTypes = false;
            boolean multipleSourceTypes = false;
            Hashtable<String, Integer> geometryModes = new Hashtable<>();
            for (Layer layer : layers) {
                FeatureCollectionWrapper fcw = layer
                        .getFeatureCollectionWrapper();
                numFeatures += fcw.size();
                numAtts += fcw.getFeatureSchema().getAttributeCount() - 1;
                for (Feature feature : fcw.getFeatures()) {
                    geo = feature.getGeometry();
                    if (geo != null) {
                        numPts += geo.getNumPoints();
                        if (geoClass.equals("")) {
                            geoClass = geo.getClass().getName();
                        } else if (!geo.getClass().getName().equals(geoClass)) {
                            multipleGeoTypes = true;
                        }
                        String geoClassName = geo.getClass().getName();
                        int count = geometryModes.get(geoClassName) == null ? 0
                                : geometryModes.get(geoClassName);
                        geometryModes.put(geoClassName, count + 1);
                    }
                }
                DataSourceQuery dsq = layer.getDataSourceQuery();
                if (dsq != null) {
                    String dsqSourceClass = dsq.getDataSource().getClass().getName();
                    if (sourceClass.equals("")) {
                        sourceClass = dsqSourceClass;
                    } else if (!sourceClass.equals(dsqSourceClass)) {
                        multipleSourceTypes = true;
                    }
                    Map properties = dsq.getDataSource().getProperties();
                    if (properties.get(DataSource.URI_KEY) != null) {
                        sourcePath = new URI(properties.get(DataSource.URI_KEY).toString()).getPath();
                    } else if (properties.get(DataSource.FILE_KEY) != null) {
                        sourcePath = properties.get(DataSource.FILE_KEY).toString();
                    } else if (properties.get(DataStoreQueryDataSource.CONNECTION_DESCRIPTOR_KEY) != null) {
                        sourcePath = properties.get(DataStoreQueryDataSource.CONNECTION_DESCRIPTOR_KEY).toString();
                    }
                }
            }
            if (numFeatures == 0) {
                geoClass = NO_FEATURES;
            } else if (multipleGeoTypes) {
                geoClass = MULTIPLE_GEOMETRY_TYPES + ": ";
                int n = geometryModes.size();
                Enumeration<Integer> modeCount = geometryModes.elements();
                Enumeration<String> modeName = geometryModes.keys();
                for (int i = 0; i < n; i++) {
                    String geometryMode = modeName.nextElement();
                    int dotPos = geometryMode.lastIndexOf(".");
                    if (dotPos > 0) {
                        geometryMode = geometryMode.substring(dotPos + 1);
                    }
                    int geometryModeCount = modeCount.nextElement();
                    geoClass = geoClass + (i == 0 ? " " : ", ") + geometryMode
                            + ":" + geometryModeCount;
                }
            } else if (geoClass.equals("")) {
                geoClass = NULL_GEOMETRIES;
            } else {
                int dotPos = geoClass.lastIndexOf(".");
                if (dotPos > 0) {
                    geoClass = geoClass.substring(dotPos + 1);
                }
            }
            if (sourceClass.equals("")) {
                sourceClass = NODATASOURCELAYER;// NOT_SAVED;
            } else if (multipleSourceTypes) {
                sourceClass = MULTIPLE_SOURCE_TYPES;
            } else {
                int dotPos = sourceClass.lastIndexOf(".");
                if (dotPos > 0) {
                    sourceClass = sourceClass.substring(dotPos + 1);
                }
                dotPos = sourceClass.lastIndexOf("$");
                if (dotPos > 0) {
                    sourceClass = sourceClass.substring(dotPos + 1);
                }
            }
            label_GeoType_R = geoClass;
            if (layers.length == 1) {
                label_NumItems_R = "" + numFeatures;
                label_NumPts_R = "" + numPts;
                label_NumAtts_R = "" + numAtts;
            } else {
                DecimalFormat df = new DecimalFormat("0.0");
                double numLayers = layers.length;
                double avgNumFeatures = numFeatures / numLayers;
                double avgNumPts = numPts / numLayers;
                double avgNumAtts = numAtts / numLayers;
                label_NumItems_R = numFeatures + "  "
                        + df.format(avgNumFeatures) + AVERAGE_PER_LAYER;
                label_NumPts_R = numPts + "  " + df.format(avgNumPts)
                        + AVERAGE_PER_LAYER;
                label_NumAtts_R = df.format(avgNumAtts) + AVERAGE_PER_LAYER;
            }
            String charsetName;
            DataSourceQuery dsq = layers[0].getDataSourceQuery();
            if (dsq != null) {
                @SuppressWarnings("unchecked")
                Map<String,Object> properties = dsq.getDataSource()
                        .getProperties();
                charsetName = (String) properties.get(DataSource.CHARSET_KEY);
                if (charsetName == null) {
                    charsetName = Charset.defaultCharset().displayName();
                    properties.put(DataSource.CHARSET_KEY, charsetName);
                }
            } else {
                charsetName = Charset.defaultCharset().displayName();
            }
            label_Charset_R = charsetName;
            // End of code from from original LayerPropertyPlugIn.class
            // The next section is added for referenceImage layers.
            // It gets:
            // a) the extension of file (JPG, TIF, etc)
            // b) the file path (ex C:/foòder/filepath.tif)
            if (layers[0].getStyle(ReferencedImageStyle.class) != null
                    && (layers[0].getDescription() != null)) {
                String sourcePathImage;
                FeatureCollection featureCollection = layers[0]
                        .getFeatureCollectionWrapper();
                int size = featureCollection.size();
                // If there is only one image file in the raster catalog
                if (size == 1) {
                    for (Feature feature : featureCollection.getFeatures()) {
                        if (!feature.getString(ImageryLayerDataset.ATTR_URI)
                                .isEmpty()) {
                            sourcePathImage = feature
                                    .getString(ImageryLayerDataset.ATTR_URI);
                            sourcePathImage = sourcePathImage.substring(5);
                            File f = new File(sourcePathImage);
                            String filePath = f.getAbsolutePath().replace("%20",
                                    " ");
                            String type = FilenameUtils.getExtension(filePath)
                                    .toUpperCase();
                            label_DSClass_IR = type + " - " + IMAGE;
                            label_Path_IR = filePath;
                        }
                    }
                    // If there are more than one image file in the raster
                    // catalog
                } else {
                    for (Iterator<?> i = featureCollection.iterator(); i
                            .hasNext();) {
                        Feature feature = (Feature) i.next();
                        sourcePathImage = feature
                                .getString(ImageryLayerDataset.ATTR_URI);
                        sourcePathImage = sourcePathImage.substring(5);
                        File f = new File(sourcePathImage);
                        String filePath = f.getAbsolutePath();
                        String filePath1 = filePath.replace("%20", " ");
                        // Load all the names into name cell
                        label_Path_IR += filePath1 + "\n";
                    }
                    label_DSClass_IR = MULTIPLE_SOURCE_TYPES;
                }
            }
            label_DSClass_R = sourceClass;
            label_Path_R = sourcePath;
            if ((layers.length > 1)
                    && (!sourcePath.equalsIgnoreCase(NOT_SAVED))) {
                label_Path_R = MULTIPLE_SOURCES;
            }
        }


        private void setInfoProjection(Layer[] layers) throws Exception {
            // [Giuseppe Aruta 28/3(2017] restored method to load SRID from Style first
            SRSInfo srsInfo = ProjUtils.getSRSInfoFromLayerStyleOrSource(layers[0]);
            //SRSInfo srsInfo = ProjUtils.getSRSInfoFromLayerSource(layers[0]);
            System.out.println("NewLayerPropertiesPlugIn.setInfoProjection: " + srsInfo);
            label_Coordinate_file = srsInfo.getSource();
            label_Coordinate = srsInfo.toString();
        }

        // Get Projection info from the layer
        /*
        private void setInfoProjection(Layer[] layers) throws Exception {
            String fileSourcePath = "";
            String projection = "";
            String projection_file = PROJECTION_UNSPECIFIED;
            String extension = "";
            SRIDStyle sridStyle = (SRIDStyle) layers[0]
                    .getStyle(SRIDStyle.class);
            final int oldSRID = sridStyle.getSRID(); // read SRID recorded as OJ
                                                     // style
            int prjSRID = ProjUtils.SRIDFromFile(layers[0]);// read SRID from
                                                            // aux file or
                                                            // geotiff tag
            if (layers.length == 1) {
                if (oldSRID != prjSRID) { // compare the 2 SRID. For non files
                                          // Layer datasources (WFS, databases).
                                          // Also if file based
                                          // layer StyleSRID is different than
                                          // ProjSRID than the layer might be
                                          // reprojected
                    projection_file = SRID_CODE;
                    String srid = String.valueOf(oldSRID);
                    projection = ProjUtils.getSRSFromWkt(srid);
                } else {// Check if selected layer is related to an image file
                    if (isImageFileLayer(layers[0])) {
                        extension = imageFileExtension(layers[0]);
                        fileSourcePath = imageFileSourcePath(layers[0]);
                        if ((extension.equals("TIF") || extension
                                .equals("TIFF"))) {
                            // If TIFF file is a geotiff, it scans into
                            // embedded tag
                            if (ProjUtils.isGeoTIFF(fileSourcePath)) {
                                projection_file = GEO_METADATA;
                                projection = ProjUtils
                                        .readSRSFromGeoTiffFile(fileSourcePath);
                                // If the TIF file is not a GeoTIFF it looks
                                // for a proj code into aux files
                            } else {
                                projection_file = ProjUtils
                                        .getAuxiliaryProjFilePath(fileSourcePath);
                                projection = ProjUtils
                                        .readSRSFromAuxiliaryFile(fileSourcePath);
                            }
                        } else {// For other image files than TIFF
                            if (fileSourcePath != null) {
                                projection_file = ProjUtils
                                        .getAuxiliaryProjFilePath(fileSourcePath);
                                projection = ProjUtils
                                        .readSRSFromAuxiliaryFile(fileSourcePath);
                            }
                        }
                    } else { // Check if source file is is not an image file
                        if (!isDataBaseLayer(layers[0])) {// than restricted
                                                          // check to vector
                                                          // files
                            fileSourcePath = vectorFileSourcePath(layers[0]);
                            projection_file = ProjUtils
                                    .getAuxiliaryProjFilePath(fileSourcePath);
                            projection = ProjUtils
                                    .readSRSFromAuxiliaryFile(fileSourcePath);
                        } else { // Other situations not solved (?)
                            projection_file = "";
                            projection = PROJECTION_UNSPECIFIED;
                        }
                    }
                }
            } else {// For a multiple selection of layers, as they can have
                    // different SRIDs
                projection_file = "";
                projection = "";
            }
            label_Coordinate_file = projection_file;
            label_Coordinate = projection;
        }
        */
    }

    
    // Get source file path of a vector layer
    // eg. c:\folder\vector.shp
    public static String vectorFileSourcePath(Layer layer) {
        String fileSourcePath = "";
        if (layer.getDataSourceQuery() != null) {
            DataSourceQuery dsq = layer.getDataSourceQuery();
            if (dsq.getDataSource() != null) {
                Map map = dsq.getDataSource().getProperties();
                if (map.get(DataSource.URI_KEY) != null) {
                    fileSourcePath = ((URI)map.get(DataSource.URI_KEY)).getPath();
                } else if (map.get(DataSource.FILE_KEY) != null) {
                    fileSourcePath = map.get(DataSource.FILE_KEY).toString();
                }
            }
        }
        return fileSourcePath;
    }

    // Get source file path of a image layer
    // eg. c:\folder\image.tif
    public static String imageFileSourcePath(Layer layer) {
        String fileSourcePath = "";
        FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        String sourcePathImage;
        for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            sourcePathImage = feature
                    .getString(ImageryLayerDataset.ATTR_URI);
            sourcePathImage = sourcePathImage.substring(5);
            File f = new File(sourcePathImage);
            String filePath = f.getAbsolutePath();
            fileSourcePath = filePath.replace("%20", " ");
        }
        return fileSourcePath;

    }

    // Get file extension of a layer
    // eg. shp, tif, ect
    public static String imageFileExtension(Layer layer) {
        String extension;
        String fileSourcePath = "";
        FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        String sourcePathImage;
        for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            sourcePathImage = feature
                    .getString(ImageryLayerDataset.ATTR_URI);
            sourcePathImage = sourcePathImage.substring(5);
            File f = new File(sourcePathImage);
            String filePath = f.getAbsolutePath();
            fileSourcePath = filePath.replace("%20", " ");
        }
        extension = FileUtil.getExtension(fileSourcePath).toUpperCase();
        return extension;
    }
    
    interface PropertyPanel {
        String getTitle();

        void updateStyles();

        String validateInput();
    }

    // Transparency panel from original LayerPropertyPlugIn.class
    private class StylePanel extends JPanel implements PropertyPanel {
        private static final long serialVersionUID = 1L;
        private JSlider transparencySlider = new JSlider();

        private StylePanel() {
            Box box = new Box(1);
            setBorder(BorderFactory
                    .createTitledBorder(PROPORTIONAL_TRANSPARENCY_ADJUSTER));
            Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
            int value = 0;
            for (int i = -100; (i <= 100) && (i >= -100); i += 20) {
                labelTable.put(value,
                        new JLabel(Integer.toString(i)));
                value += 10;
            }
            transparencySlider.setMinimumSize(new Dimension(200, 20));
            transparencySlider.setPreferredSize(new Dimension(460, 50));
            transparencySlider.setPaintLabels(true);
            transparencySlider.setPaintTicks(true);
            transparencySlider.setLabelTable(labelTable);
            transparencySlider.setMajorTickSpacing(10);
            transparencySlider.setMinimum(0);
            transparencySlider.setMaximum(100);
            transparencySlider.setValue(50);
            box.add(transparencySlider);
            add(box);
            transparencySlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSlider source = (JSlider) e.getSource();
                    if (!source.getValueIsAdjusting()) {
                        int sliderVal = source.getValue();
                        for (int i = 0; i < layers.length; i++) {
                            Layer layer = layers[i];
                            int currTrans = currTransArray[i];
                            double newTrans = currTrans;
                            if (sliderVal < 50) {
                                double percentChg = (50 - sliderVal) / 50.0D;
                                newTrans = currTrans + (255 - currTrans)
                                        * percentChg;
                            } else if (sliderVal > 50) {
                                double percentChg = (sliderVal - 50) / 50.0D;
                                newTrans = currTrans - currTrans * percentChg;
                            }
                            setAlpha(layer, 255 - (int) newTrans);
                            layer.fireAppearanceChanged();
                            styleChanged = true;
                        }
                    }
                }
            });
        }

        public String getTitle() {
            return STYLES;
        }

        public void updateStyles() {
        }

        public String validateInput() {
            return null;
        }
    }
}
