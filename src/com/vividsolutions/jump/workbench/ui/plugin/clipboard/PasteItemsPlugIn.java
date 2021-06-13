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
package com.vividsolutions.jump.workbench.ui.plugin.clipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.vividsolutions.jump.coordsys.CoordinateSystem;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.ui.images.IconLoader;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.Reprojector;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.WKTReader;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

/**
 * Lets user paste items from the clipboard.
 */

public class PasteItemsPlugIn extends AbstractPlugIn {
	
  public static ImageIcon ICON = IconLoader.icon("items_paste.png");
  
  public static final String MUST_NOT_BE_EMPTY = I18N.getInstance().get("ui.plugin.PasteItemsPlugIn.clipboard-must-not-be-empty");
  public static final String MUST_CONTAIN_GEOMETRY = I18N.getInstance().get("ui.plugin.PasteItemsPlugIn.clipboard-must-contain-geometries-or-wkt");

  private static final String DECIMAL_PATTERN = "\\d+(?:\\.\\d+)?";

  private static final String WHITESPACE_OR_COMMA = "(?:\\s+|(?:\\s*,\\s*))";

  private static final Pattern pointCoordsPattern = Pattern.compile("\\s*\\(?\\s*("
    + DECIMAL_PATTERN
    + ")"
    + WHITESPACE_OR_COMMA
    + "("
    + DECIMAL_PATTERN
    + ")(?:" + WHITESPACE_OR_COMMA + "(" + DECIMAL_PATTERN + "))?\\s*\\)?\\s*");
  
  private WKTReader reader = new WKTReader();

  // Note: Need to copy the data twice: once when the user hits Copy, so she is
  // free to modify the original afterwards, and again when the user hits Paste,
  // so she is free to modify the first copy then hit Paste again. [Jon Aquino]
  public PasteItemsPlugIn() {
    this.setShortcutKeys(KeyEvent.VK_V);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK);
  }
  
  protected PasteItemsPlugIn(String name) {
    super(name);
  }

  public String getNameWithMnemonic() {
    return StringUtil.replace(getName(), "P", "&P", false);
  }

  public boolean execute(final PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);

    Collection features;
    Transferable transferable = GUIUtil.getContents(Toolkit.getDefaultToolkit()
      .getSystemClipboard());

    if (transferable.isDataFlavorSupported(CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR)) {
      features = (Collection)GUIUtil.getContents(
        Toolkit.getDefaultToolkit().getSystemClipboard()).getTransferData(
        CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR);
    } else {
      // Allow the user to paste features using WKT. [Jon Aquino]
      String value = (String)transferable.getTransferData(DataFlavor.stringFlavor);
      features = processCoordinates(value);
      if (features.isEmpty()) {
        features = reader.read(new StringReader(value)).getFeatures();
      }
    }

    final Layer layer = context.getSelectedLayer(0);
    final Collection featureCopies = conform(features,
      layer.getFeatureCollectionWrapper().getFeatureSchema());
      execute(new UndoableCommand(getName()) {
      public void execute() {
        layer.getFeatureCollectionWrapper().addAll(featureCopies);
        context.getLayerViewPanel().getSelectionManager().getFeatureSelection().unselectItems();
        context.getLayerViewPanel().getSelectionManager().getFeatureSelection().selectItems(layer, featureCopies);
        if (layer.getStyle(SRIDStyle.class) != null) {
            ((SRIDStyle)layer.getStyle(SRIDStyle.class)).updateSRIDs(layer);
        }
      }

      public void unexecute() {
        layer.getFeatureCollectionWrapper().removeAll(featureCopies);
      }
    }, context);

    return true;
  }

  private Collection<Feature> processCoordinates(String value) {
    Matcher matcher = pointCoordsPattern.matcher(value);
    Collection<Feature> features = new ArrayList<Feature>();
    if (matcher.find() && matcher.start() == 0) {
      do {
        double x = Double.parseDouble(matcher.group(1));
        double y = Double.parseDouble(matcher.group(2));
        Coordinate coordinate = new Coordinate(x, y);
        String zString = matcher.group(3);
        if (zString != null) {
          coordinate.z = Double.parseDouble(zString);
        }
        FeatureSchema featureSchema = new FeatureSchema();
        featureSchema.addAttribute("Geometry", AttributeType.GEOMETRY);

        Feature feature = new BasicFeature(featureSchema);
        Point point = new GeometryFactory().createPoint(coordinate);
        feature.setGeometry(point);
        features.add(feature);
      } while (matcher.find());
    }
    return features;
  }

  public static Collection conform(Collection features,
    FeatureSchema targetFeatureSchema) {
    final ArrayList featureCopies = new ArrayList();

    for (Iterator i = features.iterator(); i.hasNext();) {
      Feature feature = (Feature)i.next();
      featureCopies.add(conform(feature, targetFeatureSchema));
    }

    return featureCopies;
  }

  private static Feature conform(Feature original,
    FeatureSchema targetFeatureSchema) {
    // Transfer as many attributes as possible, matching on name. [Jon Aquino]
    Feature copy = new BasicFeature(targetFeatureSchema);
    copy.setGeometry((Geometry)original.getGeometry().clone());

    for (int i = 0; i < original.getSchema().getAttributeCount(); i++) {
      if (i == original.getSchema().getGeometryIndex()) {
        continue;
      }

      String attributeName = original.getSchema().getAttributeName(i);

      if (!copy.getSchema().hasAttribute(attributeName)) {
        continue;
      }

      if (copy.getSchema().getAttributeType(attributeName) != original.getSchema()
        .getAttributeType(attributeName)) {
        continue;
      }
      // [mmichaud 2014-09-22] do not copy if target attribute is the external PK
      if (copy.getSchema().getAttributeIndex(attributeName) !=
              copy.getSchema().getExternalPrimaryKeyIndex()) {
          copy.setAttribute(attributeName, original.getAttribute(attributeName));
      }

    }

    if (original.getSchema().getCoordinateSystem() != CoordinateSystem.UNSPECIFIED
        && copy.getSchema().getCoordinateSystem() != CoordinateSystem.UNSPECIFIED ) {
        Reprojector.instance().reproject(copy.getGeometry(),
                original.getSchema().getCoordinateSystem(),
                copy.getSchema().getCoordinateSystem());
    }

    return copy;
  }

  public static MultiEnableCheck createEnableCheck(
    final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return new MultiEnableCheck().add(
      checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck()).add(
      checkFactory.createExactlyNLayersMustBeSelectedCheck(1)).add(
      checkFactory.createSelectedLayersMustBeEditableCheck()).add(
      new EnableCheck() {
        public String check(JComponent component) {
          Transferable transferable = GUIUtil.getContents(Toolkit.getDefaultToolkit()
            .getSystemClipboard());

          if (transferable == null) {
            return MUST_NOT_BE_EMPTY;
          }

          if (transferable.isDataFlavorSupported(CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR)) {
            return null;
          }

          try {
            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
              String value = (String)transferable.getTransferData(DataFlavor.stringFlavor);
              if (isWKT(value) || isCoordinates(value)) {
                return null;
              }
            }
          } catch (Exception e) {
            workbenchContext.getWorkbench().getFrame().warnUser(e.getMessage());
          }

          return MUST_CONTAIN_GEOMETRY;
        }

        private boolean isCoordinates(String value) {
          return pointCoordsPattern.matcher(value).find();
        }

        private boolean isWKT(String s) {
          try {
            new WKTReader().read(new StringReader(s));

            return true;
          } catch (Exception e) {
            return false;
          }
        }
      });
  }
  
  public ImageIcon getIcon() {
      return ICON;
  }
}
