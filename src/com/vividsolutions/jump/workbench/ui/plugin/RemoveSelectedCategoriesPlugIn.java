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

package com.vividsolutions.jump.workbench.ui.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.OrderedMap;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;


public class RemoveSelectedCategoriesPlugIn extends AbstractPlugIn {
    public RemoveSelectedCategoriesPlugIn() {
    }

    private Category pickUnselectedCategory(LayerNamePanel layerNamePanel,
        LayerManager layerManager) {
        Collection selectedCategories = layerNamePanel.getSelectedCategories();
        Category workingCategory = layerManager.getCategory(StandardCategoryNames.WORKING);

        if ((workingCategory != null) &&
                !selectedCategories.contains(workingCategory)) {
            return workingCategory;
        }

        for (Iterator i = layerManager.getCategories().iterator(); i.hasNext();) {
            Category category = (Category) i.next();

            if (!selectedCategories.contains(category)) {
                return category;
            }
        }

        return null;
    }

    public boolean execute(PlugInContext context) throws Exception {
        execute(toCategorySpecToLayerablesMap(toOrderedCategories(
                    context.getLayerNamePanel().getSelectedCategories())),
            pickUnselectedCategory(context.getLayerNamePanel(),
                context.getLayerManager()), context);

        return true;
    }

    private List toOrderedCategories(Collection unorderedCategories) {
        ArrayList orderedCategories = new ArrayList(unorderedCategories);
        Collections.sort(orderedCategories,
            new Comparator() {
                public int compare(Object o1, Object o2) {
                    Category c1 = (Category) o1;
                    Category c2 = (Category) o2;

                    return new Integer(c1.getLayerManager().indexOf(c1)).compareTo(new Integer(
                            c2.getLayerManager().indexOf(c2)));
                }
            });

        return orderedCategories;
    }

    private OrderedMap toCategorySpecToLayerablesMap(
        List selectedCategoriesInOrder) {
        //Need OrderedMap so that categories get re-inserted in the correct order.
        //[Jon Aquino]
        OrderedMap map = new OrderedMap();

        for (Iterator i = selectedCategoriesInOrder.iterator(); i.hasNext();) {
            Category category = (Category) i.next();

            //new ArrayList because #getLayers returns a view of the category's
            //layers, which will be cleared. [Jon Aquino]
            map.put(new CategorySpec(category.getName(),
                    category.getLayerManager().indexOf(category)),
                new ArrayList(category.getLayerables()));
        }

        return map;
    }

    private void execute(final OrderedMap originalCategorySpecToLayerablesMap,
        final Category newCategory, final PlugInContext context)
        throws Exception {
        execute(new UndoableCommand(getName()) {
                public void execute() {
                    for (Iterator i = originalCategorySpecToLayerablesMap.keyList()
                                                                         .iterator();
                            i.hasNext();) {
                        final CategorySpec originalCategorySpec = (CategorySpec) i.next();
                        List layers = (List) originalCategorySpecToLayerablesMap.get(originalCategorySpec);

                        for (Iterator j = layers.iterator(); j.hasNext();) {
                            final Layerable layerable = (Layerable) j.next();
                            context.getLayerManager().remove(layerable);
                            context.getLayerManager().addLayerable(newCategory.getName(),
                                layerable);
                        }

                        context.getLayerManager().removeIfEmpty(context.getLayerManager()
                                                                       .getCategory(originalCategorySpec.name));
                    }
                }

                public void unexecute() {
                    for (Iterator i = originalCategorySpecToLayerablesMap.keyList()
                                                                         .iterator();
                            i.hasNext();) {
                        final CategorySpec originalCategorySpec = (CategorySpec) i.next();
                        List layers = (List) originalCategorySpecToLayerablesMap.get(originalCategorySpec);
                        Assert.isTrue(null == context.getLayerManager()
                                                     .getCategory(originalCategorySpec.name));
                        context.getLayerManager().addCategory(originalCategorySpec.name,
                            originalCategorySpec.index);

                        for (Iterator j = layers.iterator(); j.hasNext();) {
                            final Layerable layerable = (Layerable) j.next();
                            Assert.isTrue(context.getLayerManager().getCategory(layerable) == newCategory);
                            context.getLayerManager().remove(layerable);
                            context.getLayerManager().addLayerable(originalCategorySpec.name,
                                layerable);
                        }
                    }
                }
            }, context);
    }

    public MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                                     .add(checkFactory.createAtLeastNCategoriesMustBeSelectedCheck(
                1)).add(new EnableCheck() {
                public String check(JComponent component) {
                    return (pickUnselectedCategory(workbenchContext.getLayerNamePanel(),
                        workbenchContext.getLayerManager()) == null)
                    ? I18N.get("ui.plugin.RemoveSelectedCategoriesPlugIn.at-least-1-category-must-be-left-unselected") : null;
                }
            });
    }

    private static class CategorySpec {
        private int index;
        private String name;

        public CategorySpec(String name, int index) {
            this.name = name;
            this.index = index;
        }
    }
    
    public Icon getIcon(){
      return IconLoader.icon("fugue/folder--minus-round.png");
    }
}
