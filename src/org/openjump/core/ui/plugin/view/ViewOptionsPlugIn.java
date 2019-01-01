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
import org.apache.batik.ext.swing.GridBagConstants;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;

/**
 * Plugin responsible for initializing/editing view options
 */
public class ViewOptionsPlugIn extends AbstractPlugIn {



    public static final String DATE_FORMAT_KEY = ViewOptionsPlugIn.class.getName() + " - DATE_FORMAT_KEY";

    public static final String SELECTION_SYNC_KEY = ViewOptionsPlugIn.class.getName() + " - SELECTION_SYNCHRONIZATION";

    private static final String VIEW_OPTIONS = I18N.get("org.openjump.core.ui.plugin.view.ViewOptionsPlugIn");

    private static final String ATTRIBUTES_FORMAT = I18N.get("org.openjump.core.ui.plugin.view.ViewOptionsPlugIn.Attributes-format-in-attribute-table");
    private static final String DATE_FORMAT = I18N.get("org.openjump.core.ui.plugin.view.ViewOptionsPlugIn.Date-format");
    private JComboBox dateFormatChooser;

    private static final String SELECTION_SYNCHRONIZATION = I18N.get("org.openjump.core.ui.plugin.view.ViewOptionsPlugIn.Selection-synchronization");
    private static final String SYNC = I18N.get("org.openjump.core.ui.plugin.view.ViewOptionsPlugIn.Synchronize");
    private JCheckBox synchronizationCheckBox;

    private Blackboard blackBoard;

    public void initialize(final PlugInContext context) throws Exception {

        blackBoard = PersistentBlackboardPlugIn.get(context.getWorkbenchContext());

        ViewOptionsPanel viewOptionsPanel = new ViewOptionsPanel(context);
        viewOptionsPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

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

        synchronizationCheckBox = new JCheckBox();

        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 10;
        c.ipady = 10;
        c.weighty = 0;

        c.gridx = 0;
        c.gridwidth = 2;
        viewOptionsPanel.add(new JPanel(), c);
        c.gridy++;

        c.gridwidth = 2;
        c.anchor = GridBagConstants.WEST;
        viewOptionsPanel.add(new JLabel(ATTRIBUTES_FORMAT), c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstants.EAST;
        viewOptionsPanel.add(new JLabel(DATE_FORMAT), c);

        c.gridx = 1;
        c.anchor = GridBagConstants.WEST;
        viewOptionsPanel.add(dateFormatChooser, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 2;
        viewOptionsPanel.add(new JPanel(), c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstants.WEST;
        viewOptionsPanel.add(new JLabel(SELECTION_SYNCHRONIZATION), c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstants.EAST;
        viewOptionsPanel.add(new JLabel(SYNC), c);
        c.gridx = 1;
        c.anchor = GridBagConstants.WEST;
        viewOptionsPanel.add(synchronizationCheckBox, c);
        c.gridy++;

        c.gridx = 0;
        c.weighty = 1;
        c.fill = GridBagConstants.VERTICAL;
        viewOptionsPanel.add(new JPanel(), c);

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
            //Blackboard blackBoard = PersistentBlackboardPlugIn.get(context.getWorkbenchContext());
            blackBoard.put(DATE_FORMAT_KEY, dateFormatChooser.getSelectedItem().toString());
            JInternalFrame[] frames = context.getWorkbenchFrame().getInternalFrames();
            for (JInternalFrame frame : frames) {
                if (frame instanceof InfoFrame || frame instanceof ViewAttributesPlugIn.ViewAttributesFrame) {
                    frame.repaint();
                }
            }
            blackBoard.put(SELECTION_SYNC_KEY, synchronizationCheckBox.isSelected());
        }

        public void init() {
            // Init formatter from the Workbench.xml configuration file
            Object persistedFormat = blackBoard.get(DATE_FORMAT_KEY);
            if (persistedFormat != null) {
                dateFormatChooser.setSelectedItem(
                        PersistentBlackboardPlugIn
                                .get(context.getWorkbenchContext())
                                .get(DATE_FORMAT_KEY));
            } else {
                dateFormatChooser.setSelectedIndex(0);
            }
            Object sync = blackBoard.get(SELECTION_SYNC_KEY);
            if (sync != null) {
                synchronizationCheckBox.setSelected(
                        Boolean.parseBoolean(sync.toString()));
            } else {
                synchronizationCheckBox.setSelected(true);
            }
        }
    }
}
