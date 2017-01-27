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

import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.model.AbstractVectorLayerFinder;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.ui.cursortool.Animations;
import com.vividsolutions.jump.workbench.ui.cursortool.SpecifyFeaturesTool;

public abstract class AbstractDeleteVectorTool extends SpecifyFeaturesTool {

    public AbstractDeleteVectorTool() {
        super();
        //The cursor is big and the pin icons are big, so make the click buffer big. [Jon Aquino]
        setViewClickBuffer(6);
    }
       
    private void showAnimation(Collection vectorFeatures) {
        try {
            Animations.drawExpandingRings(
                getPanel().getViewport().toViewPoints(centres(vectorFeatures)),
                true,
                getColor(),
                getPanel(),
                new float[] { 15, 15 });
        } catch (NoninvertibleTransformException e) {
            //Eat it. [Jon Aquino]
        }
    }        

    private Collection centres(Collection vectorFeatures) {
        ArrayList centers = new ArrayList();
        for (Iterator i = vectorFeatures.iterator(); i.hasNext();) {
            Feature vectorFeature = (Feature) i.next();
            Envelope envelope = vectorFeature.getGeometry().getEnvelopeInternal();
            //<<TODO>> Envelope shouldn't be null. Use assert instead of if. [Jon Aquino]
            if (envelope.isNull()) {
                continue;
            }
            centers.add(EnvelopeUtil.centre(envelope));
        }
        return centers;
    }
    
    protected abstract AbstractVectorLayerFinder createVectorLayerFinder(LayerManagerProxy layerManagerProxy);
                
    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();
        AbstractVectorLayerFinder finder = createVectorLayerFinder(getPanel());
        if (finder.getLayer() == null) {
            return;
        }
        if (!layerToSpecifiedFeaturesMap().containsKey(finder.getLayer())) {
            return;
        }
        execute(createCommand());        
    }
    
    protected UndoableCommand createCommand() throws NoninvertibleTransformException {
        final AbstractVectorLayerFinder finder = createVectorLayerFinder(getPanel());
        final Collection vectorFeaturesToDelete =
            (Collection) layerToSpecifiedFeaturesMap().get(finder.getLayer());
        Assert.isTrue(vectorFeaturesToDelete != null);
        Assert.isTrue(!vectorFeaturesToDelete.isEmpty());
        return new UndoableCommand(getName()) {
            public void execute() {                
                finder.getLayer().getFeatureCollectionWrapper().removeAll(vectorFeaturesToDelete);
                showAnimation(vectorFeaturesToDelete);
            }
            public void unexecute() {
                finder.getLayer().getFeatureCollectionWrapper().addAll(vectorFeaturesToDelete);
            }
        };
    }    

}
