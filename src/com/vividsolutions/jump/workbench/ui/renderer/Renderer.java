package com.vividsolutions.jump.workbench.ui.renderer;

import java.awt.Graphics2D;
import java.awt.geom.NoninvertibleTransformException;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

/**
 * First call #createRunnable. If it returns null, get the image using #copyTo.
 * Otherwise, run the Runnable in a separate thread. You can call #copyTo while
 * it's drawing to get the partially drawn image. Drawing is done when
 * #isRendering returns false.
 */
public interface Renderer {
	public abstract void clearImageCache();
	public abstract boolean isRendering();
    /**
     *@param  contentID  identifies this Renderer by what it draws
     */    
	public abstract Object getContentID();
	public abstract void copyTo(Graphics2D graphics);
    /**
     * @return null if no rendering work needs to be done
     */
	public abstract Runnable createRunnable();
	public abstract void cancel();
    
    public static interface Factory {
        public Renderer create();
    }    
    
    //[sstein: 20.01.2006] from Ole for RenderingManager changes
    // for not hardwired renderers and to including pirol image layers
    /**
     * @deprecated Replaced by {@link RendererFactory}
     */
    public static interface ContentDependendFactory {
        public Renderer create(Object contentID);
    } 
}