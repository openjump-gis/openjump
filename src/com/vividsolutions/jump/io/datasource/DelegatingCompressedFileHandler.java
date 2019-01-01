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

import java.util.ArrayList;
import java.util.Collection;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.AbstractJUMPReader;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.JUMPReader;

/**
 * If the file is a .zip or .gz file, mangles the DriverProperties into the format
 * expected by many of the first JUMPReaders, which take responsibility
 * for doing the decompression. Really, JUMPReaders should not
 * have to be responsible for decompression -- they should be wrapped
 * by a CompressedFileHandler (not yet written) which would decompress the 
 * data before handing it to the JUMPReader. Anyway, developers should now be
 * writing DataSources instead of JUMPReaders.
 * 
 * @Deprecated use CompressedFile instead [07.2016]
 */
@Deprecated
public class DelegatingCompressedFileHandler extends AbstractJUMPReader {

    private Collection<String> endings;

    private JUMPReader reader;

    /**
     * Constructs a DelegatingCompressedFileHandler that wraps a JUMPReader. 
     * @param endings strings found at the ends of filenames, used to identify
     * the file to extract from a .zip; does not apply to .gz files
     * 
     * @Deprecated use CompressedFile instead [07.2016]
     */
    @Deprecated
    public DelegatingCompressedFileHandler(JUMPReader reader, Collection<String> endings) {
        this.reader = reader;
        this.endings = new ArrayList<>(endings);
    }

    /**
	 * @param dp
	 *                  only the "File" property is necessary; if it is a .zip or
	 *                  .gz, the required "CompressedFile" property will be
	 *                  determined automatically
	 */
    @Deprecated
    public FeatureCollection read(DriverProperties dp) throws Exception {
      FeatureCollection fc = reader.read(dp);
      getExceptions().addAll(reader.getExceptions());
      return fc;
    }

    // [ede 05.2012]
    //  we keep this as stub as StandardReaderWriter seems to need the definition
    protected void mangle(DriverProperties dp, String fileProperty,
        String compressedFileProperty, Collection<String> myEndings) throws Exception {
    }

}
