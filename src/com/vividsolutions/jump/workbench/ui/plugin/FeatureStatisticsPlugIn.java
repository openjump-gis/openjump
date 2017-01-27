
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


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;


/**
 * Computes various statistics for selected layers.
 */
public class FeatureStatisticsPlugIn extends AbstractPlugIn {

    private static final String nPtsAttr   = "nPts";
    private static final String nHolesAttr = "nHoles";
    private static final String nCompsAttr = "nComponents";
    private static final String areaAttr   = "area";
    private static final String lengthAttr = "length";
    private static final String typeAttr   = "type";

    public FeatureStatisticsPlugIn() {
    }

    public void initialize(PlugInContext context) throws Exception
    {
        	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    		featureInstaller.addMainMenuPlugin(
    	        this,
  				new String[] {MenuNames.TOOLS, MenuNames.STATISTICS},
                this.getName() + "...",
                false,			//checkbox
                null,			//icon
                createEnableCheck(context.getWorkbenchContext()));
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
    private static FeatureSchema getStatisticsSchema() {
        FeatureSchema featureSchema = new FeatureSchema();
        featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        featureSchema.addAttribute(nPtsAttr, AttributeType.INTEGER);
        featureSchema.addAttribute(nHolesAttr, AttributeType.INTEGER);
        featureSchema.addAttribute(nCompsAttr, AttributeType.INTEGER);
        featureSchema.addAttribute(areaAttr, AttributeType.DOUBLE);
        featureSchema.addAttribute(lengthAttr, AttributeType.DOUBLE);
        featureSchema.addAttribute(typeAttr, AttributeType.STRING);

        return featureSchema;
    }

    public boolean execute(PlugInContext context) throws Exception {
        //Call #getSelectedLayers before #clear, because #clear will surface
        //output window. [Jon Aquino]
        Layer[] selectedLayers = context.getSelectedLayers();

        for (Layer selectedLayer : selectedLayers) {
            featureStatistics(selectedLayer, context);
        }

        return true;
    }

    private void featureStatistics(final Layer layer, PlugInContext context) {
        FeatureSchema statsSchema = getStatisticsSchema();
        FeatureDataset statsFC = new FeatureDataset(statsSchema);

        for (Feature feature : layer.getFeatureCollectionWrapper().getFeatures()) {

            Geometry g = feature.getGeometry();

            // these both need work - need to recurse into geometries
            // work done by mmichaud on 2010-12-12
            int[] comps_and_holes = new int[]{0,0};
            comps_and_holes = recurse(g, comps_and_holes);

            Feature statsf = new BasicFeature(statsSchema);

            // this aliases the geometry of the input feature, but this shouldn't matter,
            // since if geometries are edited they should be completely replaced
            statsf.setAttribute("GEOMETRY", g);
            statsf.setAttribute(nPtsAttr,   g.getCoordinates().length);
            statsf.setAttribute(nCompsAttr, comps_and_holes[0]);
            statsf.setAttribute(nHolesAttr, comps_and_holes[1]);
            statsf.setAttribute(areaAttr,   g.getArea());
            statsf.setAttribute(lengthAttr, g.getLength());
            statsf.setAttribute(typeAttr,   g.getGeometryType());
            statsFC.add(statsf);
        }

        Layer statsLayer = context.addLayer(StandardCategoryNames.QA,
                "Statistics-" + layer.getName(), statsFC);
        statsLayer.setStyles(layer.cloneStyles());
    }
    
    private int[] recurse(Geometry g, int[] comps_holes) {
        if (g instanceof GeometryCollection) {
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                comps_holes = recurse(g.getGeometryN(i), comps_holes);
            }
        }
        else {
            comps_holes[0]++;
            if (g instanceof Polygon) {
                comps_holes[1] += ((Polygon)g).getNumInteriorRing();
            }
        }
        return comps_holes;
    }
}
