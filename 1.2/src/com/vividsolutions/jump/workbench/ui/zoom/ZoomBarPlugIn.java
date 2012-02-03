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
package com.vividsolutions.jump.workbench.ui.zoom;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.NoninvertibleTransformException;

import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxPlugIn;

public class ZoomBarPlugIn extends ToolboxPlugIn {

    private static final int WIDTH = 300;
    protected void initializeToolbox(ToolboxDialog toolbox) {
        try {
            final ZoomBar zoomBar =
                new ZoomBar(true, false, toolbox.getContext().getWorkbench().getFrame());
            toolbox.getCenterPanel().add(zoomBar, BorderLayout.CENTER);
            zoomBar.setPreferredSize(new Dimension(WIDTH, (int)zoomBar.getPreferredSize().getHeight()));
            toolbox.addWindowListener(new WindowAdapter() {
                public void windowOpened(WindowEvent e) {
                    try {
                        zoomBar.updateComponents();
                    } catch (NoninvertibleTransformException x) {
                        //Eat it. [Jon Aquino]
                    }
                }
            });
            toolbox.setInitialLocation(new GUIUtil.Location(20, false, 20, true));
        } catch (NoninvertibleTransformException x) {
            toolbox.getContext().getWorkbench().getFrame().handleThrowable(x);
        }
    }

}
