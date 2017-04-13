package com.vividsolutions.jump.workbench.imagery.geoimg;

/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
import it.geosolutions.imageio.gdalframework.GDALImageReaderSpi;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import org.geotiff.image.jai.GeoTIFFDescriptor;

import com.sun.media.jai.codec.ImageCodec;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.graphic.AbstractGraphicImageFactory;
import com.vividsolutions.jump.workbench.imagery.imageio.JP2GDALEcwImageReaderSpi;
import com.vividsolutions.jump.workbench.imagery.imageio.JP2GDALJasperImageReaderSpi;
import com.vividsolutions.jump.workbench.imagery.imageio.JP2GDALOpenJPEGImageReaderSpi;
import com.vividsolutions.jump.workbench.model.Prioritized;

/**
 * A factory for referenced images.
 */
public class GeoImageFactory extends AbstractGraphicImageFactory {
  Object loader = null;

  public void setLoader(Object loader) {
    this.loader = loader;
  }

  public Object getLoader() {
    return loader;
  }

  public GeoImageFactory() {
    // register optional codecs TODO: how to register them more effortlessly?
    IIORegistry.getDefaultInstance().registerServiceProvider(
        new JP2GDALOpenJPEGImageReaderSpi());
    IIORegistry.getDefaultInstance().registerServiceProvider(
        new JP2GDALEcwImageReaderSpi());
    IIORegistry.getDefaultInstance().registerServiceProvider(
        new JP2GDALJasperImageReaderSpi());

    // initialize extensions
    final Iterator<? extends ImageReaderSpi> iter = IIORegistry
        .getDefaultInstance().getServiceProviders(ImageReaderSpi.class, true);
    for (; iter.hasNext();) {
      ImageReaderSpi reader = (ImageReaderSpi) iter.next();
      String[] exts = reader.getFileSuffixes();
      // this is mainly for the NITF imageio-ext reader, which has one empty ext
      // supposedly because too much file extensions (?) exist for this format
      if (exts.length == 0
          || (exts.length == 1 && exts[0] instanceof String && exts[0].trim()
              .isEmpty()))
        addExtension("*");
      else
        addExtensions(Arrays.asList(exts));
    }

    // add plain JAI codecs extensions
    for (Enumeration<ImageCodec> e = ImageCodec.getCodecs(); e.hasMoreElements();) {
      ImageCodec codec = (ImageCodec) e.nextElement();
      String ext = codec.getFormatName().toLowerCase();
      addExtension(ext);
    }

  }

  // GeoImageFactory with an enforced specific loader
  public GeoImageFactory(Object loader) {
    this.loader = loader;
    if (loader instanceof ImageCodec) {
      String ext = ((ImageCodec) loader).getFormatName().toLowerCase();
      addExtension(ext);
    } else if (loader instanceof ImageReaderSpi) {
      ImageReaderSpi reader = (ImageReaderSpi) loader;
      String[] exts = reader.getFileSuffixes();
      if (exts.length == 0
          || (exts.length == 1 && exts[0] instanceof String && exts[0].trim()
              .isEmpty()))
        addExtension("*");
      else
        addExtensions(Arrays.asList(exts));
    }
  }

  public String getTypeName() {
    return "Referenced Image";
  }

  public ReferencedImage createImage(String location) throws Exception {
    // System.out.println("GIF: " + getDescription());
    return new GeoImage(location, loader);
  }

  public String getDescription() {
    return getTypeName() + " " + loaderString();
  }

