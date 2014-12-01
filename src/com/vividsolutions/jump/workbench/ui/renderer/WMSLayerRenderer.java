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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class WMSLayerRenderer extends ImageCachingRenderer {
	public WMSLayerRenderer(WMSLayer layer, LayerViewPanel panel) {
		super(layer, panel);
	}

	public ThreadSafeImage getImage() {
		if (!getLayer().isVisible()) {
			return null;
		}

		return super.getImage();
	}

	public Runnable createRunnable() {
		if (!LayerRenderer.render(getLayer(), panel)) {
			return null;
		}
		return super.createRunnable();
	}
	
	public void copyTo(Graphics2D graphics) {
		if (!LayerRenderer.render(getLayer(), panel)) {
			return;
		}	
		super.copyTo(graphics);
	}	

	private WMSLayer getLayer() {
		return (WMSLayer) getContentID();
	}

	protected void renderHook(ThreadSafeImage image) throws Exception {
		if (!getLayer().isVisible()) {
			return;
		}

		//Create the image outside the synchronized call to #draw, because it
		// takes
		//a few seconds, and we don't want to block repaints. [Jon Aquino]
		final Image sourceImage = getLayer().createImage(panel);

		//Drawing can take a long time. If the renderer is cancelled during
		// this
		//time, don't draw when the request returns. [Jon Aquino]
		if (cancelled) {
			return;
		}

		image.draw(new ThreadSafeImage.Drawer() {
			public void draw(Graphics2D g) throws Exception {
				//Not sure what the best rule is; SRC_OVER seems to work. [Jon
				// Aquino]
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, getLayer().getAlpha() / 255f));
				g.drawImage(sourceImage, 0, 0, null);
			}
		});
	}
}