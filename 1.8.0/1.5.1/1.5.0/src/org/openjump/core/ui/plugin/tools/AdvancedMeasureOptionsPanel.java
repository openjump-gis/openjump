package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.MeasureLayerFinder;
import com.vividsolutions.jump.workbench.ui.FontChooser;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;
import com.vividsolutions.jump.workbench.ui.OptionsPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import de.latlon.deejump.plugin.style.VertexStylesFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openjump.core.ui.swing.VertexStylePanel;

/**
 * This Panel displays the options for the AdvancedMeasureTool.
 * The options are:
 * - Summary
 *		- display area and distance?
 *			- font and size
 *			- font color
 * - vertex
 *		- display distance per vertex?
 *			- font and size
 *			- font color
 *		- paint vertex?
 *			- vertex style for the first vertex and the following vertexes
 * - line and fill style
 *		- paint line?
 *			- linecolor
 *		- paint fill?
 *			- fillcolor
 *
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class AdvancedMeasureOptionsPanel extends JPanel implements OptionsPanel {

	// Blackboard keys
	// summary
	public static final String BB_SUMMARY_PAINT_LENGTH = AdvancedMeasureOptionsPanel.class.getName() + " - SUMMARY_PAINT_LENGTH";
	public static final String BB_SUMMARY_PAINT_AREA = AdvancedMeasureOptionsPanel.class.getName() + " - SUMMARY_PAINT_AREA";
	public static final String BB_SUMMARY_FONT = AdvancedMeasureOptionsPanel.class.getName() + " - SUMMARY_FONT";
	public static final String BB_SUMMARY_FONT_COLOR = AdvancedMeasureOptionsPanel.class.getName() + " - SUMMARY_FONT_COLOR";
	// vertex
	public static final String BB_VERTEX_PAINT_DISTANCE = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_PAINT_DISTANCE";
	public static final String BB_VERTEX_FONT = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FONT";
	public static final String BB_VERTEX_FONT_COLOR = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FONT_COLOR";
	public static final String BB_VERTEX_PAINT = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_PAINT";
	public static final String BB_VERTEX_FIRST_COLOR = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FIRST_COLOR";
	public static final String BB_VERTEX_FIRST_FORM = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FIRST_FORM";
	public static final String BB_VERTEX_FIRST_SIZE = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FIRST_SIZE";
	public static final String BB_VERTEX_FOLLOWING_COLOR = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FOLLOWING_COLOR";
	public static final String BB_VERTEX_FOLLOWING_FORM = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FOLLOWING_FORM";
	public static final String BB_VERTEX_FOLLOWING_SIZE = AdvancedMeasureOptionsPanel.class.getName() + " - VERTEX_FOLLOWING_SIZE";
	// line and fill
	public static final String BB_LINE_PAINT = AdvancedMeasureOptionsPanel.class.getName() + " - LINE_PAINT";
	public static final String BB_LINE_COLOR = AdvancedMeasureOptionsPanel.class.getName() + " - LINE_COLOR";
	public static final String BB_FILL_PAINT = AdvancedMeasureOptionsPanel.class.getName() + " - FILL_PAINT";
	public static final String BB_FILL_COLOR = AdvancedMeasureOptionsPanel.class.getName() + " - FILL_COLOR";

	// Default values
	// summary
	public static final Font DEFAULT_SUMMARY_FONT = new Font("Dialog", Font.PLAIN, 24);
	public static final Color DEFAULT_SUMMARY_COLOR = Color.black;
	public static final boolean DEFAULT_SUMMARY_PAINT_LENGTH = true;
	public static final boolean DEFAULT_SUMMARY_PAINT_AREA = true;
	// vertex
	public static final boolean DEFAULT_VERTEX_PAINT_DISTANCE = true;
	public static final Font DEFAULT_VERTEX_FONT = new Font("Dialog", Font.PLAIN, 12);
	public static final Color DEFAULT_VERTEX_FONT_COLOR = Color.black;
	public static final boolean DEFAULT_VERTEX_PAINT = true;
	public static final Color DEFAULT_VERTEX_FIRST_COLOR = Color.orange;
	public static final String DEFAULT_VERTEX_FIRST_FORM = VertexStylesFactory.SQUARE_STYLE;
	public static final int DEFAULT_VERTEX_FIRST_SIZE = 10;
	public static final Color DEFAULT_VERTEX_FOLLOWING_COLOR = Color.red;
	public static final String DEFAULT_VERTEX_FOLLOWING_FORM = VertexStylesFactory.SQUARE_STYLE;
	public static final int DEFAULT_VERTEX_FOLLOWING_SIZE = 5;
	// line and fill
	public static final boolean DEFAULT_LINE_PAINT = true;
	public static final Color DEFAULT_LINE_COLOR = Color.red;
	public static final boolean DEFAULT_FILL_PAINT = true;
	public static final Color DEFAULT_FILL_COLOR = Color.red;

	private WorkbenchContext context = null;
	private Blackboard blackboard = null;
	private JPanel mainPanel;

	// summary widgets
	private JButton summaryFontButton;
	private JButton summaryFontColorButton;
	private JCheckBox paintSummaryLengthCheckBox;
	private JCheckBox paintSummaryAreaCheckBox;

	// vertex widgets
	private JCheckBox paintVertexDistanceCheckBox;
	private JButton vertexFontButton;
	private JButton vertexFontColorButton;
	private JCheckBox paintVertexCheckBox;
	private JButton vertexStyleButton;
	private JPanel vertexStylePanels;
	private VertexStylePanel vertexStylePanelFirst;
	private VertexStylePanel vertexStylePanelFollowing;

	// line and fill widgets
	private JCheckBox paintLineCheckBox;
	private JButton lineColorButton;
	private JCheckBox paintFillCheckBox;
	private JButton fillColorButton;

	// the variables for the options
	// summary
	private Font summaryFont = DEFAULT_SUMMARY_FONT;
	private Color summaryFontColor = DEFAULT_SUMMARY_COLOR;
	// vertex
	private Font vertexFont = DEFAULT_VERTEX_FONT;
	private Color vertexFontColor = DEFAULT_VERTEX_FONT_COLOR;
	// line and fill
	private Color lineColor = DEFAULT_LINE_COLOR;
	private Color fillColor = DEFAULT_FILL_COLOR;

	public AdvancedMeasureOptionsPanel(WorkbenchContext context) {
		this.context = context;
		blackboard = PersistentBlackboardPlugIn.get(this.context);
		initComponents();
	}

	private void initComponents() {
		GridBagConstraints gridBagConstraints;

		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
		mainPanel = new JPanel(new GridBagLayout());
		this.add(mainPanel, BorderLayout.CENTER);

		/* ************************************************
		 * summary settings
		 * ************************************************ */
		JPanel summaryPanel = new JPanel(new GridBagLayout());
		summaryPanel.setBorder(new TitledBorder(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.summary")));
		JPanel summaryContentPanel = new JPanel(new GridBagLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		summaryPanel.add(summaryContentPanel, gridBagConstraints);
		summaryFontButton = new JButton(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-font"));
		summaryFontColorButton = new JButton(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"));

		// checkbox for paint the summary (distance and area)
		// length checkbox
		paintSummaryLengthCheckBox = new JCheckBox(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.paint-summary-length"));
		paintSummaryAreaCheckBox = new JCheckBox(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.paint-summary-area"));
		paintSummaryLengthCheckBox.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				summaryFontButton.setEnabled(paintSummaryLengthCheckBox.isSelected() || paintSummaryAreaCheckBox.isSelected());
				summaryFontColorButton.setEnabled(paintSummaryLengthCheckBox.isSelected() || paintSummaryAreaCheckBox.isSelected());
			}
		});
		paintSummaryLengthCheckBox.setSelected(DEFAULT_SUMMARY_PAINT_LENGTH);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        summaryContentPanel.add(paintSummaryLengthCheckBox, gridBagConstraints);

		// area checkbox
		paintSummaryAreaCheckBox.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				summaryFontButton.setEnabled(paintSummaryLengthCheckBox.isSelected() || paintSummaryAreaCheckBox.isSelected());
				summaryFontColorButton.setEnabled(paintSummaryLengthCheckBox.isSelected() || paintSummaryAreaCheckBox.isSelected());
			}
		});
		paintSummaryAreaCheckBox.setSelected(DEFAULT_SUMMARY_PAINT_AREA);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        summaryContentPanel.add(paintSummaryAreaCheckBox, gridBagConstraints);


		// font label
		JLabel summaryFontLabel = new JLabel(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.font"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 26, 0, 0);
        summaryContentPanel.add(summaryFontLabel, gridBagConstraints);
		// font button
		summaryFontButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Font newFont = FontChooser.showDialog(OptionsDialog.instance(context.getWorkbench()), I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-font"), summaryFont, true);
				if (newFont != null) summaryFont = newFont;
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        summaryContentPanel.add(summaryFontButton, gridBagConstraints);

		// font color label
		JLabel fontColorLabel = new JLabel(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.fontcolor"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 26, 0, 0);
        summaryContentPanel.add(fontColorLabel, gridBagConstraints);
		// font color button
		summaryFontColorButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				summaryFontColor = JColorChooser.showDialog(OptionsDialog.instance(context.getWorkbench()), I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"), summaryFontColor);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        summaryContentPanel.add(summaryFontColorButton, gridBagConstraints);

		// summaryPanel
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        mainPanel.add(summaryPanel, gridBagConstraints);

		/* ************************************************
		 * Vertex styling
		 * ************************************************ */
		JPanel vertexPanel = new JPanel(new GridBagLayout());
		vertexPanel.setBorder(new TitledBorder(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.vertex")));
		JPanel vertexContentPanel = new JPanel(new GridBagLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		vertexPanel.add(vertexContentPanel, gridBagConstraints);
		vertexFontButton = new JButton(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-font"));
		vertexFontColorButton = new JButton(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"));

		// checkbox for paint the distance per vertex
		paintVertexDistanceCheckBox = new JCheckBox(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.paint-vertex-distance"));
		paintVertexDistanceCheckBox.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				vertexFontButton.setEnabled(paintVertexDistanceCheckBox.isSelected());
				vertexFontColorButton.setEnabled(paintVertexDistanceCheckBox.isSelected());
			}
		});
		paintVertexDistanceCheckBox.setSelected(DEFAULT_VERTEX_PAINT_DISTANCE);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        vertexContentPanel.add(paintVertexDistanceCheckBox, gridBagConstraints);

		// font label
		JLabel vertexFontLabel = new JLabel(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.font"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 26, 0, 0);
        vertexContentPanel.add(vertexFontLabel, gridBagConstraints);

		// font button
		vertexFontButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Font newFont = FontChooser.showDialog(OptionsDialog.instance(context.getWorkbench()), I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-font"), vertexFont, true);
				if (newFont != null) vertexFont = newFont;
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        vertexContentPanel.add(vertexFontButton, gridBagConstraints);

		// font color label
		JLabel vertexFontColorLabel = new JLabel(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.fontcolor"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 26, 0, 0);
        vertexContentPanel.add(vertexFontColorLabel, gridBagConstraints);
		// font color button
		vertexFontColorButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				vertexFontColor = JColorChooser.showDialog(OptionsDialog.instance(context.getWorkbench()), I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"), vertexFontColor);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        vertexContentPanel.add(vertexFontColorButton, gridBagConstraints);

		// vertex style
		vertexStyleButton = new JButton(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.change-style"));
		// checkbox for paint vertex or not
		paintVertexCheckBox = new JCheckBox(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.paint-vertex"));
		paintVertexCheckBox.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				vertexStyleButton.setEnabled(paintVertexCheckBox.isSelected());
			}
		});
		paintVertexCheckBox.setSelected(DEFAULT_VERTEX_PAINT);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        vertexContentPanel.add(paintVertexCheckBox, gridBagConstraints);

		// vertex style label
		JLabel vertexStyleLabel = new JLabel(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.vertexstyle"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 26, 0, 0);
        vertexContentPanel.add(vertexStyleLabel, gridBagConstraints);
		// dialog with the vertex style settings
		vertexStylePanels = new JPanel(new FlowLayout());
		vertexStylePanelFirst = new VertexStylePanel(false);
		vertexStylePanelFirst.setBorder(new TitledBorder(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.first-vertex")));
		vertexStylePanelFollowing = new VertexStylePanel(false);
		vertexStylePanelFollowing.setBorder(new TitledBorder(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.following-vertexes")));
		vertexStylePanels.add(vertexStylePanelFirst);
		vertexStylePanels.add(vertexStylePanelFollowing);
		// vertex style button
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		vertexStyleButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// save the current value to restore it, if cancel button was pressed
				Color firstVertexColor = vertexStylePanelFirst.getVertexColor();
				String firstVertexForm = vertexStylePanelFirst.getVertexForm();
				int firstVertexSize = vertexStylePanelFirst.getVertexSize();
				Color followingVertexColor = vertexStylePanelFollowing.getVertexColor();
				String followingVertexForm = vertexStylePanelFollowing.getVertexForm();
				int followingVertexSize = vertexStylePanelFollowing.getVertexSize();
				// create the dialog
				OKCancelDialog vertexStyleDialog = new OKCancelDialog(context.getWorkbench().getFrame(), 
						I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.change-style"),
						true, vertexStylePanels, new OKCancelDialog.Validator() {

					public String validateInput(Component component) {
						return null;
					}
				});
				vertexStyleDialog.setVisible(true);
				// if the use have the cancel button pressed, then restore the old values
				if (!vertexStyleDialog.wasOKPressed()) {
					vertexStylePanelFirst.setVertexColor(firstVertexColor);
					vertexStylePanelFirst.setVertexForm(firstVertexForm);
					vertexStylePanelFirst.setVertexSize(firstVertexSize);
					vertexStylePanelFollowing.setVertexColor(followingVertexColor);
					vertexStylePanelFollowing.setVertexForm(followingVertexForm);
					vertexStylePanelFollowing.setVertexSize(followingVertexSize);
				}
			}
		});
		vertexContentPanel.add(vertexStyleButton, gridBagConstraints);

		// vertexPanel
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        mainPanel.add(vertexPanel, gridBagConstraints);

		/* ************************************************
		 * line and fill settings
		 * ************************************************ */
		JPanel lineFillPanel = new JPanel(new GridBagLayout());
		lineFillPanel.setBorder(new TitledBorder(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.line-and-fill")));
		JPanel lineFillContentPanel = new JPanel(new GridBagLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		lineFillPanel.add(lineFillContentPanel, gridBagConstraints);
		lineColorButton = new JButton(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"));
		fillColorButton = new JButton(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"));

		// checkbox for paint lines or not
		paintLineCheckBox = new JCheckBox(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.paint-line"));
		paintLineCheckBox.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				lineColorButton.setEnabled(paintLineCheckBox.isSelected());
			}
		});
		paintLineCheckBox.setSelected(DEFAULT_LINE_PAINT);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        lineFillContentPanel.add(paintLineCheckBox, gridBagConstraints);

		// line color label
		JLabel lineColorLabel = new JLabel(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.linecolor"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 26, 0, 0);
        lineFillContentPanel.add(lineColorLabel, gridBagConstraints);
		// line color button
		lineColorButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				lineColor = JColorChooser.showDialog(OptionsDialog.instance(context.getWorkbench()), I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"), lineColor);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        lineFillContentPanel.add(lineColorButton, gridBagConstraints);

		// checkbox for fill or not
		paintFillCheckBox = new JCheckBox(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.filling"));
		paintFillCheckBox.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				fillColorButton.setEnabled(paintFillCheckBox.isSelected());
			}
		});
		paintFillCheckBox.setSelected(DEFAULT_FILL_PAINT);
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        lineFillContentPanel.add(paintFillCheckBox, gridBagConstraints);

		// font color label
		JLabel fillColorLabel = new JLabel(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.fillcolor"));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 26, 0, 0);
        lineFillContentPanel.add(fillColorLabel, gridBagConstraints);
		// fill color button
		fillColorButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				fillColor = JColorChooser.showDialog(OptionsDialog.instance(context.getWorkbench()), I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel.choose-color"), fillColor);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
        lineFillContentPanel.add(fillColorButton, gridBagConstraints);

		// lineFillPanel
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        mainPanel.add(lineFillPanel, gridBagConstraints);


		/* ************************************************
		 * empty fill Panel for nice layout
		 * ************************************************ */
		JPanel fillPanel = new JPanel();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(fillPanel, gridBagConstraints);

	}

	public String validateInput() {
		return null;
	}

	public void okPressed() {
		// first store into the Blackboard
		// summary
		blackboard.put(BB_SUMMARY_FONT_COLOR, summaryFontColor);
		blackboard.put(BB_SUMMARY_FONT, summaryFont);
		blackboard.put(BB_SUMMARY_PAINT_LENGTH, paintSummaryLengthCheckBox.isSelected());
		blackboard.put(BB_SUMMARY_PAINT_AREA, paintSummaryAreaCheckBox.isSelected());
		// vertex
		blackboard.put(BB_VERTEX_FONT, vertexFont);
		blackboard.put(BB_VERTEX_FONT_COLOR, vertexFontColor);
		blackboard.put(BB_VERTEX_PAINT_DISTANCE, paintVertexDistanceCheckBox.isSelected());

		blackboard.put(BB_VERTEX_PAINT, paintVertexCheckBox.isSelected());
		blackboard.put(BB_VERTEX_FIRST_COLOR, vertexStylePanelFirst.getVertexColor());
		blackboard.put(BB_VERTEX_FIRST_FORM, vertexStylePanelFirst.getVertexForm());
		blackboard.put(BB_VERTEX_FIRST_SIZE, vertexStylePanelFirst.getVertexSize());
		blackboard.put(BB_VERTEX_FOLLOWING_COLOR, vertexStylePanelFollowing.getVertexColor());
		blackboard.put(BB_VERTEX_FOLLOWING_FORM, vertexStylePanelFollowing.getVertexForm());
		blackboard.put(BB_VERTEX_FOLLOWING_SIZE, vertexStylePanelFollowing.getVertexSize());

		// line and fill
		blackboard.put(BB_LINE_PAINT, paintLineCheckBox.isSelected());
		blackboard.put(BB_LINE_COLOR, lineColor);
		blackboard.put(BB_FILL_PAINT, paintFillCheckBox.isSelected());
		blackboard.put(BB_FILL_COLOR, fillColor);

		// second change the Layer's Style
		Layer layer = (new MeasureLayerFinder(context, context)).getLayer();
		// if the MeasureLayer is available, then appy the changes
		if (layer != null) {
			MeasurementStyle style = (MeasurementStyle) layer.getStyle(MeasurementStyle.class);
			// summary
			style.setSummaryFont(summaryFont);
			style.setSummaryColor(summaryFontColor);
			style.setPaintSummaryLength(paintSummaryLengthCheckBox.isSelected());
			style.setPaintSummaryArea(paintSummaryAreaCheckBox.isSelected());
			// vertex labeling
			style.setVertexFont(vertexFont);
			style.setVertexFontColor(vertexFontColor);
			style.setVertexPaintDistance(paintVertexDistanceCheckBox.isSelected());
			// vertex painting
			style.setVertexPaint(paintVertexCheckBox.isSelected());
			style.setVertexFirstColor(vertexStylePanelFirst.getVertexColor());
			style.setVertexFirstForm(vertexStylePanelFirst.getVertexForm());
			style.setVertexFirstSize(vertexStylePanelFirst.getVertexSize());
			style.setVertexFollowingColor(vertexStylePanelFollowing.getVertexColor());
			style.setVertexFollowingForm(vertexStylePanelFollowing.getVertexForm());
			style.setVertexFollowingSize(vertexStylePanelFollowing.getVertexSize());

			// line and fill
			BasicStyle basicStyle = layer.getBasicStyle();
			basicStyle.setRenderingLine(paintLineCheckBox.isSelected());
			basicStyle.setLineColor(lineColor);
			basicStyle.setRenderingFill(paintFillCheckBox.isSelected());
			basicStyle.setFillColor(fillColor);

			context.getLayerViewPanel().repaint();
		}
	}

	public void init() {
		Object font;
		Object color;
		Object string;
		// summary settings
		paintSummaryLengthCheckBox.setSelected(blackboard.get(BB_SUMMARY_PAINT_LENGTH, DEFAULT_SUMMARY_PAINT_LENGTH));
		paintSummaryAreaCheckBox.setSelected(blackboard.get(BB_SUMMARY_PAINT_AREA, DEFAULT_SUMMARY_PAINT_AREA));
		font = blackboard.get(BB_SUMMARY_FONT, DEFAULT_SUMMARY_FONT);
		if (font instanceof Font) summaryFont = (Font) font;
		color = blackboard.get(BB_SUMMARY_FONT_COLOR, DEFAULT_SUMMARY_COLOR);
		if (color instanceof Color) summaryFontColor = (Color) color;
		// vertex settings
		paintVertexDistanceCheckBox.setSelected(blackboard.get(BB_VERTEX_PAINT_DISTANCE, DEFAULT_VERTEX_PAINT_DISTANCE));
		font = blackboard.get(BB_VERTEX_FONT, DEFAULT_VERTEX_FONT);
		if (font instanceof Font) vertexFont = (Font) font;
		color = blackboard.get(BB_VERTEX_FONT_COLOR, DEFAULT_VERTEX_FONT_COLOR);
		if (color instanceof Color) vertexFontColor = (Color) color;

		paintVertexCheckBox.setSelected(blackboard.get(BB_VERTEX_PAINT, DEFAULT_VERTEX_PAINT));
		color = blackboard.get(BB_VERTEX_FIRST_COLOR, DEFAULT_VERTEX_FIRST_COLOR);
		if (color instanceof Color) vertexStylePanelFirst.setVertexColor((Color) color);
		string = blackboard.get(BB_VERTEX_FIRST_FORM, DEFAULT_VERTEX_FIRST_FORM);
		if (string instanceof String) vertexStylePanelFirst.setVertexForm((String) string);
		vertexStylePanelFirst.setVertexSize(blackboard.get(BB_VERTEX_FIRST_SIZE, DEFAULT_VERTEX_FIRST_SIZE));
		color = blackboard.get(BB_VERTEX_FOLLOWING_COLOR, DEFAULT_VERTEX_FOLLOWING_COLOR);
		if (color instanceof Color) vertexStylePanelFollowing.setVertexColor((Color) color);
		string = blackboard.get(BB_VERTEX_FOLLOWING_FORM, DEFAULT_VERTEX_FOLLOWING_FORM);
		if (string instanceof String) vertexStylePanelFollowing.setVertexForm((String) string);
		vertexStylePanelFollowing.setVertexSize(blackboard.get(BB_VERTEX_FOLLOWING_SIZE, DEFAULT_VERTEX_FOLLOWING_SIZE));

		// line and fill
		paintLineCheckBox.setSelected(blackboard.get(BB_LINE_PAINT, DEFAULT_LINE_PAINT));
		color = blackboard.get(BB_LINE_COLOR, DEFAULT_LINE_COLOR);
		if (color instanceof Color) lineColor = (Color) color;
		paintFillCheckBox.setSelected(blackboard.get(BB_FILL_PAINT, DEFAULT_FILL_PAINT));
		color = blackboard.get(BB_FILL_COLOR, DEFAULT_FILL_COLOR);
		if (color instanceof Color) fillColor = (Color) color;
	}


}
