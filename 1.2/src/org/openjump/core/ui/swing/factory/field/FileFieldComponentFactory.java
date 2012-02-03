/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.swing.factory.field;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComponent;

import org.openjump.swing.factory.field.FieldComponentFactory;
import org.openjump.swing.listener.ValueChangeEvent;
import org.openjump.swing.listener.ValueChangeListener;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.FileNamePanel;

public class FileFieldComponentFactory implements FieldComponentFactory {
  private WorkbenchContext workbenchContext;

  public FileFieldComponentFactory(final WorkbenchContext workbenchContext) {
    this.workbenchContext = workbenchContext;
  }

  public Object getValue(final JComponent component) {
    if (component instanceof FileNamePanel) {
      FileNamePanel fileNamePanel = (FileNamePanel)component;
      return fileNamePanel.getSelectedFile();
    }
    return null;
  }

  public void setValue(JComponent component, Object value) {
    if (component instanceof FileNamePanel) {
      FileNamePanel fileNamePanel = (FileNamePanel)component;
      File file = null;
      if (value != null) {
        file = new File(value.toString());
      }
      fileNamePanel.setSelectedFile(file);
    }
  }

  public JComponent createComponent() {
    FileNamePanel fileNamePanel = new FileNamePanel(
      workbenchContext.getErrorHandler());
    fileNamePanel.setUpperDescription("");
    return fileNamePanel;
  }

  public JComponent createComponent(final ValueChangeListener listener) {
    final FileNamePanel fileNamePanel = new FileNamePanel(
      workbenchContext.getErrorHandler());
    fileNamePanel.setUpperDescription("");
    fileNamePanel.addBrowseListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        File file = fileNamePanel.getSelectedFile();
        listener.valueChanged(new ValueChangeEvent(fileNamePanel,
          file.getAbsolutePath()));
      }
    });
    return fileNamePanel;
  }

}
