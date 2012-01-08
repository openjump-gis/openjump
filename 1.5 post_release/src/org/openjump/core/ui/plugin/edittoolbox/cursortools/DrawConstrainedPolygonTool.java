
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
 */

package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;

public class DrawConstrainedPolygonTool extends ConstrainedPolygonTool
{
    private FeatureDrawingUtil featureDrawingUtil;
    final static String drawConstrainedPolygon =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawConstrainedPolygonTool.Draw-Constrained-Polygon");
    
    protected DrawConstrainedPolygonTool(FeatureDrawingUtil featureDrawingUtil)
    {
        this.featureDrawingUtil = featureDrawingUtil;
    }
    
    public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy)
    {
        FeatureDrawingUtil featureDrawingUtil =
        new FeatureDrawingUtil(layerNamePanelProxy);
        
        return featureDrawingUtil.prepare(
        new DrawConstrainedPolygonTool(featureDrawingUtil),
        true);
    }
    
    public Icon getIcon()
    {
        return new ImageIcon(getClass().getResource("DrawPolygonConstrained.gif"));
    }
    
    public String getName() {                            
        return drawConstrainedPolygon;
    }
    
    protected void gestureFinished() throws Exception
    {
        reportNothingToUndoYet();
        
        if (!checkPolygon())
        {
            return;
        }
        
        featureDrawingUtil.drawRing(
        getPolygon(),
        isRollingBackInvalidEdits(),
        this,
        getPanel());       
    }
}
