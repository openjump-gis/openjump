
package org.openjump.core.ui.plugin.edittoolbox.cursortools;


import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;



import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.PolygonTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
* @author ERWAN BOCHER Laboratoire RESO UMR CNRS 6590 et Olivier Bonnefont
* @url www.projet-sigle.org
* @curentdate 18 mai 2006
* @package name org.openjump.sigle.plugin.edittoolbox
* @license Licence CeCILL http://www.cecill.info/
* @todo TODO
* 
* Cette classe réalise les opérations géometriques en fonction des polygones selectionés et du polygone déssiné.
 * 
* 
*/
public class CutPolygonTool extends PolygonTool {
		  
    final static String sCookieCut = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.CutPolygonTool.Create-Cookie-Cut");
    		
	Geometry geomSelected = null;
	Geometry geomDraw = null;
	Geometry newGeomIntersect = null;
	Geometry newGeomDiff = null;
	
		private FeatureDrawingUtil featureDrawingUtil;
					
	    protected CutPolygonTool(FeatureDrawingUtil featuredrawingutil)
	    {
	        this.featureDrawingUtil = featureDrawingUtil;
	    }

		public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy) {
			FeatureDrawingUtil featureDrawingUtil =
				new FeatureDrawingUtil(layerNamePanelProxy);

			return featureDrawingUtil.prepare(
				new CutPolygonTool(featureDrawingUtil),
				true);
		}
    	
	    //	Ici on va chercher l'icone pour le plugin. L'icone est localisé dans le repertoire de votre package
	    public Icon getIcon() {
	    	return new ImageIcon(getClass().getResource("CutPolygon.gif"));
		}

	    public String getName(){
	    	return sCookieCut;
	    }

	    //Traitement réalisé lors du déplacement de la souris
	    protected void gestureFinished()throws Exception {
	    	  
	    	//Ici on va chercher la couche
	    	  	
	    	  	WorkbenchContext context = getWorkbench().getContext();
	    	  	
	    	  	LayerNamePanel layernamepanel = context.getLayerNamePanel();
	    	  	
				
	            Layer[] selectedLayers = layernamepanel.getSelectedLayers();
	           
	            // Conditions pour l'utilisation de la fonction de découpage de polygones
	            
	            if (selectedLayers.length == 0){
	            
	            	JOptionPane.showMessageDialog(null, I18N.get("org.openjump.sigle.plugin.edittoolbox.At-least-one-layer-must-be-selected"),  I18N.get("org.openjump.sigle.plugin.edittoolbox.Information"), JOptionPane.INFORMATION_MESSAGE);
	            }
	            
	           else if (selectedLayers.length > 1) {
	           	
	           	JOptionPane.showMessageDialog(null,  I18N.get("org.openjump.sigle.plugin.edittoolbox.One-layer-must-be-selected"),  I18N.get("org.openjump.sigle.plugin.edittoolbox.Information"), JOptionPane.INFORMATION_MESSAGE);
	           }
	           
	           
	           else {
	            
	           	Layer activeLayer = (Layer) selectedLayers[0];
	            Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager() .getFeaturesWithSelectedItems(activeLayer);
	               
	           if (activeLayer.isEditable()) {
	    	
	            if (!checkPolygon()) {
				return;
	            }
	    		    	
	            else  {
	    		    		
	    		
	    		for (Iterator k = selectedFeatures.iterator(); k.hasNext();){
	    			
	    			Feature featureSelected = (Feature) k.next();
	    			 
	    			geomSelected = featureSelected.getGeometry();
	    			geomDraw = getPolygon();
	    			
	    				    			
	    			if(!getPolygon().intersects(geomSelected))
	    			 {
	    				
	    			 }
	    			else {
	    				if ((geomSelected instanceof Polygon)||(geomSelected instanceof MultiPolygon)){
	    			newGeomIntersect = geomSelected.intersection(geomDraw);
	    			newGeomDiff = geomSelected.difference(newGeomIntersect);
	    			
	    			
	    			  	BasicFeature featureIntersect = new BasicFeature(activeLayer.getFeatureCollectionWrapper().getFeatureSchema());
	    		        BasicFeature featureDiff = new BasicFeature(activeLayer.getFeatureCollectionWrapper().getFeatureSchema());
	    		        FeatureUtil.copyAttributes(featureSelected, featureIntersect);
	    		        featureIntersect.setGeometry(newGeomIntersect);
	    		        FeatureUtil.copyAttributes(featureSelected, featureDiff);	    		        	
	    		        featureDiff.setGeometry(newGeomDiff);
	    		        
	    		        // on suprime l'entité d'entrée que l'on remplace par les entités produites
	    		        	activeLayer.getFeatureCollectionWrapper().remove(featureSelected);
	    		        	activeLayer.getFeatureCollectionWrapper().add(featureIntersect);
	    		        	activeLayer.getFeatureCollectionWrapper().add(featureDiff);
	    		        
	    		       //	rafraîchissement de l’affichage
	    		        	context.getLayerViewPanel().repaint();
	    				   }
	    			    	}
	    	}
	            }
	    	  }
	           else {
	        	
	           	JOptionPane.showMessageDialog(null,  I18N.get("org.openjump.sigle.plugin.edittoolbox.Layer-must-be-editable"),  I18N.get("org.openjump.sigle.plugin.edittoolbox.Information"), JOptionPane.INFORMATION_MESSAGE);
	          
	           }
	    	  }
	    		    		    	
	    	  }   	  
	    
	        
	}