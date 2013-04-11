
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

import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.warp.AffineTransform;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 *  Applies an affine transform to the selected layers. The affine transform is
 *  specified using three vectors drawn by the user.
 */
public class AffineTransformPlugIn extends AbstractPlugIn {
    public AffineTransformPlugIn() {
    }

    public static EnableCheck getEnableCheck(EnableCheckFactory checkFactory) {
        return new MultiEnableCheck().add(
        		checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
    .add(checkFactory
                .createWindowWithLayerNamePanelMustBeActiveCheck())
    .add(checkFactory
                .createExactlyNLayersMustBeSelectedCheck(1))
    .add(checkFactory
                .createBetweenNAndMVectorsMustBeDrawnCheck(1, 3))  ; 	
    }
    
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(this,
            new String[] { MenuNames.TOOLS, MenuNames.TOOLS_WARP }, getName(), false, null,
            getEnableCheck(context.getCheckFactory())
        );
    }

    public boolean execute(PlugInContext context) throws Exception {
        AffineTransform transform = affineTransform(context);
        FeatureCollection featureCollection = transform.transform(context.getSelectedLayer(
                    0).getFeatureCollectionWrapper());
        context.getLayerManager().addLayer(StandardCategoryNames.WORKING,
        		I18N.get("ui.warp.AffineTransformPlugIn.affined")+" " + context.getSelectedLayer(0).getName(),
            featureCollection);
        checkValid(featureCollection, context);

        return true;
    }

    public static void checkValid(FeatureCollection featureCollection,
        PlugInContext context) {
        for (Iterator i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();

            if (!feature.getGeometry().isValid()) {
                context.getLayerViewPanel().getContext().warnUser(I18N.get("ui.warp.AffineTransformPlugIn.some-geometries-are-not-valid"));

                return;
            }
        }
    }

    /**
     *@return either the tip or the tail coordinate of the nth vector
     */
    private Coordinate vectorCoordinate(int n, boolean tip,
        PlugInContext context, WarpingVectorLayerFinder vectorLayerManager) {
        LineString vector = (LineString) vectorLayerManager.getVectors().get(n);

        return tip ? vector.getCoordinateN(1) : vector.getCoordinateN(0);
    }

    private AffineTransform affineTransform(PlugInContext context) {
        WarpingVectorLayerFinder vlm = new WarpingVectorLayerFinder(context);

        switch (vlm.getVectors().size()) {
        case 1:
            return new AffineTransform(vectorCoordinate(0, false, context, vlm),
                vectorCoordinate(0, true, context, vlm));

        case 2:
            return new AffineTransform(vectorCoordinate(0, false, context, vlm),
                vectorCoordinate(0, true, context, vlm),
                vectorCoordinate(1, false, context, vlm),
                vectorCoordinate(1, true, context, vlm));

        case 3:
            return new AffineTransform(vectorCoordinate(0, false, context, vlm),
                vectorCoordinate(0, true, context, vlm),
                vectorCoordinate(1, false, context, vlm),
                vectorCoordinate(1, true, context, vlm),
                vectorCoordinate(2, false, context, vlm),
                vectorCoordinate(2, true, context, vlm));
        }

        Assert.shouldNeverReachHere();

        return null;
    }
}
