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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.basic.BasicSliderUI;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CoordinateArrays;
import com.vividsolutions.jump.util.MathUtil;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.ViewportListener;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.IncrementChooser;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.MetricSystem;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.RoundQuantity;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.ScaleBarRenderer;
import com.vividsolutions.jump.workbench.ui.renderer.java2D.Java2DConverter;

public class ZoomBar extends JPanel implements Java2DConverter.PointConverter {

  private int totalGeometries() {
    int totalGeometries = 0;
    // Restrict count to visible layers [mmichaud 2007-05-27]
    for (Layer layer : layerViewPanel().getLayerManager().getVisibleLayers(true)) {
      totalGeometries += layer.getFeatureCollectionWrapper().size();
    }
    return totalGeometries;
  }

  private Envelope lastGoodEnvelope = null;
  private WorkbenchFrame frame;

  private JSlider slider = new JSlider();
  private JLabel label = new JLabel();
  private IncrementChooser incrementChooser = new IncrementChooser();
  private Collection metricUnits = new MetricSystem(1).createUnits();

  // Add java2DConverter and affineTransform for coordinate decimation
  private Java2DConverter java2DConverter;
  private AffineTransform affineTransform;

  public ZoomBar(boolean showingSliderLabels, boolean showingRightSideLabel, WorkbenchFrame frame)
      throws NoninvertibleTransformException {
    this.frame = frame;
    this.showingSliderLabels = showingSliderLabels;
    slider.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        try {
          updateComponents();
        } catch (NoninvertibleTransformException x) {
          // Eat it. [Jon Aquino]
        }
      }
    });
    if (showingSliderLabels) {
      // Add a dummy label so that ZoomBars added to Toolboxes are
      // packed properly. [Jon Aquino]
      Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
      labelTable.put(0, new JLabel(" "));
      slider.setLabelTable(labelTable);
    }
    try {
      jbInit();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    if (!showingRightSideLabel) {
      remove(label);
    }
    label.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 3 && SwingUtilities.isRightMouseButton(e)) {
          viewBlackboard().put(USER_DEFINED_MIN_SCALE, null);
          viewBlackboard().put(USER_DEFINED_MAX_SCALE, null);
          clearModelCaches();
        }
      }
    });
    slider.addMouseMotionListener(new MouseMotionAdapter() {
      // Use #mouseDragged rather than JSlider#stateChanged because we
      // are interested in user-initiated slider changes, not programmatic
      // slider changes. [Jon Aquino]
      public void mouseDragged(MouseEvent e) {
        try {
          layerViewPanel().erase((Graphics2D) layerViewPanel().getGraphics());
          drawWireframe();
          ScaleBarRenderer scaleBarRenderer = (ScaleBarRenderer) layerViewPanel().getRenderingManager()
              .getRenderer(ScaleBarRenderer.CONTENT_ID);
          if (scaleBarRenderer != null) {
            scaleBarRenderer.paint((Graphics2D) layerViewPanel().getGraphics(), getScale());
          }
          updateLabel();
        } catch (NoninvertibleTransformException x) {
          // Eat it. [Jon Aquino]
        }
      }
    });
    if (slider.getUI() instanceof BasicSliderUI) {
      slider.addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseMoved(MouseEvent e) {
          if (layerViewPanel() == dummyLayerViewPanel) {
            return;
          }
          // try {
          slider.setToolTipText(I18N.get("ui.zoom.ZoomBar.zoom-to") + " "
              + chooseGoodIncrement(toScale(((BasicSliderUI) slider.getUI()).valueForXPosition(e.getX()))).toString());
          // } catch (NoninvertibleTransformException x) {
          // slider.setToolTipText(I18N.get("ui.zoom.ZoomBar.zoom"));
          // }
        }
      });
    }
    // label.setPreferredSize(new Dimension(50, label.getHeight()));
    slider.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        try {
          if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            gestureFinished();
          }
        } catch (NoninvertibleTransformException t) {
          layerViewPanel().getContext().handleThrowable(t);
        }
      }
    });
    slider.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (!slider.isEnabled()) {
          return;
        }
        layerViewPanel().getRenderingManager().setPaintingEnabled(false);
      }

      public void mouseReleased(MouseEvent e) {
        try {
          gestureFinished();
        } catch (NoninvertibleTransformException t) {
          layerViewPanel().getContext().handleThrowable(t);
        }
      }
    });

    //
    // Whenever anything happens on an internal frame we want to do this.
    //
    GUIUtil.addInternalFrameListener(frame.getDesktopPane(), GUIUtil.toInternalFrameListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        installListenersOnCurrentPanel();
        try {
          updateComponents();
        } catch (NoninvertibleTransformException x) {
          // Eat it. [Jon Aquino]
        }
      }
    }));

    // added to use the decimator implemented in Java2DConverter [mmichaud
    // 2007-05-27]
    java2DConverter = new Java2DConverter(this, 2);

    installListenersOnCurrentPanel();
    updateComponents();
  }

  private void installListenersOnCurrentPanel() {
    installViewListeners();
    installModelListeners();
  }

  private void installViewListeners() {

    // Use hash code to uniquely identify this zoom bar (there may be other
    // zoom bars) [Jon Aquino]
    String VIEW_LISTENERS_INSTALLED_KEY = Integer.toHexString(hashCode()) + " - VIEW LISTENERS INSTALLED";
    if (viewBlackboard().get(VIEW_LISTENERS_INSTALLED_KEY) != null) {
      return;
    }
    if (layerViewPanel() == null) {
      return;
    }
    layerViewPanel().getViewport().addListener(new ViewportListener() {
      public void zoomChanged(Envelope modelEnvelope) {
        if (!viewBlackboard().get(CENTRE_LOCKED_KEY, false)) {
          viewBlackboard().put(CENTRE_KEY, null);
        }
        viewBlackboard().put(SCALE_KEY, null);
        try {
          if (layerViewPanel().getViewport().getScale() < getMinScale()) {
            viewBlackboard().put(USER_DEFINED_MIN_SCALE, layerViewPanel().getViewport().getScale());
          }
          if (layerViewPanel().getViewport().getScale() > getMaxScale()) {
            viewBlackboard().put(USER_DEFINED_MAX_SCALE, layerViewPanel().getViewport().getScale());
          }
          updateComponents();
        } catch (NoninvertibleTransformException e) {
          // Eat it. [Jon Aquino]
        }
      }
    });
    viewBlackboard().put(VIEW_LISTENERS_INSTALLED_KEY, new Object());
  }

  private void installModelListeners() {

    // Use hash code to uniquely identify this zoom bar (there may be other
    // zoom bars) [Jon Aquino]
    String MODEL_LISTENERS_INSTALLED_KEY = Integer.toHexString(hashCode()) + " - MODEL LISTENERS INSTALLED";
    if (viewBlackboard().get(MODEL_LISTENERS_INSTALLED_KEY) != null) {
      return;
    }
    if (layerViewPanel() == null) {
      return;
    }

    layerViewPanel().getLayerManager().addLayerListener(new LayerListener() {
      public void categoryChanged(CategoryEvent e) {
      }

      public void featuresChanged(FeatureEvent e) {
        if (e.getType() == FeatureEventType.ADDED || e.getType() == FeatureEventType.DELETED
            || e.getType() == FeatureEventType.GEOMETRY_MODIFIED) {
          clearModelCaches();
        }
      }

      // add LayerEventType.VISIBILITY_CHANGED condition [mmichaud 2007-05-27]
      public void layerChanged(LayerEvent e) {
        if (e.getType() == LayerEventType.ADDED || e.getType() == LayerEventType.REMOVED
            || e.getType() == LayerEventType.VISIBILITY_CHANGED) {
          clearModelCaches();
        }
      }
    });
    viewBlackboard().put(MODEL_LISTENERS_INSTALLED_KEY, new Object());
  }

  private void queueComponentUpdate() {
    componentUpdateTimer.restart();
  }

  /** Coalesces component updates */
  private Timer componentUpdateTimer = GUIUtil.createRestartableSingleEventTimer(200, new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      try {
        updateComponents();
      } catch (NoninvertibleTransformException x) {
        // Eat it. [Jon Aquino]
      }
    }
  });

  public void updateComponents() throws NoninvertibleTransformException {
    LayerViewPanel layerViewPanel = layerViewPanel();
    if (layerViewPanel == dummyLayerViewPanel || layerViewPanel == null) {
      setComponentsEnabled(false);
      return;
    }
    setComponentsEnabled(true);
    // Must set slider value *before* updating the label on the right. [Jon Aquino]
    // I'm currently hiding the label on the right, to save real estate. [Jon
    // Aquino]
    slider.setValue(toSliderValue(viewBlackboard().get(SCALE_KEY, layerViewPanel.getViewport().getScale())));
    updateLabel();
    updateSliderLabels();
  }

  private void gestureFinished() throws NoninvertibleTransformException {
    if (!slider.isEnabled()) {
      return;
    }
    try {
      viewBlackboard().put(CENTRE_LOCKED_KEY, true);
      try {
        layerViewPanel().getViewport().zoom(proposedModelEnvelope());
      } finally {
        viewBlackboard().put(CENTRE_LOCKED_KEY, false);
      }
    } finally {
      layerViewPanel().getRenderingManager().setPaintingEnabled(true);
    }
  }

  private Envelope proposedModelEnvelope() throws NoninvertibleTransformException {
    Coordinate centre = (Coordinate) viewBlackboard().get(CENTRE_KEY,
        EnvelopeUtil.centre(layerViewPanel().getViewport().getEnvelopeInModelCoordinates()));
    double width = layerViewPanel().getWidth() / getScale();
    double height = layerViewPanel().getHeight() / getScale();
    Envelope proposedModelEnvelope = new Envelope(centre.x - (width / 2), centre.x + (width / 2),
        centre.y - (height / 2), centre.y + (height / 2));
    if (proposedModelEnvelope.getWidth() == 0 || proposedModelEnvelope.getHeight() == 0) {
      // We're zoomed waaay out! Avoid infinite scale. [Jon Aquino]
      proposedModelEnvelope = lastGoodEnvelope;
    } else {
      lastGoodEnvelope = proposedModelEnvelope;
    }
    return proposedModelEnvelope;
  }

  /**
   * Return the scale of the view according to the zoom bar
   */
  // getScale() public to implement Java2DConverter.PointConverter [mmichaud
  // 2007-05-26]
  public double getScale() throws NoninvertibleTransformException {
    return toScale(slider.getValue());
  }

  private Stroke stroke = new BasicStroke(1);

  private void drawWireframe() throws NoninvertibleTransformException {
    Graphics2D g = (Graphics2D) layerViewPanel().getGraphics();
    g.setColor(Color.lightGray);
    g.setStroke(stroke);
    g.draw(getWireFrame());
  }

  private static final String SEGMENT_CACHE_KEY = ZoomBar.class.getName() + " - SEGMENT CACHE";

  private void clearModelCaches() {
    // Use LayerManager blackboard for segment cache, so that multiple
    // views can share it. [Jon Aquino]
    modelBlackboard().put(SEGMENT_CACHE_KEY, null);
    modelBlackboard().put(MIN_EXTENT_KEY, null);
    modelBlackboard().put(MAX_EXTENT_KEY, null);
    // It's expensive to recompute these cached values, so queue the call
    // to #updateComponents [Jon Aquino]
    queueComponentUpdate();
  }

  // private LineSegmentEnvelopeIntersector lineSegmentEnvelopeIntersector =
  // new LineSegmentEnvelopeIntersector();

  // Modified by [mmichaud 2007-05-26] to use decimation algorithm from
  // Java2DConverter
  private Shape getWireFrame() throws NoninvertibleTransformException {
    // affineTransform computed according to the zoombar slider (getScale)
    affineTransform = Viewport.modelToViewTransform(getScale(),
        new Point2D.Double(proposedModelEnvelope().getMinX(), proposedModelEnvelope().getMinY()),
        layerViewPanel().getSize().getHeight());
    // view2D rectangle
    Rectangle2D view2D = new Rectangle2D.Double(0.0, 0.0, layerViewPanel().getWidth(), layerViewPanel().getWidth());
    GeneralPath wireFrame = new GeneralPath();
    List<Coordinate[]> segments = new ArrayList<>(getSegmentCache());
    segments.addAll(toSegments(largeOnScreenGeometries()));
    for (Coordinate[] coords : segments) {
      Coordinate[] coordinates = java2DConverter.toViewCoordinates(coords);
      boolean drawing = false;

      for (int j = 1; j < coordinates.length; j++) {
        if (!view2D.intersectsLine(coordinates[j - 1].x, coordinates[j - 1].y, coordinates[j].x, coordinates[j].y)) {
          drawing = false;
          continue;
        }
        if (!drawing) {
          wireFrame.moveTo((float) coordinates[j - 1].x, (float) coordinates[j - 1].y);
        }
        wireFrame.lineTo((float) coordinates[j].x, (float) coordinates[j].y);
        drawing = true;
      }

    }
    return wireFrame;

  }

  private Collection<Coordinate[]> getSegmentCache() {
    // Use LayerManager blackboard for segment cache, so that multiple
    // views can share it. [Jon Aquino]
    if (modelBlackboard().get(SEGMENT_CACHE_KEY) == null) {
      modelBlackboard().put(SEGMENT_CACHE_KEY, toSegments(largeGeometries(LARGE_GEOMETRIES)));
      //
      // We only want to do this when a frame closes. If we don't clear the
      // cache in the blackboard then we'll get a memory leak.
      //
      frame.getActiveInternalFrame().addInternalFrameListener(new InternalFrameAdapter() {
        public void internalFrameClosing(InternalFrameEvent e) {
          modelBlackboard().put(SEGMENT_CACHE_KEY, null);
        }
      });
    }
    return (Collection<Coordinate[]>) modelBlackboard().get(SEGMENT_CACHE_KEY);
  }

  private Collection<Coordinate[]> toSegments(Collection<Geometry> geometries) {
    List<Coordinate[]> segments = new ArrayList<>();
    for (Geometry geometry : geometries) {
      segments.addAll(CoordinateArrays.toCoordinateArrays(geometry, false));
    }
    return segments;
  }

  // Replace RANDOM_ONSCREEN_GEOMETRIES by LARGE_ONSCREEN_GEOMETRIES
  // private static final int RANDOM_ONSCREEN_GEOMETRIES = 100;
  // private static final int RANDOM_GEOMETRIES = 100;
  private static final int LARGE_GEOMETRIES = 100;
  private static final int LARGE_ONSCREEN_GEOMETRIES = 200;

  // start [mmichaud]
  // additional code to replace randomGeometries by a largestGeometries approach
  // one bad side effect of random geometries approach was visible for a big set
  // of points
  // and a small set of polygons : polygons were not visible because of the
  // proportional
  // selection of geometries, and points are not displayed :-(

  // static final Comparator MAX_SIZE_COMPARATOR = new Comparator() {
  // public int compare(Object f1, Object f2) {
  // Envelope env1 = ((Feature)f1).getGeometry().getEnvelopeInternal();
  // Envelope env2 = ((Feature)f2).getGeometry().getEnvelopeInternal();
  // double size1 = Math.max(env1.getWidth(), env1.getHeight());
  // double size2 = Math.max(env2.getWidth(), env2.getHeight());
  // return size1 < size2 ? 1 : (size1 > size2 ? -1 : 0);
  // }
  // };

  // Collect large geometries in a single pass (focus on efficiency rather than
  // precision)
  // Explore maxSize * 10 geometries (not all geometries)
  // up to maxSize/2, select geometries with areas > mean previously selected area
  // after maxSize/2, select geometries with areas > max previously selected areas
  private Collection<Geometry> largeGeometries(int maxSize, List<Feature> features) {
    // Collections.sort(features, MAX_SIZE_COMPARATOR);
    int step = features.size() / maxSize / 10;
    double totalArea = 0;
    double meanArea = 0;
    double maxArea = 0;
    int countTotal = 0;
    int countExplored = 0;
    List<Geometry> largeGeometries = new ArrayList<>();
    for (Feature feature : features) {
      if (countTotal % Math.max(step, 1) == 0) {
        boolean firstHalf = largeGeometries.size() < maxSize / 2;
        Geometry geom = feature.getGeometry();
        countExplored++;
        double area = geom.getArea();
        totalArea += area;
        if ((firstHalf && area >= meanArea) || area > maxArea) {
          largeGeometries.add(geom);
        }
        meanArea = totalArea / countExplored;
        if (area > maxArea)
          maxArea = area;
        countTotal++;
      }
      if (largeGeometries.size() >= maxSize)
        break;
    }
    return largeGeometries;
  }

  private Collection<Geometry> largeOnScreenGeometries() {
    List<Feature> onScreenFeatures = new ArrayList<>();
    if (totalGeometries() == 0) {
      return new ArrayList<>();
    }
    // Use proposedModelEnvelope (dynamically computed while the mouse is dragged)
    // instead of layerViewPanel().getViewport().getEnvelopeInModelCoordinates()
    // [mmichaud 2007-05-27]
    Envelope modelEnvelope;
    try {
      modelEnvelope = proposedModelEnvelope();
    } catch (NoninvertibleTransformException e) {
      modelEnvelope = layerViewPanel().getViewport().getEnvelopeInModelCoordinates();
    } // Restrict to visible layers [mmichaud 2007-05-27]
    for (Layer layer : layerViewPanel().getLayerManager().getVisibleLayers(true)) {
      // Select features intersecting the window
      List<Feature> visibleFeatures = layer.getFeatureCollectionWrapper().query(modelEnvelope);
      if (visibleFeatures.size() < LARGE_ONSCREEN_GEOMETRIES) {
        // onScreenFeatures.addAll(layer.getFeatureCollectionWrapper().query(modelEnvelope));
        onScreenFeatures.addAll(visibleFeatures);
      }
      // If there are more than 1000 visible features in this layer
      // select a maximum of 2000 features stepping through the list
      else {
        int step = visibleFeatures.size() / LARGE_ONSCREEN_GEOMETRIES;
        for (int i = 0, max = visibleFeatures.size(); i < max; i += step) {
          onScreenFeatures.add(visibleFeatures.get(i));
        }
      }
    }
    return largeGeometries(LARGE_ONSCREEN_GEOMETRIES, onScreenFeatures);
  }

  // private Collection largeGeometries() {
  // ArrayList<Feature> largeGeometries = new ArrayList<>();
  // if (totalGeometries() == 0) {
  // return largeGeometries;
  // }
  // //Envelope modelEnvelope =
  // layerViewPanel().getViewport().getEnvelopeInModelCoordinates();
  // for (Layer layer : layerViewPanel().getLayerManager().getVisibleLayers(true))
  // {
  // //List visibleFeatures =
  // layer.getFeatureCollectionWrapper().query(modelEnvelope);
  // largeGeometries.addAll(layer.getFeatureCollectionWrapper().getFeatures());
  // }
  // return largeGeometries(LARGE_GEOMETRIES, largeGeometries);
  // }
  // end [mmichaud]

  // Get at most maxSize geometries
  private Collection<Geometry> largeGeometries(int maxSize) {
    int totalGeometries = totalGeometries();
    List<Geometry> largeGeometries = new ArrayList<>();
    if (totalGeometries == 0)
      return largeGeometries;
    int step = totalGeometries / maxSize / 10;
    double totalArea = 0;
    double meanArea = 0;
    double maxArea = 0;
    int countTotal = 0;
    int countExplored = 0;
    for (Layer layer : layerViewPanel().getLayerManager().getVisibleLayers(true)) {
      for (Feature feature : layer.getFeatureCollectionWrapper().getFeatures()) {
        // we explore maxSize * 10 features but we only keep features which are at least
        // as large as the mean area of previous kept features
        if (countTotal % Math.max(1, step) == 0) {
          boolean firstHalf = largeGeometries.size() < maxSize / 2;
          Geometry geom = feature.getGeometry();
          countExplored++;
          double area = geom.getArea();
          totalArea += area;
          if (area > maxArea || (firstHalf && area >= meanArea)) {
            largeGeometries.add(geom);
            totalArea += area;
          }
          meanArea = totalArea / countExplored;
          if (area > maxArea)
            maxArea = area;
        }
        countTotal++;
        if (largeGeometries.size() >= maxSize)
          break;
      }
      if (largeGeometries.size() >= maxSize)
        break;
    }
    return largeGeometries;
  }

  // Replaced Random geometries by large geometries
  /*
   * private Collection randomGeometries(int maxSize, List features) { if
   * (features.size() <= maxSize) { return FeatureUtil.toGeometries(features); }
   * ArrayList randomGeometries = new ArrayList(); for (int j = 0; j < maxSize;
   * j++) { randomGeometries.add( ((Feature) features.get((int) (Math.random() *
   * features.size()))).getGeometry()); } return randomGeometries; }
   * 
   * private Collection randomOnScreenGeometries() { ArrayList
   * randomOnScreenGeometries = new ArrayList(); // Avoid method computation
   * inside the loop [mmichaud] int totalGeometries = totalGeometries(); if
   * (totalGeometries == 0) { return randomOnScreenGeometries; } // Use
   * proposedModelEnvelope (dynamically computed while the mouse is dragged) //
   * instead of layerViewPanel().getViewport().getEnvelopeInModelCoordinates() //
   * [mmichaud 2007-05-27] Envelope modelEnvelope; try { modelEnvelope =
   * proposedModelEnvelope(); } catch(NoninvertibleTransformException e) {
   * modelEnvelope =
   * layerViewPanel().getViewport().getEnvelopeInModelCoordinates(); } // Restrict
   * to visible layers [mmichaud 2007-05-27] for (Iterator i =
   * layerViewPanel().getLayerManager().getVisibleLayers(true).iterator();
   * i.hasNext();) { Layer layer = (Layer) i.next();
   * randomOnScreenGeometries.addAll( randomGeometries( RANDOM_ONSCREEN_GEOMETRIES
   * layer.getFeatureCollectionWrapper().size() / totalGeometries(),
   * layer.getFeatureCollectionWrapper().query(
   * //layerViewPanel().getViewport().getEnvelopeInModelCoordinates())));
   * modelEnvelope))); } return randomOnScreenGeometries; }
   * 
   * private Collection randomGeometries() { ArrayList randomGeometries = new
   * ArrayList(); if (totalGeometries() == 0) { return randomGeometries; } //
   * Restrict to visible layers [mmichaud 2007-05-27] for (Iterator i =
   * layerViewPanel().getLayerManager().getVisibleLayers(true).iterator();
   * i.hasNext();) { Layer layer = (Layer) i.next(); randomGeometries.addAll(
   * randomGeometries( RANDOM_GEOMETRIES
   * layer.getFeatureCollectionWrapper().size() / totalGeometries(),
   * layer.getFeatureCollectionWrapper().getFeatures()));
   * 
   * } return randomGeometries; }
   */

  private int toSliderValue(double scale) throws NoninvertibleTransformException {
    return slider.getMaximum()
        - (int) (slider.getMaximum() * (MathUtil.base10Log(scale) - MathUtil.base10Log(getMinScale()))
            / (MathUtil.base10Log(getMaxScale()) - MathUtil.base10Log(getMinScale())));
  }

  private double getMinExtent() {
    if (modelBlackboard().get(MIN_EXTENT_KEY) == null) {
      double smallSegmentLength = chooseSmallSegmentLength(getSegmentCache());
      // -1 smallSegmentLength means there is no data or the data are all
      // points (i.e. no segments). [Jon Aquino]
      if (smallSegmentLength == -1) {
        return -1;
      }
      modelBlackboard().put(MIN_EXTENT_KEY, smallSegmentLength);
    }
    Assert.isTrue(modelBlackboard().getDouble(MIN_EXTENT_KEY) > 0);
    return modelBlackboard().getDouble(MIN_EXTENT_KEY);
  }

  private double chooseSmallSegmentLength(Collection<Coordinate[]> segmentCache) {
    int segmentsChecked = 0;
    double smallSegmentLength = -1;
    for (Coordinate[] coordinates : segmentCache) {
      for (int j = 1; j < coordinates.length; j++) {
        double segmentLength = coordinates[j].distance(coordinates[j - 1]);
        segmentsChecked++;
        if (segmentLength > 0 && (smallSegmentLength == -1 || segmentLength < smallSegmentLength)) {
          smallSegmentLength = segmentLength;
        }
        if (segmentsChecked > 100) {
          break;
        }
      }
      if (segmentsChecked > 100) {
        break;
      }
    }
    return smallSegmentLength;
  }

  private double getMaxExtent() {
    if (modelBlackboard().get(MAX_EXTENT_KEY) == null) {
      if (getSegmentCache().isEmpty()) {
        return -1;
      }
      modelBlackboard().put(MAX_EXTENT_KEY, layerViewPanel().getLayerManager().getEnvelopeOfAllLayers().getWidth());
    }
    return modelBlackboard().getDouble(MAX_EXTENT_KEY);
  }

  private double getMaxScale() {
    double maxScale = (getMinExtent() == -1 || getMinExtent() == 0) ? 1E3
        : (1000 * layerViewPanel().getWidth() / getMinExtent());
    if (viewBlackboard().get(USER_DEFINED_MAX_SCALE) != null) {
      return Math.max(maxScale, viewBlackboard().getDouble(USER_DEFINED_MAX_SCALE));
    }
    return maxScale;
  }

  private double getMinScale() {
    double minScale = (getMaxExtent() == -1 || getMaxExtent() == 0) ? 1E-3
        : (0.001 * layerViewPanel().getWidth() / getMaxExtent());
    if (viewBlackboard().get(USER_DEFINED_MIN_SCALE) != null) {
      return Math.min(minScale, viewBlackboard().getDouble(USER_DEFINED_MIN_SCALE));
    }
    return minScale;
  }

  private double toScale(int sliderValue) {
    return Math
        .pow(10,
            ((slider.getMaximum() - sliderValue)
                * (MathUtil.base10Log(getMaxScale()) - MathUtil.base10Log(getMinScale())) / slider.getMaximum())
                + MathUtil.base10Log(getMinScale()));
  }

  private void setComponentsEnabled(boolean componentsEnabled) {
    slider.setEnabled(componentsEnabled);
    label.setEnabled(componentsEnabled);
  }

  private static final String SCALE_KEY = ZoomBar.class.getName() + " - SCALE";
  private static final String CENTRE_KEY = ZoomBar.class.getName() + " - CENTRE";
  // Store centre-locked flag on blackboard rather than field because there could
  // be several zoom bars [Jon Aquino]
  private static final String CENTRE_LOCKED_KEY = ZoomBar.class.getName() + " - CENTRE LOCKED";
  private static final String MIN_EXTENT_KEY = ZoomBar.class.getName() + " - MIN EXTENT";
  private static final String USER_DEFINED_MIN_SCALE = ZoomBar.class.getName() + " - USER DEFINED MIN SCALE";
  private static final String USER_DEFINED_MAX_SCALE = ZoomBar.class.getName() + " - USER DEFINED MAX SCALE";
  private static final String MAX_EXTENT_KEY = ZoomBar.class.getName() + " - MAX EXTENT";

  private Blackboard viewBlackboard() {
    return layerViewPanel() != null ? layerViewPanel().getBlackboard() : new Blackboard();
  }

  private Blackboard modelBlackboard() {
    return layerViewPanel().getLayerManager().getBlackboard();
  }

  private final LayerViewPanel dummyLayerViewPanel = new LayerViewPanel(new LayerManager(),
      new LayerViewPanelContext() {

        public void setStatusMessage(String message) {
        }

        public void warnUser(String warning) {
        }

        public void handleThrowable(Throwable t) {
        }

      });

  private LayerViewPanel layerViewPanel() {
    if (!(frame.getActiveInternalFrame() instanceof LayerViewPanelProxy)) {
      return dummyLayerViewPanel;
    }
    return ((LayerViewPanelProxy) frame.getActiveInternalFrame()).getLayerViewPanel();
  }

  void jbInit() {
    this.setLayout(new BorderLayout());
    label.setText(" ");
    slider.setPaintLabels(true);
    slider.setToolTipText(I18N.get("ui.zoom.ZoomBar.zoom"));
    slider.setMaximum(1000);

    this.add(slider, BorderLayout.CENTER);
    this.add(label, BorderLayout.EAST);
  }

  private void updateLabel() throws NoninvertibleTransformException {
    // Inexpensive. [Jon Aquino]
    label.setText(chooseGoodIncrement(getScale()).toString());
  }

  private RoundQuantity chooseGoodIncrement(double scale) {
    return incrementChooser.chooseGoodIncrement(metricUnits, layerViewPanel().getWidth() / scale);
  }

  private Font sliderLabelFont = new Font("Dialog", Font.PLAIN, 10);

  private boolean showingSliderLabels;

  private void updateSliderLabels() throws NoninvertibleTransformException {
    // Expensive if the data cache has been cleared. [Jon Aquino]
    if (!showingSliderLabels) {
      return;
    }
    if (!(slider.getUI() instanceof BasicSliderUI)) {
      return;
    }
    Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
    final int LABEL_WIDTH = 60;
    int lastLabelPosition = -2 * LABEL_WIDTH;
    for (int i = 0; i < slider.getWidth(); i++) {
      if (i < (lastLabelPosition + LABEL_WIDTH)) {
        continue;
      }
      int sliderValue = ((BasicSliderUI) slider.getUI()).valueForXPosition(i);
      JLabel label = new JLabel(chooseGoodIncrement(toScale(sliderValue)).toString());
      label.setFont(sliderLabelFont);
      labelTable.put(sliderValue, label);
      lastLabelPosition = i;
    }
    if (labelTable.isEmpty()) {
      // Get here during initialization. [Jon Aquino]
      return;
    }
    slider.setLabelTable(labelTable);
  }

  /**
   * Return a Point2D in the view model from a model coordinate.
   */
  // Added to implement Java2DConverter.PointConverter interface (used in
  // getWireFrame)
  // [mmichaud 2007-05-27]
  public Point2D toViewPoint(Coordinate modelCoordinate) {
    Point2D.Double pt = new Point2D.Double(modelCoordinate.x, modelCoordinate.y);
    return affineTransform.transform(pt, pt);
  }

  // Added to implement Java2DConverter.PointConverter modified on 2011-03-06
  public Envelope getEnvelopeInModelCoordinates() {
    return lastGoodEnvelope;
  }

}
