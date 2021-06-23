package org.openjump.core.ui.plugin.edit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
import com.vividsolutions.jump.tools.OverlayEngine;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

public class ClipToFencePlugIn extends AbstractPlugIn implements ThreadedPlugIn  {

	private static String FENCELAYERMUSTBEPRESENT = "Fence layer must be present";
	private static String DIALOGMSG = "All vector layers will be clipped to the Fence."+
			" Warning: if your task loaded with layers not visible, they have not be loaded" +
			" and therefore will not be clipped.";
	private static String DIALOGWARNING = "This operation is not undoable.";
	private static String VISIBLEONLY = "Visible Only (see Warning)";
	private static final boolean POLYGON_OUTPUT = false;
	
	private WorkbenchContext workbenchContext;
	private boolean visibleOnly = true;

    public String getName() {  //for the menu
    	return I18N.getInstance().get("org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.Clip-Map-to-Fence");
    }

	public void initialize( PlugInContext context ) throws Exception 
    {
		workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuPlugin(this,
				new String[] {MenuNames.EDIT}, getName()+ "...",
				false, null,
				new MultiEnableCheck()
						.add(new EnableCheckFactory(context.getWorkbenchContext()).createTaskWindowMustBeActiveCheck())
						.add(fenceLayerMustBePresent()));
        
        DIALOGWARNING=I18N.getInstance().get("org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.This-operation-is-not-undoable");
        VISIBLEONLY = I18N.getInstance().get("org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.Visible-Only-(-see-Warning-)");
        DIALOGMSG=I18N.getInstance().get("org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.description");
        FENCELAYERMUSTBEPRESENT = I18N.getInstance().get("org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.Fence-layer-must-be-present");
    }
    
    public boolean execute(PlugInContext context) throws Exception {
		MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(),
				getName(), true);
		dialog.setSideBarDescription(DIALOGMSG);
		dialog.addLabel(DIALOGWARNING);
		dialog.addCheckBox(VISIBLEONLY, visibleOnly);
		GUIUtil.centreOnWindow(dialog);
		dialog.setVisible(true);
		if (dialog.wasOKPressed()) {
			visibleOnly = dialog.getCheckBox(VISIBLEONLY).isSelected();	
			return true;
		}
        return false;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

		LayerManager layerManager = context.getLayerManager();
		Layer fence = layerManager.getLayer(FenceLayerFinder.LAYER_NAME);
		ArrayList<Layer> layerList;
		if (visibleOnly) {
			layerList = new ArrayList<>(layerManager.getVisibleLayers(false));
		} else {
			layerList = new ArrayList<>(layerManager.getLayers());
		}
		OverlayEngine overlayEngine = new OverlayEngine();
		overlayEngine.setAllowingPolygonsOnly(POLYGON_OUTPUT);
		overlayEngine.setSplittingGeometryCollections(POLYGON_OUTPUT);
        FeatureCollection a = fence.getFeatureCollectionWrapper();
        List<Layer> unprocessedLayers = new ArrayList<>();
		for (Layer layer : layerList) {
			if (layer == fence) continue;
	        FeatureCollection b = layer.getFeatureCollectionWrapper();
	        if (hasDuplicateAttributeNames(b.getFeatureSchema())) {
	            context.getWorkbenchFrame().warnUser(I18N.getInstance().get(
	                "org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.duplicate-attribute-names-are-not-supported"));
	            unprocessedLayers.add(layer);
	        }
	        else {
	            FeatureCollection overlay = overlayEngine.overlay(a, b, mapping(a, b), monitor);
	            layer.setFeatureCollection(overlay);
	        }
		}
		if (unprocessedLayers.size()>0) {
		    HTMLFrame outputFrame = context.getWorkbenchFrame().getOutputFrame();
		    outputFrame.createNewDocument();
		    outputFrame.addHeader(1, I18N.getInstance().get("org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.Clip-Map-to-Fence"));
		    outputFrame.addHeader(2, I18N.getInstance().get("org.openjump.core.ui.plugin.edit.ClipToFencePlugIn.unprocessed-layers"));
		    for (Layer layer : unprocessedLayers) {
		        outputFrame.append(layer.getName());
		    }
		}
    }

    private AttributeMapping mapping(FeatureCollection a, FeatureCollection b) {
        return new AttributeMapping( new FeatureSchema(), b.getFeatureSchema());
    }
    
    private boolean hasDuplicateAttributeNames(FeatureSchema schema) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            if (!set.add(schema.getAttributeName(i))) return true ;
        }
        return false;
    }

    private EnableCheck fenceLayerMustBePresent() {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (workbenchContext.getLayerViewPanel().getFence() == null)
                    ? FENCELAYERMUSTBEPRESENT
                    : null;
            }
        };
    }

}
