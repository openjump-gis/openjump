/*
 * Created on 16.02.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.plugIns;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorDialog;

import de.fho.jump.pirol.utilities.apiTools.FeatureCollectionTools;
import de.fho.jump.pirol.utilities.apiTools.LayerTools;
import de.fho.jump.pirol.utilities.apiTools.SelectionTools;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;
import de.fho.jump.pirol.utilities.i18n.PirolPlugInMessages;
import de.fho.jump.pirol.utilities.settings.PirolPlugInSettings;

/**
 * Overrides most of the abstract methods of the AbstractPlugIn to implement
 * a default behavior, that fits the needs for a plugIn in the PIROL context.
 * Also implements the ErrorHandler interface and offers methods to post error
 * messages to the GUI.
 * 
 * @author Ole Rahn, Stefan Ostermann
 * <br>
 * <br>FH Osnabrück - University of Applied Sciences Osnabrück,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 */
public abstract class StandardPirolPlugIn extends AbstractPlugIn implements ErrorHandler {
    
    /**
     * buffer for messages of errors that occured during execution - needed to implement the ErrorHandler interface
     */
    protected StringBuffer messages = new StringBuffer();
    /**
     * Are errors to be shown in the statusbar (bringUpMessages = false, DEFAULT) or in an
     * output window (bringUpMessages = true)?
     */
    protected boolean bringUpMessages = false;
    
    protected static EnableCheckFactory checkFactory = null;
    
    /**
     * see description for method getIconString() for more information
     */
    protected ImageIcon icon = null;
    protected boolean useToolIconSize = false;
    
    protected static GeometryFactory geometryFactory = new GeometryFactory();
    
    /**
     * logger reference for use with the StandardPirolPlugIn. Since loggers are personalized, this member has to be
     * initialized in derived classes by the programmer, before it can be used. For downward compatibility no abstract
     * method or constructur parameter was introduced to do this.
     * @since rev.1.12
     */
    protected PersonalLogger logger = null;
    
    /**
     * Please, use this constructor in the deriving class!
     *@param logger the logger that will controll console outputs
     *@since rev.1.17
     */    
    public StandardPirolPlugIn(PersonalLogger logger) {
        super();
        this.logger = logger;
    }


