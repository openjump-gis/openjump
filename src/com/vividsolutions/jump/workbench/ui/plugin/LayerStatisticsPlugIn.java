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

package com.vividsolutions.jump.workbench.ui.plugin;

import java.util.Iterator;

import org.openjump.sextante.gui.additionalResults.AdditionalResults;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Computes various statistics for selected layers.
 */
public class LayerStatisticsPlugIn extends AbstractPlugIn {

    public LayerStatisticsPlugIn() {
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        final FeatureInstaller featureInstaller = new FeatureInstaller(
                context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(this, new String[] {
                MenuNames.TOOLS, MenuNames.STATISTICS }, getName() + "...",
                false, // checkbox
                null, // icon
                createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        // Call #getSelectedLayers before #clear, because #clear will surface
        // output window. [Jon Aquino]
        final Layer[] selectedLayers = context.getSelectedLayers();
        // HTMLFrame out = context.getOutputFrame();
        final HTMLPanel out = new HTMLPanel();
        out.setRecordNavigationControlVisible(false);
        out.createNewDocument();
        out.addHeader(1,
                I18N.get("ui.plugin.LayerStatisticsPlugIn.layer-statistics"));

        final LayerStatistics totalStats = new LayerStatistics();
        final Envelope totalEnv = new Envelope();

        for (final Layer layer : selectedLayers) {
            final LayerStatistics ls = layerStatistics(layer, totalStats);

            out.addHeader(2, I18N.get("ui.plugin.LayerStatisticsPlugIn.layer")
                    + " " + layer.getName());

            final Envelope layerEnv = layer.getFeatureCollectionWrapper()
                    .getEnvelope();
            out.addField(I18N.get("ui.plugin.LayerStatisticsPlugIn.envelope"),
                    layerEnv.toString());
            totalEnv.expandToInclude(layerEnv);
            output(ls, out);
        }

        if (selectedLayers.length > 1) {
            out.addHeader(
                    2,
                    I18N.get("ui.plugin.LayerStatisticsPlugIn.summary-for-all-layers"));
            out.addField(I18N.get("ui.plugin.LayerStatisticsPlugIn.envelope"),
                    totalEnv.toString());
            output(totalStats, out);
        }

        AdditionalResults.addAdditionalResultAndShow(getName(), out);

        // out.surface();

        return true;
    }

    private LayerStatistics layerStatistics(final Layer layer,
            LayerStatistics totalStats) {
        final LayerStatistics ls = new LayerStatistics();

        for (final Iterator i = layer.getFeatureCollectionWrapper().iterator(); i
                .hasNext();) {
            final Feature f = (Feature) i.next();
            final Geometry g = f.getGeometry();
            final double area = g.getArea();
            final double length = g.getLength();

            // these both need work - need to recurse into geometries
            // work done by mmichaud on 2010-12-12
            int[] comps_and_holes = new int[] { 0, 0 };
            comps_and_holes = recurse(g, comps_and_holes);
            final int comps = comps_and_holes[0];
            final int holes = comps_and_holes[1];

            final Coordinate[] pts = g.getCoordinates();

            ls.addFeature(pts.length, holes, comps, area, length);
            totalStats.addFeature(pts.length, holes, comps, area, length);
        }

        return ls;
    }

    private int[] recurse(Geometry g, int[] comps_holes) {
        if (g instanceof GeometryCollection) {
            for (int i = 0; i < g.getNumGeometries(); i++) {
                comps_holes = recurse(g.getGeometryN(i), comps_holes);
            }
        } else {
            comps_holes[0]++;
            if (g instanceof Polygon) {
                comps_holes[1] += ((Polygon) g).getNumInteriorRing();
            }
        }
        return comps_holes;
    }

    public void output(LayerStatistics ls, HTMLPanel out) {
        // ========= Output ===============
        out.addField("# Features:", ls.featureCount + "");

        out.append("<table border='1'>");
        out.append("<tr><td bgcolor=#CCCCCC>&nbsp;</td><td  bgcolor=#CCCCCC align='center'> Min </td><td  bgcolor=#CCCCCC align='center'> Max </td><td  bgcolor=#CCCCCC align='center'> "
                + I18N.get("ui.plugin.LayerStatisticsPlugIn.avg")
                + " </td><td  bgcolor=#CCCCCC align='center'> Total </td></tr>");
        out.append("<tr><td bgcolor=#CCCCCC> Pts </td><td align='right'>"
                + ls.minCoord + "</td><td align='right'>" + ls.maxCoord
                + "</td><td align='right'>" + ls.avgCoord()
                + "</td><td align='right'>" + ls.totalCoord + "</td></tr>");
        out.append("<tr><td bgcolor=#CCCCCC> "
                + I18N.get("ui.plugin.LayerStatisticsPlugIn.holes")
                + " </td><td align='right'>" + ls.minHoles
                + "</td><td align='right'>" + ls.maxHoles
                + "</td><td align='right'>" + ls.avgHoles()
                + "</td><td align='right'>" + ls.totalHoles + "</td></tr>");
        out.append("<tr><td bgcolor=#CCCCCC> "
                + I18N.get("ui.plugin.LayerStatisticsPlugIn.components")
                + " </td><td align='right'>" + ls.minComp
                + "</td><td align='right'>" + ls.maxComp
                + "</td><td align='right'>" + ls.avgComp()
                + "</td><td align='right'>" + ls.totalComp + "</td></tr>");
        out.append("<tr><td bgcolor=#CCCCCC> "
                + I18N.get("ui.plugin.LayerStatisticsPlugIn.area")
                + " </td><td align='right'>" + ls.minArea
                + "</td><td align='right'>" + ls.maxArea
                + "</td><td align='right'>" + ls.avgArea()
                + "</td><td align='right'>" + ls.totalArea + "</td></tr>");
        out.append("<tr><td bgcolor=#CCCCCC> "
                + I18N.get("ui.plugin.LayerStatisticsPlugIn.length")
                + " </td><td align='right'>" + ls.minLength
                + "</td><td align='right'>" + ls.maxLength
                + "</td><td align='right'>" + ls.avgLength()
                + "</td><td align='right'>" + ls.totalLength + "</td></tr>");
        out.append("</table>");
    }

    private class LayerStatistics {
        boolean isFirst = true;
        int minCoord = 0;
        int maxCoord = 0;
        int totalCoord = 0;
        int minComp = 0;
        int maxComp = 0;
        int totalComp = 0;
        int minHoles = 0;
        int maxHoles = 0;
        int totalHoles = 0;
        double minArea = 0.0;
        double maxArea = 0.0;
        double totalArea = 0.0;
        double minLength = 0.0;
        double maxLength = 0.0;
        double totalLength = 0.0;
        int featureCount = 0;

        private void addFeature(int coordCount, int holeCount, int compCount,
                double area, double length) {
            featureCount++;

            if (isFirst || (coordCount < minCoord)) {
                minCoord = coordCount;
            }

            if (isFirst || (coordCount > maxCoord)) {
                maxCoord = coordCount;
            }

            totalCoord += coordCount;

            if (isFirst || (holeCount < minHoles)) {
                minHoles = holeCount;
            }

            if (isFirst || (holeCount > maxHoles)) {
                maxHoles = holeCount;
            }

            totalHoles += holeCount;

            if (isFirst || (compCount < minComp)) {
                minComp = compCount;
            }

            if (isFirst || (compCount > maxComp)) {
                maxComp = compCount;
            }

            totalComp += compCount;

            if (isFirst || (area < minArea)) {
                minArea = area;
            }

            if (isFirst || (area > maxArea)) {
                maxArea = area;
            }

            totalArea += area;

            if (isFirst || (length < minLength)) {
                minLength = length;
            }

            if (isFirst || (length > maxLength)) {
                maxLength = length;
            }

            totalLength += length;

            isFirst = false;
        }

        private double avgCoord() {
            return (featureCount == 0) ? 0.0
                    : ((double) totalCoord / featureCount);
        }

        private double avgHoles() {
            return (featureCount == 0) ? 0.0
                    : ((double) totalHoles / featureCount);
        }

        private double avgComp() {
            return (featureCount == 0) ? 0.0
                    : ((double) totalComp / featureCount);
        }

        private double avgArea() {
            return (featureCount == 0) ? 0.0 : (totalArea / featureCount);
        }

        private double avgLength() {
            return (featureCount == 0) ? 0.0 : (totalLength / featureCount);
        }
    }
}
