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
package com.vividsolutions.jump.workbench.ui.renderer.style;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * A Style mapping different basic styles for different attribute values.
 */
public class ColorThemingStyle implements Style, AlphaSetting {

	public ColorThemingStyle() {
		//Parameterless constructor for Java2XML. [Jon Aquino]
	}

	/**
	 * Call this method after calling #setAttributeValueToBasicStyleMap rather
	 * than before.
	 */
	public void setAlpha(int alpha) {
        if (isGlobalTransparencyEnabled()) {
            defaultStyle.setAlpha(alpha);
            for (BasicStyle style : attributeValueToBasicStyleMap.values()) {
                style.setAlpha(alpha);
            }
        }
	}

	/**
	 * Call this method after calling #setAttributeValueToBasicStyleMap rather
	 * than before.
	 */
	public void setLineWidth(int lineWidth) {
		if (isGlobalLineWidthEnabled()) {
            defaultStyle.setLineWidth(lineWidth);
            for (BasicStyle style : attributeValueToBasicStyleMap.values()) {
                style.setLineWidth(lineWidth);
            }
        }
	}

	/**
     * @param attributeName name of the attribute used to choose the feature Style
     * @param attributeValueToBasicStyleMap map attribute values (or range) to styles
	 * @param defaultStyle style used for features with a null attribute value.
     *      <code>null</code> to prevent drawing features with a null attribute value.
	 */
    public ColorThemingStyle(String attributeName,
            Map<Object,BasicStyle> attributeValueToBasicStyleMap,
            BasicStyle defaultStyle) {
        this(attributeName, attributeValueToBasicStyleMap,
                attributeValueToLabelMap(attributeValueToBasicStyleMap),
                defaultStyle);
        // [sstein: 2.Dec.06] i guess this constructor comes from Erwan to
        // allow different types of classing
    }

    /**
     * @param attributeName name of the attribute used to choose the feature Style
     * @param attributeValueToBasicStyleMap map attribute values (or range) to styles
     * @param attributeValueToLabelMap map attribute values (or range) to labels
     * @param defaultStyle style used for features with a null attribute value.
     *      <code>null</code> to prevent drawing features with a null attribute value.
     */
	public ColorThemingStyle(String attributeName,
			Map<Object,BasicStyle> attributeValueToBasicStyleMap,
            Map<Object,String> attributeValueToLabelMap, BasicStyle defaultStyle) {
		setAttributeName(attributeName);
		setAttributeValueToBasicStyleMap(attributeValueToBasicStyleMap);
        setAttributeValueToLabelMap(attributeValueToLabelMap);
		setDefaultStyle(defaultStyle);
	}

    /**
     * Returns a map mapping attribute values (or range) to labels from the
     * map mapping attribute values (or range) to {@link BasicStyle}s.
     * @param attributeValueToBasicStyleMap the values to BasicStyles map
     * @return a values to labels map
     */
	private static Map<Object,String> attributeValueToLabelMap(
            Map<Object,BasicStyle> attributeValueToBasicStyleMap) {
        // Be sure to use the same Map class -- it may be a RangeTreeMap [Jon Aquino 2005-07-30]
        Map<Object,String> attributeValueToLabelMap =
                (Map<Object,String>) LangUtil.newInstance(attributeValueToBasicStyleMap.getClass());
        for (Object value : attributeValueToBasicStyleMap.keySet()) {
            attributeValueToLabelMap.put(value, value.toString());
        }
        return attributeValueToLabelMap;
    }

    private BasicStyle defaultStyle;

    private boolean globalTransparencyEnabled = true;
    private boolean globalLineWidthEnabled = true;
    private boolean vertexStyleEnabled;

	public void paint(Feature f, Graphics2D g, Viewport viewport)
			throws Exception {
		getStyle(f).paint(f, g, viewport);
	}

