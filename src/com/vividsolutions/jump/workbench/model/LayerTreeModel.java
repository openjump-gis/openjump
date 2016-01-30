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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.tree.TreePath;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.util.SimpleTreeModel;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterSymbology;

/**
 * JTree model for displaying the Layers, WMSLayers, and other Layerables
 * contained in a LayerManager.
 */
public class LayerTreeModel extends SimpleTreeModel {

    public static class Root {
        private Root() {
        }
    }

    private LayerManagerProxy layerManagerProxy;

    public LayerTreeModel(LayerManagerProxy layerManagerProxy) {
        super(new Root());
        this.layerManagerProxy = layerManagerProxy;
    }
    
    public static class ColorThemingValue {
        private Object value;
        private BasicStyle style;
        private String label;
        
        public ColorThemingValue(Object value, BasicStyle style, String label) {
            this.value = value;
            this.style = style;
            Assert.isTrue(label != null);
            this.label = label;
        }
        @Override
        public String toString() {
            return label;
        }
        @Override
        public boolean equals(Object other) {
            return other instanceof ColorThemingValue
                    && LangUtil.bothNullOrEqual(value,
                            ((ColorThemingValue) other).value)
                    && style == ((ColorThemingValue) other).style;
        }
        public BasicStyle getStyle() {
            return style;
        }
    }    
    
    public static class RasterStyleValueIntv {
       
        private final String colorMapType;
        private final Color color;
        private final Double nextValue;
        private final Double value;
        private final String label;
        private int width;
        private int height;
        
        public RasterStyleValueIntv(
                String colorMapType,
                Color color,
                Double value,
                Double nextValue,
                String label) {
            this.colorMapType = colorMapType;
            this.color = color;
            this.nextValue = nextValue;
            this.value = value;
            this.label = label;
        }

        public String getColorMapType() {
            return colorMapType;
        }

        public Color getColor() {
            return color;
        }
        
        public Double getNextValue() {
            return nextValue;
        }
        
