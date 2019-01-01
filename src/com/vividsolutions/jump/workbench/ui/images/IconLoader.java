
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

package com.vividsolutions.jump.workbench.ui.images;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.workbench.Logger;

/**
 * Gets an icon from this class' package.
 */
public class IconLoader {
  // cache icons, in case they are requested more than once to speedup OJ start
  private static HashMap<String, ImageIcon> iconCache = new HashMap<>();
  //default icon if the choosen one doesn't exist from Kosmo SAIG
  private static ImageIcon DEFAULT_UNKNOWN_ICON = new ImageIcon(IconLoader.class.getResource("saig/default_icon.png"));

  public static ImageIcon icon(String filename) {
    return getIcon(filename);
  }

  public static BufferedImage image(String filename) {
    ImageIcon icon = getIcon(filename);
    Image image = icon.getImage();

    // create a buffered image with transparency
    BufferedImage bufImg = new BufferedImage(image.getWidth(null),
        image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

    // draw the image on to the buffered image
    Graphics2D bGr = bufImg.createGraphics();
    bGr.drawImage(image, 0, 0, null);
    bGr.dispose();

    return bufImg;
  }

  protected static ImageIcon getIcon(String filename) {
    // automagically search through subfolders
    filename = resolveFile(filename);
    return getIcon(IconLoader.class, filename);
  }

  // utility method for the icon loaders based on this 
  // like below but protected against null URLs because of non existing files/paths
  protected static ImageIcon getIcon(Class clazz, String filename) {
    URL url = clazz.getResource(filename);

    // didn't find the file via resource loading 
    if (url == null) {
      Logger.error("Couldn't find '"+filename+"' via resource loading. Returning dummy default icon for now.");
      return DEFAULT_UNKNOWN_ICON;
    }

    return getIcon(url);
  }

  protected static ImageIcon getIcon(URL url) {
    ImageIcon icon = null;
    // check for null
    if (url == null)
      throw new InvalidParameterException("parameter url must not be null.");

    String key = url.toExternalForm();
    // try loading the image
    try {
      // check cache
      icon = iconCache.get(key);
      if (icon != null)
        return icon;

      // we keep using ImageIcon, as other loaders like ImageIO, commons Imaging
      // choke on our icon gifs currently
      icon = new ImageIcon(url);
      // cache the image
      iconCache.put(key, icon);
    } catch (Exception e) {
      icon = DEFAULT_UNKNOWN_ICON;
      Logger.error("Error loading '"+key+"'. Using dummy default icon for now.", e);
    }

    return icon;
  }

  /**
   * utility method to automagically resolve images that moved into their
   * appropriate iconset subfolders for legacy code
   * 
   * @param filename
   * @return
   */
  protected static String resolveFile(String filename) {
    // iterate over each location, return on first hit
    for (String path : new String[] { "", "famfam/", "fugue/", "saig/" }) {
      if (IconLoader.class.getResource(path + filename) != null)
        return path + filename;
    }

    // if push comes to shove, we let the calling method deal w/ the
    // consequences, exactly as it was before
    return filename;
  }
}
