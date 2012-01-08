package org.openjump.core.ui.plugin.file.open;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openjump.core.ui.swing.wizard.WizardGroup;
import org.openjump.core.ui.swing.wizard.WizardGroupListCellRenderer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class OpenDataTypePanel extends JPanel implements WizardPanel {
  public static final String KEY = OpenDataTypePanel.class.getName();

  private static final String TITLE = I18N.get(KEY);

  private static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  private JList list;

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  public OpenDataTypePanel(final WorkbenchContext workbenchContext,
    final WizardDialog dialog, final List<WizardGroup> wizards) {
    super(new BorderLayout());
    list = new JList(wizards.toArray());
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setVisibleRowCount(-1);
    list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    list.setCellRenderer(new WizardGroupListCellRenderer());
    list.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    list.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        for (InputChangedListener listener : listeners) {
          listener.inputChanged();
        }

      }
    });
    list.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int index = list.locationToIndex(e.getPoint());
          if (index != -1) {
            Rectangle bounds = list.getCellBounds(index, index);
            if (bounds.contains(e.getPoint())) {
              dialog.next();
            }
          }
        }
      }
    });
    JScrollPane scrollPane = new JScrollPane(list);
    add(scrollPane, BorderLayout.CENTER);
  }

  public void add(InputChangedListener listener) {
    listeners.add(listener);
  }

  public void enteredFromLeft(Map dataMap) {
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return KEY;
  }

  public String getInstructions() {
    return INSTRUCTIONS;
  }

  public String getNextID() {
    WizardGroup wizard = getSlectedWizardGroup();
    if (wizard != null) {
      return wizard.getFirstId();
    } else {
      return null;
    }
  }

  public WizardGroup getSlectedWizardGroup() {
    if (list.isSelectionEmpty()) {
      return null;
    } else {
      return (WizardGroup)list.getSelectedValue();
    }
  }

  public String getTitle() {
    return TITLE;
  }

  public boolean isInputValid() {
    return !list.isSelectionEmpty();
  }

  public void remove(InputChangedListener listener) {
    listeners.remove(listener);
  }

}
