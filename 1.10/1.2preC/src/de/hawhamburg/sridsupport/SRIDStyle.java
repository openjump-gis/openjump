package de.hawhamburg.sridsupport;

import java.awt.Graphics2D;
import java.util.Iterator;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
/**
 * Ensures that all geometries have a given SRID. Because it is a Style, it will
 * be saved to the task file.
 */
public class SRIDStyle implements Style {
    private int srid = 0;
    public void paint(Feature f, Graphics2D g, Viewport viewport)
            throws Exception {
    }
    private boolean initialized = false;
    public void initialize(Layer layer) {
        if (initialized) {
            return;
        }
        updateSRIDs(layer);
        layer.getLayerManager().addLayerListener(new LayerListener() {
            public void featuresChanged(FeatureEvent e) {
                for (Iterator i = e.getFeatures().iterator(); i.hasNext();) {
                    Feature feature = (Feature) i.next();
                    feature.getGeometry().setSRID(srid);
                }
            }
            public void layerChanged(LayerEvent e) {
            }
            public void categoryChanged(CategoryEvent e) {
            }
        });
        initialized = true;
    }
    public void updateSRIDs(Layer layer) {
        for (Iterator i = layer.getFeatureCollectionWrapper().iterator(); i
                .hasNext();) {
            Feature feature = (Feature) i.next();
            feature.getGeometry().setSRID(srid);
        }
    }
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Assert.shouldNeverReachHere();
            return null;
        }
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    private boolean enabled = true;
    public boolean isEnabled() {
        return enabled;
    }
    public int getSRID() {
        return srid;
    }
    public void setSRID(int srid) {
        this.srid = srid;
    }

}
