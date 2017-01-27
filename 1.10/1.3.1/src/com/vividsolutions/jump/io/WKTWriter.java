
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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;


/**
 * WKTWriter is a {@link JUMPWriter} specialized to write WTK (Well Known Text) files.
 *
 * <p>
 *   DataProperties for the JUMPWriter
 *   write(featureSchema,DataProperties) interface:<br>
 * </p>
 *
 * <p>
 * <table border='1' cellspacing='0' cellpadding='4'>
 *   <tr>
 *     <th>Parameter</th>
 *     <th>Meaning</th>
 *   </tr>
 *   <tr>
 *     <td>OutputFile or DefaultValue</td>
 *     <td>File name for output .wkt file</td>
 *   </tr>
 * </table>
 * <br>
 * </p>
 */
public class WKTWriter implements JUMPWriter {
    private FUTURE_JTS_WKTWriter wktWriter = new FUTURE_JTS_WKTWriter();

    /**constuctor*/
    public WKTWriter() {
    }

    /**
     * Main method - writes a list of wkt features (no attributes).
     *
     * @param featureCollection features to write
     * @param dp 'OutputFile' or 'DefaultValue' to specify the output file.
     */
    public void write(FeatureCollection featureCollection, DriverProperties dp)
        throws IllegalParametersException, Exception {
        String outputFname;

        outputFname = dp.getProperty("File");

        if (outputFname == null) {
            outputFname = dp.getProperty("DefaultValue");
        }

        if (outputFname == null) {
            throw new IllegalParametersException(
                "call to WKTWrite.write() has DataProperties w/o a OutputFile specified");
        }

        java.io.Writer w;

        w = new java.io.FileWriter(outputFname);
        this.write(featureCollection, w);
        w.close();
    }

    /**
     * Function that actually does the writing
     *@param featureCollection features to write
     *@param writer where to write
     */
    public void write(FeatureCollection featureCollection, Writer writer)
        throws IOException {
        for (Iterator i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            wktWriter.writeFormatted(feature.getGeometry(), writer);

            if (i.hasNext()) {
                writer.write("\n\n");
            }
        }
    }
}
