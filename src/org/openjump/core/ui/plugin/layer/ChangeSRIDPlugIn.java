package org.openjump.core.ui.plugin.layer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openjump.core.ccordsys.srid.SRIDStyle;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class ChangeSRIDPlugIn extends AbstractPlugIn {
  public void initialize(PlugInContext context) throws Exception {
    EnableCheckFactory enableCheckFactory = new EnableCheckFactory(context.getWorkbenchContext());
    EnableCheck enableCheck = new MultiEnableCheck()
        .add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck())
        .add(enableCheckFactory.createExactlyNLayersMustBeSelectedCheck(1));
    new FeatureInstaller(context.getWorkbenchContext()).addMainMenuPlugin(this, new String[] { MenuNames.LAYER },
        getName() + "...", false, null, enableCheck);
  }

  public String getName() {
    return I18N.get("org.openjump.core.ui.plugin.layer.ChangeSRIDPlugIn.Change-SRID");
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    final Layer layer = context.getSelectedLayer(0);
    final SRIDStyle sridStyle = (SRIDStyle) layer.getStyle(SRIDStyle.class);
    final int oldSRID = sridStyle.getSRID();
    String input = "";

    final JButton okay = new JButton(I18N.get("ui.OKCancelPanel.ok"));
    okay.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JOptionPane pane = getOptionPane((JComponent) e.getSource());
        pane.setValue(okay);
      }
    });
    okay.setEnabled(false);
    final JButton cancel = new JButton(I18N.get("ui.OKCancelPanel.cancel"));
    cancel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JOptionPane pane = getOptionPane((JComponent) e.getSource());
        pane.setValue(cancel);
      }
    });

    final JTextField field = new JTextField("0123456789");
    // add some padding
    Border padding = BorderFactory.createEmptyBorder(1, 3, 1, 3);
    final Border defaultBorder = BorderFactory.createCompoundBorder(field.getBorder(), padding);
    final Border redBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red), padding);
    field.setBorder(defaultBorder);
    field.setText("" + oldSRID);
    // switch ok button on/off
    field.getDocument().addDocumentListener(new DocumentListener() {
      protected void update() {
        String value = field.getText().trim();
        boolean changed = !value.equals("" + oldSRID);
        boolean valid = value.matches("\\d+");
        field.setBorder(valid ? defaultBorder : redBorder);
        okay.setEnabled(changed && valid);
      }
      public void insertUpdate(DocumentEvent e) {
        update();
      }
      public void removeUpdate(DocumentEvent e) {
        update();
      }
      public void changedUpdate(DocumentEvent e) {
        update();
      }
    });

    int res = JOptionPane.showOptionDialog(context.getWorkbenchFrame(), field, getName(), JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE, null, new Object[] { okay, cancel }, okay);
    // do nothing if cancelled
    if (res != 0) {
      return false;
    }

    input = field.getText().trim();
    final int newSRID = Integer.parseInt(input);

    // no change, nothing to do
    if (newSRID == oldSRID) {
      return false;
    }

    execute(new UndoableCommand(getName()) {
      public void execute() {
        sridStyle.setSRID(newSRID);
        sridStyle.updateSRIDs(layer);
        layer.setFeatureCollectionModified(true);
      }

      public void unexecute() {
        sridStyle.setSRID(oldSRID);
        sridStyle.updateSRIDs(layer);
        layer.setFeatureCollectionModified(true);
      }
    }, context);
    return true;
  }

  // find the parent JOptionPane of a given JComponent
  private JOptionPane getOptionPane(JComponent parent) {
    JOptionPane pane = null;
    if (!(parent instanceof JOptionPane)) {
      pane = getOptionPane((JComponent) parent.getParent());
    } else {
      pane = (JOptionPane) parent;
    }
    return pane;
  }
}
