/*
 * Created on 30.08.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.latlon.deejump.plugin.style;

import java.awt.Shape;
import java.awt.geom.Arc2D;

import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

/**
 * @author hamammi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CircleVertexStyle extends VertexStyle {

	public CircleVertexStyle() {
		super( createDefaultCircle() );
	}

	protected static Shape createDefaultCircle(){
		Arc2D s = new Arc2D.Double(100, 100, 10, 10, 0, 360, Arc2D.OPEN );
		return s;
	}
	
}