  /**
   * prepare a proper description of a forced loader
   * currently "(description, version x.x, vendor)"
   */
  private String loaderString() {
    // a specified loader
    if (loader != null) {
      String loaderString ="";
      // imageIO readers
      if (loader instanceof ImageReaderSpi) {
        // workaround for nasty mac error preventing startup
        // "java.lang.NoSuchMethodError: com.sun.media.imageioimpl.common.PackageUtil.getSpecificationTitle()"
        try {
          loaderString = ((ImageReaderSpi) loader).getDescription(Locale
              .getDefault());
          loaderString = loaderString.replace(
              "Java Advanced Imaging Image I/O Tools", "ImageIO");
        }
        catch (NoSuchMethodError e) {
          loaderString = loader.getClass().getSimpleName();
          loaderString = loaderString.replace("ImageReaderSpi", "");
          loaderString += " (" + loader.getClass().getPackage().getName() + ")";
        }
        
        if (loader instanceof GDALImageReaderSpi) 
          loaderString = "GDAL " + loaderString;
        // always include version info
        if (!loaderString.toLowerCase().contains("version"))
          loaderString += ", version " + ((ImageReaderSpi) loader).getVersion();
        
        loaderString += ", "+((ImageReaderSpi) loader).getVendorName();
      } 
      // JAI codecs
      else if (loader instanceof ImageCodec) {
        loaderString = "JAI " + ((ImageCodec) loader).getFormatName().toUpperCase();
      }
      // all else by classname
      else {
        loaderString = loader.getClass().getName();
      }
      
      return "("+loaderString+")";
    }
    // or automatic
    return "(ImageIO[ext],JAI)";
  }

  // returns the priority of this factory
  public int getPriority() {
    // the autoload (w/o defined loader) has topmost priority
    if (loader == null)
      return 0;

    return getPriority(loader);
  }

  // return a pririty for the given loader object
  public static int getPriority(Object loader) {
    String name = loader.getClass().getName();
    
    // some special cases
    if (loader instanceof JP2GDALOpenJPEGImageReaderSpi)
      return Prioritized.NOPRIORITY; // currently very unstable
    if (name.equals("it.geosolutions.imageio.plugins.jp2ecw.JP2GDALEcwImageReaderSpi"))
      return Prioritized.NOPRIORITY; // replaced by our patched version under com.vividsolutions.jump.workbench.imagery
    
    // we've got some patched
    if (name.startsWith("com.vividsolutions.jump.workbench.imagery"))
      return 10;
    // prefer imageio-ext readers
    else if (name.startsWith("it.geosolutions.imageio")){
      // prefer plain java readers
      if (loader instanceof GDALImageReaderSpi)
        return 25;
      return 20;
    }
    // next are sun's imageio readers
    else if (name.startsWith("com.sun.media.imageio"))
      return 30;
    // next in line are all other imageio readers
    else if (loader instanceof ImageReaderSpi)
      return 40;

    // return prio or hardcoded 100 priority,
    // after all GeoImage is supposed to be superior
    return 100;
  }

  public boolean isAvailable(WorkbenchContext context) {

    // check JAI availability (usually part of jdk)
    Class c = null, c2 = null;
    try {
      c = this.getClass().getClassLoader().loadClass("javax.media.jai.JAI");
      c2 = this.getClass().getClassLoader()
          .loadClass("com.sun.media.jai.codec.ImageCodec");
      if (c == null || c2 == null)
        return false;
    } catch (ClassNotFoundException e) {
      return false;
    }
    
    // register xtiff codec, mainly for reading geotiff tags
    if (ImageCodec.getCodec("xtiff") == null) {
      GeoTIFFDescriptor.register();
    }
    
    // set up imagio caching, not sure if this really has an effect
    File temp = new File(System.getProperty("java.io.tmpdir"));
//    System.out.println("GIF temp: "+temp);
    if (temp.isDirectory() && temp.exists()) {
      ImageIO.setCacheDirectory(temp);
      ImageIO.setUseCache(true);
    }
    // print a list of available GDAL drivers
//    List a = new SortedList();
//    for (int i = 0; GDALUtilities.isGDALAvailable()
//        && i < gdal.GetDriverCount(); i++) {
//      Driver d = gdal.GetDriver(i);
//      String e = d.GetMetadataItem(gdalconst.GDAL_DMD_EXTENSION);
//      a.add(d.getShortName() + "(" + e + ")");
//    }
//    for (Object n : a) {
//      System.out.print(n + ",");
//    }
//    System.out.println();
    
    // imageio is part of jdk, so assume we are avail
    return true;
  }
  
}