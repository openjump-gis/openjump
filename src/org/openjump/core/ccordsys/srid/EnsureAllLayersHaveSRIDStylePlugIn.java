package org.openjump.core.ccordsys.srid;

import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JInternalFrame;

import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import org.openjump.core.model.TaskEvent;
import org.openjump.core.model.TaskListener;

/**
 * Adds the SRIDStyle to every layer that JUMP encounters.
 */
public class EnsureAllLayersHaveSRIDStylePlugIn extends AbstractPlugIn {

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    initializeCurrentAndFutureInternalFrames(context.getWorkbenchFrame(), new Block() {

      private Collection<JInternalFrame> initializedFrames = new ArrayList<>();

      public Object yield(Object internalFrame) {
        if (internalFrame instanceof JInternalFrame &&
                !initializedFrames.contains(internalFrame)) {
          initialize((JInternalFrame) internalFrame);
          initializedFrames.add((JInternalFrame) internalFrame);
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

    for (Layer layer : layerManager.getLayers()) {
      ensureHasSRIDStyle(layer);
    }

    LayerListener layerListener = new LayerListener() {

      public void featuresChanged(FeatureEvent e) { }

      public void layerChanged(LayerEvent e) {
        if (e.getLayerable() instanceof Layer) {
          ensureHasSRIDStyle((Layer) e.getLayerable());
        }
      }

      public void categoryChanged(CategoryEvent e) { }
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
    CoordinateSystem cs = layer.getFeatureCollectionWrapper()
            .getFeatureSchema().getCoordinateSystem();
    if (!cs.equals(CoordinateSystem.UNSPECIFIED)) {
      srid = cs.getEPSGCode();
    }
    // OR fetch it from first geometry
    else if (layer.getFeatureCollectionWrapper().size() > 0) {
      srid = layer.getFeatureCollectionWrapper()
              .iterator().next().getGeometry().getSRID();
    }
    sridStyle.setSRID(srid);
    layer.addStyle(sridStyle);
  }

  private void initializeCurrentAndFutureInternalFrames(
          WorkbenchFrame workbenchFrame, final Block block) {

    workbenchFrame.addTaskListener(new TaskListener() {
      @Override
      public void taskAdded(TaskEvent taskEvent) {
        initialize(taskEvent.getTask().getLayerManager());
      }

      @Override
      public void taskLoaded(TaskEvent taskEvent) {

      }
    });
    //workbenchFrame.getDesktopPane().addContainerListener(
    //        new ContainerAdapter() {
    //          public void componentAdded(ContainerEvent e) {
    //            if (!(e.getChild() instanceof JInternalFrame)) {
    //              return;
    //            }
    //            block.yield(e.getChild());
    //          }
    //        });
//
    for (JInternalFrame internalFrame : workbenchFrame.getInternalFrames()) {
      block.yield(internalFrame);
    }
  }
}
