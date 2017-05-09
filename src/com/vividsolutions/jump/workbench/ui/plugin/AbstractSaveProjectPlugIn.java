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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.swing.JInternalFrame;
import javax.xml.namespace.QName;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

/**
 * Subclass this to implement a 'Save Project' plugin.
 */
public abstract class AbstractSaveProjectPlugIn extends AbstractPlugIn {

  public AbstractSaveProjectPlugIn() {
  }

  @Override
  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    context.getFeatureInstaller().addMainMenuPlugin(this,
        new String[] { MenuNames.FILE });
  }

  protected void save(Task task, File file, WorkbenchFrame frame)
      throws Exception {
    // First use StringWriter to make sure no errors occur before we touch the
    // original file -- we don't want to damage the original if an error occurs.
    // [Jon Aquino]
    JInternalFrame taskWindow = frame.getActiveInternalFrame();
    task.setMaximized(taskWindow.isMaximum());
    if (taskWindow.isMaximum()) { // save the rectangle that it would be
                                  // restored to
      Rectangle normalBounds = taskWindow.getNormalBounds();
      task.setTaskWindowLocation(new Point(normalBounds.x, normalBounds.y));
      task.setTaskWindowSize(new Dimension(normalBounds.width,
          normalBounds.height));
    } else {
      task.setTaskWindowLocation(taskWindow.getLocation());
      task.setTaskWindowSize(taskWindow.getSize());
    }
    task.setSavedViewEnvelope(frame.getContext().getLayerViewPanel()
        .getViewport().getEnvelopeInModelCoordinates());
    task.setProperty(new QName(Task.PROJECT_FILE_KEY), file.getAbsolutePath());
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    task.setProperty(new QName(Task.PROJECT_TIME_KEY),
            dateFormat.format(date));
    StringWriter stringWriter = new StringWriter();

    try {
      new Java2XML().write(task, "project", stringWriter);
    } finally {
      stringWriter.flush();
    }

    FileUtil.setContents(file.getAbsolutePath(), stringWriter.toString(), "UTF-8");
    task.setName(GUIUtil.nameWithoutExtension(file));
    task.setProjectFile(file);

    Collection<Layer> ignoredLayers = new ArrayList<>(ignoredLayers(task));

    if (!ignoredLayers.isEmpty()) {

      Logger.info(I18N
              .get("ui.plugin.AbstractSaveProjectPlugIn.some-layers-were-not-saved-to-the-task-file"));

      for (Layer ignoredLayer : ignoredLayers) {
        Logger.info("- " + ignoredLayer.getName() + " (" +
                I18N.get("ui.plugin.AbstractSaveProjectPlugIn.data-source-is-write-only") + ")");
      }

    }
  }

  protected Collection<Layer> ignoredLayers(Task task) {
    ArrayList<Layer> ignoredLayers = new ArrayList<>();

    for (Layer layer : task.getLayerManager().getLayers()) {
      if (!layer.hasReadableDataSource()) {
        ignoredLayers.add(layer);
      }
    }

    return ignoredLayers;
  }

  @Override
  public EnableCheck getEnableCheck() {
    return EnableCheckFactory.getInstance().createTaskWindowMustBeActiveCheck();
  }
}
