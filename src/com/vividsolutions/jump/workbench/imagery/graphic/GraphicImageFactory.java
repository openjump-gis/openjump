package com.vividsolutions.jump.workbench.imagery.graphic;

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
import java.io.FilenameFilter;
import java.io.IOException;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;

public class GraphicImageFactory implements ReferencedImageFactory {
    public String getTypeName() {
        return "Picture";
    }

    public ReferencedImage createImage(String location) {

        WorldFile wf = findWF(location);
        
        return new GraphicImage(new File(location), wf);
    }

    private WorldFile findWF(final String location) {
		File f = new File(location);
		if(f!=null && f.exists()){
			File parent = f.getParentFile();
			String name = f.getName();
			
			int i = name.indexOf(".");
			name = i == -1?name:name.substring(0,i);
			final String nm = name;
			File[] children = parent.listFiles(new FilenameFilter(){
				public boolean accept(File dir, String name) {
					String suffix = name.substring(name.indexOf(".")+1);
					if(suffix!= null)
						if(suffix.equalsIgnoreCase("jgw") || suffix.equalsIgnoreCase("tfw") || suffix.equalsIgnoreCase("gfw") || suffix.equalsIgnoreCase("bpw") )
							return name.startsWith(nm);
					if(name!=null && name.equalsIgnoreCase(location+"w"))
						return true;
					return false;
				}
			});
			if(children!=null){
				for(int c=0;c<children.length;c++){
					if(children[c].exists() && !children[c].isDirectory()){
						WorldFile wf;
						try {
							wf = WorldFile.read(children[c]);
							if(wf != null)
								return wf;
						} catch (IOException e) {
							// eat it ... we are guessing here
						}
					}
				}
			}
		}
		return null;
	}

	public String getDescription() {
        return getTypeName();
    }

    public String[] getExtensions() {
        return new String[] { "gif", "png", "jpg", "jpeg" };
    }

    public boolean isEditableImage(String location) {
        return true;
    }

	public boolean isAvailable(WorkbenchContext context) {
		Class c = null;
		try{
			c = this.getClass().getClassLoader().loadClass("javax.media.jai.JAI");
		}catch(ClassNotFoundException e){
			// eat it
			return false;
		}
		
		return c != null;
	}

}