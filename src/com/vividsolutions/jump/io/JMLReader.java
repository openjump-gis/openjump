
/*
 * JMLReader.java
 *
 * Created on July 12, 2002, 3:00 PM
 */
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

package com.vividsolutions.jump.io;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.DataSource;


/**
 * JMLReader is a {@link JUMPReader} specialized to read JML.
 *
 * <p>
 * This is a simple class that passes the work off to the {@link GMLReader } class which
 * already has support for auto-generating a JML input template 
 * (see  {@link GMLInputTemplate}).
 * </p>
 *
 * <p>
 *   DataProperties for the JMLReader load(DataProperties) interface:<br><br>
 * </p>
 *
 *  <table border='1' cellspacing='0' cellpadding='4'>
 *  <tr>
 *    <th>Parameter</th>
 *    <th>Meaning</th>
 *  </tr>
 *  <tr>
 *    <td>File or DefaultValue</td>
 *    <td>File name for the input JML file</td>
 *  </tr>
 *  <tr>
 *    <td>CompressedFile</td>
 *    <td>
 *      File name (a .zip or .gz) with a .jml/.xml/.gml inside (specified by
 *      File)
 *    </td>
 *  </tr>
 *  <tr>
 *    <td>CompressedFileTemplate</td>
 *    <td>
 *      File name (.zip or .gz) with the input template in (specified by
 *      InputTemplateFile)
 *    </td>
 *  </tr>
 * </table>
 * <br>
 * <br>
 */
public class JMLReader extends AbstractJUMPReader {

    /** Creates new JMLReader */
    public JMLReader() {
    }

    /**
     * Read a JML file - passes the work off to {@link GMLReader}.
     *
     *@param dp 'InputFile' or 'DefaultValue' for the input JML file
     */
    public FeatureCollection read(DriverProperties dp)
            throws Exception {
        GMLReader gmlReader;
        String inputFname;

        inputFname = dp.getProperty(DataSource.FILE_KEY);

        if (inputFname == null) {
            inputFname = dp.getProperty(DriverProperties.DEFAULT_VALUE_KEY);
        }

        if (inputFname == null) {
            throw new IllegalParametersException(
                "call to JMLReader.read() has DataProperties w/o a InputFile specified");
        }

        gmlReader = new GMLReader();

        return gmlReader.read(dp);
    }
}
