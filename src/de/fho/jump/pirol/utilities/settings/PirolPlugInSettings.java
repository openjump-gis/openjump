/*
 * Created on 30.11.2004
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$s
 *  $Author$
 */
package de.fho.jump.pirol.utilities.settings;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;
import de.fho.jump.pirol.utilities.i18n.PirolPlugInMessages;

/**
 * Holds general information, that have to be available everywhere in the project.
 * 
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 */
public class PirolPlugInSettings {
    
    protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);
    
    private static final String KEY_PIROLMENUNAME = "PirolMenuName";
    
    private static final String KEY_RESULTCATEGORYNAME = "ResultCategoryName";
    
    private static final String KEY_COORDINATEMENUNAME = "CoordinateMenuName";
    private static final String KEY_ATTRIBUTEMENUNAME = "AttributeMenuName";
    private static final String KEY_SELECTIONMENUNAME = "SelectionMenuName";
    private static final String KEY_VISUALTOOLSMENUNAME = "VisualTools";
    private static final String KEY_CONTEXTMENUNAME = "ContextInformation";
    private static final String KEY_TRANSFERMENUNAME = "TransferTools";
    private static final String KEY_DATAPRCESSINGMENUNAME = "DataProcessingMenuName";
    
    public final static int StandardPlugInIconWidth = 16, StandardPlugInIconHeight = 16;
    public final static int StandardToolIconWidth = 24, StandardToolIconHeight = 24;
    
    private final static String DIRECTORYNAME_OPENJUMP_IN_HOME = ".OpenJump_PIROL";
    
    private static String USERSHOMEDIR = null;
    
    /**
     * Default key to store a workbench context in a blackboard.
     */
    public final static String KEY_WORKBENCHCONTEXT_IN_BLACKBOARD = "workbenchContext";

	public static String getName_PirolMenu(){
		return PirolPlugInMessages.getString(KEY_PIROLMENUNAME);
	}
	
	public static String getName_ProcessingMenu(){
		return PirolPlugInMessages.getString(KEY_DATAPRCESSINGMENUNAME);
	}
	
	public static String getName_CoordinateMenu(){
		return PirolPlugInMessages.getString(KEY_COORDINATEMENUNAME);
	}
    
    public static String getName_AttributeMenu(){
        return PirolPlugInMessages.getString(KEY_ATTRIBUTEMENUNAME);
    }
    
    public static String getName_ContextInformationMenu(){
        return PirolPlugInMessages.getString(KEY_CONTEXTMENUNAME);
    }
    
    public static String getName_TransferMenu(){
        return PirolPlugInMessages.getString(KEY_TRANSFERMENUNAME);
    }
    
    public static String getName_SelectionMenu(){
        return PirolPlugInMessages.getString(KEY_SELECTIONMENUNAME);
    }
    
    /**
     *@return Name for the tools sub menu containing tools that display something
     */
    public static String getName_VisualToolsMenu(){
        return PirolPlugInMessages.getString(KEY_VISUALTOOLSMENUNAME);
    }
	
	public static String resultLayerCategory(){
		return PirolPlugInMessages.getString(KEY_RESULTCATEGORYNAME);
	}
	
	public static File configDirectory(){
        String usersHomeDir = getUsersHomeDir();

		File dir = null;
		
		dir = new File( usersHomeDir + File.separator + DIRECTORYNAME_OPENJUMP_IN_HOME + File.separator + "config" + File.separator );
		
	    if (!dir.exists()){
            dir.mkdirs();
	    }
	    
	    return dir;
	}
	
	public static File tempDirectory(){
        String usersHomeDir = getUsersHomeDir();
        
		File dir = null;
		
		dir = new File( usersHomeDir + File.separator + DIRECTORYNAME_OPENJUMP_IN_HOME + File.separator + "tmp" + File.separator );
		
	    if (!dir.exists()){
            dir.mkdirs();
	    }
	    
	    return dir;
	}
    
    private final static String getUsersHomeDir(){
        while (USERSHOMEDIR == null){
            try {
            	if (USERSHOMEDIR==null){
            		USERSHOMEDIR = System.getProperty("user.home");
            	}
                
                if (USERSHOMEDIR!=null){
                	USERSHOMEDIR = new String(USERSHOMEDIR);
                    break;
                }
                
                System.out.println("... waiting to receive the user's HOME directory");
                Thread.sleep(100);
                
            } catch (InterruptedException e) {}
        }
        return USERSHOMEDIR;
    }
    
    /**
     * 
     *@return the standard number format to be used in all dialogs, etc. (... from now on)
     */
    public static NumberFormat getDefaultNumberFormat(){
        DecimalFormat doubleFormat = new DecimalFormat();
        doubleFormat.setGroupingUsed(false);
        
        return doubleFormat;
    }
    
}
