package com.vividsolutions.jump.workbench.ui.renderer;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

/**
 * <p>
 * The RendererFactory interface is used to create an instance of the renderer
 * for the class of the content to be rendered.
 * </p>
 * <p>
 * Renderers for a class can be registered using the
 * {@link RenderingManager#setRendererFactory(Class, RendererFactory)} method.
 * </p>
 * 
 * @author Paul Austin
 * @param <T> The type of object to create the renderer for
 */
public interface RendererFactory<T extends Object> {
  /**
   * Create a renderer for the content.
   * 
   * @param content The content to render.
   * @param panel The panel to render the content to.
   * @param maxfeatures The maximum number of features to render.
   * @return The renderer.
   */
  public Renderer create(T content, LayerViewPanel panel, int maxFeatures);
}
