package org.openjump.core.ui.plugin.mousemenu;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JPopupMenu;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.io.WKTReader;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.MoveSelectedItemsTool;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CollectionOfFeaturesTransferable;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;

public class PasteItemsAtPlugIn extends PasteItemsPlugIn {

    WKTReader reader = new WKTReader();
	private static final String PASTE_ITEMS_AT_POINT = I18N.get("org.openjump.core.ui.plugin.mousemenu.PasteItemsAtPlugIn.Paste-Items-At-Point");
	
    public void initialize(PlugInContext context) throws Exception
	    {     
    		WorkbenchContext workbenchContext = context.getWorkbenchContext();
	        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
	        JPopupMenu popupMenu = LayerViewPanel.popupMenu();
	        featureInstaller.addPopupMenuItem(popupMenu,
	            this, getNameWithMnemonic() + "{pos:10}",
	            false, null,  //to do: add icon
	            this.createEnableCheck(workbenchContext));
	    }
    
	public String getName() {
		return PASTE_ITEMS_AT_POINT;
	}

    public boolean execute(final PlugInContext context)
    throws Exception {
    reportNothingToUndoYet(context);

    Collection features;
    Transferable transferable = GUIUtil.getContents(Toolkit.getDefaultToolkit()
                                                           .getSystemClipboard());

    if (transferable.isDataFlavorSupported(
                CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR)) {
        features = (Collection) GUIUtil.getContents(Toolkit.getDefaultToolkit()
                                                           .getSystemClipboard())
                                       .getTransferData(CollectionOfFeaturesTransferable.COLLECTION_OF_FEATURES_FLAVOR);
    } else {
        //Allow the user to paste features using WKT. [Jon Aquino]
        features = reader.read(new StringReader(
                    (String) transferable.getTransferData(
                        DataFlavor.stringFlavor))).getFeatures();
    }

    final SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
    final Layer layer = context.getSelectedLayer(0);
    final Collection featureCopies = conform(features,
            layer.getFeatureCollectionWrapper().getFeatureSchema());
    Feature feature = ((Feature) featureCopies.iterator().next());
	Coordinate firstPoint = feature.getGeometry().getCoordinate();
	Coordinate cursorPt = context.getLayerViewPanel().getViewport().toModelCoordinate(
            context.getLayerViewPanel().getLastClickedPoint());
	Coordinate displacement = CoordUtil.subtract(cursorPt, firstPoint);
	moveAll(featureCopies,displacement);
    
    execute(new UndoableCommand(getName()) {
    	public void execute() {
    		layer.getFeatureCollectionWrapper().addAll(featureCopies);
    		selectionManager.clear();
    		selectionManager.getFeatureSelection().selectItems(layer, featureCopies);
    	}

    	public void unexecute() {
    		layer.getFeatureCollectionWrapper().removeAll(featureCopies);
    	}
    }, context);

    return true;
}

    private void moveAll( Collection featureCopies, Coordinate displacement) {
    	for (Iterator j = featureCopies.iterator(); j.hasNext();) {
    		Feature item = (Feature) j.next();
    		move(item.getGeometry(), displacement);
    		item.getGeometry().geometryChanged();
    	}
    }

    private void move(Geometry geometry, final Coordinate displacement) {
        geometry.apply(new CoordinateFilter() {
            public void filter(Coordinate coordinate) {
                coordinate.setCoordinate(CoordUtil.add(coordinate, displacement));
            }
        });
    }
    

}
