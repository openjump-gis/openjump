/*
 * Created on 15.09.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.latlon.deejump.plugin.style;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Point2D;

import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;
/**
 * @author hamammi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BitmapVertexStyle extends VertexStyle {
	
	private Image image;
	private Point2D point;
	private String imageURL;
	
	
	public BitmapVertexStyle(){}
	/**
	 * 
	 * @param image cannot be null
	 */
	public BitmapVertexStyle(String imageURL){
		// init with a this.shape = poly with 1 point (?)
		super(null);
		if ( imageURL == null){
			throw new NullPointerException("Image URL cannot be null.");
		}
		setImageURL( imageURL );
	}
	
	public void paint(Graphics2D g, Point2D p) {
		// don't use this, use this.shape
		
//		this.polygon.xpoints = new int [] { (int) p.getX()};
//		this.polygon.ypoints = new int [] { (int) p.getY() };
//		this.polygon.npoints=1;
		this.point = p;
		render(g);
    }
	
	protected void render(Graphics2D g) {
	    g.drawImage(image, (int)point.getX() - ((image.getWidth(null))/2),
	    		(int)point.getY() - ( (image.getHeight(null))/2), null);
   	
   }
    public Image getImage() {
        return image;
    }
    
    public String getImageURL() {
        return imageURL;
    }
    public void setImageURL( String imageURL ) {
        
        this.imageURL = imageURL;
        this.image = Toolkit.getDefaultToolkit().getImage(imageURL);
    }
}