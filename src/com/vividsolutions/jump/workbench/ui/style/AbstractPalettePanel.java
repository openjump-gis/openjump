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
package com.vividsolutions.jump.workbench.ui.style;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.util.java2xml.XML2Java;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;

public abstract class AbstractPalettePanel extends JPanel {

    protected ArrayList listeners = new ArrayList();

    public static class BasicStyleList {
        private ArrayList basicStyles = new ArrayList();
        public List getBasicStyles() {
            return Collections.unmodifiableList(basicStyles);
        }
        public void addBasicStyle(BasicStyle basicStyle) {
            basicStyles.add(basicStyle);
        }
    }

    public static interface Listener {
        public void basicStyleChosen(BasicStyle basicStyle);
    }

    public void add(Listener listener) {
        listeners.add(listener);
    }

    public abstract void setAlpha(int alpha);

    protected void fireBasicStyleChosen(BasicStyle basicStyle) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            Listener listener = (Listener) i.next();
            listener.basicStyleChosen(basicStyle);
        }
    }

    public static List basicStyles() {
        try {
            if (basicStyleList == null) {
                InputStream stream =
                    AbstractPalettePanel.class.getResourceAsStream(
                        StringUtil.classNameWithoutQualifiers(
                            AbstractPalettePanel.class.getName())
                            + ".xml");
                try {
                    InputStreamReader reader = new InputStreamReader(stream);
                    try {
                        basicStyleList =
                            ((BasicStyleList) new XML2Java()
                                .read(reader, BasicStyleList.class));
                    } finally {
                        reader.close();
                    }
                } finally {
                    stream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Assert.shouldNeverReachHere();
            return null;
        }
        return basicStyleList.getBasicStyles();
    }

    private static BasicStyleList basicStyleList = null;

    public static void main(String[] args) {
        Color c = new Color(255, 28, 174).darker();
        System.out.println(c.getRed() + ", " + c.getGreen() + ", " + c.getBlue());
    }
}
