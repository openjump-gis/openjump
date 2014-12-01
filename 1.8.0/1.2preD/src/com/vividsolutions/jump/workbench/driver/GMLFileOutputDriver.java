
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

package com.vividsolutions.jump.workbench.driver;

import java.io.File;

import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.GMLWriter;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.GMLFileDriverPanel;


public class GMLFileOutputDriver extends AbstractOutputDriver {
    private GMLFileDriverPanel panel;
    private GMLWriter writer = new GMLWriter();

    public GMLFileOutputDriver() {
    }

    public void output(Layer layer) throws Exception {
        File selectedFile = panel.getGMLFile();
        String fname = selectedFile.getAbsolutePath();

        DriverProperties dp = new DriverProperties();
        dp.set("File", fname);
        dp.set("TemplateFile", panel.getTemplateFile().getAbsolutePath());
        writer.write(layer.getFeatureCollectionWrapper(), dp);

    }

    public String toString() {
        return "GML 2.0";
    }

    public AbstractDriverPanel getPanel() {
        return panel;
    }

    public void initialize(DriverManager driverManager,
        ErrorHandler errorHandler) {
        super.initialize(driverManager, errorHandler);
        panel = new GMLFileDriverPanel(errorHandler);
        panel.setGMLFileMustExist(false);
        panel.setTemplateFileDescription("JCS GML Output Template File");
        panel.addPossibleTemplateExtension(".jot");
        panel.addPossibleTemplateExtension("_output.xml");
    }
    

}
