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

package com.vividsolutions.jump.workbench.ui.plugin;

import java.io.File;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class GenerateLogPlugIn extends AbstractPlugIn {

  public GenerateLogPlugIn() {
  }

  public String getName() {
    return I18N.get("ui.plugin.GenerateLogPlugIn.log");
  }

  public ImageIcon getIcon() {
    return IconLoader.icon("application_view_list.png");
  }

  public boolean execute(PlugInContext context) throws java.lang.Exception {
    reportNothingToUndoYet(context);

    // we always create a new log file view
    HTMLFrame f = new HTMLFrame(context.getWorkbenchFrame());

    List<File> files = Logger.getLogFiles();
    for (File file : files) {

      StringBuffer buf = new StringBuffer();
      // no file , no log
      if (!file.canRead()) {
        buf.append("can't read " + file.getAbsolutePath());
      } else {
        int max_lines = 1000;
        int counter = 0;
        ReversedLinesFileReader rlrdr = null;
        try {
          rlrdr = new ReversedLinesFileReader(file, 4096, "UTF-8");

          String line;
          while ((line = rlrdr.readLine()) != null && counter < max_lines) {
            buf.insert(0, GUIUtil.escapeHTML(line, false, true)+"<br>");
            counter++;
          }

          buf.insert(0,
              "last " + max_lines + " lines of " + file.getAbsolutePath()
                  + ":<br><hr>");
        } finally {
          FileUtil.close(rlrdr);
        }
      }

      f.createNewDocument();
      f.addHeader(1, file.getName());
      f.addHeader(2, I18N.get("ui.plugin.GenerateLogPlugIn.generated") + " "
          + new Date());
      f.append(buf.toString());

    }
    f.surface();
    return true;
  }
}
