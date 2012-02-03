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
import java.util.Arrays;
import java.util.Iterator;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;

public class DeleteAllFeaturesPlugIn extends AbstractPlugIn {
    public DeleteAllFeaturesPlugIn() {}

    public boolean execute(final PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        ArrayList transactions = new ArrayList();
        for (Iterator i = Arrays.asList(context.getLayerNamePanel().getSelectedLayers()).iterator(); i.hasNext(); ) {
            Layer layer = (Layer) i.next();
            transactions.add(createTransaction(layer, context));
        }
        return EditTransaction.commit(transactions);
    }

    private EditTransaction createTransaction(Layer layer, PlugInContext context) {
        EditTransaction transaction = new EditTransaction(new ArrayList(), 
        getName(), layer, isRollingBackInvalidEdits(context), true, context.getWorkbenchFrame());
        for (Iterator i = layer.getFeatureCollectionWrapper().getFeatures().iterator(); i.hasNext(); ) {
            Feature feature = (Feature) i.next();
            transaction.deleteFeature(feature);
        }
        return transaction;
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()            
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1))
            .add(checkFactory.createSelectedLayersMustBeEditableCheck());
    }
}
