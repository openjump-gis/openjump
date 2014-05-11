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

package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.Collection;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.AbstractTransferable;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.DummyClipboardOwner;

/**
 * Copy a Feature Schema in the clipboard.
 */
public class CopySchemaPlugIn extends AbstractPlugIn {
	
	public static ImageIcon ICON = IconLoader.icon("schema_copy.png");	
 
    public void initialize(PlugInContext context) throws Exception {
    	    	 
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        
        JPopupMenu layerNamePopupMenu = 
            context
            .getWorkbenchContext()
            .getWorkbench()
            .getFrame()
            .getLayerNamePopupMenu();
       	        
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, this, 
        		new String[] { I18N.get("ui.MenuNames.SCHEMA") }, 
        		getName(), 
                false, 
                getIcon(),
                CopySchemaPlugIn.createEnableCheck(workbenchContext));
    }
    
    public boolean execute(PlugInContext context) throws Exception {
    	String schemaString = "";
    	Collection layerCollection = (Collection) context.getWorkbenchContext().getLayerNamePanel().selectedNodes(Layer.class);
    	Layer layer = (Layer) layerCollection.iterator().next();
    	FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
    	int numAttributes = featureSchema.getAttributeCount();
    	
    	for (int index = 0; index < numAttributes; index++) {
    		String name = featureSchema.getAttributeName(index);
    		AttributeType type = featureSchema.getAttributeType(index);
    		schemaString = schemaString + name + "\t" + type + "\n";
    	}
        
    	final String clipString = schemaString;
    	
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new AbstractTransferable(
                    new DataFlavor[] { DataFlavor.stringFlavor }) {
                    public Object getTransferData(DataFlavor flavor)
                        throws UnsupportedFlavorException, IOException {
                        return clipString;
                    }
                }, new DummyClipboardOwner());

        return true;
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
            .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
    }  
  
    public ImageIcon getIcon() {
        return ICON;
    }
    
}
