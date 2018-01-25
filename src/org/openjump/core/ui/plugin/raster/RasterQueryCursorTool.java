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

package org.openjump.core.ui.plugin.raster;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;

import javax.swing.Icon;

import org.openjump.core.CheckOS;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayer.RasterDataNotFoundException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;

import de.latlon.deejump.plugin.style.CrossVertexStyle;

public class RasterQueryCursorTool extends NClickTool {

    /*
     * [2013_05_27] Giuseppe Aruta Simple plugin that allows to inspect raster
     * cell value for DTM ver 0.1 2013_05_27
     * 
     * [2014_01_24] Giuseppe Aruta - Extended inspection to multiband raster
     * layers. Now multiple measure are displayed (and saved) by default. Press
     * SHIFT to display only last measure. Moving cursor on image shows raster
     * cell value on lower panel
     * 
     * [2014_02_24] Giuseppe Aruta - Fixed minor bug on lower panel [2015_07_08]
     * Giuseppe Aruta - Fixed bug #407 Sextante raster : displaying cell values
     * throws NPE
     */

    protected Coordinate tentativeCoordinate;
    public static final String LAYER_NAME = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.values");
    public static final String LAYER = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.layer");
    private final static String RASTER_NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
    private String lastClick = "-";
    // protected int width, height; // The dimensions of the image

    private String VALUE = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.value");
    private String name;
    PlugInContext context;
    LayerNamePanel namePanel;

    public RasterQueryCursorTool() {
        super(1);

    }

    @Override
    public Icon getIcon() {
        return IconLoader.icon("information_16x16.png");
    }

    @Override
    public Cursor getCursor() {
        // [ede 03.2103] linux currently support only 2 color cursors
        Image i = !CheckOS.isLinux() ? IconLoader
                .image("information_cursor.png") : IconLoader
                .image("information_cursor_2color.gif");
        return createCursor(i);
    }

    @Override
    protected void gestureFinished() throws NoninvertibleTransformException,
            IOException, RasterDataNotFoundException {
        reportNothingToUndoYet();

        RasterImageLayer rLayer = null;

        final WorkbenchContext wbcontext = this.getWorkbench().getContext();
        RasterImageLayer aLayer = null;

        Layerable[] ls = (Layerable[]) wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class)
                .toArray(new Layerable[] {});
        if (ls != null && ls.length > 0) {
            rLayer = (RasterImageLayer) ls[0];

            name = rLayer.getName();
            Coordinate coord = (Coordinate) getCoordinates().get(0);
            String cellValues = null;
            if (getPoint().within(rLayer.getWholeImageEnvelopeAsGeometry())) {
                try {
                    cellValues = "";
                    for (int b = 0; b < rLayer.getNumBands(); b++) {
                        Double cellValue = rLayer.getCellValue(coord.x,
                                coord.y, b);
                        if (cellValue != null) {
                            if (rLayer.isNoData(cellValue)) {
                                cellValues = "("
                                        + RASTER_NODATA
                                        + ") "
                                        + cellValues.concat(Double
                                                .toString(cellValue));
                            } else {
                                cellValues = cellValues.concat(Double
                                        .toString(cellValue));
                            }
                        }
                        cellValues = cellValues.concat("  ");
                        if (Double.isNaN(cellValue))
                            this.lastClick = "    ";
                        else {
                            this.lastClick = cellValues;
                        }
                    }
                } catch (RasterDataNotFoundException ex) {
                    cellValues = "???";
                }
                Geometry measureGeometry = null;
                if (wasShiftPressed()) {
                    pixelLayer().getFeatureCollectionWrapper().clear();
                    pixelLayer().getFeatureCollectionWrapper().add(
                            toFeature(measureGeometry, pixelLayer()
                                    .getFeatureCollectionWrapper()
                                    .getFeatureSchema()));
                } else {

                    pixelLayer().getFeatureCollectionWrapper().add(
                            toFeature(measureGeometry, pixelLayer()
                                    .getFeatureCollectionWrapper()
                                    .getFeatureSchema()));
                }
                getPanel().getContext().setStatusMessage(
                        "(" + name + ") " + VALUE + ": " + lastClick);
                getPanel().setViewportInitialized(true);
            } else {
                getPanel()
                        .getContext()
                        .warnUser(
                                I18N.get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.message"));
            }
        } else
            getPanel()
                    .getContext()
                    .warnUser(
                            I18N.get("org.openjump.core.rasterimage.SelectRasterImageFilesPanel.Select-Raster-Image"));

    }

    private Feature toFeature(Geometry measureGeometry, FeatureSchema schema)
            throws NoninvertibleTransformException {
        Feature feature = new BasicFeature(pixelLayer()
                .getFeatureCollectionWrapper().getFeatureSchema());
        feature.setGeometry(measureGeometry);
        feature.setAttribute("Pixel", lastClick);
        feature.setAttribute(LAYER, name);
        feature.setAttribute("X", new Double(getPoint().getCoordinate().x));
        feature.setAttribute("Y", new Double(getPoint().getCoordinate().y));
        feature.setAttribute("GEOM", getPoint());
        return feature;
    }

