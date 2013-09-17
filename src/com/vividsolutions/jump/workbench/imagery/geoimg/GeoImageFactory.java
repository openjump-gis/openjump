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
import java.util.Enumeration;

import javax.imageio.ImageIO;

import com.sun.media.jai.codec.ImageCodec;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.graphic.AbstractGraphicImageFactory;
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
  }

  // enforce a specific loader
  public GeoImageFactory(Object loader) {
    this.loader = loader;
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

  private String loaderString() {
    // a specified loader
    if (loader != null)
      return "(" + loader.getClass().getName() + ")";
    // or automatic
    else
      return "(ImageIO[ext],JAI)";
  }

  // we have priority over them all
  public int getPriority() {
    // the autoload (w/o defined loader) has topmost priority
    if (loader == null)
      return 0;

    String name = loader.getClass().getName();
    if (name.startsWith("it.geosolutions.imageioimpl"))
      return 10;
    else if (name.startsWith("com.sun.media.imageioimpl"))
      return 20;

    // return prio or hardcoded 50% priority,
    // after all GeoImage is supposed to be superior
    return Prioritized.NOPRIORITY / 2;
  }

  public boolean isAvailable(WorkbenchContext context) {
    // add imageio[-ext] formats
    for (String ext : ImageIO.getReaderFileSuffixes()) {
      addExtension(ext);
    }

    // add plain JAI codecs
    Class c = null, c2 = null;
    try {
      c = this.getClass().getClassLoader().loadClass("javax.media.jai.JAI");
      c2 = this.getClass().getClassLoader()
          .loadClass("com.sun.media.jai.codec.ImageCodec");
      if (c == null || c2 == null)
        return false;

      for (Enumeration e = ImageCodec.getCodecs(); e.hasMoreElements();) {
        ImageCodec codec = (ImageCodec) e.nextElement();
        String ext = codec.getFormatName().toLowerCase();
        addExtension(ext);
      }
    } catch (ClassNotFoundException e) {
      // eat it
    }

    // System.out.println(this.getClass().getName() + ": "
    // + Arrays.toString(getExtensions()));

    // imageio is part of jdk, so assume we are avail
    return true;
  }
}