



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

package com.vividsolutions.wms.ui;

import java.awt.*;

/**
 * A simple canvas which draws a given image in the upper left corner of itself.
 *
 * @author  Chris Hodgson chodgson@refractions.net
 */
public class ImageCanvas extends Canvas {
  
  Image img;
  
  /** 
   * Creates a new instance of ImageCanvas.
   */
  public ImageCanvas() {
    super();
  }
  
  /**
   * Sets the Image to be displayed on the canvas.
   * @param img the Image to be displayed
   */
  public void setImage( Image img ) {
    this.img = img;
  }
  
  /**
   * Paints its Image (if specified) in the upper-left hand corner of itself.
   * @param g the Graphics object on which to paint.
   */
  public void paint( Graphics g ) {
    if( img != null ) {
      g.drawImage( img, 0, 0, this );
    }
  }
}
