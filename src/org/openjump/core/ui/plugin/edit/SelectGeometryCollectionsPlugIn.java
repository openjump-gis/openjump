package org.openjump.core.ui.plugin.edit;

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;

import java.util.ArrayList;
import java.util.List;

/**
 * A plugin to select features with complex geometries (MultiPoint, MultiLineString,
 * MultiPolygon or GeometryCollection).
 * Useful to decompose these geometries.
 */
public class SelectGeometryCollectionsPlugIn extends AbstractPlugIn {

    public void initialize(PlugInContext context) throws Exception {

        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] {MenuNames.EDIT, MenuNames.SELECTION},
                getName(), false, null,
                createEnableCheck(context.getWorkbenchContext()));
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    public boolean execute(PlugInContext context) throws Exception{
        reportNothingToUndoYet(context);
        List<Feature> selectedFeatures = new ArrayList<>();
        LayerViewPanel layerViewPanel = context.getWorkbenchContext().getLayerViewPanel();
        layerViewPanel.getSelectionManager().clear();
        for (Layer layer : context.getSelectedLayers()) {
            selectedFeatures.clear();
            if (layer.isVisible()) {
                FeatureCollection featureCollection = layer.getFeatureCollectionWrapper();
                for (Feature feature : featureCollection.getFeatures()) {
                    if (feature.getGeometry() instanceof GeometryCollection) {
                        selectedFeatures.add(feature);
                    }
                }
                if (selectedFeatures.size() > 0) {
                    context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().getFeatureSelection()
                            .selectItems(layer, selectedFeatures);
                }
            }
        }
        return true;
    }
}
