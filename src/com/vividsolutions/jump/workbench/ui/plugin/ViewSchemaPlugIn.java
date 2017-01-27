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
package com.vividsolutions.jump.workbench.ui.plugin;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.util.FlexibleDateParser;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DateFormat;

import java.util.*;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;


public class ViewSchemaPlugIn extends AbstractPlugIn {
    
    private static final String KEY = ViewSchemaPlugIn.class + " - FRAME";
    private EditingPlugIn editingPlugIn;
    private GeometryFactory factory = new GeometryFactory();
    private WKTReader wktReader = new WKTReader(factory);
    private FlexibleDateParser dateParser = new FlexibleDateParser();
    private DateFormat dateFormatter = DateFormat.getDateInstance();

    private static final String P_LAYER_NAME = "LayerName";
    private static final String P_SCHEMA_MAPPING = "SchemaMapping";
    private static final String P_FORCE_INVALID_CONVERSIONS_TO_NULL = "ForceInvalidConversionsToNull";

    public ViewSchemaPlugIn(EditingPlugIn editingPlugIn) {
        this.editingPlugIn = editingPlugIn;
    }

    public ViewSchemaPlugIn() {
      this.editingPlugIn = EditingPlugIn.getInstance();
    }
    
    public String getName() {
        return I18N.get("ui.plugin.ViewSchemaPlugIn.view-edit-schema");
    }

    private void applyChanges(final Layer layer, final SchemaPanel panel, final WorkbenchFrame workbenchFrame)
        throws Exception {
        if (!panel.isModified()) {
            //User just pressed the Apply button even though he made no edits.
            //Don't truncate the undo history; instead, exit. [Jon Aquino]
            return;
        }

        if (panel.validateInput() != null) {
            throw new Exception(panel.validateInput());
        }

        panel.getModel().removeBlankRows();

        // If the schema is modified, features of the layer are changed,
        // the corresponding attributeTab in the InfoFrame is emptied and
        // the columns are schrinked to a null width.
        // Removing the attributeTab in the InfoFrame avoid these side effects
        for (JInternalFrame iFrame : workbenchFrame.getInternalFrames()) {
            if (iFrame instanceof InfoFrame) ((InfoFrame)iFrame).getModel().remove(layer);
        }

        FeatureSchema newSchema = new FeatureSchema();
        //-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
        FeatureSchema oldSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        //-- end

        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            //-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
            String attributeName = panel.getModel().get(i).getName();
            newSchema.addAttribute(attributeName, panel.getModel().get(i).getType());
            if (oldSchema.hasAttribute(attributeName)) {
                // [mmichaud - 2012-10-13]
                if (newSchema.getAttributeType(attributeName)
                    .equals(oldSchema.getAttributeType(attributeName))) {
                    newSchema.setAttributeReadOnly(
                        newSchema.getAttributeIndex(attributeName),
                        oldSchema.isAttributeReadOnly(oldSchema.getAttributeIndex(attributeName))
                    );
                    newSchema.setOperation(
                        newSchema.getAttributeIndex(attributeName),
                        oldSchema.getOperation(oldSchema.getAttributeIndex(attributeName))
                    );
                }
                else {
                    if (ColorThemingStyle.get(layer) != null) {
                        layer.removeStyle(ColorThemingStyle.get(layer));
                        layer.getBasicStyle().setEnabled(true);
                        layer.fireAppearanceChanged();
                    }
                }
            }
            //-- END: added/modyfied by Ole
        }

        List originalFeatures = layer.getFeatureCollectionWrapper().getFeatures();
        ArrayList tempFeatures = new ArrayList();

        //Two-phase commit. 
        //Phase 1: check that no conversion errors occur. [Jon Aquino]
        for (Iterator i = layer.getFeatureCollectionWrapper().iterator();
                i.hasNext();) {
            Feature feature = (Feature) i.next();
            tempFeatures.add(convert(feature, panel, newSchema));
        }

