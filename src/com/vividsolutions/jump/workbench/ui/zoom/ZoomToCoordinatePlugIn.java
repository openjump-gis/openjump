package com.vividsolutions.jump.workbench.ui.zoom;

import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.util.CoordinateArrays;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.Animations;

public class ZoomToCoordinatePlugIn extends AbstractPlugIn {
  private Coordinate lastCoordinate = new Coordinate(0, 0);

  public boolean execute(PlugInContext context) throws Exception {
    Coordinate coordinate = null;
    boolean retry = true;
    while (retry) {
      try {
        String value = JOptionPane
            .showInputDialog(
                context.getWorkbenchFrame(),
                I18N.get("ui.zoom.ZoomToCoordinatePlugIn.enter-coordinate-to-zoom-to"),
                lastCoordinate.x + ", " + lastCoordinate.y);
        if (value == null) {
          retry = false;
        } else {
          coordinate = toCoordinate(value);
        }
      } catch (Exception e) {
        context.getWorkbenchContext().getErrorHandler().handleThrowable(e);
      }
    }

    if (coordinate == null) {
      return false;
    }

    lastCoordinate = coordinate;
    context.getLayerViewPanel().getViewport()
        .zoom(toEnvelope(coordinate, context.getLayerManager()));
    Animations.drawExpandingRing(context.getLayerViewPanel().getViewport()
        .toViewPoint(lastCoordinate), false, Color.BLUE,
        context.getLayerViewPanel(), new float[] { 20, 20 });

    return true;
  }

  private Envelope toEnvelope(Coordinate coordinate, LayerManager layerManager) {
    int segments = 0;
    int segmentSum = 0;
    outer: for (Iterator i = layerManager.iterator(); i.hasNext();) {
      Layer layer = (Layer) i.next();
      for (Iterator j = layer.getFeatureCollectionWrapper().iterator(); j
          .hasNext();) {
        Feature feature = (Feature) j.next();
        Collection coordinateArrays = CoordinateArrays.toCoordinateArrays(
            feature.getGeometry(), false);
        for (Iterator k = coordinateArrays.iterator(); k.hasNext();) {
          Coordinate[] coordinates = (Coordinate[]) k.next();
          for (int a = 1; a < coordinates.length; a++) {
            segments++;
            segmentSum += coordinates[a].distance(coordinates[a - 1]);
            if (segments > 100) {
              break outer;
            }
          }
        }
      }
    }
    Envelope envelope = new Envelope(coordinate);
    // Choose a reasonable magnification [Jon Aquino 10/22/2003]
    if (segmentSum > 0) {
      envelope = EnvelopeUtil.expand(envelope, segmentSum / (double) segments);
    } else {
      envelope = EnvelopeUtil.expand(envelope, 50);
    }
    return envelope;
  }

  private Coordinate toCoordinate(String s) throws Exception {
    s = StringUtil.replaceAll(s, ",", " ");
    StringTokenizer tokenizer = new StringTokenizer(s);
    if (tokenizer.countTokens() < 2) {
      throw new JUMPException(
          I18N.get("ui.zoom.ZoomToCoordinatePlugIn.enter-two-values"));
    }

    String x = tokenizer.nextToken();
    if (!StringUtil.isNumber(x))
      throw new JUMPException(I18N.getMessage("ui.zoom.ZoomToCoordinatePlugIn.{0}-is-not-a-number", x));

    String y = tokenizer.nextToken();
    if (!StringUtil.isNumber(y))
      throw new JUMPException(I18N.getMessage("ui.zoom.ZoomToCoordinatePlugIn.{0}-is-not-a-number", y));

    return new Coordinate(Double.parseDouble(x), Double.parseDouble(y));
  }

  public MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return new MultiEnableCheck().add(checkFactory
        .createWindowWithLayerViewPanelMustBeActiveCheck());
  }
}