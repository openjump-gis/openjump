package org.openjump.swing.list;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class LabelListCellRenderer extends DefaultListCellRenderer {

  public Component getListCellRendererComponent(JList list, Object value,
    int index, boolean isSelected, boolean cellHasFocus) {
    if (value instanceof JLabel) {
      JLabel label = (JLabel)value;
      label.setOpaque(true);

      label.setComponentOrientation(list.getComponentOrientation());

      Color bg = null;
      Color fg = null;

      if (isSelected) {
        label.setBackground(bg == null ? list.getSelectionBackground() : bg);
        label.setForeground(fg == null ? list.getSelectionForeground() : fg);
      } else {
        label.setBackground(list.getBackground());
        label.setForeground(list.getForeground());
      }

      label.setEnabled(list.isEnabled());
      label.setFont(list.getFont());

      Border border = null;
      if (cellHasFocus) {
        if (isSelected) {
          border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
        }
        if (border == null) {
          border = UIManager.getBorder("List.focusCellHighlightBorder");
        }
      } else {
        border = new EmptyBorder(1, 1, 1, 1);
      }
      label.setBorder(border);

      return label;
    } else {
      return super.getListCellRendererComponent(list, value, index, isSelected,
        cellHasFocus);
    }
  }

}
