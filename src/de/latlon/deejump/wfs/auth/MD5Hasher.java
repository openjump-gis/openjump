//$Header: $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2006 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstraße 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de

 ---------------------------------------------------------------------------*/

package de.latlon.deejump.wfs.auth;

import java.security.MessageDigest;

/**
 * @author olaf
 * 
 * Utility-Methode zum Erzeugen von MD5-Hashes,
 * basierend auf Code von Nico Seessle (nico@seessle.de),
 * den dieser in de.comp.lang.java veroeffentlicht hat
 */

public class MD5Hasher {
    
    private static final char HEX_CHARS[] = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * @param s
     * @return a classic md5 string
     */
    public static String getMD5(String s) {
        String s2 = s;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            s2 = getHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException nsae) {
            // MD5 must exist
        }
        return s2;
    }

    /**
     * @param bytes 
     * @return a String containing the hex-values for the byte-array
     */
    public static String getHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < bytes.length; i++) {
            int n = bytes[i];
            if (n < 0)
                n += 255;
            int i1 = n / 16;
            int i2 = n % 16;

            sb.append(HEX_CHARS[i1]);
            sb.append(HEX_CHARS[i2]);

        }

        return sb.toString();
    }
}
