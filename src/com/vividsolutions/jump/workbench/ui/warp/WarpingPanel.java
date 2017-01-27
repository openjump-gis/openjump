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

package com.vividsolutions.jump.workbench.ui.warp;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.warp.CoordinateTransform;
import com.vividsolutions.jump.warp.DummyTransform;
import com.vividsolutions.jump.warp.BilinearInterpolatedTransform;
import com.vividsolutions.jump.warp.Triangulator;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelListener;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com
    .vividsolutions
    .jump
    .workbench
    .ui
    .plugin
    .CopySelectedLayersToWarpingVectorsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.generate.ShowTriangulationPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;

public class WarpingPanel extends JPanel {

    //This class is huge -- could do with some refactoring! [Jon Aquino]

    public final static String MODIFIED_OUTSIDE_WARP_KEY =
        WarpingPanel.class.getName() + " - MODIFIED_OUTSIDE_WARP";

    /** Will be an empty Collection if MODIFIED_OUTSIDE_WARP_KEY returns true */
    public final static String RECONSTRUCTION_VECTORS_KEY =
        WarpingPanel.class.getName() + " - RECONSTRUCTION VECTORS";

    private DummyTaskMonitor dummyMonitor = new DummyTaskMonitor();

    private Triangulator triangulator = new Triangulator();

    private boolean warping = false;

    private void addModificationListener(final Layer outputLayer) {
        outputLayer.getLayerManager().addLayerListener(new LayerListener() {
            public void categoryChanged(CategoryEvent e) {}
            public void layerChanged(LayerEvent e) {
                //Appearance and metadata changes don't modify the layer from a
                //warping point of view because this information stays on the layer.
                //Unlike feature changes! [Jon Aquino]
            }
            public void featuresChanged(FeatureEvent e) {
                if (e.getLayer() != outputLayer) {
                    return;
                }
                if (warping) {
                    return;
                }
                outputLayer.getBlackboard().put(MODIFIED_OUTSIDE_WARP_KEY, true);
                outputLayer.getBlackboard().put(
                    RECONSTRUCTION_VECTORS_KEY,
                    new ArrayList());
            }
        });
    }

    public UndoableCommand addWarping(final UndoableCommand wrappeeCommand) {
        return new UndoableCommand(wrappeeCommand.getName()) {
            //Cache warping because user may change #isWarpingIncrementally. [Jon Aquino]
            //Must cache warping lazily because #warpConditionsMet requires that
            //drawCommand execute first. [Jon Aquino]
            private Boolean warping = null;
            //Must create warpCommand lazily because it requires that
            //#warping return true. [Jon Aquino]
            UndoableCommand warpCommand = null;
            private boolean warping() {
                if (warping == null) {
                    warping =
                        new Boolean(isWarpingIncrementally() && warpConditionsMet());
                    if (warping.booleanValue()) {
                        warpCommand = createWarpCommand();
                    }
                }
                return warping.booleanValue();
            }
            public void execute() {
                wrappeeCommand.execute();
                if (warping()) {
                    warpCommand.execute();
                }
            }
            public void unexecute() {
                if (warping()) {
                    warpCommand.unexecute();
                }
                wrappeeCommand.unexecute();
            }
        };
    }

