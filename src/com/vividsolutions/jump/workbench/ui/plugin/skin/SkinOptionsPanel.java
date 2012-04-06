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

package com.vividsolutions.jump.workbench.ui.plugin.skin;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;
import com.vividsolutions.jump.workbench.ui.OptionsPanel;
import com.vividsolutions.jump.workbench.ui.TrackedPopupMenu;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * 
 * Implements an {@link OptionsPanel} to allow skin selection.
 * 
 */

public class SkinOptionsPanel extends JPanel implements OptionsPanel {
  private static final String CURRENT_SKIN_KEY = SkinOptionsPanel.class
      + " - CURRENT SKIN";
  public static final String SKINS_KEY = SkinOptionsPanel.class + " - SKINS";
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JComboBox comboBox = new JComboBox();
  private JPanel fillerPanel = new JPanel();
  private JLabel label = new JLabel();
  private PlugInContext context;
  private Blackboard blackboard;
  private Blackboard blackboard_persist;

  public SkinOptionsPanel(PlugInContext plc) {
    this.blackboard = plc.getWorkbenchContext().getWorkbench().getBlackboard();
    this.blackboard_persist = PersistentBlackboardPlugIn.get(plc
        .getWorkbenchContext());
    this.context = plc;

    String saved_skin = (String) blackboard_persist.get(CURRENT_SKIN_KEY);
    String cur_skin = UIManager.getLookAndFeel().getClass().getName();

    try {
      jbInit();

      DefaultComboBoxModel model = new DefaultComboBoxModel();

      for (Iterator i = ((Collection) blackboard.get(SKINS_KEY)).iterator(); i
          .hasNext();) {
        LookAndFeelProxy proxy = (LookAndFeelProxy) i.next();
        String proxy_skin = proxy.getLookAndFeel().getClass().getName();
        model.addElement(proxy);

        // activate saved laf if available
        if (saved_skin instanceof String && saved_skin.equals(proxy_skin)
            && cur_skin instanceof String && !cur_skin.equals(proxy_skin)) {
          updateAll(proxy.getLookAndFeel());
          cur_skin = proxy_skin;
        }
        // reflect active laf selected in combobox
        if (cur_skin == proxy_skin)
          model.setSelectedItem(proxy);
      }

      comboBox.setModel(model);
    } catch (Exception ex) {
      context.getWorkbenchFrame().handleThrowable(ex);
    }
  }

  void jbInit() throws Exception {
    this.setLayout(gridBagLayout1);
    label.setText(I18N.get("ui.plugin.skin.InstallSkinsPlugIn.skins") + ":");
    this.add(comboBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0,
            10, 10), 0, 0));
    this.add(fillerPanel, new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
            0), 0, 0));
    this.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10,
            10, 4), 0, 0));
  }

  // all initialization is done in constructor
  public void init() {
  }

  public void okPressed() {
    updateAll(((LookAndFeelProxy) comboBox.getSelectedItem()).getLookAndFeel());
  }

  private void updateAll(LookAndFeel laf) {

    // do nothing if this laf is already set
    if (UIManager.getLookAndFeel().getClass().equals(laf.getClass())) {
      return;
    }

    try {
      UIManager.setLookAndFeel(laf);
    } catch (UnsupportedLookAndFeelException e) {
      context.getWorkbenchFrame().handleThrowable(e);
    }
    updateFrames();
    updatePopupMenus();
    SwingUtilities.updateComponentTreeUI(OptionsDialog.instance(
        context.getWorkbenchContext().getWorkbench()).getContentPane());
    // during start we are added to the optionspanel later, hence update us
    // explicitely
    SwingUtilities.updateComponentTreeUI(this);

    // save current laf to workbench state for restoration after restart
    blackboard_persist.put(CURRENT_SKIN_KEY, laf.getClass().getName());
  }

  private void updatePopupMenus() {
    for (Iterator i = TrackedPopupMenu.trackedPopupMenus().iterator(); i
        .hasNext();) {
      final JPopupMenu menu = (JPopupMenu) i.next();
      SwingUtilities.updateComponentTreeUI(menu);
    }
  }

  private void updateFrames() {
    Frame[] frames = Frame.getFrames();

    for (int i = 0; i < frames.length; i++) {
      Window[] windows = frames[i].getOwnedWindows();
      for (int j = 0; j < windows.length; j++) {
        updateWindow(windows[j]);
      }
      SwingUtilities.updateComponentTreeUI(frames[i]);
    }
  }

  private void updateWindow(final Window w) {
    Window[] children = w.getOwnedWindows();
    for (int i = 0; i < children.length; i++) {
      updateWindow(children[i]);
    }
    SwingUtilities.updateComponentTreeUI(w);
    w.pack();
  }

  public String validateInput() {
    return null;
  }

}
