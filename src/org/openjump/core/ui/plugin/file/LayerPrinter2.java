/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */

package org.openjump.core.ui.plugin.file;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.util.Collection;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.renderer.ThreadQueue;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;

/**
 * Renders layers as an Image, which can then be saved to a file or printed.
 */
public class LayerPrinter2 {

	private RenderingManager renderingManager;
	private Graphics2D graphics;
	private LayerViewPanel panel = null;
	
	public LayerViewPanel getLayerViewPanel(){
		return panel;
	}
	
    /**
	 * @param layers earlier layers will be rendered above later layers
	 */
	public BufferedImage print(Collection layers, Envelope envelope, int extentInPixels)
	throws Exception {

		final Throwable[] throwable = new Throwable[] { null };
		panel = 
			new LayerViewPanel( 
					(!layers.isEmpty()) ? ((Layerable)layers.iterator().next()).getLayerManager()
							: new LayerManager(),
							new LayerViewPanelContext() {
						public void setStatusMessage(String message) {
						}

						public void warnUser(String warning) {
						}

						public void handleThrowable(Throwable t) {
							throwable[0] = t;
						}
					});
		int extentInPixelsX;
		int extentInPixelsY;
		double width = envelope.getWidth();
		double height = envelope.getHeight();

		if (width > height)
		{
			extentInPixelsX = extentInPixels;
			extentInPixelsY = (int)Math.round(height / width * extentInPixels);
		}
		else
		{
			extentInPixelsY = extentInPixels;
			extentInPixelsX = (int)Math.round(width / height * extentInPixels);
		}

		panel.setSize(extentInPixelsX, extentInPixelsY);
		panel.getViewport().zoom(envelope);

		BufferedImage bufferedImage = new BufferedImage(panel.getWidth(),
				panel.getHeight(), BufferedImage.TYPE_INT_RGB); //formerly TYPE_INT_ARGB);
				graphics = bufferedImage.createGraphics();
				graphics.setRenderingHint(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				paintBackground(graphics, extentInPixelsX, extentInPixelsY);

				renderingManager = panel.getRenderingManager();
//				old method
				renderingManager.renderAll();
				ThreadQueue runningThreads = renderingManager.getDefaultRendererThreadQueue();
				while (runningThreads.getRunningThreads()>0)
				Thread.sleep(200);
				renderingManager.copyTo(graphics);
//				new method of rendering requires RenderingManager changes
//				panel.getRenderingManager().setRenderingMode(
//						RenderingManager.EXECUTE_ON_EVENT_THREAD); //block the GUI until done
//				renderingManager.renderAll();
//				renderingManager.copyTo(graphics);
//				panel.getRenderingManager().setRenderingMode(RenderingManager.INTERACTIVE);

				if (throwable[0] != null) {
					throw throwable[0] instanceof Exception ? (Exception) throwable[0]
					                                                                : new Exception(throwable[0].getMessage());
				}
				panel.dispose();
				return bufferedImage;
	}

    private void paintBackground(Graphics2D graphics, int extentX, int extentY) {
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, extentX, extentY);
    }

}
