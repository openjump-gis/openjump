/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package de.latlon.deejump.plugin.style;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.SquareVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;
public class DeeColorThemingStyle implements Style {
	public DeeColorThemingStyle() {
		//Parameterless constructor for Java2XML. [Jon Aquino]
	    //UT
	    this( "", new HashMap(), new HashMap(),new HashMap(), new BasicStyle());
	}

	/**
	 * Call this method after calling #setAttributeValueToBasicStyleMap rather
	 * than before.
	 */
	public void setAlpha(int alpha) {
		defaultStyle.setAlpha(alpha);
		for (Iterator i = attributeValueToBasicStyleMap.values().iterator(); i
				.hasNext();) {
			BasicStyle style = (BasicStyle) i.next();
			style.setAlpha(alpha);
		}
	}
	/**
	 * Call this method after calling #setAttributeValueToBasicStyleMap rather
	 * than before.
	 */
	public void setLineWidth(int lineWidth) {
		defaultStyle.setLineWidth(lineWidth);
		for (Iterator i = attributeValueToBasicStyleMap.values().iterator(); i
				.hasNext();) {
			BasicStyle style = (BasicStyle) i.next();
			style.setLineWidth(lineWidth);
		}
	}
	/**
	 * @param defaultStyle
	 *                  <code>null</code> to prevent drawing features with a null
	 *                  attribute value
	 */
    public DeeColorThemingStyle(String attributeName,
            Map attributeValueToBasicStyleMap, BasicStyle defaultStyle) {
        this(attributeName, attributeValueToBasicStyleMap,
                attributeValueToLabelMap(attributeValueToBasicStyleMap),
                defaultStyle);

    }
	public DeeColorThemingStyle(String attributeName,
			Map attributeValueToBasicStyleMap,
			Map attributeValueToLabelMap,
			BasicStyle defaultStyle) {
		this(attributeName, attributeValueToBasicStyleMap,
                attributeValueToLabelMap(attributeValueToBasicStyleMap),
				attributeValueToVertexStyleMap(attributeValueToBasicStyleMap),
                defaultStyle);
		
	}    
	
	public DeeColorThemingStyle(String attributeName,
			Map attributeValueToBasicStyleMap,
			Map attributeValueToLabelMap,
			Map attributeValueToVertexStyleMap,
			BasicStyle defaultStyle) {
		setAttributeName(attributeName);
		setAttributeValueToBasicStyleMap(attributeValueToBasicStyleMap);
        setAttributeValueToLabelMap(attributeValueToLabelMap);
        setAttributeValueToVertexStyleMap(attributeValueToVertexStyleMap);
		setDefaultStyle(defaultStyle);
		
	}    
	/**
	 * @param attributeValueToVertexStyleMap2
	 */
	public void setAttributeValueToVertexStyleMap(Map attributeValueToVertexStyleMap) {
		this.attributeValueToVertexStyleMap = attributeValueToVertexStyleMap;
		
	}

	private static Map attributeValueToLabelMap(Map attributeValueToBasicStyleMap) {
        // Be sure to use the same Map class -- it may be a RangeTreeMap [Jon Aquino 2005-07-30]
        Map attributeValueToLabelMap = (Map) LangUtil.newInstance(attributeValueToBasicStyleMap.getClass());
        for (Iterator i = attributeValueToBasicStyleMap.keySet().iterator(); i.hasNext(); ) {
            Object value = i.next();
            attributeValueToLabelMap.put(value, value.toString());
        }
        return attributeValueToLabelMap;
    }
	
	private static Map attributeValueToVertexStyleMap(Map attributeValueToBasicStyleMap) {
        Map attributeValueToVertexStyleMap = (Map) LangUtil.newInstance(attributeValueToBasicStyleMap.getClass());
        for (Iterator i = attributeValueToBasicStyleMap.keySet().iterator(); i.hasNext(); ) {
            Object value = i.next();
            attributeValueToVertexStyleMap.put(value, value.toString());
        }
        return attributeValueToVertexStyleMap;
    }

    private BasicStyle defaultStyle;
	public void paint(Feature f, Graphics2D g, Viewport viewport)
			throws Exception {
		getStyle(f).paint(f, g, viewport);
		getVertexStyle(f).paint(f, g, viewport);
	}
	/**
	 * @param f
	 * @return
	 */
	private VertexStyle getVertexStyle(Feature feature) {
	    //TODO why star vertex style as default?!??!?!?
		VertexStyle style = attributeName != null
		&& feature.getSchema().hasAttribute(attributeName)
		&& feature.getAttribute(attributeName) != null ? (VertexStyle) attributeValueToVertexStyleMap
		.get(trimIfString(feature.getAttribute(attributeName)))
		: new SquareVertexStyle();
       return style == null ? new SquareVertexStyle() : style;
		
		
	}

