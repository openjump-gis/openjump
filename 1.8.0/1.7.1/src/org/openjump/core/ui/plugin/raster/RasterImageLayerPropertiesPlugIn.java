/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.raster;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class RasterImageLayerPropertiesPlugIn  extends AbstractPlugIn 
{
   
	 /*
	   * Giuseppe Aruta 
	   * Simple plugin that allows to view some properties of Sextante Raster Layer:
	   * name, file location, raster dimension (in cell), raster extension,
	   * X cell size, numbers of bands, min-max-mean of 1st band value (if the raster
	   * is monoband)
	   * ver 0.1 2013_05_27
	   */
	
    private double minVal;
    private double maxVal;
    private double noVal; 
    private int type;
    private double meanVal;
    private String file;
    private double cellSize;
    private String name;
    private int X;
    private int Y;
    private int bands;
 
   
    
   
  private final static String INFO = 
        	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Info");  //information
        private final static String LAYER_NAME = 
        	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Layer-Name");
        private final static String DATASOURCE_CLASS = 
        	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.DataSource-Class");  //class name
        private final static String EXTENT =
    	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.extent");
        private final static String XMIN =
    	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmin");
    	private final static String YMIN =
    	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymin");
    	private final static String XMAX =
    	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmax");
    	private final static String YMAX =
    	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymax");
    	
    	
    	  private final static String BANDS = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.bands");
    	  private final static String RASTER_SIZE = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.size");
    	  private final static String CELL_SIZE = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.size");
    	  private final static String CELL_VALUES = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.values");
    	  private final static String MAX = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.max");
    	  private final static String MIN = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.min");
    	  private final static String MEAN = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.mean");
    	  private final static String NODATA = 
    			  I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
    	
    	
    	
    	
    	 private Envelope extent;
    	
    	
    public void initialize(PlugInContext context) throws Exception
    {    
    
    	
    	
    	WorkbenchContext workbenchContext = context.getWorkbenchContext();
       new FeatureInstaller(workbenchContext);
        
               
       
   		/*

        context.getFeatureInstaller().addMainMenuItem(
        		this, 
        		new String[] {MenuNames.RASTER},
        		I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn"), 
				false, 
				getIcon(),
				createEnableCheck(context.getWorkbenchContext()));
				*/
       
   }

    public static MultiEnableCheck createEnableCheck(
			final WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(
				workbenchContext);
		MultiEnableCheck multiEnableCheck = new MultiEnableCheck();

        multiEnableCheck.add( checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class) );

		return multiEnableCheck;
	}
    
    
 /*   
    
    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createTaskWindowMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class));
    }
    */
    public Icon getIcon() 
    {
    	  return IconLoader.icon("information_16x16.png");
    	    }
    
   public Icon getIcon2() 
    {
    	return IconLoader.icon("mapSv2_13.png");
    	    }
 
   public String getName() {
       return I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn");
   } 
 
    public boolean execute(PlugInContext context) throws Exception {
   	
    	new GeometryFactory();
		//-- get the rasterimage layer
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);
        //System.out.println(rLayer);
        extent = rLayer.getEnvelope();
      
        
      //  PlanarImage image = JAI.create(LayerTools.getSelectedLayerable(context, RasterImageLayer.class), args[0])
		
		//-- create a sextante raster layer since it is easier to handle
		OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
		rstLayer.create(rLayer);
    
	
		
		
		minVal = rstLayer.getMinValue();
 	    maxVal = rstLayer.getMaxValue();
 	    meanVal = rstLayer.getMeanValue();
 	    noVal = rstLayer.getNoDataValue();
 		file = rstLayer.getFilename();
 		cellSize =  rstLayer.getLayerCellSize();
 		name =rstLayer.getName();
 		type = rstLayer.getDataType();
 		X =  rstLayer.getNX();
 		Y = rstLayer.getNY();
 		bands= rstLayer.getBandsCount();
 	 
 		
 		if (rstLayer.getBandsCount() == 1){
 			String testo =  
 	 				LAYER_NAME+": "+name  +"\n"
 	 				+DATASOURCE_CLASS+": "+file +"\n"
 	 				 +RASTER_SIZE+": "+X+"x"+Y +"\n"
 	 				 +EXTENT+": "+"\n"
 	 		         +"     "+ XMIN + ": " + extent.getMinX() +"\n" 
 	 		         +"     "+ YMIN + ": " + extent.getMinY() +"\n" 
 	 		         +"     "+ XMAX + ": " + extent.getMaxX() +"\n" 
 	 		         +"     "+ YMAX + ": " + extent.getMaxY() +"\n" 
 	 	   			 // +" \n"
 	 				 // +"Type: "+type +"\n"
 	 	   			  +BANDS+": "+bands +"\n"
 	 	   			  +CELL_SIZE +": "+cellSize +"\n"
 	 		          +CELL_VALUES+": "+"\n"
 	 		         +"      "+ MAX +": "+maxVal +"\n"
 	 		         +"      "+ MIN +": "+minVal +"\n"
 	 		         +"      "+ MEAN +": "+meanVal +"\n"
 	 		         +"      "+ NODATA+": "+noVal +"\n"
 	 		         +"      "+  "\n"
 	 		     ;
 	 	
 	        JOptionPane.showMessageDialog(null,

 	   			  testo,
 	   			INFO
 	   			  ,
 	         	    JOptionPane.INFORMATION_MESSAGE,
 	         	    getIcon2()
 	         	    );
 	      
 	       
 	       return true;
 		}
 		
 		else {
 		String testob =  
 				LAYER_NAME+": "+name  +"\n"
 				+DATASOURCE_CLASS+": "+file +"\n"
 				 +RASTER_SIZE+": "+X+"x"+Y +"\n"
 				 +EXTENT+": "+"\n"
 		         +"     "+ XMIN + ": " + extent.getMinX() +"\n" 
 		         +"     "+ YMIN + ": " + extent.getMinY() +"\n" 
 		         +"     "+ XMAX + ": " + extent.getMaxX() +"\n" 
 		         +"     "+ YMAX + ": " + extent.getMaxY() +"\n" 
 	   			 // +" \n"
 				 // +"Type: "+type +"\n"
 	   			  +BANDS+": "+bands +"\n"
 	   			  +CELL_SIZE +": "+cellSize +"\n"
 		        		     ;
 	
        JOptionPane.showMessageDialog(null,

   			  testob,
   			INFO
   			  ,
         	    JOptionPane.INFORMATION_MESSAGE,
         	    getIcon2()
         	    );
      
       
       return true;
 		 }
      }
	

    
    
} 

