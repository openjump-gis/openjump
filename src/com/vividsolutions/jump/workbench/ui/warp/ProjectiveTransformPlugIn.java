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

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openjump.core.apitools.LayerTools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.util.AssertionFailedException;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.warp.ProjectiveTransform;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Applies a projective transform to the selected layers. The projective
 * transform is specified using four vectors drawn by the user.
 * 
 * @date 2017_11_10 Giuseppe Aruta (giuseppe_aruta[AT]yahoo.it) First realize
 */

public class ProjectiveTransformPlugIn extends AbstractPlugIn {

    public static String path;

    public static EnableCheck getEnableCheck(
            EnableCheckFactory enableCheckFactory) {
        return new MultiEnableCheck().add(
                enableCheckFactory.createAtLeastNLayerablesMustExistCheck(1))
                .add(enableCheckFactory
                        .createExactlyNVectorsMustBeDrawnCheck(4));

    }

    @Override
    public String getName() {
        return I18N
                .get("com.vividsolutions.jump.workbench.ui.warp.ProjectiveTransformPlugIn");
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] { MenuNames.TOOLS, MenuNames.TOOLS_WARP },
                getName(), false, null,
                getEnableCheck(context.getCheckFactory()));
    }

    @Override
    public boolean execute(final PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        Layerable layer = LayerTools.getSelectedLayerable(context,
                Layerable.class);

        if (layer instanceof Layer
                && ((Layer) layer).getStyle(ReferencedImageStyle.class) == null) {
            affineTransformVector(context);

            // We exlude for now image files loaded via Layer.class
        } else if (layer instanceof Layer
                && ((Layer) layer).getStyle(ReferencedImageStyle.class) != null) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            I18N.get("com.vividsolutions.jump.workbench.ui.warp.AffineTransformPlugIn.message1"),
                            null, JOptionPane.INFORMATION_MESSAGE);
            return false;
            // WE exclude WMS layer
        } else {
            JOptionPane
                    .showMessageDialog(
                            null,
                            I18N.get("com.vividsolutions.jump.workbench.ui.warp.AffineTransformPlugIn.message2"),
                            null, JOptionPane.INFORMATION_MESSAGE);
            return false;

        }

        return true;
    }

    /*
     * Affine transformation of a Vector Layer
     */
    public static void affineTransformVector(PlugInContext context)
            throws JUMPException {
        ProjectiveTransform transform = projectiveTransform(context);
        FeatureCollection featureCollection = transform.transform(context
                .getSelectedLayer(0).getFeatureCollectionWrapper());
        context.getLayerManager().addLayer(
                StandardCategoryNames.WORKING,
                I18N.get("ui.warp.AffineTransformPlugIn.affined") + " "
                        + context.getSelectedLayer(0).getName(),
                featureCollection);
        checkValid(featureCollection, context);
    }

    public static void checkValid(FeatureCollection featureCollection,
            PlugInContext context) {
        for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();

            if (!feature.getGeometry().isValid()) {
                context.getLayerViewPanel()
                        .getContext()
                        .warnUser(
                                I18N.get("ui.warp.AffineTransformPlugIn.some-geometries-are-not-valid"));

                return;
            }
        }
    }

    private static Coordinate vectorCoordinate(int n, boolean tip,
            PlugInContext context, WarpingVectorLayerFinder vectorLayerManager) {
        LineString vector = (LineString) vectorLayerManager.getVectors().get(n);

        return tip ? vector.getCoordinateN(1) : vector.getCoordinateN(0);
    }

    private static ProjectiveTransform projectiveTransform(PlugInContext context) {
        WarpingVectorLayerFinder vlm = new WarpingVectorLayerFinder(context);

        Integer numVectors = vlm.getVectors().size();
        if (numVectors < 4) {
            JUMPWorkbench
                    .getInstance()
                    .getFrame()
                    .getContext()
                    .getLayerViewPanel()
                    .getContext()
                    .setStatusMessage(
                            I18N.getMessage("Warping vectors: " + "{0}",
                                    new Object[] { numVectors }
                                            + "\n Especting 4"));
        } else if (numVectors == 4) {
            return new ProjectiveTransform(vectorCoordinate(0, false, context,
                    vlm), vectorCoordinate(0, true, context, vlm),
                    vectorCoordinate(1, false, context, vlm), vectorCoordinate(
                            1, true, context, vlm), vectorCoordinate(2, false,
                            context, vlm), vectorCoordinate(2, true, context,
                            vlm), vectorCoordinate(3, false, context, vlm),
                    vectorCoordinate(3, true, context, vlm));
        } else {
            JUMPWorkbench.getInstance().getFrame().getContext()
                    .getLayerViewPanel().getContext()
                    .warnUser(I18N.get("ui.warp.WarpingPanel.warning_1"));
        }

        return null;
    }

    public static void warning(String paramString) {
        throw new AssertionFailedException(paramString != null ? paramString
                : "");
    }

    /*
     * To work with WarpPanel
     */

    private static Coordinate vectorCoordinatePublic(int n, boolean tip,
            WarpingVectorLayerFinder vectorLayerManager) {
        LineString vector = (LineString) vectorLayerManager.getVectors().get(n);

        return tip ? vector.getCoordinateN(1) : vector.getCoordinateN(0);
    }

    public static ProjectiveTransform projectiveTransformPublic() {

        WarpingVectorLayerFinder vlm = new WarpingVectorLayerFinder(
                JUMPWorkbench.getInstance().getContext());

        Integer numVectors = vlm.getVectors().size();
        if (numVectors == 4) {
            return new ProjectiveTransform(
                    vectorCoordinatePublic(0, false, vlm),
                    vectorCoordinatePublic(0, true, vlm),
                    vectorCoordinatePublic(1, false, vlm),
                    vectorCoordinatePublic(1, true, vlm),
                    vectorCoordinatePublic(2, false, vlm),
                    vectorCoordinatePublic(2, true, vlm),
                    vectorCoordinatePublic(3, false, vlm),
                    vectorCoordinatePublic(3, true, vlm));
        } else {
            JUMPWorkbench.getInstance().getFrame().getContext()
                    .getLayerViewPanel().getContext()
                    .warnUser(I18N.get("ui.warp.WarpingPanel.warning_2"));
        }

        return null;
    }

    public Icon createEnableCheck(WorkbenchContext workbenchContext) {
        return null;
    }
}