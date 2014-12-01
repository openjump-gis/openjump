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
 * michael.michaud@free.fr
 */

package org.openjump.core.ui.plugin.mousemenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.openjump.core.ui.images.IconLoader;

import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class ReverseOrientationPlugIn extends AbstractPlugIn {

    public static final ImageIcon ICON = IconLoader.icon("reverse.png");

    public void initialize(PlugInContext context) throws Exception {
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        JPopupMenu popupMenu = LayerViewPanel.popupMenu();
        featureInstaller.addPopupMenuItem(popupMenu,
            this, getName(),
            false, ICON, 
            this.createEnableCheck(workbenchContext)); 
    }
    
    public boolean execute(final PlugInContext context) throws Exception {
        final ArrayList transactions = new ArrayList();
        reportNothingToUndoYet(context);
        
        Collection layers = context.getLayerViewPanel()
                                   .getSelectionManager()
                                   .getLayersWithSelectedItems();
        //Collection geometries = context.getLayerViewPanel()
        //                               .getSelectionManager()
        //                               .getSelectedItems();
        for (Iterator i = layers.iterator(); i.hasNext(); ) {
            Layer layerWithSelectedItems = (Layer) i.next();
            transactions.add(createTransaction(layerWithSelectedItems, context));
        }
        EditTransaction.commit(transactions);
        return true;
    }
     
    private EditTransaction createTransaction(Layer layer, PlugInContext context) {
        EditTransaction transaction = EditTransaction.createTransactionOnSelection(
            new EditTransaction.SelectionEditor() {
                public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) {
                    for (Iterator j = selectedItems.iterator(); j.hasNext();) {
                        reverse((Geometry) j.next());
                    }
                    return geometryWithSelectedItems;
                }
            }, 
            context.getLayerViewPanel(), 
            context.getLayerViewPanel().getContext(), 
            getName(), 
            layer, 
            false,
            false
        );
        return transaction;
    }
    
    private void reverse(Geometry geometry) {
        if (geometry instanceof GeometryCollection) reverse((GeometryCollection)geometry);
        else if (geometry instanceof Polygon) reverse((Polygon)geometry);
        else if (geometry instanceof LineString) reverse((LineString)geometry);
        else;
    }
    
    private void reverse(GeometryCollection geometry) {
        for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
            reverse(geometry.getGeometryN(i));
        }
    }
    
    private void reverse(Polygon geometry) {
        CoordinateArrays.reverse(geometry.getExteriorRing().getCoordinates());
        for (int i = 0 ; i < geometry.getNumInteriorRing() ; i++) {
            CoordinateArrays.reverse(geometry.getInteriorRingN(i).getCoordinates());
        }
    }
    
    private void reverse(LineString geometry) {
        CoordinateArrays.reverse(geometry.getCoordinates());
    }
    
    private void reverse(Point geometry) {}
    
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(1))
            .add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());
    }
}
