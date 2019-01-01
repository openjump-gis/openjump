//$HeadURL: svn+ssh://edso@svn.code.sf.net/p/jump-pilot/code/plug-ins/WFSPlugin/trunk/src/de/latlon/deejump/wfs/jump/WFSFeature.java $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2007 by:
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
 Aennchenstr. 19
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

package de.latlon.deejump.wfs.jump;

import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * <code>WFSFeature</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1515 $, $Date: 2008-07-17 11:04:39 +0200 (Do, 17 Jul 2008) $
 */
public class WFSFeature extends BasicFeature {

    private static final long serialVersionUID = 3151195241527169438L;

    private String id;

    /**
     * @param featureSchema
     * @param gmlid
     *            the GML id (or something similar)
     */
    public WFSFeature( FeatureSchema featureSchema, String gmlid ) {
        super( featureSchema );
        id = gmlid;
    }

    /**
     * Copies the original feature.
     * 
     * @param original
     * @param gmlid
     */
    public WFSFeature( Feature original, String gmlid ) {
        super( original.getSchema() );
        id = gmlid;
        setAttributes( original.getAttributes() );
    }

    /**
     * @return the GML id
     */
    public String getGMLId() {
        return id;
    }

    /**
     * @param gmlid
     */
    public void setGMLId( String gmlid ) {
        id = gmlid;
    }

}
