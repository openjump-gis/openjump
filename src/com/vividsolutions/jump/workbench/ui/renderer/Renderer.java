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

	void clearImageCache();

	boolean isRendering();

	/**
   *@return contentID which identifies this Renderer by what it draws
   */
	Object getContentID();

	void copyTo(Graphics2D graphics);

	/**
   * @return null if no rendering work needs to be done
   */
	Runnable createRunnable();

	void cancel();
    
  interface Factory {
    Renderer create();
  }
    
  //[sstein: 20.01.2006] from Ole for RenderingManager changes
  // for not hardwired renderers and to including pirol image layers
  /**
   * @deprecated Replaced by {@link RendererFactory}
   */
  interface ContentDependendFactory {
    Renderer create(Object contentID);
  }
}