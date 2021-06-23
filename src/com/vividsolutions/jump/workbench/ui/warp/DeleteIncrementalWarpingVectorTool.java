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

import java.awt.Cursor;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Collection;

import javax.swing.Icon;

import org.locationtech.jts.util.Assert;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.AbstractVectorLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class DeleteIncrementalWarpingVectorTool extends AbstractDeleteVectorTool {

    public DeleteIncrementalWarpingVectorTool(WorkbenchContext context, WarpingPanel warpingPanel) {
      super(context);
      this.warpingPanel = warpingPanel;
    }

    private WarpingPanel warpingPanel;

    protected AbstractVectorLayerFinder createVectorLayerFinder(LayerManagerProxy layerManagerProxy) {
        return new WarpingVectorLayerFinder(layerManagerProxy);
    }

    protected AbstractVectorLayerFinder createIncrementalVectorLayerFinder(LayerManagerProxy layerManagerProxy) {
        return new IncrementalWarpingVectorLayerFinder(layerManagerProxy);
    }

    protected UndoableCommand createCommand() throws NoninvertibleTransformException {
        return warpingPanel.addWarping(warpingPanel.addWarpingVectorGeneration(createBaseCommand()));
    }

    public Icon getIcon() {
        return IconLoader.icon("DeleteVectors.gif");
    }

    public Cursor getCursor() {
        return createCursor(IconLoader.icon("DeleteVectorCursor.gif").getImage());
    }

    /**
     * The command returned uses super.createCommand to delete the feature in VectorLayer
     * and add the deletion of the feature from IncrementalVectorLayer
     * @return an UndoableCommand
     * @throws NoninvertibleTransformException if an Exception occurred in layerToSpecifiedFeaturesMap
     */
    protected UndoableCommand createBaseCommand() throws NoninvertibleTransformException {
        final UndoableCommand superCommand = super.createCommand();
        final AbstractVectorLayerFinder incrementalVectorLayerFinder =
                createIncrementalVectorLayerFinder(getPanel());
        Assert.isTrue(incrementalVectorLayerFinder != null);
        Layer layer = incrementalVectorLayerFinder.getLayer();
        Assert.isTrue(layer != null);
        boolean oldVisible = layer.isVisible();
        layer.setVisible(true); // next instruction search only in visible layers
        final Collection incrementalVectorFeaturesToDelete =
                (Collection) layerToSpecifiedFeaturesMap().get(layer);
        layer.setVisible(oldVisible);
        Assert.isTrue(incrementalVectorFeaturesToDelete != null);
        Assert.isTrue(!incrementalVectorFeaturesToDelete.isEmpty());
        return new UndoableCommand(getName()) {
            public void execute() {
                superCommand.execute();
                incrementalVectorLayerFinder.getLayer().getFeatureCollectionWrapper()
                        .removeAll(incrementalVectorFeaturesToDelete);
                showAnimation(incrementalVectorFeaturesToDelete);
            }
            public void unexecute() {
                superCommand.unexecute();
                incrementalVectorLayerFinder.getLayer()
                        .getFeatureCollectionWrapper().addAll(incrementalVectorFeaturesToDelete);
            }
        };
    }

}
