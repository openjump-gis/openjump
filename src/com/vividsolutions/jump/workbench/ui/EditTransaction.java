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

package com.vividsolutions.jump.workbench.ui;

import java.util.*;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.ui.SelectionManager;

/**
 * Takes care of "rollback" (if any geometries are invalid) and undo,
 * for PlugIns and CursorTools that modify geometries.
 * <p> Also:
 * <UL>
 *      <LI>warns the user if invalid geometries are found</LI>
 *      <LI>invalidates the layer envelope cache</LI>
 *      <LI>invalidates the geometry envelope caches</LI>
 *      <LI>(undoably) removes features from the layer when their geometries are made empty</LI>
 *      <LI>(undoably) adds features to the layer when they start with empty geometries </LI>
 * </UL></p>
 */
public class EditTransaction {

    // [mmichaud 2011-09-17] change List to Set
    // several parts of the code used to check object unicity in the feature
    // collection using "!features.contains(Feature)" which is very inefficient 
    // with a list.
    private Set features;
    // Maps a feature id to its old geometry [mmichaud 2011-09-17 : List->Map]
    private Map<Integer,Geometry> originalGeometries;
    // Maps a feature id to its new geometry [mmichaud 2011-09-17 : List->Map]
    private Map<Integer,Geometry> proposedGeometries;
    private Layer layer;
    private String name;
    private boolean rollingBackInvalidEdits;

    public static final String ROLLING_BACK_INVALID_EDITS_KEY =
        EditTransaction.class.getName() + " - ROLLING_BACK_INVALID_EDITS";
        
    // Empty Geometry is used for not yet created or deleted features
    public static final Geometry EMPTY_GEOMETRY = new GeometryFactory().createGeometryCollection(new Geometry[0]);

    private LayerViewPanelContext layerViewPanelContext;

    /**
     * Creates a new EditTransaction modifying features.
     * @param features features to be modified
     * @param name display name for undo. Use PlugIn#getName or CursorTool#getName.
     * @param layer the layer to which the features belong
     * @param rollingBackInvalidEdits is true if we want to roll back transaction
     * in case of invalid edits
     * @param allowAddingAndRemovingFeatures whether to treat empty
     * geometries as indications to add/remove features or as in fact empty geometries
     * @param layerViewPanel the view where editing operations take place.
     */
    public EditTransaction(
        Collection features,
        String name,
        Layer layer,
        boolean rollingBackInvalidEdits,
        boolean allowAddingAndRemovingFeatures,
        LayerViewPanel layerViewPanel) {
        this(
            features,
            name,
            layer,
            rollingBackInvalidEdits,
            allowAddingAndRemovingFeatures,
            layerViewPanel.getContext());
    }

    /**
     * If you want to delete a feature, you can either (1) include the feature in
     * the features parameter, set allowAddingAndRemovingFeatures to true,
     * then call #setGeometry(feature, empty geometry); or (2) not include the feature in
     * the features parameter, instead using #deleteFeature
     * @param features features to be modified
     * @param name display name for undo. Use PlugIn#getName or CursorTool#getName.
     * @param layer the layer to which the features belong
     * @param rollingBackInvalidEdits is true if we want to roll back transaction
     * in case of invalid edits
     * @param allowAddingAndRemovingFeatures whether to treat empty
     * geometries as indications to add/remove features or as in fact empty geometries
     * @param layerViewPanelContext the view where editing operations take place.
     */
    public EditTransaction(
        Collection features,
        String name,
        Layer layer,
        boolean rollingBackInvalidEdits,
        boolean allowAddingAndRemovingFeatures,
        LayerViewPanelContext layerViewPanelContext) {
        this.layerViewPanelContext = layerViewPanelContext;
        this.layer = layer;
        this.rollingBackInvalidEdits = rollingBackInvalidEdits;
        this.allowAddingAndRemovingFeatures = allowAddingAndRemovingFeatures;
        this.name = name;
        
        // [mmichaud 2011-09-17]
        // Uses a LinkedHashSet instead of HashSet, because it makes
        // implementation of "getGeometry(integer)" and "getFeature(integer)"
        // (which are kept for compatibility reasons) more efficient.
        this.features = new LinkedHashSet(features);
        //Clone the Geometries, and don't commit it until we're sure that no 
        //errors occurred. [Jon Aquino]
        originalGeometries = geometryClones(features);
        proposedGeometries = geometryClones(features);
    }

