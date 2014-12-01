package org.openjump.sigle.plugin.geoprocessing.layers;




import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.geom.*;

import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.feature.FeatureCollection;


/**
* @author ERWAN BOCHER Laboratoire RESO UMR CNRS 6590
* @url www.projet-sigle.org
* @curentdate 20 févr. 2006
* @package name org.openjump.sigle.plugin.geoprocessing.layers
* @license Licence CeCILL http://www.cecill.info/
* @todo TODO
* 
* Ce plugin permet de réaliser des jointures spatiales en utilisant différents
* prédicats spatiaux.
* 
*/

public class SpatialJoinPlugIn extends ThreadedBasePlugIn {
    
	   
	private final static String LAYER1 = GenericNames.LAYER_A;
	  private final static String LAYER2 = GenericNames.LAYER_B;
	  //-- reset in execute to correct language 
	  private static String METHODS = I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.spatial-operation");

	  private static String METHOD_EQUAL = I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.equal");
	  private static String METHOD_WITHIN = "A " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.within") +" B";
	  private static String METHOD_EQUAL_AND_WITHIN = "A " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.equal-AND-within" +" B");
	  private static String METHOD_EQUAL_OR_WITHIN = "A " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.equal-OR-within" +" B");
	 
	  private static Collection getSpatialJoinMethodNames()
	  {
	    Collection names = new ArrayList();
	    names.add(METHOD_EQUAL);
	    names.add(METHOD_WITHIN);
	    names.add(METHOD_EQUAL_AND_WITHIN);
	    names.add(METHOD_EQUAL_OR_WITHIN);
	 
	    return names;
	  }

	  private Collection SpatialJoinMethodNames;
	  private MultiInputDialog dialog;
	  private Layer layer1, layer2;
	  private String methodNameToRun;
	  private boolean exceptionThrown = false;

	/** 
	* Sets geomentryMethodNames variable using getGeometryMethodNames function.
	*/
	  public SpatialJoinPlugIn()
	  {
	  	SpatialJoinMethodNames = getSpatialJoinMethodNames();
	  }

