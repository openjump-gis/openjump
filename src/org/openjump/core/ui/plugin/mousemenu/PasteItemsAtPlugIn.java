package org.openjump.core.ui.plugin.mousemenu;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.openjump.core.ui.images.IconLoader;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.io.WKTReader;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CollectionOfFeaturesTransferable;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;

public class PasteItemsAtPlugIn extends PasteItemsPlugIn {
  public static ImageIcon ICON = IconLoader.icon("items_paste_here.png");
  WKTReader reader = new WKTReader();
  private static final String PASTE_ITEMS_AT_POINT = I18N.getInstance().get("org.openjump.core.ui.plugin.mousemenu.PasteItemsAtPlugIn.Paste-Items-At-Point");

  public PasteItemsAtPlugIn() {
    super(PASTE_ITEMS_AT_POINT);
    this.setShortcutKeys(KeyEvent.VK_V);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK+KeyEvent.SHIFT_MASK);
  }

  public void initialize(PlugInContext context) throws Exception {
//    WorkbenchContext workbenchContext = context.getWorkbenchContext();
//    FeatureInstaller featureInstaller = context.getFeatureInstaller();
//    JPopupMenu popupMenu = LayerViewPanel.popupMenu();
//    featureInstaller.addPopupMenuItem(popupMenu, this, getNameWithMnemonic()
//        + "{pos:10}", false, this.getIcon(),
//        this.createEnableCheck(workbenchContext));
  }

  public boolean execute(final PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);

    Collection features;
    Transferable transferable = GUIUtil.getContents(Toolkit.getDefaultToolkit()
        .getSystemClipboard());

    if (transferable
        .isDataFlavorSupported(CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR)) {
      features = (Collection) GUIUtil.getContents(
          Toolkit.getDefaultToolkit().getSystemClipboard()).getTransferData(
          CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR);
    } else {
      // Allow the user to paste features using WKT. [Jon Aquino]
      features = reader.read(
          new StringReader((String) transferable
              .getTransferData(DataFlavor.stringFlavor))).getFeatures();
    }

    final SelectionManager selectionManager = context.getLayerViewPanel()
        .getSelectionManager();
    final Layer layer = context.getSelectedLayer(0);
    final Collection featureCopies = conform(features, layer
        .getFeatureCollectionWrapper().getFeatureSchema());
    Feature feature = ((Feature) featureCopies.iterator().next());
    Coordinate firstPoint = feature.getGeometry().getCoordinate();
    Coordinate cursorPt = context.getLayerViewPanel().getViewport()
        .toModelCoordinate(context.getLayerViewPanel().getLastMouseLocation());
    Coordinate displacement = CoordUtil.subtract(cursorPt, firstPoint);
    moveAll(featureCopies, displacement);

    execute(new UndoableCommand(getName()) {
      public void execute() {
        layer.getFeatureCollectionWrapper().addAll(featureCopies);
        selectionManager.clear();
        selectionManager.getFeatureSelection()
            .selectItems(layer, featureCopies);
      }

      public void unexecute() {
        layer.getFeatureCollectionWrapper().removeAll(featureCopies);
      }
    }, context);

    return true;
  }

  private void moveAll(Collection featureCopies, Coordinate displacement) {
    for (Iterator j = featureCopies.iterator(); j.hasNext();) {
      Feature item = (Feature) j.next();
      move(item.getGeometry(), displacement);
      item.getGeometry().geometryChanged();
    }
  }

  private void move(Geometry geometry, final Coordinate displacement) {
    geometry.apply(new CoordinateFilter() {
      public void filter(Coordinate coordinate) {
        // coordinate.setCoordinate(CoordUtil.add(coordinate, displacement));
        coordinate.x += displacement.x;
        coordinate.y += displacement.y;
      }
    });
  }

  public ImageIcon getIcon() {
    return ICON;
  }

}
