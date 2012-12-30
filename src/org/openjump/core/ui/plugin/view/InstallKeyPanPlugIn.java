/**
 * 
 */
package org.openjump.core.ui.plugin.view;

import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JInternalFrame;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
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
    private static final int ZOOM_FULL = 6;

    /* matrix defining directions */
    private static final int[][] DIRECTIONS = 
        {   {0, -1, 0, -1}, //NORTH
            {-1, 0, -1, 0}, //EAST
            {0, 1, 0, 1},
            {1, 0, 1, 0},
            {1, 1, -1, -1}, //ZOOM_IN
            {-1, -1, 1, 1}};//ZOOM_OUT
          
    
    private static double panPercentage;
    
    PlugIn zoom_in, zoom_out;
    PlugIn[] plugIns =  
        { this, new PanHelper( NORTH ), new PanHelper( EAST ), 
            new PanHelper( SOUTH ), new PanHelper( WEST ), 
            zoom_in=new PanHelper( ZOOM_IN ), zoom_out=new PanHelper( ZOOM_OUT ),
            zoom_in, zoom_out, new PanHelper( ZOOM_FULL )};
    
    int[] keys = { KeyEvent.VK_HOME, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, 
            KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_PLUS, 
            KeyEvent.VK_MINUS, KeyEvent.VK_ADD, KeyEvent.VK_SUBTRACT,
            KeyEvent.VK_0};
    /**
     * Default constructor 
     */
    public InstallKeyPanPlugIn() {
        this( 0.5 );
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
        return true;
    }

    private static Envelope createEnvelopeFromDirection( Envelope oldEnvelope, int mode) {
        
        double oldWidth = panPercentage*oldEnvelope.getWidth();
        double oldHeight = panPercentage*oldEnvelope.getHeight();
        
        double dxPlus = DIRECTIONS[ mode ][0]*oldWidth;
        double dyPlus = DIRECTIONS[ mode ][1]*oldHeight;
        double dxMinus = DIRECTIONS[ mode ][2]*oldWidth;
        double dyMinus = DIRECTIONS[ mode ][3]*oldHeight;
        
        return new Envelope(
                        oldEnvelope.getMinX() - dxMinus, 
                        oldEnvelope.getMaxX() - dxPlus,
                        oldEnvelope.getMinY() - dyMinus,
                        oldEnvelope.getMaxY() - dyPlus);
    }
    
    private boolean pan(JInternalFrame jif, int mode) {
      if (jif instanceof TaskFrame) {
        try {
          TaskFrame taskFrame = (TaskFrame) jif;
          LayerViewPanel lvp = taskFrame.getLayerViewPanel();
          Viewport vp = lvp.getViewport();
          // zoom to full
          if (mode == ZOOM_FULL){
            vp.zoomToFullExtent();
            return true;
          }

          Envelope oldEnvelope = vp.getEnvelopeInModelCoordinates();
          vp.zoom(createEnvelopeFromDirection(oldEnvelope, mode));
        } catch (NoninvertibleTransformException e1) {
          e1.printStackTrace();
          return false;
        }
      }
      return true;
    }

    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);
        
 
        
    }

    public boolean registerShortcut() {
      for (int i = 0; i < keys.length; i++) {
        JUMPWorkbench.getWorkBench().getFrame()
            .addKeyboardShortcut(keys[i], KeyEvent.CTRL_MASK, plugIns[i], null);
      }
      return true;
    }

    public String getName() { return ""; }

    /**
     * Get the pan/zoom percentage, a value between 0 and 1. Deafult is 0.25 (=25%)
     */
    public static double getPanPercentage() {
        return 2*panPercentage;
    }

    /**
     * Set the pan percentage. Legal values are between greater than 0 and less than 
     * or equal to 1.0
     * @param panPercent The value in percent of screen size to pan/zoom. Accepted 
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
        private final int mode;
        public PanHelper( int mode ) {
            this.mode = mode;
        }
        public boolean execute(PlugInContext context) throws Exception {
            return pan( context.getWorkbenchFrame().getActiveInternalFrame(), mode );
        }
        public String getName() {
          return "PanHelper";
        }
    }

}