/*
 * CompressedFile.java
 *
 * Created on December 12, 2002, 9:51 AM
 */
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive
 * GUI for visualizing and manipulating spatial features with geometry
 * and attributes.
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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Utility class for dealing with compressed (.zip, .gz) files.
 *
 * @author  dblasby
 * @TODO :I18N
 */
public class CompressedFile {

    /** Creates a new CompressedFile */
    public CompressedFile() {
    }

    /**
     *   Searches through the .zip file looking for a file with the
     *   given extension.  Returns null if it doesn't find one.
     */
    public static String getInternalZipFnameByExtension(String extension,
        String compressedFile) throws Exception {
        //zip file
        String inside_zip_extension;
        InputStream IS_low = new FileInputStream(compressedFile);
        ZipInputStream fr_high = new ZipInputStream(IS_low);

        //need to find the correct file within the .zip file
        ZipEntry entry;

        entry = fr_high.getNextEntry();

        while (entry != null) {
            inside_zip_extension = 
		entry.getName().substring(entry.getName().length() -
					  extension.length());

            if (inside_zip_extension.compareToIgnoreCase(extension) == 0) {
                return (entry.getName());
            }

            entry = fr_high.getNextEntry();
        }

        return null;
    }

    /**
     *   Utility file open function - handles compressed and
     *   un-compressed files.
     *
     *  @param compressedFile name of file to open.
     *  @param extension look for file with this extension in zipfile.
     *
     *  <p>
     *  compressedFile = null, then opens a FileInputStream on
     *  COMPRESSEDFILE (!)  should be file.shp or such
     *  </p>
     *
     *  <p> 
     *  compressedFile ends in ".zip" - opens the compressed zipfile
     *  and lookes for first file with extension 'extension'
     *  </p> 
     *
     *  <p> 
     *  compressedFile ends in ".gz"  - opens the compressed .gz file.
     *  </p> 
     */
    static InputStream openFileExtension(String extension, String compressedFile)
        throws Exception {
        String compressed_extension;
        String inside_zip_extension;

        if ((compressedFile == null) || (compressedFile.length() == 0)) {
            throw new Exception("openFileExtension- no compressed file given.");
        }

        compressed_extension = compressedFile.substring(compressedFile.length() -
                3);

        if (compressed_extension.compareToIgnoreCase(".gz") == 0) {
            // gz file -- easy

	    // low-level reader
            InputStream IS_low = new FileInputStream(compressedFile); 

	    // high-level reader
            return (new java.util.zip.GZIPInputStream(IS_low)); 
        }

        if (compressed_extension.compareToIgnoreCase("zip") == 0) {
            //zip file
            InputStream IS_low = new FileInputStream(compressedFile);
            ZipInputStream fr_high = new ZipInputStream(IS_low);

            //need to find the correct file within the .zip file
            ZipEntry entry;

            entry = fr_high.getNextEntry();

            while (entry != null) {
                inside_zip_extension = 
		    entry.getName().substring(entry.getName().length() -
					      extension.length());

                if (inside_zip_extension.compareToIgnoreCase(extension) == 0) {
                    return (fr_high);
                }

                entry = fr_high.getNextEntry();
            }

            throw new Exception("couldnt find file with extension" + 
				extension +
				" in compressed file " + 
				compressedFile);
        }

        throw new Exception("couldnt determine compressed file type for file " +
            compressedFile + "- should end in .zip or .gz");
    }

    /**
     * Utility file open function - handles compressed and un-compressed files.
     *
     * @param fname name of the file to search for.
     * @param compressedFile name of the compressed file.
     *
     *  <p>
     *  If compressedFile = null,  opens a FileInputStream on fname
     *  </p>
     *
     *  <p>
     *  compressedFile ends in ".zip" - opens the compressed Zip and
     *  lookes for the file called fname
     *  </p>     
     *
     *  <p>
     *  compressedFile ends in ".gz"  - opens the compressed .gz file.
     *  </p>
     */
    static InputStream openFile(String fname, String compressedFile)
        throws Exception {
        String extension;

        if ((compressedFile == null) || (compressedFile.length() == 0)) {
            return new FileInputStream(fname);
        }

        extension = compressedFile.substring(compressedFile.length() - 3);

        if (extension.compareToIgnoreCase(".gz") == 0) {
            // gz file -- easy

	    // low-level reader
            InputStream IS_low = new FileInputStream(compressedFile); 

	    // high-level reader
            return (new java.util.zip.GZIPInputStream(IS_low)); 
        }

        if (extension.compareToIgnoreCase("zip") == 0) {
            //zip file
            InputStream IS_low = new FileInputStream(compressedFile);
            ZipInputStream fr_high = new ZipInputStream(IS_low);

            //need to find the correct file within the .zip file
            ZipEntry entry;

            entry = fr_high.getNextEntry();

            while (entry != null) {
                if (entry.getName().compareToIgnoreCase(fname) == 0) {
                    return (fr_high);
                }

                entry = fr_high.getNextEntry();
            }

            throw new Exception("couldnt find " + fname +
                " in compressed file " + compressedFile);
        }

        throw new Exception("Couldn't determine compressed file type for file " +
			    compressedFile + 
			    " - Should end in .zip or .gz");
    }
}
