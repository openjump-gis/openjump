package com.vividsolutions.jump.workbench.imagery.graphic;

import java.util.Arrays;

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
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.io.FilenameUtils;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;

public class CommonsImageFactory extends AbstractGraphicImageFactory {
  public String getTypeName() {
    return "Commons Imaging";
  }

  public ReferencedImage createImage(String location) {
    // try to read the format from file header
    try {
      ByteSource byteSource = ByteSource.inputStream(CompressedFile.openFile(location), UriUtil.getFileName(location));
      ImageFormat format = Imaging.guessFormat(byteSource);
      if (format.equals(ImageFormats.TIFF))
        return new CommonsTIFFImage(location, null);
    } catch (Exception e) {
      Logger.error(e);
    }
    // or use file extension
    String ext = FilenameUtils.getExtension(location).toLowerCase();
    if (ext.matches("tiff?"))
      return new CommonsTIFFImage(location, null);

    return new CommonsImage(location, null);
  }

  public boolean isAvailable(WorkbenchContext context) {
    Class c = null;
    try {
      c = this.getClass().getClassLoader().loadClass("org.apache.commons.imaging.Imaging");
      if (c == null)
        return false;

      for (ImageFormat fmt : ImageFormats.values()) {
        // skip mysterious unknown entry
        if (fmt.getName().equalsIgnoreCase("unknown"))
          continue;
        addExtensions(Arrays.asList(fmt.getExtensions()));
      }

      return true;
    } catch (ClassNotFoundException e) {
      // eat it
    }

    return false;
  }

}