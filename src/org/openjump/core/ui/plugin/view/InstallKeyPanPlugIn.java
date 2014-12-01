/**
 * 
 */
package org.openjump.core.ui.plugin.view;

import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JInternalFrame;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiShortcutEnabled;
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
public class InstallKeyPanPlugIn extends AbstractPlugIn implements MultiShortcutEnabled {

    private static final int NORTH = 0; 
    private static final int EAST  = 1; 
    private static final int SOUTH = 2; 
    private static final int WEST  = 3; 
    
    private static final int ZOOM_IN  = 4;
    private static final int ZOOM_OUT = 5;
    private static final int ZOOM_FULL = 6;
    
    public static final String sPAN_NORTH = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.pan-north");
    public static final String sPAN_EAST  = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.pan-east");
    public static final String sPAN_SOUTH = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.pan-south");
    public static final String sPAN_WEST  = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.pan-west");
    public static final String sZOOM_IN   = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.zoom-in");
    public static final String sZOOM_OUT  = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.zoom-out");
    public static final String sZOOM_EXT  = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.zoom-to-full-extent");
    public static final String sACCEPTED_VALUES = I18N.get("org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn.accepted-values");

    /* matrix defining directions */
    private static final int[][] DIRECTIONS = 
        {   {0, -1, 0, -1}, //NORTH
            {-1, 0, -1, 0}, //EAST
            {0, 1, 0, 1},
            {1, 0, 1, 0},
            {1, 1, -1, -1}, //ZOOM_IN
            {-1, -1, 1, 1}};//ZOOM_OUT
          
    
    private static double panPercentage;
    
    AbstractPlugIn[] plugIns = { 
        new PanHelper(sPAN_NORTH, NORTH),
        new PanHelper(sPAN_EAST, EAST), 
        new PanHelper(sPAN_SOUTH, SOUTH),
        new PanHelper(sPAN_WEST, WEST),
        new PanHelper(sZOOM_IN, ZOOM_IN), new PanHelper(sZOOM_OUT, ZOOM_OUT), 
        new PanHelper(sZOOM_IN, ZOOM_IN), new PanHelper(sZOOM_OUT, ZOOM_OUT),
        new PanHelper(sZOOM_EXT, ZOOM_FULL) };
  
    int[] keys = { KeyEvent.VK_UP, KeyEvent.VK_RIGHT, 
        KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, 
        KeyEvent.VK_PLUS, KeyEvent.VK_MINUS, 
        KeyEvent.VK_ADD, KeyEvent.VK_SUBTRACT, 
        KeyEvent.VK_0 };
    
    private static EnableCheck check = EnableCheckFactory.getInstance()
        .createTaskWindowMustBeActiveCheck();

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
    public InstallKeyPanPlugIn( double panPercentage ) {
        super();
        setPanPercentage( panPercentage );
        
        // assign shortcuts to plugins
        for (int i = 0; i < keys.length; i++) {
          plugIns[i].setShortcutKeys(keys[i]);
          plugIns[i].setShortcutModifiers(KeyEvent.CTRL_MASK);
        }
    }

    public boolean execute(PlugInContext context) throws Exception {
        return true;
    }

    /**
     * Get the pan/zoom percentage, a value between 0 and 1. Deafult is 0.25 (=25%)
     */
    public double getPanPercentage() {
        return 2*panPercentage;
    }

    /**
     * Set the pan percentage. Legal values are between greater than 0 and less than 
     * or equal to 1.0
     * @param panPercent The value in percent of screen size to pan/zoom. Accepted 
     * values are in the range 0 < percentage <= 1
     */
    public void setPanPercentage(double panPercent ) {
        if ( panPercent <= 0 || panPercent > 1d ) {
            throw new IllegalArgumentException(sACCEPTED_VALUES);
        }
        //have percentage, otherwise it's percentage value in each direction
        //making it twice as much!
        panPercentage = panPercent/2d;
    }

    public PlugIn[] getShortcutEnabledPlugins() {
      return plugIns;
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

    private static boolean pan(JInternalFrame jif, int mode) {
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

    /**
     * Helper class to pan in the direction given in constructor 
     */
    private class PanHelper extends AbstractPlugIn {
        private final int mode;
        private String name;
        public PanHelper( String name, int mode ) {
          this.mode = mode;
          this.name = name;
        }
        public boolean execute(PlugInContext context) throws Exception {
          return pan( context.getWorkbenchFrame().getActiveInternalFrame(), mode );
        }
        public String getName() {
          return name;
        }
        public EnableCheck getEnableCheck(){
          return check;
        }
    }

}