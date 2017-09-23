/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Micha&euml;l Michaud.
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
 */

package org.openjump.core.ui.plugin.window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.ButtonGroup;
import javax.swing.JInternalFrame;
import javax.swing.JRadioButtonMenuItem;

import org.openjump.core.ui.plugin.AbstractUiPlugIn;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.ViewportListener;

/**
 * A plugin to synchronize / desynchronize LayerViewPanels.
 * 
 * @author Michael Michaud
 * @version 0.1 (2008-04-06)
 * @since 1.2F
 * 
 *        Giuseppe Aruta [2017-09-23] Added Syncronize zoom only
 */
public class SynchronizationPlugIn extends AbstractUiPlugIn {

    static ViewportListener vpl;

    public SynchronizationPlugIn() {
        super();
    }

    public SynchronizationPlugIn(String name) {
        super(name);
    }

    @Override
    public void initialize(final PlugInContext context) throws Exception {

        // Set the workbenchContext used in getEnableCheck method
        super.initialize(context);

        final JRadioButtonMenuItem desynchronizeMI = new JRadioButtonMenuItem(
                I18N.get("org.openjump.core.ui.plugin.window.SyncronizationPlugIn.Desynchronize"));
        final JRadioButtonMenuItem synchronizePanMI = new JRadioButtonMenuItem(
                I18N.get("org.openjump.core.ui.plugin.window.SyncronizationPlugIn.Synchronize-pan-only"));
        final JRadioButtonMenuItem synchronizeZoomMI = new JRadioButtonMenuItem(
                I18N.get("org.openjump.core.ui.plugin.window.SyncronizationPlugIn.Synchronize-zoom-only"));
        final JRadioButtonMenuItem synchronizeAllMI = new JRadioButtonMenuItem(
                I18N.get("org.openjump.core.ui.plugin.window.SyncronizationPlugIn.Synchronize-pan-and-zoom"));

        ButtonGroup bgroup = new ButtonGroup();
        bgroup.add(desynchronizeMI);
        bgroup.add(synchronizePanMI);
        bgroup.add(synchronizeZoomMI);
        bgroup.add(synchronizeAllMI);

        desynchronizeMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                desynchronize();
            }
        });
        synchronizePanMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronize(false);
            }
        });
        synchronizeAllMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronize(true);
            }
        });
        synchronizeZoomMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronizeZoom(true);
            }
        });

        context.getFeatureInstaller().addMainMenuItem(
                new String[] { MenuNames.WINDOW,
                        MenuNames.WINDOW_SYNCHRONIZATION }, this,
                synchronizePanMI, 0);
        context.getFeatureInstaller().addMainMenuItem(
                new String[] { MenuNames.WINDOW,
                        MenuNames.WINDOW_SYNCHRONIZATION }, this,
                synchronizeZoomMI, 1);
        context.getFeatureInstaller().addMainMenuItem(
                new String[] { MenuNames.WINDOW,
                        MenuNames.WINDOW_SYNCHRONIZATION }, this,
                synchronizeAllMI, 2);
        context.getFeatureInstaller().addMainMenuItem(
                new String[] { MenuNames.WINDOW,
                        MenuNames.WINDOW_SYNCHRONIZATION }, this,
                desynchronizeMI, 3);

    }

    @Override
    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.window.SyncronizationPlugIn.Synchronization");
    }

    @Override
    public EnableCheck getEnableCheck() {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck().add(checkFactory
                .createWindowWithLayerViewPanelMustBeActiveCheck());
    }

    public void synchronize(boolean panAndZoom) {
        ViewportListener oldViewportListener = vpl;
        vpl = createViewportListener(panAndZoom);
        // add the listener to every active map window
        JInternalFrame[] iframes = workbenchContext.getWorkbench().getFrame()
                .getInternalFrames();
        for (JInternalFrame iframe : iframes) {
            if (iframe instanceof TaskFrame) {
                // Remove the old listener if any before adding a new one
                // Thus, one can reinitialize synchronization if the number
                // of map windows change
                // Ideally, one should intercept new window creation or window
                // focus
                // change to add new listeners to new windows..
                ((TaskFrame) iframe).getLayerViewPanel().getViewport()
                        .removeListener(oldViewportListener);
                ((TaskFrame) iframe).getLayerViewPanel().getViewport()
                        .addListener(vpl);
            }
        }
    }

    public void synchronizeZoom(boolean zoom) {
        ViewportListener oldViewportListener = vpl;
        vpl = createViewportListenerZoom(zoom);
        // add the listener to every active map window
        JInternalFrame[] iframes = workbenchContext.getWorkbench().getFrame()
                .getInternalFrames();
        for (JInternalFrame iframe : iframes) {
            if (iframe instanceof TaskFrame) {
                // Remove the old listener if any before adding a new one
                // Thus, one can reinitialize synchronization if the number
                // of map windows change
                // Ideally, one should intercept new window creation or window
                // focus
                // change to add new listeners to new windows..
                ((TaskFrame) iframe).getLayerViewPanel().getViewport()
                        .removeListener(oldViewportListener);
                ((TaskFrame) iframe).getLayerViewPanel().getViewport()
                        .addListener(vpl);

            }
        }
    }

    public void desynchronize() {
        ViewportListener oldViewPortListener = vpl;
        vpl = null;
        // add the listener to every active map window
        JInternalFrame[] iframes = workbenchContext.getWorkbench().getFrame()
                .getInternalFrames();
        for (JInternalFrame iframe : iframes) {
            if (iframe instanceof TaskFrame) {
                // Remove the old listener if any before adding a new one
                // Thus, one can reinitialize synchronization if the number
                // of map windows change
                // Ideally, one should intercept new window creation or window
                // focus
                // change to add new listeners to new windows..
                ((TaskFrame) iframe).getLayerViewPanel().getViewport()
                        .removeListener(oldViewPortListener);
            }
        }
    }

    private ViewportListener createViewportListener(final boolean zoom) {
        final WorkbenchContext context = workbenchContext;
        return new ViewportListener() {
            @Override
            public void zoomChanged(Envelope modelEnvelope) {
                JInternalFrame[] iframes = context.getWorkbench().getFrame()
                        .getInternalFrames();
                for (JInternalFrame iframe : iframes) {
                    if (iframe instanceof TaskFrame
                            && ((TaskFrame) iframe).getLayerViewPanel()
                                    .getViewport() != context
                                    .getLayerViewPanel().getViewport()) {
                        try {
                            // Copy of method viewport.zoom(envelope)
                            // without the statement
                            // fireZoomChanged(modelEnvelope)
                            // to avoid entering an infinite loop
                            // window 1 change --> window 2 change --> window 1
                            // change ...
                            Viewport viewport = ((TaskFrame) iframe)
                                    .getLayerViewPanel().getViewport();
                            double w = viewport.getPanel().getWidth();
                            double h = viewport.getPanel().getHeight();
                            double scale = viewport.getScale();
                            if (zoom) {
                                scale = Math.min(w / modelEnvelope.getWidth(),
                                        h / modelEnvelope.getHeight());
                            }
                            double xCenteringOffset = ((w / scale) - modelEnvelope
                                    .getWidth()) / 2.0;
                            double yCenteringOffset = ((h / scale) - modelEnvelope
                                    .getHeight()) / 2.0;
                            Point2D.Double viewOriginAsPerceivedByModel = new Point2D.Double(
                                    modelEnvelope.getMinX() - xCenteringOffset,
                                    modelEnvelope.getMinY() - yCenteringOffset);
                            viewport.initialize(scale,
                                    viewOriginAsPerceivedByModel);
                            viewport.update();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        };
    }

    private ViewportListener createViewportListenerZoom(final boolean panAndZoom) {
        final WorkbenchContext context = workbenchContext;
        return new ViewportListener() {
            @Override
            public void zoomChanged(Envelope modelEnvelope) {
                JInternalFrame[] iframes = context.getWorkbench().getFrame()
                        .getInternalFrames();
                for (JInternalFrame iframe : iframes) {
                    if (iframe instanceof TaskFrame
                            && ((TaskFrame) iframe).getLayerViewPanel()
                                    .getViewport() != context
                                    .getLayerViewPanel().getViewport()) {
                        try {
                            // Copy of method viewport.zoom(envelope)
                            // without the statement
                            // fireZoomChanged(modelEnvelope)
                            // to avoid entering an infinite loop
                            // window 1 change --> window 2 change --> window 1
                            // change ...
                            Viewport viewport = ((TaskFrame) iframe)
                                    .getLayerViewPanel().getViewport();
                            double w = viewport.getPanel().getWidth();
                            double h = viewport.getPanel().getHeight();
                            double scale = viewport.getScale();

                            if (panAndZoom) {
                                scale = Math.min(w / modelEnvelope.getWidth(),
                                        h / modelEnvelope.getHeight());
                            }

                            double xCenteringOffset = ((w / scale) - modelEnvelope
                                    .getWidth()) / 2.0;
                            double yCenteringOffset = ((h / scale) - modelEnvelope
                                    .getHeight()) / 2.0;
                            Point2D.Double viewOriginAsPerceivedByModel = new Point2D.Double(
                                    modelEnvelope.getMinX() - xCenteringOffset,
                                    modelEnvelope.getMinY() - yCenteringOffset);
                            viewport.initialize(scale,
                                    viewport.getOriginInModelCoordinates());
                            viewport.update();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        };
    }

    /**
     * For this plugin, this method is unused All the work is defined in action
     * listeners
     */
    @Override
    public boolean execute(PlugInContext context) throws Exception {
        return true;
    }

}