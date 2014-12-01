package com.vividsolutions.jump.workbench.imagery.geoimg;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.openjump.core.ui.io.file.FileLayerLoader;

import com.vividsolutions.jump.workbench.JUMPWorkbenchContext;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactoryFileLayerLoader;
import com.vividsolutions.jump.workbench.registry.Registry;

public class GeoImageFactoryFileLayerLoader extends
    ReferencedImageFactoryFileLayerLoader {

  public GeoImageFactoryFileLayerLoader(WorkbenchContext workbenchContext,
      GeoImageFactory gif, String[] additionalSupportedFileExtensions) {
    super(workbenchContext, gif, additionalSupportedFileExtensions);
  }

  public List<FileLayerLoader> getValidImageSubFactories(URI uri)
      throws IOException {
    List<FileLayerLoader> l = new ArrayList();
    if (imageFactory instanceof GeoImageFactory) {
      // fetch all loaders
      for (Object loader : GeoRaster.listValidReaders(uri)) {
        GeoImageFactory f = new GeoImageFactory(loader);
        ReferencedImageFactoryFileLayerLoader fll = new ReferencedImageFactoryFileLayerLoader(
            workbenchContext, f, null);
        l.add(fll);
      }
    }

    l.add(this);

    return l;
  }

  static public void register(WorkbenchContext wbc) {
    GeoImageFactory gif = new GeoImageFactory();
    // check availability and init file extension list
    if (!gif.isAvailable(wbc))
      return;

    Registry registry = wbc.getRegistry();
    // create generic autoloader
    GeoImageFactoryFileLayerLoader gifll = new GeoImageFactoryFileLayerLoader(
        wbc, gif, null);
    registry.createEntry(FileLayerLoader.KEY, gifll);
    
    // create/register all fixed loader filelayerloaders
    List<Object> loaders = GeoRaster.listAllReaders();
    for (Object loader : loaders) {
      gif = new GeoImageFactory(loader);
      gifll = new GeoImageFactoryFileLayerLoader(
          wbc, gif, null);
//      System.out.println("GIFFLL: register "+gifll+"/"+gifll.getFileExtensions());
      registry.createEntry(FileLayerLoader.KEY, gifll);
    }
    
  }

}
