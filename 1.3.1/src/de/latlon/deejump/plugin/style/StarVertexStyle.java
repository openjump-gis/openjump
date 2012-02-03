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

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Point2D;

import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * 
 */
public class StarVertexStyle extends VertexStyle{
	
	public StarVertexStyle(){
		super(createDefaultTriangle());
	}
	
	public void paint(Graphics2D g,Point2D p ){
	    int s = getSize();
        int x0 = (int)p.getX();
        int y0 = (int)p.getY();
        double sin36 = Math.sin( Math.toRadians(36) );
        double cos36 = Math.cos( Math.toRadians(36) );
        double sin18 = Math.sin( Math.toRadians(18) );
        double cos18 = Math.cos( Math.toRadians(18) );
        int smallRadius = (int)( s * sin18/Math.sin( Math.toRadians(54)));
        
        int p0X = x0;
        int p0Y = y0 - s;
        int p1X = x0 + (int)(smallRadius * sin36);
        int p1Y = y0 - (int)(smallRadius * cos36);
        int p2X = x0 + (int)(s * cos18);
        int p2Y = y0 - (int)(s * sin18);
        int p3X = x0 + (int)(smallRadius * cos18);
        int p3Y = y0 + (int)(smallRadius * sin18);
        int p4X = x0 + (int)(s * sin36);
        int p4Y = y0 + (int)(s * cos36);
        int p5Y = y0 + smallRadius;
        int p6X = x0 - (int)(s * sin36);
        int p7X = x0 - (int)(smallRadius * cos18);
        int p8X = x0 - (int)(s * cos18);
        int p9X = x0 - (int)(smallRadius * sin36);

        ((Polygon)this.shape).xpoints =
            new int[]{ p0X, p1X, p2X, p3X, p4X, p0X, p6X, p7X, p8X, p9X};
        
        ((Polygon)this.shape).ypoints = 
            new int[]{ p0Y, p1Y, p2Y, p3Y, p4Y, p5Y, p4Y, p3Y, p2Y, p1Y};
        
        ((Polygon)this.shape).npoints = ((Polygon)this.shape).xpoints.length;
         
        render(g);
	}
	
	private static Shape createDefaultTriangle(){
		return new Polygon();
	}

}