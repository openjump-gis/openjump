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

import java.util.List;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Save current category and layer location
 * 
 * @author clark4444
 * 
 */
public class SortCategorySavePlugIn extends AbstractPlugIn {

	private static final ImageIcon ICON = null;
	private static String menuLabel = "Save";

	// no I18N needed
	final protected static String BLACKBOARD_LAYER = "SortCategorySavePlugIn_Layer_Location";
	static final String BLACKBOARD_CATEGORY = "SortCategorySavePlugIn_Category_Location";

	public void initialize(PlugInContext context) throws Exception {

		menuLabel = I18N
				.get("org.openjump.core.ui.plugin.layer.SortCategorySavePlugIn.Save");

		context
				.getFeatureInstaller()
				.addMainMenuItem(
						this,
						new String[] {
								MenuNames.LAYER,
								I18N
										.get(SortCategoryAbstractPlugIn.I18N_SORT_MENU_LABEL) },
						menuLabel, false, ICON, null);
	}

	public boolean execute(PlugInContext context) throws Exception {
		try {
			List<Category> categories = context.getWorkbenchContext()
					.getLayerNamePanel().getLayerManager().getCategories();
			for (Category category : categories) {
				saveLayerLocation(category);
			}

			return true;
		} catch (Exception e) {
			context.getWorkbenchFrame().warnUser("Error: see output window");
			context.getWorkbenchFrame().getOutputFrame().createNewDocument();
			context.getWorkbenchFrame().getOutputFrame().addText(
					getName() + "PlugIn Exception:" + e.toString());
			return false;
		}
	}

	private void saveLayerLocation(Category category) {
		List<Layerable> layerables = category.getLayerables();
		int count = 0;
		for (Layerable layerable : layerables) {
			layerable.getBlackboard().put(BLACKBOARD_LAYER, count++);
			layerable.getBlackboard().put(BLACKBOARD_CATEGORY,
					category.getName());
		}
	}
}
