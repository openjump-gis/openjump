package com.vividsolutions.jump.workbench.imagery;

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
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.AlphaSetting;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

/**
 * A JUMP style that will paint images
 */
public class ReferencedImageStyle implements Style, AlphaSetting {
  boolean enabled = true;
  int alpha = 255;

  public ReferencedImageStyle() {
  }

  private ImageryLayerDataset imageryLayerDataset = new ImageryLayerDataset();

  public void paint(Feature f, java.awt.Graphics2D g, Viewport viewport)
      throws Exception {
    if (imageryLayerDataset.referencedImage(f) == null) {
      return;
    }

    ReferencedImage img = imageryLayerDataset.referencedImage(f);
    if (img instanceof AlphaSetting)
      ((AlphaSetting) img).setAlpha(alpha);
    img.paint(f, g, viewport);

  }

  public void initialize(Layer l) {
  }

  public Object clone() {
    return new ReferencedImageStyle();
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public ImageryLayerDataset getImageryLayerDataset() {
    return imageryLayerDataset;
  }

  public int getAlpha() {
    return alpha;
  }

  public void setAlpha(int alpha) {
    this.alpha = alpha;
  }
}