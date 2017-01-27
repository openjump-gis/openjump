
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

import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.GMLWriter;
import com.vividsolutions.jump.io.JMLReader;
import com.vividsolutions.jump.io.ParseException;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;


public class JMLFileOutputDriver extends AbstractOutputDriver {
    private GMLWriter gmlWriter = new GMLWriter();

    public JMLFileOutputDriver() {
    }

    public void output(Layer layer)
        throws FileNotFoundException, IOException, ParseException, 
            com.vividsolutions.jts.io.ParseException, Exception {
        File selectedFile = driverManager.getSharedSaveBasicFileDriverPanel()
                                         .getSelectedFile();
        String fname = selectedFile.getAbsolutePath();

        DriverProperties dp = new DriverProperties();
        dp.set("File", fname);
        gmlWriter.write(layer.getFeatureCollectionWrapper(), dp);
    }

    public String toString() {
        return GUIUtil.jmlDesc;
    }
}