    private Layer pixelLayer() {
        Layer pixelLayer = getPanel().getLayerManager().getLayer(LAYER_NAME);
        if (pixelLayer != null) {
            return pixelLayer;
        }
        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("Pixel", AttributeType.STRING);
        schema.addAttribute("X", AttributeType.DOUBLE);
        schema.addAttribute("Y", AttributeType.DOUBLE);
        schema.addAttribute(LAYER, AttributeType.STRING);
        schema.addAttribute("GEOM", AttributeType.GEOMETRY);
        new FeatureSchema();
        new FeatureDataset(schema);
        FeatureCollection featureCollection = new FeatureDataset(schema);
        Layer layer;
        boolean firingEvents = getPanel().getLayerManager().isFiringEvents();
        getPanel().getLayerManager().setFiringEvents(false);
        try {
            layer = new Layer(LAYER_NAME, Color.red, featureCollection,
                    getPanel().getLayerManager());
            layer.removeStyle(layer.getVertexStyle());
            layer.addStyle(new CrossVertexStyle());
            layer.getBasicStyle().setLineColor(Color.black);
            layer.getBasicStyle().setFillColor(Color.black);
            layer.getBasicStyle().setLineWidth(1);
            layer.getBasicStyle().setAlpha(255);
            layer.getBasicStyle().setRenderingLine(true);
            layer.getBasicStyle().setRenderingFill(false);
            layer.getVertexStyle().setEnabled(false);
            layer.getVertexStyle().setSize(1);
            layer.setDrawingLast(true);
            LabelStyle labelStyle = layer.getLabelStyle();
            labelStyle.setAttribute("Pixel");
            labelStyle.setVerticalAlignment(LabelStyle.ABOVE_LINE);
            labelStyle.setHorizontalPosition(LabelStyle.RIGHT_SIDE);
            labelStyle.setHorizontalAlignment(LabelStyle.JUSTIFY_RIGHT);
            labelStyle.setEnabled(true);
            labelStyle.setColor(Color.black);
            labelStyle.setHeight(14);
            labelStyle.setOutlineShowing(true);
            labelStyle.setOutlineColor((Color.white));
            labelStyle.setHidingOverlappingLabels(false);
            labelStyle.setFont(layer.getLabelStyle().getFont()
                    .deriveFont(Font.BOLD, 16));
            layer.setDrawingLast(true);
        } finally {
            getPanel().getLayerManager().setFiringEvents(firingEvents);
        }
        getPanel().getLayerManager().addLayer(StandardCategoryNames.SYSTEM,
                layer);
        return layer;
    }

    protected Point getPoint() throws NoninvertibleTransformException {
        return new GeometryFactory().createPoint((Coordinate) getCoordinates()
                .get(0));
    }

    public MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        /*
         * Works only with one selected RasterImageLayer
         */
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory
                        .createWindowWithLayerViewPanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(
                        1, RasterImageLayer.class));

    }

    /*
     * TODO: if user drag on image, measures on lower panel are no more
     * displayed. Try to find a solution
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        // mouseLocationChanged(e);
    }

    /*
     * Displays cell values on system bar while moving cursor on the raster
     */
    PlugInContext gContext;

    @Override
    public void mouseMoved(MouseEvent me) {

        final WorkbenchContext wbcontext = this.getWorkbench().getContext();
        RasterImageLayer aLayer = null;
        for (java.util.Iterator i = wbcontext.getLayerNamePanel()
                .selectedNodes(RasterImageLayer.class).iterator(); i.hasNext();) {
            aLayer = (RasterImageLayer) i.next();
        }
        String cellValues = null;
        try {
            cellValues = "";
            Coordinate tentativeCoordinate = getPanel().getViewport()
                    .toModelCoordinate(me.getPoint());
            for (int b = 0; b < aLayer.getNumBands(); b++) {
                Double cellValue = aLayer.getCellValue(tentativeCoordinate.x,
                        tentativeCoordinate.y, b);
                if (cellValue != null) {
                    if (aLayer.isNoData(cellValue)) {
                        cellValues = Double.toString(Double.NaN);
                    } else {
                        cellValues = cellValues.concat(Double
                                .toString(cellValue));
                    }
                }
                cellValues = cellValues.concat("  ");
            }

        } catch (RasterDataNotFoundException ex) {
            cellValues = "???";
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoninvertibleTransformException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        name = aLayer.getName();

        // gContext.getWorkbenchContext()
        // .getLayerViewPanel()
        // .setToolTipText(
        // "(" + name + ") " + VALUE + ": "
        // + cellValues.toString());

        getPanel().getContext().setStatusMessage(
                "(" + name + ") " + VALUE + ": " + cellValues.toString());
    }
}