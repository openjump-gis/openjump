package com.vividsolutions.jump.workbench.imagery.graphic;

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
import java.util.HashSet;

import com.sun.media.jai.codec.ImageCodec;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;

public class JAIGraphicImageFactory extends AbstractGraphicImageFactory {
  protected HashSet extensions = new HashSet();

  public String getTypeName() {
    return "JAI";
  }

  public ReferencedImage createImage(String location) throws Exception {
    return new JAIGraphicImage(location, null);
  }

  public boolean isAvailable(WorkbenchContext context) {
    Class c = null, c2 = null;
    try {
      c = this.getClass().getClassLoader().loadClass("javax.media.jai.JAI");
      c2 = this.getClass().getClassLoader()
          .loadClass("com.sun.media.jai.codec.ImageCodec");
      if (c == null || c2 == null)
        return false;

      for (Enumeration e = ImageCodec.getCodecs(); e.hasMoreElements();) {
        ImageCodec codec = (ImageCodec) e.nextElement();
        // System.out.println("JAIGF: "+codec);
        String ext = codec.getFormatName().toLowerCase();
        addExtension(ext);
      }

      // System.out.println(this.getClass().getName()+": "+extensions);
      return true;
    } catch (ClassNotFoundException e) {
      // eat it
    }

    return false;
  }

}