    private BasicStyle getStyle(Feature feature) {
		//Attribute name will be null if a layer has only a spatial attribute [Jon Aquino]
		//If we can't find an attribute with this name, just use the defaultStyle.
		// The attribute may have been deleted. [Jon Aquino]
		// If the attribute data type for color theming has been changed -
		// throws multiple exceptions and the layer dissappears due to the 
		// fact that it can't find the style in the valuetobasicstyle map.
		// Solved here by catching the exception and returning the default style 
		// (just like when the attribute name has been changed). [Ed Deen]
		BasicStyle style = null;
		try {
            style = attributeName != null
			        && feature.getSchema().hasAttribute(attributeName)
					&& feature.getAttribute(attributeName) != null ?
                    attributeValueToBasicStyleMap.get(trimIfString(feature.getAttribute(attributeName)))
			        : defaultStyle;
		}
		catch (ClassCastException e) {
			// Do Nothing
		}
		return style == null ? defaultStyle : style;
	}

	public static Object trimIfString(Object object) {
		return (object != null && object instanceof String) ?
                ((String) object).trim() : object;
	}

	private Layer layer;

    private Map<Object,BasicStyle> attributeValueToBasicStyleMap
            = new HashMap<Object,BasicStyle>(); //[sstein 2.Dec.06] added = new Hashmap

    private Map<Object,String> attributeValueToLabelMap;

    private String attributeName;

	//[sstein 2.Dec.06] note: some things here are different. I am not sure if the changes
	// come from changes by VividSolution or preparations for different classing by Erwan
    @Override
	public Object clone() {
		try {
			ColorThemingStyle clone = (ColorThemingStyle) super.clone();
			//Deep-copy the map, to facilitate undo. [Jon Aquino]
            clone.attributeValueToBasicStyleMap =
                    (Map<Object,BasicStyle>)attributeValueToBasicStyleMap.getClass().newInstance();
			for (Object attribute : attributeValueToBasicStyleMap.keySet()) {
                clone.attributeValueToBasicStyleMap.put(attribute,
						(BasicStyle)(attributeValueToBasicStyleMap
								.get(attribute)).clone());
			}
            clone.attributeValueToLabelMap =
                    (Map<Object,String>)attributeValueToLabelMap.getClass().newInstance();
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
	public void setAttributeValueToBasicStyleMap(
            Map<Object,BasicStyle> attributeValueToBasicStyleMap) {
		this.attributeValueToBasicStyleMap = attributeValueToBasicStyleMap;
	}

    /**
     * You can set the keys to Ranges if the Map is a Range.RangeTreeMap. But
     * don't mix Ranges and non-Ranges -- the UI expects homogeneity in this
     * regard (i.e. to test whether or not there are ranges, only the first
     * attribute value is tested).
     */
    public void setAttributeValueToLabelMap(
            Map<Object,String> attributeValueToLabelMap) {
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

    /**
     * Creates a default ColorThemingStyle for this layer if none is already set.
     */
	public static ColorThemingStyle get(Layer layer) {
		if (layer.getStyle(ColorThemingStyle.class) == null) {
			ColorThemingStyle colorThemingStyle = new ColorThemingStyle(
					pickNonSpatialAttributeName(layer
							.getFeatureCollectionWrapper().getFeatureSchema()),
					new HashMap<Object,BasicStyle>(),
                    new XBasicStyle(new BasicStyle(Color.lightGray), new SquareVertexStyle()));
			layer.addStyle(colorThemingStyle);
		}
		return (ColorThemingStyle)layer.getStyle(ColorThemingStyle.class);
	}

    /**
     * Returns the first non spatial attribute name for this schema.
     */
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

    public boolean isGlobalTransparencyEnabled() {
        return globalTransparencyEnabled;
    }

    public void setGlobalTransparencyEnabled(boolean globalTransparencyEnabled) {
        this.globalTransparencyEnabled = globalTransparencyEnabled;
    }

    public boolean isGlobalLineWidthEnabled() {
        return globalLineWidthEnabled;
    }

    public void setGlobalLineWidthEnabled(boolean globalLineWidthEnabled) {
        this.globalLineWidthEnabled = globalLineWidthEnabled;
    }

    public boolean isVertexStyleEnabled() {
        return vertexStyleEnabled;
    }

    public void setVertexStyleEnabled(boolean vertexStyleEnabled) {
        this.vertexStyleEnabled = vertexStyleEnabled;
    }

    public int getAlpha() {
        return defaultStyle.getAlpha();
    }

}