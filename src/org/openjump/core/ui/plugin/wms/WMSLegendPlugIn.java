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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang.StringUtils;
import org.openjump.core.apitools.LayerTools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.wms.AbstractBasicRequest;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;

/**
 * Giuseppe Aruta 2015_6_4 Plugin that allows to show the legends of WMS layer
 */

public class WMSLegendPlugIn extends AbstractPlugIn {

    public WMSLegendPlugIn() {
        super();
    }

    public boolean execute(final PlugInContext context) throws Exception {

        JDialog dialogLegend = new JDialog(context.getWorkbenchFrame(), PANEL,
                false);
        dialogLegend.setSize(200, 500);
        JScrollPane sp = new JScrollPane(getLegendPanel(context));

        dialogLegend.add(sp);

        /*
         * JPanel savePanel = new JPanel(); JButton saveButton = new JButton(
         * I18N.get("deejump.plugin.SaveLegendPlugIn.Save-legend"));
         * savePanel.add(saveButton);
         * 
         * saveButton.setAction(new SaveImageAsAction(dialogLegend,
         * getLegendPanel(context)));// getLegendPanel(context)));
         * 
         * 
         * dialogLegend.add(savePanel, BorderLayout.SOUTH);
         */

        dialogLegend.setVisible(true);
        dialogLegend.validate();

        return true;
    }

//    // Get the WMS legend URL by name of layer
//    private String getLegendUrl(PlugInContext context, String names)
//            throws IOException {
//        WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(context,
//                WMSLayer.class);
//        String serverURL = layer.getService().getServerUrl();
//
//        String version = layer.getWmsVersion();
//        if ("1.0.0".equals(version)) {
//            serverURL = serverURL
//                    + "REQUEST=GetLegendGraphic&feature_info&WMTVER=1.0.0";
//        } else if (("1.1.0".equals(version)) || ("1.1.1".equals(version))
//                || ("1.3.0".equals(version))) {
//            serverURL = serverURL
//                    + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION="
//                    + version;
//        }
//        serverURL = serverURL + "&FORMAT=image/png&WIDTH=16&HEIGHT=16";
//        serverURL = serverURL
//                + "&legend_options=bgColor:0xFFFFEE;dpi:100;fontAntiAliasing:true;forceLabels:on";
//
//        serverURL = serverURL + "&LAYER=" + names;
//        return serverURL;
//
//    }

//    // Get the WMS style URL by name of layer
//    private String getStyles(PlugInContext context, String names)
//            throws IOException {
//        WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(context,
//                WMSLayer.class);
//        String serverURL = layer.getService().getServerUrl();
//
//        String version = layer.getWmsVersion();
//        if ("1.0.0".equals(version)) {
//            serverURL = serverURL
//                    + "REQUEST=GetLegendGraphic&feature_info&WMTVER=1.0.0";
//        } else if (("1.1.0".equals(version)) || ("1.1.1".equals(version))
//                || ("1.3.0".equals(version))) {
//            serverURL = serverURL + "&SERVICE=WMS&REQUEST=GetStyles&VERSION="
//                    + version;
//        }
//        serverURL = serverURL + "&LAYER=" + names;
//        return serverURL;
//
//    }

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
            mainPanel.add(nameLabel, BorderLayout.NORTH);

//            String legendUrl = getLegendUrl(context, layerName);
//            Image image;
//            URL selectedUrl = null;
//            selectedUrl = new URL(legendUrl);
//            image = ImageIO.read(selectedUrl);
            LegendRequest req = new LegendRequest(layer.getService(), layerName);
            Image image = req.getImage();
            ImageIcon legendIcon = new ImageIcon(image);

            if (/*getStyles(context, layerName) != null &&*/ legendIcon != null) {
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

class LegendRequest extends AbstractBasicRequest {
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
      serverURL = serverURL + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION="
          + version;
    }
    serverURL = serverURL + "&FORMAT=image/png&WIDTH=16&HEIGHT=16";
    serverURL = serverURL
        + "&legend_options=bgColor:0xFFFFEE;dpi:100;fontAntiAliasing:true;forceLabels:on";

    serverURL = serverURL + "&LAYER=" + layerName;
    
    return new URL(serverURL);
  }

}