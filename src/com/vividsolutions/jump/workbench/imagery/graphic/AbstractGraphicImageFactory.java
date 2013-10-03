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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;
import com.vividsolutions.jump.workbench.model.Prioritized;

/**
 * Class implementing generic code used in all *Imagefactory classes.
 * 
 * @author ed
 * 
 */
abstract public class AbstractGraphicImageFactory implements
    ReferencedImageFactory, Prioritized {
  protected HashSet extensions = new HashSet();

  abstract public String getTypeName();

  public String getDescription() {
    return "Buffered Image (" + getTypeName() + ")";
  }

  abstract public ReferencedImage createImage(String location) throws Exception;

  public String[] getExtensions() {
    return (String[]) extensions.toArray(new String[] {});
  }

  public boolean isEditableImage(String location) {
    return true;
  }

  abstract public boolean isAvailable(WorkbenchContext context);

  protected void addExtension(String ext) {
    if (ext == null || ext.equals(""))
      // ignore empty extensions
      return;

    ext = ext.toLowerCase();
    if (ext.matches("x?tiff?"))
      extensions.addAll(Arrays.asList(new String[] { "tiff", "tif" }));
    else if (ext.matches("jpe?g?"))
      extensions.addAll(Arrays.asList(new String[] { "jpeg", "jpg" }));
    else if (ext.matches("jb(?:ig)2"))
      extensions.addAll(Arrays.asList(new String[] { "jbig2", "jb2" }));
    else if (ext.matches("rgbe"))
      extensions.addAll(Arrays.asList(new String[] { ext, "hdr" }));
    else
      extensions.add(ext);
  }
  
  protected void addExtensions(Collection<String> exts){
    for (String ext : exts) {
      addExtension(ext);
    }
  }

  public int getPriority() {
    return Prioritized.NOPRIORITY;
  }
}