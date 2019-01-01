package org.openjump.core.ui.plugin.mousemenu;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.MeasureLayerFinder;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class MeasureSelectedPlugIn extends AbstractPlugIn {
    public static final String NAME = I18N
            .get("org.openjump.core.ui.plugin.mousemenu.MeasureSelectedFeaturePlugIn.name");

    public static final Icon ICON = IconLoader.icon("Ruler.gif");

    public Icon getIcon() {
        return ICON;
    }

    public String getName() {
        return NAME;
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        LayerViewPanel layerViewPanel = context.getWorkbenchContext()
                .getLayerViewPanel();
        WorkbenchContext wbc = context.getWorkbenchContext();
        Collection<Layer> layers = ((SelectionManagerProxy) wbc.getWorkbench()
                .getFrame().getActiveInternalFrame()).getSelectionManager()
                .getFeatureSelection().getLayersWithSelectedItems();
        // Giuseppe Aruta 2015-6-25
        // The code is already set for measuring multiple selected geometies.
        // Enablecheck deactives that until a good solution is found for
        // multigeometries (multiPolygon, multiLinestring, multipoint and
        // geometry collections)
        // and for points (measure coordinates)
        for (Layer layer : layers) {

            SelectionManager manager = context.getLayerViewPanel().getSelectionManager();

            for (Feature feature : manager.createFeaturesFromSelectedItems(layer)) {
                try {
                    layerViewPanel.setViewportInitialized(true);

                    Geometry geom = feature.getGeometry();
                    if (geom instanceof Polygon || geom instanceof LineString) {
                        measure(wbc, geom);
                    }
                    else {
                        // Giuseppe Aruta 2015-6-25
                        // Set here the code for multiplegeometries and point
                    }
                } catch (IllegalArgumentException e) {
                    context.getWorkbenchFrame().warnUser(e.toString());
                }
            }

        }

        return true;

    }

    public void measure(WorkbenchContext context, Geometry geom) {
        MeasureLayerFinder measureLayerFinder = new MeasureLayerFinder(
                context.getLayerViewPanel(), context);

        measureLayerFinder.setMeasure(geom);

        // and set it visible
        if (!measureLayerFinder.getLayer().isVisible()) {
            measureLayerFinder.getLayer().setVisible(true);
        }

    }

    public static EnableCheck createEnableCheck(final WorkbenchContext context) {
        MultiEnableCheck mec = new MultiEnableCheck();

        mec.add(new EnableCheckFactory(context)
                .createWindowWithSelectionManagerMustBeActiveCheck());
        mec.add(new EnableCheckFactory(context)
                .createExactlyNFeaturesMustBeSelectedCheck(1));

        mec.add(new EnableCheck() {
            public String check(JComponent component) {
                Feature feature = context.getLayerViewPanel().getSelectionManager()
                        .getFeaturesWithSelectedItems().iterator().next();
                Geometry geom = feature.getGeometry();

                return geom instanceof GeometryCollection
                        || geom instanceof Point ? geom.getGeometryType()
                        + " - "
                        + I18N.get("org.openjump.core.ui.plugin.mousemenu.MeasureSelectedFeaturePlugIn.message1")
                        : null;
            }
        });
        return mec;
    }

}
