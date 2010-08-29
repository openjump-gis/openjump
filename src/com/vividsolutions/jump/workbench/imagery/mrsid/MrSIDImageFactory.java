package com.vividsolutions.jump.workbench.imagery.mrsid;

/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
import java.io.File;

import org.apache.log4j.Logger;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;

public class MrSIDImageFactory implements ReferencedImageFactory {

	private Logger logger = Logger.getLogger(MrSIDImageFactory.class);
			
	//[sstein 19Apr2008] -- new
    public static String WORKING_DIR;
    public static String ETC_PATH;
    public static String TMP_PATH;
    public static String MRSIDDECODE;
    public static String MRSIDINFO;
    //--
    public static final String MRSIDDECODEFILE = "mrsidgeodecode.exe";
    public static final String MRSIDINFOFILE = "mrsidgeoinfo.exe";
    
	final static String sNotInstalled=I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.not-installed");
	final static String sErrorSeeOutputWindow =I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.Error-See-Output-Window");
	
    public String getTypeName() {
        return "MrSID";
    }

    public ReferencedImage createImage(String location) throws JUMPException {
        return new MrSIDReferencedImage(SIDInfo.readInfo(location),location);
    }

    public String getDescription() {
        return getTypeName();
    }

    public String[] getExtensions() {
        return new String[] { "sid" };
    }

    public boolean isEditableImage(String location) {
        return false;
    }

	public boolean isAvailable(WorkbenchContext context) {
		int i = -1;
		// [sstein 19.Apr.2008] replaced with old code from AddSIDLayerPlugIn 
//		try{
			/*
			Process p = Runtime.getRuntime().exec(MRSIDINFO+" -h");
	        p.waitFor();
	        i = p.exitValue();
	        p.destroy();
	        */
			//-- new
			File empty = new File("");
			String sep = File.separator;
			try{
				WORKING_DIR = context.getWorkbench().getPlugInManager().getPlugInDirectory() + sep;
			}
			catch(Exception e){//eat it (the PlugInDirectory may be "null")
				return false;
			}
		    ETC_PATH = WORKING_DIR + "etc" + sep;
		    TMP_PATH = WORKING_DIR + "etc" + sep + "tmp" + sep;
	        MRSIDDECODE = ETC_PATH + MRSIDDECODEFILE;
	        MRSIDINFO = ETC_PATH + MRSIDINFOFILE;
	        
            if (!new File(MRSIDDECODE).exists())
            {
            	//-- error messages can not be send, as the workbench does not exist yet 
                //context.getWorkbench().getFrame().warnUser(sErrorSeeOutputWindow);                
                //context.getWorkbench().getFrame().getOutputFrame().addText(MRSIDDECODE + " " + sNotInstalled);
            	logger.warn(MRSIDDECODE + " " + sNotInstalled);
                return false;
            }
            
            if (!new File(MRSIDINFO).exists())
            {
            	//-- error messages can not be send, as the workbench does not exist yet
                //context.getWorkbench().getFrame().warnUser(sErrorSeeOutputWindow);                
                //context.getWorkbench().getFrame().getOutputFrame().addText(MRSIDINFO + " " + sNotInstalled);
            	logger.warn(MRSIDINFO + " " + sNotInstalled);
                return false;
            }
         logger.trace("found Mrsid decode files");
         return true;
        //-- end new stuff   
//		}catch(IOException e){
//			// eat it
//			return false;
//		} catch (InterruptedException e) {
//			// eat it
//			return false;
//		}
//        
//        return i == 0;
	}

}