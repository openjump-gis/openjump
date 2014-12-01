package org.openjump.core.ui;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.ColorChooserPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.OptionsPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.AbstractSelectionRenderer;
import com.vividsolutions.jump.workbench.ui.renderer.FeatureSelectionRenderer;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * OptionsPanel for setting up the size, color and the from of selected features.
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class SelectionStyllingOptionsPanel extends JPanel implements OptionsPanel {

	// Blackboard keys
	public static final String BB_SELECTION_STYLE_COLOR = SelectionStyllingOptionsPanel.class.getName() + " SELECTION_STYLE_COLOR";
	public static final String BB_SELECTION_STYLE_FILL_COLOR = SelectionStyllingOptionsPanel.class.getName() + " SELECTION_STYLE_FILL_COLOR";
	public static final String BB_SELECTION_STYLE_FILL_OPACITY = SelectionStyllingOptionsPanel.class.getName() + " SELECTION_STYLE_FILL_OPACITY";
	public static final String BB_SELECTION_STYLE_POINT_FORM = SelectionStyllingOptionsPanel.class.getName() + " SELECTION_STYLE_POINT_FORM";
	public static final String BB_SELECTION_STYLE_POINT_SIZE = SelectionStyllingOptionsPanel.class.getName() + " SELECTION_STYLE_POINT_SIZE";
	

	// Default values
	public static final Color DEFAULT_SELECTION_STYLE_COLOR = Color.yellow;
	public static final Color DEFAULT_SELECTION_STYLE_FILL_COLOR = GUIUtil.alphaColor(Color.white, 75);
	public static final int DEFAULT_SELECTION_STYLE_FILL_OPACITY = 75;
	public static final String DEFAULT_SELECTION_STYLE_POINT_FORM = VertexStylesFactory.SQUARE_STYLE;
	public static final Integer DEFAULT_SELECTION_STYLE_POINT_SIZE = 5;
	

	private JPanel mainPanel;
    private JLabel lineColorLabel;
    private ColorChooserPanel lineColorChooserPanel;
    private JLabel fillColorLabel;
    private ColorChooserPanel fillColorChooserPanel;
    private JLabel fillOpacityLabel;
    private JSlider fillOpacitySlider;
    private JComboBox pointStyleComboBox;
    private JLabel pointStyleLabel;
	private JLabel pointSizeLabel;
	private JSlider pointSizeSlider;
	private JPanel fillPanel;
	private JButton restoreDefaultsButton;

	private Blackboard blackboard = null;
	private WorkbenchContext context = null;

	public SelectionStyllingOptionsPanel(WorkbenchContext context) {
		this.context = context;
		blackboard = PersistentBlackboardPlugIn.get(context);
		initComponents();
	}

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

		mainPanel = new JPanel();
		
        lineColorLabel = new JLabel();
        lineColorChooserPanel = new ColorChooserPanel();
        
        fillColorLabel = new JLabel();
        fillColorChooserPanel = new ColorChooserPanel();
        fillOpacityLabel = new JLabel();
        fillOpacitySlider = new JSlider();
        
        pointStyleLabel = new JLabel();
        
        restoreDefaultsButton = new JButton();

		restoreDefaultsButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				lineColorChooserPanel.setColor(DEFAULT_SELECTION_STYLE_COLOR);
				fillColorChooserPanel.setColor(DEFAULT_SELECTION_STYLE_FILL_COLOR);
				fillOpacitySlider.setValue(DEFAULT_SELECTION_STYLE_FILL_OPACITY);
				pointSizeSlider.setValue(DEFAULT_SELECTION_STYLE_POINT_SIZE);
				// select the default item in the pointStyleComboBox
				int count = pointStyleComboBox.getItemCount();
				for (int i = 0; i < count; i++) {
					String[] item = (String[]) pointStyleComboBox.getItemAt(i);
					if (item[1].equals(DEFAULT_SELECTION_STYLE_POINT_FORM)) {
						pointStyleComboBox.setSelectedIndex(i);
						break;
					}
				}

			}
		});

		lineColorChooserPanel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ColorChooserPanel ccp = (ColorChooserPanel) e.getSource();
				ccp.setAlpha(255);
			}
		});
		lineColorChooserPanel.setAlpha(255);
		
		fillColorChooserPanel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ColorChooserPanel ccp = (ColorChooserPanel) e.getSource();
				ccp.setAlpha(fillOpacitySlider.getValue());
			}
		});
		
		fillOpacitySlider.addChangeListener(new ChangeListener() {
		    
		    public void stateChanged(ChangeEvent e) {
				fillColorChooserPanel.setAlpha(((JSlider)e.getSource()).getValue());
			}
		});
		
        pointStyleComboBox = new javax.swing.JComboBox();
		pointStyleComboBox.setRenderer(new ComboRenderer());
		pointStyleComboBox.setEditable(false);
		// an item is a String Array, index 0 is the Text in the ComboBox and index 1 is the VertexStyle
		pointStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.square"), VertexStylesFactory.SQUARE_STYLE});
        pointStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.circle"), VertexStylesFactory.CIRCLE_STYLE});
        pointStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.triangle"), VertexStylesFactory.TRIANGLE_STYLE});
        pointStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.cross"), VertexStylesFactory.CROSS_STYLE});
        pointStyleComboBox.addItem(new String[] {I18N.get("deejump.ui.style.RenderingStylePanel.star"), VertexStylesFactory.STAR_STYLE});
		pointSizeLabel = new JLabel();
		pointSizeSlider = new JSlider();
		fillPanel = new JPanel();

		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
		mainPanel.setLayout(new GridBagLayout());
		this.add(mainPanel, BorderLayout.CENTER);

		// Linecolor
        lineColorLabel.setText(I18N.get("ui.SelectionStyllingOptionsPanel.LineColor"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(lineColorLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(lineColorChooserPanel, gridBagConstraints);
        
        // Fillcolor
        fillColorLabel.setText(I18N.get("ui.SelectionStyllingOptionsPanel.FillColor"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(fillColorLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(fillColorChooserPanel, gridBagConstraints);
        
        // Opacity
        fillOpacityLabel.setText(I18N.get("ui.SelectionStyllingOptionsPanel.FillOpacity"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(fillOpacityLabel, gridBagConstraints);

        Hashtable opLabelTable = new Hashtable();
        opLabelTable.put(new Integer(0), new JLabel("0"));
        //opLabelTable.put(new Integer(32), new JLabel("32"));
        opLabelTable.put(new Integer(64), new JLabel("64"));
        //opLabelTable.put(new Integer(96), new JLabel("96"));
        opLabelTable.put(new Integer(128), new JLabel("128"));
        //opLabelTable.put(new Integer(160), new JLabel("160"));
        opLabelTable.put(new Integer(192), new JLabel("192"));
        //opLabelTable.put(new Integer(224), new JLabel("224"));
        opLabelTable.put(new Integer(255), new JLabel("255"));
		fillOpacitySlider.setLabelTable(opLabelTable);
		fillOpacitySlider.setMinorTickSpacing(1);
		fillOpacitySlider.setMajorTickSpacing(0);
		fillOpacitySlider.setPaintLabels(true);
		fillOpacitySlider.setMinimum(0);
		fillOpacitySlider.setValue(75);
		fillOpacitySlider.setMaximum(255);
		fillOpacitySlider.setSnapToTicks(false);
		fillOpacitySlider.setPreferredSize(new Dimension(130, 49));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(fillOpacitySlider, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
		mainPanel.add(GUIUtil.createSyncdTextField(fillOpacitySlider, 3), gridBagConstraints);

		// Pointform
        pointStyleLabel.setText(I18N.get("ui.SelectionStyllingOptionsPanel.PointStyle"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(pointStyleLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(pointStyleComboBox, gridBagConstraints);

		// Pointsize
        pointSizeLabel.setText(I18N.get("ui.SelectionStyllingOptionsPanel.PointSize"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(pointSizeLabel, gridBagConstraints);

        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(1), new JLabel("1"));
        labelTable.put(new Integer(5), new JLabel("5"));
        labelTable.put(new Integer(10), new JLabel("10"));
        labelTable.put(new Integer(15), new JLabel("15"));
        labelTable.put(new Integer(20), new JLabel("20"));
		pointSizeSlider.setLabelTable(labelTable);
		pointSizeSlider.setMinorTickSpacing(1);
		pointSizeSlider.setMajorTickSpacing(0);
		pointSizeSlider.setPaintLabels(true);
		pointSizeSlider.setMinimum(1);
		pointSizeSlider.setValue(2);
		pointSizeSlider.setMaximum(20);
		pointSizeSlider.setSnapToTicks(true);
		pointSizeSlider.setPreferredSize(new Dimension(130, 49));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        mainPanel.add(pointSizeSlider, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
		mainPanel.add(GUIUtil.createSyncdTextField(pointSizeSlider, 3), gridBagConstraints);

		// empty fill Panel for nice layout
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(fillPanel, gridBagConstraints);

		// Button "Restore default settings"
		restoreDefaultsButton.setText(I18N.get("ui.SelectionStyllingOptionsPanel.RestoreDefaultsSettings"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        mainPanel.add(restoreDefaultsButton, gridBagConstraints);

    }


	public String validateInput() {
		return null;
	}

	public void okPressed() {
		// first store into Blackboard for saving
		blackboard.put(BB_SELECTION_STYLE_COLOR, lineColorChooserPanel.getColor());
		blackboard.put(BB_SELECTION_STYLE_FILL_COLOR, fillColorChooserPanel.getColor());
		blackboard.put(BB_SELECTION_STYLE_FILL_OPACITY, fillColorChooserPanel.getAlpha());
		blackboard.put(BB_SELECTION_STYLE_POINT_SIZE, pointSizeSlider.getValue());
		blackboard.put(BB_SELECTION_STYLE_POINT_FORM, ((String[])pointStyleComboBox.getSelectedItem())[1]);
		// second set the values for the AbstractSelectionRenderer, which is the "rootclass" of all SelectionRenderer's
		LayerViewPanel layerViewPanel = context.getLayerViewPanel();
		if (layerViewPanel != null) { // if no project is there the LayerViewPanel is null
			AbstractSelectionRenderer renderer = (AbstractSelectionRenderer) layerViewPanel.getRenderingManager().getRenderer(FeatureSelectionRenderer.CONTENT_ID);
			renderer.setSelectionLineColor(lineColorChooserPanel.getColor());
			renderer.setSelectionFillColor(GUIUtil.alphaColor(fillColorChooserPanel.getColor(), fillColorChooserPanel.getAlpha()));
			renderer.setSelectionPointSize(pointSizeSlider.getValue());
			renderer.setSelectionPointForm(((String[])pointStyleComboBox.getSelectedItem())[1]);
			layerViewPanel.repaint();
		}
	}

	public void init() {
		Object color = blackboard.get(BB_SELECTION_STYLE_COLOR, DEFAULT_SELECTION_STYLE_COLOR);
		if (color instanceof Color) lineColorChooserPanel.setColor((Color) color);
		color = blackboard.get(BB_SELECTION_STYLE_FILL_COLOR, DEFAULT_SELECTION_STYLE_COLOR);
		if (color instanceof Color) fillColorChooserPanel.setColor((Color) color);
		Object opacity = blackboard.get(BB_SELECTION_STYLE_FILL_OPACITY, DEFAULT_SELECTION_STYLE_FILL_OPACITY);
		if (opacity instanceof Integer) fillOpacitySlider.setValue(((Integer) blackboard.get(BB_SELECTION_STYLE_FILL_OPACITY, DEFAULT_SELECTION_STYLE_FILL_OPACITY)).intValue());
		Object size = blackboard.get(BB_SELECTION_STYLE_POINT_SIZE, DEFAULT_SELECTION_STYLE_POINT_SIZE);
		if (size instanceof Integer) pointSizeSlider.setValue(((Integer) blackboard.get(BB_SELECTION_STYLE_POINT_SIZE, DEFAULT_SELECTION_STYLE_POINT_SIZE)).intValue());
		// select the item
		String style = (String) blackboard.get(BB_SELECTION_STYLE_POINT_FORM, DEFAULT_SELECTION_STYLE_POINT_FORM);
		int count = pointStyleComboBox.getItemCount();
		for (int i = 0; i < count; i++) {
			String[] item = (String[]) pointStyleComboBox.getItemAt(i);
			if (item[1].equals(style)) {
				pointStyleComboBox.setSelectedIndex(i);
				break;
			}
		}
	}

  // moved this into an extra class because it lead to a stackoverflow when
  // switching skins in JComboBox#updateUI, essentially the method grabs the
  // ListCellRenderer and executes SwingUtilities#updateComponentTreeUI on it
  // again which is a bad idea if the ListCellRenderer is also the parent
  // Component [ ede 5.4.2012 ]
  private class ComboRenderer implements ListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      JLabel label = new JLabel(((String[]) value)[0]);
      label.setOpaque(true);
      if (isSelected) {
        label.setBackground(new Color(163, 184, 204)); // may be the original
                                                       // Color of a JComboBox
      }
      return label;
    }
  }
}