    /**
     * A utility class creating a transaction able to rollback selected
     * features after they have been edited according to the SelectionEditor 
     * definition.
     */
    public static EditTransaction createTransactionOnSelection(
        SelectionEditor editor,
        SelectionManagerProxy selectionManagerProxy,
        LayerViewPanelContext layerViewPanelContext,
        String name,
        Layer layer,
        boolean rollingBackInvalidEdits,
        boolean allowAddingAndRemovingFeatures) {
        Map featureToNewGeometryMap = featureToNewGeometryMap(editor, selectionManagerProxy, layer);
        EditTransaction transaction = new EditTransaction(
            featureToNewGeometryMap.keySet(),
            name,
            layer,
            rollingBackInvalidEdits,
            allowAddingAndRemovingFeatures,
            layerViewPanelContext);
        transaction.setGeometries(featureToNewGeometryMap);
        return transaction;
    }

    /**
     * Utility method to create a map between features and there modified 
     * geometry.
     */
    public static Map featureToNewGeometryMap(
        SelectionEditor editor,
        SelectionManagerProxy selectionManagerProxy,
        Layer layer) {
        Map featureToNewGeometryMap = new HashMap();
        SelectionManager selectionManager = selectionManagerProxy.getSelectionManager();
        for (Iterator i = selectionManager.getFeaturesWithSelectedItems(layer)
                                                   .iterator(); i.hasNext(); ) {
            Feature feature = (Feature) i.next();
            Geometry newGeometry = (Geometry) feature.getGeometry().clone();
            ArrayList selectedItems = new ArrayList();
            for (Iterator j = selectionManager.getSelections().iterator(); j.hasNext(); ) {
                AbstractSelection selection = (AbstractSelection) j.next();
                //Use #getSelectedItemIndices rather than #getSelectedItems, because
                //we want the selected items from newGeometry, not the original
                //Geometry (so that editor can freely modify them). [Jon Aquino]
                selectedItems.addAll(
                    selection.items(newGeometry, selection.getSelectedItemIndices(layer, feature))
                );
            }
            newGeometry = editor.edit(newGeometry, selectedItems);
            featureToNewGeometryMap.put(feature, newGeometry);
        }
        return featureToNewGeometryMap;
    }