    void clearOutputButton_actionPerformed(ActionEvent e) {
        toolbox.getContext().getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .reportNothingToUndoYet();
            final Layer sourceLayer = currentSourceLayer();
            final Layer outputLayer = currentOutputLayer();
            final boolean outputLayerExistedOriginally = currentOutputLayer() != null;
            //Output layer's reconstruction vectors will not necessarily be the same as vectors
            //(i.e. if user manually modifies the vectors). [Jon Aquino]
            final ArrayList reconstructionVectors = new ArrayList();
            if (outputLayerExistedOriginally) {
                if (outputLayer.getBlackboard().getBoolean(MODIFIED_OUTSIDE_WARP_KEY)) {
                    toolbox
                        .getContext()
                        .getLayerManager()
                        .getUndoableEditReceiver()
                        .reportIrreversibleChange();
                } else {
                    reconstructionVectors.addAll(
                        (Collection) outputLayer.getBlackboard().get(
                            RECONSTRUCTION_VECTORS_KEY));
                }
            }
            final boolean willShowSourceLayer =
                isAutoHidingLayers() && sourceLayer != null && !sourceLayer.isVisible();
            UndoableCommand command =
                Layer
                    .addUndo(
                        warpingVectorLayerFinder().getLayerName(),
                        toolbox.getContext(),
                        Layer
                            .addUndo(
                                incrementalWarpingVectorLayerFinder().getLayerName(),
                                toolbox.getContext(),
                                ShowTriangulationPlugIn
                                    .addUndo(new UndoableCommand(
                                        clearOutputButton
                                        .getText()) {
                public void execute() {
                    if (warpingVectorLayerFinder().getLayer() != null) {
                        toolbox.getContext().getLayerManager().remove(
                            warpingVectorLayerFinder().getLayer());
                    }
                    if (incrementalWarpingVectorLayerFinder().getLayer() != null) {
                        toolbox.getContext().getLayerManager().remove(
                            incrementalWarpingVectorLayerFinder().getLayer());
                    }
                    if (outputLayerExistedOriginally) {
                        //Can't just remove outputLayer because in the undo a new layer
                        //will be generated by #warp. [Jon Aquino]
                        toolbox.getContext().getLayerManager().remove(
                            toolbox.getContext().getLayerManager().getLayer(
                                outputLayer.getName()));
                    }
                    if (willShowSourceLayer) {
                        sourceLayer.setVisible(true);
                    }
                    if (toolbox
                        .getContext()
                        .getLayerManager()
                        .getLayer(ShowTriangulationPlugIn.SOURCE_LAYER_NAME)
                        != null) {
                        toolbox.getContext().getLayerManager().remove(
                            toolbox.getContext().getLayerManager().getLayer(
                                ShowTriangulationPlugIn.SOURCE_LAYER_NAME));
                    }
                    if (toolbox
                        .getContext()
                        .getLayerManager()
                        .getLayer(ShowTriangulationPlugIn.DESTINATION_LAYER_NAME)
                        != null) {
                        toolbox.getContext().getLayerManager().remove(
                            toolbox.getContext().getLayerManager().getLayer(
                                ShowTriangulationPlugIn.DESTINATION_LAYER_NAME));
                    }
                }
                public void unexecute() {
                    //Triangulation layer undo is handled by ShowTriangulationPlugIn#addUndo. [Jon Aquino]
                    try {
                        if (willShowSourceLayer) {
                            sourceLayer.setVisible(false);
                        }
                        if (outputLayerExistedOriginally) {
                            warp(sourceLayer, reconstructionVectors, false);
                        }
                    } catch (Throwable t) {
                        toolbox.getContext().getErrorHandler().handleThrowable(t);
                        toolbox
                            .getContext()
                            .getLayerManager()
                            .getUndoableEditReceiver()
                            .reportIrreversibleChange();
                    }
                }
            }, toolbox.getContext())));
            command.execute();
            toolbox.getContext().getLayerManager().getUndoableEditReceiver().receive(
                command.toUndoableEdit());
        } finally {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .stopReceiving();
        }
    }

    private void clearWarpingFlag() {
        //Give pending Swing events a chance to execute first
        //i.e. don't end the window prematurely. [Jon Aquino]
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                warping = false;
            }
        });
    }

    private Collection clone(Collection features) {
        ArrayList clone = new ArrayList();
        for (Iterator i = features.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            clone.add(feature.clone());
        }
        return clone;
    }

    private Collection collapseToTip(Collection vectors) {
        ArrayList collapsedVectors = new ArrayList();
        for (Iterator i = vectors.iterator(); i.hasNext();) {
            Feature vector = (Feature) i.next();
            Feature collapsedVector = (Feature) vector.clone();
            tail(collapsedVector).setCoordinate(tip(collapsedVector));
            collapsedVector.getGeometry().geometryChanged();
            collapsedVectors.add(collapsedVector);
        }
        return collapsedVectors;
    }

    void copyLayerButton_actionPerformed(ActionEvent e) {
        toolbox.getContext().getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            new CopySelectedLayersToWarpingVectorsPlugIn().execute(
                toolbox.getContext().createPlugInContext());
        } catch (Throwable t) {
            toolbox.getContext().getErrorHandler().handleThrowable(t);
        } finally {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .stopReceiving();
        }
    }

    public UndoableCommand generateWarpingVectorsCommand() {
        Collection reconstructionVectors =
            currentOutputLayer() == null
                || currentOutputLayer().getBlackboard().getBoolean(
                    MODIFIED_OUTSIDE_WARP_KEY)
                    ? new ArrayList()
                    : (Collection) currentOutputLayer().getBlackboard().get(
                        RECONSTRUCTION_VECTORS_KEY);
        final Collection newWarpingVectors =
            toWarpingVectors(
                incrementalWarpingVectorLayerFinder()
                    .getLayer()
                    .getFeatureCollectionWrapper()
                    .getFeatures(),
                reconstructionVectors,
                currentSourceLayer());
        return Layer
            .addUndo(
                warpingVectorLayerFinder().getLayerName(),
                toolbox.getContext(),
                new UndoableCommand(I18N.get("ui.warp.WarpingPanel.generate-warping-vectors-from-incremental-warping-vectors")) {
            public void execute() {
                try {
                    if (warpingVectorLayerFinder().getLayer() == null) {
                        warpingVectorLayerFinder().createLayer();
                    } else {
                        warpingVectorLayerFinder()
                            .getLayer()
                            .getFeatureCollectionWrapper()
                            .clear();
                    }
                    warpingVectorLayerFinder()
                        .getLayer()
                        .getFeatureCollectionWrapper()
                        .addAll(
                        newWarpingVectors);
                } catch (Throwable t) {
                    toolbox.getContext().getErrorHandler().handleThrowable(t);
                    toolbox
                        .getContext()
                        .getLayerManager()
                        .getUndoableEditReceiver()
                        .reportIrreversibleChange();
                }
            }
            public void unexecute() {}
        });
    }

    private void hideTriangulation() {
        if (!(toolbox.getContext().getWorkbench().getFrame().getActiveInternalFrame()
            instanceof LayerViewPanelProxy)) {
            return;
        }
        toolbox.getContext().getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            UndoableCommand command =
                ShowTriangulationPlugIn
                    .addUndo(new UndoableCommand(I18N.get("ui.warp.WarpingPanel.hide-triangulation")) {
                public void execute() {
                    if (toolbox
                        .getContext()
                        .getLayerManager()
                        .getLayer(ShowTriangulationPlugIn.SOURCE_LAYER_NAME)
                        != null) {
                        toolbox.getContext().getLayerManager().remove(
                            toolbox.getContext().getLayerManager().getLayer(
                                ShowTriangulationPlugIn.SOURCE_LAYER_NAME));
                    }
                    if (toolbox
                        .getContext()
                        .getLayerManager()
                        .getLayer(ShowTriangulationPlugIn.DESTINATION_LAYER_NAME)
                        != null) {
                        toolbox.getContext().getLayerManager().remove(
                            toolbox.getContext().getLayerManager().getLayer(
                                ShowTriangulationPlugIn.DESTINATION_LAYER_NAME));
                    }
                }
                public void unexecute() {
                    //Handled by #addUndo
                }
            }, toolbox.getContext());
            command.execute();
            toolbox.getContext().getLayerManager().getUndoableEditReceiver().receive(
                command.toUndoableEdit());
        } catch (Throwable t) {
            toolbox.getContext().getErrorHandler().handleThrowable(t);
        } finally {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .stopReceiving();
        }
    }

    public boolean isAutoHidingLayers() {
        return autoHideCheckBox.isSelected();
    }

    private boolean layerViewPanelProxyActive() {
        return toolbox.getContext().getWorkbench().getFrame().getActiveInternalFrame()
            instanceof LayerViewPanelProxy;
    }

    private Layer outputLayer(String sourceLayerName) {
        Layer outputLayer =
            toolbox.getContext().getLayerManager().getLayer(
                outputLayerName(sourceLayerName));
        if (outputLayer == null) {
            return null;
        }
        if (outputLayer.getBlackboard().get(MODIFIED_OUTSIDE_WARP_KEY) == null) {
            //Handles case in which the user has created a layer named "Warp Output". [Jon Aquino]
            outputLayer.getBlackboard().put(MODIFIED_OUTSIDE_WARP_KEY, true);
            outputLayer.getBlackboard().put(RECONSTRUCTION_VECTORS_KEY, new ArrayList());
            addModificationListener(outputLayer);
        }
        return outputLayer;
    }

    private String outputLayerName(String sourceLayerName) {
        return I18N.get("ui.warp.WarpingPanel.warped")+" "+sourceLayerName;
    }

    private void setWarpingFlag() {
        warping = true;
    }

    private void showTriangulation() {
        ShowTriangulationPlugIn showTriangulationPlugIn =
            new ShowTriangulationPlugIn(this);
        if (showTriangulationPlugIn.createEnableCheck(toolbox.getContext()).check(null)
            != null) {
            return;
        }
        toolbox.getContext().getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            showTriangulationPlugIn.execute(toolbox.getContext().createPlugInContext());
        } catch (Throwable t) {
            toolbox.getContext().getErrorHandler().handleThrowable(t);
        } finally {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .stopReceiving();
        }
    }

    private Coordinate tail(Feature vector) {
        return ((LineString) vector.getGeometry()).getCoordinateN(0);
    }

    private Coordinate tip(Feature vector) {
        return ((LineString) vector.getGeometry()).getCoordinateN(1);
    }

    private Collection toWarpingVectors(
        Collection incrementalWarpingVectors,
        Collection reconstructionVectors,
        Layer sourceLayer) {
        ArrayList warpingVectors = new ArrayList();
        CoordinateTransform transform =
            reconstructionVectors.isEmpty()
                || sourceLayer == null
                    ? (CoordinateTransform) new DummyTransform()
                    : new BilinearInterpolatedTransform(
                        CollectionUtil.inverse(
                            triangleMap(
                                sourceLayer.getFeatureCollectionWrapper().getEnvelope(),
                                reconstructionVectors,
                                new ArrayList(),
                                Triangulator.taggedVectorVertices(
                                    false,
                                    FeatureUtil.toGeometries(
                                        incrementalWarpingVectors)))),
                        new DummyTaskMonitor());
        Collection reconstructionVectorTips =
            Triangulator.taggedVectorVertices(
                true,
                FeatureUtil.toGeometries(reconstructionVectors));
        //Explicitly add the reconstruction vectors to handle the following case:
        //You've done a warp using warping vectors. Now you want to do more
        //warping using incremental warping vectors. At this point, you don't
        //have incremental warping vectors to turn into warping vectors -- you've
        //just got reconstruction vectors. [Jon Aquino]                        
        warpingVectors.addAll(reconstructionVectors);
        for (Iterator i = incrementalWarpingVectors.iterator(); i.hasNext();) {
            Feature incrementalWarpingVector = (Feature) i.next();
            Feature warpingVector = (Feature) incrementalWarpingVector.clone();
            Coordinate tail =
                ((LineString) warpingVector.getGeometry()).getCoordinateN(0);
            Coordinate tip = ((LineString) warpingVector.getGeometry()).getCoordinateN(1);
            if (tail.equals(tip) && reconstructionVectorTips.contains(tip)) {
                //If this zero-length incremental vector came from a warping vector,
                //the warping vector is now a reconstruction vector and has 
                //alrady been added above. [Jon Aquino]
                continue;
            }
            tail.setCoordinate(transform.transform(tail));
            warpingVector.getGeometry().geometryChanged();
            warpingVectors.add(warpingVector);
        }
        return warpingVectors;
    }

    public Map triangleMap(
        Envelope sourceLayerEnvelope,
        Collection vectorFeatures,
        Collection sourceHints,
        Collection destinationHints) {
        Collection vectorLineStrings =
            FeatureUtil.toGeometries(
                CopySelectedLayersToWarpingVectorsPlugIn.removeNonVectorFeaturesAndWarn(
                    vectorFeatures,
                    toolbox.getContext().getWorkbench().getFrame()));        
        Map triangleMap =
            triangulator.triangleMap(
                sourceLayerEnvelope,
                vectorLineStrings,
                sourceHints,
                destinationHints,
                dummyMonitor);
        Assert.isTrue(
            triangulator.getIgnoredVectors().isEmpty(),
            !triangulator.getIgnoredVectors().isEmpty()
                ? triangulator.getIgnoredVectors().iterator().next().toString()
                : "");
        return triangleMap;
    }

    void triangulationCheckBox_actionPerformed(ActionEvent e) {
        if (triangulationCheckBox.isSelected()) {
            showTriangulation();
        } else {
            hideTriangulation();
        }
    }

    private void warp() {
        toolbox.getContext().getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .reportNothingToUndoYet();
            UndoableCommand command = createWarpCommand();
            command.execute();
            toolbox.getContext().getLayerManager().getUndoableEditReceiver().receive(
                command.toUndoableEdit());
        } finally {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .stopReceiving();
        }
    }

    void warpButton_actionPerformed(ActionEvent e) {
        try {
            if (warpConditionsMet()) {
            	//[sstein 31Mar2008] -- added to inform user
            	if(this.currentSourceLayer() != null){
            		if (this.currentSourceLayer().getFeatureCollectionWrapper().size() == 1){
            			Feature f = (Feature)this.currentSourceLayer().getFeatureCollectionWrapper().getFeatures().get(0);
            			if (f.getGeometry() instanceof Point){
		            		String sWarning = I18N.get("ui.warp.WarpingPanel.initerror-for-one-point");
		            		toolbox.getContext().getWorkbench().getFrame().warnUser(sWarning);
            			}
	            	}
            	}
                warp();
            }
        } catch (Throwable t) {
            toolbox.getContext().getErrorHandler().handleThrowable(t);
        }
    }

    public boolean warpConditionsMet() {
        return layerViewPanelProxyActive() && sourceLayerComboBox.getSelectedIndex() > -1;
    }

    /**
     * @return null if the output layer does not yet exist
     */
    private Layer currentOutputLayer() {
        if (currentSourceLayer() == null) {
            return null;
        }
        return outputLayer(currentSourceLayer().getName());
    }

    /**
     * @return null if the combo box is empty
     */
    public Layer currentSourceLayer() {
        return (Layer) sourceLayerComboBox.getSelectedItem();
    }

    public UndoableCommand createWarpCommand() {
        Assert.isTrue(currentSourceLayer() != null);
        final Layer outputLayer = currentOutputLayer();
        final boolean outputLayerExistedOriginally = outputLayer != null;
        final Collection oldVectors =
            outputLayer != null
                ? new ArrayList(
                    (Collection) outputLayer.getBlackboard().get(
                        RECONSTRUCTION_VECTORS_KEY))
                : new ArrayList();
        final Collection newVectors =
            warpingVectorLayerFinder().getLayer() == null
                ? new ArrayList()
                : new ArrayList(
                    warpingVectorLayerFinder()
                        .getLayer()
                        .getFeatureCollectionWrapper()
                        .getFeatures());
        if (outputLayerExistedOriginally
            && outputLayer.getBlackboard().getBoolean(MODIFIED_OUTSIDE_WARP_KEY)) {
            toolbox
                .getContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                .reportIrreversibleChange();
        }
        final Layer sourceLayer = currentSourceLayer();
        final boolean willHideWarpingVectorLayer =
            isAutoHidingLayers()
                && warpingVectorLayerFinder().getLayer() != null
                && warpingVectorLayerFinder().getLayer().isVisible()
                && isWarpingIncrementally();
        final boolean willHideIncrementalWarpingVectorLayer =
                    isAutoHidingLayers()
                        && incrementalWarpingVectorLayerFinder().getLayer() != null
                        && incrementalWarpingVectorLayerFinder().getLayer().isVisible()
                        && !isWarpingIncrementally();                
        final boolean willHideSourceLayer =
            isAutoHidingLayers() && sourceLayer != null && sourceLayer.isVisible();
        final boolean warpingIncrementally = isWarpingIncrementally();
        return Layer
            .addUndo(
                incrementalWarpingVectorLayerFinder().getLayerName(),
                toolbox.getContext(),
                new ShowTriangulationPlugIn(
                    this).addLayerGeneration(new UndoableCommand(warpButton.getText()) {
            public void execute() {
                try {
                    warp(sourceLayer, newVectors, warpingIncrementally);
                    if (willHideIncrementalWarpingVectorLayer) {
                        incrementalWarpingVectorLayerFinder().getLayer().setVisible(false);
                    }
                    if (willHideWarpingVectorLayer) {
                        warpingVectorLayerFinder().getLayer().setVisible(false);
                    }
                    if (willHideSourceLayer) {
                        sourceLayer.setVisible(false);
                    }
                } catch (Throwable t) {
                    toolbox.getContext().getErrorHandler().handleThrowable(t);
                    toolbox
                        .getContext()
                        .getLayerManager()
                        .getUndoableEditReceiver()
                        .reportIrreversibleChange();
                }
            }
            public void unexecute() {
                try {
                    if (willHideSourceLayer) {
                        sourceLayer.setVisible(true);
                    }
                    if (willHideIncrementalWarpingVectorLayer) {
                        incrementalWarpingVectorLayerFinder().getLayer().setVisible(true);
                    }
                    if (willHideWarpingVectorLayer) {
                        warpingVectorLayerFinder().getLayer().setVisible(true);
                    }
                    if (outputLayerExistedOriginally) {
                        warp(sourceLayer, oldVectors, false);
                    } else {
                        toolbox.getContext().getLayerManager().remove(
                            outputLayer(sourceLayer.getName()));
                    }
                } catch (Throwable t) {
                    toolbox.getContext().getErrorHandler().handleThrowable(t);
                    toolbox
                        .getContext()
                        .getLayerManager()
                        .getUndoableEditReceiver()
                        .reportIrreversibleChange();
                }
            }
        }, toolbox.getContext(), false));
    }

    private void warp(
        Layer sourceLayer,
        Collection warpingVectors,
        boolean generateIncrementalWarpingVectors)
        throws JUMPException {
        setWarpingFlag();
        try {
            Map triangleMap =
                triangleMap(
                    sourceLayer.getFeatureCollectionWrapper().getEnvelope(),
                    warpingVectors,
                    new ArrayList(),
                    new ArrayList());
            CoordinateTransform transform =
                new BilinearInterpolatedTransform(triangleMap, dummyMonitor);
            FeatureCollection outputFeatureCollection =
                transform.transform(sourceLayer.getFeatureCollectionWrapper());
            Layer outputLayer = outputLayer(sourceLayer.getName());
            if (outputLayer == null) {
                outputLayer =
                    toolbox.getContext().getLayerManager().addLayer(
                        StandardCategoryNames.RESULT_SUBJECT,
                        outputLayerName(sourceLayer.getName()),
                        outputFeatureCollection);
                outputLayer.setStyles(sourceLayer.cloneStyles());
                addModificationListener(outputLayer);
            } else {
                outputLayer.setFeatureCollection(outputFeatureCollection);
            }
            outputLayer.getBlackboard().put(MODIFIED_OUTSIDE_WARP_KEY, false);
            outputLayer.getBlackboard().put(
                RECONSTRUCTION_VECTORS_KEY,
                clone(warpingVectors));
            if (generateIncrementalWarpingVectors) {
                if (incrementalWarpingVectorLayerFinder().getLayer() == null) {
                    incrementalWarpingVectorLayerFinder().createLayer();
                }
                incrementalWarpingVectorLayerFinder()
                    .getLayer()
                    .getFeatureCollectionWrapper()
                    .clear();
                incrementalWarpingVectorLayerFinder()
                    .getLayer()
                    .getFeatureCollectionWrapper()
                    .addAll(
                    collapseToTip(warpingVectors));
            }
        } finally {
            clearWarpingFlag();
        }
    }

    public boolean isWarpingIncrementally() {
        return warpIncrementallyCheckBox.isEnabled()
            && warpIncrementallyCheckBox.isSelected();
    }

    void sourceComboBox_actionPerformed(ActionEvent e) {
        if (initializingSourceLayerComboBox) {
            //Selected item fluctuates during this time, confusing the "last source layer"
            //cache. [Jon Aquino]
            return;
        }
        if (sourceLayerComboBoxModel.getSize() == 0) {
            return;
        }
        ((Layer) sourceLayerComboBoxModel.getSelectedItem())
            .getLayerManager()
            .getBlackboard()
            .put(LAST_SOURCE_LAYER_KEY, sourceLayerComboBoxModel.getSelectedItem());
    }

    private final static String LAST_SOURCE_LAYER_KEY =
        WarpingPanel.class.getName() + " - LAST SOURCE LAYER";

    private IncrementalWarpingVectorLayerFinder incrementalWarpingVectorLayerFinder() {
        return new IncrementalWarpingVectorLayerFinder(toolbox.getContext());
    }

    private WarpingVectorLayerFinder warpingVectorLayerFinder() {
        return new WarpingVectorLayerFinder(toolbox.getContext());
    }

    private boolean excludingFromLayerList(Layer layer) {
        if (layer == warpingVectorLayerFinder().getLayer()) {
            return true;
        }
        if (layer == incrementalWarpingVectorLayerFinder().getLayer()) {
            return true;
        }
        if (layer.getName().equals(ShowTriangulationPlugIn.SOURCE_LAYER_NAME)) {
            return true;
        }
        if (layer.getName().equals(ShowTriangulationPlugIn.DESTINATION_LAYER_NAME)) {
            return true;
        }
        return false;
    }

    private boolean initializingSourceLayerComboBox = false;

    private DefaultComboBoxModel sourceLayerComboBoxModel = new DefaultComboBoxModel();
    private ToolboxDialog toolbox;

    public WarpingPanel(ToolboxDialog toolbox) {
        this.toolbox = toolbox;
        toolbox.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                updateComponents();
            }
        });
        GUIUtil
            .addInternalFrameListener(
                toolbox.getContext().getWorkbench().getFrame().getDesktopPane(),
                GUIUtil.toInternalFrameListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateComponents();
            }
        }));
        sourceLayerComboBox.setModel(sourceLayerComboBoxModel);
        LayerNameRenderer layerListCellRenderer = new LayerNameRenderer();
        layerListCellRenderer.setCheckBoxVisible(false);
        layerListCellRenderer.setProgressIconLabelVisible(false);
        sourceLayerComboBox.setRenderer(layerListCellRenderer);
        warpButton.setIcon(IconLoader.icon("GoalFlag.gif"));
        layerLabel.setText(I18N.get("ui.warp.WarpingPanel.source-layer"));
        this.setLayout(gridBagLayout1);
        warpIncrementallyCheckBox.setToolTipText(
        		I18N.get("ui.warp.WarpingPanel.warps-relative-to-the-output-layer-as-soon-as-a-vector-is-drawn"));
        warpIncrementallyCheckBox.setSelected(false);
        warpIncrementallyCheckBox.setText(I18N.get("ui.warp.WarpingPanel.warp-incrementally"));
        warpIncrementallyCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                warpIncrementallyCheckBox_actionPerformed(e);
            }
        });
        sourceLayerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sourceComboBox_actionPerformed(e);
            }
        });
        buttonPanel.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(2);
        warpButton.setText(I18N.get("ui.warp.WarpingPanel.warp"));
        warpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                warpButton_actionPerformed(e);
            }
        });
        clearOutputButton.setText(I18N.get("ui.warp.WarpingPanel.clear-all-vectors"));
        clearOutputButton.setToolTipText(I18N.get("ui.warp.WarpingPanel.deletes-the-warp-output-layer-and-the-vectors"));
        clearOutputButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearOutputButton_actionPerformed(e);
            }
        });
        autoHideCheckBox.setToolTipText(
        		I18N.get("ui.warp.WarpingPanel.auto-hides-the-source-layer-and-the-warping-vectors"));
        autoHideCheckBox.setSelected(true);
        autoHideCheckBox.setText(I18N.get("ui.warp.WarpingPanel.auto-hide-layers"));
        triangulationCheckBox.setToolTipText(
        		I18N.get("ui.warp.WarpingPanel.shows-the-initial-and-final-triangulation-layers"));
        triangulationCheckBox.setText(I18N.get("ui.warp.WarpingPanel.display-triangulation"));
        triangulationCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                triangulationCheckBox_actionPerformed(e);
            }
        });
        copyLayerButton.setToolTipText(I18N.get("ui.warp.WarpingPanel.copies-the-feature-in-the-selected-layer-to-the-warping-vectors-layer"));
        copyLayerButton.setText(I18N.get("ui.warp.WarpingPanel.copy-layer-to-vectors"));
        copyLayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyLayerButton_actionPerformed(e);
            }
        });
        this.add(
            layerLabel,
            new GridBagConstraints(
                0,
                1,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
            sourceLayerComboBox,
            new GridBagConstraints(
                0,
                2,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
            warpIncrementallyCheckBox,
            new GridBagConstraints(
                0,
                4,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 4, 0, 4),
                0,
                0));
        this.add(
            buttonPanel,
            new GridBagConstraints(
                0,
                8,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
            autoHideCheckBox,
            new GridBagConstraints(
                0,
                5,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 4, 0, 0),
                0,
                0));
        this.add(
            triangulationCheckBox,
            new GridBagConstraints(
                0,
                6,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 4, 0, 4),
                0,
                0));
        this.add(
            warpButton,
            new GridBagConstraints(
                0,
                10,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 4, 4, 4),
                0,
                0));
        this.add(
            clearOutputButton,
            new GridBagConstraints(
                0,
                11,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 4, 0, 4),
                0,
                0));
        this.add(
            copyLayerButton,
            new GridBagConstraints(
                0,
                12,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 4, 4, 4),
                0,
                0));

    }

    private JCheckBox autoHideCheckBox = new JCheckBox();

    private JPanel buttonPanel = new JPanel();

    private JButton clearOutputButton = new JButton();

    private JButton copyLayerButton = new JButton();

    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    private GridLayout gridLayout1 = new GridLayout();

    private JLabel layerLabel = new JLabel();

    private JComboBox sourceLayerComboBox = new JComboBox();

    private JCheckBox triangulationCheckBox = new JCheckBox();

    private JButton warpButton = new JButton();

    private JCheckBox warpIncrementallyCheckBox = new JCheckBox();

    void warpIncrementallyCheckBox_actionPerformed(ActionEvent e) {
        updateComponents();
    }

    private LayerNamePanelListener layerNamePanelListener =
        new LayerNamePanelListener() {
        public void layerSelectionChanged() {
            updateComponents();
        }
    };

    private LayerNamePanel lastLayerNamePanel = null;

    public void updateComponents() {
        toolbox.updateEnabledState();
        clearOutputButton.setEnabled(
            toolbox.getContext().getWorkbench().getFrame().getActiveInternalFrame()
                instanceof TaskFrame);
        if (toolbox.getContext().getWorkbench().getFrame().getActiveInternalFrame()
            instanceof TaskFrame) {
            if (lastLayerNamePanel != null) {
                lastLayerNamePanel.removeListener(layerNamePanelListener);
            }
            lastLayerNamePanel =
                ((LayerNamePanelProxy) toolbox
                    .getContext()
                    .getWorkbench()
                    .getFrame()
                    .getActiveInternalFrame())
                    .getLayerNamePanel();
            lastLayerNamePanel.addListener(layerNamePanelListener);
        }
        copyLayerButton.setEnabled(
            null
                == new CopySelectedLayersToWarpingVectorsPlugIn()
                    .createEnableCheck(toolbox.getContext())
                    .check(null));
        triangulationCheckBox.setSelected(
            toolbox.getContext().getLayerViewPanel() != null
                && toolbox.getContext().getLayerManager() != null
                && toolbox.getContext().getLayerManager().getLayer(
                    ShowTriangulationPlugIn.SOURCE_LAYER_NAME)
                    != null
                && toolbox
                    .getContext()
                    .getLayerManager()
                    .getLayer(ShowTriangulationPlugIn.SOURCE_LAYER_NAME)
                    .isVisible()
                && toolbox.getContext().getLayerManager().getLayer(
                    ShowTriangulationPlugIn.DESTINATION_LAYER_NAME)
                    != null
                && toolbox
                    .getContext()
                    .getLayerManager()
                    .getLayer(ShowTriangulationPlugIn.DESTINATION_LAYER_NAME)
                    .isVisible());
        updateSourceLayerComboBox();
        if (toolbox.getButton(DrawIncrementalWarpingVectorTool.class).isSelected()
            && !toolbox.getButton(DrawIncrementalWarpingVectorTool.class).isEnabled()) {
            toolbox.getButton(DrawWarpingVectorTool.class).doClick();
        }
        if (toolbox.getButton(DeleteIncrementalWarpingVectorTool.class).isSelected()
            && !toolbox.getButton(DeleteIncrementalWarpingVectorTool.class).isEnabled()) {
            toolbox.getButton(DeleteWarpingVectorTool.class).doClick();
        }
        if (toolbox.getButton(DrawWarpingVectorTool.class).isSelected()
            && !toolbox.getButton(DrawWarpingVectorTool.class).isEnabled()) {
            toolbox.getButton(DrawIncrementalWarpingVectorTool.class).doClick();
        }
        if (toolbox.getButton(DeleteWarpingVectorTool.class).isSelected()
            && !toolbox.getButton(DeleteWarpingVectorTool.class).isEnabled()) {
            toolbox.getButton(DeleteIncrementalWarpingVectorTool.class).doClick();
        }
    }

    private void updateSourceLayerComboBox() {
        initializingSourceLayerComboBox = true;
        try {
            sourceLayerComboBoxModel.removeAllElements();
            if (!(toolbox.getContext().getWorkbench().getFrame().getActiveInternalFrame()
                instanceof LayerViewPanelProxy)) {
                return;
            }
            LayerViewPanelProxy proxy =
                (LayerViewPanelProxy) toolbox
                    .getContext()
                    .getWorkbench()
                    .getFrame()
                    .getActiveInternalFrame();
            for (Iterator i =
                proxy.getLayerViewPanel().getLayerManager().getLayers().iterator();
                i.hasNext();
                ) {
                Layer layer = (Layer) i.next();
                if (excludingFromLayerList(layer)) {
                    continue;
                }
                sourceLayerComboBoxModel.addElement(layer);
            }
            if (sourceLayerComboBoxModel.getSize() > 0) {
                Layer lastSourceLayer =
                    (Layer) proxy
                        .getLayerViewPanel()
                        .getLayerManager()
                        .getBlackboard()
                        .get(
                        LAST_SOURCE_LAYER_KEY);
                if (lastSourceLayer == null
                    || !proxy.getLayerViewPanel().getLayerManager().getLayers().contains(
                        lastSourceLayer)) {
                    proxy.getLayerViewPanel().getLayerManager().getBlackboard().put(
                        LAST_SOURCE_LAYER_KEY,
                        sourceLayerComboBoxModel.getElementAt(0));
                }
                sourceLayerComboBoxModel.setSelectedItem(
                    proxy.getLayerViewPanel().getLayerManager().getBlackboard().get(
                        LAST_SOURCE_LAYER_KEY));
            }
            String listenerAddedKey = getClass().getName() + " - LISTENER ADDED";
            if (!proxy
                .getLayerViewPanel()
                .getLayerManager()
                .getBlackboard()
                .get(listenerAddedKey, false)) {
                proxy
                    .getLayerViewPanel()
                    .getLayerManager()
                    .addLayerListener(new LayerListener() {
                    public void categoryChanged(CategoryEvent e) {}
                    public void layerChanged(LayerEvent e) {
                        updateSourceLayerComboBox();
                    }
                    public void featuresChanged(FeatureEvent e) {}
                });
                proxy.getLayerViewPanel().getLayerManager().getBlackboard().put(
                    listenerAddedKey,
                    true);
            }
        } finally {
            initializingSourceLayerComboBox = false;
        }
    }

    public UndoableCommand addWarpingVectorGeneration(final UndoableCommand wrappeeCommand)
        throws NoninvertibleTransformException {
        return new UndoableCommand(wrappeeCommand.getName()) {
            private UndoableCommand generateWarpingVectorsCommand = null;
            //Initialize generateWarpingVectorsCommand lazily because it requires that
            //addRelativeVectorCommand execute first. [Jon Aquino]
            private UndoableCommand generateWarpingVectorsCommand() {
                if (generateWarpingVectorsCommand == null) {
                    generateWarpingVectorsCommand =
                        WarpingPanel.this.generateWarpingVectorsCommand();
                }
                return generateWarpingVectorsCommand;
            }
            public void execute() {
                wrappeeCommand.execute();
                generateWarpingVectorsCommand().execute();
            }
            public void unexecute() {
                generateWarpingVectorsCommand().unexecute();
                wrappeeCommand.unexecute();
            }
        };
    }

}
