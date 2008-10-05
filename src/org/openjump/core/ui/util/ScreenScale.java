/*****************************************************
* created:  		01.10.2005
* last modified:  						
* 
* @author sstein
* 
* description:
* 	contains function for accessing the actual map scale of the screen
*  
*****************************************************/
package org.openjump.core.ui.util;

import java.awt.Toolkit;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * Contains function for accessing the actual map scale of the screen 
 *
 * @author sstein
 *
 **/
public class ScreenScale {

	/**
	 * Delivers the scale of the map shown on the display. The scale is 
	 * calculated for the horizontal map direction<p> 
	 * note: The scale may differ for horizontal and vertical direction
	 * due to the type of map projection.
	 *  
	 * @param context PlugInContext
	 * @return actual scale 
	 */
	public static double getHorizontalMapScale(Viewport port){	
	
		double horizontalScale = 0;
		//[sstein] maybe store screenres on the blackboard 
		//         if obtaining is processing intensive? 
	    double SCREENRES = Toolkit.getDefaultToolkit().getScreenResolution(); //72 dpi or 96 dpi or ..     
	    double INCHTOCM = 2.54; //cm
	    
	    double panelWidth = port.getPanel().getWidth(); //pixel
	    double modelWidth = port.getEnvelopeInModelCoordinates().getWidth(); //m
	    //-----
	    // example:
	    // screen resolution: 72 dpi
	    // 1 inch = 2.54 cm
	    // ratio = 2.54/72 (cm/pix) ~ 0.35mm
	    // mapLength[cm] = noPixel * ratio
	    // scale = realLength *100 [m=>cm] / mapLength
	    //-----                            
	    horizontalScale = modelWidth*100 / (INCHTOCM / SCREENRES * panelWidth);

		return horizontalScale;
	}
}