    public static interface SelectionEditor {
        /**
         * selectedItems may have the whole geometry, parts (collection elements),
         * or linestrings, or a mix of all three. But there will be no duplicate data
         * (that is, you can't select both the whole and one of its parts -- only the
         * whole geometry will be selected; similarly, you can't select a part and
         * one of its linestrings -- only the part will be selected).
         * @param geometryWithSelectedItems a clone of the geometry containing the selected items.
         * Because geometryWithSelectedItems is a clone, feel free to modify it, as no other
         * parties reference it. Then return it (or return something totally different).
         * @param selectedItems clones of the selected items (each of which have class Geometry).
         * selectedItems' elements are "live"; that is, they are objects taken from geometryWithSelectedItems.
         * So, for example, modifying selectedItem's coordinates will modify geometryWithSelectedItems'
         * coordinates.
         * @return a new Geometry for the Feature (typically geometryWithSelectedItems, but can
         * be a completely different Geometry), or an empty geometry to (undoably) remove the Feature from the Layer
         */
        public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems);
    }

    /**
     * Returns the geometry of element i of the transaction.
     * This method is deprecated, now that transaction features are held in a 
     * This method is deprecated. Use getGeometry(Feature) instead.
     * @deprecated
     */
    public Geometry getGeometry(int i) {
        Feature f = (Feature)features.toArray()[i];
        return (Geometry)proposedGeometries.get(f.getID());
    }

    public Geometry getGeometry(Feature feature) {
        return proposedGeometries.get(feature.getID());
    }

    public void setGeometry(Feature feature, Geometry geometry) {
        proposedGeometries.put(feature.getID(), geometry);
    }

    public void setGeometries(Map featureToGeometryMap) {
        for (Iterator i = featureToGeometryMap.keySet().iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            proposedGeometries.put(feature.getID(),
                editor.removeRepeatedPoints((Geometry)featureToGeometryMap.get(feature)));
        }
    }

   /**
    * This method is deprecated. Use getGeometry(Feature) instead.
    * @deprecated
    */
    public void setGeometry(int i, Geometry geometry) {
        Feature f = (Feature)features.toArray()[i];
        proposedGeometries.put(f.getID(), editor.removeRepeatedPoints(geometry));
    }

    private GeometryEditor editor = new GeometryEditor();

    private boolean allowAddingAndRemovingFeatures;

    public static interface SuccessAction {
        public void run();
    }

    public boolean commit() {
        return commit(Collections.singleton(this));
    }

    public static boolean commit(Collection editTransactions) {
        return commit(editTransactions, new SuccessAction() {
            public void run() {}
        });
    }

    /**
     * Commits several EditTransactions if their proposed geometries are all valid.
     * Useful for committing changes to several layers because an EditTransaction
     * handles one layer only. Gets the undo name and the UndoManager
     * from the first EditTransaction.
     * @param successAction run after the first execution (i.e. not after redos) if all
     * proposed geometries are valid (or rollingBackInvalidEdits is false)
     */
    public static boolean commit(Collection editTransactions, SuccessAction successAction) {
        if (editTransactions.isEmpty()) {
            return true;
        }
        final ArrayList commands = new ArrayList();
        for (Iterator i = editTransactions.iterator(); i.hasNext();) {
            EditTransaction editTransaction = (EditTransaction) i.next();
            editTransaction.clearEnvelopeCaches();
            if (!editTransaction.proposedGeometriesValid()) {
                if (editTransaction.rollingBackInvalidEdits) {
                    editTransaction.layerViewPanelContext.warnUser(
                        I18N.get("ui.EditTransaction.the-geometry-is-invalid-cancelled"));
                    return false;
                } else {
                    editTransaction.layerViewPanelContext.warnUser(I18N.get("ui.EditTransaction.the-new-geometry-is-invalid"));
                }
            }
            commands.add(editTransaction.createCommand());
        }
        successAction.run();
        UndoableCommand command =
            new UndoableCommand(((UndoableCommand) commands.iterator().next()).getName()) {
            public void execute() {
                for (Iterator i = commands.iterator(); i.hasNext();) {
                    UndoableCommand subCommand = (UndoableCommand) i.next();
                    if (!subCommand.isCanceled()) subCommand.execute();
                }
            }
            public void unexecute() {
                for (Iterator i = commands.iterator(); i.hasNext();) {
                    UndoableCommand subCommand = (UndoableCommand) i.next();
                    if (!subCommand.isCanceled()) subCommand.unexecute();
                }
            }
        };
        command.execute();
        ((EditTransaction) editTransactions.iterator().next())
            .layer
            .getLayerManager()
            .getUndoableEditReceiver()
            .receive(command.toUndoableEdit());
        return true;
    }

    /**
     * @param successAction will be run if the geometries are valid (or
     * OptionsPlugIn#isRollingBackInvalidEdits returns false), before the layer-change
     * events are fired. Useful for animations and other visual indicators which would
     * be slowed down if the layer-change events were fired first.
     * @return true if all the proposed geometries are valid
     */
    public boolean commit(SuccessAction successAction) {
        return commit(Collections.singleton(this), successAction);
    }

    public void clearEnvelopeCaches() {
        for (Iterator i = proposedGeometries.values().iterator(); i.hasNext() ; ) {
            Geometry proposedGeometry = (Geometry)i.next();
            //Because the proposedGeometry is a clone, its cached envelope is old.
            //Invalidate the envelope. [Jon Aquino]
            proposedGeometry.geometryChanged();
        }
    }

    public boolean proposedGeometriesValid() {
        for (Iterator i = proposedGeometries.values().iterator(); i.hasNext() ; ) {
            Geometry proposedGeometry = (Geometry) i.next();
            if (!proposedGeometry.isValid()) {
                return false; 
            }
        }
        return true;
    }

    protected UndoableCommand createCommand() {
        UndoableCommand command = new UndoableCommand(name, layer) {
            public void dispose() {
                super.dispose();
                proposedGeometries.clear();
                originalGeometries.clear();
            }
            public void execute() {
                changeGeometries(proposedGeometries, originalGeometries, layer);
            }

            public void unexecute() {
                changeGeometries(originalGeometries, proposedGeometries, layer);
            }
        };
        return command;
    }

    private Map<Integer,Geometry> geometryClones(Collection features) {
        Map<Integer,Geometry> geometryClones = new HashMap<Integer,Geometry>();
        for (Iterator i = features.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            // [mmichaud 2011-09-17] Tried to get rid of clone, but some
            // transactions need that (ex. SnapVerticesOp)
            geometryClones.put(feature.getID(), (Geometry)feature.getGeometry().clone());
            //geometryClones.put(feature.getID(), feature.getGeometry());
        }
        return geometryClones;
    }

    /**
     * Switch features between old geometries and new Geometries
     * @param newGeometries an empty geometry indicates that we should remove 
     * the feature to the layer
     * @param oldGeometries an empty geometry indicates that we should add
     * the feature to the layer
     * @param layer the layer where edit operations take place
     */
    private void changeGeometries(Map<Integer,Geometry> newGeometries, 
                                  Map<Integer,Geometry> oldGeometries, 
                                  Layer layer) {
        ArrayList modifiedFeatures = new ArrayList();
        ArrayList modifiedFeaturesOldClones = new ArrayList();
        ArrayList featuresToAdd = new ArrayList();
        ArrayList featuresToRemove = new ArrayList();
        for (Iterator it = features.iterator() ; it.hasNext() ; ) {
            Feature feature = (Feature) it.next();
            Geometry oldGeometry = oldGeometries.get(feature.getID());
            Geometry newGeometry = newGeometries.get(feature.getID());
            if (allowAddingAndRemovingFeatures
                && oldGeometry.isEmpty()
                && !newGeometry.isEmpty()) {
                featuresToAdd.add(feature);
            } else if (
                allowAddingAndRemovingFeatures
                    && newGeometry.isEmpty()
                    // the second condition was preventing empty geometries to be removed
                    // now, it should remove a feature which new geometry is empty,
                    // but it does not. Why ? [mmichaud 2010-10-16]
                    /*&& !oldGeometry.isEmpty()*/) { 
                featuresToRemove.add(feature);
            } else {
                modifiedFeatures.add(feature);
                modifiedFeaturesOldClones.add(feature.clone());
                feature.setGeometry(newGeometry);
            }
        }

        Layer.tryToInvalidateEnvelope(layer);
        //Important to fire the feature-removed event first (before the feature-added
        //and feature-modified events) so that any selections that need to be cleared
        //get cleared. [Jon Aquino]
        if (!featuresToRemove.isEmpty()) {
            layer.getFeatureCollectionWrapper().removeAll(featuresToRemove);
        }
        if (!featuresToAdd.isEmpty()) {
            layer.getFeatureCollectionWrapper().addAll(featuresToAdd);
        }
        if (!modifiedFeatures.isEmpty()) {
            layer.getLayerManager().fireGeometryModified(
                modifiedFeatures,
                layer,
                modifiedFeaturesOldClones);
        }
    }

    public int size() {
        return features.size();
    }
    
    /**
     * Returns the feature of element i of the transaction.
     * This method is deprecated, use getFeatures() to iterate over the whole
     * set of features.
     * @deprecated
     */
    public Feature getFeature(int i) {
        return (Feature)features.toArray()[i];
    }
    
    /**
     * Returns the features modified by this transaction [mmichaud 2011-09-17]
     */
    public Collection<Feature> getFeatures() {
        return java.util.Collections.unmodifiableSet(features);
    }
    
    public void createFeature(Feature feature) {
        Assert.isTrue(allowAddingAndRemovingFeatures);
        //Assert.isTrue(!features.contains(feature));
        features.add(feature);
        //originalGeometries.put(feature.getID(), feature.getGeometry().getFactory().createGeometryCollection(new Geometry[0]));
        originalGeometries.put(feature.getID(), EMPTY_GEOMETRY);
        // [mmichaud 2011-09-18] Do not clone in the case of feature deletion :
        // It can save time and memory for large transactions like explode features
        // Hopefully, it is never required
        //proposedGeometries.put(feature.getID(), (Geometry)feature.getGeometry().clone());
        proposedGeometries.put(feature.getID(), feature.getGeometry());
    }
    /**
     * @param feature must not have been passed into the constructor
     */
    public void deleteFeature(Feature feature) {
        Assert.isTrue(allowAddingAndRemovingFeatures);
        //Assert.isTrue(!features.contains(feature));
        features.add(feature);
        // [mmichaud 2011-09-18] Do not clone in the case of feature deletion :
        // It can save time and memory for large transactions like explode features
        // Hopefully, it is never required
        //originalGeometries.put(feature.getID(), (Geometry)feature.getGeometry().clone());
        originalGeometries.put(feature.getID(), feature.getGeometry());
        //proposedGeometries.put(feature.getID(), feature.getGeometry().getFactory().createGeometryCollection(new Geometry[0]));
        proposedGeometries.put(feature.getID(), EMPTY_GEOMETRY);
    }
    
    public Layer getLayer() {
        return layer;
    }
    
    public static int emptyGeometryCount(Collection transactions) {
        int count = 0;
        for (Iterator i = transactions.iterator(); i.hasNext(); ) {
            EditTransaction transaction = (EditTransaction) i.next();
            count += transaction.getEmptyGeometryCount();
        }
        return count;
    }

    private int getEmptyGeometryCount() {
        int count = 0;
        for (Iterator it = features.iterator() ; it.hasNext() ; ) {
            Geometry geometry = ((Feature)it.next()).getGeometry();
            if (geometry.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
