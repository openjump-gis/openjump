package com.vividsolutions.jump.workbench.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.ui.renderer.LayerRenderer;


/**
 * Renders layers as an Image, which can then be saved to a file or printed.
 */
public class LayerPrinter {

    /**
	 * @param layers earlier layers will be rendered above later layers
	 */
	public BufferedImage print(Collection layers, Envelope envelope, int extentInPixels)
	    throws Exception {
	    Assert.isTrue(!layers.isEmpty());
	
	    final Throwable[] throwable = new Throwable[] { null };
	    LayerViewPanel panel = new LayerViewPanel(((Layer) layers.iterator()
	                                                             .next()).getLayerManager(),
	            new LayerViewPanelContext() {
	                public void setStatusMessage(String message) {
	                }
	
	                public void warnUser(String warning) {
	                }
	
	                public void handleThrowable(Throwable t) {
	                    throwable[0] = t;
	                }
	            });
	    panel.setSize(extentInPixels, extentInPixels);
	    panel.getViewport().zoom(envelope);
	
	    BufferedImage bufferedImage = new BufferedImage(panel.getWidth(),
	            panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
	    Graphics2D graphics = bufferedImage.createGraphics();
	    paintBackground(graphics, extentInPixels);
	
	    ArrayList layersReversed = new ArrayList(layers);
	    Collections.reverse(layersReversed);
	
	    for (Iterator i = layersReversed.iterator(); i.hasNext();) {
	        Layer layer = (Layer) i.next();

	        LayerRenderer renderer = new LayerRenderer(layer, panel);
	
	        //Wait for rendering to complete rather than running it in a separate thread. [Jon Aquino]
	        Runnable runnable = renderer.createRunnable();
	        if (runnable != null) { runnable.run(); }
	
	        //I hope no ImageObserver is needed. Set to null. [Jon Aquino]
	        renderer.copyTo(graphics);
	    }
	
	    if (throwable[0] != null) {
	        throw throwable[0] instanceof Exception ? (Exception) throwable[0]
	                                                : new Exception(throwable[0].getMessage());
	    }
	
	    return bufferedImage;
	}

    private void paintBackground(Graphics2D graphics, int extent) {
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, extent, extent);
    }

}