        public Double getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof RasterStyleValueIntv
                    && LangUtil.bothNullOrEqual(value, ((RasterStyleValueIntv) other).value)
                    && this.getValue() == ((RasterStyleValueIntv) other).getValue().doubleValue();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + (this.color != null ? this.color.hashCode() : 0);
            hash = 89 * hash + (this.value != null ? this.value.hashCode() : 0);
            return hash;
        }
        
    }
    
    public static class RasterStyleValueRamp {
        
        private final Double topValue;
        private final Double bottomValue;
        private final Color[] colors;            
        private int width;
        private int height;
        
        public RasterStyleValueRamp(Double topValue, Double bottomValue, Color[] colors) {
            this.topValue = topValue;
            this.bottomValue = bottomValue;
            this.colors = colors;
        }
        
        public RasterStyleValueRamp(Double topValue, Double bottomValue, Color[] colors, int width, int height) {
            this.topValue = topValue;
            this.bottomValue = bottomValue;
            this.colors = colors;
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        @Override
        public String toString() {
            return String.valueOf(bottomValue) + "-" + String.valueOf(topValue);
        }
        
        public Double getTopValue() {
            return  topValue;
        }
        
        public Double getBottomValue() {
            return  bottomValue;
        }
        
        public Color[] getColors() {
            return colors;
        }
        
        @Override
        public boolean equals(Object other) {
            return other instanceof RasterStyleValueRamp
                    && LangUtil.bothNullOrEqual(topValue, ((RasterStyleValueRamp) other).topValue)
                    && LangUtil.bothNullOrEqual(bottomValue, ((RasterStyleValueRamp) other).bottomValue);
        }
        
    }
    
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        for (int i = 0; i < getChildCount(parent); i++) {
            // ColorThemingValue are value objects. [Jon Aquino]
            if (child instanceof ColorThemingValue
                    && getChild(parent, i) instanceof ColorThemingValue
                    && getChild(parent, i).equals(child)) {
                return i;
            }
            
            if (child instanceof RasterStyleValueIntv
                    && getChild(parent, i) instanceof RasterStyleValueIntv
                    && getChild(parent, i).equals(child)) {
                return i;
            }
            
            if (child instanceof RasterStyleValueRamp
                    && getChild(parent, i) instanceof RasterStyleValueRamp
                    && getChild(parent, i).equals(child)) {
                return i;
            }
            
        }
        return super.getIndexOfChild(parent, child);
    }
    
    @Override
    public List getChildren(Object parent) {
        if (parent == getRoot()) {
            return layerManagerProxy.getLayerManager().getCategories();
        }
        if (parent instanceof Category) {
            return ((Category) parent).getLayerables();
        }
        if (parent instanceof Layer
                && ColorThemingStyle.get((Layer) parent).isEnabled()) {
            Map attributeValueToBasicStyleMap = ColorThemingStyle.get(
                    (Layer) parent).getAttributeValueToBasicStyleMap();
            Map attributeValueToLabelMap = ColorThemingStyle
                    .get((Layer) parent).getAttributeValueToLabelMap();
            List colorThemingValues = new ArrayList();
            for (Iterator i = attributeValueToBasicStyleMap.keySet().iterator(); i
                    .hasNext();) {
                Object value = (Object) i.next();
                colorThemingValues.add(new ColorThemingValue(value,
                        (BasicStyle) attributeValueToBasicStyleMap.get(value),
                        (String) attributeValueToLabelMap.get(value)));
            }
            return colorThemingValues;
        }
        if (parent instanceof ColorThemingValue) {
            return Collections.EMPTY_LIST;
        }
    
        if(parent instanceof Layerable) {
            if (parent instanceof RasterImageLayer) {
                
                RasterImageLayer rasterImageLayer = (RasterImageLayer)parent;
                if(rasterImageLayer.getSymbology() != null && rasterImageLayer.getMetadata() != null) {

                    RasterSymbology rasterSymbology = rasterImageLayer.getSymbology();

                    if(!rasterImageLayer.getSymbology().getColorMapType().equals(RasterSymbology.TYPE_RAMP)) {

                        List<RasterStyleValueIntv> styleValues_l = new ArrayList<RasterStyleValueIntv>();

                        Double[] keys = rasterSymbology.getColorMapEntries_tm()
                                .keySet().toArray(new Double[rasterSymbology.getColorMapEntries_tm().size()]);

                        for(int i=0; i<keys.length; i++) {

                            Double key = keys[i];
                            if(!rasterImageLayer.isNoData(key)) {

                                Double nextValue;
                                if(i == keys.length - 1) {
                                    nextValue = rasterImageLayer.getMetadata().getStats().getMax(0);
                                } else {
                                    nextValue = keys[i+1];
                                }

                                Color color = rasterSymbology.getColorMapEntries_tm().get(key);


                                styleValues_l.add(new RasterStyleValueIntv(
                                        rasterSymbology.getColorMapType(),
                                        color,
                                        key,
                                        nextValue,
                                        key.toString()));
                            }

                        }

                        return styleValues_l;

                    } else {

                        List<RasterStyleValueRamp> styleValues_l = new ArrayList<RasterStyleValueRamp>();
              
                        double topValue = rasterImageLayer.getMetadata().getStats().getMax(0);
                        double bottomValue = rasterImageLayer.getMetadata().getStats().getMin(0);

                        Double[] keys = rasterSymbology.getColorMapEntries_tm()
                                .keySet().toArray(new Double[rasterSymbology.getColorMapEntries_tm().size()]);

                        List<Color> colors_l = new ArrayList<Color>();
                        for(int i=keys.length-1; i>=0; i--) {
                            Double key = keys[i];
                            if(!rasterImageLayer.isNoData(key)) {
                                Color color = rasterSymbology.getColorMapEntries_tm().get(key);
                                colors_l.add(color);
                            }
                        }

                        Color[] colors = colors_l.toArray(new Color[colors_l.size()]);

                        RasterStyleValueRamp ramp = new RasterStyleValueRamp(
                                topValue,
                                bottomValue,
                                colors);

                        styleValues_l.add(ramp);

                        return styleValues_l;

                    }
                }
            }
            return new ArrayList();
        }
        if (parent instanceof RasterStyleValueIntv) {
            return Collections.EMPTY_LIST;
        }
        
        if (parent instanceof RasterStyleValueRamp) {
            return Collections.EMPTY_LIST;
        }
        
        
        Assert.shouldNeverReachHere(parent.getClass().getName());
        return null;
    }
    
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        if (path.getLastPathComponent() instanceof Layerable) {
            ((Layerable)path.getLastPathComponent()).setName((String)newValue);
        }
        else if (path.getLastPathComponent() instanceof Category) {
            ((Category)path.getLastPathComponent()).setName((String)newValue);
        }
        else {
            Assert.shouldNeverReachHere();
        }
    }    

}
