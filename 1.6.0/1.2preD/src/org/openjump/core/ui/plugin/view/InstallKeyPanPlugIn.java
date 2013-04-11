/**
 * 
 */
package org.openjump.core.ui.plugin.view;

import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JInternalFrame;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * Plug in for navigation with keyboards keys. <br/>
 * Navigation is as follows:<br/>
 * Arrows keys move the viewport.<br/>
 * Page down and up are zoom in and zoom out, respectively<br/>
 * Home key zooms in or out to full extent<br/>
 * Pan and zoom percentage is, by default, 20%
 * 
 * @author Ugo Taddei <taddei@lat-lon.de>
 *
 */
public class InstallKeyPanPlugIn extends AbstractPlugIn {

    private static final int NORTH = 0; 
    private static final int EAST  = 1; 
    private static final int SOUTH = 2; 
    private static final int WEST  = 3; 
    
    
    private static final int ZOOM_IN  = 4;   
    private static final int ZOOM_OUT = 5; 
    
    /* matrix defining directions */
    private static final int[][] DIRECTIONS = 
        {   {0, -1, 0, -1}, //NORTH
            {-1, 0, -1, 0}, //EAST
            {0, 1, 0, 1},
            {1, 0, 1, 0},
            {1, 1, -1, -1}, //ZOOM_IN
            {-1, -1, 1, 1}};//ZOOM_OUT
    
    private static double panPercentage;
    
    /**
     * Default constructor 
     */
    public InstallKeyPanPlugIn() {
        this( 0.2 );
    }

    /**
     * 
     * Creates a new plug-in with pan_percentage as pan percentage value
     *   
     * pan_percentage The value in percent of screen size to pan/zoom. Accepted 
     * values are in the range 0 < percentage <= 1
     */
    public InstallKeyPanPlugIn( double panPercentag ) {
        super();
        setPanPercentage( panPercentag );
    }

    public boolean execute(PlugInContext context) throws Exception {
        context.getLayerViewPanel().getViewport().zoomToFullExtent();
        return true;
    }
    
    private static Envelope createEnvelopeFromDirection( Envelope oldEnvelope, int direction) {
        
        double oldWidth = panPercentage*oldEnvelope.getWidth();
        double oldHeight = panPercentage*oldEnvelope.getHeight();
        
        double dxPlus = DIRECTIONS[ direction ][0]*oldWidth;
        double dyPlus = DIRECTIONS[ direction ][1]*oldHeight;
        double dxMinus = DIRECTIONS[ direction ][2]*oldWidth;
        double dyMinus = DIRECTIONS[ direction ][3]*oldHeight;
        
        return new Envelope(
                        oldEnvelope.getMinX() - dxMinus, 
                        oldEnvelope.getMaxX() - dxPlus,
                        oldEnvelope.getMinY() - dyMinus,
                        oldEnvelope.getMaxY() - dyPlus);
    }
    
    public boolean pan( JInternalFrame jif, int direction ) {

        if (jif instanceof TaskFrame) {
            TaskFrame taskFrame = (TaskFrame) jif;
            LayerViewPanel lvp = taskFrame.getLayerViewPanel();
            Viewport vp = lvp.getViewport();
            Envelope oldEnvelope = vp.getEnvelopeInModelCoordinates();
            
            try {
                vp.zoom(
                    createEnvelopeFromDirection( oldEnvelope, direction) );
            } catch (NoninvertibleTransformException e1) {
                e1.printStackTrace();
                return false;
            }                
        } 
        return true;
    }
    
    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);
        
        AbstractPlugIn[] plugIns =  
            { this, new PanHelper( NORTH ), new PanHelper( EAST ), 
                new PanHelper( SOUTH), new PanHelper( WEST ), 
                new PanHelper( ZOOM_IN ), new PanHelper( ZOOM_OUT ) };
        
        int[] keys = { KeyEvent.VK_HOME, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, 
                KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_PAGE_DOWN, 
                KeyEvent.VK_PAGE_UP };
        
        for (int i = 0; i < keys.length; i++) {
            context.getWorkbenchContext().getWorkbench().getFrame()
                .addKeyboardShortcut( keys[i], 0, plugIns[i], null);
            
        }
        
    }
    
    public String getName() { return ""; }
   
    /**
     * Get the pan/zoom percentage, a value between 0 and 1. Deafult is 0.25 (=25%) 
     * @return
     */
    public static double getPanPercentage() {
        return 2*panPercentage;
    }
    
    /**
     * Set the pan percentage. Legal values are between greater than 0 and less than 
     * or equal to 1.0
     * @param panPercentage The value in percent of screen size to pan/zoom. Accepted 
     * values are in the range 0 < percentage <= 1
     */
    public static void setPanPercentage(double panPercent ) {
        if ( panPercent <= 0 || panPercent > 1d ) {
            throw new IllegalArgumentException( "Accepted values are in the "
                    +" range 0 < percentage <= 1" );
        }
        //have percentage, otherwise it's percentage value in each direction
        //making it twice as much!
        panPercentage = panPercent/2d;
    }

    /**
     * Helper class to pan in the direction given in constructor 
     */
    private class PanHelper extends AbstractPlugIn {
        private final int direction;
        public PanHelper( int direction ) {
            this.direction = direction;
        }
        public boolean execute(PlugInContext context) throws Exception {
            return pan( context.getWorkbenchFrame().getActiveInternalFrame(), direction );
        }
    }

}