/*****************************************************
 * created:  		07.12.2004
 * last modified:  	
 * 
 * description: 
 *  Merges two selected polygons if they intersect each other in more than one point.
 *  The first feature in the selection gets the new geometry while the second feature is deleted.
 *****************************************************/

package org.openjump.core.ui.plugin.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.geomutils.algorithm.PolygonMerge;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;


/**
 * @author sstein
 * 
 **/
public class MergeTwoSelectedPolygonsPlugIn extends AbstractPlugIn{


	private String sMergeTwoPolys = I18N.get("org.openjump.core.ui.plugin.tools.MergeTwoSelectedPolygonsPlugIn.Merge-Two-Polygons");
	//private String sMergeTwoPolys = "MergeTwoPolygons";
	
    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuItem(
    	        this,								//exe
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY}, 	//menu path
                this.getName(), //name methode .getName recieved by AbstractPlugIn 
                false,			//checkbox
                null,			//icon
                createEnableCheck(context.getWorkbenchContext())); //enable check        
    }
    
    public String getName() {
        return sMergeTwoPolys;
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createSelectedLayersMustBeEditableCheck())
                        .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(2));
    }
    
	public boolean execute(PlugInContext context) throws Exception{       
    	        		
	    Collection features = context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
	    if (features.size() == 2){
	    	Iterator iter = features.iterator();
	    	Feature f1 = (Feature)iter.next();
	    	Feature f2 = (Feature)iter.next();
	    	PolygonMerge merge = new PolygonMerge(f1.getGeometry(), f2.getGeometry()); 
	    	if(merge.isMergeSuccesfull()==1){
	    		//-- merge using an edit transaction to enable undo
	    		ArrayList fToEdit = new ArrayList();
	    		fToEdit.add(f1);
	    		Map layer2FeatList = LayerTools.getLayer2FeatureMap(fToEdit, context);	    	        
	    	    Layer[] layersWithFeatures = (Layer[])layer2FeatList.keySet().toArray(new Layer[0]);	    	 
	    		EditTransaction edtr = new EditTransaction(fToEdit,"setgeom",layersWithFeatures[0],true,false,context.getLayerViewPanel());
	    		Geometry g = merge.getOutPolygon();
	    		//f1.setGeometry(g);
	    		edtr.setGeometry(f1,g);
	            edtr.commit();
	            edtr.clearEnvelopeCaches();
	            //-- delete other feature
	    		ArrayList fToDelete = new ArrayList();
	    		fToDelete.add(f2);
	    		FeatureCollectionTools.deleteFeatures(fToDelete,context);
	    	}
	    }
		else{
			context.getWorkbenchFrame().warnUser("more than 2 objects selected");
		}
    	//context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().clear();
	    return true;
    }	
	
	    
  
}
