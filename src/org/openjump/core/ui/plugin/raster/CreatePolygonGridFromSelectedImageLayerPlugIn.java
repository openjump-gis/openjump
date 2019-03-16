/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This class implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */

/*****************************************************
 * created:  		28.Oct.2009
 * last modified:   13.March 2019 (Giuseppe Aruta)    					
 * 					
 * 
 * @author sstein
 * 
 * description:
 * 
 *****************************************************/

package org.openjump.core.ui.plugin.raster;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Creates a polygon grid from the current selected raster image
 * TODO : I was going todo this as a normal plugin, but this won't work since
 * raster images are Layerables and not layer objects, so the drop down list doesn't
 * display them
 * @author sstein
 *
 *Added Layerable drop down list for raster layers
 *@author Giuseppe Aruta[2019-03-16]
 **/
public class CreatePolygonGridFromSelectedImageLayerPlugIn extends
        AbstractPlugIn implements ThreadedPlugIn {

    private MultiInputDialog dialog;

    private final String sName = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.Create-Polygon-Grid-from-Raster");
    public String sRemoveZeroCells = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.remove-cells-with-values")
            + " =< 0";
    private final String sSidebar = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.Creates-a-polygon-grid-from-the-selected-raster");
    public String sMaxCellsToDisplay = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.max-cells-to-display");
    private final String sCreatingPolygons = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.Creating-polygons")
            + "...";
    private final String sGrid = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.grid");
    public static String Source_Layer = I18N
            .get("ui.GenericNames.Source-Layer");
    private final String sToManyPolygons = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.To-many-polygons-to-generate");
    private final String sBand = I18N
            .get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.band");

    GeometryFactory gfactory = new GeometryFactory();
    public int maxCells = 200000;
    public boolean removeZeroCells = false;

    private JComboBox<RasterImageLayer> layerableComboBox = new JComboBox<RasterImageLayer>();
    private RasterImageLayer rLayer;
    List<RasterImageLayer> fLayers = new ArrayList<RasterImageLayer>();

    @Override
    public void initialize(PlugInContext context) throws Exception {

        FeatureInstaller.getInstance()
                .addMainMenuPlugin(
                        this, //exe
                        new String[] { MenuNames.RASTER,
                                MenuNames.RASTER_VECTORIALIZE }, //menu path
                        sName + "...", false, //checkbox
                        null, //icon
                        createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class));
    }

    /**
     *@inheritDoc
     */
    public String getIconString() {
        return null;
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes.
        setDialogValues(context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        } else {
            getDialogValues(dialog);
        }
        return true;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.allowCancellationRequests();
        final GeometryFactory gf = new GeometryFactory();
        //-- get the rasterimage layer

        if (rLayer == null) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return;
        }

        //-- create a sextante raster layer since it is easier to handle
        final OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        // [mmichaud 2013-05-25] false : this is a temporary image not a file based image
        rstLayer.create(rLayer, false);
        // create a gridwrapper to later access the cells
        final GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        //-- create the FeatureSchema
        final FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("geometry", AttributeType.GEOMETRY);
        final int numBands = rstLayer.getBandsCount();
        for (int i = 0; i < numBands; i++) {
            fs.addAttribute(sBand + "_" + i, AttributeType.DOUBLE);
        }
        //-- create a new empty dataset
        final FeatureCollection fd = new FeatureDataset(fs);
        //-- create points
        monitor.report(sCreatingPolygons);
        final int nx = rstLayer.getLayerGridExtent().getNX();
        final int ny = rstLayer.getLayerGridExtent().getNY();
        final double halfCellDimX = 0.5 * rstLayer.getLayerGridExtent()
                .getCellSize().x;
        final double halfCellDimY = 0.5 * rstLayer.getLayerGridExtent()
                .getCellSize().y;
        final int numPoints = nx * ny;
        if (numPoints > maxCells) {
            context.getWorkbenchFrame().warnUser(
                    sToManyPolygons + ": " + numPoints + " > " + maxCells);
            return;
        }
        for (int x = 0; x < nx; x++) {//cols
            for (int y = 0; y < ny; y++) {//rows
                final Feature ftemp = new BasicFeature(fs);
                final Point2D pt = rstLayer.getLayerGridExtent()
                        .getWorldCoordsFromGridCoords(x, y);
                final Coordinate[] coords = new Coordinate[5];
                coords[0] = new Coordinate(pt.getX() - halfCellDimX, pt.getY()
                        + halfCellDimY); //topleft
                coords[1] = new Coordinate(pt.getX() + halfCellDimX, pt.getY()
                        + halfCellDimY); //topright
                coords[2] = new Coordinate(pt.getX() + halfCellDimX, pt.getY()
                        - halfCellDimY); //lowerright
                coords[3] = new Coordinate(pt.getX() - halfCellDimX, pt.getY()
                        - halfCellDimY); //lowerleft
                //-- to close poly
                coords[4] = (Coordinate) coords[0].clone(); //topleft
                //-- create the cell poly
                final LinearRing lr = gf.createLinearRing(coords);
                final Geometry poly = gf.createPolygon(lr, null);
                ftemp.setGeometry(poly);
                //-- set attributes
                double sumvalue = 0;
                for (int i = 0; i < numBands; i++) {
                    final double value = gwrapper.getCellValueAsDouble(x, y, i);
                    ftemp.setAttribute(sBand + "_" + i, value);
                    sumvalue = sumvalue + value;
                }
                //-- add the feature
                if (removeZeroCells == true) {
                    if (sumvalue > 0) {
                        fd.add(ftemp);
                    }
                } else {
                    fd.add(ftemp);
                }
                //-- check if user wants to stop
                if (monitor.isCancelRequested()) {
                    if (fd.size() > 0) {
                        context.addLayer(StandardCategoryNames.RESULT,
                                rstLayer.getName() + "_cancel_" + sGrid, fd);
                    }
                    return;
                }
            }
        }
        //-- output
        if (fd.size() > 0) {
            context.addLayer(StandardCategoryNames.RESULT, rstLayer.getName()
                    + "_" + sGrid, fd);
        }
    }

    private void setDialogValues(PlugInContext context) {

        dialog = new MultiInputDialog(context.getWorkbenchFrame(), sName, true);
        dialog.setSideBarDescription(sSidebar);
        if (!context.getLayerNamePanel().selectedNodes(RasterImageLayer.class)
                .isEmpty()) {
            rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(
                    context, RasterImageLayer.class);
        } else {
            rLayer = context.getTask().getLayerManager()
                    .getLayerables(RasterImageLayer.class).get(0);
        }
        fLayers = context.getTask().getLayerManager()
                .getLayerables(RasterImageLayer.class);
        layerableComboBox = dialog.addLayerableComboBox(Source_Layer, rLayer,
                "", fLayers);
        layerableComboBox.setSize(200,
                layerableComboBox.getPreferredSize().height);
        dialog.addCheckBox(sRemoveZeroCells, removeZeroCells);
        dialog.addIntegerField(sMaxCellsToDisplay, maxCells, 10,
                sMaxCellsToDisplay);
        //dialog.addDoubleField(T1, 20.0, 4);
        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        rLayer = (RasterImageLayer) dialog.getLayerable(Source_Layer);
        removeZeroCells = dialog.getBoolean(sRemoveZeroCells);
        maxCells = dialog.getInteger(sMaxCellsToDisplay);
    }
}
