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

package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jump.util.Blackboard;

/**
 * A "sheet" of spatial data, overlaid on other "sheets".
 */
public interface Layerable {

  void setName(String name);

  String getName();

  void setVisible(boolean visible);

  boolean isVisible();

  void setEditable(boolean editable);

  boolean isEditable();

  boolean isReadonly();

  void setReadonly(boolean value);

  boolean isSelectable();

  void setSelectable(boolean value);

  LayerManager getLayerManager();

  /**
   * Called by Java2XML
   */
  void setLayerManager(LayerManager layerManager);

  Blackboard getBlackboard();

  /**
   * @return the larger units/pixel value
   */
  Double getMinScale();

  Layerable setMinScale(Double minScale);

  /**
   * @return the smaller units/pixel value
   */
  Double getMaxScale();

  Layerable setMaxScale(Double maxScale);

  boolean isScaleDependentRenderingEnabled();

  Layerable setScaleDependentRenderingEnabled(
      boolean scaleDependentRenderingEnabled);

}
