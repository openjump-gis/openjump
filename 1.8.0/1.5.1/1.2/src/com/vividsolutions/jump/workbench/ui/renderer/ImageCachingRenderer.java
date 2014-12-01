/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
import javax.swing.SwingUtilities;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
public abstract class ImageCachingRenderer implements Renderer {
	protected volatile boolean cancelled = false;
	private Object contentID;
	protected volatile ThreadSafeImage image = null;
	protected LayerViewPanel panel;
	protected volatile boolean rendering = false;
	public ImageCachingRenderer(Object contentID, LayerViewPanel panel) {
		this.contentID = contentID;
		this.panel = panel;
	}
	public void clearImageCache() {
		image = null;
	}
	public boolean isRendering() {
		return rendering;
	}
	public Object getContentID() {
		return contentID;
	}
	protected ThreadSafeImage getImage() {
		return image;
	}
	public void copyTo(Graphics2D graphics) {
		//Some subclasses override #getImage [Jon Aquino]
		if (getImage() == null) {
			return;
		}
		getImage().copyTo(graphics, null);
	}
	public Runnable createRunnable() {
		if (image != null) {
			return null;
		}
		//Rendering starts as soon as the #createRunnable request is made,
		//to get the animated clock icons going. [Jon Aquino]
		rendering = true;
		cancelled = false;
		return new Runnable() {
			public void run() {
				try {
					if (cancelled) {
						//This short-circuit exit made a big speed increase
						// (21 March 2003). [Jon Aquino]
						return;
					}
					image = new ThreadSafeImage(panel);
					try {
						renderHook(image);
					} catch (Throwable t) {
						panel.getContext()
								.warnUser(WorkbenchFrame.toMessage(t));
						t.printStackTrace(System.err);
						return;
					}
					//Don't wait for the RenderingManager's 1-second
					// repaint-timer. [Jon Aquino]
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							panel.superRepaint();
						}
					});
				} finally {
					rendering = false;
				}
			}
		};
	}
	protected abstract void renderHook(ThreadSafeImage image) throws Exception;
	public void cancel() {
		cancelled = true;
	}
}
