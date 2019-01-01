package org.openjump.core.rasterimage.styler;

import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class RasterStylesExtension extends Extension {

    @Override
    public void configure(PlugInContext pic) throws Exception {

        new RasterStylesPlugIn().initialize(pic);
        new RasterLegendPlugIn().initialize(pic);

    }

    public static final String extensionName = "Raster Styles";
    public static final String suffixBlackBKey = "_Styles";

}