        //Phase 2: commit. [Jon Aquino]
        for (int i = 0; i < originalFeatures.size(); i++) {
            Feature originalFeature = (Feature) originalFeatures.get(i);
            Feature tempFeature = (Feature) tempFeatures.get(i);

            //Modify existing features rather than creating new features, because
            //there may be references to the existing features (e.g. Attribute Viewers).
            //[Jon Aquino]            
            originalFeature.setSchema(tempFeature.getSchema());
            originalFeature.setAttributes(tempFeature.getAttributes());
        }

        //Non-undoable. [Jon Aquino]
        layer.getLayerManager().getUndoableEditReceiver().getUndoManager()
             .discardAllEdits();
        layer.setFeatureCollection(new FeatureDataset(originalFeatures,
                newSchema));
        layer.fireLayerChanged(LayerEventType.METADATA_CHANGED);

        // [mmichaud 2014-10-05] add parameters to persist plugin execution in a macro
        addParameter(P_FORCE_INVALID_CONVERSIONS_TO_NULL, panel.isForcingInvalidConversionsToNull());
        Map<String,Attribute> schemaMapping = new LinkedHashMap<String, Attribute>();
        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            String attributeName = panel.getModel().get(i).getName();
            Attribute attribute = new Attribute();
            attribute.setType(panel.getModel().get(i).getType());
            if (oldSchema.hasAttribute(attributeName)) {
                int oldIndex = oldSchema.getAttributeIndex(attributeName);
                attribute.setReadOnly(oldSchema.isAttributeReadOnly(oldIndex));
            }
            attribute.setOldIndex(panel.getModel().get(i).getOriginalIndex());
            schemaMapping.put(attributeName, attribute);
        }
        addParameter(P_SCHEMA_MAPPING, schemaMapping);
        //

        // [mmichaud 2009-05-16] update originalIndexes after a modification
        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            panel.getModel().get(i).setOriginalIndex(i);
        }
        // -end
        panel.markAsUnmodified();

    }

    /**
     * Version of applyChanges used when the plugin is executed as a macro
     * @TODO ideally, the same code should be executed in both cases (DRY)
     * @param layer
     * @param schemaMapping
     * @param isForcingInvalidConversionsToNull
     * @throws Exception
     */
    private void applyChanges(final Layer layer, final Map<String,Attribute> schemaMapping,
                              final boolean isForcingInvalidConversionsToNull) throws Exception {

        //-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
        FeatureSchema oldSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        //-- end

        for (String name : schemaMapping.keySet()) {
            if (oldSchema.hasAttribute(name)) {
                if (!schemaMapping.get(name).getType().equals(oldSchema.getAttributeType(name))) {
                    if (ColorThemingStyle.get(layer) != null) {
                        layer.removeStyle(ColorThemingStyle.get(layer));
                        layer.getBasicStyle().setEnabled(true);
                        layer.fireAppearanceChanged();
                    }
                }
            }
        }

        FeatureSchema newSchema = new FeatureSchema();
        for (String name : schemaMapping.keySet()) {
            newSchema.addAttribute(name, schemaMapping.get(name).getType());
            newSchema.setAttributeReadOnly(newSchema.getAttributeIndex(name), schemaMapping.get(name).isReadOnly());
        }

        List originalFeatures = layer.getFeatureCollectionWrapper().getFeatures();
        ArrayList tempFeatures = new ArrayList();

        //Two-phase commit.
        //Phase 1: check that no conversion errors occur. [Jon Aquino]
        for (Iterator i = layer.getFeatureCollectionWrapper().iterator();
             i.hasNext();) {
            Feature feature = (Feature) i.next();
            tempFeatures.add(convert(feature, schemaMapping, newSchema, isForcingInvalidConversionsToNull));
        }

        //Phase 2: commit. [Jon Aquino]
        for (int i = 0; i < originalFeatures.size(); i++) {
            Feature originalFeature = (Feature) originalFeatures.get(i);
            Feature tempFeature = (Feature) tempFeatures.get(i);

            //Modify existing features rather than creating new features, because
            //there may be references to the existing features (e.g. Attribute Viewers).
            //[Jon Aquino]
            originalFeature.setSchema(tempFeature.getSchema());
            originalFeature.setAttributes(tempFeature.getAttributes());
        }

        //Non-undoable. [Jon Aquino]
        layer.getLayerManager().getUndoableEditReceiver().getUndoManager()
                .discardAllEdits();
        layer.setFeatureCollection(new FeatureDataset(originalFeatures,
                newSchema));
        layer.fireLayerChanged(LayerEventType.METADATA_CHANGED);
    }

    private Feature convert(Feature oldFeature, SchemaPanel panel,
        FeatureSchema newSchema) throws ConversionException {
        Feature newFeature = new BasicFeature(newSchema);

        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            if (panel.getModel().get(i).getOriginalIndex() == -1) {
                newFeature.setAttribute(i,
                    (panel.getModel().get(i).getType() == AttributeType.GEOMETRY)
                    ? oldFeature.getGeometry() : null);
            } else {
                newFeature.setAttribute(i, convert(
                        oldFeature.getAttribute(panel.getModel().get(i).getOriginalIndex()),
                        oldFeature.getSchema().getAttributeType(panel.getModel().get(i).getOriginalIndex()),
                        newFeature.getSchema().getAttributeType(i),
                        panel.getModel().get(i).getName(),
                        panel.isForcingInvalidConversionsToNull()));
            }
        }

        return newFeature;
    }

    /**
     * Conversion tool used by macro (no dependence to SchemaPanel).
     * @param oldFeature
     * @param schemaMapping
     * @param newSchema
     * @return
     * @throws ConversionException
     */
    private Feature convert(Feature oldFeature, Map<String,Attribute> schemaMapping, FeatureSchema newSchema,
                            boolean isForcingInvalidConversionsToNull) throws ConversionException {
        Feature newFeature = new BasicFeature(newSchema);

        for (String name : schemaMapping.keySet()) {
            if (schemaMapping.get(name).getOldIndex() == -1) {
                newFeature.setAttribute(name,
                        (newSchema.getAttributeType(name) == AttributeType.GEOMETRY)
                                ? oldFeature.getGeometry() : null);
            } else {
                newFeature.setAttribute(name, convert(
                        oldFeature.getAttribute(schemaMapping.get(name).getOldIndex()),
                        oldFeature.getSchema().getAttributeType(schemaMapping.get(name).getOldIndex()),
                        newFeature.getSchema().getAttributeType(name),
                        name, isForcingInvalidConversionsToNull));
            }
        }

        return newFeature;
    }

    private String limitLength(Object obj) {
        //Limit length of values reported in error messages -- WKT is potentially large.
        //[Jon Aquino]
        return obj == null ? null : limitLength(obj.toString());
    }

    private String limitLength(String s) {
        //Limit length of values reported in error messages -- WKT is potentially large.
        //[Jon Aquino]
        return StringUtil.limitLength(s, 30);
    }

    Pattern TRUE_PATTERN = Pattern.compile("(?i)^(T(rue)?|Y(es)?|V(rai)?|1)$");
    Pattern FALSE_PATTERN = Pattern.compile("(?i)^(F(alse)?|N(o)?|F(aux)?|0)$");

    public Object convert(Object from, AttributeType fromType, AttributeType toType,
            String attributeName, boolean forcingInvalidConversionsToNull)
            throws ConversionException {
        if (fromType == AttributeType.STRING) return convert((String)from, toType, attributeName, forcingInvalidConversionsToNull);
        if (fromType == AttributeType.GEOMETRY) return convert((Geometry)from, toType, attributeName, forcingInvalidConversionsToNull);
        if (fromType == AttributeType.BOOLEAN) return convert((Boolean)from, toType, attributeName, forcingInvalidConversionsToNull);
        if (fromType == AttributeType.INTEGER) return convert((Integer)from, toType, attributeName, forcingInvalidConversionsToNull);
        if (fromType == AttributeType.LONG) return convert((Long)from, toType, attributeName, forcingInvalidConversionsToNull);
        if (fromType == AttributeType.DOUBLE) return convert((Double)from, toType, attributeName, forcingInvalidConversionsToNull);
        if (fromType == AttributeType.DATE) return convert((Date)from, toType, attributeName, forcingInvalidConversionsToNull);
        if (fromType == AttributeType.OBJECT) return convert(from, toType, attributeName, forcingInvalidConversionsToNull);
        else throw new ConversionException("Unknown type: " + fromType);
    }

    private ConversionException conversionException(String type, Object obj, String name) {
        return new ConversionException(I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-" + type) +
                " \"" + limitLength(obj) + "\" (" + name + ")");
    }

    public Object convert(String from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return from;
        } else if (toType == AttributeType.GEOMETRY) {
            try {
                return wktReader.read(from);
            } catch (ParseException e) {
                if (forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
                throw conversionException("geometry", from, attributeName);
            }
        } else if (toType == AttributeType.BOOLEAN) {
            if (FALSE_PATTERN.matcher(from).matches()) return Boolean.FALSE;
            else if (TRUE_PATTERN.matcher(from).matches()) return Boolean.TRUE;
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("boolean", from, attributeName);
        } else if (toType == AttributeType.INTEGER) {
            try {
                return Integer.parseInt(from.replaceAll("^0*([^0])","$1"));
            } catch (NumberFormatException e) {
                if (forcingInvalidConversionsToNull) return null;
                throw conversionException("integer", from, attributeName);
            }
        } else if (toType == AttributeType.LONG) {
            try {
                return Long.parseLong(from.replaceAll("^0*([^0])]", "$1"));
            } catch (NumberFormatException e) {
                if (forcingInvalidConversionsToNull) return null;
                throw conversionException("long", from, attributeName);
            }
        } else if (toType == AttributeType.DOUBLE) {
            try {
                return Double.parseDouble(from);
            } catch (NumberFormatException e) {
                if (forcingInvalidConversionsToNull) return null;
                throw conversionException("double", from, attributeName);
            }
        } else if (toType == AttributeType.DATE) {
            try {
                return dateParser.parse(from, false);
            } catch (java.text.ParseException e) {
                if (forcingInvalidConversionsToNull) return null;
                throw conversionException("date", from, attributeName);
            }
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }

    public Object convert(Geometry from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return from.toText();
        } else if (toType == AttributeType.GEOMETRY) {
            return from;
        } else if (toType == AttributeType.BOOLEAN) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("boolean", from, attributeName);
        } else if (toType == AttributeType.INTEGER) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("integer", from, attributeName);
        } else if (toType == AttributeType.LONG) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("long", from, attributeName);
        } else if (toType == AttributeType.DOUBLE) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("double", from, attributeName);
        } else if (toType == AttributeType.DATE) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("date", from, attributeName);
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }

    public Object convert(Boolean from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return from.toString();
        } else if (toType == AttributeType.GEOMETRY) {
            if (forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            throw conversionException("geometry", from, attributeName);
        } else if (toType == AttributeType.BOOLEAN) {
            return from;
        } else if (toType == AttributeType.INTEGER) {
            return from ? 1 : 0;
        } else if (toType == AttributeType.LONG) {
            return from ? 1L : 0L;
        } else if (toType == AttributeType.DOUBLE) {
            return from ? 1.0 : 0.0;
        } else if (toType == AttributeType.DATE) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("date", from, attributeName);
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }

    public Object convert(Integer from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return from.toString();
        } else if (toType == AttributeType.GEOMETRY) {
            if (forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            throw conversionException("geometry", from, attributeName);
        } else if (toType == AttributeType.BOOLEAN) {
            return from.intValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
        } else if (toType == AttributeType.INTEGER) {
            return from;
        } else if (toType == AttributeType.LONG) {
            return from.longValue();
        } else if (toType == AttributeType.DOUBLE) {
            return from.doubleValue();
        } else if (toType == AttributeType.DATE) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("date", from, attributeName);
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }

    public Object convert(Long from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return from.toString();
        } else if (toType == AttributeType.GEOMETRY) {
            if (forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            throw conversionException("geometry", from, attributeName);
        } else if (toType == AttributeType.BOOLEAN) {
            return from.longValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
        } else if (toType == AttributeType.INTEGER) {
            if (from.longValue() >= Integer.MIN_VALUE && from.longValue() <= Integer.MAX_VALUE) return from.intValue();
            else if (forcingInvalidConversionsToNull) return null;
            throw conversionException("integer", from, attributeName);
        } else if (toType == AttributeType.LONG) {
            return from;
        } else if (toType == AttributeType.DOUBLE) {
            return from.doubleValue();
        } else if (toType == AttributeType.DATE) {
            return new Date(from);
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }

    public Object convert(Double from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return from.toString();
        } else if (toType == AttributeType.GEOMETRY) {
            if (forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            throw conversionException("geometry", from, attributeName);
        } else if (toType == AttributeType.BOOLEAN) {
            return from.doubleValue() == 0.0 ? Boolean.FALSE : Boolean.TRUE;
        } else if (toType == AttributeType.INTEGER) {
            if (from.doubleValue() >= Integer.MIN_VALUE && from.doubleValue() <= Integer.MAX_VALUE) return from.intValue();
            else if (forcingInvalidConversionsToNull) return null;
            throw conversionException("integer", from, attributeName);
        } else if (toType == AttributeType.LONG) {
            if (from.doubleValue() >= Long.MIN_VALUE && from.doubleValue() <= Long.MAX_VALUE) return from.longValue();
            else if (forcingInvalidConversionsToNull) return null;
            throw conversionException("long", from, attributeName);
        } else if (toType == AttributeType.DOUBLE) {
            return from;
        } else if (toType == AttributeType.DATE) {
            if (from.doubleValue() < Long.MAX_VALUE && from.doubleValue() > 0) return new Date(from.longValue());
            else if (forcingInvalidConversionsToNull) return null;
            throw conversionException("date", from, attributeName);
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }

    public Object convert(Date from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return dateFormatter.format(from);
        } else if (toType == AttributeType.GEOMETRY) {
            if (forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            throw conversionException("geometry", from, attributeName);
        } else if (toType == AttributeType.BOOLEAN) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("boolean", from, attributeName);
        } else if (toType == AttributeType.INTEGER) {
            if (forcingInvalidConversionsToNull) return null;
            throw conversionException("integer", from, attributeName);
        } else if (toType == AttributeType.LONG) {
            return from.getTime();
        } else if (toType == AttributeType.DOUBLE) {
            return (double)from.getTime();
        } else if (toType == AttributeType.DATE) {
            return from;
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }

    public Object convert(Object from, AttributeType toType, String attributeName, boolean forcingInvalidConversionsToNull) throws ConversionException {
        if (from == null) {
            if (toType == AttributeType.GEOMETRY && forcingInvalidConversionsToNull) return factory.createPoint((Coordinate)null);
            else if (toType == AttributeType.GEOMETRY) throw conversionException("geometry", from, attributeName);
            else return null;
        } else if (toType == AttributeType.STRING) {
            return from.toString();
        } else if (toType == AttributeType.GEOMETRY) {
            return convert(from.toString(), toType, attributeName, forcingInvalidConversionsToNull);
        } else if (toType == AttributeType.BOOLEAN) {
            return convert(from.toString(), toType, attributeName, forcingInvalidConversionsToNull);
        } else if (toType == AttributeType.INTEGER) {
            return convert(from.toString(), toType, attributeName, forcingInvalidConversionsToNull);
        } else if (toType == AttributeType.LONG) {
            return convert(from.toString(), toType, attributeName, forcingInvalidConversionsToNull);
        } else if (toType == AttributeType.DOUBLE) {
            return convert(from.toString(), toType, attributeName, forcingInvalidConversionsToNull);
        } else if (toType == AttributeType.DATE) {
            return convert(from.toString(), toType, attributeName, forcingInvalidConversionsToNull);
        } else if (toType == AttributeType.OBJECT) {
            return from;
        } else {
            throw new ConversionException("Unknown type: " + toType);
        }
    }


    private void commitEditsInProgress(final SchemaPanel panel) {
        //Skip if nothing is being edited, otherwise may get false positive. [Jon Aquino]
        if (panel.getTable().getEditingRow() != -1) {
            //If user is in the middle of editing a field name, call stopCellEditing
            //so that new field name is committed (if valid) or an error is recorded
            //(if invalid). [Jon Aquino]
            panel.getTable()
                 .getCellEditor(panel.getTable().getEditingRow(),
                panel.getTable().getEditingColumn()).stopCellEditing();
        }
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        // [mmichaud 2014-10-05] had to test if we are in macro mode or in interactive mode
        // maybe a better way vould be that all plugins implement a non interactive method
        // called either by execute method (interactive mode) or directly by the RunMacro
        if (!context.getWorkbenchContext().getBlackboard().get(MacroManager.MACRO_RUNNING, false)) {
            //Can't simply use Blackboard#get(key, default) because default requires that
            //we create a new EditSchemaFrame, and we don't want to do this unless we
            //have to because the EditSchemaFrame constructor modifies the blackboard.
            //Result: Executing this plug-in twice creates two frames, even if we don't close
            //the first. [Jon Aquino]
            if (frame(context) == null) {
                addParameter(P_LAYER_NAME, context.getSelectedLayer(0).getName());
                context.getSelectedLayer(0).getBlackboard().put(KEY,
                        new EditSchemaFrame(context.getWorkbenchFrame(),
                                context.getSelectedLayer(0), editingPlugIn));
            }

            frame(context).surface();

            if (context.getWorkbenchContext().getBlackboard().get(MacroManager.MACRO_STARTED, false)) {
                ((Macro) context.getWorkbenchContext().getBlackboard().get(MacroManager.MACRO)).addProcess(this);
            }
        }
        else {
            // in macro mode, the layer to which the new schema will be applied is choosen in the following order :
            // - if one layer is selected : apply to the selected layer
            // - if several layers are selected : apply to P_LAYER_NAME
            //   or to the first selected layer if P_LAYER_NAME is not selected
            // - if no layer are selected : apply to P_LAYER_NAME or do not apply
            Layer layer;
            Layer[] selectedLayers = context.getLayerNamePanel().getSelectedLayers();
            if (selectedLayers.length == 1) {
                layer = selectedLayers[0];
            } else if (context.getLayerManager().getLayer((String)getParameter(P_LAYER_NAME)) != null) {
                layer = context.getLayerManager().getLayer((String)getParameter(P_LAYER_NAME));
            } else return false;
            applyChanges(layer,
                    (Map<String,Attribute>)getParameter(P_SCHEMA_MAPPING),
                    getBooleanParam(P_FORCE_INVALID_CONVERSIONS_TO_NULL));
        }

        return true;
    }

    private EditSchemaFrame frame(PlugInContext context) {
        return (EditSchemaFrame) context.getSelectedLayer(0).getBlackboard()
                                        .get(KEY);
    }

    public static MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                                     .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(
                1));
    }

    private static class ConversionException extends Exception {
        public ConversionException(String message) {
            super(message);
        }
    }
    
    public static final ImageIcon ICON = IconLoader.icon("Object.gif");

    private class EditSchemaFrame extends JInternalFrame
        implements LayerNamePanelProxy, LayerNamePanel, LayerManagerProxy {
        private LayerManager layerManager;
        private Layer layer;
        private WorkbenchFrame workbenchFrame;

        public EditSchemaFrame(final WorkbenchFrame workbenchFrame,
            final Layer layer, EditingPlugIn editingPlugIn) {
            this.layer = layer;
            this.workbenchFrame = workbenchFrame;
            layer.getBlackboard().put(KEY, this);

            this.layerManager = layer.getLayerManager();
            addInternalFrameListener(new InternalFrameAdapter() {
                    public void internalFrameClosed(InternalFrameEvent e) {
                        layer.getBlackboard().put(KEY, null);
                    }
                });

            final SchemaPanel panel = new SchemaPanel(layer, editingPlugIn,
                    workbenchFrame.getContext());
            setResizable(true);
            setClosable(true);
            setMaximizable(true);
            setIconifiable(true);
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(panel, BorderLayout.CENTER);
            setSize(500, 300);
            updateTitle(layer);
            layer.getLayerManager().addLayerListener(new LayerListener() {
                    public void categoryChanged(CategoryEvent e) {
                    }

                    public void featuresChanged(FeatureEvent e) {
                    }

                    public void layerChanged(LayerEvent e) {
                        updateTitle(layer);
                    }
                });
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            panel.add(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            commitEditsInProgress(panel);
                            applyChanges(layer, panel, workbenchFrame);
                        } catch (Exception x) {
                            workbenchFrame.handleThrowable(x);
                        }
                    }
                });
            addInternalFrameListener(new InternalFrameAdapter() {
                    public void internalFrameClosing(InternalFrameEvent e) {
                        commitEditsInProgress(panel);

                        if (!layer.isEditable() || !panel.isModified()) {
                            dispose();

                            return;
                        }

                        switch (JOptionPane.showConfirmDialog(EditSchemaFrame.this,
                            I18N.get("ui.plugin.ViewSchemaPlugIn.apply-changes-to-schema"), "JUMP",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE)) {
                        case JOptionPane.YES_OPTION:

                            try {
                                applyChanges(layer, panel, workbenchFrame);
                            } catch (Exception x) {
                                workbenchFrame.handleThrowable(x);
                                return;
                            }

                            dispose();

                            return;

                        case JOptionPane.NO_OPTION:
                            dispose();

                            return;

                        case JOptionPane.CANCEL_OPTION:
                            return;

                        default:
                            Assert.shouldNeverReachHere();
                        }
                    }
                });
        }

        private void updateTitle(Layer layer) {
            setTitle((layer.isEditable() ? 
                    I18N.get("ui.plugin.ViewSchemaPlugIn.edit") : 
                    I18N.get("ui.plugin.ViewSchemaPlugIn.view")) 
                    + " "+I18N.get("ui.plugin.ViewSchemaPlugIn.schema")+": " +
                layer.getName());
        }

        public LayerManager getLayerManager() {
            return layerManager;
        }

        public Layer chooseEditableLayer() {
            return TreeLayerNamePanel.chooseEditableLayer(this);
        }

        public void surface() {
            if (!workbenchFrame.hasInternalFrame(this)) {
                workbenchFrame.addInternalFrame(this, false, true);
            }

            workbenchFrame.activateFrame(this);
            moveToFront();
        }

        public LayerNamePanel getLayerNamePanel() {
            return this;
        }

        public Collection getSelectedCategories() {
            return new ArrayList();
        }

        public Layer[] getSelectedLayers() {
            return new Layer[] { layer };
        }

        public Collection selectedNodes(Class c) {
            if (!Layerable.class.isAssignableFrom(c)) {
                return new ArrayList();
            }

            return Arrays.asList(getSelectedLayers());
        }

        public void addListener(LayerNamePanelListener listener) {}
        public void removeListener(LayerNamePanelListener listener) {}
    }

    public static class ToNewSchema {

        public ToNewSchema(SchemaPanel panel) {

        }
    }

    public static class Attribute {

        AttributeType type;
        boolean readOnly;
        boolean primaryKey;
        Operation operation;
        int oldIndex;

        public Attribute(){}

        public void setType(AttributeType type) {this.type = type;}
        public AttributeType getType() {return type;}

        public void setReadOnly(boolean readOnly) {this.readOnly = readOnly;}
        public boolean isReadOnly() {return readOnly;}

        public void setPrimaryKey(boolean primaryKey) {this.primaryKey = primaryKey;}
        public boolean isPrimaryKey() {return primaryKey;}

        public void setOperation(Operation operation) {this.operation = operation;}
        public Operation getOperation() {return operation;}

        public void setOldIndex(int oldIndex) {this.oldIndex = oldIndex;}
        public int getOldIndex() {return oldIndex;}
    }
}
