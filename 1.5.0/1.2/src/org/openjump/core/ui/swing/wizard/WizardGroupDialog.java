package org.openjump.core.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openjump.swing.listener.InvokeMethodActionListener;
import org.openjump.swing.listener.InvokeMethodListSelectionListener;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.CancelNextException;
import com.vividsolutions.jump.workbench.ui.wizard.WizardContext;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class WizardGroupDialog extends WizardDialog implements WizardContext,
  InputChangedListener {
  private List<WizardGroup> wizards = new ArrayList<WizardGroup>();

  private JList groupSelectList;

  public WizardGroup currentWizard;

  private WorkbenchContext workbenchContext;

  public WizardGroupDialog(WorkbenchContext workbenchContext, Frame frame,
    String title) {
    super(frame, title, workbenchContext.getErrorHandler());
    this.workbenchContext = workbenchContext;

    initUi();
  }

  private void initUi() {
    Container contentPane = getContentPane();

    groupSelectList = new JList(new DefaultListModel());
    groupSelectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    groupSelectList.setVisibleRowCount(-1);
    groupSelectList.setLayoutOrientation(JList.VERTICAL);
    groupSelectList.setCellRenderer(new WizardGroupListCellRenderer());
    groupSelectList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    groupSelectList.addListSelectionListener(new InvokeMethodListSelectionListener(
      this, "updateSelectedWizard"));

    JScrollPane groupScrollPane = new JScrollPane(groupSelectList);
    contentPane.add(groupScrollPane, BorderLayout.WEST);
  }

  public void addWizard(WizardGroup wizard) {
    DefaultListModel model = (DefaultListModel)groupSelectList.getModel();
    model.addElement(wizard);
  }

  public void addWizards(List<WizardGroup> wizards) {
    for (WizardGroup wizard : wizards) {
      addWizard(wizard);
    }
  }

  public void updateSelectedWizard() {
    WizardGroup selectedWizard = getSelectedWizard();
    if (selectedWizard != null) {
      if (currentWizard != selectedWizard) {
        initSelectedWizard(selectedWizard);
      }
    }
  }

  private void initSelectedWizard(WizardGroup selectedWizard) {
    currentWizard = selectedWizard;
    currentWizard.initialize(workbenchContext, WizardGroupDialog.this);
    setPanels(selectedWizard.getPanels());
    setCurrentWizardPanel(currentWizard.getFirstId());
    pack();
  }

  public WizardGroup getSelectedWizard() {
    return (WizardGroup)groupSelectList.getSelectedValue();
  }

  public void setSelectedWizard(WizardGroup wizardGroup) {
    if (wizardGroup != null) {
      if (wizardGroup == currentWizard) {
        initSelectedWizard(wizardGroup);
      } else {
        groupSelectList.setSelectedValue(wizardGroup, true);
      }
    } else {
      groupSelectList.setSelectedIndex(0);
    }
  }
}
