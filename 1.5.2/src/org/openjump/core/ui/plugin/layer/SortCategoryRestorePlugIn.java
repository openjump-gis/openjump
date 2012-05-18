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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Restore layers from the saved location
 * 
 * @author clark4444
 *
 */
public class SortCategoryRestorePlugIn extends AbstractPlugIn {

	static private WorkbenchContext workbenchContext = null;
	private static final ImageIcon ICON = null;

	private String menuLabel = "Restore";

	public void initialize(PlugInContext context) throws Exception {

		menuLabel = I18N
				.get("org.openjump.core.ui.plugin.layer.SortCategoryRestorePlugIn.Restore");

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
			LayerManager layerManager = context.getWorkbenchContext()
					.getLayerManager();

			List<Layerable> allLayerables = context.getWorkbenchContext()
					.getLayerNamePanel().getLayerManager().getLayerables(
							Layerable.class);

			Map<LayerableLocation, Layerable> saved = new TreeMap<LayerableLocation, Layerable>(
					Collections.reverseOrder());
			List<Layerable> unsaved = new ArrayList<Layerable>();

			getSavedUnsavedLayerables(saved, unsaved, allLayerables);
			Map<Layerable, String> layerableToCategory = getLayerableToCategory(
					unsaved, layerManager.getCategories());

			try {
				removeLayers(layerManager, allLayerables);

				// add unsaved to end, saved to beginning
				addUnSavedBack(layerManager, allLayerables, unsaved,
						layerableToCategory);
				addSavedBack(layerManager, allLayerables, saved);

			} finally {
				// context.getLayerManager().setFiringEvents(firingEvents);
				context.getLayerViewPanel().repaint();
				context.getWorkbenchFrame().repaint();
			}
			return true;
		} catch (Exception e) {
			context.getWorkbenchFrame().warnUser("Error: see output window");
			context.getWorkbenchFrame().getOutputFrame().createNewDocument();
			context.getWorkbenchFrame().getOutputFrame().addText(
					getName() + " PlugIn Exception:" + e.toString());
			return false;

		}
	}

	private Map<Layerable, String> getLayerableToCategory(
			List<Layerable> unsaved, List<Category> categories) {

		Map<Layerable, String> layerableToCategory = new HashMap<Layerable, String>();

		for (Layerable layerable : unsaved) {
			for (Category category : categories) {
				if (category.contains(layerable)) {
					layerableToCategory.put(layerable, category.getName());
					break;
				} else
					continue;
			}
		}

		return layerableToCategory;
	}

	private void addSavedBack(LayerManager layerManager,
			List<Layerable> allLayerables,
			Map<LayerableLocation, Layerable> saved) {

		for (Layerable layerable : saved.values()) {
			if (layerable.getBlackboard().get(
					SortCategorySavePlugIn.BLACKBOARD_CATEGORY) != null)
				layerManager.addLayerable((String) layerable.getBlackboard()
						.get(SortCategorySavePlugIn.BLACKBOARD_CATEGORY),
						layerable);
		}
	}

	private void addUnSavedBack(LayerManager layerManager,
			List<Layerable> allLayerables, List<Layerable> unsaved,
			Map<Layerable, String> layerableToCategory) {

		Collections.reverse(unsaved);

		for (Layerable layerable : unsaved) {
			layerManager.addLayerable(layerableToCategory.get(layerable),
					layerable);

		}
	}

	private void getSavedUnsavedLayerables(
			Map<LayerableLocation, Layerable> saved, List<Layerable> unsaved,
			List<Layerable> allLayerables) {

		for (Layerable layerable : allLayerables) {
			if (layerable.getBlackboard().get(
					SortCategorySavePlugIn.BLACKBOARD_CATEGORY) != null
					&& layerable != null) {
				saved.put(new LayerableLocation((String) layerable
						.getBlackboard().get(
								SortCategorySavePlugIn.BLACKBOARD_CATEGORY),
						layerable.getBlackboard().getInt(
								SortCategorySavePlugIn.BLACKBOARD_LAYER)),
						layerable);
			} else if (layerable != null) {
				unsaved.add(layerable);
			} else
				throw new IllegalStateException("Unknown layerable");
		}
	}

	private void removeLayers(LayerManager layerManager, List<Layerable> layers) {
		for (Layerable layerable : layers) {
			layerManager.remove(layerable);
		}
	}

	static public EnableCheck createSaveCategorySectionMustExistCheck() {
		return new EnableCheck() {
			public String check(JComponent component) {
				boolean notSaved = true;
				Collection layerCollection = (Collection) workbenchContext
						.getLayerNamePanel().getLayerManager().getLayerables(
								Layerable.class);
				for (Iterator i = layerCollection.iterator(); i.hasNext();) {
					Layerable layer = (Layerable) i.next();
					if (layer.getBlackboard().get(
							SortCategorySavePlugIn.BLACKBOARD_LAYER) != null)
						notSaved = false;

				}
				return (((notSaved))) ? "Use Save Category first." : null;
			}
		};
	}

	public static MultiEnableCheck createEnableCheck(
			WorkbenchContext workbenchContext) {
		return new MultiEnableCheck().add(SortCategoryRestorePlugIn
				.createSaveCategorySectionMustExistCheck());
	}

	class LayerableLocation implements Comparable<LayerableLocation> {

		private String category;
		private Integer position;

		LayerableLocation(String category, Integer position) {
			this.category = category;
			this.position = position;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public Integer getPosition() {
			return position;
		}

		public void setPosition(Integer position) {
			this.position = position;
		}

		public int compareTo(LayerableLocation location) {
			if (category.compareTo(location.getCategory()) == 0)
				return position.compareTo(location.getPosition());
			else
				return category.compareTo(location.getCategory());
		}

	}

}
