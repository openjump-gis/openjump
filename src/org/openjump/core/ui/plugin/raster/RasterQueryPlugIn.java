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
 * www.ashs.isa.com
 */

package org.openjump.core.ui.plugin.raster;

import javax.swing.Icon;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class RasterQueryPlugIn extends AbstractPlugIn {

    /*
     * [2013_05_27] Giuseppe Aruta Simple plugin that allows to inspect raster cell value for
     *  DTM ver 0.1 2013_05_27
     * 
     * [2014_01_24] Giuseppe Aruta - Extended inspection to multiband raster
     *  layers. Now multiple measure are displayed (and saved) by default. Press
     *  SHIFT to display only last measure. Moving cursor on image shows raster
     *  cell value on lower panel
     */
    private final static String sErrorSeeOutputWindow = I18N
            .get("org.openjump.core.ui.plugin.tools.MeasureM_FPlugIn.Error-see-output-window");

    public void initialize(PlugInContext context) throws Exception {

        context.getFeatureInstaller()
                .addMainMenuPlugin(
                        this,
                        new String[] { MenuNames.RASTER },
                        // new String[] {MenuNames.PLUGINS,
                        // I18NPlug.getI18N("RasterInfo_Extension")},
                        I18N.get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn"),
                        false, getIcon(),
                        createEnableCheck(context.getWorkbenchContext()));

    }

    public boolean execute(PlugInContext context) throws Exception {
        try {

            context.getLayerViewPanel().setCurrentCursorTool(
                    new RasterQueryCursorTool());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            context.getWorkbenchFrame().warnUser(sErrorSeeOutputWindow);
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame().getOutputFrame()
                    .addText("MeasureM_FPlugIn Exception:" + e.toString());
            return false;
        }
    }

    /*
     * private Icon getIcon() {
     * 
     * return IconLoader.icon("Raster_Info.png"); }
     */
    public Icon getIcon() {
        return IconLoader.icon("grid_info.png");
    }

    public MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck().add(
                checkFactory.createTaskWindowMustBeActiveCheck()).add(
                checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class));
    }
}
