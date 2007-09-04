
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

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ImageIcon;


import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EnterWKTDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class AddNewFeaturesPlugIn extends WKTPlugIn {
    public AddNewFeaturesPlugIn() {}

    protected Layer layer(PlugInContext context) {
        return context.getLayerNamePanel().chooseEditableLayer();
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        return super.execute(context);
    }

    protected void apply(FeatureCollection c, final PlugInContext context) {
        //Can't use WeakHashMap, otherwise the features will vanish when the command
        //is undone! [Jon Aquino]
        final ArrayList features = new ArrayList();               
        
        FeatureSchema fs = this.layer.getFeatureCollectionWrapper().getFeatureSchema();
        
        for (Iterator i = c.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();      
            features.add(FeatureUtil.toFeature(feature.getGeometry(), fs));
        }

        execute(new UndoableCommand(getName()) {
            public void execute() {
                layer.getFeatureCollectionWrapper().addAll(features);
            }

            public void unexecute() {
                layer.getFeatureCollectionWrapper().removeAll(features);
            }
        }, context);
    }

    protected EnterWKTDialog createDialog(PlugInContext context) {
        EnterWKTDialog d = super.createDialog(context);
        d.setTitle(I18N.get("ui.plugin.AddNewFeaturesPlugIn.add-features-to")+" " + layer);
        d.setDescription("<HTML>"+I18N.get("ui.plugin.AddNewFeaturesPlugIn.enter-well-known-text-for-one-or-more-geometries")+"</HTML>");

        return d;

        //<<TODO:DEFECT>> Look at the points drawn. The line and the fill do not line
        //up perfectly. [Jon Aquino]
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1));
    }
    
    public static final ImageIcon ICON = IconLoader.icon("add.png");

}
