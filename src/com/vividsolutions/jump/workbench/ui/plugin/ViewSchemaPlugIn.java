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
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
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
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelListener;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.SchemaPanel;
import com.vividsolutions.jump.workbench.ui.TreeLayerNamePanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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

    public ViewSchemaPlugIn(EditingPlugIn editingPlugIn) {
        this.editingPlugIn = editingPlugIn;
    }

    public String getName() {
        return I18N.get("ui.plugin.ViewSchemaPlugIn.view-edit-schema");
    }

    private void applyChanges(final Layer layer, final SchemaPanel panel)
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

        FeatureSchema newSchema = new FeatureSchema();
        //-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
        FeatureSchema oldSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        String attributeName = null;
        //-- end

        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
        	//-- [sstein 10. Oct 2006] bugfix for colortheming by Ole
        	attributeName = panel.getModel().get(i).getName();
        	newSchema.addAttribute(attributeName, panel.getModel().get(i).getType());
        	if ( oldSchema.hasAttribute(attributeName) &&
        		!newSchema.getAttributeType(attributeName).equals(oldSchema.getAttributeType(attributeName))
        		){
        		if (ColorThemingStyle.get(layer) != null){
        		layer.removeStyle(ColorThemingStyle.get(layer));
        		layer.getBasicStyle().setEnabled(true);
        		layer.fireAppearanceChanged();
        		}
        	}
        	//-- END: added/modyfied by Ole
        }

        List originalFeatures = layer.getFeatureCollectionWrapper().getFeatures();
        ArrayList tempFeatures = new ArrayList();

        //Two-phase commit. Phase 1: check that no conversion errors occur. [Jon Aquino]
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
        // [mmichaud 2009-05-16] update originalIndexes after a modification
        for (int i = 0; i < panel.getModel().getRowCount(); i++) {
            panel.getModel().get(i).setOriginalIndex(i);
        }
        // -end
        panel.markAsUnmodified();
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
                newFeature.setAttribute(i,
                    convert(oldFeature.getAttribute(panel.getModel().get(i)
                                                         .getOriginalIndex()),
                        oldFeature.getSchema().getAttributeType(panel.getModel()
                                                                     .get(i)
                                                                     .getOriginalIndex()),
                        newFeature.getSchema().getAttributeType(i),
                        panel.getModel().get(i).getName(),
                        panel.isForcingInvalidConversionsToNull()));
            }
        }

        return newFeature;
    }

    private String limitLength(String s) {
        //Limit length of values reported in error messages -- WKT is potentially large.
        //[Jon Aquino]
        return StringUtil.limitLength(s, 30);
    }

    private Object convert(Object oldValue, AttributeType oldType,
        AttributeType newType, String name,
        boolean forcingInvalidConversionsToNull) throws ConversionException {
        try {
            if (oldValue == null) {
                return (newType == AttributeType.GEOMETRY)
                ? factory.createPoint((Coordinate)null) : null;
            }

            if (oldType == AttributeType.STRING) {
                String oldString = (String) oldValue;

                if (newType == AttributeType.STRING) {
                    return oldString;
                }

                if (newType == AttributeType.INTEGER) {
                    try {
                        return new Integer(oldString);
                    } catch (NumberFormatException e) {
                        throw new ConversionException(
                        	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-integer")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }

                if (newType == AttributeType.DOUBLE) {
                    try {
                        return new Double(oldString);
                    } catch (NumberFormatException e) {
                        throw new ConversionException(
                        	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-double")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }

                if (newType == AttributeType.GEOMETRY) {
                    try {
                        return wktReader.read(oldString);
                    } catch (ParseException e) {
                        throw new ConversionException(
                        	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }

                if (newType == AttributeType.DATE) {
                    try {
                        return dateParser.parse(oldString, false);
                    } catch (java.text.ParseException e) {
                        throw new ConversionException(
                        	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }
            }

            if (oldType == AttributeType.INTEGER) {
                int oldInt = ((Integer) oldValue).intValue();

                if (newType == AttributeType.STRING) {
                    return "" + oldInt;
                }

                if (newType == AttributeType.INTEGER) {
                    return oldValue;
                }

                if (newType == AttributeType.DOUBLE) {
                    return new Double(oldInt);
                }

                if (newType == AttributeType.GEOMETRY) {
                    throw new ConversionException(
                    	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DATE) {
                    try {
                        return dateParser.parse("" + oldInt, false);
                    } catch (java.text.ParseException e) {
                        throw new ConversionException(
                        	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                            limitLength(oldValue.toString()) + "\" (" + name +
                            ")");
                    }
                }
            }

            if (oldType == AttributeType.DOUBLE) {
                double oldDouble = ((Double) oldValue).doubleValue();

                if (newType == AttributeType.STRING) {
                    return "" + oldDouble;
                }

                if (newType == AttributeType.INTEGER) {
                    return new Integer((int) oldDouble);
                }

                if (newType == AttributeType.DOUBLE) {
                    return oldValue;
                }

                if (newType == AttributeType.GEOMETRY) {
                    throw new ConversionException(
                    	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DATE) {
                    throw new ConversionException(I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }
            }

            if (oldType == AttributeType.GEOMETRY) {
                Geometry oldGeometry = (Geometry) oldValue;

                if (newType == AttributeType.STRING) {
                    return oldGeometry.toString();
                }

                if (newType == AttributeType.INTEGER) {
                    throw new ConversionException(
                    		I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-integer")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DOUBLE) {
                    throw new ConversionException(
                    	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-double")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.GEOMETRY) {
                    return oldGeometry;
                }

                if (newType == AttributeType.DATE) {
                    throw new ConversionException(I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-date")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }
            }

            if (oldType == AttributeType.DATE) {
                Date oldDate = (Date) oldValue;

                if (newType == AttributeType.STRING) {
                    return dateFormatter.format(oldDate);
                }

                if (newType == AttributeType.INTEGER) {
                    return new Integer((int) oldDate.getTime());
                }

                if (newType == AttributeType.DOUBLE) {
                    return new Double(oldDate.getTime());
                }

                if (newType == AttributeType.GEOMETRY) {
                    throw new ConversionException(
                    	I18N.get("ui.plugin.ViewSchemaPlugIn.cannot-convert-to-geometry")+" \"" +
                        limitLength(oldValue.toString()) + "\" (" + name + ")");
                }

                if (newType == AttributeType.DATE) {
                    return oldValue;
                }
            }

            Assert.shouldNeverReachHere(newType.toString());

            return null;
        } catch (ConversionException e) {
            if (forcingInvalidConversionsToNull) {
                return (newType == AttributeType.GEOMETRY)
                ? factory.createPoint((Coordinate)null) : null;
            }

            throw e;
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

        //Can't simply use Blackboard#get(key, default) because default requires that
        //we create a new EditSchemaFrame, and we don't want to do this unless we
        //have to because the EditSchemaFrame constructor modifies the blackboard. 
        //Result: Executing this plug-in twice creates two frames, even if we don't close
        //the first. [Jon Aquino]
        if (frame(context) == null) {
            context.getSelectedLayer(0).getBlackboard().put(KEY,
                new EditSchemaFrame(context.getWorkbenchFrame(),
                    context.getSelectedLayer(0), editingPlugIn));
        }

        frame(context).surface();

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
                            applyChanges(layer, panel);
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
                                applyChanges(layer, panel);
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
}
