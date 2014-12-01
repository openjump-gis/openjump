
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

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.GMLReader;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.GMLFileDriverPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFileFilter;


public class GMLFileInputDriver extends AbstractInputDriver {
    private GMLFileDriverPanel panel;
    private GMLReader reader = new GMLReader();

    public GMLFileInputDriver() {
    }

    public void input(LayerManager layerManager, String categoryName)
        throws Exception {
        String extension_gml;
        String extension_template;
        File selectedFile = panel.getGMLFile();
        String layerName = GUIUtil.nameWithoutExtension(selectedFile);
        String fname = selectedFile.getAbsolutePath();

        DriverProperties dp = new DriverProperties();

        extension_gml = fname.substring(fname.length() - 3);
        extension_template = panel.getTemplateFile().getAbsolutePath()
                                  .substring(panel.getTemplateFile()
                                                  .getAbsolutePath().length() -
                3);

        if (extension_gml.equalsIgnoreCase("zip")) {
            String internalName;

            dp.set("CompressedFile", fname);

            internalName = CompressedFile.getInternalZipFnameByExtension(".gml",
                    fname);

            if (internalName == null) {
                internalName = CompressedFile.getInternalZipFnameByExtension(".xml",
                        fname);
            }

            if (internalName == null) {
                throw new Exception(
                    "Couldnt find a .xml or .gml file inside the .zip file: " +
                    fname);
            }

            dp.set("File", internalName);
        } else if (extension_gml.equalsIgnoreCase(".gz")) {
            dp.set("CompressedFile", fname);
            dp.set("File", fname); // not useed
        } else {
            dp.set("File", fname);
        }

        if (extension_template.equalsIgnoreCase("zip")) {
            String internalName;

            dp.set("CompressedFileTemplate",
                panel.getTemplateFile().getAbsolutePath());
            internalName = CompressedFile.getInternalZipFnameByExtension("_input.xml",
                    panel.getTemplateFile().getAbsolutePath());

            if (internalName == null) {
                internalName = CompressedFile.getInternalZipFnameByExtension(".input",
                        panel.getTemplateFile().getAbsolutePath());
            }

            if (internalName == null) {
                internalName = CompressedFile.getInternalZipFnameByExtension(".template",
                        panel.getTemplateFile().getAbsolutePath());
            }

            if (internalName == null) {
                throw new Exception(
                    "Couldnt find a _input.xml, .input, or .template file inside the .zip file: " +
                    panel.getTemplateFile().getAbsolutePath());
            }

            dp.set("TemplateFile", internalName);
        } else if (extension_template.equalsIgnoreCase(".gz")) {
            dp.set("CompressedFileTemplate",
                panel.getTemplateFile().getAbsolutePath());
            dp.set("TemplateFile", panel.getTemplateFile().getAbsolutePath()); // not useed
        } else {
            dp.set("TemplateFile", panel.getTemplateFile().getAbsolutePath());
        }

        FeatureCollection featureCollection = reader.read(dp);
        Layer layer = layerManager.addLayer(categoryName, layerName,
                featureCollection);
    }

    public String toString() {
        return "GML 2.0";
    }

    public void initialize(DriverManager driverManager,
        ErrorHandler errorHandler) {
        super.initialize(driverManager, errorHandler);
        panel = new GMLFileDriverPanel(errorHandler);
        panel.setGMLFileMustExist(true);
        panel.setTemplateFileDescription("JCS GML Input Template File");
        panel.addPossibleTemplateExtension(".jit");
        panel.addPossibleTemplateExtension("_input.xml");
        panel.addPossibleTemplateExtension(".gz");
        panel.addPossibleTemplateExtension(".zip");
    }

    public AbstractDriverPanel getPanel() {
        return panel;
    }
}
