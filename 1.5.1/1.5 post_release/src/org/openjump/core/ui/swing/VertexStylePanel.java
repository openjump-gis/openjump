package org.openjump.core.ui.swing;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.ColorChooserPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import de.latlon.deejump.plugin.style.VertexStylesFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

/**
 * A JPanel for styling a vertex. The stylings you can apply to the
 * {@linkplain de.latlon.deejump.plugin.style.VertexStylesFactory VertexStylesFactory}.
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class VertexStylePanel extends JPanel implements ListCellRenderer {

	// Default values
	/**
	 * Default vertex Color is yellow.
	 */
	public static final Color DEFAULT_VERTEX_COLOR = Color.red;
	/**
	 * Default vertex form is a square.
	 */
	public static final String DEFAULT_VERTEX_FORM = VertexStylesFactory.SQUARE_STYLE;
	/**
	 * Default vertex size is 5 points.
	 */
	public static final Integer DEFAULT_VERTEX_SIZE = 5;

	private JPanel mainPanel;
    private ColorChooserPanel vertexColorChooserPanel;
    private JLabel vertexColorLabel;
    private JComboBox vertexStyleComboBox;
    private JLabel vertexStyleLabel;
	private JLabel vertexSizeLabel;
	private JSlider vertexSizeSlider;
	private JPanel fillPanel;
	private JButton restoreDefaultsButton;
	private boolean displayRestoreDefaultsButton = true;

	/**
	 * Creates a new panel.
	 */
	public VertexStylePanel() {
		super();
		initComponents();
	}

	/**
	 * Creates a new panel.
	 *
	 * @param displayRestoreDefaultsButton - should the restore defaults button be displayed?
	 */
	public VertexStylePanel(boolean displayRestoreDefaultsButton) {
		super();
		this.displayRestoreDefaultsButton = displayRestoreDefaultsButton;
		initComponents();
		setVertexColor(DEFAULT_VERTEX_COLOR);
		setVertexForm(DEFAULT_VERTEX_FORM);
		setVertexSize(DEFAULT_VERTEX_SIZE);
	}

	/**
	 * Gets the vertex color.
	 * @return the vertex color
	 */
	public Color getVertexColor() {
		return vertexColorChooserPanel.getColor();
	}

	/**
	 * Gets the vertex form as a Sting.
	 * @return the vertex form.
	 */
	public String getVertexForm() {
		return ((String[])vertexStyleComboBox.getSelectedItem())[1];
	}

	/**
	 * Gets the vertex size.
	 * @return the vertex size.
	 */
	public int getVertexSize() {
		return vertexSizeSlider.getValue();
	}

	/**
	 * Sets the vertex color.
	 */
	public void setVertexColor(Color color) {
		vertexColorChooserPanel.setColor(color);
	}

	/**
	 * Sets the vertex form. For possible forms please see
	 * {@linkplain de.latlon.deejump.plugin.style.VertexStylesFactory VertexStylesFactory}
	 * constants.
	 */
	public void setVertexForm(String form) {
		// select the right item in the JCombobox
		int count = vertexStyleComboBox.getItemCount();
		for (int i = 0; i < count; i++) {
			String[] item = (String[]) vertexStyleComboBox.getItemAt(i);
			if (item[1].equals(form)) {
				vertexStyleComboBox.setSelectedIndex(i);
				break;
			}
		}

	}

	/**
	 * Sets the vertex Size.
	 */
	public void setVertexSize(int size) {
		vertexSizeSlider.setValue(size);
	}

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

		mainPanel = new JPanel();
        vertexColorLabel = new JLabel();
        vertexStyleLabel = new JLabel();
        vertexColorChooserPanel = new ColorChooserPanel();
		restoreDefaultsButton = new JButton();

		restoreDefaultsButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				vertexColorChooserPanel.setColor(DEFAULT_VERTEX_COLOR);
				vertexSizeSlider.setValue(DEFAULT_VERTEX_SIZE);
				// select the default item in the pointStyleComboBox
				int count = vertexStyleComboBox.getItemCount();
				for (int i = 0; i < count; i++) {
					String[] item = (String[]) vertexStyleComboBox.getItemAt(i);
					if (item[1].equals(DEFAULT_VERTEX_FORM)) {
						vertexStyleComboBox.setSelectedIndex(i);
						break;
					}
				}

			}
		});

		vertexColorChooserPanel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ColorChooserPanel ccp = (ColorChooserPanel) e.getSource();
				ccp.setAlpha(255);
			}
		});
		vertexColorChooserPanel.setAlpha(255);
        vertexStyleComboBox = new javax.swing.JComboBox();
		vertexStyleComboBox.setRenderer(this);
		vertexStyleComboBox.setEditable(false);
		// an item is a String Array, index 0 is the Text in the ComboBox and index 1 is the VertexStyle
		vertexStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.square"), VertexStylesFactory.SQUARE_STYLE});
        vertexStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.circle"), VertexStylesFactory.CIRCLE_STYLE});
        vertexStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.triangle"), VertexStylesFactory.TRIANGLE_STYLE});
        vertexStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.cross"), VertexStylesFactory.CROSS_STYLE});
        vertexStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.star"), VertexStylesFactory.STAR_STYLE});
		vertexSizeLabel = new JLabel();
		vertexSizeSlider = new JSlider();
		fillPanel = new JPanel();

		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
		mainPanel.setLayout(new GridBagLayout());
		this.add(mainPanel, BorderLayout.CENTER);

		// Vertexcolor
        vertexColorLabel.setText(I18N.get("org.openjump.core.ui.swing.VertexStylePanel.vertex-color"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(vertexColorLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(vertexColorChooserPanel, gridBagConstraints);

		// Vertexform
        vertexStyleLabel.setText(I18N.get("org.openjump.core.ui.swing.VertexStylePanel.vertex-style"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(vertexStyleLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(vertexStyleComboBox, gridBagConstraints);

		// Vertexsize
        vertexSizeLabel.setText(I18N.get("org.openjump.core.ui.swing.VertexStylePanel.vertexsize"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(vertexSizeLabel, gridBagConstraints);

        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(1), new JLabel("1"));
        labelTable.put(new Integer(5), new JLabel("5"));
        labelTable.put(new Integer(10), new JLabel("10"));
        labelTable.put(new Integer(15), new JLabel("15"));
        labelTable.put(new Integer(20), new JLabel("20"));
		vertexSizeSlider.setLabelTable(labelTable);
		vertexSizeSlider.setMinorTickSpacing(1);
		vertexSizeSlider.setMajorTickSpacing(0);
		vertexSizeSlider.setPaintLabels(true);
		vertexSizeSlider.setMinimum(1);
		vertexSizeSlider.setValue(2);
		vertexSizeSlider.setMaximum(20);
		vertexSizeSlider.setSnapToTicks(true);
		vertexSizeSlider.setPreferredSize(new Dimension(130, 49));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(vertexSizeSlider, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
		mainPanel.add(GUIUtil.createSyncdTextField(vertexSizeSlider, 3), gridBagConstraints);

		// empty fill Panel for nice layout
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(fillPanel, gridBagConstraints);

		// Button "Restore default settings"
		if (displayRestoreDefaultsButton) {
		restoreDefaultsButton.setText(I18N.get("org.openjump.core.ui.swing.VertexStylePanel.RestoreDefaultsSettings"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 4;
			gridBagConstraints.gridwidth = 2;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
			mainPanel.add(restoreDefaultsButton, gridBagConstraints);
		}

    }

	/**
	 * This is the ListCellRenderer for the vertexStyleComboBox, because the
	 * items are String arrays. So we need an own renderer, that displays
	 * the first index of the array.
	 *
	 * @param list
	 * @param value
	 * @param index
	 * @param isSelected
	 * @param cellHasFocus
	 * @return the label representing the first vertex style of the list
	 */
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		JLabel label = new JLabel(((String[]) value)[0]);
		label.setOpaque(true);
		if (isSelected) {
			label.setBackground(new Color(163, 184, 204)); // may be the original Color of a JComboBox
		}
		return label;
	}


}
