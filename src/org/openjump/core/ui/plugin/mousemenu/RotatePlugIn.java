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

import org.openjump.core.ui.images.IconLoader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

public class RotatePlugIn extends AbstractPlugIn {

    private static final String sRotate = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.Rotate");
    private static final String sRotateSelectedFeatures = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.Rotate-Selected-Features");
    private static final String sRotateAbout = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.Rotate-about");
    private static final String sTheAngleInDegreesClockwise = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.The-angle-in-degrees-clockwise");

    private static final String METHOD_ABOUTCENTER = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.Center");
    private static final String METHOD_ABOUTCLICKPOINT = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.Click-Point");
    private final static String ANGLE = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.Rotation-Angle");
    private final static String ROTATEABOUT = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.RotatePlugIn.Rotate-About");
    private final double Deg2Rad = 0.0174532925199432; // pi/180
    private WorkbenchContext workbenchContext;
    private double rotateAngle = 45.0;
    private double radiansAngle = 0.0;
    private double cosAngle = 0.0;
    private double sinAngle = 0.0;
    private Coordinate rotationPoint = new Coordinate(0.0, 0.0);
    private Collection methodNames = new ArrayList();
    private String methodNameToRun = METHOD_ABOUTCENTER;

    public static final ImageIcon ICON = IconLoader.icon("Rotate16.gif");

    public String getName() {
        return sRotate;
    }

    public void initialize(PlugInContext context) throws Exception {
        /*
         * workbenchContext = context.getWorkbenchContext(); FeatureInstaller
         * featureInstaller = new FeatureInstaller(workbenchContext); JPopupMenu
         * popupMenu = LayerViewPanel.popupMenu();
         * featureInstaller.addPopupMenuItem(popupMenu, this, sRotate, false,
         * ICON, this.createEnableCheck(workbenchContext));
         */
        methodNames.add(METHOD_ABOUTCENTER);
        methodNames.add(METHOD_ABOUTCLICKPOINT);
    }

    public boolean execute(final PlugInContext context) throws Exception {
        final ArrayList transactions = new ArrayList();
        reportNothingToUndoYet(context);
        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        // /GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);

        workbenchContext = context.getWorkbenchContext();
        Collection layers = workbenchContext.getLayerViewPanel()
                .getSelectionManager().getLayersWithSelectedItems();
        if (methodNameToRun.equals(METHOD_ABOUTCENTER)) {
            // rotationPoint = getRotationPoint(layers);
            Envelope en = new Envelope();
            Collection geometries = context.getLayerViewPanel()
                    .getSelectionManager().getSelectedItems();
            for (Iterator j = geometries.iterator(); j.hasNext();) {
                Geometry geometry = (Geometry) j.next();
                en.expandToInclude(geometry.getEnvelopeInternal());
            }
            rotationPoint.x = en.getMinX() + (en.getMaxX() - en.getMinX())
                    / 2.0;
            rotationPoint.y = en.getMinY() + (en.getMaxY() - en.getMinY())
                    / 2.0;
        } else if (methodNameToRun.equals(METHOD_ABOUTCLICKPOINT)) {
            rotationPoint = context
                    .getLayerViewPanel()
                    .getViewport()
                    .toModelCoordinate(
                            context.getLayerViewPanel().getLastClickedPoint());
        }
        radiansAngle = Deg2Rad * rotateAngle;
        cosAngle = Math.cos(radiansAngle);
        sinAngle = Math.sin(radiansAngle);

        for (Iterator i = layers.iterator(); i.hasNext();) {
            Layer layerWithSelectedItems = (Layer) i.next();
            transactions.add(createTransaction(layerWithSelectedItems));
        }
        EditTransaction.commit(transactions);
        return true;
    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        dialog.setSideBarImage(new ImageIcon(getClass().getResource(
                "Rotate.png")));
        dialog.setSideBarDescription(sRotateSelectedFeatures);
        dialog.addComboBox(ROTATEABOUT, methodNameToRun, methodNames,
                sRotateAbout);
        dialog.addDoubleField(ANGLE, rotateAngle, 6,
                sTheAngleInDegreesClockwise);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        // JComboBox combobox = dialog.getComboBox(ROTATEABOUT);
        methodNameToRun = dialog.getText(ROTATEABOUT);
        rotateAngle = dialog.getDouble(ANGLE);
    }

    private EditTransaction createTransaction(Layer layer) {
        EditTransaction transaction = EditTransaction
                .createTransactionOnSelection(
                        new EditTransaction.SelectionEditor() {
                            public Geometry edit(
                                    Geometry geometryWithSelectedItems,
                                    Collection selectedItems) {
                                for (Iterator j = selectedItems.iterator(); j
                                        .hasNext();) {
                                    Geometry item = (Geometry) j.next();
                                    rotate(item);
                                }
                                return geometryWithSelectedItems;
                            }
                        }, workbenchContext.getLayerViewPanel(),
                        workbenchContext.getLayerViewPanel().getContext(),
                        getName(), layer, false, false);// isRollingBackInvalidEdits(),
                                                        // false);
        return transaction;
    }

    // rotate geometry about rotationPoint by rotationAngle degrees (+
    // clockwise)
    private void rotate(Geometry geometry) {
        geometry.apply(new CoordinateFilter() {
            public void filter(Coordinate coordinate) {
                double x = coordinate.x - rotationPoint.x;
                double y = coordinate.y - rotationPoint.y;
                coordinate.x = rotationPoint.x + (x * cosAngle)
                        + (y * sinAngle);
                coordinate.y = rotationPoint.y + (y * cosAngle)
                        - (x * sinAngle);
            }
        });
    }

    public MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithLayerViewPanelMustBeActiveCheck())
                .add(checkFactory
                        .createAtLeastNFeaturesMustHaveSelectedItemsCheck(1))
                .add(checkFactory
                        .createSelectedItemsLayersMustBeEditableCheck());
    }
}
