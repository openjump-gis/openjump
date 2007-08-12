
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

package com.vividsolutions.jump.workbench.ui.zoom;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.NoninvertibleTransformException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.ThreadQueue;

/**
 * Zoom to the features, then flash them.
 */
public class ZoomToSelectedItemsPlugIn extends AbstractPlugIn {

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        zoom(
            context.getLayerViewPanel().getSelectionManager().getSelectedItems(),
            context.getLayerViewPanel());
        return true;
    }

    public void zoom(final Collection geometries, final LayerViewPanel panel)
        throws NoninvertibleTransformException {
        if (envelope(geometries).isNull()) {
            return;
        }
        Envelope proposedEnvelope =
            EnvelopeUtil.bufferByFraction(
                envelope(geometries),
                zoomBufferAsExtentFraction(geometries));

        if ((proposedEnvelope.getWidth()
            > panel.getLayerManager().getEnvelopeOfAllLayers().getWidth())
            || (proposedEnvelope.getHeight()
                > panel.getLayerManager().getEnvelopeOfAllLayers().getHeight())) {
            //We've zoomed out farther than we would if we zoomed to all layers.
            //This is too far. Set scale to that of zooming to all layers,
            //and center on the selected features. [Jon Aquino]
            proposedEnvelope = panel.getViewport().fullExtent();
            EnvelopeUtil.translate(
                proposedEnvelope,
                CoordUtil.subtract(
                    EnvelopeUtil.centre(envelope(geometries)),
                    EnvelopeUtil.centre(proposedEnvelope)));
        }

        panel.getViewport().zoom(proposedEnvelope);
        //Wait until the zoom is complete before executing the flash. [Jon Aquino]
        ThreadQueue.Listener listener = new ThreadQueue.Listener() {
            public void allRunningThreadsFinished() {
                panel.getRenderingManager().getDefaultRendererThreadQueue().remove(this);
                try {
                    GUIUtil.invokeOnEventThread(new Runnable() {
                        public void run() {
                            try {
                                flash(geometries, panel);
                            } catch (NoninvertibleTransformException e) {}
                        }
                    });
                } catch (InterruptedException e) {} catch (InvocationTargetException e) {}
            }
        };
        panel.getRenderingManager().getDefaultRendererThreadQueue().add(listener);

    }

    private Envelope envelope(Collection geometries) {
        Envelope envelope = new Envelope();

        for (Iterator i = geometries.iterator(); i.hasNext();) {
            Geometry geometry = (Geometry) i.next();
            envelope.expandToInclude(geometry.getEnvelopeInternal());
        }

        return envelope;
    }

    private double zoomBufferAsExtentFraction(Collection geometries) {
        //Easiest to express zoomBuffer as a multiple of the average extent of
        //the individual features, rather than a multiple of the average extent
        //of the features combined. For example, 2 * the average extent of the
        //features combined can be a huge zoomBuffer if the features are far
        //apart. But if you consider the average extent of the individual features,
        //you don't need to think about how far apart the features are. [Jon Aquino]
        double zoomBuffer = 2 * averageExtent(geometries);
        double averageFullExtent = averageFullExtent(geometries);

        if (averageFullExtent == 0) {
            //Point feature. Just return 0. Rely on EnvelopeUtil#buffer to choose
            //a reasonable buffer for point features. [Jon Aquino]
            return 0;
        }

        return zoomBuffer / averageFullExtent;
    }

    private double averageExtent(Collection geometries) {
        Assert.isTrue(!geometries.isEmpty());

        double extentSum = 0;

        for (Iterator i = geometries.iterator(); i.hasNext();) {
            Geometry geometry = (Geometry) i.next();
            extentSum += geometry.getEnvelopeInternal().getWidth();
            extentSum += geometry.getEnvelopeInternal().getHeight();
        }

        return extentSum / (2d * geometries.size());
        //2 because width and height [Jon Aquino]
    }

    private double averageFullExtent(Collection geometries) {
        Envelope envelope = envelope(geometries);

        return (envelope.getWidth() + envelope.getHeight()) / 2d;
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1));
    }

    public void flash(Collection geometries, final LayerViewPanel panel)
        throws NoninvertibleTransformException {
        final GeometryCollection gc = toGeometryCollection(geometries);

        if (!panel
            .getViewport()
            .getEnvelopeInModelCoordinates()
            .intersects(gc.getEnvelopeInternal())) {
            return;
        }

        panel.flash(gc);
    }

    private GeometryCollection toGeometryCollection(Collection geometries) {
        return new GeometryFactory().createGeometryCollection(
            (Geometry[]) geometries.toArray(new Geometry[] {}));
    }

    public ImageIcon getIcon() {
        return IconLoader.icon("ZoomSelected.gif");
    }
}
