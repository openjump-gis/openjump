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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.OptionsPanel;
import com.vividsolutions.jump.workbench.ui.TrackedPopupMenu;

/**
* 
* Implements an {@link OptionsPanel} to allow skin selection.
* 
*/

public class SkinOptionsPanel extends JPanel implements OptionsPanel {
    private static final String CURRENT_SKIN_KEY = SkinOptionsPanel.class +
        " - CURRENT SKIN";
    public static final String SKINS_KEY = SkinOptionsPanel.class + " - SKINS";
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JComboBox comboBox = new JComboBox();
    private JPanel fillerPanel = new JPanel();
    private JLabel label = new JLabel();
    private Blackboard blackboard;
    private boolean modified;

    public SkinOptionsPanel(Blackboard blackboard, Window window) {
        this.blackboard = blackboard;
        try {
            comboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    modified = true;
                }
            });
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        label.setText(I18N.get("ui.plugin.skin.InstallSkinsPlugIn.skins") + ":");
        this.add(comboBox,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(10, 0, 10, 10), 0, 0));
        this.add(fillerPanel,
            new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(label,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(10, 10, 10, 4), 0, 0));
    }

    public void init() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        for (Iterator i = ((Collection) blackboard.get(SKINS_KEY)).iterator();
                i.hasNext();) {
            LookAndFeelProxy proxy = (LookAndFeelProxy) i.next();
            model.addElement(proxy);
        }

        comboBox.setModel(model);
        comboBox.setSelectedItem(blackboard.get(CURRENT_SKIN_KEY,
                comboBox.getModel().getElementAt(0)));
        modified = false;
    }

    public void okPressed() {
        if (!modified) {
            return;
        }
        blackboard.put(CURRENT_SKIN_KEY, comboBox.getSelectedItem());
        try {
            UIManager.setLookAndFeel(((LookAndFeelProxy) comboBox.getSelectedItem()).getLookAndFeel());            
        } catch (UnsupportedLookAndFeelException e) {
            Assert.shouldNeverReachHere(e.toString());
        }
        updateFrames();
        updatePopupMenus();
    }

    private void updatePopupMenus() {
        for (Iterator i = TrackedPopupMenu.trackedPopupMenus().iterator(); i.hasNext();) {
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
        // [mmichaud] 2011-07-10 dirty hack, because updating OptionsDialog
        // throws an exception (may be caused by Options dialog ui being updated
        // and closed in a concurrent way, but not sure...)
        if (w instanceof com.vividsolutions.jump.workbench.ui.OptionsDialog) return;
        // [UT] 2005-10-26 bug fix for editing toolbox resize problem found by uwe dalluege
        // [mmichaud] 2011-07-10 pack moved after updateComponentTreeUI
        //w.pack();        
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
