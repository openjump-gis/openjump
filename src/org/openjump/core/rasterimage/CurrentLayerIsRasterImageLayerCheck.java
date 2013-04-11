/*
 * Created on 05.07.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2434 $
 *  $Date: 2006-09-12 12:31:50 +0200 (Di, 12 Sep 2006) $
 *  $Id: CurrentLayerIsRasterImageLayerCheck.java 2434 2006-09-12 10:31:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.rasterimage;

import javax.swing.JComponent;

import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * Enable check (to controll menu items) that checks, if the selected layer is a RasterImage Layer<br>
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2434 $
 * 
 */
public class CurrentLayerIsRasterImageLayerCheck implements EnableCheck {

    protected PlugInContext context = null;
    
    protected EnableCheck checker = null;

    public CurrentLayerIsRasterImageLayerCheck(PlugInContext context) {
        super();
        this.context = context;
        this.checker = new EnableCheckFactory(context.getWorkbenchContext()).createExactlyNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class);
    }
    
    /**
     *@inheritDoc
     */
    public String check(JComponent component) {
        return this.checker.check(component);        
    }

}
