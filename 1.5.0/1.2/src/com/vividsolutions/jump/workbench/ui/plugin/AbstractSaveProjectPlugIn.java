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

package com.vividsolutions.jump.workbench.ui.plugin;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

/**
* Subclass this to implement a 'Save Project' plugin.
*/
public abstract class AbstractSaveProjectPlugIn extends AbstractPlugIn {
    public AbstractSaveProjectPlugIn() {
    }

    protected void save(Task task, File file, WorkbenchFrame frame)
        throws Exception {
        //First use StringWriter to make sure no errors occur before we touch the
        //original file -- we don't want to damage the original if an error occurs.
        //[Jon Aquino]
        StringWriter stringWriter = new StringWriter();

        try {
            new Java2XML().write(task, "project", stringWriter);
        } finally {
            stringWriter.flush();
        }

        FileUtil.setContents(file.getAbsolutePath(), stringWriter.toString());
        task.setName(GUIUtil.nameWithoutExtension(file));
        task.setProjectFile(file);

        ArrayList ignoredLayers = new ArrayList(ignoredLayers(task));

        if (!ignoredLayers.isEmpty()) {
            String warning = I18N.get("ui.plugin.AbstractSaveProjectPlugIn.some-layers-were-not-saved-to-the-task-file")+" ";

            for (int i = 0; i < ignoredLayers.size(); i++) {
                Layer ignoredLayer = (Layer) ignoredLayers.get(i);

                if (i > 0) {
                    warning += "; ";
                }

                warning += ignoredLayer.getName();
            }
            
            warning += " ("+I18N.get("ui.plugin.AbstractSaveProjectPlugIn.data-source-is-write-only")+")";

            frame.warnUser(warning);
        }
    }

    private Collection ignoredLayers(Task task) {
        ArrayList ignoredLayers = new ArrayList();

        for (Iterator i = task.getLayerManager().getLayers().iterator();
                i.hasNext();) {
            Layer layer = (Layer) i.next();

            if (!layer.hasReadableDataSource()) {
                ignoredLayers.add(layer);
            }
        }

        return ignoredLayers;
    }
}
