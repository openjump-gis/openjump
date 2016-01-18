package org.openjump.core.ui.plugin.view;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.InfoFrame;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;
import com.vividsolutions.jump.workbench.ui.OptionsPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ViewAttributesPlugIn;

import javax.swing.*;
import java.text.SimpleDateFormat;

/**
 * Plugin responsible for initializing/editing view options
 */
public class ViewOptionsPlugIn extends AbstractPlugIn {

    private static String VIEW_OPTIONS = I18N.get("org.openjump.core.ui.plugin.view.ViewOptionsPlugIn");

    public static final String DATE_FORMAT_KEY = ViewOptionsPlugIn.class.getName() + " - DATE_FORMAT_KEY";

    private static final String DATE_FORMAT = I18N.get("org.openjump.core.ui.plugin.view.ViewOptionsPlugIn.Date-format");
    private JComboBox dateFormatChooser;

    public void initialize(final PlugInContext context) throws Exception {

        ViewOptionsPanel viewOptionsPanel = new ViewOptionsPanel(context);

        // Most useful formats for english and european speakers
        dateFormatChooser = new JComboBox(new String[]{
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd",
                "hh:mm:ss",
                "dd/MM/yyyy",
                "dd/MM/yyyy HH:mm:ss"
        });
        dateFormatChooser.setEnabled(true);
        dateFormatChooser.setEditable(true);

        viewOptionsPanel.add(new JLabel(DATE_FORMAT));
        viewOptionsPanel.add(dateFormatChooser);

        OptionsDialog.instance(context.getWorkbenchContext().getWorkbench())
                .addTab(VIEW_OPTIONS, viewOptionsPanel);
    }

    class ViewOptionsPanel extends JPanel implements OptionsPanel {

        PlugInContext context;

        ViewOptionsPanel(final PlugInContext context) {
            this.context = context;
        }

        public String validateInput() {
            try {
                new SimpleDateFormat(dateFormatChooser.getSelectedItem().toString());
                return null;
            } catch(IllegalArgumentException e) {
                return e.getMessage();
            }
        }

        public void okPressed() {
            // If ok pressed,save the format in Workbench.xml configuration file
            Blackboard blackBoard = PersistentBlackboardPlugIn.get(context.getWorkbenchContext());
            blackBoard.put(DATE_FORMAT_KEY, dateFormatChooser.getSelectedItem().toString());
            JInternalFrame[] frames = context.getWorkbenchFrame().getInternalFrames();
            for (JInternalFrame frame : frames) {
                if (frame instanceof InfoFrame || frame instanceof ViewAttributesPlugIn.ViewAttributesFrame) {
                    frame.repaint();
                }
            }
        }

        public void init() {
            // Init formatter from the Workbench.xml configuration file
            Object persistedFormat = PersistentBlackboardPlugIn
                    .get(context.getWorkbenchContext())
                    .get(DATE_FORMAT_KEY);
            if (persistedFormat != null) {
                dateFormatChooser.setSelectedItem(
                        PersistentBlackboardPlugIn
                                .get(context.getWorkbenchContext())
                                .get(DATE_FORMAT_KEY));
            } else {
                dateFormatChooser.setSelectedIndex(0);
            }
        }
    }
}
