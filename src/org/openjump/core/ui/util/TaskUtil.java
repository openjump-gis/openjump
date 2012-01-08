package org.openjump.core.ui.util;

import java.util.Collection;
import java.util.List;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;

public class TaskUtil {
  public static Category getSelectedCategoryName(WorkbenchContext workbenchContext) {
    LayerNamePanel layerNamePanel = workbenchContext.getLayerNamePanel();
    Collection<Category> selectedCategories = layerNamePanel.getSelectedCategories();
    if (selectedCategories.isEmpty()) {
      LayerManager layerManager = layerNamePanel.getLayerManager();
      List<Category> categories = layerManager.getCategories();
      return categories.get(0);
    } else {
      return selectedCategories.iterator().next();
    }

  }
}
