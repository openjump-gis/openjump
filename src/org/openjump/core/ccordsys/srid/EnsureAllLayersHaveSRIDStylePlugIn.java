package org.openjump.core.ccordsys.srid;

import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JInternalFrame;

import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
/**
 * Adds the SRIDStyle to every layer that JUMP encounters.
 */
public class EnsureAllLayersHaveSRIDStylePlugIn extends AbstractPlugIn {
    public void initialize(PlugInContext context) throws Exception {
        initializeCurrentAndFutureInternalFrames(context.getWorkbenchFrame(),
                new Block() {
                    private Collection initializedFrames = new ArrayList();
                    public Object yield(Object internalFrame) {
                        if (!initializedFrames.contains(internalFrame)) {
                            initialize((JInternalFrame) internalFrame);
                            initializedFrames.add(internalFrame);
                        }
                        return null;
                    }
                });
    }
    private void initialize(JInternalFrame internalFrame) {
        if (!(internalFrame instanceof LayerManagerProxy)) {
            return;
        }
        initialize(((LayerManagerProxy) internalFrame).getLayerManager());
    }
    private void initialize(final LayerManager layerManager) {
        for (Iterator i = layerManager.iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            ensureHasSRIDStyle(layer);
        }
        LayerListener layerListener = new LayerListener() {
            public void featuresChanged(FeatureEvent e) {
            }
            public void layerChanged(LayerEvent e) {
                if (e.getLayerable() instanceof Layer) {
                    ensureHasSRIDStyle((Layer) e.getLayerable());
                }
                if (e.getType() == LayerEventType.REMOVED) {
                    layerManager.removeLayerListener(this);
                }
            }
            public void categoryChanged(CategoryEvent e) {
            }
        };
        layerManager.addLayerListener(layerListener);
    }
    private void ensureHasSRIDStyle(Layer layer) {
        if (layer.getStyle(SRIDStyle.class) != null) {
            return;
        }
        SRIDStyle sridStyle = new SRIDStyle();
        int srid = sridStyle.getSRID();
        
        // freshly loaded featcolls only set the featureschema's property
        CoordinateSystem cs = layer.getFeatureCollectionWrapper().getFeatureSchema().getCoordinateSystem();
        if (!cs.equals(CoordinateSystem.UNSPECIFIED)) {
          srid = cs.getEPSGCode();
        }
        // OR fetch it from first geometry
        else if (layer.getFeatureCollectionWrapper().size() > 0) {
            srid = ((Feature) layer.getFeatureCollectionWrapper()
                    .iterator().next()).getGeometry().getSRID();
        }
        sridStyle.setSRID(srid);
        layer.addStyle(sridStyle);
    }
    private void initializeCurrentAndFutureInternalFrames(
            WorkbenchFrame workbenchFrame, final Block block) {
        workbenchFrame.getDesktopPane().addContainerListener(
                new ContainerAdapter() {
                    public void componentAdded(ContainerEvent e) {
                        if (!(e.getChild() instanceof JInternalFrame)) {
                            return;
                        }
                        block.yield((JInternalFrame) e.getChild());
                    }
                });
        for (Iterator i = Arrays.asList(workbenchFrame.getInternalFrames())
                .iterator(); i.hasNext();) {
            JInternalFrame internalFrame = (JInternalFrame) i.next();
            block.yield(internalFrame);
        }
    }
}
