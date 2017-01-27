package org.openjump.core.ui;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserDialog;
import com.vividsolutions.jump.workbench.datasource.SaveDatasetAsPlugIn;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.OptionsPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.Charset;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoader;

/**
 * OptionsPanel for setting up some dataset options.
 * - should the Charset selection be shown
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class DatasetOptionsPanel extends JPanel implements OptionsPanel {

	// Blackboard keys
	public static final String BB_DATASET_OPTIONS_SHOW_CHARSET_SELECTION = DatasetOptionsPanel.class.getName() + "SHOW_CHARSET_SELECTION";

	private JPanel mainPanel;
	private JPanel fillPanel;
	private JCheckBox charsetSelectionCheckBox;

	private Blackboard blackboard = null;
	private WorkbenchContext context = null;

	public DatasetOptionsPanel(WorkbenchContext context) {
		this.context = context;
		blackboard = PersistentBlackboardPlugIn.get(context);
		initComponents();
	}

	private void initComponents() {
		GridBagConstraints gridBagConstraints;
		mainPanel = new JPanel();
		fillPanel = new JPanel();
		charsetSelectionCheckBox = new JCheckBox(I18N.get("ui.DatasetOptionsPanel.ShowCharsetSelection"));

		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
		mainPanel.setLayout(new GridBagLayout());
		this.add(mainPanel, BorderLayout.CENTER);

		// Charset selection
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(charsetSelectionCheckBox, gridBagConstraints);

		// empty fill Panel for nice layout
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(fillPanel, gridBagConstraints);

	}

	public String validateInput() {
		return null;
	}

	public void okPressed() {
		blackboard.put(BB_DATASET_OPTIONS_SHOW_CHARSET_SELECTION, charsetSelectionCheckBox.isSelected());
		Registry registry = context.getRegistry();
		List loaders = registry.getEntries(FileLayerLoader.KEY);
		for (Object loader : loaders) {
			if (loader instanceof DataSourceFileLayerLoader) {
				DataSourceFileLayerLoader fileLoader = (DataSourceFileLayerLoader)loader;
				if (fileLoader.getDescription().equals("ESRI Shapefile")) {
					fileLoader.removeOption("charset", "CharSetComboBoxField", Charset.defaultCharset().displayName(), true);
					if (charsetSelectionCheckBox.isSelected()) {
                        fileLoader.addOption("charset", "CharSetComboBoxField", Charset.defaultCharset().displayName(), true);
                    }
                    // update the DataSourceQueryChooserDialog for the current format
                    String KEY = SaveDatasetAsPlugIn.class.getName() + " - DIALOG";
                    if (null != context.getWorkbench().getBlackboard().get(KEY)) {
                        DataSourceQueryChooserDialog dialog = (DataSourceQueryChooserDialog) context.getWorkbench().getBlackboard().get(KEY);
                        dialog.showFormat();
                    }
				}
			}
		}
	}

	public void init() {
		// set the checkbox from the Blackboard value
		Object showCharsetSelection = blackboard.get(BB_DATASET_OPTIONS_SHOW_CHARSET_SELECTION);
		if (showCharsetSelection instanceof Boolean) {
			charsetSelectionCheckBox.setSelected(((Boolean) showCharsetSelection).booleanValue());
		} else {
			// or to false, if we do not have an Blackboard value
			charsetSelectionCheckBox.setSelected(false);
		}
	}

}
