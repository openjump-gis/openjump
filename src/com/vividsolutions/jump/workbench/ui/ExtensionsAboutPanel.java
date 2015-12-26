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
package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.vividsolutions.jts.JTSVersion;
import com.vividsolutions.jump.workbench.plugin.Configuration;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;

public class ExtensionsAboutPanel extends JPanel {

    private JEditorPane editorPane = new JEditorPane();
    private PlugInManager plugInManager;

    public ExtensionsAboutPanel(PlugInManager plugInManager) {
        this.plugInManager = plugInManager;
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        addAncestorListener(new AncestorListener() {
          
          @Override
          public void ancestorRemoved(AncestorEvent event) {
          }
          
          @Override
          public void ancestorMoved(AncestorEvent event) {
          }
          // reload if the tab is activated
          @Override
          public void ancestorAdded(AncestorEvent event) {
            refresh();
          }
        });

    }

    public void refresh() {
        StringBuffer sb = new StringBuffer();
        sb.append("<html><head></head><body>");

        // display standard system libs info
        sb.append("<b>JTS " + JTSVersion.CURRENT_VERSION + "</b><br>");
        sb.append("<hr>");

        // user extensions
        for (Iterator i = plugInManager.getConfigurations().iterator(); i.hasNext();) {
            Configuration configuration = (Configuration) i.next();
            String msg = PlugInManager.message(configuration);
            sb.append(
                "<b>"
                    + GUIUtil.escapeHTML(PlugInManager.name(configuration), false, false)
                    + "</b> "
                    + GUIUtil.escapeHTML(PlugInManager.version(configuration), false, false)
                    + GUIUtil.escapeHTML(!msg.isEmpty()?" -> "+msg:"", false, false)
                    + "<br>");
        }
        sb.append("</body></html>");
        // workaround: update panel by removal and readding
        // update became necessary after extensions might change their messages
        // during runtime
        remove(editorPane);
        editorPane.setText(sb.toString());
        add(editorPane);
    }

    void jbInit() throws Exception {
        setLayout(new BorderLayout());
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setText("jEditorPane1");
        editorPane.setContentType("text/html");
        editorPane.setBorder(BorderFactory.createEmptyBorder());
        //this.add(editorPane, BorderLayout.NORTH);
    }
}