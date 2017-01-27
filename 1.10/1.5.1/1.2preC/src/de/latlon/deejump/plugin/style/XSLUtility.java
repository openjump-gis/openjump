/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/exse/
 lat/lon Fitzke/Fretter/Poth GbR
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth
 lat/lon Fitzke/Fretter/Poth GbR
 Meckenheimer Allee 176
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Jens Fitzke
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: jens.fitzke@uni-bonn.de

 
 ---------------------------------------------------------------------------*/
package de.latlon.deejump.plugin.style;

import java.io.File;
import java.net.MalformedURLException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei </a>
 *  
 */
public class XSLUtility {

    static {

        //FIXME there is a problem when this class is added as an extension
        // jump cannot find it (not in class path, because it's dynamically loaded)
        //       System.out.println( "never called" );
        //       System.out.println( System.getProperty( "java.class.path" ) );
    }

    public static String toHexColor( Node colorNode ) {
        String value = "#000000";
        if(colorNode == null) return value;
        
        try {//FIXME no good to grab 1st child and then node val
            if(colorNode.getFirstChild() == null) return value;
            String nodeVal = colorNode.getFirstChild().getNodeValue();
            String[] components = nodeVal.split( ", " );
            StringBuffer sb = new StringBuffer( 100 );
            sb.append( "#" );
            for (int i = 0; i < components.length - 1; i++) {

                String uglyHack = Integer.toHexString( Integer.parseInt( components[i] ) );
                uglyHack = uglyHack.length() == 1 ? "0"
                    + uglyHack : uglyHack;
                sb.append( uglyHack );

            }

            value = sb.toString();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    public static String toAlphaValue( Node colorNode ) {
        String value = "1";

        try {//FIXME no good to grab 1st child than node val
            String nodeVal = colorNode.getFirstChild().getNodeValue();
            String[] components = nodeVal.split( ", " );

            value = String.valueOf( Double.parseDouble( components[3] ) / 255d );
        } catch (Exception e) {
            e.printStackTrace();
        }
        // don't care about number formating, just trim the string
        if ( value.length() > 6 ) {
            value = value.substring( 0, 5 );
        }
        return value;
    }

    public static String toFontFamily( Node colorNode ) {
        String value = "Dialog";

        try {//FIXME no good to grab 1st child than node val
            String nodeVal = colorNode.getFirstChild().getNodeValue();
            String[] components = nodeVal.split( ", " );
            value = components[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static String toFontStyle( Node fontNode ) {
        // bold not supported in SLD?
        final String[] styles = { "normal", "normal", "italic" };

        String value = styles[0];
        try {//FIXME no good to grab 1st child than node val
            String nodeVal = fontNode.getFirstChild().getNodeValue();
            String[] components = nodeVal.split( ", " );

            //cheap, cheap, cheap
            value = styles[Integer.parseInt( components[1] )];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static boolean _toWellKnowName( Node vertexStyleNode ) {
        String value = "square";
        try {//FIXME no good to grab 1st child than node val
            String nodeVal = vertexStyleNode.getFirstChild().getNodeValue();
            String[] components = nodeVal.split( ", " );

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    
    public static String toWellKnowName( Node vertexStyleNode ) {
        if( vertexStyleNode == null ){
            return "";
        }
        
        String value = "square";
        try {
            
            NamedNodeMap atts = vertexStyleNode.getAttributes();
            String nodeVal = atts.getNamedItem( "class" ).getNodeValue();
            
            if ( nodeVal.indexOf( "Square" ) > -1  ){
                //already there
            } else if ( nodeVal.indexOf( "Circle" ) > -1 ){
                value = "circle";
            } else if ( nodeVal.indexOf( "Cross" ) > -1 ){
                value = "cross";
            } else if ( nodeVal.indexOf( "Star" ) > -1 ){
                value = "star";
            } else if ( nodeVal.indexOf( "Triangle" ) > -1 ){
                value = "triangle";
            } 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static String fileToURL( String filename ) {
        File f = new File( filename );
        f.deleteOnExit();
        
        try {
            return f.toURL().toString();
        } catch (MalformedURLException e) {
            return filename;
        }
    }
}