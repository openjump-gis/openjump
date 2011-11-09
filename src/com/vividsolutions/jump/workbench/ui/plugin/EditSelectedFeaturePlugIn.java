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

import java.util.Arrays;

import javax.swing.ImageIcon;

import org.openjump.core.ui.images.IconLoader;

import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.WorkbenchException;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.EnterWKTDialog;

public class EditSelectedFeaturePlugIn extends WKTPlugIn {
    
	public static ImageIcon ICON = IconLoader.icon("view_edit_geometry.png");
	
	private Feature feature;

	public EditSelectedFeaturePlugIn() {}

	protected Layer layer(PlugInContext context) {
		return (Layer) context.getLayerViewPanel().getSelectionManager()
				.getLayersWithSelectedItems().iterator().next();
	}

    public String getName() {
        return I18N.get("ui.plugin.EditSelectedFeaturePlugIn.view-edit-selected-feature");
    }
    
    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createExactlyNFeaturesMustHaveSelectedItemsCheck(1));
    }
    
    public boolean execute(PlugInContext context) throws Exception {
        return execute(
            context,
            (Feature) context
                .getLayerViewPanel()
                .getSelectionManager()
                .getFeaturesWithSelectedItems()
                .iterator()
                .next(),
            true);
    }
    
    public boolean execute(PlugInContext context, Feature feature, boolean editable)
        throws Exception {
        this.feature = feature;
        reportNothingToUndoYet(context);
        return super.execute(context);
    }
    
    protected void apply(String wkt, PlugInContext context) throws Exception {
        if (!layer(context).isEditable()) {
            return;
        }
        super.apply(wkt, context);
    }
    
    protected void apply(FeatureCollection c, PlugInContext context)
        throws WorkbenchException {
        if (c.size() != 1) {
            throw new WorkbenchException(I18N.get("ui.plugin.EditSelectedFeaturePlugIn.expected-1-feature-but-found")+" " + c.size());
        }
        EditTransaction transaction =
            new EditTransaction(
                Arrays.asList(new Feature[] { feature }),
                getName(),
                layer,
                isRollingBackInvalidEdits(context),
                false,
                context.getWorkbenchFrame());
        //Can't simply pass the LayerViewPanel to the transaction because if there is
        //an attribute viewer up and its TaskFrame has been closed, the LayerViewPanel's
        //LayerManager will be null. [Jon Aquino]
        Feature newFeature = (Feature) c.iterator().next();
        transaction.setGeometry(feature, newFeature.getGeometry());
        transaction.commit();
    }

	protected EnterWKTDialog createDialog(PlugInContext context) {
		EnterWKTDialog d = super.createDialog(context);
        d.setTitle(
            (layer(context).isEditable() ? I18N.get("ui.plugin.EditSelectedFeaturePlugIn.edit")+" " : "")
                + I18N.get("ui.plugin.EditSelectedFeaturePlugIn.feature")+" "
                + feature.getID()
                + " "+I18N.get("ui.plugin.EditSelectedFeaturePlugIn.in")+" "
                + layer
                + (layer(context).isEditable() ? "" : " ("+I18N.get("ui.plugin.EditSelectedFeaturePlugIn.layer-is-uneditable")+")"));
        d.setEditable(layer(context).isEditable());
		d.setText(helper.format(new WKTWriter(3).write(feature
				.getGeometry())));
		return d;
	}

	private WKTDisplayHelper helper = new WKTDisplayHelper();
	
    public ImageIcon getIcon() {
        return ICON;
    }
}