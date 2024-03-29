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

import com.vividsolutions.jump.workbench.ui.renderer.style.SquareVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * 
 */
public class VertexStylesFactory {
    
	public static final String SQUARE_STYLE = "SQUARE"; 
	
	public static final String CIRCLE_STYLE = "CIRCLE"; 
	
	public static final String TRIANGLE_STYLE = "TRIANGLE";
	
	public static final String STAR_STYLE = "STAR";
	
	public static final String CROSS_STYLE = "CROSS";
	
	public static final String BITMAP_STYLE = "BITMAP";
	
	private VertexStylesFactory(){
	    //prevents init
	}

	public static VertexStyle createVertexStyle(String wellKnowName ){
	    
	    VertexStyle vStyle = null;
	    
	    if ( SQUARE_STYLE.equals( wellKnowName ) ){
	        vStyle = new SquareVertexStyle();
	    } else if( CIRCLE_STYLE.equals( wellKnowName ) ){
	        vStyle = new CircleVertexStyle();   
	    } else if( TRIANGLE_STYLE.equals( wellKnowName ) ){
	        vStyle = new TriangleVertexStyle();   
	    } else if( STAR_STYLE.equals( wellKnowName ) ){
	        vStyle = new StarVertexStyle();   
	    } else if( CROSS_STYLE.equals( wellKnowName ) ){
	        vStyle = new CrossVertexStyle();   
	    } else {
	        vStyle = new BitmapVertexStyle( wellKnowName );   
	    } 
	    //FIXME if none of the above? or wrong URL? 
	    
	    
	    return vStyle;
	}
}
