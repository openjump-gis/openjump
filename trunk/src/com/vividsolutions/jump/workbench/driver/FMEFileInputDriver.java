
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
import java.io.FileNotFoundException;
import java.io.IOException;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.FMEGMLReader;
import com.vividsolutions.jump.io.ParseException;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;
import com.vividsolutions.jump.workbench.ui.BasicFileDriverPanel;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFileFilter;


public class FMEFileInputDriver extends AbstractInputDriver {
    private BasicFileDriverPanel panel;
    private DriverProperties dp = new DriverProperties();

    public FMEFileInputDriver() {
    }

    public String toString() {
        return GUIUtil.fmeDesc;
    }

    public void initialize(DriverManager driverManager,
        ErrorHandler errorHandler) {
        super.initialize(driverManager, errorHandler);
        panel = new BasicFileDriverPanel(errorHandler);
        panel.setFileMustExist(true);
        panel.setFileDescription(GUIUtil.fmeDesc);
        panel.setFileFilter(new WorkbenchFileFilter(GUIUtil.fmeDesc));
    }

    public void input(LayerManager layerManager, String categoryName)
        throws FileNotFoundException, IOException, ParseException, 
            com.vividsolutions.jts.io.ParseException, 
            com.vividsolutions.jump.io.IllegalParametersException, Exception {
        String extension;
        File selectedFile = panel.getSelectedFile();
        FeatureCollection featureCollection;
        FMEGMLReader fmeReader = new FMEGMLReader();
        String name = selectedFile.getAbsolutePath();

        //<<TODO:DEFECT>> Must create a new DataProperties object. If I try to reuse
        //the old one, FMEGMLReader seems to use the old filename. Need to investigate
        //this. [Jon Aquino]
        extension = name.substring(name.length() - 3);
        dp = new DriverProperties();

        if (extension.equalsIgnoreCase("zip")) {
            String internalName;

            dp.set("CompressedFile", name);
            internalName = CompressedFile.getInternalZipFnameByExtension(".fme",
                    name);

            if (internalName == null) {
                internalName = CompressedFile.getInternalZipFnameByExtension(".xml",
                        name);
            }

            if (internalName == null) {
                internalName = CompressedFile.getInternalZipFnameByExtension(".gml",
                        name);
            }

            if (internalName == null) {
                throw new Exception(
                    "Couldnt find a .fme, .xml, or .gml file inside the .zip file: " +
                    name);
            }

            dp.set("File", internalName);
        } else if (extension.equalsIgnoreCase(".gz")) {
            dp.set("CompressedFile", name);
            dp.set("File", name); // not useed
        } else {
            dp.set("File", name);
        }

        // no "TemplateFile" specified, so read it from the top of the "File"
        featureCollection = fmeReader.read(dp);

        Layer layer = layerManager.addLayer(categoryName,
                GUIUtil.nameWithoutExtension(selectedFile), featureCollection);
    }

    public AbstractDriverPanel getPanel() {
        return panel;
    }
}
