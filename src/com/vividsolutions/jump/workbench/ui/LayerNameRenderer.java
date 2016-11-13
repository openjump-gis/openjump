/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeCellRenderer;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.imagery.ReferencedImagesLayer;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;
import com.vividsolutions.jump.workbench.ui.plugin.wms.MapLayerPanel;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;

import de.latlon.deejump.wfs.jump.WFSLayer;

public class LayerNameRenderer extends JPanel implements ListCellRenderer,
        TreeCellRenderer {
    // <<TODO>> See how the colour looks with other L&F's. [Jon Aquino]

    public static final String USE_CLOCK_ANIMATION_KEY = LayerNameRenderer.class
            .getName() + " - USE CLOCK ANIMATION";

    private final static Color UNSELECTED_EDITABLE_FONT_COLOR = Color.red;
    private final static Color SELECTED_EDITABLE_FONT_COLOR = Color.yellow;
    protected JCheckBox checkBox = new JCheckBox();

    private LayerColorPanel colorPanel = new LayerColorPanel(13);

    GridBagLayout gridBagLayout = new GridBagLayout();

    protected JLabel label = new JLabel();

    private boolean indicatingEditability = false;
    private boolean indicatingProgress = false;
    private int progressIconSize = 13;
    private Icon[] progressIcons = null;
    private Icon clearProgressIcon = GUIUtil.resize(
            IconLoader.icon("Clear.gif"), progressIconSize);

    public static String PROGRESS_ICON_KEY = "PROGRESS_ICON";

    public static String FEATURE_COUNT = I18N
            .get("ui.LayerNameRenderer.feature-count");

    private DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
    private RenderingManager renderingManager;
    private JLabel progressIconLabel = new JLabel();
    private Font font = new JLabel().getFont();
    private Font editableFont = font.deriveFont(Font.BOLD);
    private Font unselectableFont = font.deriveFont(Font.ITALIC);
    private Font editableUnselectableFont = font.deriveFont(Font.BOLD
            + Font.ITALIC);

    private JLabel imageLabel = new JLabel();
    private ImageIcon wmsIcon = MapLayerPanel.ICON;
    private ImageIcon multiRasterIcon = IconLoader.icon("maps_13.png");
    private ImageIcon rasterIcon = IconLoader.icon("map_13.png");
    private ImageIcon sextante_rasterIcon = IconLoader.icon("mapSv2_13.png");
    private ImageIcon sextante_rasterIcon2 = IconLoader.icon("mapSv2_13bw.png");
    private ImageIcon table_Icon = IconLoader.icon("Table.gif");
    private final static String LAYER_NAME = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Layer-Name");
    private final static String FILE_NAME = I18N.get("ui.MenuNames.FILE");
    private final static String MODIFIED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Modified");
    private final static String SRS = I18N
            .get("ui.plugin.wms.EditWMSQueryPanel.coordinate-reference-system");
    private final static String URL = "Url";
    private final static String NODATASOURCELAYER = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.nodatasourcelayer.message");
    // I18N
    // .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not-Saved");
    private final static String SOURCE_PATH = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Source-Path");
    private final static String SEXTANTE = I18N
            .get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Sextante-Raster-Image");
    private final static String DATASOURCE_CLASS = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.DataSource-Class");
    private final static String EXTENT = I18N
            .get("ui.plugin.analysis.GeometryFunction.Envelope");
    private final static String MULTIPLESOURCE = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-Sources");

    public LayerNameRenderer() {
        super();
        setOpaque(true);
        setName("List.layerNameRenderer");

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setIndicatingEditability(boolean indicatingEditability) {
        this.indicatingEditability = indicatingEditability;
    }

    public void setIndicatingProgress(boolean indicatingProgress,
            RenderingManager renderingManager) {
        this.indicatingProgress = indicatingProgress;
        this.renderingManager = renderingManager;
    }

    public JLabel getLabel() {
        return label;
    }

    /**
     * @return relative to this panel
     */
    public Rectangle getCheckBoxBounds() {
        int i = gridBagLayout.getConstraints(checkBox).gridx;
        int x = 0;
        for (int j = 0; j < i; j++) {
            x += getColumnWidth(j);
        }
        return new Rectangle(x, 0, getColumnWidth(i), getRowHeight());
    }

    /**
     * @param i
     *            zero-based
     */
    protected int getColumnWidth(int i) {
        validate();
        return gridBagLayout.getLayoutDimensions()[0][i];
    }

    protected int getRowHeight() {
        validate();
        return gridBagLayout.getLayoutDimensions()[1][0];
    }

    private boolean showProgressIconLabel = true;
    private boolean showImageLabel = true;
    private boolean showColorPanel = true;
    private boolean showCheckBox = true;
    private boolean showLabel = true;

    public void setProgressIconLabelVisible(boolean visible) {
        showProgressIconLabel = visible;
    }

    public void setImageLabelVisible(boolean visible) {
        showImageLabel = visible;
    }

    public void setColorPanelVisible(boolean visible) {
        showColorPanel = visible;
    }

    public void setCheckBoxVisible(boolean visible) {
        showCheckBox = visible;
    }

    public void setLabelVisible(boolean visible) {
        showLabel = visible;
    }

    /**
     * Workaround for bug 4238829 in the Java bug database: "JComboBox
     * containing JPanel fails to display selected item at creation time"
     */
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        validate();
    }

    /**
     * Special getListCellRendererComponent to render simple Strings. It is not
     * the normal use, but it makes it possible to pass special values as
     * "All Layers" or "Selected Layers" (used in QueryDialog). [mmichaud
     * 2011-09-27]
     */
    public Component getListCellRendererComponent(JList list, String value,
            int index, boolean isSelected, boolean cellHasFocus) {
        label.setText((String) value);
        imageLabel.setVisible(false);
        colorPanel.setVisible(false);
        if (isSelected) {
            setForeground(list.getSelectionForeground());
            setBackground(list.getSelectionBackground());
        } else {
            setForeground(list.getForeground());
            setBackground(list.getBackground());
        }
        return this;
    }

    private Component formatLayerEntry(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        // only treat layers & strings
        if (value == null
                || !(value instanceof Layerable || value instanceof String))
            return defaultListCellRenderer.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

        // Accepting String is not the normal use, but it makes it possible
        // to pass special values as "All Layers" or "Selected Layers" (used in
        // QueryDialog).
        if (value instanceof String) {
            return getListCellRendererComponent(list, (String) value, index,
                    isSelected, cellHasFocus);
        }

        // assign layername to list entry
        Layerable layerable = (Layerable) value;
        label.setText(layerable.getName());
        // show if allowed
        label.setVisible(showLabel);

        /*
         * setToolTipText(layerable.getName() + ((layerable instanceof Layer &&
         * (((Layer) layerable).getDescription() != null) && (((Layer)
         * layerable) .getDescription().trim().length() > 0)) ? (": " + ((Layer)
         * layerable) .getDescription()) : ""));
         */

        /*
         * Giuseppe Aruta (giuseppe_aruta@yahoo.it) Add Layer name and extension
         * of layer at layer tooltip
         */
        /*
         * String tooltip = "";* if (layerable instanceof Layer) { if (((Layer)
         * layerable).getDescription() == null || ((Layer)
         * layerable).getDescription().trim().length() == 0 || ((Layer)
         * layerable).getDescription().equals( layerable.getName())) { tooltip =
         * "<html>" + LAYER_NAME + ": " + ((Layer) layerable).getName() + "<br>"
         * + XMIN + ": " + ((Layer) layerable).getFeatureCollectionWrapper()
         * .getEnvelope().getMinX() + "<br>" + YMIN + ": " + ((Layer)
         * layerable).getFeatureCollectionWrapper() .getEnvelope().getMinX() +
         * "<br>" + XMAX + ": " + ((Layer)
         * layerable).getFeatureCollectionWrapper() .getEnvelope().getMinY() +
         * "<br>" + YMAX + ": " + ((Layer)
         * layerable).getFeatureCollectionWrapper() .getEnvelope().getMaxX() +
         * "<br>" + "maxY: " + ((Layer) layerable).getFeatureCollectionWrapper()
         * .getEnvelope().getMaxY() + "<br>"
         * 
         * + FEATURE_COUNT + ": " + ((Layer)
         * layerable).getFeatureCollectionWrapper() .size() + "</html>"; } else
         * { tooltip = layerable.getName() + ": " + ((Layer)
         * layerable).getDescription(); }
         * 
         * } else { tooltip = layerable.getName(); } setToolTipText(tooltip);
         */

        /**
         * Giuseppe Aruta [2015-01-04] Generated tooltip text [2015-03-29] Made
         * tooltip optional (original/enhanced)
         */
        boolean layerTooltipsOn = PersistentBlackboardPlugIn
                .get(JUMPWorkbench.getInstance().getContext())
                .get(EditOptionsPanel.LAYER_TOOLTIPS_KEY, false);
        if (layerTooltipsOn) {
            setToolTipText(generateMinimalToolTipText(layerable));

        } else {
            setToolTipText(generateToolTipText(layerable));
        }
        // setToolTipText(generateToolTipText(layerable));
        if (isSelected) {
            Color sbg = list.getSelectionBackground();
            Color sfg = list.getSelectionForeground();

            // [ede 11.2012] the following calculates the brightness y of the
            // backgroundcolor sbg
            // the workaround was meant to enforce a readable fg text color,
            // because on win7 the combobox
            // content was somehow painted white on white. this seems to be
            // solved but i just keep it
            // here because we might need it again, who knows
            // double ybg = (299 * sbg.getRed() + 587 * sbg.getGreen() + 114 *
            // sbg.getBlue()) / 1000;
            // System.out.println(sbg+"/"+sfg+" -> "+ybg+"/"+yfg);
            // sfg = ybg>=128 ? Color.BLACK : Color.WHITE;
            setBackground(sbg);
            setForeground(sfg);
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        checkBox.setSelected(layerable.isVisible());
        checkBox.setVisible(showCheckBox);

        // indicate editablility (if enabled) via text formatting
        // (regular,italic ...)
        if (indicatingEditability && layerable instanceof Layer) {
            if (((Layer) layerable).isEditable()) {
                if (!((Layer) layerable).isSelectable()) {
                    label.setFont(editableUnselectableFont); // LDB [2007-09-18]
                                                             // italic
                                                             // feedback
                } else {
                    label.setFont(editableFont);
                }
            } else {
                if (!((Layer) layerable).isSelectable()) {
                    label.setFont(unselectableFont);
                } else {
                    label.setFont(font);
                }
            }
            label.setForeground(isSelected ? SELECTED_EDITABLE_FONT_COLOR
                    : UNSELECTED_EDITABLE_FONT_COLOR);
        } else {
            label.setFont(font);
        }

        // either add image icon for image layers (if allowed)
        imageLabel.setVisible(false);
        // or colorpanel for vector layers
        colorPanel.setVisible(false);
        if (showImageLabel && layerable instanceof ReferencedImagesLayer) {
            // switch icon accoring to contained image count
            imageLabel.setIcon(((ReferencedImagesLayer) layerable)
                    .getFeatureCollectionWrapper().size() > 1 ? multiRasterIcon
                    : rasterIcon);
            imageLabel.setVisible(true);
        } else if (showColorPanel && layerable instanceof Layer
            && isTable((Layer) layerable)) {
          //Show a table icon if the Layer has features with empty geometries
        imageLabel.setIcon(table_Icon);
        imageLabel.setVisible(true);

        } else if (showColorPanel && layerable instanceof Layer) {
            colorPanel.init((Layer) layerable, isSelected,
                    list.getBackground(), list.getSelectionBackground());
            colorPanel.setVisible(true);
        } else if (showImageLabel && layerable instanceof WMSLayer) {
            imageLabel.setIcon(wmsIcon);
            imageLabel.setVisible(true);
        }     else if (showImageLabel && layerable instanceof RasterImageLayer) {

          if (((RasterImageLayer) layerable).getNumBands() == 1) {
              imageLabel.setIcon(sextante_rasterIcon2);
          } else {
              imageLabel.setIcon(sextante_rasterIcon);
          }
          imageLabel.setVisible(true);
      }

        progressIconLabel.setVisible(false);
        // show the progress icon if allowed
        if (showProgressIconLabel) {
            // Only show the progress icon (clocks) for WMSLayers and
            // database-backed layers, not Layers. Otherwise it's too busy.
            // [Jon Aquino]
            if (layerable.getBlackboard().get(USE_CLOCK_ANIMATION_KEY, false)
                    && indicatingProgress
                    && (renderingManager.getRenderer(layerable) != null)
                    && renderingManager.getRenderer(layerable).isRendering()) {
                layerable.getBlackboard()
                        .put(PROGRESS_ICON_KEY,
                                layerable.getBlackboard().get(
                                        PROGRESS_ICON_KEY, 0) + 1);
                if (layerable.getBlackboard().getInt(PROGRESS_ICON_KEY) > (getProgressIcons().length - 1)) {
                    layerable.getBlackboard().put(PROGRESS_ICON_KEY, 0);
                }
                progressIconLabel.setIcon(getProgressIcons()[layerable
                        .getBlackboard().getInt(PROGRESS_ICON_KEY)]);
                progressIconLabel.setVisible(true);
            } else {
                progressIconLabel.setIcon(clearProgressIcon);
                layerable.getBlackboard().put(PROGRESS_ICON_KEY, null);
                progressIconLabel.setVisible(false);
            }
        }

        return this;
    }

    private JList list(JTree tree) {
        JList list = new JList();
        list.setForeground(tree.getForeground());
        list.setBackground(tree.getBackground());
        list.setSelectionForeground(UIManager
                .getColor("Tree.selectionForeground"));
        list.setSelectionBackground(UIManager
                .getColor("Tree.selectionBackground"));
        return list;
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        // generally format layer
        formatLayerEntry(list, value, index, isSelected, cellHasFocus);

        // assign proper width to cell entry
        // setPreferredSize(getPreferredListCellSize());

        return this;
    }

    // calculate the optimum width for listcells to show complete content
    private Dimension getPreferredListCellSize() {
        int width = 0, height = 0;
        for (Component comp : getComponents()) {
            if (!comp.isVisible())
                continue;
            int cheight = comp.getPreferredSize().height;
            height = cheight > height ? cheight : height;
            width += comp.getPreferredSize().width;
        }
        // add some padding
        return new Dimension(width + 10, height);
    }

    // helper method to assign fg/bgcolor to _all_ panel components at once
    private void _setComponentsFBGColor(Color c, boolean fg) {
        for (Component comp : getComponents()) {
            if (fg)
                comp.setForeground(c);
            else
                comp.setBackground(c);
        }
    }

    @Override
    public void setForeground(Color c) {
        super.setForeground(c);
        _setComponentsFBGColor(c, true);
    }

    @Override
    public void setBackground(Color c) {
        super.setBackground(c);
        _setComponentsFBGColor(c, false);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        Layerable layerable = (Layerable) value;
        // generally format layer
        formatLayerEntry(list(tree), layerable, row, selected, hasFocus);
        // assign proper width to cell entry
        // setPreferredSize(getPreferredListCellSize());
        if (selected) {
            label.setForeground(UIManager.getColor("Tree.selectionForeground"));
            label.setBackground(UIManager.getColor("Tree.selectionBackground"));
            setForeground(UIManager.getColor("Tree.selectionForeground"));
            setBackground(UIManager.getColor("Tree.selectionBackground"));
        } else {
            label.setForeground(tree.getForeground());
            label.setBackground(tree.getBackground());
            setForeground(tree.getForeground());
            setBackground(tree.getBackground());
        }
        if (indicatingEditability && layerable instanceof Layer) {
            if (((Layer) layerable).isEditable()) {
                label.setForeground(selected ? SELECTED_EDITABLE_FONT_COLOR
                        : UNSELECTED_EDITABLE_FONT_COLOR);
            }
        }

        return this;
    }

    void jbInit() throws Exception {
        Insets zero_insets = new Insets(0, 0, 0, 0);
        this.setLayout(gridBagLayout);
        // checkBox.setOpaque(false);
        checkBox.setVisible(false);
        checkBox.setMargin(zero_insets);
        checkBox.setBorder(new EmptyBorder(zero_insets));
        // label.setOpaque(false);
        label.setText("None");
        // label gets an extra left padding
        label.setBorder(new EmptyBorder(new Insets(0, 2, 0, 0)));
        Insets space_insets = new Insets(1, 2, 1, 0);
        this.add(imageLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                space_insets, 0, 0));
        this.add(colorPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                space_insets, 0, 0));
        this.add(checkBox, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                space_insets, 0, 0));
        this.add(progressIconLabel, new GridBagConstraints(3, 0, 1, 1, 0.0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                space_insets, 0, 0));
        this.add(label, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, space_insets,
                0, 0));
    }

    private Icon[] getProgressIcons() {
        // Create lazily -- OptimizeIt tells me creating these images takes 20
        // seconds [Jon Aquino 2004-05-14]
        if (progressIcons == null) {
            progressIcons = new Icon[] {
                    GUIUtil.resize(IconLoader.icon("ClockN.gif"),
                            progressIconSize),
                    GUIUtil.resize(IconLoader.icon("ClockNE.gif"),
                            progressIconSize),
                    GUIUtil.resize(IconLoader.icon("ClockE.gif"),
                            progressIconSize),
                    GUIUtil.resize(IconLoader.icon("ClockSE.gif"),
                            progressIconSize),
                    GUIUtil.resize(IconLoader.icon("ClockS.gif"),
                            progressIconSize),
                    GUIUtil.resize(IconLoader.icon("ClockSW.gif"),
                            progressIconSize),
                    GUIUtil.resize(IconLoader.icon("ClockW.gif"),
                            progressIconSize),
                    GUIUtil.resize(IconLoader.icon("ClockNW.gif"),
                            progressIconSize) };
        }
        return progressIcons;
    }

    /*
     * This method takes a String of text and simulates word wrapping by
     * applying HTML code <BR> after n characters per line. It will check to
     * make sure that we are not in the middle of a word before breaking the
     * line.
     */
    public static String SplitString(String string, int n) {

        StringBuffer buf = new StringBuffer();
        String tempString = string;

        if (string != null) {

            while (tempString.length() > n) {
                String block = tempString.substring(0, n);
                int index = block.lastIndexOf(File.separator);
                if (index < 0) {
                    index = tempString.indexOf(File.separator);
                }
                if (index >= 0) {
                    buf.append(tempString.substring(0, index) + "<BR>");
                }
                tempString = tempString.substring(index + 1);
            }
        } else {
            tempString = File.separator;
        }
        buf.append(tempString);
        return buf.toString();

    }

    /*
     * Associate Byte, Megabytes, etc to file
     */
    private static final String[] Q = new String[] { "", "KB", "MB", "GB",
            "TB", "PB", "EB" };

    /*
     * Return bytres as string
     */
    public String getAsString(long bytes) {
        for (int i = 6; i > 0; i--) {
            double step = Math.pow(1024, i);
            if (bytes > step)
                return String.format("%3.1f %s", bytes / step, Q[i]);
        }
        return Long.toString(bytes);
    }

    /*
     * Enumeration of File extension used in Sextante Raster Layer
     */
    public enum TypeFile {
        ASC, CSV, DXF, FLT, TIF, TIFF, JPG, JPEG, PNG, GIF, GRD, JP2, BMP, ECW, MrSID, TXT
    }

    private String filetype;

    /*
     * Return type of the Sextante Raster Layer as String
     */
    public String filetype(File file) {
        TypeFile extension1 = TypeFile.valueOf(getExtension(file));
        switch (extension1) {
        case ASC: {
            filetype = "ASC - ESRI ASCII grid";
            break;
        }
        case CSV: {
            filetype = "CSV - Comma-separated values";
            break;
        }
        case DXF: {
            filetype = "Autocad DXF - Drawing Exchange Format";
            break;
        }
        case FLT: {
            filetype = "FLT - ESRI Binary grid";
            break;
        }
        case TIF: {
            filetype = "GEOTIF/TIFF Tagged Image File Format";
            break;
        }
        case TIFF: {
            filetype = "GEOTIF/TIFF Tagged Image File Format";
            break;
        }
        case JPG: {
            filetype = "JPEG/JPG - Joint Photographic Experts Group";
            break;
        }
        case JPEG: {
            filetype = "JPEG/JPG - Joint Photographic Experts Group";
            break;
        }
        case PNG: {
            filetype = "PNG - Portable Network Graphics";
            break;
        }
        case GIF: {
            filetype = "GIF - Graphics Interchange Format";
            break;
        }
        case GRD: {
            filetype = "GRD - Surfer ASCII Grid";
            break;
        }
        case JP2: {
            filetype = "JPEG 2000 - Joint Photographic Experts Group";
            break;
        }
        case BMP: {
            filetype = "BMP - Windows Bitmap";
            break;
        }
        case ECW: {
            filetype = "ECW - Enhanced Compression Wavelet";
            break;
        }
        case MrSID: {
            filetype = "MrSID - Multiresolution seamless image database";
            break;
        }
        }
        return filetype;
    }

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toUpperCase();
        }
        return ext;
    }

    /**
     * Giuseppe Aruta [2015-01-04] Create a tooltip Original JUMP version
     */
    private String generateMinimalToolTipText(Layerable layerable) {

        String tooltip = "";
        if (layerable instanceof Layer) {
            if (((Layer) layerable).getDescription() == null
                    || ((Layer) layerable).getDescription().trim().length() == 0
                    || ((Layer) layerable).getDescription().equals(
                            layerable.getName())) {
                tooltip = FEATURE_COUNT
                        + " = "
                        + ((Layer) layerable).getFeatureCollectionWrapper()
                                .size();
            } else {
                tooltip = layerable.getName() + ": "
                        + ((Layer) layerable).getDescription();
            }
        } else
            tooltip = layerable.getName();
        return tooltip;
    }

    private String generateToolTipText(Layerable layerable) {
        // String tooltip = layerable.getName();

        String tooltip = "";

        String sourceClass = "";
        new JEditorPane();

        String sourcePath = NODATASOURCELAYER.toUpperCase();

        /*
         * WMSLayer.class
         */
        if (layerable instanceof WMSLayer) {
            WMSLayer layer = (WMSLayer) layerable;
            String url = layer.getServerURL();// Url server of WMF layer
            String srs = layer.getSRS();// SRS of WMS layer
            Envelope env = layer.getEnvelope();// Get Envelope of WMS layer
            tooltip = "<HTML><BODY>";
            tooltip += "<DIV style=\"width: 400px; text-justification: justify;\">";
            tooltip += "<b>" + LAYER_NAME + ": </b>" + layer.getName() + "<br>";
            tooltip += "<b>" + DATASOURCE_CLASS + ": </b>" + "WMS" + "<br>";
            tooltip += "<b>" + URL + ": </b>" + StringUtil.split(url, 350)
                    + "<br>";
            tooltip += "<b>" + SRS + ": </b>" + srs + "<br>";
            tooltip += "<b>" + EXTENT + ": </b>" + env.toString() + "<br>";
            tooltip += "</DIV></BODY></HTML>";
        }
        /*
         * WFSLayer.class
         */
        else if (layerable instanceof WFSLayer) {
            WFSLayer layer = (WFSLayer) layerable;
            String url = layer.getServerURL();// Url server of WFS layer
            String srs = layer.getCrs();// SRS of WFS layer

            Envelope env = layer.getFeatureCollectionWrapper().getEnvelope();// Get
                                                                             // Envelope
                                                                             // of
                                                                             // WFS
                                                                             // layer
            int size = -1;// Layer size
            size = layer.getFeatureCollectionWrapper().size();// Get number
            tooltip = "<HTML><BODY>";
            tooltip += "<DIV style=\"width: 400px; text-justification: justify;\">";
            tooltip += "<b>" + LAYER_NAME + ": </b>" + layer.getName() + "<br>";
            tooltip += "<b>" + DATASOURCE_CLASS + ": </b>" + "WFS" + "<br>";
            tooltip += "<b>" + URL + ": </b>" + StringUtil.split(url, 350)
                    + "<br>";
            tooltip += "<b>" + SRS + ": </b>" + srs + "<br>";
            tooltip += "<b>" + EXTENT + ": </b>" + env.toString() + "<br>";
            tooltip += "<b>" + FEATURE_COUNT + ": </b>" + size + "<br>";
            tooltip += "</DIV></BODY></HTML>";
        }
        /*
         * Sextante RasterImageLayer.class
         */

        else if (layerable instanceof RasterImageLayer) {
            RasterImageLayer layer = (RasterImageLayer) layerable;
            // RasterImageLayer.class must have a datasource but also not stored into a TEMP folder
            if (layer.getImageFileName() != null && !layer.getImageFileName().contains(System.getProperty("java.io.tmpdir"))) {
               	File image = new File(layer.getImageFileName());
                String type = filetype(image);
                String path = StringUtil.split(image.toString(), 350);
                String temporallayer =I18N
                        .get("ui.GenericNames.Temporal-layer");
                tooltip = "<HTML><BODY>";
                tooltip += "<DIV style=\"width: 400px; text-justification: justify;\">";
                tooltip += "<b>" + LAYER_NAME + ": </b>" + layer.getName()+ "<br>";
                tooltip += "<b>" + DATASOURCE_CLASS + ": </b>" + ":  " + type + " (" + SEXTANTE + ")<br>";
                // tooltip += "<b>" + FILE_NAME + ": </b>" + nameFile + "<br>";
                          
                tooltip += "<b>" + SOURCE_PATH + ": </b>" + path + "<br>";
                tooltip += "<b>" + FEATURE_COUNT + ": </b>" + "1" + "<br>";
                tooltip += "</DIV></BODY></HTML>";
            }
            // If RasterImageLayer.class has no datasource or if it is stored into a TEMP folder
            //tooltip show it as it has no datasource
            else {
            	sourcePath = NODATASOURCELAYER;
                tooltip = "<HTML><BODY>";
                tooltip += "<DIV style=\"width: 400px; text-justification: justify;\">";
                tooltip += "<b>" + LAYER_NAME + ": </b>" + layer.getName()
                        + "<br>";
                tooltip += "<b>" + DATASOURCE_CLASS + ": </b>";
                tooltip += SEXTANTE + "<br>";

                tooltip += "<b>" + SOURCE_PATH + ": </b>"
                        + "<b><font color='red'>" + NODATASOURCELAYER
                        + "</font></b><br>";
                tooltip += "<b>" + FEATURE_COUNT + ": </b>" + "1" + "<br>";
                tooltip += "</DIV></BODY></HTML>";
            }
        }
        /*
         * Layer.class
         */
        else if (layerable instanceof Layer) {
            Layer layer = (Layer) layerable;

            int size = -1;// Layer size
            String layerName = layerable.getName();
            size = layer.getFeatureCollectionWrapper().size();// Get number
                                                              // layers

            /*
             * Layer.class - NOT an Image Layer
             */
            if (layer.getStyle(ReferencedImageStyle.class) == null
                    && ((Layer) layerable).getDescription() != null) {
                /*
                 * Code from LayerPropertyPlugin that gets back the file mame of
                 * a Non-Image Layer.class
                 */
                DataSourceQuery dsq = layer.getDataSourceQuery();

                if (dsq != null) {
                    String dsqSourceClass = dsq.getDataSource().getClass()
                            .getName();
                    if (sourceClass.equals(""))
                        sourceClass = dsqSourceClass;
                    Object fnameObj = dsq.getDataSource().getProperties()
                            .get("File");
                    if (fnameObj == null) {
                        fnameObj = dsq
                                .getDataSource()
                                .getProperties()
                                .get(DataStoreDataSource.CONNECTION_DESCRIPTOR_KEY);
                    }
                    if (fnameObj != null) {
                        sourcePath = fnameObj.toString();
                    }
                    int dotPos = sourceClass.lastIndexOf(".");
                    if (dotPos > 0)
                        sourceClass = sourceClass.substring(dotPos + 1);
                    dotPos = sourceClass.lastIndexOf("$");
                    if (dotPos > 0)
                        sourceClass = sourceClass.substring(dotPos + 1);
                    File f = new File(sourcePath);
                    f.getName();
                    String path = StringUtil.split(sourcePath, 350);

                    // Layer.class with datasource that has been modified
                    if (layer.isFeatureCollectionModified()) {
                        tooltip = "<HTML><BODY>"; //$NON-NLS-1$
                        tooltip += "<DIV style=\"width: 400px; text-justification: justify;\">";
                        tooltip += "<b>" + LAYER_NAME + ": </b>" + layerName
                                + " - <b><font color='blue'>" + MODIFIED
                                + "</font></b><br>";
                        tooltip += "<b>" + DATASOURCE_CLASS + ": </b>"
                                + sourceClass + "<br>";
                        tooltip += "<b>" + SOURCE_PATH + ": </b>" + path
                                + "<br>";
                        tooltip += "<b>" + FEATURE_COUNT + ": </b>" + size
                                + "<br>";
                        tooltip += "</DIV></BODY></HTML>";

                    } else
                        // Layer.class with datasource not modified
                        tooltip = "<HTML><BODY>"; //$NON-NLS-1$
                    tooltip += "<DIV style=\"width: 400px; text-justification: justify;\">";
                    tooltip += "<b>" + LAYER_NAME + ": </b>" + layerName
                            + "<br>";
                    tooltip += "<b>" + DATASOURCE_CLASS + ": </b>"
                            + sourceClass + "<br>";
                    tooltip += "<b>" + SOURCE_PATH + ": </b>"
                            + StringUtil.split(sourcePath, 350) + "<br>";
                    tooltip += "<b>" + FEATURE_COUNT + ": </b>" + size + "<br>";
                    tooltip += "</DIV></BODY></HTML>";

                } else {

                    sourcePath = NODATASOURCELAYER;
                    tooltip = "<HTML><BODY>"; //$NON-NLS-1$
                    // tooltip +=
                    // "<DIV style=\"width: 300px; text-justification: justify;\">";
                    tooltip += "<b>" + LAYER_NAME + ": </b>" + layerName
                            + "<br>";
                    tooltip += "<b>" + DATASOURCE_CLASS + ": </b>" + ""
                            + "<br>";
                    tooltip += "<b>" + FILE_NAME + ": </b>"
                            + "<b><font color='red'>" + sourcePath
                            + "</font></b><br>";
                    tooltip += "<b>" + FEATURE_COUNT + ": </b>" + size + "<br>";
                    tooltip += "</BODY></HTML>";
                }

            }

            /*
             * Check if the selected Layer.class is a Image Layer
             */
            else if (layer.getStyle(ReferencedImageStyle.class) != null
                    && ((Layer) layerable).getDescription() != null) {
                /*
                 * Code from ImageLayerManagerPlugin to find Path and extension
                 * of a selected Image Layer.class
                 */
                String sourcePathImage = null;
                String sourceClassImage = null;
                FeatureCollection featureCollection = layer
                        .getFeatureCollectionWrapper();
                for (Iterator i = featureCollection.iterator(); i.hasNext();) {
                    Feature feature = (Feature) i.next();
                    sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
                    if (sourcePathImage == null || sourcePathImage.length() < 5) {
                        sourcePathImage = "";
                    } else {
                        sourcePathImage = sourcePathImage.substring(5);
                    }
                    sourceClassImage = feature.getString(ImageryLayerDataset.ATTR_TYPE);
                    if (sourceClassImage == null) {
                        sourceClassImage = "";
                    } else {
                        sourceClassImage.replace("%20", " ");
                    }
                    /*
                     * Check if the Image Layer.class has only one file loaded
                     */
                    if (size == 1) {
                        File f = new File(sourcePathImage);
                        String filePath = f.getAbsolutePath();
                        String filePath1 = filePath.replace("%20", " ");
                        String type = filetype(f);
                        f.getName();
                        tooltip = "<HTML><BODY>"; //$NON-NLS-1$
                        tooltip += "<DIV style=\"width: 400px; text-justification: justify;\">";
                        tooltip += "<b>" + LAYER_NAME + ": </b>" + layerName
                                + "<br>";

                        tooltip += "<b>" + DATASOURCE_CLASS + ": </b>" + type
                                + "<br>";

                        tooltip += "<b>" + SOURCE_PATH + ": </b>"
                                + StringUtil.split(filePath1, 350) + "<br>";
                        tooltip += "<b>" + FEATURE_COUNT + ": </b>" + size
                                + "<br>";
                        tooltip += "</DIV></BODY></HTML>";
                    }
                    /*
                     * In this case ImageLayerManagerPlugin has loaded more than
                     * one file as Image Layer.class
                     */
                    else {

                        tooltip = "<HTML><BODY>";
                        // tooltip +=
                        // "<DIV style=\"width: 300px; text-justification: justify;\">";
                        tooltip += "<b>" + LAYER_NAME + ": </b>" + layerName
                                + "<br>";
                        tooltip += "<b>" + SOURCE_PATH + ": </b>"
                                + MULTIPLESOURCE + "<br>";
                        tooltip += "<b>" + FEATURE_COUNT + ": </b>" + size
                                + "<br>";
                        tooltip += "</BODY></HTML>";
                    }
                }
            }
            /*
             * Check other Layer.class layer with no datasource
             */
            else {
                tooltip = "<HTML><BODY>";
                tooltip += "<b>" + LAYER_NAME + ": </b>" + layerName + "<br>";
                tooltip += "<b>" + FILE_NAME + ": </b>" + NODATASOURCELAYER
                        + "<br>";
                tooltip += "<b>" + FEATURE_COUNT + ": </b>" + size + "<br>";
                tooltip += "</BODY></HTML>";
            }
        }

        return tooltip;

    }

    @Override
    // [ede 11.2012] this is necessary for comboboxes with transparent bg, like
    // in
    // default vista/win7 lnf, else ugly background is painted behind the
    // letters
    public boolean isOpaque() {
        Color bgc = getBackground();
        Component p;
        // fetch cellrendererpane's parent if possible
        if ((p = getParent()) != null)
            p = p.getParent();
        // calculate our opaque state by honoring our parents values
        boolean colorMatchOrOpaque = (bgc != null) && (p != null)
                && bgc.equals(p.getBackground()) && p.isOpaque();
        return !colorMatchOrOpaque && super.isOpaque();
    }
    
    /* 
     * [Giuseppe Aruta 11.2016] . True if all the layer geometries are empty
     * (Geometrycollection empty). Workaround to decode table files (like .csv or .dbf)
     *  so that they are loaded in Sextante as table
      */
    public static boolean isTable(Layer layer) {
        FeatureCollectionWrapper featureCollection = layer
                .getFeatureCollectionWrapper();
        List featureList = featureCollection.getFeatures();
        Geometry nextGeo = null;
        for (@SuppressWarnings("unchecked")
        Iterator<FeatureCollectionWrapper> i = featureList.iterator(); i
                .hasNext();) {
            Feature feature = (Feature) i.next();
            nextGeo = feature.getGeometry();
        }
        if (!featureCollection.isEmpty() && nextGeo.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}