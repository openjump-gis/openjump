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

import java.awt.Color;

import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * 
 */
public class PointSymbolizerBasicStyle extends BasicStyle {

    /**
     * 
     */
    public PointSymbolizerBasicStyle() {
        super();
        // TODO Auto-generated constructor stub
    }

    private VertexStyle vertexsStyle;
    
    public PointSymbolizerBasicStyle( BasicStyle bs, VertexStyle vertexStyle){
        super( bs.getFillColor() );
        setFillPattern( bs.getFillPattern() );
        setLineColor( bs.getLineColor() );
        setAlpha( bs.getAlpha() );
        setRenderingFill( bs.isRenderingFill() );
        setRenderingFillPattern( bs.isRenderingFillPattern() );
        setRenderingLine( bs.isRenderingLine() );
        setLineWidth( bs.getLineWidth() );
        setRenderingLinePattern( bs.isRenderingLinePattern() );
        setLinePattern( bs.getLinePattern() );
        this.vertexsStyle = vertexStyle;
        this.setEnabled( bs.isEnabled() );        
    }
    
    public VertexStyle getVertexsStyle() {
        return vertexsStyle;
    }
    public void setEnabled( boolean enable ){
        this.vertexsStyle.setEnabled( enable );
        super.setEnabled( enable );
    }
    public boolean isEnabled(){
        return super.isEnabled();
    }
    public boolean isRenderingFill(){
        return super.isRenderingFill();
    }
    public Color getFillColor(){
        return super.getFillColor();
    }
    public boolean isRenderingLine(){
        return super.isRenderingLine();
    }
    public int getLineWidth(){
        return super.getLineWidth();
    }
    public Color getLineColor(){
        return super.getLineColor();
    }
    public int getAlpha(){
        return super.getAlpha();
    }
    public VertexStyle getVertexStyle(){
        return this.vertexsStyle;
    }
    public VertexStyle setVertexStyle( VertexStyle vertexsStyle ){
        return this.vertexsStyle = vertexsStyle;
    }
}
