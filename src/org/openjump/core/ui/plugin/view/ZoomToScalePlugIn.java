/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */
/*****************************************************
 * created:  		04.01.2005
 * last modified:  	01.10.2005 [scale obtained now from other class 
 * 								and change in layout]
 * 
 * description:
 *   zooms to a given map scale, which is received from an input dialog 
 * 
 *****************************************************/

package org.openjump.core.ui.plugin.view;

import org.openjump.core.ui.util.ScreenScale;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.Viewport;


/**
 * Zooms to a given map scale, received from a input dialog
 * 
 * @author sstein
 */
public class ZoomToScalePlugIn extends AbstractPlugIn {

    private String T1 = "scale"; //[sstein] this string is not used anymore
    int scale = 25000;
    double oldHorizontalScale = 0; // is calculated for panel-width (not heigth!!)
    double modelWidth = 0;
    double panelWidth = 0;
    String text =I18N.get("org.openjump.core.ui.plugin.view.ZoomToScalePlugIn.set-new-scale-to-zoom") + ":  1 : ";

    public void initialize(PlugInContext context) throws Exception {
    	
		this.T1 = I18N.get("org.openjump.core.ui.plugin.view.ZoomToScalePlugIn.scale") + ": ";
	    context.getFeatureInstaller().addMainMenuItem(this,
	        new String[]
			{MenuNames.VIEW},
	        I18N.get("org.openjump.core.ui.plugin.view.ZoomToScalePlugIn.zoom-to-scale")+"{pos:9}", 
			false, 
			null, 
			createEnableCheck(context.getWorkbenchContext()));
    }
    
    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(checkFactory
                .createAtLeastNLayerablesMustExistCheck(1));
    }
    
    
	public boolean execute(PlugInContext context) throws Exception{
	    
		Viewport port = context.getLayerViewPanel().getViewport();
        this.oldHorizontalScale = ScreenScale.getHorizontalMapScale(port);
        
	    MultiInputDialog dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), 
	            I18N.get("org.openjump.core.ui.plugin.view.ZoomToScalePlugIn.zoom-to-scale"), 
				true);
	        setDialogValues(dialog, context);
	        GUIUtil.centreOnWindow(dialog);
	        dialog.setVisible(true);
	        if (! dialog.wasOKPressed()) { return false; }
	        getDialogValues(dialog);
	        
	    zoomToNewScale(context);
        
	    return true;
	}
	
	public void zoomToNewScale(PlugInContext context) throws Exception {
	    Viewport port = context.getLayerViewPanel().getViewport();
        this.oldHorizontalScale = ScreenScale.getHorizontalMapScale(port);
        
        //-- get zoom factor
        double factor = this.scale/this.oldHorizontalScale;

        //--calculating new screen using the envelope of the corner LineString 
        Envelope oldEnvelope = port.getEnvelopeInModelCoordinates();

        double xc = 0.5*(oldEnvelope.getMaxX() + oldEnvelope.getMinX());
        double yc = 0.5*(oldEnvelope.getMaxY() + oldEnvelope.getMinY());
        double xmin = xc - 1/2.0 * factor * oldEnvelope.getWidth();
        double xmax = xc + 1/2.0 * factor * oldEnvelope.getWidth();
        double ymin = yc - 1/2.0 * factor * oldEnvelope.getHeight();
        double ymax = yc + 1/2.0 * factor * oldEnvelope.getHeight();
        Coordinate[] coords = new Coordinate[]{
            new Coordinate(xmin,ymin), new Coordinate(xmax,ymax)};
        Geometry g1 = new GeometryFactory().createLineString(coords);       
        port.zoom(g1.getEnvelopeInternal());
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        dialog.addLabel(I18N.get("org.openjump.core.ui.plugin.view.ZoomToScalePlugIn.actual-scale-in-horizontal-direction") + " 1 : " +(int)this.oldHorizontalScale);
	    dialog.addIntegerField(text, scale, 7, text);	    
	}

	private void getDialogValues(MultiInputDialog dialog) {
	    this.scale = dialog.getInteger(text);    	    
	}
	
	public void setScale(double scale) {
	    this.scale = (int)scale;
	}
    
}
