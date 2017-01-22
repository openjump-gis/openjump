package org.openjump.core.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.openjump.swing.listener.InvokeMethodListSelectionListener;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardContext;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

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
    groupSelectList = new JList(new DefaultListModel());
    groupSelectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    groupSelectList.setVisibleRowCount(-1);
    // alternate way to force a specific width, currently unused
    // groupSelectList.setFixedCellWidth(100);
    groupSelectList.setLayoutOrientation(JList.VERTICAL);
    // let's wrap long description via custom renderer
    ListCellRenderer renderer = new WrappingWizardGroupListCellRenderer();
    groupSelectList.setCellRenderer(renderer);
    groupSelectList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    groupSelectList.addListSelectionListener(new InvokeMethodListSelectionListener(
      this, "updateSelectedWizard"));

    JScrollPane groupScrollPane = new JScrollPane();
    groupScrollPane.setViewportView(groupSelectList);
    // disable horizontal scrolling
    groupScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    add(groupScrollPane, BorderLayout.WEST);
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

  public int getWizardCount(){
    return groupSelectList.getModel().getSize();
  }

  public WizardGroup getWizardAt(int i){
    return (WizardGroup) groupSelectList.getModel().getElementAt(i);
  }

//  /**
//   * @override this method to return all "contained" ui components for
//   * SkinOptinsPanel to SwingUtilities.updateComponentTreeUI() them after
//   * L&F change [ede 05.2012]
//   * [mmichaud 2012-07-01] removed in a try to fix bug 3528917
//   * [ede 2012-07-01] fix bug 3528917 by adding super.getComponents()
//   * [ede 2012-08-10] commented again to prevent left column flicker during dialog resize
//   *                  skin seems to be propagated properly since java7 if switched
//   */
//  public Component[] getComponents() {
//    // get parent components, linkedhashset is an ordered unique list
//    LinkedHashSet components = new LinkedHashSet(Arrays.asList(super.getComponents()));
//    // add all additional components and panels 
//    DefaultListModel model = (DefaultListModel)groupSelectList.getModel();
//    components.addAll(Arrays.asList(getContentPane().getComponents()));
//    for (Object wo : model.toArray()) {
//      WizardGroup wg = (WizardGroup) wo;
//      for (WizardPanel wp : wg.getPanels()) {
//        //System.out.println("add "+wp.getID()+" ->"+(wp instanceof Component));
//        if (wp instanceof Component)
//          components.add((Component)wp);
//      }
//    }
//    return (Component[])components.toArray(new Component[]{});
//  }
  
}
