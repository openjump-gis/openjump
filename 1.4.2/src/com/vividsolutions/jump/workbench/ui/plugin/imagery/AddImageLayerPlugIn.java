package com.vividsolutions.jump.workbench.ui.plugin.imagery;

import java.awt.Color;
import java.util.Collection;


import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

import org.openjump.core.ui.plugin.AbstractUiPlugIn;



public class AddImageLayerPlugIn extends AbstractUiPlugIn {
    private static int nameCounter = 1;
    
    public void initialize(final PlugInContext context) throws Exception {
        super.initialize(context);
        context.getFeatureInstaller()
               .addMainMenuItem(new String[]{MenuNames.FILE},this,4);
    }

    public String getName(){
    	return I18N.get("ui.plugin.imagery.AddImageLayerPlugIn.Add-Image-Layer");
    }

    public boolean execute(PlugInContext context) throws Exception {
        LayerManager lm = context.getLayerManager();
        ImageFeatureCreator ifc = new ImageFeatureCreator();

        lm.setFiringEvents(false);
        Layer layer = createLayer(lm);
        lm.setFiringEvents(true);

        Collection features = ifc.getImages(context, layer);

        if ( features != null ) {
            lm.addLayer(chooseCategory(context), layer);
            layer.getFeatureCollectionWrapper().addAll(features);
            ifc.setLayerSelectability(layer);
        }

        return false;
    }


    private String chooseCategory(PlugInContext context) {
       return context.getLayerNamePanel() == null ? StandardCategoryNames.WORKING
               : context.getLayerNamePanel().getSelectedCategories().isEmpty() ? StandardCategoryNames.WORKING
                       : context.getLayerNamePanel().getSelectedCategories().iterator().next().toString();
    }


    private Layer createLayer(LayerManager lm) {
        String newLayerName = I18N.get("ui.plugin.imagery.AddImageLayerPlugIn.Image")+"_"+nameCounter++;
        Layer layer = new Layer(newLayerName,
                        Color.black,
                        new FeatureDataset(ImageryLayerDataset.getSchema()),
                        lm);
        layer.setEditable(true);
        // Set fill just for the icon beside the layer name [Jon Aquino
        // 2005-04-11]
        layer.getBasicStyle().setRenderingFill(false);
        layer.getBasicStyle().setEnabled(false);
        layer.addStyle(new ReferencedImageStyle());
        return layer;
    }

}