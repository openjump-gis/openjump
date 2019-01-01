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
package org.openjump.core.ui.plugin.mousemenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.plugin.edit.NoderPlugIn;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.*;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

/**
 * This is a mouse menu plugin activating Noder plugin with following options
 * <ul>
 * <li>update selected features</li>
 * <li>split polygons</li>
 * </ul>
 *
 * @author Micha&euml;l Michaud
 */
public class SplitLinesNodePolygonsPlugIn extends AbstractThreadedUiPlugIn {
    
    public static final ImageIcon ICON = IconLoader.icon("split_lines.png");
    
    NoderPlugIn noder = new NoderPlugIn();
    
    public SplitLinesNodePolygonsPlugIn() { }
  
    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        JPopupMenu popupMenu = context.getLayerViewPanel().popupMenu();
        featureInstaller.addPopupMenuItem(popupMenu,
            this, 
            new String[]{noder.getName()},
            getName(),
            false,
            ICON,
            this.createEnableCheck(context.getWorkbenchContext()));
    }
  
    public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
            .add(checkFactory.createExactlyOneSelectedLayerMustBeEditableCheck())
            .add(checkFactory.createAtLeastNFeaturesMustBeSelectedCheck(1));
    }

    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        
        monitor.allowCancellationRequests();
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.noding-input"));
        
        final Layer layer = context.getLayerNamePanel().chooseEditableLayer();
        
        noder.setUseSelected(true); 
        noder.setFindIntersections(false);
        noder.setLineProcessor(NoderPlugIn.Processor.SPLIT);
        noder.setPolygonProcessor(NoderPlugIn.Processor.NODE);
        noder.setInterpolateZ(true);
        noder.setInterpolatedZDp(3);
        
        noder.run(monitor, context);
        
        if (monitor.isCancelRequested()) return;
    }
  
}
