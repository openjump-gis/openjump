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
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida 32548
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;

/**
 * An image whose source is a bitmap
 * 
 * Much of this code was donated by Larry Becker and Robert Littlefield of
 * Integrated Systems Analysts, Inc.
 */
public class IOGraphicImage extends JAIGraphicImage

{

  public IOGraphicImage(String uri, WorldFile wf) {
    super(uri, wf);
  }

  protected void initImage() throws ReferencedImageException {
    BufferedImage image = getImage();
    if (image != null)
      return;
    InputStream is = null;
    try {
      String uri = getUri();
      is = CompressedFile.openFile(uri);
      image = ImageIO.read(is);
      if (image==null)
        throw new IOException("ImageIO read returned null");
      setImage(image);
      setType(FileUtil.getExtension(CompressedFile.getTargetFileWithPath(new URI(uri))));
    } catch (URISyntaxException e) {
      throw new ReferencedImageException("Could not open image file "+getUri(), e);
    } catch (IOException e) {
      throw new ReferencedImageException("Could not open image file "+getUri(), e);
    } finally {
      // close streams on any failure
      close(is);
    }
  }
}