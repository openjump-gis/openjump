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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JMenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Sort Categories abstract plugin
 * 
 * @author clark4444
 *
 */
public abstract class SortCategoryAbstractPlugIn extends AbstractPlugIn
		implements ActionListener {

	protected static final ImageIcon ICON = null;

	protected String menuLabelOnLayer = "Sort Selected Categories";
	protected final static String I18N_SORT_MENU_LABEL = "org.openjump.core.ui.plugin.layer.SortCategoryAbstractPlugIn.Sort-Selected-Categories";

	private String labelSelected;

	public void initialize(PlugInContext context) throws Exception {

		menuLabelOnLayer = I18N.get(I18N_SORT_MENU_LABEL);

		addMenuOptions(context);
		addActionListenersToMenu(context);
	}

	protected abstract void addMenuOptions(PlugInContext context);

	private void addActionListenersToMenu(PlugInContext context) {
		JMenu menu = context.getFeatureInstaller().menuBarMenu(MenuNames.LAYER);
		// register action listener with the menu items
		for (int j = 0; j < menu.getItemCount(); j++) {
			if (menu.getItem(j) == null)
				continue;
			if (menu.getItem(j).getText().equals(menuLabelOnLayer)) {
				JMenu submenu;
				try {
					submenu = (JMenu) menu.getItem(j);
					for (int k = 0; k < submenu.getItemCount(); k++) {
						if (submenu.getItem(k).getText().equals(
								getSubMenuLabel())) {
							submenu = (JMenu) submenu.getItem(k);
							break;
						}
					}
				} catch (ClassCastException cexc) {
					context
							.getWorkbenchContext()
							.getErrorHandler()
							.handleThrowable(
									new Exception(
											"Menuitem is an unexpected object type."));
					return;
				}
				// add listener for selection of submenu items
				for (int k = 0; k < submenu.getItemCount(); k++) {
					submenu.getItem(k).addActionListener(this);
				}
				break;
			}
		}
	}

	protected final String[] getMenuLocation(String submenuLabel) {
		return new String[] { MenuNames.LAYER, menuLabelOnLayer, submenuLabel };
	}

	protected abstract String getSubMenuLabel();

	public boolean execute(PlugInContext context) throws Exception {
		try {
			reportNothingToUndoYet(context);
			LayerManager layerManager = context.getWorkbenchContext()
					.getLayerManager();

			ArrayList<Category> selectedCategories = null;
			try {
				selectedCategories = (ArrayList) context.getWorkbenchContext()
						.getLayerNamePanel().getSelectedCategories();
			} catch (ClassCastException e) {
				context
						.getWorkbenchContext()
						.getErrorHandler()
						.handleThrowable(
								new Exception(
										"Categories is an unexpected object type."));
				return false;
			}

			try {
				// sort layers in each selected category by option
				for (Category category : selectedCategories) {
					ArrayList<Layerable> layers = getOrderedLayersInCategory(
							category, labelSelected);
					removeLayers(layerManager, layers);
					addLayers(layerManager, category, layers);
				}
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

	private void addLayers(LayerManager layerManager, Category category,
			ArrayList<Layerable> layers) {
		for (Layerable layerable : layers) {
			layerManager.addLayerable(category.getName(), layerable);
		}
	}

	private void removeLayers(LayerManager layerManager,
			ArrayList<Layerable> layers) {
		for (Layerable layerable : layers) {
			layerManager.remove(layerable);
		}
	}

	abstract ArrayList<Layerable> getOrderedLayersInCategory(Category category,
			String sortLabel);

	protected ArrayList<Layerable> getCategoryArrayList(Category category) {
		List<Layerable> categoryList = category.getLayerables();
		ArrayList<Layerable> categoryOrderedList = new ArrayList<Layerable>();

		for (Layerable layer : categoryList) {
			categoryOrderedList.add(layer);
		}

		return categoryOrderedList;
	}

	class LayerableNameSort implements Comparator<Layerable> {
		public int compare(Layerable layer1, Layerable layer2) {
			return layer2.getName().compareTo(layer1.getName());
		}
	}

	public static MultiEnableCheck createEnableCheck(
			WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(
				workbenchContext);

		return new MultiEnableCheck()
				.add(
						checkFactory
								.createWindowWithLayerNamePanelMustBeActiveCheck())
				.add(
						checkFactory
								.createAtLeastNCategoriesMustBeSelectedCheck(1));
	}

	public void actionPerformed(ActionEvent event) {
		if (event != null) {
			labelSelected = event.getActionCommand();
		}
	}

}