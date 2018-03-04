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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.algorithm.PointLocator;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Fmt;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.TextFrame;

public class VerticesInFencePlugIn extends AbstractPlugIn {

  private WKTWriter wktWriter = new WKTWriter(3);
  private GeometryFactory factory = new GeometryFactory();

  public VerticesInFencePlugIn() {
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    TextFrame textFrame = new TextFrame(context.getWorkbenchFrame());
    textFrame.setTitle(I18N
        .get("ui.plugin.VerticesInFencePlugIn.vertices-in-fence"));
    textFrame.clear();
    textFrame.setText(description(context));
    textFrame.setSize(550, 300);
    context.getWorkbenchFrame().addInternalFrame(textFrame);
    return true;
  }

  public MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck()).add(
        checkFactory.createFenceMustBeDrawnCheck());
  }

  private String description(PlugInContext context) {
    FenceLayerFinder fenceLayerFinder = new FenceLayerFinder(context);
    StringBuffer description = new StringBuffer();
    description.append("<html><body>");
    for (Iterator i = context.getLayerManager().iterator(); i.hasNext();) {
      Layer layer = (Layer) i.next();
      if (!layer.isVisible()) {
        continue;
      }
      if (layer == fenceLayerFinder.getLayer()) {
        continue;
      }
      description.append(description(layer, context));
    }
    description.append("</body></html>");
    return description.toString();
  }

  public static Collection verticesInFence(Collection geometries,
      Geometry fence, boolean skipClosingVertex) {
    ArrayList verticesInFence = new ArrayList();
    for (Iterator i = geometries.iterator(); i.hasNext();) {
      Geometry geometry = (Geometry) i.next();
      verticesInFence
          .addAll(verticesInFence(geometry, fence, skipClosingVertex)
              .getCoordinates());
    }
    return verticesInFence;
  }

  /**
   * @param skipClosingVertex
   *          whether to ignore the duplicate point that closes off a LinearRing
   *          or Polygon
   */
  public static VerticesInFence verticesInFence(Geometry geometry,
      final Geometry fence, final boolean skipClosingVertex) {
    final ArrayList coordinates = new ArrayList();
    final ArrayList indices = new ArrayList();
    // PointLocator is non-re-entrant. Therefore, create a new instance for each
    // fence.
    // [Jon Aquino]
    final PointLocator pointLocator = new PointLocator();
    final IntWrapper index = new IntWrapper(-1);
    geometry.apply(new GeometryComponentFilter() {
      public void filter(Geometry geometry) {
        if (geometry instanceof GeometryCollection
            || geometry instanceof Polygon) {
          // Wait for the elements to be passed into the filter [Jon Aquino]
          return;
        }
        Coordinate[] component = geometry.getCoordinates();
        for (int j = 0; j < component.length; j++) {
          index.value++;
          if (skipClosingVertex && (component.length > 1)
              && (j == (component.length - 1))
              && component[j].equals(component[0])) {
            continue;
          }
          if (pointLocator.locate(component[j], fence) == Location.EXTERIOR) {
            continue;
          }
          coordinates.add(component[j]);
          indices.add(new Integer(index.value));
        }
      }
    });
    return new VerticesInFence() {
      public List getCoordinates() {
        return coordinates;
      }

      public int getIndex(int i) {
        return ((Integer) indices.get(i)).intValue();
      }
    };
  }

  /**
   * @return an empty String if the layer has no coordinates in the fence
   */
  private String description(Layer layer, PlugInContext context) {
    boolean foundVertices = false;
    String description = "<Table width=100%><tr><td colspan=2 valign=top><i>"
        + I18N.get("ui.plugin.VerticesInFencePlugIn.layer")
        + " </i><font color='#3300cc'><b>" + layer.getName()
        + "</b></font></td></tr>";
    String bgcolor = "darkgrey";
    for (Iterator i = layer.getFeatureCollectionWrapper()
        .query(context.getLayerViewPanel().getFence().getEnvelopeInternal())
        .iterator(); i.hasNext();) {
      Feature feature = (Feature) i.next();
      VerticesInFence verticesInFence = verticesInFence(feature.getGeometry(),
          context.getLayerViewPanel().getFence(), true);
      if (verticesInFence.getCoordinates().isEmpty()) {
        continue;
      }
      if (bgcolor.equals("#faebd7")) {
        bgcolor = "darkgrey";
      } else {
        bgcolor = "#faebd7";
      }
      foundVertices = true;
      // <<TODO:DEFECT>> Get platform-specific newline rather than "\n" [Jon
      // Aquino]
      description += ("<tr bgcolor=" + bgcolor
          + "><td width=10% valign=top><font size='-1'><i>"
          + I18N.get("ui.plugin.VerticesInFencePlugIn.feature-id")
          + " </i></font><font size='-1' color='#3300cc'><b>" + feature.getID() + "</b></font><td>");
      description += description(verticesInFence, feature.getGeometry());
      description += "</td></tr>";
    }
    description += "</table>";
    return foundVertices ? description : "";
  }

  private WKTDisplayHelper helper = new WKTDisplayHelper();

  private String description(VerticesInFence verticesInFence, Geometry geometry) {
    StringBuffer description = new StringBuffer();
    // <<TODO:FEATURE>> Perhaps we should change these \n's to the line
    // separators
    // specific to the current platform. Then the user could copy the text and
    // paste it to any editor without any funny symbols potentially appearing.
    // [Jon Aquino]
    description.append("<pre>");
    for (int i = 0; i < verticesInFence.getCoordinates().size(); i++) {
      description.append(GUIUtil.escapeHTML(
          "["
              + Fmt.fmt(helper.annotation(geometry,
                  (Coordinate) verticesInFence.getCoordinates().get(i)), 10)
              + "] "
              + wktWriter.write(factory
                  .createPoint((Coordinate) verticesInFence.getCoordinates()
                      .get(i))) + "\n", false, false));
    }
    description.append("</pre>");
    return description.toString();
  }

  public static interface VerticesInFence {
    public List getCoordinates();

    public int getIndex(int i);
  }

  private static class IntWrapper {
    public int value;

    public IntWrapper(int value) {
      this.value = value;
    }
  }

  public static void main(String[] args) {
    new WKTWriter();
    ((Geometry) (new Object())).toString();
  }
}
