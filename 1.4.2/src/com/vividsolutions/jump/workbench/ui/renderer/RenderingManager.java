/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.renderer;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import com.vividsolutions.jump.util.OrderedMap;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class RenderingManager {

  // [sstein: 20.01.2006] added for Ole
  /** @deprecated */
  protected static HashMap layerableClassToRendererFactoryMap = new HashMap();

  private static final Map<Class, RendererFactory> CLASS_RENDERER_FACTORY_MAP = new HashMap<Class, RendererFactory>();

  private LayerViewPanel panel;

  /**
   * this variable will be used for {@link LayerRenderer} which extends
   * {@link FeatureCollectionRenderer} default in FeatureCollectionRenderer is
   * 100 features.
   */
  private int maxFeatures = 100; // this variable will be used for

  // LayerRenderer.class which extends
  // FeatureCollectionRenderer.class
  // default in FeatureCollectionRenderer is 100 features.

  /**
   * @see ThreadQueue
   */
  public static final String USE_MULTI_RENDERING_THREAD_QUEUE_KEY = RenderingManager.class.getName()
    + " - USE MULTI RENDERING THREAD QUEUE";

  private Map contentIDToRendererMap = new OrderedMap();

  private OrderedMap contentIDToLowRendererFactoryMap = new OrderedMap();

  private OrderedMap contentIDToHighRendererFactoryMap = new OrderedMap();

  /**
   * There's no performance advantage to rendering dozens of non-WMS or
   * non-database layers in parallel. In fact, it will make the GUI less
   * responsive. [Jon Aquino]
   */
  private ThreadQueue defaultRendererThreadQueue = new ThreadQueue(1);

  /**
   * WMS and database processing are done on the server side, so allow these
   * queries to be done in parallel. But not too many, as each Thread consumes 1
   * MB of memory (see http://mindprod.com/jglossthread.html). The Threads may
   * pile up if the server is down. [Jon Aquino]
   */
  private ThreadQueue multiRendererThreadQueue = new ThreadQueue(20);

  // 250 ms wasn't as good as 1 s because less got painted on each repaint,
  // making rendering appear to be slower. [Jon Aquino]
  // LDB: changed from 400 to 800 ms after raster stretch mouse wheel zooming
  private Timer repaintTimer = new Timer(800, new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      for (Iterator i = contentIDToRendererMap.values().iterator(); i.hasNext();) {
        Renderer renderer = (Renderer)i.next();
        if (renderer.isRendering()) {
          repaintPanel();
          return;
        }
      }

      repaintTimer.stop();
      repaintPanel();
    }
  });

  private boolean paintingEnabled = true;

  public RenderingManager(final LayerViewPanel panel) {
    this.panel = panel;
    repaintTimer.setCoalesce(true);
    putAboveLayerables(SelectionBackgroundRenderer.CONTENT_ID,
      new Renderer.Factory() {
        public Renderer create() {
          return new SelectionBackgroundRenderer(panel);
        }
      });
    putAboveLayerables(FeatureSelectionRenderer.CONTENT_ID,
      new Renderer.Factory() {
        public Renderer create() {
          return new FeatureSelectionRenderer(panel);
        }
      });
    putAboveLayerables(LineStringSelectionRenderer.CONTENT_ID,
      new Renderer.Factory() {
        public Renderer create() {
          return new LineStringSelectionRenderer(panel);
        }
      });
    putAboveLayerables(PartSelectionRenderer.CONTENT_ID,
      new Renderer.Factory() {
        public Renderer create() {
          return new PartSelectionRenderer(panel);
        }
      });
  }

  public void putBelowLayerables(Object contentID, Renderer.Factory factory) {
    contentIDToLowRendererFactoryMap.put(contentID, factory);
  }

  public void putAboveLayerables(Object contentID, Renderer.Factory factory) {
    contentIDToHighRendererFactoryMap.put(contentID, factory);
  }

  public void renderAll() {
    defaultRendererThreadQueue.clear();
    multiRendererThreadQueue.clear();

    for (Iterator i = contentIDs().iterator(); i.hasNext();) {
      Object contentID = i.next();
      render(contentID);
    }
  }

  protected List contentIDs() {
    ArrayList contentIDs = new ArrayList();
    contentIDs.addAll(contentIDToLowRendererFactoryMap.keyList());
    for (Iterator i = panel.getLayerManager().reverseIterator(Layerable.class); i.hasNext();) {
      Layerable layerable = (Layerable)i.next();
      contentIDs.add(layerable);
    }

    contentIDs.addAll(contentIDToHighRendererFactoryMap.keyList());

    return contentIDs;
  }

  public Renderer getRenderer(Object contentID) {
    return (Renderer)contentIDToRendererMap.get(contentID);
  }

  private void setRenderer(Object contentID, Renderer renderer) {
    contentIDToRendererMap.put(contentID, renderer);
  }

  public void render(Object contentID) {
    render(contentID, true);
  }

  public void render(Object contentID, boolean clearImageCache) {

    if (getRenderer(contentID) == null) {
      setRenderer(contentID, createRenderer(contentID));
    }

    if (getRenderer(contentID).isRendering()) {
      getRenderer(contentID).cancel();

      // It might not cancel immediately, so create a new Renderer [Jon
      // Aquino]
      setRenderer(contentID, createRenderer(contentID));
    }

    if (clearImageCache) {
      getRenderer(contentID).clearImageCache();
    }
    Runnable runnable = getRenderer(contentID).createRunnable();
    if (runnable != null) {
      // Before I would create threads that did nothing. Now I never do
      // that -- I just return null. A dozen threads that do nothing make
      // the system sluggish. [Jon Aquino]
      ((contentID instanceof Layerable && ((Layerable)contentID).getBlackboard()
        .get(USE_MULTI_RENDERING_THREAD_QUEUE_KEY, false)) ? multiRendererThreadQueue
        : defaultRendererThreadQueue).add(runnable);
    }

    if (!repaintTimer.isRunning()) {
      repaintPanel();
      repaintTimer.start();
    }
  }

  public void repaintPanel() {
    if (!paintingEnabled) {
      return;
    }

    panel.superRepaint();
  }

  // [sstein: 20.01.2006]
  // Start: added by Ole
  // everything is static to make it useable before a LayerManager instance
  // (containing a RenderingManager) is created
  // which is the case, at the time the PlugIns are initialized and to have one
  // map
  // for all RenderingManager
  /**
   * @deprecated see {@link #getRendererFactory(Class)}
   */
  public static Renderer.ContentDependendFactory getRenderFactoryForLayerable(
    Class clss) {
    if (layerableClassToRendererFactoryMap.containsKey(clss)) {
      return (Renderer.ContentDependendFactory)layerableClassToRendererFactoryMap.get(clss);
    }
    return null;
  }

  /**
   * @param clss
   * @param rendererFactory
   * @deprecated see {@link #setRendererFactory(Class, RendererFactory)}
   */
  public static void putRendererForLayerable(Class clss,
    Renderer.ContentDependendFactory rendererFactory) {
    if (!layerableClassToRendererFactoryMap.containsKey(clss)) {
      layerableClassToRendererFactoryMap.put(clss, rendererFactory);
    }
  }

  // End: added by Ole*

  // this method is called by method render();
  protected Renderer createRenderer(Object contentID) {
    RendererFactory rendererFactory = getRendererFactory(contentID.getClass());
    if (rendererFactory != null) {
      return rendererFactory.create(contentID, panel, maxFeatures);
    }
    // p_d_austin: The following items should be removed when all renderers
    // are migrated to the RendererFactory framework.

    // [sstein: 20.01.2006] Start: added by Ole
    if (RenderingManager.getRenderFactoryForLayerable(contentID.getClass()) != null) {
      return RenderingManager.getRenderFactoryForLayerable(contentID.getClass())
        .create(contentID);
    }
    // End: added by Ole*
    if (contentIDToLowRendererFactoryMap.containsKey(contentID)) {
      return ((Renderer.Factory)contentIDToLowRendererFactoryMap.get(contentID)).create();
    }
    if (contentIDToHighRendererFactoryMap.containsKey(contentID)) {
      return ((Renderer.Factory)contentIDToHighRendererFactoryMap.get(contentID)).create();
    }

    throw new IllegalArgumentException("No renderer defined for layerable: "
      + contentID);
  }

  public void setPaintingEnabled(boolean paintingEnabled) {
    this.paintingEnabled = paintingEnabled;
  }

  public void copyTo(Graphics2D destination) {
    for (Iterator i = contentIDs().iterator(); i.hasNext();) {
      Object contentID = i.next();

      if (getRenderer(contentID) != null) {
        getRenderer(contentID).copyTo(destination);
      }
    }
  }

  public ThreadQueue getDefaultRendererThreadQueue() {
    return defaultRendererThreadQueue;
  }

  public void dispose() {
    repaintTimer.stop();
    defaultRendererThreadQueue.dispose();
    multiRendererThreadQueue.dispose();
    // The ThreadSafeImage cached in each Renderer consumes 1 MB of memory,
    // according to OptimizeIt [Jon Aquino]
    contentIDToRendererMap.clear();
  }

  public LayerViewPanel getPanel() {
    return panel;
  }

  // [sstein] added 30.07.2005

  /**
   * @return Returns the number of maxFeatures to render as vector graphic.
   */
  public int getMaxFeatures() {
    return maxFeatures;
  }

  /**
   * @param maxFeatures The maximum number of Features to render as vector
   *          graphic.
   *          <p>
   *          Use this method before using method render(Object contentID) or
   *          render(Object contentID, boolean clearImageCache)
   */
  public void setMaxFeatures(int maxFeatures) {
    this.maxFeatures = maxFeatures;
  }

  /**
   * Remove the LayerRenderer when a Layer is removed (helps to free the memory)
   * Added on 2007-05-21 [Michael Michaud and Larry Becker] Called by
   * LayerManager
   * 
   * @param contentID layer to remove
   */
  public void removeLayerRenderer(Object contentID) {
    contentIDToRendererMap.remove(contentID);
  }

  /**
   * Get the renderer factory for the class.
   * 
   * @param clazz The class
   * @return The renderer factory.
   */
  public static RendererFactory getRendererFactory(Class clazz) {
    if (clazz == null) {
      return null;
    } else {
      RendererFactory factory = CLASS_RENDERER_FACTORY_MAP.get(clazz);
      if (factory == null) {
        return getRendererFactory(clazz.getSuperclass());
      }
      return factory;
    }
  }

  /**
   * Set the renderer factory for the class.
   * 
   * @param clazz The class
   * @param factory The renderer factory.
   */
  public static void setRendererFactory(Class clazz, RendererFactory factory) {
    CLASS_RENDERER_FACTORY_MAP.put(clazz, factory);
  }

}
