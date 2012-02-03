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

package org.openjump.core.ui.plugin.layer;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * Sort categories by name
 * 
 * @author clark4444
 *
 */
public class SortCategoryByNamePlugIn extends SortCategoryAbstractPlugIn implements ActionListener {

	private static final ImageIcon ICON = null;
	
	private String submenuLabel = "By Name";
	private String descending = "Descending";
	private String ascending = "Ascending";
		
	@Override
	protected void addMenuOptions(PlugInContext context) {

		this.submenuLabel = I18N
				.get("org.openjump.core.ui.plugin.layer.SortCategoryByNamePlugIn.By-Name");
		this.descending = I18N
				.get("org.openjump.core.ui.plugin.layer.SortCategoryByNamePlugIn.Descending");
		this.ascending = I18N
				.get("org.openjump.core.ui.plugin.layer.SortCategoryByNamePlugIn.Ascending");

//addMainMenuItemWithJava14Fix causing error with ja_JP language, but worked with others
//		context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
		context.getFeatureInstaller().addMainMenuItem(this,
				getMenuLocation(submenuLabel), ascending, false, ICON,
				createEnableCheck(context.getWorkbenchContext()));
		context.getFeatureInstaller().addMainMenuItem(this,
				getMenuLocation(submenuLabel), descending, false, ICON,
				createEnableCheck(context.getWorkbenchContext()));
	}

	@Override
	protected String getSubMenuLabel() {
		return submenuLabel;
	}
	
	@Override
	ArrayList<Layerable> getOrderedLayersInCategory(Category category, String sortLabel) {
		ArrayList<Layerable> layers = getCategoryArrayList(category);

		if (sortLabel.equals(ascending)) {
			Collections.sort(layers, new LayerableNameSort());			
		}
		else if (sortLabel.equals(descending)) {
			Collections.sort(layers, Collections.reverseOrder(new LayerableNameSort())); 
		}
		else 
			throw new IllegalStateException();
		
		return layers;
	}
	
	class LayerableNameSort implements Comparator<Layerable> {
		public int compare(Layerable layer1, Layerable layer2) {
			return layer2.getName().compareTo(layer1.getName());
		}
	}

}
