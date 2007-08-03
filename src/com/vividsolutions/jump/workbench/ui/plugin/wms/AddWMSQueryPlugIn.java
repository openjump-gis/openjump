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
package com.vividsolutions.jump.workbench.ui.plugin.wms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;


public class AddWMSQueryPlugIn extends AbstractPlugIn {
    
    private String cachedURL = "http://demo.deegree.org/deegree-wms/services";
    private String lastWMSVersion = WMService.WMS_1_1_1;
    
    private static final String CACHED_URL = "AddWMSQueryPlugin.CACHED_URL";

    private List toLayerNames(List mapLayers) {
        ArrayList names = new ArrayList();
        for (Iterator i = mapLayers.iterator(); i.hasNext();) {
            MapLayer layer = (MapLayer) i.next();
            names.add(layer.getName());
        }

        return names;
    }

    public boolean execute(final PlugInContext context)
        throws Exception {
	String s = (String)PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).get(CACHED_URL);
	if(s != null) cachedURL = s;
	
        reportNothingToUndoYet(context);

        WizardDialog d = new WizardDialog(context.getWorkbenchFrame(),
        		I18N.get("ui.plugin.wms.AddWMSQueryPlugIn.connect-to-web-map-server"), context.getErrorHandler());

        d.init(new WizardPanel[] {
                new URLWizardPanel(cachedURL, lastWMSVersion), new MapLayerWizardPanel(),
                new SRSWizardPanel(), new OneSRSWizardPanel()
            });

        //Set size after #init, because #init calls #pack. [Jon Aquino]
        d.setSize(500, 400);
        GUIUtil.centreOnWindow(d);
        d.setVisible(true);
        if (!d.wasFinishPressed()) {
            return false;
        }

        // title of the layer will be the title of the first WMS layer
        String title = ((MapLayer)((List)d.getData(MapLayerWizardPanel.LAYERS_KEY)).get(0)).getTitle();
        
        final WMSLayer layer = new WMSLayer(title,context.getLayerManager(),
                (WMService) d.getData(URLWizardPanel.SERVICE_KEY),
                (String) d.getData(SRSWizardPanel.SRS_KEY),
                toLayerNames((List) d.getData(MapLayerWizardPanel.LAYERS_KEY)),
                ((String) d.getData(URLWizardPanel.FORMAT_KEY)));
        execute(new UndoableCommand(getName()) {
                public void execute() {
                    Collection selectedCategories = context.getLayerNamePanel()
                                                           .getSelectedCategories();
                    context.getLayerManager().addLayerable(selectedCategories.isEmpty()
                        ? StandardCategoryNames.WORKING
                        : selectedCategories.iterator().next().toString(), layer);
                }

                public void unexecute() {
                    context.getLayerManager().remove(layer);
                }
            }, context);
        cachedURL = (String) d.getData(URLWizardPanel.URL_KEY);
        lastWMSVersion = (String) d.getData( URLWizardPanel.VERSION_KEY );

        PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).put(CACHED_URL, cachedURL);
        
        return true;
    }
}
