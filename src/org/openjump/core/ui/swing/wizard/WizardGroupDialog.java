package org.openjump.core.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openjump.swing.listener.InvokeMethodListSelectionListener;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
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

  /**
   * @override this method to return all "contained" ui components for
   * SkinOptinsPanel to SwingUtilities.updateComponentTreeUI() them after
   * L&F change [ede 05.2012]
   */
  public Component[] getComponents() {
    DefaultListModel model = (DefaultListModel)groupSelectList.getModel();
    ArrayList components = new ArrayList<Component>();
    components.addAll(Arrays.asList(getContentPane().getComponents()));
    for (Object wo : model.toArray()) {
      WizardGroup wg = (WizardGroup) wo;
      for (WizardPanel wp : wg.getPanels()) {
        //System.out.println("add "+wp.getID()+" ->"+(wp instanceof Component));
        if (wp instanceof Component)
          components.add((Component)wp);
      }
    }
    return (Component[])components.toArray(new Component[]{});
  }
  
}
