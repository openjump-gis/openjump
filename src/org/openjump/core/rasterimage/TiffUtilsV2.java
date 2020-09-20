package org.openjump.core.rasterimage;

import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoReferencedRaster;

public class TiffUtilsV2 {
  // a File -> RenderedOp cache mapping to prevent recreating inputs for the same file
  private static HashMap<File,GeoReferencedRaster> geoRasterCache = new HashMap<File,GeoReferencedRaster>();

  
  public static RenderedOp getRenderedOp(File tiffFile) throws IOException {
    GeoReferencedRaster geoRaster = getGeoReferencedRaster(tiffFile);
    RenderedOp rop;
    try {
      rop = geoRaster.getImage();
    } catch (ReferencedImageException e) {
      // TODO: handle errors better, wrapping it in IOException here
      //       because that's what's handled up from here
      throw new IOException(e);
    }
    return rop;
  }

  public static Envelope getEnvelope(File tiffFile) throws IOException {
    GeoReferencedRaster geoRaster = getGeoReferencedRaster(tiffFile);
    
    return geoRaster.getOriginalEnvelope();
  }

  private static GeoReferencedRaster getGeoReferencedRaster(File tiffFile) throws IOException {
    // prevent recreating inputs by reusing cached RenderedOp
    if (geoRasterCache.containsKey(tiffFile))
      return geoRasterCache.get(tiffFile);

    GeoReferencedRaster geoRaster;
    try {
      geoRaster = new GeoReferencedRaster(tiffFile.toString(),
          new com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi());
    } catch (ReferencedImageException e) {
      // TODO: handle errors better, wrapping it in IOException here
      //       because that's what's handled up from here
      throw new IOException(e);
    }

    // save in cache
    geoRasterCache.put(tiffFile, geoRaster);

    return geoRaster;
  }

  public static RenderedOp readSubsampled(File tiffFile, float xScale, float yScale) throws IOException {
    RenderedOp renderedOp = getRenderedOp(tiffFile);
    ParameterBlock parameterBlock = new ParameterBlock();
    parameterBlock.addSource(renderedOp);
    parameterBlock.add(xScale);
    parameterBlock.add(yScale);
    renderedOp = JAI.create("scale", parameterBlock);
    return JAI.create("scale", parameterBlock);
  }
}
