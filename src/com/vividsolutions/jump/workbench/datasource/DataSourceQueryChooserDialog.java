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
package com.vividsolutions.jump.workbench.datasource;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;

import java.awt.*;
import java.awt.event.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.*;

import org.openjump.core.CheckOS;

/**
 * Contains the various DataSourceQueryChooser panels, regardless of whether
 * they are for files, databases, web services, or other kinds of DataSources.
 * <p>
 * A bit confusing for files, as there are two "format" comboboxes for the user
 * to choose from: one for the DataSource type, and another for the file extension.
 * In the future, file DataSources may have their own dialog, eliminating the
 * first combobox.
 */
public class DataSourceQueryChooserDialog extends JDialog {

    private CardLayout cardLayout = new CardLayout();
    private BorderLayout borderLayout2 = new BorderLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private JPanel formatPanel = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JComboBox formatComboBox = new JComboBox();
    private JLabel formatLabel = new JLabel() {
            {
                setDisplayedMnemonic('F');
                setLabelFor(formatComboBox);
            }
        };

    private HashMap componentToNameMap = new HashMap();
    private OKCancelPanel okCancelPanel = new OKCancelPanel();
    
    public static int  LOADDIALOG = 1;
    public static int SAVEDIALOG = 2;
    private int dialogTask = 0;

    public DataSourceQueryChooserDialog(Collection dataSourceQueryChoosers,
        Frame frame, String title, boolean modal) {
        super(frame, title, modal);
        init(dataSourceQueryChoosers);
        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        addComponentListener(new ComponentAdapter() {
                public void componentShown(ComponentEvent e) {
                    okCancelPanel.setOKPressed(false);
                }
            });
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    //User may have hit OK, got a validation-error dialog, then hit the
                    //X button. [Jon Aquino]
                    okCancelPanel.setOKPressed(false);
                }
            });

        //Set the selected item to trigger the event that sets the panel. [Jon Aquino]
        formatComboBox.setSelectedItem(formatComboBox.getItemAt(0));
    }

    private void init(Collection dataSourceQueryChoosers) {
        //Some components may be shared, so use a Set. [Jon Aquino]
        HashSet components = new HashSet();
        for (Iterator i = dataSourceQueryChoosers.iterator(); i.hasNext();) {
            DataSourceQueryChooser chooser = (DataSourceQueryChooser) i.next();
            formatComboBox.addItem(chooser);
            components.add(chooser.getComponent());
        }

        int j = 0;
        for (Iterator i = components.iterator(); i.hasNext();) {
            Component component = (Component) i.next();

            //Can't use DataSourceQueryChooser name because several DataSourceQueryChoosers may
            //share a component (e.g. FileDataSourceQueryChooser). [Jon Aquino]
            j++;
            componentToNameMap.put(component, I18N.get("datasource.DataSourceQueryChooserDialog.card")+" "+ j);
            mainPanel.add(component, name(component));
        }
    }

    private String name(Component component) {
        return (String) componentToNameMap.get(component);
    }

    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout2);
        formatPanel.setLayout(gridBagLayout1);
        formatPanel.setBorder(BorderFactory.createEtchedBorder());
        formatLabel.setText( I18N.get("datasource.DataSourceQueryChooserDialog.format"));
        formatComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    formatComboBox_actionPerformed(e);
                    pack();
                }
            });
        okCancelPanel.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    okCancelPanel_actionPerformed(e);
                }
            });
        
        this.getContentPane().add(mainPanel, BorderLayout.NORTH);
        this.getContentPane().add(formatPanel, BorderLayout.CENTER);
        this.getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
        formatPanel.add(formatComboBox,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(16, 4, 16, 4), 0, 0));
        formatPanel.add(formatLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(16, 4, 16, 4), 0, 0));
    }

    /**
     * @return true if the user hit OK; false if the user hit Cancel or the Close Window button.
     */
    public boolean wasOKPressed() {
        return okCancelPanel.wasOKPressed();
    }

    public void setOKPressed() {
        // we just process this one time
        if (okCancelPanel.wasOKPressed())
          return;

        // It is important to call setOKPressed before calling isInputValid
        // otherwise we run into an infinite loop of actionPerformed calls.
        okCancelPanel.setOKPressed(true);

        if ( getCurrentChooser().isInputValid() ) {
            setVisible(false);
        } else {
            okCancelPanel.setOKPressed(false);
        }
    }

    void formatComboBox_actionPerformed(ActionEvent e) {
        showFormat();
    }

    public void showFormat() {
        cardLayout.show(mainPanel, name(getCurrentChooser().getComponent()));
    }

    public DataSourceQueryChooser getCurrentChooser() {
        return (DataSourceQueryChooser) formatComboBox.getSelectedItem();
    }

    void okCancelPanel_actionPerformed(ActionEvent e) {

        if (!okCancelPanel.wasOKPressed()) { //cancel case
            setVisible(false);
        }
        else{ 
          if (this.getDialogTask() == DataSourceQueryChooserDialog.LOADDIALOG) {
            // --sstein: leave out validation - because it returns always "false" on
            // Mac-OSX ?
            // because the getCurrentChooser() returns has a null pointer
            // System.out.println("validate input:" +
            // getCurrentChooser().isInputValid());
            if ((okCancelPanel.wasOKPressed()) && (CheckOS.isMacOsx())) {
              // System.out.println("this is a mac and we load data");
              okCancelPanel.setOKPressed(true);
              setVisible(false);
            } else {
              if (getCurrentChooser().isInputValid()) {
                setVisible(false);
              }
            }
          } else { // Now we use the dialog for saving
            if (getCurrentChooser().isInputValid()) {
              setVisible(false);
            }else{
              okCancelPanel.setOKPressed(false);
            }
          }
        }
    }
    
    public String getSelectedFormat() {
        return formatComboBox.getSelectedItem().toString();
    }

    public void setSelectedFormat(String format) {
        for (int i = 0; i < formatComboBox.getItemCount(); i++) {
            DataSourceQueryChooser chooser = (DataSourceQueryChooser) formatComboBox.getItemAt(i);
            if (chooser.toString().equals(format)) {
                formatComboBox.setSelectedIndex(i);

                return;
            }
        }
    }

	public int getDialogTask() {
		return dialogTask;
	}

	public void setDialogTask(int dialogTask) {
		this.dialogTask = dialogTask;
	}
}
