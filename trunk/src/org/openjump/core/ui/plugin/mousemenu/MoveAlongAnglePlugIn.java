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
 */

package org.openjump.core.ui.plugin.mousemenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class MoveAlongAnglePlugIn extends AbstractPlugIn {
	
	private final static String sMoveAlongAngle = I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.Move-Along-Angle");
	private final static String sMoveSelectedFeaturesAlongAngle =I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.Move-Selected-Features-Along-Angle");
	private final static String sTheDistanceInMapUnitsToMove=I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.The-distance-in-map-units-to-move");
	private final static String sTheAngleInDegreesToMove = I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.The-angle-in-degrees-to-move");
	private final static String sWillCovert = I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.Will-convert-input-value-from-feet-to-meters");
	
    private WorkbenchContext workbenchContext;
    private final static String ANGLE = I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.Move-Angle");
    private final static String DISTANCE = I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.Move-Distance");
    private final double Deg2Rad = 0.0174532925199432;	//pi/180
    private MultiInputDialog dialog;
    private double moveAngle = 0.0;
    private double moveDistance = 1.0;

    private final static String CONVERTTOMETERS = I18N.get("org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn.Convert-Feet-to-Meters");
    private double conversionFactor = 1.0;

    public void initialize(PlugInContext context) throws Exception
    {     
        workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        JPopupMenu popupMenu = LayerViewPanel.popupMenu();
        featureInstaller.addPopupMenuItem(popupMenu,
            this, sMoveAlongAngle,
            false, null,  //to do: add icon
            this.createEnableCheck(workbenchContext));
    }
    
    public boolean execute(final PlugInContext context) throws Exception {
        final ArrayList transactions = new ArrayList();
        reportNothingToUndoYet(context);
        MultiInputDialog dialog = new MultiInputDialog(
            context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        
        double angle = Deg2Rad * moveAngle;
        double x = Math.cos(angle) * moveDistance * conversionFactor;
        double y = Math.sin(angle) * moveDistance * conversionFactor;
        Coordinate displacement = new Coordinate(x,y);   
        workbenchContext = context.getWorkbenchContext();
        for (Iterator i = workbenchContext.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems().iterator();
            i.hasNext();
            ) {
            Layer layerWithSelectedItems = (Layer) i.next();
            transactions.add(createTransaction(layerWithSelectedItems, displacement));
        }
        EditTransaction.commit(transactions);
        return true;
    }
    
      private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
      {
        dialog.setSideBarImage(new ImageIcon(getClass().getResource("Compass.png"))); 
        dialog.setSideBarDescription(sMoveSelectedFeaturesAlongAngle);
        dialog.addDoubleField(DISTANCE, moveDistance, 6, sTheDistanceInMapUnitsToMove);
        dialog.addDoubleField(ANGLE, moveAngle, 6, sTheAngleInDegreesToMove);
        dialog.addCheckBox(CONVERTTOMETERS, (!(conversionFactor == 1.0)), sWillCovert);
      }

      private void getDialogValues(MultiInputDialog dialog) {
        if(dialog.getCheckBox(CONVERTTOMETERS).isSelected())
        	conversionFactor =  0.3048;
        else
        	conversionFactor = 1.0;
        moveDistance = dialog.getDouble(DISTANCE);
        moveAngle = dialog.getDouble(ANGLE);
      }

    private EditTransaction createTransaction(Layer layer, final Coordinate displacement) {
        EditTransaction transaction =
            EditTransaction.createTransactionOnSelection(new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) {
                for (Iterator j = selectedItems.iterator(); j.hasNext();) {
                    Geometry item = (Geometry) j.next();
                    move(item, displacement);
                }

                return geometryWithSelectedItems;
            }
        }, workbenchContext.getLayerViewPanel(), workbenchContext.getLayerViewPanel().getContext(), getName(), layer, false,false);// isRollingBackInvalidEdits(), false);
        return transaction;
    }

    private void move(Geometry geometry, final Coordinate displacement) {
        geometry.apply(new CoordinateFilter() {
            public void filter(Coordinate coordinate) {
                coordinate.setCoordinate(CoordUtil.add(coordinate, displacement));
            }
        });
    }
    
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(1))
            .add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());

    }
}
