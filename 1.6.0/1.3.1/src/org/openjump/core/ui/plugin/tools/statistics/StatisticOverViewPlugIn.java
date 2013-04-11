/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */
package org.openjump.core.ui.plugin.tools.statistics;

import javax.swing.ImageIcon;

import org.openjump.core.apitools.FeatureCollectionTools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;


/**
 * PlugIn that gives a quick impression on the value ranges, means (modes) and the deviation of the layer features' 
 * attribute values.
 *
 * @author Ole Rahn, sstein
 * modified: [sstein] 16.Feb.2009 adaptation for OpenJUMP
 */
public class StatisticOverViewPlugIn extends AbstractPlugIn{
    
    private WorkbenchContext workbenchContext;
    
    String sStatisticsOverview=I18N.get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewPlugIn.Attribute-Statistics-Overview");
	
    public void initialize(PlugInContext context) throws Exception
    {     
        workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuItemWithJava14Fix(
        		this, 
				new String[] { MenuNames.TOOLS, MenuNames.STATISTICS}, 
				this.getName() + "...", 
				false, 
				null, 
				this.createEnableCheck(workbenchContext));
    }
    
    public String getName() {
        return sStatisticsOverview;
    }
    
    /**
     *@inheritDoc
     */
    public String getIconString() {
        return "statsOverview.png";
    }
    
    /**
     *@inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        
        Layer layer = context.getSelectedLayer(0);
        /* -- [sstein] not needed due to enableCheck settings 
        if (layer==null){
            context.getWorkbenchFrame().warnUser(I18N.get("no-layer-selected"));
        }
        */
        FeatureCollection featureColl = layer.getFeatureCollectionWrapper().getWrappee();
        Feature[] features = FeatureCollectionTools.FeatureCollection2FeatureArray(featureColl);
        /* -- [sstein] not needed as we get the stats for the full layer
        if (features==null || features.length==0){
        	context.getWorkbenchFrame().warnUser(I18N.get("no-features-in-selection"));
        }
        */
        StatisticOverViewDialog dialog =  new StatisticOverViewDialog(context.getWorkbenchFrame(), this.getName(), false, features);
        
        dialog.setVisible(true);
        
        return true;
    }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) 
    {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
                                     .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
    } 
}
