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
import java.io.IOException;

import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;

public class MrSIDImageFactory implements ReferencedImageFactory {
    public static final String MRSIDDECODE = "mrsidgeodecode.exe";
    public static final String MRSIDINFO = "mrsidgeoinfo.exe";
    
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

	public boolean isAvailable() {
		int i = -1;
		try{
			Process p = Runtime.getRuntime().exec(MRSIDINFO+" -h");
	        p.waitFor();
	        i = p.exitValue();
	        p.destroy();
		}catch(IOException e){
			// eat it
			return false;
		} catch (InterruptedException e) {
			// eat it
			return false;
		}
        
        return i == 0;
	}

}