		public void initialize(PlugInContext context) throws Exception {
	        context.getFeatureInstaller().addMainMenuItem(this,new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES}, 
	    			this.getName(), false, null, 
	    			new MultiEnableCheck().add(new EnableCheckFactory(context.getWorkbenchContext()).createTaskWindowMustBeActiveCheck())
					.add(new EnableCheckFactory(context.getWorkbenchContext()).createAtLeastNLayersMustExistCheck(2))
					); 
	    }
	 

	  public boolean execute(PlugInContext context) throws Exception {
		 METHODS = I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.spatial-operation");
		 METHOD_EQUAL = I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.equal");
		 METHOD_WITHIN = "A " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.within" +" B");
		 METHOD_EQUAL_AND_WITHIN = "A " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.equal-AND-within" +" B");
		 METHOD_EQUAL_OR_WITHIN = "A " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.equal-OR-within" +" B");
	  	
	    MultiInputDialog dialog = new MultiInputDialog(
	        context.getWorkbenchFrame(), getName(), true);
	    setDialogValues(dialog, context);
	    GUIUtil.centreOnWindow(dialog);
	    dialog.setVisible(true);
	    if (! dialog.wasOKPressed()) { return false; }
	    getDialogValues(dialog);
	    return true;
	  }

	  public String getName(){
	  	return I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Transfer-Attributes");
	  }
	  
	  public void run(TaskMonitor monitor, PlugInContext context)
	      throws Exception
	  {
	    FeatureSchema featureSchema = new FeatureSchema();
	    
	   
	    FeatureCollection resultColl = runSpatialJoinMethod(layer1.getFeatureCollectionWrapper(),
	        layer2.getFeatureCollectionWrapper(),
	        methodNameToRun);
	  if (resultColl.size()>0)
	    context.addLayer(StandardCategoryNames.WORKING, I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Result") + methodNameToRun, resultColl);
	    if (exceptionThrown)
	      context.getWorkbenchFrame().warnUser(I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Error-while-executing-spatial-function"));
	  }

	  private FeatureCollection runSpatialJoinMethod(FeatureCollection fcA,
	                                     FeatureCollection fcB,
	                                     String methodName
	                                     )
	  {
	    exceptionThrown = false;
	    FeatureCollection resultFC;
	    Feature fEqual = null;
	    Feature fWithin = null;
	    Feature fEqualAndWithin = null;
	    Feature fEqualOrWithin = null;
	    
    	AttributeMapping mapping = null;
	    
    	mapping = new AttributeMapping(new FeatureSchema(), new FeatureSchema());
        List aFeatures = null;
        mapping = new AttributeMapping(fcB.getFeatureSchema(), fcA.getFeatureSchema());
        aFeatures = fcA.getFeatures();
	    
        FeatureDataset fcRecup = new FeatureDataset(mapping.createSchema("GEOMETRY"));
        IndexedFeatureCollection indexedB = new IndexedFeatureCollection(fcB);
        
        for (int i = 0; (i < aFeatures.size());i++) {
        	Feature aFeature = (Feature) aFeatures.get(i);
        	Feature feature = new BasicFeature(fcRecup.getFeatureSchema());
        	int nbFeatureEqual = 0;
        	int nbFeatureWithin = 0;
        	int nbFeatureEqualAndWithin = 0;
        	int nbFeatureEqualOrWithin=0;
        	int nbFeature =0;
        	        	
        	for (Iterator j = indexedB.query(aFeature.getGeometry().getEnvelopeInternal()).iterator();
        		j.hasNext();) {
        	    
        	    Feature bFeature = (Feature) j.next();
        	    if (methodName.equals(METHOD_EQUAL)) {
        	    	if (aFeature.getGeometry().equals(bFeature.getGeometry())) {
		        	nbFeatureEqual++;
		        	nbFeature++;
					fEqual = bFeature;
        	    	}
        	}
        	    
        	    else if (methodName.equals(METHOD_WITHIN)) {
        	    	if (aFeature.getGeometry().within(bFeature.getGeometry())) {
		        	nbFeatureWithin++;
		        	nbFeature++;
					fWithin = bFeature;
        	    	}
        	}
        	    
        	   else if (methodName.equals(METHOD_EQUAL_AND_WITHIN)) {
        	    	if ((aFeature.getGeometry().equals(bFeature.getGeometry()))&& ((aFeature.getGeometry().within(bFeature.getGeometry())))) {
		        	nbFeatureEqualAndWithin++;
		        	nbFeature++;
					fEqualAndWithin = bFeature;
        	    	}
        	}
        	   
        	   else if (methodName.equals(METHOD_EQUAL_OR_WITHIN)) {
    	    	if ((aFeature.getGeometry().equals(bFeature.getGeometry()))|| ((aFeature.getGeometry().within(bFeature.getGeometry())))) {
	        	nbFeatureEqualOrWithin++;
	        	nbFeature++;
				fEqualOrWithin = bFeature;
    	    	}
    	}
        	}
	        // on ne transfere les attributs que lorsque la geometry resultat 
        	// n'est contenue que une seule geometry source
	        if (nbFeatureEqual == 1) {
	        	mapping.transferAttributes(fEqual, aFeature, feature);
	        	feature.setGeometry((Geometry) aFeature.getGeometry().clone()); 
		        fcRecup.add(feature);
	        }
	        
	        else if (nbFeatureWithin == 1){
	        	mapping.transferAttributes(fWithin, aFeature, feature);
	        	feature.setGeometry((Geometry) aFeature.getGeometry().clone()); 
		        fcRecup.add(feature);
	        }
	        
	        else if (nbFeatureEqualAndWithin == 1){
	        	mapping.transferAttributes(fEqualAndWithin, aFeature, feature);
	        	feature.setGeometry((Geometry) aFeature.getGeometry().clone()); 
		        fcRecup.add(feature);
	        }
	        
	        else if (nbFeatureEqualOrWithin == 1){
	        	mapping.transferAttributes(fEqualOrWithin, aFeature, feature);
	        	feature.setGeometry((Geometry) aFeature.getGeometry().clone()); 
		        fcRecup.add(feature);
	        }
	        	        
	        // on clone la geometry pour que les modifs sur la geometry source 
	        // ne soient pas transferees sur la geometry resultat
	        //feature.setGeometry((Geometry) aFeature.getGeometry().clone()); 
	       // fcRecup.add(feature);
	        
	    }
	    return fcRecup;
	  }
  

	  private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {
	    //dialog.setSideBarImage(new ImageIcon(getClass().getResource("DiffSegments.png")));
	    dialog.setSideBarDescription(I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Transfers-the-attributes-of-Layer-B-to-Layer-A-using-a-spatial-criterion"));
	    //Set initial layer values to the first and second layers in the layer list.
	    //In #initialize we've already checked that the number of layers >= 2. [Jon Aquino]
	    dialog.addLayerComboBox(LAYER1, layer1, context.getLayerManager());
	    dialog.addLayerComboBox(LAYER2, layer2, context.getLayerManager());
	    dialog.addComboBox(METHODS, methodNameToRun, SpatialJoinMethodNames, null);
	    
	  }

	  private void getDialogValues(MultiInputDialog dialog) {
	    layer1 = dialog.getLayer(LAYER1);
	    layer2 = dialog.getLayer(LAYER2);
	    methodNameToRun = dialog.getText(METHODS);
	  }
     
}