	private BasicStyle getStyle(Feature feature) {
		//Attribute name will be null if a layer has only a spatial attribute.
		// [Jon Aquino]
		//If we can't find an attribute with this name, just use the
		//defaultStyle. The attribute may have been deleted. [Jon Aquino]
		BasicStyle style = attributeName != null
				&& feature.getSchema().hasAttribute(attributeName)
				&& feature.getAttribute(attributeName) != null ? (BasicStyle) attributeValueToBasicStyleMap
				.get(trimIfString(feature.getAttribute(attributeName)))
				: defaultStyle;
		return style == null ? defaultStyle : style;
	}
	public static Object trimIfString(Object object) {
		return object != null && object instanceof String ? ((String) object)
				.trim() : object;
	}
	private Layer layer;
	private Map attributeValueToBasicStyleMap;
    private Map attributeValueToLabelMap;
    private Map attributeValueToVertexStyleMap;
	private String attributeName;
	public Object clone() {
		try {
			DeeColorThemingStyle clone = (DeeColorThemingStyle) super.clone();
			//Deep-copy the map, to facilitate undo. [Jon Aquino]
            clone.attributeValueToBasicStyleMap = (Map) attributeValueToBasicStyleMap.getClass()
					.newInstance();
            
            
            clone.attributeValueToVertexStyleMap = (Map) attributeValueToVertexStyleMap.getClass()
			        .newInstance();
            
			for (Iterator i = attributeValueToBasicStyleMap.keySet().iterator(); i
					.hasNext();) {
				Object attribute = (Object) i.next();
							
				BasicStyle bs = (BasicStyle)((BasicStyle) attributeValueToBasicStyleMap
				.get(attribute)).clone();
				
				VertexStyle vs = (VertexStyle)((VertexStyle) attributeValueToVertexStyleMap
    							.get(attribute)).clone(); 
				
                    clone.attributeValueToBasicStyleMap.put(attribute,
						new PointSymbolizerBasicStyle( bs, vs));
                
                    clone.attributeValueToVertexStyleMap.put(attribute,
    					vs);
			}
			clone.attributeValueToLabelMap = (Map) attributeValueToLabelMap.getClass().newInstance();
            clone.attributeValueToLabelMap.putAll(attributeValueToLabelMap);
            
            return clone;
		} catch (InstantiationException e) {
			Assert.shouldNeverReachHere();
			return null;
		} catch (IllegalAccessException e) {
			Assert.shouldNeverReachHere();
			return null;
		} catch (CloneNotSupportedException e) {
			Assert.shouldNeverReachHere();
			return null;
		}
	}
	/**
	 * @return null if the layer has no non-spatial attributes
	 */
	public String getAttributeName() {
		return attributeName;
	}
	/**
	 * You can set the keys to Ranges if the Map is a Range.RangeTreeMap. But
	 * don't mix Ranges and non-Ranges -- the UI expects homogeneity in this
	 * regard (i.e. to test whether or not there are ranges, only the first
	 * attribute value is tested).
	 */
	public void setAttributeValueToBasicStyleMap(Map attributeValueToBasicStyleMap) {
		this.attributeValueToBasicStyleMap = attributeValueToBasicStyleMap;
	}
    /**
     * You can set the keys to Ranges if the Map is a Range.RangeTreeMap. But
     * don't mix Ranges and non-Ranges -- the UI expects homogeneity in this
     * regard (i.e. to test whether or not there are ranges, only the first
     * attribute value is tested).
     */
    public void setAttributeValueToLabelMap(Map attributeValueToLabelMap) {
        this.attributeValueToLabelMap = attributeValueToLabelMap;
    }
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public Map getAttributeValueToBasicStyleMap() {
		return attributeValueToBasicStyleMap;
	}
    public Map getAttributeValueToLabelMap() {
        return attributeValueToLabelMap;
    }
    public Map getAttributeValueToVertexStyleMap(){
    	return attributeValueToVertexStyleMap;
    }
	private boolean enabled = false;
	public void initialize(Layer layer) {
		this.layer = layer;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public static DeeColorThemingStyle get(Layer layer) {
		if ((DeeColorThemingStyle) layer.getStyle(DeeColorThemingStyle.class) == null) {
		    DeeColorThemingStyle colorThemingStyle = new DeeColorThemingStyle(
					pickNonSpatialAttributeName(layer
							.getFeatureCollectionWrapper().getFeatureSchema()),
					new HashMap(), new BasicStyle(Color.lightGray));
			layer.addStyle(colorThemingStyle);
		}
		return (DeeColorThemingStyle) layer.getStyle(DeeColorThemingStyle.class);
	}
	private static String pickNonSpatialAttributeName(FeatureSchema schema) {
		for (int i = 0; i < schema.getAttributeCount(); i++) {
			if (schema.getGeometryIndex() != i) {
				return schema.getAttributeName(i);
			}
		}
		return null;
	}
	public BasicStyle getDefaultStyle() {
		return defaultStyle;
	}
	public void setDefaultStyle(BasicStyle defaultStyle) {
		this.defaultStyle = defaultStyle;
	}
	
	
}