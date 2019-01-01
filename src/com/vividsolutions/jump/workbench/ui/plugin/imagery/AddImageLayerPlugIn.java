package com.vividsolutions.jump.workbench.ui.plugin.imagery;

import java.awt.Color;

import javax.swing.Icon;

import org.openjump.core.ui.plugin.AbstractUiPlugIn;
import org.openjump.core.ui.util.TaskUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.imagery.ReferencedImagesLayer;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class AddImageLayerPlugIn extends AbstractUiPlugIn {
  private static int nameCounter = 1;

  @Override
  public String getName() {
    return I18N.get("ui.plugin.imagery.AddImageLayerPlugIn.Add-Image-Layer");
  }

  @Override
  public Icon getIcon() {
    return IconLoader.icon("map_add_16.png");
  }

  @Override
  public boolean execute(PlugInContext context) throws Exception {
    LayerManager lm = context.getLayerManager();
    lm.setFiringEvents(false);
    Layer layer = createLayer(lm);
    lm.setFiringEvents(true);
    Category category = TaskUtil.getSelectedCategoryName(workbenchContext);
    lm.addLayerable(category.getName(), layer);

    boolean success = new ImageLayerManagerPlugIn().execute(context);

    return success;
  }

  private String chooseCategory(PlugInContext context) {
    return context.getLayerNamePanel() == null ? StandardCategoryNames.WORKING
        : context.getLayerNamePanel().getSelectedCategories().isEmpty() ? StandardCategoryNames.WORKING
            : context.getLayerNamePanel().getSelectedCategories().iterator()
                .next().toString();
  }

  private Layer createLayer(LayerManager lm) {
    String newLayerName = I18N
        .get("ui.plugin.imagery.AddImageLayerPlugIn.Image")
        + "_"
        + nameCounter++;
    Layer layer = new ReferencedImagesLayer(newLayerName, Color.black,
        new FeatureDataset(ImageryLayerDataset.getSchema()), lm);
    layer.setEditable(true);
    // Set fill just for the icon beside the layer name [Jon Aquino
    // 2005-04-11]
    layer.getBasicStyle().setRenderingFill(false);
    layer.getBasicStyle().setEnabled(false);
    layer.addStyle(new ReferencedImageStyle());
    return layer;
  }

  public MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return new MultiEnableCheck().add(checkFactory
        .createWindowWithLayerManagerMustBeActiveCheck());
  }

}