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
package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;

public class FillPolygonTool extends NClickTool {

  public static final String AREA_NOT_CLOSED = I18N
      .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.clicked-area-is-not-closed");
  public static final String EXTEND_SEARCH = I18N
      .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.do-you-want-to-extend-search-out-of-the-view");
  public static final String INTERRUPTION = I18N
      .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.interrupted-operation");
  public static final String COMPUTING = I18N
      .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.computing");

  private WorkbenchContext context;
  private static long START = 0;
  private static long END = 0;
  private static boolean INTERRUPTED = false;
  final OKCancelDialog okCancelDialog;
  final JDialog progressDialog;

  public FillPolygonTool(WorkbenchContext context) {
    super(1);
    this.context = context;
    okCancelDialog = new OKCancelDialog(
          context.getWorkbench().getFrame(),
          I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool"),
          true, getOKCancelPanel(), null);
    progressDialog = new JDialog(context.getWorkbench().getFrame(), COMPUTING + "...", true);
    initProgressDialog();
  }
  
  private JPanel getOKCancelPanel(){
    JPanel panel = new JPanel(new GridLayout(2, 1));
    panel.add(new JLabel(AREA_NOT_CLOSED));
    panel.add(new JLabel(EXTEND_SEARCH));
    return panel;
  }
  
  private void initProgressDialog() {
    JProgressBar jpb = new JProgressBar();
    jpb.setIndeterminate(true);
    progressDialog.add(jpb);
    progressDialog.pack();
    progressDialog.setLocationRelativeTo(context.getWorkbench().getFrame());
    // check if progressBar has terminated normally or has been interrupted
    progressDialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        try {
          if (START > END) {
            INTERRUPTED = true;
          }
          else {
            INTERRUPTED = false;
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        } finally {
          INTERRUPTED = false;
        }
      }
    });
  }

  protected Shape getShape() throws NoninvertibleTransformException {
    // Don't want anything to show up when the user drags. [Jon Aquino]
    return null;
  }

  public Icon getIcon() {
    return new ImageIcon(getClass().getResource("FillPolygon.gif"));
  }

  public String getName() {
    return I18N
        .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool");
  }

  protected void gestureFinished() throws Exception {
    reportNothingToUndoYet();
    FeatureDrawingUtil featureDrawingUtil = new FeatureDrawingUtil(
              (LayerNamePanelProxy) context.getLayerNamePanel());
    // What is the logic for this test ?
    // If the user click several times to close the progress bar and
    // interrupt the process, the first click will close the dialog box,
    // but the next one will be queued in the EDT to start a new FillPolygon
    // process. This is not what we want.
    // The only way I found to cancel this second click is to check it does
    // not activate gestureFinished just after the processing thread ended.
    // If gestureFinished is called less than 1 s after the previous process
    // end, I suppose the click happened during the process and just
    // awaiting turn.
    if (new Date().getTime() < END + 500) {
      return;
    }
    
    Polygon polygon = getPolygon(true);
    if (INTERRUPTED) {
      context.getWorkbench().getFrame().warnUser(INTERRUPTION);
    }
    else if (polygon!=null && !polygon.isEmpty()) {
      UndoableCommand command = featureDrawingUtil.createAddCommand(polygon,
          isRollingBackInvalidEdits(), getPanel(), this);
      if (command != null) {
        execute(command);
      }
    }
    else {
      GUIUtil.centreOnWindow(okCancelDialog);
      okCancelDialog.setVisible(true);
      if (okCancelDialog.wasOKPressed()) {
        polygon = getPolygon(false);
        if (INTERRUPTED) {
          context.getWorkbench().getFrame().warnUser(INTERRUPTION);
        } else if (!polygon.isEmpty()) {
          execute(featureDrawingUtil.createAddCommand(polygon,
              isRollingBackInvalidEdits(), getPanel(), this));
        } else {
          context.getWorkbench().getFrame().warnUser(AREA_NOT_CLOSED);
        }
      } else {
        context.getWorkbench().getFrame().warnUser(INTERRUPTION);
      }
    }
    INTERRUPTED = false;
  }

  protected Polygon getPolygon(final boolean inViewportOnly)
      throws NoninvertibleTransformException {

    final Collection polys = new java.util.ArrayList(); 
    INTERRUPTED = false;

    final Thread t = new Thread() {
      public void run() {
        START = new Date().getTime();
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(getVisibleGeometries(inViewportOnly));
        polys.addAll(polygonizer.getPolygons());
        END = new Date().getTime();
        // don't know why I need to dispose here not only setVisible(false)
        if (progressDialog.isShowing()) progressDialog.dispose();
      }
    };
    t.start();
    progressDialog.setVisible(true);
    GUIUtil.centreOnWindow(progressDialog);

    try {
      t.join();
      if (!INTERRUPTED) {
        Coordinate c = (Coordinate) getCoordinates().get(0);
        Point p = new GeometryFactory().createPoint(c);
        for (Object poly : polys) {
          if (((Polygon) poly).intersects(p)) {
            return (Polygon) poly;
          }
        }
        GeometryFactory gf = new GeometryFactory();
        return gf.createPolygon(gf.createLinearRing(new Coordinate[0]), null);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      INTERRUPTED = false;
    }

    return null;
  }
  

  private Set<Geometry> getVisibleGeometries(boolean inViewportOnly) {
    List layers = context.getLayerManager().getVisibleLayers(false);
    Envelope env = null;
    Set<Geometry> list = new HashSet<Geometry>();
    if (inViewportOnly) {
      env = context.getLayerViewPanel().getViewport()
          .getEnvelopeInModelCoordinates();
    } else {
      env = new Envelope();
      for (Object layer : layers) {
        env.expandToInclude(((Layer) layer).getFeatureCollectionWrapper()
            .getEnvelope());
      }
    }
    for (Object layer : layers) {
      Collection features = ((Layer) layer).getFeatureCollectionWrapper()
          .getFeatures();
      for (Object f : features) {
        Geometry geom = ((Feature) f).getGeometry();
        if (geom.getEnvelopeInternal().intersects(env)
            && geom.getDimension() > 0) {
          extractLinearComponents(geom, list);
        }
      }
    }
    return list;
  }

  private void extractLinearComponents(Geometry geom,
      Set<Geometry> linearComponents) {
    for (int i = 0; i < geom.getNumGeometries(); i++) {
      Geometry g = geom.getGeometryN(i);
      if (g instanceof Polygon) {
        extractLinearComponents((Polygon) g, linearComponents);
      } else if (g instanceof LineString) {
        extractLinearComponents((LineString) g, linearComponents);
      } else if (g instanceof Point) {
      } else
        extractLinearComponents(g, linearComponents);
    }
  }

  private void extractLinearComponents(Polygon poly,
      Set<Geometry> linearComponents) {
    extractLinearComponents((LineString) poly.getExteriorRing(),
        linearComponents);
    for (int i = 0; i < poly.getNumInteriorRing(); i++) {
      extractLinearComponents((LineString) poly.getInteriorRingN(i),
          linearComponents);
    }
  }

  private void extractLinearComponents(LineString line,
      Set<Geometry> linearComponents) {
    Coordinate[] cc = line.getCoordinates();
    for (int i = 1; i < cc.length; i++) {
      LineString ls = line.getFactory().createLineString(
          new Coordinate[] { new Coordinate(cc[i - 1]), new Coordinate(cc[i]) });
      ls.normalize();
      linearComponents.add(ls);
    }
  }

}
