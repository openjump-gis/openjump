package org.openjump.core.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class WrappingWizardGroupListCellRenderer extends DefaultListCellRenderer {
  JPanel selectionPanel = new JPanel(new BorderLayout());
  JPanel wrappingPanel = new JPanel(new BorderLayout());
  JLabel label = new JLabel();

  public WrappingWizardGroupListCellRenderer() {
    super();
    label.setVerticalTextPosition(BOTTOM);
    label.setHorizontalAlignment(CENTER);
    label.setHorizontalTextPosition(CENTER);
    selectionPanel.add(Box.createVerticalStrut(2),BorderLayout.NORTH);
    selectionPanel.add(label,BorderLayout.CENTER);
    selectionPanel.add(Box.createVerticalStrut(2),BorderLayout.SOUTH);
    
    wrappingPanel.add(selectionPanel);
    wrappingPanel.add(Box.createVerticalStrut(2),BorderLayout.SOUTH);
  }

  public Component getListCellRendererComponent(JList list, Object value,
      int index, boolean isSelected, boolean cellHasFocus) {

    super.getListCellRendererComponent(list, value, index, isSelected,
        cellHasFocus);

    WizardGroup wizard = (WizardGroup) value;
    label.setText("<html><center><p style='width:100px'>" + GUIUtil.escapeHTML(wizard.getName()) +"</p></center><html>");
    label.setIcon(wizard.getIcon());
    label.setForeground(getForeground());
    label.setBackground(getBackground());
    
    selectionPanel.setBorder(getBorder());
    selectionPanel.setForeground(getForeground());
    selectionPanel.setBackground(getBackground());

    wrappingPanel.setBackground(list.getBackground());
    return wrappingPanel;
  }

}