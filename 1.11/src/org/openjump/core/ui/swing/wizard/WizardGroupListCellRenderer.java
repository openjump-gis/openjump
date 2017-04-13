package org.openjump.core.ui.swing.wizard;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

public class WizardGroupListCellRenderer extends DefaultListCellRenderer {

  public Component getListCellRendererComponent(JList list, Object value,
    int index, boolean isSelected, boolean cellHasFocus) {
    setVerticalAlignment(JLabel.BOTTOM);
    setVerticalTextPosition(JLabel.BOTTOM);
    setHorizontalAlignment(JLabel.CENTER);
    setHorizontalTextPosition(JLabel.CENTER);
    super.getListCellRendererComponent(list, value, index, isSelected,
      cellHasFocus);
    if (value instanceof WizardGroup) {
      WizardGroup wizard = (WizardGroup)value;
      setText(wizard.getName());
      setIcon(wizard.getIcon());
    }
    setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    return this;
  }

}
