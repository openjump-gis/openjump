package org.openjump.core.ui.plugin.wms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.swing.DetachableInternalFrame;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.wms.AbstractWMSRequest;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;

/**
 * Giuseppe Aruta 2015_6_4 Plugin that allows to show the legends of WMS layer
 */

public class WMSLegendPlugIn extends AbstractPlugIn {

    public WMSLegendPlugIn() {
        super();
    }

    private static ImageIcon ICON = IconLoader.icon("saig/addLegend.gif");
    private JScrollPane scrollPane = new JScrollPane();

    @Override
    public boolean execute(final PlugInContext context) throws Exception {
        final WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(
                context, WMSLayer.class);

        final DetachableInternalFrame frame = new DetachableInternalFrame();
        frame.setTitle(PANEL + " (" + layer.getName() + ")");
        frame.setResizable(true);
        frame.setClosable(true);
        frame.setIconifiable(true);
        frame.setMaximizable(true);
        frame.setFrameIcon(ICON);
        frame.setSize(200, 500);
        frame.setLayer(JLayeredPane.PALETTE_LAYER);
        scrollPane = new JScrollPane(getLegendPanel(context),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(scrollPane.getPreferredSize());
        frame.add(panel, BorderLayout.NORTH);
        final JPanel okPanel = new JPanel();
        final JButton saveButton = new JButton(SAVE) {

            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };

        final JButton closeButton = new JButton(CLOSE) {
            private static final long serialVersionUID = 2L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };

        saveButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save(scrollPane);
            }
        });

        closeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        okPanel.add(saveButton, BorderLayout.WEST);
        okPanel.add(closeButton, BorderLayout.EAST);

        frame.add(okPanel, BorderLayout.SOUTH);
        frame.pack();
        context.getWorkbenchFrame().addInternalFrame(frame, true, true);
        // Detachable internal frame now opens on left/middle part of the
        // project view
        // frame.setBounds(context.getLayerViewPanel().getWidth() - 10, 100,
        // frame.getWidth(), frame.getHeight());

        return true;
    }

    // // Get the WMS legend URL by name of layer
    // private String getLegendUrl(PlugInContext context, String names)
    // throws IOException {
    // WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(context,
    // WMSLayer.class);
    // String serverURL = layer.getService().getServerUrl();
    //
    // String version = layer.getWmsVersion();
    // if ("1.0.0".equals(version)) {
    // serverURL = serverURL
    // + "REQUEST=GetLegendGraphic&feature_info&WMTVER=1.0.0";
    // } else if (("1.1.0".equals(version)) || ("1.1.1".equals(version))
    // || ("1.3.0".equals(version))) {
    // serverURL = serverURL
    // + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION="
    // + version;
    // }
    // serverURL = serverURL + "&FORMAT=image/png&WIDTH=16&HEIGHT=16";
    // serverURL = serverURL
    // +
    // "&legend_options=bgColor:0xFFFFEE;dpi:100;fontAntiAliasing:true;forceLabels:on";
    //
    // serverURL = serverURL + "&LAYER=" + names;
    // return serverURL;
    //
    // }

    // // Get the WMS style URL by name of layer
    // private String getStyles(PlugInContext context, String names)
    // throws IOException {
    // WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(context,
    // WMSLayer.class);
    // String serverURL = layer.getService().getServerUrl();
    //
    // String version = layer.getWmsVersion();
    // if ("1.0.0".equals(version)) {
    // serverURL = serverURL
    // + "REQUEST=GetLegendGraphic&feature_info&WMTVER=1.0.0";
    // } else if (("1.1.0".equals(version)) || ("1.1.1".equals(version))
    // || ("1.3.0".equals(version))) {
    // serverURL = serverURL + "&SERVICE=WMS&REQUEST=GetStyles&VERSION="
    // + version;
    // }
    // serverURL = serverURL + "&LAYER=" + names;
    // return serverURL;
    //
    // }

    // Modified From Kosmo SAIG. Build the legend panel. For each layer it
    // displays the layer name at the top and the legend at the bottom

    public void save(JScrollPane pane) {
        FileNameExtensionFilter filter;
        final JPanel panel = (JPanel) pane.getViewport().getView();
        final int w = panel.getWidth();
        final int h = panel.getHeight();
        final BufferedImage bi = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = bi.createGraphics();
        panel.paint(g);

        filter = new FileNameExtensionFilter("Portable Network Graphics (png)",
                "png");
        final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
                "png");
        fc.setFileFilter(filter);
        fc.addChoosableFileFilter(filter);
        final int returnVal = fc.showSaveDialog(JUMPWorkbench.getInstance()
                .getFrame());
        fc.getWidth();
        fc.getHeight();
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                final File file = new File(fc.getSelectedFile() + ".png");
                ImageIO.write(bi, "png", file);
                saved(file);
            } catch (final Exception e) {
                notsaved();
                Logger(this.getClass(), e);
            }
        }
    }

    public static void Logger(Class<?> plugin, Exception e) {
        final Logger LOG = Logger.getLogger(plugin);
        JUMPWorkbench
                .getInstance()
                .getFrame()
                .warnUser(
                        plugin.getSimpleName() + " Exception: " + e.toString());
        LOG.error(plugin.getName() + " Exception: ", e);
    }

    protected void saved(File file) {
        JUMPWorkbench.getInstance().getFrame()
                .setStatusMessage(sSaved + " :" + file.getAbsolutePath());
    }

    protected void notsaved() {
        JOptionPane.showMessageDialog(null, SCouldNotSave, I18N.get(getName()),
                JOptionPane.WARNING_MESSAGE);
    }

    private final String sSaved = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final String SCouldNotSave = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");
    private final String SAVE = I18N
            .get("deejump.plugin.SaveLegendPlugIn.Save");
    private final String CLOSE = I18N
            .get("ui.plugin.imagery.ImageLayerManagerDialog.Close");

    public JPanel getLegendPanel(PlugInContext context) throws IOException {

        final JPanel mainPanel = new JPanel(new GridBagLayout());
        final WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(
                context, WMSLayer.class);
        final List<String> names = layer.getLayerNames();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        for (final String layerName : names) {
            final MapLayer mapLayer = layer.getService().getCapabilities()
                    .getMapLayerByName(layerName);

            final String layerTitle = StringUtils.isNotEmpty(mapLayer
                    .getTitle()) ? mapLayer.getTitle() : layerName;
            final JLabel nameLabel = new JLabel(
                    I18N.getMessage(
                            "org.openjump.core.ui.plugin.queries.SimpleQuery.layer" + ": {0}", //$NON-NLS-1$
                            layerTitle));
            nameLabel.setHorizontalAlignment(JLabel.CENTER);

            nameLabel.setFont(new Font("Verdana", Font.BOLD, 16));
            mainPanel.add(nameLabel, BorderLayout.CENTER);

            // String legendUrl = getLegendUrl(context, layerName);
            // Image image;
            // URL selectedUrl = null;
            // selectedUrl = new URL(legendUrl);
            // image = ImageIO.read(selectedUrl);
            final LegendRequest req = new LegendRequest(layer.getService(),
                    layerName);
            final Image image = req.getImage();
            final ImageIcon legendIcon = new ImageIcon(image);

            if (legendIcon != null) {
                final JLabel labelIcon = new JLabel(legendIcon, JLabel.CENTER);
                mainPanel.add(labelIcon, BorderLayout.SOUTH);
            } else {
                final JLabel textLabel = new JLabel(MESSAGE);
                mainPanel.add(textLabel, BorderLayout.SOUTH);
            }
        }

        return mainPanel;
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(
                        1, WMSLayer.class));
    }

    @Override
    public String getName() {
        return PLUGIN;
    }

    public ImageIcon getIcon() {
        return ICON;
    }

    private final String PLUGIN = I18N
            .get("org.openjump.core.ui.plugin.wms.WMSLegendPlugIn");
    private final String PANEL = I18N
            .get("org.openjump.core.ui.plugin.wms.WMSLegendPlugIn.panel");
    private final String MESSAGE = I18N
            .get("org.openjump.core.ui.plugin.wms.WMSLegendPlugIn.message");
}

class LegendRequest extends AbstractWMSRequest {
    private final String layerName;

    public LegendRequest(WMService service, String name) {
        super(service);
        layerName = name;
    }

    @Override
    public URL getURL() throws MalformedURLException {
        String serverURL = service.getServerUrl();

        final String version = service.getVersion();
        if (WMService.WMS_1_0_0.equals(version)) {
            serverURL = serverURL
                    + "REQUEST=GetLegendGraphic&feature_info&WMTVER=1.0.0";
        } else {
            serverURL = serverURL
                    + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION="
                    + version;
        }
        serverURL = serverURL + "&FORMAT=image/png&WIDTH=16&HEIGHT=16";
        serverURL = serverURL
                + "&legend_options=bgColor:0xFFFFEE;dpi:100;fontAntiAliasing:true;forceLabels:on";

        serverURL = serverURL + "&LAYER=" + layerName;

        return new URL(serverURL);
    }

}