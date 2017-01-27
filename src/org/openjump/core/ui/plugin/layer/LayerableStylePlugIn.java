package org.openjump.core.ui.plugin.layer;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.plugin.layer.pirolraster.ChangeRasterImagePropertiesPlugIn;
import org.openjump.core.ui.plugin.wms.WMSStylePlugIn;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import de.latlon.deejump.plugin.style.DeeChangeStylesPlugIn;
import de.latlon.deejump.wfs.jump.WFSLayer;

public class LayerableStylePlugIn extends AbstractPlugIn {
    public boolean execute(final PlugInContext context) throws Exception {

        /**
         * July 02 2015 [Giuseppe Aruta] - General class that substitute
         * DeeChangeStylesPlugIn on the main toolbar. This plugin allows to set
         * style according to selected layer type: Layer.class, WMSLayer.class
         * and RasterImageLayer.class
         */
        Layerable layer = (Layerable) LayerTools.getSelectedLayerable(context,
                Layerable.class);
        if (layer instanceof Layer) {
            DeeChangeStylesPlugIn vectorLayerChangeStylePlugIn = new DeeChangeStylesPlugIn();
            vectorLayerChangeStylePlugIn.execute(context);

        } else if (layer instanceof RasterImageLayer) {

            ChangeRasterImagePropertiesPlugIn rasterLayerChangeStylePlugIn = new ChangeRasterImagePropertiesPlugIn();
            rasterLayerChangeStylePlugIn.execute(context);

        } else if (layer instanceof WMSLayer) {

            WMSStylePlugIn wmsLayerChangeStylePlugIn = new WMSStylePlugIn();
            wmsLayerChangeStylePlugIn.execute(context);

        } else if (layer instanceof WFSLayer) {

            DeeChangeStylesPlugIn vectorLayerChangeStylePlugIn = new DeeChangeStylesPlugIn();
            vectorLayerChangeStylePlugIn.execute(context);

        } else {

            JOptionPane.showMessageDialog(null,
                    "Styling not supported for this type of datasource",
                    getName(), JOptionPane.PLAIN_MESSAGE);
        }

        return false;

    }

    public ImageIcon getIcon() {
        // return IconLoaderFamFam.icon("palette.png");
        return IconLoader.icon("Palette.png");
    }

    public String getName() {
        return I18N.get("ui.style.ChangeStylesPlugIn.change-styles");
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(
                        1, Layerable.class));
    }

}