    public String getShortClassName(){
        int pointPos = this.getClass().getName().lastIndexOf(".");
        if (pointPos > -1) {
            return this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1);
        }
        this.logger.printWarning("class name is very short: " + this.getClass().getName());
        return this.getClass().getName();
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext, boolean needFence) {
        if (StandardPirolPlugIn.checkFactory == null){
            StandardPirolPlugIn.checkFactory = new EnableCheckFactory(workbenchContext);
        }
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        
        multiEnableCheck.add( StandardPirolPlugIn.checkFactory.createAtLeastNLayersMustExistCheck(1) );
        multiEnableCheck.add( StandardPirolPlugIn.checkFactory.createAtLeastNLayersMustBeSelectedCheck(1) );
        if (needFence)
            multiEnableCheck.add( StandardPirolPlugIn.checkFactory.createFenceMustBeDrawnCheck() );
        return multiEnableCheck;
	}

    /**
     * Method to enable loading an icon from the surrounding jar-file if necessary.
     * If the plugin doesn't need an icon, just return <code>null</code> else
     * return the file name, e.g. "xyz.png" and put the picture in the same folder
     * (package) as the deriving class. 
     * @return the filename of the icon or <code>null</code> if the plugin has no icon.
     */
	public abstract String getIconString();
	
	/**
	 * This method is called to execute the PlugIn.
	 * @return always <code>true</code> ?
	 * @param context the snapshot of the current workbench.
	 * @throws Exception
	 * @see com.vividsolutions.jump.workbench.plugin.AbstractPlugIn#execute(com.vividsolutions.jump.workbench.plugin.PlugInContext)
	 */
    public abstract boolean execute(PlugInContext context) throws Exception;
    
    /**
     * Name of the PlugIn to be shown in the menus or as a tooltip in JUMP.<br>
     * Looks for a key (the PlugIn's name with no path) in the i18n resources, if none is found
     * the standard jump name generation will be used.
     */
	public String getName() {
		try {
            return PirolPlugInMessages.getString(this.getShortClassName());
        } catch (RuntimeException e) {
            return super.getName();
        }
        
	}
    
    
	/**
     * The PlugIn will be added to the <code>PIROL Tools/<b>[return value of getCategoryName()]</b></code> menu and a standard {@link com.vividsolutions.jump.workbench.plugin.EnableCheck} is applied.
     *@param context PlugInContext
     *@throws Exception
     *@see StandardPirolPlugIn#getCategoryName()
	 */
	public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem( this, new String[]{PirolPlugInSettings.getName_PirolMenu(), getCategoryName() }, this.getName(), false, null, StandardPirolPlugIn.createEnableCheck(context.getWorkbenchContext(),false));
    }
    
    /**
     * The name of the category, a PlugIn can be found in - this method should be overridden by any derived class! 
     *@return name of the category, a PlugIn can be found in
     */
    public String getCategoryName(){
        return PirolPlugInMessages.getString("unsorted");
    }

    /**
	 * Standard intialization: PlugIn is added to the <code>PIROL Tools/<b>subMenuName</b></code> (or localized menu name) menu and a standard {@link com.vividsolutions.jump.workbench.plugin.EnableCheck} is applied.
	 * @deprecated
	 */
	public void initialize(PlugInContext context, String subMenuName) throws Exception {
        if (this.logger!=null)
            this.logger.printDebug("subMenuName: " + subMenuName);
		context.getFeatureInstaller().addMainMenuItem( this, new String[]{PirolPlugInSettings.getName_PirolMenu(), subMenuName }, this.getName(), false, null, StandardPirolPlugIn.createEnableCheck(context.getWorkbenchContext(),false));
	}

	/**
	 *@param context the current PlugIn context
	 *@return a TaskMonitorDialog, to show progress information to the user
	 */
	public static TaskMonitorDialog getMonitor(PlugInContext context){
	    return new TaskMonitorDialog( context.getWorkbenchFrame(), context.getWorkbenchFrame() );
	}
	
	/**
	 * Prints output to the stdout. Can be invoked by deriving classes.
	 * @param function the name of the method from within this one is called.
	 * E.g. "myMethod()".
	 * @param msg the message String.
	 * @see #println(Class, String, String)
	 * @deprecated use logger instead!
	 */
	protected void println( String function,  String msg ){
        if (this.logger != null){
            this.logger.printDebug(msg);
        } else {
            println( this.getClass(), function, msg );
        }
	}
	
	/**
	 * Writes the given message and function string to the stdout.
	 * The output will be formated as "classname.function: message".
	 * @param c the calling class
	 * @param function the method from within this one is called.
	 * @param message the text with some useful information.
	 * @deprecated use logger instead!
	 */
	public static void println( Class c, String function, String message ){
		System.out.println( c.getName() + "." + function + "(): " + message );
	}
	
	/**
	 * Puts a warning message into the statusbar of JUMP 
	 *@param context curr. PlugInContext
	 *@param msg message to be shown to the user
	 */
	public static void warnUser(PlugInContext context, String msg){
	    context.getWorkbenchFrame().warnUser(msg);
	}
	
	/**
	 * Get a given number of selected Layers.
	 * @param context the current PlugInContext
	 * @param num max. number of layers to return, -1 returns all selected layers
	 * @return a given number of selected Layers, null if no Layers are selected
	 */
	public static Layer[] getSelectedLayers(PlugInContext context, int num){
	    return LayerTools.getSelectedLayers(context, num);
	}
	
	/**
	 * get one Layer that is selected
	 * @param context the current PlugInContext
	 * @return one selected Layer, null if no Layers are selected
	 */
	public static Layer getSelectedLayer(PlugInContext context){
	    return LayerTools.getSelectedLayer(context);
	}
    
    
	
	/**
	 * To be called, when leaving the execute()-method. Puts out the error messages
	 * collected during execution if any and returns the given boolean value.
	 * Example:<pre>return finishExecution(context, true);</pre>
	 *@param context the current PlugInContext
	 *@param retVal the value to be returned by execute()
	 *@return retVal
	 */
	protected boolean finishExecution( PlugInContext context, boolean retVal ){
	    this.postMessagesToGui(context);
	    return retVal;
	}
	
	protected void postMessagesToGui(PlugInContext context){
	    if (messages.length()!=0 && context!=null){
			context.getOutputFrame().createNewDocument();
			context.getOutputFrame().addText( messages.toString() );
			if (bringUpMessages)
			    context.getOutputFrame().surface();
			messages.delete(0,messages.length());
		} else if (context==null){
		    this.println( "postMessagesToGui(PlugInContext context)", "-> context == null!");
		}
	}
	
    public void handleThrowable(Throwable t) {
        if (t.getMessage()==null || t.getMessage().equals("null")){
            StackTraceElement[] stre = t.getStackTrace();
            for (int i=0; i<stre.length; i++){
                messages.append(stre[i].toString() + "\n");
            }
        } else if (t.getMessage()!=null){
            messages.append(t.getMessage() + "\n");
        }
    }
    
    /**
     * Method to load an icon from the surrounding jar-file if necessary.
     * @return the icon or <code>null</code> if the getIconString() returned null (or problems occured)
     */
    public Icon getIcon() {
        
		if ( icon == null && this.getIconString() != null){
			InputStream in = this.getClass().getResourceAsStream(this.getIconString());
			BufferedImage img = null;
            Image scaledImg = null;
			try {
				img = ImageIO.read(in);
                scaledImg = img.getScaledInstance(this.useToolIconSize?PirolPlugInSettings.StandardToolIconWidth:PirolPlugInSettings.StandardPlugInIconWidth, this.useToolIconSize?PirolPlugInSettings.StandardToolIconHeight:PirolPlugInSettings.StandardPlugInIconHeight, img.getType());
			} catch (IOException e) {
				e.printStackTrace();
				img = null;
				icon = null;
			}
			if (scaledImg != null){
				icon = new ImageIcon(scaledImg);
			}
		}
		return icon;
	}
    
    /**
     * Get a List of Features (from the given Layer) that reside within the fence (if there is a fence) or just the features of the layer
     * (if there is currently no fence) 
     *@param context the plugIn context
     *@param layer the layer holding the features
     *@return List of features within fence if present or just within the layer 
     */
    public static Feature[] getFeaturesInFenceOrInLayer(PlugInContext context,Layer layer){
        SelectionTools st = new SelectionTools(context);
        Feature[] featureToBeUsed = null;
        
        if (st.getFenceGeometry()==null){
            featureToBeUsed = FeatureCollectionTools.FeatureCollection2FeatureArray(layer.getFeatureCollectionWrapper().getUltimateWrappee());
        } else {
            featureToBeUsed = SelectionTools.getFeaturesInFenceInLayer(layer, st.getFenceGeometry());
        }
        
        return featureToBeUsed;
    }
    

    /**
     *@return the personal logger
     *@see PersonalLogger
     */
    public PersonalLogger getLogger() {
        return logger;
    }
    /**
     * Sets the personal logger - enables debug statement handling
     *@param logger the new personal logger
     *@see PersonalLogger
     */
    public void setLogger(PersonalLogger logger) {
        this.logger = logger;
    }
    
    public void setUseInToolbox(boolean toolboxUse){
    	this.useToolIconSize = toolboxUse;
    }
}
