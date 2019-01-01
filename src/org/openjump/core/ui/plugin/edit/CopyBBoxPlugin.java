//$HeadURL: https://sushibar/svn/deegree/base/trunk/resources/eclipse/svn_classfile_header_template.xml $
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

package org.openjump.core.ui.plugin.edit;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * <code>CopyBBoxPlugin</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 */
public class CopyBBoxPlugin extends AbstractPlugIn {

    @Override
    public String getName() {
      return I18N.get( "org.openjump.core.ui.plugin.edit.CopyBBoxPlugin.name" );
    }

    @Override
    public void initialize( PlugInContext context ) {
    }

    @Override
    public boolean execute( PlugInContext context ) {
        Envelope env = context.getWorkbenchContext().getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();

        //StringBuilder sb = new StringBuilder( 512 );
        //sb.append( "POLYGON((" );
        //sb.append( env.getMinX() ).append( " " ).append( env.getMinY() );
        //sb.append( "," );
        //sb.append( env.getMinX() ).append( " " ).append( env.getMaxY() );
        //sb.append( "," );
        //sb.append( env.getMaxX() ).append( " " ).append( env.getMaxY() );
        //sb.append( "," );
        //sb.append( env.getMaxX() ).append( " " ).append( env.getMinY() );
        //sb.append( "," );
        //sb.append( env.getMinX() ).append( " " ).append( env.getMinY() );
        //sb.append( "))" );

        StringBuilder sbcleartext = new StringBuilder( 128 )
        .append( "bbox(" )
        .append( env.getMinX() )
        .append( "," )
        .append( env.getMinY() )
        .append( "," )
        .append( env.getMaxX() )
        .append( "," )
        .append( env.getMaxY() )
        .append( ")" );

        
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents( new StringSelection( sbcleartext.toString() ), null );
        //-- [sstein: 21mar2008 ] the following line seems to be buggy so I comment it
        //Toolkit.getDefaultToolkit().getSystemSelection().setContents( new StringSelection( sb.toString() ), null );

        return false;
    }

}
