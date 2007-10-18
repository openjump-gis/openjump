package org.openjump.core.ui.plugin.file.open;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyVetoException;
import java.lang.ref.WeakReference;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import org.openjump.swing.listener.InvokeMethodActionListener;
import org.openjump.swing.listener.InvokeMethodListSelectionListener;
import org.openjump.swing.util.SpringUtilities;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.wizard.AbstractWizardPanel;

public class ChooseProjectPanel extends AbstractWizardPanel {
  private static final long serialVersionUID = -1681453503884354145L;

  private WorkbenchContext workbenchContext;

  private JList projectList;

  private JRadioButton newButton;

  private JRadioButton existingButton;

  private WeakReference<TaskFrame> selectedTaskFrame;

  public ChooseProjectPanel(final WorkbenchContext workbenchContext,
    final String nextId) {
    this.workbenchContext = workbenchContext;
    setNextID(nextId);

    setLayout(new BorderLayout());
    JPanel view = new JPanel(new SpringLayout());

    newButton = new JRadioButton("New Project");
    newButton.setSelected(true);
    view.add(newButton);

    existingButton = new JRadioButton("Existing Project");
    view.add(existingButton);

    ButtonGroup group = new ButtonGroup();
    group.add(newButton);
    group.add(existingButton);

    projectList = new JList(new DefaultListModel());
    JScrollPane listScroller = new JScrollPane(projectList);
    listScroller.setPreferredSize(new Dimension(300, 150));
    view.add(listScroller);

    SpringUtilities.makeCompactGrid(view, 3, 1, 5, 5, 5, 5);

    InvokeMethodActionListener actionListener = new InvokeMethodActionListener(
      this, "updateButtons");
    newButton.addActionListener(actionListener);
    existingButton.addActionListener(actionListener);
    projectList.addListSelectionListener(new InvokeMethodListSelectionListener(
      this, "listSelected", true));
    add(new JScrollPane(view), BorderLayout.CENTER);

  }

  public void enteredFromLeft(Map data) {
    super.enteredFromLeft(data);

    DefaultListModel model = (DefaultListModel)projectList.getModel();
    projectList.setCellRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected,
          cellHasFocus);
        setText(((TaskFrame)value).getTask().getName());
        return this;
      }

    });
    model.removeAllElements();
    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame workbenchFrame = workbench.getFrame();
    JDesktopPane desktopPane = workbenchFrame.getDesktopPane();
    for (JInternalFrame frame : desktopPane.getAllFrames()) {
      if (frame instanceof TaskFrame) {
        TaskFrame taskFrame = (TaskFrame)frame;
        model.addElement(taskFrame);
      }

    }
  }

  public void exitingToRight() throws Exception {
    TaskFrame taskFrame = (TaskFrame)projectList.getSelectedValue();
    if (taskFrame != null) {
      selectedTaskFrame = new WeakReference<TaskFrame>(taskFrame);
    } else {
      selectedTaskFrame = null;
    }
    DefaultListModel model = (DefaultListModel)projectList.getModel();
    model.removeAllElements();
  }

  public boolean hasActiveTaskFrame() {
    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame workbenchFrame = workbench.getFrame();
    JInternalFrame activeFrame = workbenchFrame.getActiveInternalFrame();
    return activeFrame instanceof TaskFrame && !activeFrame.isIcon();
  }

  public boolean hasTaskFrames() {
    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame workbenchFrame = workbench.getFrame();
    JDesktopPane desktopPane = workbenchFrame.getDesktopPane();
    for (JInternalFrame frame : desktopPane.getAllFrames()) {
      if (frame instanceof TaskFrame) {
        return true;
      }
    }
    return false;
  }

  public void setNextID(String nextId) {
    super.setNextID(nextId);
  }

  public void updateButtons() {
    if (newButton.isSelected()) {
      projectList.getSelectionModel().clearSelection();
    } else {
      if (projectList.getSelectedIndex() == -1) {
        projectList.setSelectedIndex(0);
      }
    }
  }

  public void listSelected() {
    if (projectList.getSelectedIndex() == -1) {
      newButton.setSelected(true);
    } else {
      existingButton.setSelected(true);
    }
  }

  public void activateSelectedProject() {
    if (!hasActiveTaskFrame()) {
      JUMPWorkbench workbench = workbenchContext.getWorkbench();
      final WorkbenchFrame frame = workbench.getFrame();
      if (selectedTaskFrame == null) {
        frame.addTaskFrame();
      } else {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            TaskFrame taskFrame = selectedTaskFrame.get();
            try {
              taskFrame.setSelected(true);
            } catch (PropertyVetoException e) {
            }
            if (taskFrame.isIcon()) {
              try {
                taskFrame.setIcon(false);
              } catch (PropertyVetoException e) {
              }
            }
          }
        });
      }
    }
  }
}
