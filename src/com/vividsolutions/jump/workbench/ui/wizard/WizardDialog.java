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

package com.vividsolutions.jump.workbench.ui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;


public class WizardDialog extends JDialog implements WizardContext,
    InputChangedListener {
    private JPanel panel1 = new JPanel();
    private BorderLayout borderLayout1 = new BorderLayout();
    private ArrayList completedWizardPanels;
    private JPanel buttonPanel = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JButton cancelButton = new JButton();
    private JButton nextButton = new JButton();
    private JButton backButton = new JButton();
    private JPanel fillerPanel = new JPanel();
    private Border border2;
    private Border border3;
    private JPanel outerCenterPanel = new JPanel();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
    private JPanel centerPanel = new JPanel();
    private Border border4;
    private JLabel titleLabel = new JLabel();
    private CardLayout cardLayout1 = new CardLayout();
    private WizardPanel currentWizardPanel;
    private List allWizardPanels;
    private ErrorHandler errorHandler;
    private JTextArea instructionTextArea = new JTextArea();
    private boolean finishPressed = false;
    private HashMap dataMap = new HashMap();
    private Border border5;
    private Border border6;
    private Border border7;

    public WizardDialog(Frame frame, String title, ErrorHandler errorHandler) {
        super(frame, title, true);
        this.errorHandler = errorHandler;

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    cancel();
                }
            });
    }

    private void checkIDs(Collection wizardPanels) {
        ArrayList ids = new ArrayList();

        for (Iterator i = wizardPanels.iterator(); i.hasNext();) {
            WizardPanel panel = (WizardPanel) i.next();
            ids.add(panel.getID());
        }

        for (Iterator i = wizardPanels.iterator(); i.hasNext();) {
            WizardPanel panel = (WizardPanel) i.next();

            if (panel.getNextID() == null) {
                continue;
            }

            Assert.isTrue(ids.contains(panel.getNextID()),
                I18N.get("ui.wizard.WizardDialog.required-panel-missing")+" " + panel.getNextID());
        }
    }

    private void setCurrentWizardPanel(WizardPanel wizardPanel) {
        if (currentWizardPanel != null) {
            currentWizardPanel.remove(this);
        }

        titleLabel.setText(wizardPanel.getTitle());
        cardLayout1.show(centerPanel, wizardPanel.getID());
        currentWizardPanel = wizardPanel;
        updateButtons();
        currentWizardPanel.add(this);
        instructionTextArea.setText(currentWizardPanel.getInstructions());
    }

    private WizardPanel getCurrentWizardPanel() {
        return currentWizardPanel;
    }

    private void updateButtons() {
        backButton.setEnabled(!completedWizardPanels.isEmpty());
        nextButton.setEnabled(getCurrentWizardPanel().isInputValid());
        nextButton.setText((getCurrentWizardPanel().getNextID() == null)
            ? I18N.get("ui.wizard.WizardDialog.finish"): I18N.get("ui.wizard.WizardDialog.next")+" >");
    }

    public void inputChanged() {
        updateButtons();
    }

    /**
     * @param wizardPanels the first of which will be the first WizardPanel that is displayed
     */
    public void init(WizardPanel[] wizardPanels) {
        allWizardPanels = Arrays.asList(wizardPanels);
        checkIDs(allWizardPanels);

        for (int i = 0; i < wizardPanels.length; i++) {
            centerPanel.add((Component) wizardPanels[i], wizardPanels[i].getID());
        }

        completedWizardPanels = new ArrayList();
        wizardPanels[0].enteredFromLeft(dataMap);
        setCurrentWizardPanel(wizardPanels[0]);
        pack();
    }

    private void jbInit() throws Exception {
        border7 = BorderFactory.createEmptyBorder(20, 20, 20, 10);
        instructionTextArea.setEnabled(false);
        instructionTextArea.setFont(new JLabel().getFont());
        instructionTextArea.setOpaque(false);
        instructionTextArea.setToolTipText("");
        //Don't hardcode colour to black, as user may be using an inverted colour scheme
        //[Jon Aquino 2004-05-31]
        instructionTextArea.setDisabledTextColor(instructionTextArea.getForeground());
        instructionTextArea.setEditable(false);
        instructionTextArea.setLineWrap(true);
        instructionTextArea.setWrapStyleWord(true);
        border5 = BorderFactory.createLineBorder(Color.black, 2);
        border6 = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(
                    Color.white, new Color(148, 145, 140)),
                BorderFactory.createEmptyBorder(20, 30, 20, 10));
        centerPanel.setLayout(cardLayout1);
        border2 = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        border3 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        border4 = BorderFactory.createBevelBorder(BevelBorder.LOWERED,
                Color.white, Color.white, new Color(103, 101, 98),
                new Color(148, 145, 140));
        panel1.setLayout(borderLayout1);
        buttonPanel.setLayout(gridBagLayout1);
        cancelButton.setText(I18N.get("ui.wizard.WizardDialog.cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancelButton_actionPerformed(e);
                }
            });
        nextButton.setText(I18N.get("ui.wizard.WizardDialog.next")+" >");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    nextButton_actionPerformed(e);
                }
            });
        backButton.setText("< "+I18N.get("ui.wizard.WizardDialog.back"));
        backButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    backButton_actionPerformed(e);
                }
            });
        buttonPanel.setBorder(border3);
        outerCenterPanel.setLayout(gridBagLayout2);
        titleLabel.setBackground(Color.white);
        titleLabel.setForeground(Color.black);
        titleLabel.setFont(new java.awt.Font("Dialog", 1, 12));
        titleLabel.setBorder(border7);
        titleLabel.setOpaque(true);
        titleLabel.setText("Title");
        outerCenterPanel.setBorder(border6);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        instructionTextArea.setText("instructionTextArea");
        getContentPane().add(panel1);
        panel1.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.add(cancelButton,
            new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 20, 0, 0), 0, 0));
        buttonPanel.add(nextButton,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(backButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(fillerPanel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        panel1.add(outerCenterPanel, BorderLayout.CENTER);
        outerCenterPanel.add(centerPanel,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        outerCenterPanel.add(instructionTextArea,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 20, 0), 0, 0));
        this.getContentPane().add(titleLabel, BorderLayout.NORTH);
    }

    void cancelButton_actionPerformed(ActionEvent e) {
        cancel();
    }

    private void cancel() {
        setVisible(false);
    }

    void nextButton_actionPerformed(ActionEvent e) {
        try {
            getCurrentWizardPanel().exitingToRight();

            if (getCurrentWizardPanel().getNextID() == null) {
                finishPressed = true;
                setVisible(false);

                return;
            }

            completedWizardPanels.add(getCurrentWizardPanel());

            WizardPanel nextWizardPanel = find(getCurrentWizardPanel()
                                                   .getNextID());
            nextWizardPanel.enteredFromLeft(dataMap);
            setCurrentWizardPanel(nextWizardPanel);
        } catch (Throwable x) {
            errorHandler.handleThrowable(x);
        }
    }

    private WizardPanel find(String id) {
        for (Iterator i = allWizardPanels.iterator(); i.hasNext();) {
            WizardPanel wizardPanel = (WizardPanel) i.next();

            if (wizardPanel.getID().equals(id)) {
                return wizardPanel;
            }
        }

        Assert.shouldNeverReachHere();

        return null;
    }

    public boolean wasFinishPressed() {
        return finishPressed;
    }

    void backButton_actionPerformed(ActionEvent e) {
        WizardPanel prevPanel = (WizardPanel) completedWizardPanels.remove(completedWizardPanels.size() -
                1);
        setCurrentWizardPanel(prevPanel);

        //Don't init panel if we're going back. [Jon Aquino]
    }

    public void setData(String name, Object value) {
        dataMap.put(name, value);
    }

    public Object getData(String name) {
        return dataMap.get(name);
    }
}
