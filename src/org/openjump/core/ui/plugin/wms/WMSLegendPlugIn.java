package org.openjump.core.ui.plugin.wms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang.StringUtils;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.ui.swing.DetachableInternalFrame;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
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

    private static ImageIcon ICON = IconLoader.icon("globe3_13.png");
    private JScrollPane scrollPane = new JScrollPane();

    public boolean execute(final PlugInContext context) throws Exception {
        final WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(
                context, WMSLayer.class);

        DetachableInternalFrame frame = new DetachableInternalFrame();
        frame.setTitle(PANEL + " (" + layer.getName() + ")");
        frame.setResizable(true);
        frame.setClosable(true);
        frame.setIconifiable(true);
        frame.setMaximizable(true);
        frame.setFrameIcon(ICON);
        frame.setSize(200, 500);
        frame.setLayer(JLayeredPane.PALETTE_LAYER);
        scrollPane = new JScrollPane(getLegendPanel(context));
        frame.add(scrollPane, BorderLayout.CENTER);

        context.getWorkbenchFrame().addInternalFrame(frame, true, true);
        //Detachable internal frame now opens on left/middle part of the project view
        frame.setBounds(context.getLayerViewPanel().getWidth() - 10, 100,
                frame.getWidth(), frame.getHeight());

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

    public JPanel getLegendPanel(PlugInContext context) throws IOException {

        JPanel mainPanel = new JPanel(new GridBagLayout());
        WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(context,
                WMSLayer.class);
        List<String> names = layer.getLayerNames();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        for (Iterator<String> iter = names.iterator(); iter.hasNext();) {
            String layerName = iter.next();
            MapLayer mapLayer = layer.getService().getCapabilities()
                    .getMapLayerByName(layerName);

            String layerTitle = StringUtils.isNotEmpty(mapLayer.getTitle()) ? mapLayer
                    .getTitle() : layerName;
            JLabel nameLabel = new JLabel(
                    I18N.getMessage(
                            "org.openjump.core.ui.plugin.queries.SimpleQuery.layer" + ": {0}", //$NON-NLS-1$
                            new Object[] { layerTitle }));
            nameLabel.setHorizontalAlignment(JLabel.CENTER);

            nameLabel.setFont(new Font("Verdana", Font.BOLD, 16));
            mainPanel.add(nameLabel, BorderLayout.CENTER);

            // String legendUrl = getLegendUrl(context, layerName);
            // Image image;
            // URL selectedUrl = null;
            // selectedUrl = new URL(legendUrl);
            // image = ImageIO.read(selectedUrl);
            LegendRequest req = new LegendRequest(layer.getService(), layerName);
            Image image = req.getImage();
            ImageIcon legendIcon = new ImageIcon(image);

            if (/* getStyles(context, layerName) != null && */legendIcon != null) {
                JLabel labelIcon = new JLabel(legendIcon, JLabel.CENTER);
                mainPanel.add(labelIcon, BorderLayout.SOUTH);
            } else {
                JLabel textLabel = new JLabel(MESSAGE);
                mainPanel.add(textLabel, BorderLayout.SOUTH);
            }
        }

        return mainPanel;
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(
                        1, WMSLayer.class));
    }

    public String getName() {
        return PLUGIN;
    }

    private String PLUGIN = I18N
            .get("org.openjump.core.ui.plugin.wms.WMSLegendPlugIn");
    private String PANEL = I18N
            .get("org.openjump.core.ui.plugin.wms.WMSLegendPlugIn.panel");
    private String MESSAGE = I18N
            .get("org.openjump.core.ui.plugin.wms.WMSLegendPlugIn.message");
}

class LegendRequest extends AbstractWMSRequest {
    private String layerName;

    public LegendRequest(WMService service, String name) {
        super(service);
        layerName = name;
    }

    public URL getURL() throws MalformedURLException {
        String serverURL = service.getServerUrl();

        String version = service.getVersion();
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