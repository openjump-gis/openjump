
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

package com.vividsolutions.jump.workbench.ui.cursortool;

import com.vividsolutions.jts.geom.Coordinate;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openjump.core.CheckOS;

import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.ui.InfoFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import java.util.List;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayer.RasterDataNotFoundException;

public class FeatureInfoTool extends SpecifyFeaturesTool {

    public static final ImageIcon ICON = IconLoader.icon("information_20x20.png");
    public FeatureInfoTool() {
        setColor(Color.magenta);
    }

    public Icon getIcon() {
        return ICON;
    }

    public Cursor getCursor() {
      // [ede 03.2103] linux currently support only 2 color cursors
      Image i = !CheckOS.isLinux() ? IconLoader.image("information_cursor.png")
          : IconLoader.image("information_cursor_2color.gif");
      return createCursor(i);
    }
    @Override
    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();
        InfoFrame infoFrame = getTaskFrame().getInfoFrame();
        if (!wasShiftPressed()) {
            infoFrame.getModel().clear();
        }
        Map map = layerToSpecifiedFeaturesMap();
        Iterator i = map.keySet().iterator();
        while(i.hasNext()){
            Layer layer = (Layer) i.next();
            if (layer.getName().equals(FenceLayerFinder.LAYER_NAME)) {
                continue;
            }
            Collection features = (Collection) map.get(layer);
            infoFrame.getModel().add(layer, features);
        }
        
        // Raster data
        Coordinate coord = getPanel().getViewport().toModelCoordinate(getViewSource());
        List<Layerable> layerables_l = getWorkbench().getContext().getLayerManager().getLayerables(RasterImageLayer.class);
        
        String[] layerNames = new String[layerables_l.size()];
        String[] cellValues = new String[layerables_l.size()];
        
        for(int l=0; l<layerables_l.size(); l++) {
            if(layerables_l.get(l) instanceof RasterImageLayer){
                RasterImageLayer rasterImageLayer = (RasterImageLayer) layerables_l.get(l);
                if(rasterImageLayer != null) {

                    layerNames[l] = rasterImageLayer.getName();

                    try {
                        
                        cellValues[l] = "";
                        for(int b=0; b<rasterImageLayer.getNumBands(); b++) {
                            Double cellValue = rasterImageLayer.getCellValue(coord.x, coord.y, b);
                            if(cellValue != null) {
                                if(rasterImageLayer.isNoData(cellValue)) {
                                    cellValues[l] = Double.toString(Double.NaN);
                                } else {
                                    cellValues[l] = cellValues[l].concat(Double.toString(cellValue));
                                }
                            }
                            cellValues[l] = cellValues[l].concat(";");
                        }
                        
                    } catch(RasterDataNotFoundException ex) {
                        cellValues[l] = "???";
                    }
                        
                    

                }
            }
        }
            
        infoFrame.setRasterValues(layerNames, cellValues);
        
        infoFrame.surface();
    }
}
