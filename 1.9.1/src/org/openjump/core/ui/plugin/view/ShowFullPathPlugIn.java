/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */

package org.openjump.core.ui.plugin.view;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelListener;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelListener;
import com.vividsolutions.jump.workbench.ui.TaskFrame;

public class ShowFullPathPlugIn extends AbstractPlugIn {

  PlugInContext gContext;
  final static String sErrorSeeOutputWindow = I18N
      .get("org.openjump.core.ui.plugin.view.ShowFullPathPlugIn.Error-See-Output-Window");
  final static String sNumberSelected = I18N
      .get("org.openjump.core.ui.plugin.view.ShowFullPathPlugIn.NumberSelected");

  private LayerNamePanelListener layerNamePanelListener = new LayerNamePanelListener() {
    public void layerSelectionChanged() {
      Collection layerCollection = (Collection) gContext.getWorkbenchContext()
          .getLayerNamePanel().getLayerManager().getLayers();
      for (Iterator i = layerCollection.iterator(); i.hasNext();) {
        Layer layer = (Layer) i.next();
        if (layer.hasReadableDataSource()) {
          DataSourceQuery dsq = layer.getDataSourceQuery();
          String fname = "";
          Object fnameObj = dsq.getDataSource().getProperties().get("File");
          if (fnameObj != null)
            fname = fnameObj.toString();
          // layer.setDescription(fname);

          Object archiveObj = layer.getBlackboard().get("ArchiveFileName");
          // if (archiveObj != null)
          // layer.setDescription(archiveObj.toString());
        }
      }
    }
  };

  private LayerViewPanelListener layerViewPanelListener = new LayerViewPanelListener() {
    public void selectionChanged() {
      LayerViewPanel panel = gContext.getWorkbenchContext().getLayerViewPanel();
      if (panel == null) {
        return;
      } // [Jon Aquino 2005-08-04]
      Collection selectedFeatures = panel.getSelectionManager()
          .getSelectedItems();
      int numSel = selectedFeatures.size();
      int numPts = 0;
      for (Iterator i = selectedFeatures.iterator(); i.hasNext();)
        numPts += ((Geometry) i.next()).getNumPoints();

      // LDB added the following to simulate 4D Draw Coordinates Panel
      Envelope env = envelope(panel.getSelectionManager().getSelectedItems());
      String sx = panel.format(env.getWidth());
      String sy = panel.format(env.getHeight());
      // gContext.getWorkbenchFrame().setTimeMessage(sNumberSelected + " " +
      // numSel);
      gContext.getWorkbenchFrame().setTimeMessage(
          sNumberSelected + " " + numSel + " [" + numPts + " pts]");
    }

    public void cursorPositionChanged(String x, String y) {

    }

    public void painted(Graphics graphics) {

    }
  };

  public void initialize(PlugInContext context) throws Exception {
    gContext = context;

    /**** original *********************************/
    context.getWorkbenchFrame().getDesktopPane()
        .addContainerListener(new ContainerListener() {
          public void componentAdded(ContainerEvent e) {
            Component child = e.getChild();
            if (child.getClass().getName()
                .equals("com.vividsolutions.jump.workbench.ui.TaskFrame")) {
              ((TaskFrame) child).getLayerNamePanel().addListener(
                  layerNamePanelListener);
              ((TaskFrame) child).getLayerViewPanel().addListener(
                  layerViewPanelListener);
            }
          }

          public void componentRemoved(ContainerEvent e) {
            Component child = e.getChild();
            if (child.getClass().getName()
                .equals("com.vividsolutions.jump.workbench.ui.TaskFrame")) {
              ((TaskFrame) child).getLayerNamePanel().removeListener(
                  layerNamePanelListener);
              ((TaskFrame) child).getLayerViewPanel().removeListener(
                  layerViewPanelListener);
            }
          }
        });
  }

  public boolean execute(PlugInContext context) throws Exception {
    try {
      return true;
    }
    catch (Exception e) {
      context
          .getWorkbenchFrame()
          .warnUser(
              I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.Error-See-Output-Window"));
      context.getWorkbenchFrame().getOutputFrame().createNewDocument();
      context.getWorkbenchFrame().getOutputFrame()
          .addText("ShowFullPathPlugIn Exception:" + e.toString());
      return false;
    }
  }

  private Envelope envelope(Collection geometries) {
    Envelope envelope = new Envelope();

    for (Iterator i = geometries.iterator(); i.hasNext();) {
      Geometry geometry = (Geometry) i.next();
      envelope.expandToInclude(geometry.getEnvelopeInternal());
    }

    return envelope;
  }

}
