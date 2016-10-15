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

import javax.swing.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.CheckBoxed;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import java.awt.*;

public class EditablePlugIn extends AbstractPlugIn implements CheckBoxed {

  private EditingPlugIn editingPlugIn;

  private static final String CONFIRMATION_TITLE = EditablePlugIn.class.getName() + ".make-read-only-layer-editable";
  private static final String CONFIRMATION_1 = EditablePlugIn.class.getName() + ".detach-layer-from-source-1";
  private static final String CONFIRMATION_2 = EditablePlugIn.class.getName() + ".detach-layer-from-source-2";

  public static final ImageIcon ICON = IconLoader.icon("edit.gif");

  public EditablePlugIn(EditingPlugIn editingPlugIn) {
    super();
    this.editingPlugIn = editingPlugIn;
  }

  public EditablePlugIn() {
    this(EditingPlugIn.getInstance());
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);

    boolean single = PersistentBlackboardPlugIn.get(context.getWorkbenchContext())
            .get(EditOptionsPanel.SINGLE_EDITABLE_LAYER_KEY, true);

    Layerable[] layers = getSelectedLayerables(context.getWorkbenchContext());
    // assume what to do by status of first selected layer
    boolean makeEditable = !layers[0].isEditable();
    // set states for each

    for (Layerable layerable : layers) {
      if (isWritable(layerable) && layerable.isVisible()) {
        if (single) setAllLayersToUneditable(context);
        //if (makeEditable) setAllLayersToUneditable(context);
        layerable.setEditable(makeEditable);
      } else if (layerable.isVisible()) {
        String message = "<html><br>" + I18N.getMessage(CONFIRMATION_1, "<i>'"+layerable.getName()+"'</i>");
        message += "<br><br>" + I18N.get(CONFIRMATION_2) + "<br></html>";
        JLabel label = new JLabel(message);
        JPanel panel = new JPanel();
        panel.add(label);
        OKCancelDialog okCancelPanel = new OKCancelDialog(
                context.getWorkbenchFrame(),
                I18N.getMessage(CONFIRMATION_TITLE),
                true,
                panel,
                new OKCancelDialog.Validator() {
                  @Override
                  public String validateInput(Component component) {
                    return null;
                  }
                });
        okCancelPanel.setVisible(true);
        if (okCancelPanel.wasOKPressed()) {
          if (single) setAllLayersToUneditable(context);
          //if (makeEditable) setAllLayersToUneditable(context);
          layerable.setEditable(makeEditable);
          if (layerable instanceof Layer) {
            ((Layer)layerable).setDataSourceQuery(null);
          }
        }
      }
    }

    // show EditToolBox if we switched to editable
    if (makeEditable
        && !editingPlugIn.getToolbox(context.getWorkbenchContext()).isVisible()) {
      editingPlugIn.execute(context);
    }
    return true;
  }

  private boolean isWritable(Layerable layerable) {
    if (layerable instanceof Layer) {
      Layer layer = (Layer)layerable;
      if (layer.getDataSourceQuery() == null ||
              layer.getDataSourceQuery().getDataSource() == null) {
        return true;
      } else {
        DataSource source = layer.getDataSourceQuery().getDataSource();
        return (source.isWritable() && source.getProperties().get(DataSource.COMPRESSED_KEY) == null);
      }
    } else return false;
  }

  public EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    MultiEnableCheck mec = new MultiEnableCheck();

    mec.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck());
    mec.add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));

    mec.add(new EnableCheck() {
      public String check(JComponent component) {
        ((JCheckBoxMenuItem) component)
            .setSelected(getSelectedLayerables(workbenchContext)[0]
                .isEditable());
        return null;
      }
    });

    mec.add(new EnableCheck() {
      public String check(JComponent component) {
        String errMsg = null;
        Layerable[] layers = getSelectedLayerables(workbenchContext);
        for (int i = 0; i < layers.length; i++) {
          if (layers[i].isReadonly()) {
            errMsg = I18N
                .get("ui.plugin.EditablePlugIn.The-selected-layer-cannot-be-made-editable");
            break;
          }
        }
        return errMsg;
      }
    });

    return mec;
  }

  private void setAllLayersToUneditable(PlugInContext context) {
    for (Object object : context.getLayerNamePanel().getLayerManager().getLayerables(Layerable.class))  {
      ((Layerable)object).setEditable(false);
    }
  }

  private static Layerable[] getSelectedLayerables(WorkbenchContext wbc) {
    Layerable[] layers = new Layerable[] {};

    JInternalFrame frame = wbc.getWorkbench().getFrame()
        .getActiveInternalFrame();
    if (frame instanceof LayerNamePanelProxy) {
      LayerNamePanel layerNamePanel = ((LayerNamePanelProxy) frame).getLayerNamePanel();
      if (layerNamePanel instanceof LayerableNamePanel)
        layers = ((LayerableNamePanel) layerNamePanel).getSelectedLayerables().toArray(
            new Layerable[] {});
      else
        layers = layerNamePanel.getSelectedLayers();
    }

    return layers;
  }
}
