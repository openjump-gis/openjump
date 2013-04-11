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
import java.awt.geom.NoninvertibleTransformException;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

/**
 * Advantage over ImageCachingRenderer: no cached image (typically 1 MB each).
 * Disadvantage: must redraw image each time (slower). Classic tradeoff between
 * space and time.
 */
public abstract class SimpleRenderer implements Renderer {
	protected volatile boolean cancelled = false;

	public SimpleRenderer(Object contentID, LayerViewPanel panel) {
		this.contentID = contentID;
		this.panel = panel;
	}

	private Object contentID;

	protected LayerViewPanel panel;

	protected abstract void paint(Graphics2D g) throws Exception;

	public void clearImageCache() {
	}

	public boolean isRendering() {
		return false;
	}

	public Object getContentID() {
		return contentID;
	}

	public void copyTo(Graphics2D graphics) {
		try {
			cancelled = false;
			paint(graphics);
		} catch (Throwable t) {
			panel.getContext().warnUser(WorkbenchFrame.toMessage(t));
			t.printStackTrace(System.err);
			return;
		}
	}

	public Runnable createRunnable() {
		return null;
	}

	public void cancel() {
		cancelled = true;
	}
}