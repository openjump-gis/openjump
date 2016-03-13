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

import java.awt.Font;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.InlineView;

import com.vividsolutions.jts.JTSVersion;
import com.vividsolutions.jump.workbench.plugin.Configuration;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;

public class ExtensionsAboutPanel extends JPanel {

  private JTextPane editorPane = new JTextPane();
  private PlugInManager plugInManager;

  public ExtensionsAboutPanel(PlugInManager plugInManager) {
    this.plugInManager = plugInManager;
    try {
      jbInit();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }

  public void refresh() {
    StringBuffer sb = new StringBuffer();
    int width = 460;
    sb.append("<html><head></head><body style=\"white-space:normal; width:"+width+"px;\">");

    // display standard system libs info
    sb.append("<b>JTS " + JTSVersion.CURRENT_VERSION + "</b><br>");
    sb.append("<hr>");

    // user extensions
    for (Iterator i = plugInManager.getConfigurations().iterator(); i.hasNext();) {
      Configuration configuration = (Configuration) i.next();
      String msg = PlugInManager.message(configuration);
      sb.append("<div><b>"
          + GUIUtil.escapeHTML(PlugInManager.name(configuration), false, false)
          + "</b> "
          + GUIUtil.escapeHTML(PlugInManager.version(configuration), false,
              false)
          + GUIUtil.escapeHTML(!msg.isEmpty() ? " -> " + msg : "", false, false)
          + "<br></div>");
    }
    sb.append("</body></html>");
    // TODO workaround: update panel by removal and readding
    // update became necessary after extensions might change their messages
    // during runtime
    remove(editorPane);
    editorPane.setText(sb.toString());
    add(editorPane);
  }

  void jbInit() throws Exception {
    editorPane.setEditable(false);
    editorPane.setOpaque(false);
    editorPane.setText("jEditorPane1");
    editorPane.setContentType("text/html");
    editorPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));

    // support line wrapping in a html formatted pane (thx Stanislav Lapitsky)
    editorPane.setEditorKit(new HTMLEditorKit() {
      @Override
      public ViewFactory getViewFactory() {
        return new HTMLFactory() {

          public View create(Element e) {
            View v = super.create(e);
            if (v instanceof InlineView) {
              return new InlineView(e) {

                boolean nowrap = false;

                @Override
                protected void setPropertiesFromAttributes() {
                  super.setPropertiesFromAttributes();

                  Object whitespace = this.getAttributes().getAttribute(
                      CSS.Attribute.WHITE_SPACE);
                  if ((whitespace != null) && whitespace.equals("nowrap")) {
                    nowrap = true;
                  } else {
                    nowrap = false;
                  }
                }

                public int getBreakWeight(int axis, float pos, float len) {
                  if (nowrap) {
                    return BadBreakWeight;
                  }
                  return super.getBreakWeight(axis, pos, len);
                }
              };
            }
            return v;
          }
        };
      }
    });
    
    // switch to default ui fonts (thx Stanislav Lapitsky)
    Font font = UIManager.getFont("Label.font");
    long fontSize = Math.round(font. getSize()*1.1);
    String bodyRule = "body { font-family: " + font.getFamily() + "; "
        + "font-size: " + fontSize + "pt; }";
    ((HTMLDocument) editorPane.getDocument()).getStyleSheet().addRule(bodyRule);
    // add proper line spacing
    String divRule = "div { margin-top:" +Math.round(fontSize/4)+ "pt; }";
    ((HTMLDocument) editorPane.getDocument()).getStyleSheet().addRule(divRule);

    add(editorPane);
  }

}
