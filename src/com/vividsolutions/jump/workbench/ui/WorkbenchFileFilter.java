
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

package com.vividsolutions.jump.workbench.ui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Workbench filter that filters by fileType.
 */

public class WorkbenchFileFilter extends FileFilter {
    private String description;

    //CVS issue:
    //
    //This class used to be called JcsFileFilter. I renamed it to JCSFileFilter
    //and the Unix developers reported they were getting errors. So I deleted
    //the file from the CVS repository and tried to re-add it, but I got
    //CVS errors:
    //
    //  cvs server: cannot add file `com/vividsolutions/jcs/workbench/ui/JCSFileFilter.java'
    //  when RCS file `/home/cvs/jcs/jcs/com/vividsolutions/jcs/workbench/ui/JCSFileFilter.java,v'
    //  already exists
    //  cvs [server aborted]: correct above errors first!
    //
    //This is a CVS bug mentioned on the Usenet newsgroups (deleting and re-adding
    //a file). So I just renamed it to something totally different:
    //WorkbenchFileFilter. [Jon Aquino]
    

    public WorkbenchFileFilter(String fileType) {
        description = fileType;
    }

    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        String extension = GUIUtil.getExtension(file);

        if (extension != null) {
            //changed by david to be more readable and maintainable
            if (description.equals(GUIUtil.jmlDesc)) {
                return extension.equals(GUIUtil.jml) ||
                extension.equals("zip") || extension.equals("gz");
            }

            if (description.equals(GUIUtil.xmlDesc)) {
                return extension.equals(GUIUtil.xml) ||
                extension.equals("zip") || extension.equals("gz");
            }

            if (description.equals(GUIUtil.shpDesc)) {
                return extension.equals(GUIUtil.shp) ||
                extension.equals("zip");
            }

            if (description.equals(GUIUtil.shxDesc)) {
                return extension.equals(GUIUtil.shx);
            }

            if (description.equals(GUIUtil.dbfDesc)) {
                return extension.equals(GUIUtil.dbf);
            }

            if (description.equals(GUIUtil.gmlDesc)) {
                return extension.equals(GUIUtil.gml) ||
                extension.equals(GUIUtil.fme) || extension.equals("zip") ||
                extension.equals("gz");
            }

            if (description.equals(GUIUtil.wktDesc)) {
                return extension.equals(GUIUtil.wkt) ||
                extension.equals("zip") || extension.equals("gz");
            }

            if (description.equals(GUIUtil.fmeDesc)) {
                return extension.equals(GUIUtil.xml) ||
                extension.equals(GUIUtil.fme) || extension.equals("zip") ||
                extension.equals("gz");
            }
        }

        return false;
    }

    public String getDescription() {
        return description;
    }
}
