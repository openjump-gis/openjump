package com.vividsolutions.jump.workbench.ui.plugin.imagery;

import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;
import com.vividsolutions.jump.workbench.imagery.ecw.ECWImageFactory;
import com.vividsolutions.jump.workbench.imagery.geotiff.GeoTIFFImageFactory;
import com.vividsolutions.jump.workbench.imagery.graphic.GraphicImageFactory;
import com.vividsolutions.jump.workbench.imagery.mrsid.MrSIDImageFactory;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;


public class InstallReferencedImageFactoriesPlugin extends AbstractPlugIn {
    public void initialize(final PlugInContext context) throws Exception {
        Registry registry = context.getWorkbenchContext().getRegistry();

        registry.createEntry(
                        ReferencedImageFactory.REGISTRY_CLASSIFICATION,
                        new GraphicImageFactory());
        registry.createEntry(
                        ReferencedImageFactory.REGISTRY_CLASSIFICATION,
                        new ECWImageFactory());
        registry.createEntry(
                        ReferencedImageFactory.REGISTRY_CLASSIFICATION,
                        new GeoTIFFImageFactory());
        registry.createEntry(
                        ReferencedImageFactory.REGISTRY_CLASSIFICATION,
                        new MrSIDImageFactory());
    }

}