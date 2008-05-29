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

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Map;

import javax.swing.JPanel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchException;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;
import com.vividsolutions.wms.WMService;


public class MapLayerWizardPanel extends JPanel implements WizardPanel {
    public static final String LAYERS_KEY = "LAYERS";
    public static final String COMMON_SRS_LIST_KEY = "COMMON_SRS_LIST";
    public static final String FORMAT_LIST_KEY = "FORMAT_LIST";
    public final static String INITIAL_LAYER_NAMES_KEY = "INITIAL_LAYER_NAMES";
    public final static String NO_COMMON_SRS_MESSAGE = I18N.get("ui.plugin.wms.MapLayerWizardPanel.the-chosen-layers-do-not-have-a-common-epsg-coordinate-reference-system");
    private MapLayerPanel addRemovePanel = new MapLayerPanel();
    private Map dataMap;
    private String nextID = SRSWizardPanel.class.getName();
    private BorderLayout borderLayout1 = new BorderLayout();

    public MapLayerWizardPanel() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(InputChangedListener listener) {
        addRemovePanel.add(listener);
    }

    public void remove(InputChangedListener listener) {
        addRemovePanel.remove(listener);
    }

    public String getInstructions() {
        //    return "Please choose the WMS layers that should appear on the image. You " +
        //    "can change the ordering of the WMS layers using the up and down buttons.";
        return I18N.get("ui.plugin.wms.MapLayerWizardPanel.please-choose-the-wms-layers-that-should-appear-on-the-image");
    }

    public void exitingToRight() throws WorkbenchException {
        dataMap.put(LAYERS_KEY, addRemovePanel.getChosenMapLayers());

        if (addRemovePanel.commonSRSList().isEmpty()) {
            throw new WorkbenchException(NO_COMMON_SRS_MESSAGE);
        }

        dataMap.put(COMMON_SRS_LIST_KEY, addRemovePanel.commonSRSList());
        if (addRemovePanel.commonSRSList().size() == 1) {
            nextID = OneSRSWizardPanel.class.getName();
        } else {
            nextID = SRSWizardPanel.class.getName();
        }
    }

    public void enteredFromLeft(Map dataMap) {
        this.dataMap = dataMap;
        addRemovePanel.init((WMService) dataMap.get(URLWizardPanel.SERVICE_KEY),
            (Collection) dataMap.get(INITIAL_LAYER_NAMES_KEY));
    }

    public String getTitle() {
        return I18N.get("ui.plugin.wms.MapLayerWizardPanel.choose-wms-layers");
    }

    public String getID() {
        return getClass().getName();
    }

    public boolean isInputValid() {
        return !addRemovePanel.getChosenMapLayers().isEmpty();
    }

    public String getNextID() {
        return nextID;
    }

    private void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        add(addRemovePanel, BorderLayout.CENTER);
    }
}
