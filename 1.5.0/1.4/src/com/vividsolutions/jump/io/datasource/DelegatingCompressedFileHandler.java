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
package com.vividsolutions.jump.io.datasource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.JUMPReader;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.StringUtil;

/**
 * If the file is a .zip or .gz file, mangles the DriverProperties into the format
 * expected by many of the first JUMPReaders, which take responsibility
 * for doing the decompression. Really, JUMPReaders should not
 * have to be responsible for decompression -- they should be wrapped
 * by a CompressedFileHandler (not yet written) which would decompress the 
 * data before handing it to the JUMPReader. Anyway, developers should now be
 * writing DataSources instead of JUMPReaders.
 */
public class DelegatingCompressedFileHandler implements JUMPReader {

    private Collection endings;

    private JUMPReader reader;

    /**
     * Constructs a DelegatingCompressedFileHandler that wraps a JUMPReader. 
     * @param endings strings found at the ends of filenames, used to identify
     * the file to extract from a .zip; does not apply to .gz files
     */
    public DelegatingCompressedFileHandler(JUMPReader reader, Collection endings) {
        this.reader = reader;
        this.endings = new ArrayList(endings);
    }

    /**
	 * @param dp
	 *                  only the "File" property is necessary; if it is a .zip or
	 *                  .gz, the required "CompressedFile" property will be
	 *                  determined automatically
	 */
    public FeatureCollection read(DriverProperties dp) throws Exception {
        mangle(dp, "File", "CompressedFile", endings);
        return reader.read(dp);
    }

    protected void mangle(
        DriverProperties dp,
        String fileProperty,
        String compressedFileProperty,
        Collection myEndings)
        throws Exception {
        if (FileUtil
            .getExtension(new File(dp.getProperty(fileProperty)))
            .equalsIgnoreCase("zip")) {
            String internalName = null;
            for (Iterator i = myEndings.iterator(); internalName == null && i.hasNext();) {
                String ending = (String) i.next();
                internalName =
                    CompressedFile.getInternalZipFnameByExtension(
                        ending,
                        dp.getProperty(fileProperty));
            }

            if (internalName == null) {
                throw new Exception(
                    "Couldn't find a "
                        + StringUtil.toCommaDelimitedString(myEndings)
                        + " file inside the .zip file: "
                        + dp.getProperty(fileProperty));
            }
            dp.set(compressedFileProperty, dp.getProperty(fileProperty));
            dp.set(fileProperty, internalName);
        } else if (
            FileUtil.getExtension(
                new File(dp.getProperty(fileProperty))).equalsIgnoreCase(
                "gz")) {
            dp.set(compressedFileProperty, dp.getProperty(fileProperty));
        }
    }

}
