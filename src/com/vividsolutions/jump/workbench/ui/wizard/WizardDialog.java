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
import java.awt.FlowLayout;
import java.awt.Frame;
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

import org.openjump.swing.listener.InvokeMethodActionListener;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;

public class WizardDialog extends JDialog implements WizardContext,
  InputChangedListener {

  private ArrayList completedWizardPanels;

  private JPanel centerPanel = new JPanel();

  private JLabel titleLabel = new JLabel();

  private CardLayout cardLayout = new CardLayout();

  private WizardPanel currentWizardPanel;

  private List allWizardPanels;

  private ErrorHandler errorHandler;

  private JTextArea instructionTextArea = new JTextArea();

  private boolean finishPressed = false;

  private HashMap dataMap = new HashMap();

  private JButton backButton;

  private JButton nextButton;

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
      WizardPanel panel = (WizardPanel)i.next();
      ids.add(panel.getID());
    }

    for (Iterator i = wizardPanels.iterator(); i.hasNext();) {
      WizardPanel panel = (WizardPanel)i.next();

      if (panel.getNextID() == null) {
        continue;
      }

      Assert.isTrue(ids.contains(panel.getNextID()),
        I18N.get("ui.wizard.WizardDialog.required-panel-missing") + " "
          + panel.getNextID());
    }
  }

  private void setCurrentWizardPanel(WizardPanel wizardPanel) {
    if (currentWizardPanel != null) {
      currentWizardPanel.remove(this);
    }

    titleLabel.setText(wizardPanel.getTitle());
    cardLayout.show(centerPanel, wizardPanel.getID());
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
    nextButton.setText((getCurrentWizardPanel().getNextID() == null) ? I18N.get("ui.wizard.WizardDialog.finish")
      : I18N.get("ui.wizard.WizardDialog.next") + " >");
  }

  public void inputChanged() {
    updateButtons();
  }

  /**
   * @param wizardPanels the first of which will be the first WizardPanel that
   *          is displayed
   */
  public void init(WizardPanel[] wizardPanels) {
    allWizardPanels = Arrays.asList(wizardPanels);
    checkIDs(allWizardPanels);

    for (int i = 0; i < wizardPanels.length; i++) {
      centerPanel.add((Component)wizardPanels[i], wizardPanels[i].getID());
    }

    completedWizardPanels = new ArrayList();
    wizardPanels[0].enteredFromLeft(dataMap);
    setCurrentWizardPanel(wizardPanels[0]);
    pack();
  }

  private void jbInit() throws Exception {
    JPanel panel = new JPanel(new BorderLayout());
    getContentPane().add(panel);

    panel.add(createTitlePanel(), BorderLayout.NORTH);

    centerPanel.setLayout(cardLayout);
    centerPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
      BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    panel.add(centerPanel, BorderLayout.CENTER);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    panel.add(createButtonPanel(), BorderLayout.SOUTH);

  }

  private JPanel createTitlePanel() {
    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBackground(Color.white);
    titlePanel.setForeground(Color.black);
    titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    titleLabel.setFont(new java.awt.Font("Dialog", 1, 12));
    titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
    titleLabel.setText("Title");
    titlePanel.add(titleLabel, BorderLayout.NORTH);

    instructionTextArea.setEnabled(false);
    instructionTextArea.setFont(new JLabel().getFont());
    instructionTextArea.setOpaque(false);
    instructionTextArea.setToolTipText("");
    instructionTextArea.setDisabledTextColor(instructionTextArea.getForeground());
    instructionTextArea.setEditable(false);
    instructionTextArea.setLineWrap(true);
    instructionTextArea.setWrapStyleWord(true);
    instructionTextArea.setText("instructionTextArea");
    titlePanel.add(instructionTextArea, BorderLayout.SOUTH);

    return titlePanel;
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel();
    FlowLayout layout = new FlowLayout(FlowLayout.RIGHT);
    buttonPanel.setLayout(layout);

    backButton = new JButton("< " + I18N.get("ui.wizard.WizardDialog.back"));
    backButton.addActionListener(new InvokeMethodActionListener(this,
      "previousPanel"));
    buttonPanel.add(backButton);

    nextButton = new JButton(I18N.get("ui.wizard.WizardDialog.next") + " >");
    nextButton.addActionListener(new InvokeMethodActionListener(this,
      "nextPanel"));
    buttonPanel.add(nextButton);

    JPanel filler = new JPanel();
    filler.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
    buttonPanel.add(filler);

    JButton cancelButton = new JButton(
      I18N.get("ui.wizard.WizardDialog.cancel"));
    cancelButton.addActionListener(new InvokeMethodActionListener(this,
      "cancel"));
    buttonPanel.add(cancelButton);
    return buttonPanel;
  }

  public void cancel() {
    setVisible(false);
  }

  public void nextPanel() {
    try {
      getCurrentWizardPanel().exitingToRight();

      if (getCurrentWizardPanel().getNextID() == null) {
        finishPressed = true;
        setVisible(false);

        return;
      }

      completedWizardPanels.add(getCurrentWizardPanel());

      WizardPanel nextWizardPanel = find(getCurrentWizardPanel().getNextID());
      nextWizardPanel.enteredFromLeft(dataMap);
      setCurrentWizardPanel(nextWizardPanel);
    } catch (Throwable x) {
      errorHandler.handleThrowable(x);
    }
  }

  private WizardPanel find(String id) {
    for (Iterator i = allWizardPanels.iterator(); i.hasNext();) {
      WizardPanel wizardPanel = (WizardPanel)i.next();

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

  public void previousPanel() {
    WizardPanel prevPanel = (WizardPanel)completedWizardPanels.remove(completedWizardPanels.size() - 1);
    setCurrentWizardPanel(prevPanel);
  }

  public void setData(String name, Object value) {
    dataMap.put(name, value);
  }

  public Object getData(String name) {
    return dataMap.get(name);
  }
}
