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
package com.vividsolutions.jump.workbench.ui.plugin.test;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.geom.GeometryMicroscope;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
public class MicroscopePlugIn extends AbstractPlugIn {
    public MicroscopePlugIn() {
    }
    public void initialize(PlugInContext context) throws Exception {
        EnableCheckFactory checkFactory =
            new EnableCheckFactory(context.getWorkbenchContext());
        context
            .getFeatureInstaller()
			.addMainMenuItemWithJava14Fix(
                this,
                    new String[] { MenuNames.TOOLS, "Test" },
                    getName(),
                    false,
                    null,
                    new MultiEnableCheck()
                        .add(
                            checkFactory
                                .createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1))
                        .add(
                            checkFactory
                                .createWindowWithLayerViewPanelMustBeActiveCheck())
                        .add(checkFactory.createFenceMustBeDrawnCheck()));
    }
    public boolean execute(PlugInContext context) throws Exception {
        FeatureCollection fc = context.getSelectedLayer(0).getFeatureCollectionWrapper();
        Envelope fence = context.getLayerViewPanel().getFence().getEnvelopeInternal();
        FeatureCollection magFC = magnify(fc, fence);
        Layer lyr = context.addLayer(StandardCategoryNames.QA, "Microscope", magFC);
        lyr.getBasicStyle().setFillColor(Color.red);
        lyr.getBasicStyle().setLineColor(Color.red);
        lyr.getBasicStyle().setAlpha(100);
        lyr.getVertexStyle().setEnabled(true);
        lyr.fireAppearanceChanged();
        return true;
    }
    private FeatureCollection magnify(FeatureCollection fc, Envelope env) {
        List geomList = new ArrayList();
        for (Iterator i = fc.query(env).iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            geomList.add(feature.getGeometry().clone());
        }
        //double minSep = 5.0;
        double minSep = env.getWidth() / 20; // kluge
        GeometryMicroscope micro = new GeometryMicroscope(geomList, env, minSep);
        List result = micro.getAdjusted();
        return FeatureDatasetFactory.createFromGeometry(result);
    }
}
