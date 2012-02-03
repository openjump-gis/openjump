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
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class EditSelectedSidePlugIn extends AbstractPlugIn {

	private final static String sEditSelectedSide = I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSidePlugIn.Edit-Selected-Side");
	private final static String sPointsDoNotHaveSides = I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSidePlugIn.Points-do-not-have-sides");
	private final static String sSelectOnlyOnePart = I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSidePlugIn.Select-only-one-part");
	
    public void initialize(PlugInContext context) throws Exception
    {     
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        JPopupMenu popupMenu = LayerViewPanel.popupMenu();
        featureInstaller.addPopupMenuItem(popupMenu,
            this, sEditSelectedSide,
            false, null,  //to do: add icon
            this.createEnableCheck(workbenchContext)); 
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
        reportNothingToUndoYet(context);
        EditSelectedSideDialog dialog = new EditSelectedSideDialog(context, sEditSelectedSide, false);
        dialog.setVisible(true);
        return true;
    }
        
    public EnableCheck noPointsMayBeSelectedCheck(final WorkbenchContext workbenchContext) {
        return new EnableCheck() {
            public String check(JComponent component) {
	           Collection selectedItems = ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getSelectedItems();
            Geometry selectedGeo = (Geometry) selectedItems.iterator().next();
                return (selectedGeo instanceof Point)
                    ? sPointsDoNotHaveSides
                    : null;
               }
        };
    }

    public EnableCheck noMultiShapesMayBeSelectedCheck(final WorkbenchContext workbenchContext) {
        return new EnableCheck() {
            public String check(JComponent component) {
	           Collection selectedItems = ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getSelectedItems();
            Geometry selectedGeo = (Geometry) selectedItems.iterator().next();

                return (((selectedGeo instanceof MultiPoint) || (selectedGeo instanceof MultiLineString) || (selectedGeo instanceof MultiPolygon) || (selectedGeo instanceof GeometryCollection)))
                    ? (sSelectOnlyOnePart)
                    : null;
               }
        };
    }
    
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createExactlyNItemsMustBeSelectedCheck(1))
            .add(noPointsMayBeSelectedCheck(workbenchContext))
            .add(noMultiShapesMayBeSelectedCheck(workbenchContext))
            .add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());
    }
}
