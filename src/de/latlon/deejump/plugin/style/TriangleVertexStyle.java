/*
 * Created on 30.08.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.latlon.deejump.plugin.style;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Point2D;

import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

/**
 * @author hamammi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TriangleVertexStyle extends VertexStyle {
	
	public TriangleVertexStyle(){
		super( createDefaultTriangle() );
	}
	
	public void paint(Graphics2D g, Point2D p) {
		//setFrame
		//render
		int angle = 45;
		int halfSide = (int)(Math.sin(angle) * getSize());
		int p1X = (int)p.getX();
		int p1Y = (int)p.getY() - getSize();
		int p2X = p1X - halfSide;
		int p2Y = (int)p.getY() + halfSide;
		int p3X = (p1X) + halfSide;
		int p3Y = (int)p.getY() + halfSide;
		((Polygon)this.shape).xpoints = new int[]{ p1X, p2X ,p3X }; 
		((Polygon)this.shape).ypoints = new int[]{ p1Y, p2Y,p3Y };
		((Polygon)this.shape).npoints = ((Polygon)this.shape).xpoints.length;
		render(g);
	}
	
    
	protected static Shape createDefaultTriangle(){		
		return new Polygon();
	}
}