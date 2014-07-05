/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui.renderer.style;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.Range;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ColorPanel;
import com.vividsolutions.jump.workbench.ui.EnableableToolBar;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.style.attributeclassifications.JenksBreaksColorThemingState;
import com.vividsolutions.jump.workbench.ui.renderer.style.attributeclassifications.MaximalBreaksColorThemingState;
import com.vividsolutions.jump.workbench.ui.renderer.style.attributeclassifications.MeanSTDevColorThemingState;
import com.vividsolutions.jump.workbench.ui.renderer.style.attributeclassifications.QuantileColorThemingState;
import com.vividsolutions.jump.workbench.ui.renderer.style.attributeclassifications.RangeColorThemingState;
import com.vividsolutions.jump.workbench.ui.style.StylePanel;

import de.latlon.deejump.plugin.style.DeeRenderingStylePanel;

/**
 * A panel to choose Color Theming options.
 * The panel includes
 * <ul>
 * <li>a checkbox to enable Color Theming</li>
 * <li>a combobox to choose Attribute</li>
 * <li>a combobox to choose Classification Method plus additional parameters
 * in case of range-based classification</li>
 * <li>a combobox to choose a Color Scheme</li>
 * <li>a table to display colors with two plugins to add/remove lines</li>
 * <li>a slider to set the transparency</li>
 * <li>a slider to set lineWidth</li>
 * </ul>
 *
 * @author <p>jonathan aquino (original author)</p>
 * @author <p>obedel and ebocher (2005 - added Quantile Color Theming)</p>
 * @author <p>ssteiniger (2009 - added 3 more range-based classification methods)</p>
 * @author <p>mmichaud (2011 - debug, remove deprecated code and comments, give
 *         priority to attribute value choice instead of classification choice)</p>
 *         <p>(2014 - add lineWidth parameter and vetexStyleEnabled</p>
 *         <p>(2014 - manage VertexStyle with XBasicStyle, an extension of BasicStyle</p>
 */
public class ColorThemingStylePanel extends JPanel implements StylePanel {

    private static final String CUSTOM_ENTRY = I18N.get("ui.renderer.style.ColorThemingPanel.custom");
    public static final String TITLE = I18N.get("ui.renderer.style.ColorThemingPanel.colour-theming");
    private static final String COLOR_SCHEME_KEY = ColorThemingStylePanel.class.getName() + " - COLOR SCHEME";

    private static final Dimension SLIDER_DIMENSION = new Dimension(130, 28);
    private static final int SLIDER_TEXT_FIELD_COLUMNS = 3;
    
    private WorkbenchContext workbenchContext;
    private Layer layer;
    
    // jPanel1 contains enable, attribute, classification and color scheme options
    final private JPanel jPanel1 = new JPanel();
    final private JCheckBox enableColorThemingCheckBox = new JCheckBox();

    final private JLabel attributeLabel = new JLabel();
    final private JComboBox<String> attributeNameComboBox = new JComboBox<String>();
    private String lastAttributeName;

    final private JLabel classificationLabel = new JLabel(I18N.get("ui.renderer.style.ColorThemingStylePanel.Classification-Method"));
    final private JComboBox<String> classificationComboBox = new JComboBox<String>();

    final private JLabel colorSchemeLabel = new JLabel();
    final private JComboBox<String> colorSchemeComboBox = new JComboBox<String>();
    
    // jPanel2 contains the scrollPane (table), add/remove plugin, transparency and lineWidth slider)
    final private JPanel jPanel2 = new JPanel();
    final private JScrollPane scrollPane = new JScrollPane();
    final private JLabel transparencyLabel = new JLabel();
    final private JSlider transparencySlider = new JSlider();
    final private JLabel lineWidthLabel = new JLabel();
    final private JSlider lineWidthSlider = new JSlider();
    final private JLabel vertexStyleLabel = new JLabel(I18N.get("ui.renderer.style.ColorThemingStylePanel.display-vertices"));
    final private JCheckBox vertexStyleEnableCheckBox = new JCheckBox();

    final private DefaultTableCellRenderer allOtherValuesRenderer = new DefaultTableCellRenderer();
    // main table displaying color theming
    final private JTable table = new JTable() {
        
            public TableCellRenderer getCellRenderer(int row, int column) {
                TableCellRenderer renderer = getCellRendererProper(row, column);
                if (renderer instanceof JComponent) {
                    updateBackground((JComponent) renderer);
                }
                return renderer;
            }

            private TableCellRenderer getCellRendererProper(int row, int column) {
                if ((row == 0) && (column == attributeColumn())) {
                    return allOtherValuesRenderer;
                }
                if ((row == 0) && (column == labelColumn())) {
                    return allOtherValuesRenderer;
                }
                TableCellRenderer renderer = super.getCellRenderer(row, column);
                if (renderer instanceof JLabel) {
                    ((JLabel) renderer).setHorizontalAlignment(JLabel.LEFT);
                }
                return renderer;
            }
        };

    final private JLabel statusLabel = new JLabel() {
            public void setText(String text) {
                super.setText(text);
                setToolTipText(text);
            }
        };

    final private EnableableToolBar toolBar = new EnableableToolBar();
    
    private boolean updatingComponents = false;
    private boolean initializing = false;
    // Styles displayed in each combobox of the table
    final private BasicStyleListCellRenderer basicStyleListCellRenderer = new BasicStyleListCellRenderer();
    private DeeRenderingStylePanel deeRenderingStylePanel;

    // Cell editor for the table
    final private TableCellEditor basicStyleTableCellEditor = new TableCellEditor() {
        final private DefaultComboBoxModel<Object> comboBoxModel = new DefaultComboBoxModel<Object>();
        private BasicStyle originalStyle;
        final private DefaultCellEditor editor;
        // Create JComboBox to select a style among styles created from the ColorScheme
        final private JComboBox<Object> comboBox = new JComboBox<Object>(comboBoxModel) {

                public void setSelectedItem(Object anObject) {
                    if (anObject != CUSTOM_ENTRY) {
                        super.setSelectedItem(anObject);
                        return;
                    }
                    // If we do not hide popup here, the first click in the dialog opened
                    // by the prompt will do that instead of interacting with the dialog
                    this.hidePopup();
                    BasicStyle style = promptBasicStyle(originalStyle);
                    if (style == null) {
                        return;
                    }
                    comboBox.addItem(style);
                    super.setSelectedItem(style);
                }
        };

        {
            comboBox.setRenderer(basicStyleListCellRenderer);
            editor = new DefaultCellEditor(comboBox);
        }

            public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
                originalStyle = (BasicStyle) value;
                comboBoxModel.removeAllElements();
                comboBoxModel.addElement(CUSTOM_ENTRY);

                //Include the current BasicStyle; otherwise, the current BasicStyle
                //will not show up as the selected item in the combo box. [Jon Aquino]
                comboBoxModel.addElement(value);
                for (Color color : ColorScheme.create((String)colorSchemeComboBox.getSelectedItem()).getColors()) {
                    XBasicStyle xbs = new XBasicStyle(getLayer().getBasicStyle(), getLayer().getVertexStyle());
                    xbs.setFillColor(color);
                    xbs.setLineColor(color.darker());
                    if (originalStyle instanceof XBasicStyle) {
                        xbs.getVertexStyle().setFillColor(color);
                        xbs.getVertexStyle().setFillColor(color.darker());
                        xbs.getVertexStyle().setEnabled(((XBasicStyle)originalStyle).getVertexStyle().isEnabled());
                    }
                    comboBoxModel.addElement(xbs);
                }

                comboBoxModel.setSelectedItem(value);

            return editor.getTableCellEditorComponent(table, value,
                    isSelected, row, column);
        }

        public Object getCellEditorValue() {
                return editor.getCellEditorValue();
            }

        public boolean isCellEditable(EventObject anEvent) {
                return editor.isCellEditable(anEvent);
            }

        public boolean shouldSelectCell(EventObject anEvent) {
                return editor.shouldSelectCell(anEvent);
            }

        public boolean stopCellEditing() {
                return editor.stopCellEditing();
            }

        public void cancelCellEditing() {
                editor.cancelCellEditing();
            }

        public void addCellEditorListener(CellEditorListener l) {
                editor.addCellEditorListener(l);
            }

        public void removeCellEditorListener(CellEditorListener l) {
                editor.removeCellEditorListener(l);
            }
    };

    private ColorScheme colorSchemeForInserts = null;

    final private MyPlugIn insertPlugIn = new MyPlugIn() {
            public String getName() {
                return I18N.get("ui.renderer.style.ColorThemingPanel.insert-row");
            }

            public Icon getIcon() {
                return GUIUtil.toSmallIcon(IconLoader.icon("Plus.gif"));
            }

            public boolean execute(PlugInContext context)
                throws Exception {
                reportNothingToUndoYet(context);
                stopCellEditing();
                tableModel().insertAttributeValue((table.getSelectedRowCount() > 0)
                    ? table.getSelectedRows()[0] : table.getRowCount(),
                    getColorSchemeForInserts());

                if (table.getSelectedRowCount() == 0) {
                    table.scrollRectToVisible(table.getCellRect(table.getRowCount() -
                            1, 0, true));
                }

                if (table.getSelectedRowCount() != 0) {
                    int firstSelectedRow = table.getSelectedRows()[0];
                    table.clearSelection();
                    table.addRowSelectionInterval(firstSelectedRow,
                        firstSelectedRow);
                }

                return true;
            }
        };

    final private MyPlugIn deletePlugIn = new MyPlugIn() {
            public String getName() {
                return I18N.get("ui.renderer.style.ColorThemingPanel.delete-row");
            }

            public Icon getIcon() {
                return GUIUtil.toSmallIcon(IconLoader.icon("Delete.gif"));
            }

            public boolean execute(PlugInContext context)
                throws Exception {
                reportNothingToUndoYet(context);
                stopCellEditing();
                tableModel().removeAttributeValues(table.getSelectedRows());

                return true;
            }
        };

    
    //Maintain a blackboard for error messages instead of running all
    //validations whenever we need to check for messages -- some validations
    //may be expensive. [Jon Aquino]
    final private HashSet<ErrorMessage> errorMessages = new HashSet<ErrorMessage>();

    final private DiscreteColorThemingState discreteColorThemingState = new DiscreteColorThemingState(table);
    private RangeColorThemingState rangeColorThemingState;
	private QuantileColorThemingState quantileColorThemingState;
	private MeanSTDevColorThemingState meanSTDevColorThemingState;
	private MaximalBreaksColorThemingState maxBreaksColorThemingState;
	private JenksBreaksColorThemingState jenksColorThemingState;
    private State state = discreteColorThemingState;
    
    public ColorThemingStylePanel(Layer layer, WorkbenchContext workbenchContext) {
        initializing = true;

        try {
            //basicStylePanel = new BasicStylePanel(
            //    workbenchContext.getWorkbench().getBlackboard(),
            //    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            // [mmichaud 2014-06-15] replace BasicStylePanel by DeeRenderingStylePanel
            // to make it possible to do ColorTheming with VertexStyle
            deeRenderingStylePanel = new DeeRenderingStylePanel(
                    workbenchContext.getWorkbench().getBlackboard(),
                    layer,
                    PersistentBlackboardPlugIn.get(workbenchContext));
            this.layer = layer;
            this.workbenchContext = workbenchContext;
            // 0 - DiscreteColorThemingState (already initialized)
            // 1 - Equal Range
            rangeColorThemingState = new RangeColorThemingState(this);
            // 2 - Quantile or Equal Number (added by obedel)
            quantileColorThemingState = new QuantileColorThemingState(this);
            // 3 - Mean-Standard-Deviation (added by sstein)
            meanSTDevColorThemingState = new MeanSTDevColorThemingState(this);
            // 4 - Maximal Breaks (added by sstein)
            maxBreaksColorThemingState = new MaximalBreaksColorThemingState(this);
            // 5 - Jenks Optimal (added by sstein)
            jenksColorThemingState = new JenksBreaksColorThemingState(this);
            
            initAttributeNameComboBox(layer);
            jbInit();
            
            //Must set state early so that #initTable can call #fromExternalFormat. [Jon Aquino]
            state = colorThemingStyleHasRanges(getLayer()) ?
                (State) rangeColorThemingState : discreteColorThemingState;
            classificationComboBox.removeItemListener(classificationListener);
            classificationComboBox.setSelectedIndex(
                colorThemingStyleHasRanges(getLayer()) ? 1 : 0);
            classificationComboBox.addItemListener(classificationListener);

            //Init the table before calling #setState, in which
            //RangeColorThemingState will add itself as a listener
            //to the table model. [Jon Aquino]
            initTable(layer);
            setState(state);
            initColorSchemeComboBox(layer.getLayerManager());
            initTransparencySlider(layer);
            initLineWidthSlider(layer);
            initVertexStyleEnabled(layer);
            initToolBar();
            enableColorThemingCheckBox.setSelected(ColorThemingStyle.get(layer).isEnabled());
            vertexStyleEnableCheckBox.setSelected(ColorThemingStyle.get(layer).isVertexStyleEnabled());
            updateComponents();
            GUIUtil.sync(deeRenderingStylePanel.getTransparencySlider(), transparencySlider);
            GUIUtil.sync(deeRenderingStylePanel.getLineWidthSlider(), lineWidthSlider);
            deeRenderingStylePanel.setSynchronizingLineColor(layer.isSynchronizingLineColor());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            initializing = false;
        }
    }

    private void updateBackground(JComponent component) {
        component.setBackground(enableColorThemingCheckBox.isSelected()
            ? Color.white : jPanel1.getBackground());
    }

    private int attributeColumn() {
        return table.convertColumnIndexToView(ColorThemingTableModel.ATTRIBUTE_COLUMN);
    }
    private int labelColumn() {
        return table.convertColumnIndexToView(ColorThemingTableModel.LABEL_COLUMN);
    }
    private int colorColumn() {
        return table.convertColumnIndexToView(ColorThemingTableModel.COLOR_COLUMN);
    }

    public String getTitle() {
        return TITLE;
    }

    public void updateStyles() {
        boolean firingEvents = layer.getLayerManager().isFiringEvents();
        layer.getLayerManager().setFiringEvents(false);

        try {
            layer.removeStyle(ColorThemingStyle.get(layer));
            layer.addStyle(new ColorThemingStyle(getAttributeName(), state
                    .toExternalFormat(tableModel()
                            .getAttributeValueToBasicStyleMap()), state
                    .toExternalFormat(tableModel()
                            .getAttributeValueToLabelMap()), tableModel()
                    .getDefaultStyle()));
            ColorThemingStyle.get(layer).setAlpha(getAlpha());
            ColorThemingStyle.get(layer).setLineWidth(getLineWidth());
            ColorThemingStyle.get(layer).setVertexStyleEnabled(isVertexStyleEnabled());
            ColorThemingStyle.get(layer).setEnabled(
                    enableColorThemingCheckBox.isSelected());
            layer.getBasicStyle().setEnabled(
                    !enableColorThemingCheckBox.isSelected());
            // fix bug 3091363 and part of 3043312
            // ColorThemingStyle is updated after DeeRenderingStylePanel
            // enable vertex if it has been enables in DeeRenderingStylePanel
            // and if ColorThemingStyle is not activated
            layer.getVertexStyle().setEnabled(
                    layer.getVertexStyle().isEnabled() &&
                    !enableColorThemingCheckBox.isSelected());
        } finally {
            layer.getLayerManager().setFiringEvents(firingEvents);
        }
        layer.fireAppearanceChanged();
    }

    private String getAttributeName() {
        return (String) attributeNameComboBox.getSelectedItem();
    }
    
    private AttributeType getAttributeType() {
        return layer.getFeatureCollectionWrapper()
		            .getFeatureSchema()
		            .getAttributeType(getAttributeName());
    }

    private void stopCellEditing() {
        if (table.getCellEditor() instanceof DefaultCellEditor) {
            table.getCellEditor().stopCellEditing();
        }
    }

    public JCheckBox getSynchronizeCheckBox() {
        return deeRenderingStylePanel.getSynchronizeCheckBox();
    }

    public Layer getLayer() {
        return layer;
    }


    private void initTransparencySlider(Layer layer) {
        transparencySlider.setValue(transparencySlider.getMaximum() -
                ColorThemingStyle.get(layer).getDefaultStyle().getAlpha());
        transparencySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                basicStyleListCellRenderer.setAlpha(getAlpha());
            }
        });
        basicStyleListCellRenderer.setAlpha(getAlpha());
    }

    private void initLineWidthSlider(Layer layer) {
        lineWidthSlider.setValue(lineWidthSlider.getMaximum() -
                ColorThemingStyle.get(layer).getDefaultStyle().getLineWidth());
        lineWidthSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                basicStyleListCellRenderer.setLineWidth(getLineWidth());
            }
        });
        basicStyleListCellRenderer.setLineWidth(getLineWidth());
    }

    private void initVertexStyleEnabled(Layer layer) {
        vertexStyleEnableCheckBox.setSelected(
                ColorThemingStyle.get(layer).isVertexStyleEnabled());
    }

    private boolean colorThemingStyleHasRanges(Layer layer) {
        return !ColorThemingStyle.get(layer).getAttributeValueToBasicStyleMap()
                                 .isEmpty() &&
        ColorThemingStyle.get(layer).getAttributeValueToBasicStyleMap().keySet()
                         .iterator().next() instanceof Range;
    }

    private void initToolBar() {
        EnableCheck atLeast1RowMustBeSelectedCheck = new EnableCheck() {
                public String check(JComponent component) {
                    return (table.getSelectedRowCount() == 0)
                    ? I18N.get("ui.renderer.style.ColorThemingPanel.at-least-1-row-must-be-selected") : null;
                }
            };

        EnableCheck layerMustHaveAtLeast1AttributeCheck = new EnableCheck() {
                public String check(JComponent component) {
                    return (attributeNameComboBox.getItemCount() == 0)
                    ? I18N.get("ui.renderer.style.ColorThemingPanel.layer-must-have-at-least-1-attribute") : null;
                }
            };

        EnableCheck colorThemingMustBeEnabledCheck = new EnableCheck() {
                public String check(JComponent component) {
                    return (!enableColorThemingCheckBox.isSelected())
                    ? I18N.get("ui.renderer.style.ColorThemingPanel.colour-theming-must-be-enabled") : null;
                }
            };

        addPlugIn(insertPlugIn,
            new MultiEnableCheck().add(layerMustHaveAtLeast1AttributeCheck).add(colorThemingMustBeEnabledCheck));
        addPlugIn(deletePlugIn,
            new MultiEnableCheck().add(layerMustHaveAtLeast1AttributeCheck)
                                  .add(atLeast1RowMustBeSelectedCheck).add(colorThemingMustBeEnabledCheck));
    }

    private void addPlugIn(MyPlugIn plugIn, EnableCheck enableCheck) {
        JButton button = new JButton();
        toolBar.add(button, plugIn.getName(), plugIn.getIcon(),
            AbstractPlugIn.toActionListener(plugIn, workbenchContext, null),
            enableCheck);
    }

    private void updateComponents() {
        if (updatingComponents) {
            return;
        }

        updatingComponents = true;

        try {
            attributeLabel.setEnabled(enableColorThemingCheckBox.isSelected());  //LDB: keep enabled always
            attributeNameComboBox.setEnabled(enableColorThemingCheckBox.isSelected());
            state.getPanel().setEnabled(
                enableColorThemingCheckBox.isSelected() &&
                (attributeNameComboBox.getItemCount() > 0));

			classificationLabel.setEnabled(
			        enableColorThemingCheckBox.isSelected() &&
					(attributeNameComboBox.getItemCount() > 0) &&
					(getAttributeType()==AttributeType.INTEGER || getAttributeType()==AttributeType.DOUBLE));
			classificationComboBox.setEnabled(
			        enableColorThemingCheckBox.isSelected() &&
					(attributeNameComboBox.getItemCount() > 0) &&
					(getAttributeType()==AttributeType.INTEGER || getAttributeType()==AttributeType.DOUBLE));
            
			colorSchemeLabel.setEnabled(
                enableColorThemingCheckBox.isSelected() &&
                (attributeNameComboBox.getItemCount() > 0));
            colorSchemeComboBox.setEnabled(enableColorThemingCheckBox.isSelected() &&
                (attributeNameComboBox.getItemCount() > 0));
            
            table.setEnabled(enableColorThemingCheckBox.isSelected() &&
                (attributeNameComboBox.getItemCount() > 0));
            scrollPane.setEnabled(enableColorThemingCheckBox.isSelected() &&
                (attributeNameComboBox.getItemCount() > 0));
            transparencySlider.setEnabled(enableColorThemingCheckBox.isSelected() &&
                (attributeNameComboBox.getItemCount() > 0));
            lineWidthSlider.setEnabled(enableColorThemingCheckBox.isSelected() &&
                    (attributeNameComboBox.getItemCount() > 0));
            statusLabel.setEnabled(enableColorThemingCheckBox.isSelected());
            toolBar.updateEnabledState();

            //Give the "Cannot colour-theme layer" message priority. [Jon Aquino]
            if (!setErrorMessage(new ErrorMessage(
                    I18N.get("ui.renderer.style.ColorThemingPanel.cannot-colour-theme-layer-with-no-attributes")),
                    attributeNameComboBox.getItemCount() == 0)) {
                setErrorMessage(new ErrorMessage(
                    I18N.get("ui.renderer.style.ColorThemingPanel.table-must-not-be-empty")),
                    table.getRowCount() == 0);
            }

            updateErrorDisplay();

            if (table.getColumnCount() > 0) {
                //Column count == 0 during initialization. [Jon Aquino]
                table.getColumnModel()
                     .getColumn(table.convertColumnIndexToView(
                        ColorThemingTableModel.ATTRIBUTE_COLUMN))
                     .setHeaderValue(state.getAttributeValueColumnTitle());
            }
        } finally {
            updatingComponents = false;
        }
    }

    /**
     * @return null if user hits Cancel
     */
    private BasicStyle promptBasicStyle(BasicStyle basicStyle) {
        int originalTransparencySliderValue = transparencySlider.getValue();
        int originalLineWidthSliderValue = lineWidthSlider.getValue();
        deeRenderingStylePanel.setBasicStyle(basicStyle);
        deeRenderingStylePanel.getTransparencySlider().setValue(originalTransparencySliderValue);
        deeRenderingStylePanel.getLineWidthSlider().setValue(originalLineWidthSliderValue);
        //deeRenderingStylePanel.updateControls((XBasicStyle)basicStyle);

        OKCancelPanel okCancelPanel = new OKCancelPanel();
        final JDialog dialog = new JDialog((JDialog) SwingUtilities.windowForComponent(
                    this), I18N.get("ui.renderer.style.ColorThemingPanel.custom"), true);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(deeRenderingStylePanel, BorderLayout.CENTER);
        dialog.getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
        okCancelPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
        dialog.pack();
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);

        if (!okCancelPanel.wasOKPressed()) {
            transparencySlider.setValue(originalTransparencySliderValue);
            lineWidthSlider.setValue(originalLineWidthSliderValue);
        }

        return okCancelPanel.wasOKPressed() ?
                new XBasicStyle(deeRenderingStylePanel.getBasicStyle(),
                        deeRenderingStylePanel.getVertexStyle())
                : null;
    }

    private void initTable(Layer layer) {
        BasicStyle defaultStyle = colorThemingStyleHasRanges(getLayer()) ?
            ColorThemingStyle.get(layer).getDefaultStyle():
            new XBasicStyle(layer.getBasicStyle(), layer.getVertexStyle());
        assert defaultStyle instanceof XBasicStyle : "defaultStyle is not an instance of XBasicStyle";
        table.setModel(new ColorThemingTableModel(defaultStyle,
                ColorThemingStyle.get(layer).getAttributeName(),
                attributeValueToBasicStyleMap(layer),
                attributeValueToLabelMap(layer),                
                layer.getFeatureCollectionWrapper().getFeatureSchema()) {
                public Object getValueAt(int rowIndex, int columnIndex) {
                    //Don't use #attributeColumn() here because this is in the *model*,
                    //not the view. Otherwise, an exception will be thrown when
                    //the user tries to move the columns. [Jon Aquino]
                    if ((rowIndex == 0) &&
                            (columnIndex == ColorThemingTableModel.ATTRIBUTE_COLUMN)) {
                        return state.getAllOtherValuesDescription();
                    }
                    if ((rowIndex == 0) &&
                            (columnIndex == ColorThemingTableModel.LABEL_COLUMN)) {
                        return "";
                    }
                    return super.getValueAt(rowIndex, columnIndex);
                }
            });
        table.createDefaultColumnsFromModel();
        table.setRowSelectionAllowed(true);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateComponents();
                }
            });
        table.getTableHeader().addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) &&
                            (e.getClickCount() == 1) && table.isEnabled()) {
                        tableModel().sort();
                    }
                }
            });
        table.getColumnModel().getColumn(colorColumn()).setCellRenderer(new TableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                    JComponent renderer = (JComponent) basicStyleListCellRenderer.getListCellRendererComponent(new JList(),
                            value, row, isSelected, hasFocus);

                    if (!isSelected) {
                        updateBackground(renderer);
                    }

                    return renderer;
                }
            });
        table.getColumnModel().getColumn(colorColumn()).setCellEditor(basicStyleTableCellEditor);
        table.getModel().addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    updateComponents();

                    Object duplicateAttributeValue = tableModel()
                                                         .findDuplicateAttributeValue();
                    setErrorMessage(new ErrorMessage(
                            I18N.get("ui.renderer.style.ColorThemingPanel.table-must-not-have-duplicate-attribute-values"),
                            "(" + duplicateAttributeValue + ")"),
                        duplicateAttributeValue != null);
                    setErrorMessage(new ErrorMessage(
                            I18N.get("ui.renderer.style.ColorThemingPanel.table-must-not-have-null-attribute-values")),
                        tableModel().containsNullAttributeValues());
                }
            });

        int colorWidth = 10 +
            (int) basicStyleListCellRenderer.getListCellRendererComponent(new JList(),
                layer.getBasicStyle(), 0, false, false).getPreferredSize().getWidth();
        table.getColumnModel().getColumn(colorColumn()).setPreferredWidth(colorWidth);
        table.getColumnModel().getColumn(colorColumn()).setMinWidth(colorWidth);
        table.getColumnModel().getColumn(colorColumn()).setMaxWidth(colorWidth);
    }

    private Map attributeValueToBasicStyleMap(Layer layer) {
        if (!colorThemingAttributeValid(layer)) {
            return new TreeMap();
        }
        return state.fromExternalFormat(ColorThemingStyle.get(layer)
                                                         .getAttributeValueToBasicStyleMap());
    }
    
    private Map attributeValueToLabelMap(Layer layer) {
        if (!colorThemingAttributeValid(layer)) {
            return new TreeMap();
        }
        return state.fromExternalFormat(ColorThemingStyle.get(layer)
                                                         .getAttributeValueToLabelMap());
    }
    
    private boolean colorThemingAttributeValid(Layer layer) {
        if (ColorThemingStyle.get(layer).getAttributeName() == null) { return false;}
        //Schema won't have attribute name if user has deleted the attribute [Jon Aquino]
        return layer.getFeatureCollectionWrapper().getFeatureSchema()
          .hasAttribute(ColorThemingStyle.get(layer).getAttributeName());
    }

    private void initColorSchemeComboBox(LayerManager layerManager) {
        colorSchemeComboBox.setRenderer(new ColorSchemeListCellRenderer() {
                protected void color(ColorPanel colorPanel, Color fillColor,
                    Color lineColor) {
                    super.color(colorPanel,
                        GUIUtil.alphaColor(fillColor, getAlpha()),
                        GUIUtil.alphaColor(lineColor, getAlpha()));
                }

                protected ColorScheme colorScheme(String name) {
                    return state.filterColorScheme(super.colorScheme(name));
                }
            });
    }

    private int getAlpha() {
        return transparencySlider.getMaximum() - transparencySlider.getValue();
    }

    private int getLineWidth() {
        return lineWidthSlider.getValue();
    }

    private boolean isVertexStyleEnabled() {
        return vertexStyleEnableCheckBox.isSelected();
    }

    private void initAttributeNameComboBox(Layer layer) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            if (i == schema.getGeometryIndex()) {
                continue;
            }
            // [mmichaud 2013-03-07] exclude R_G_B attribute which is reserved to
            // interactive color setting
            else if (schema.getAttributeName(i).equals(BasicStyle.RGB_ATTRIBUTE_NAME)) {
                continue;
            }
            String attributeName = schema.getAttributeName(i);
            model.addElement(attributeName);
        }
        attributeNameComboBox.setModel(model);
        // At least one attribute different from Geometry
        if (model.getSize() > 0) {
            if (model.getIndexOf(lastAttributeName) >= 0) {
                attributeNameComboBox.setSelectedItem(lastAttributeName);
            } else if (ColorThemingStyle.get(layer).getAttributeName() == null) {
                attributeNameComboBox.setSelectedIndex(0);
                lastAttributeName = getAttributeName();
            } else {
                attributeNameComboBox.setSelectedItem(
                        ColorThemingStyle.get(layer).getAttributeName());
                lastAttributeName = getAttributeName();
            }
        }
    }

    private void jbInit() {
        this.setLayout(new GridBagLayout());
        jPanel1.setLayout(new GridBagLayout());
        jPanel2.setLayout(new GridBagLayout());
        enableColorThemingCheckBox.setText(I18N.get("ui.renderer.style.ColorThemingPanel.enable-colour-theming"));
        attributeLabel.setText(I18N.get("ui.renderer.style.ColorThemingPanel.attribute")+" ");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusLabel.setText(" ");
        colorSchemeLabel.setText(I18N.get("ui.renderer.style.ColorThemingPanel.colour-scheme")+" ");
        attributeNameComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    attributeNameComboBox_actionPerformed(e);
                }
            });
        colorSchemeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    colorSchemeComboBox_actionPerformed(e);
                }
            });
        enableColorThemingCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (enableColorThemingCheckBox.isSelected()) {
                        enableColorThemingCheckBox_actionPerformed(e);
                    } else {
                        layer.getVertexStyle().setEnabled(
                                ColorThemingStyle.get(layer).isVertexStyleEnabled());
                    }
                    //ColorThemingStyle.get(layer).setVertexStyleEnabled(layer.getVertexStyle().isEnabled());
                }
            });

		initClassificationComboBox(getAttributeType());
        transparencyLabel.setText(I18N.get("ui.style.BasicStylePanel.transparency"));
        transparencySlider.setMaximum(255);
        transparencySlider.setPreferredSize(SLIDER_DIMENSION);
        //Don't get squished by overlong status messages. [Jon Aquino]
        transparencySlider.setMinimumSize(SLIDER_DIMENSION);
        transparencySlider.setToolTipText(I18N.get("ui.renderer.style.ColorThemingPanel.transparency"));
        transparencySlider.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    transparencySlider_stateChanged(e);
                }
            });

        lineWidthLabel.setText(I18N.get("ui.style.BasicStylePanel.line-width"));
        lineWidthSlider.setMajorTickSpacing(5);
        lineWidthSlider.setMinorTickSpacing(1);
        lineWidthSlider.setMaximum(30);
        lineWidthSlider.setPreferredSize(SLIDER_DIMENSION);

        //Don't get squished by overlong status messages. [Jon Aquino]
        lineWidthSlider.setMinimumSize(SLIDER_DIMENSION);
        //lineWidthSlider.setToolTipText(I18N.get("ui.style.BasicStylePanel.line-width"));
        lineWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                lineWidthSlider_stateChanged(e);
            }
        });

        vertexStyleEnableCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                vertexStyleEnabled_stateChanged(e);
            }
        });

        this.add(jPanel1,
            new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        // add enableColorThemingCheckBox in row 0
        jPanel1.add(enableColorThemingCheckBox,
            new GridBagConstraints(0, 0, 4, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        jPanel1.add(attributeLabel,
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        // add enableColorThemingCheckBox in row 1
        jPanel1.add(attributeNameComboBox,
            new GridBagConstraints(1, 1, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        //jPanel1.add(fillerPanel,
        //    new GridBagConstraints(3, 0, 1, 3, 1, 0.0,
        //        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        //        new Insets(2, 2, 2, 2), 0, 0));
        // add classification method in row 2
        jPanel1.add(classificationLabel, 
        		new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
        		GridBagConstraints.WEST, GridBagConstraints.NONE, 
        		new Insets(2, 2, 2, 2), 0, 0));  
		jPanel1.add(classificationComboBox, 
				new GridBagConstraints(1, 2, 3, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 2, 2), 0, 0));
		// let row 3 for range count options in case of classification by range
        // add enableColorThemingCheckBox in row 4
        jPanel1.add(colorSchemeLabel,
            new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        jPanel1.add(colorSchemeComboBox,
            new GridBagConstraints(1, 4, 3, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 0, 0));
        
        this.add(scrollPane,
            new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 2), 0, 0));

        this.add(jPanel2,
            new GridBagConstraints(0, 5, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(statusLabel,
            new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(toolBar,
            new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(transparencyLabel,
                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(transparencySlider,
            new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(GUIUtil.createSyncdTextField(transparencySlider, SLIDER_TEXT_FIELD_COLUMNS),
                new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(lineWidthLabel,
                new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(lineWidthSlider,
                new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(GUIUtil.createSyncdTextField(lineWidthSlider, SLIDER_TEXT_FIELD_COLUMNS),
                new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(vertexStyleLabel,
                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        jPanel2.add(vertexStyleEnableCheckBox,
                new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));

        scrollPane.getViewport().add(table);
    }

    protected void enableColorThemingCheckBox_actionPerformed(ActionEvent e) {
        if (table.getRowCount() == 1) {
            //1 == just the "All other values" row. [Jon Aquino]
            populateTable();
        }
        updateComponents();
    }

    void attributeNameComboBox_actionPerformed(ActionEvent e) {
        try {
            if (initializing) {
                return;
            }
            if (attributeNameComboBox.getItemCount() == 0) {
                return;
            }
            Object selectedItem = attributeNameComboBox.getSelectedItem();
            if (selectedItem != null && selectedItem.equals(lastAttributeName)) {
                //Don't want to clear the table if the attribute hasn't changed. [Jon Aquino]
                return;
            }
            initClassificationComboBox(getAttributeType());
            updateComponents();
            stopCellEditing();
            populateTable();
        } finally {
            //Want to set lastAttributeName even during initialization. Otherwise, the following
            //undesirable behaviour occurs: Open dialog box; press Get All Values
            // -- the table is populated; edit the table; click on attribute combo box but click
            //on the same attribute -- the table is repopulated! [Jon Aquino]
            lastAttributeName = getAttributeName();
            if (table.getModel() instanceof ColorThemingTableModel) {
                //Model will be DefaultTableModel during early initialization. [Jon Aquino]
                tableModel().setAttributeName(getAttributeName());
            }
        }
    }

    public ColorThemingTableModel tableModel() {
        return (ColorThemingTableModel) table.getModel();
    }

    private SortedSet<Object> getNonNullAttributeValues() {
        TreeSet<Object> values = new TreeSet<Object>();
        for (Object obj : layer.getFeatureCollectionWrapper().getFeatures()) {
            Feature feature = (Feature)obj;
            if (feature.getAttribute(getAttributeName()) != null) {
                values.add(ColorThemingStyle.trimIfString(feature.getAttribute(getAttributeName())));
            }
        }
        return values;
    }

    public void populateTable() {
        if (!(enableColorThemingCheckBox.isSelected() &&
             (attributeNameComboBox.getItemCount() > 0))) {
            return;
        }
        stopCellEditing();
        tableModel().clear();
        Collection<Object> filteredAttributeValues = filteredAttributeValues();
        tableModel().setMaps(
                toAttributeValueToBasicStyleMap(filteredAttributeValues),
                toAttributeValueToLabelMap(filteredAttributeValues));
        tableModel().sort(tableModel().wasLastSortAscending());
        applyColorScheme();
    }

    private Collection<Object> filteredAttributeValues() {
        return state.filterAttributeValues(getNonNullAttributeValues());
    }
    
    private Map<Object,String> toAttributeValueToLabelMap(Collection<Object> attributeValues) {
        Map<Object,Object> attributeValueToValueMap = new TreeMap<Object,Object>();
        for (Object value : attributeValues) {
            attributeValueToValueMap.put(value, value);
        }
        attributeValueToValueMap =
                (Map<Object,Object>)CollectionUtil.inverse(state
                        .toExternalFormat(attributeValueToValueMap));
        Map<Object,String> attributeValueToLabelMap = new HashMap<Object,String>();
        for (Object value : attributeValueToValueMap.keySet()) {
            attributeValueToLabelMap.put(value, attributeValueToValueMap.get(value).toString());
        }
        return attributeValueToLabelMap;
    }
    
    private Map<Object,BasicStyle> toAttributeValueToBasicStyleMap(Collection<Object> attributeValues) {
        Map<Object,BasicStyle> attributeValueToBasicStyleMap = new TreeMap<Object,BasicStyle>();
        BasicStyle style = new XBasicStyle(getLayer().getBasicStyle(), getLayer().getVertexStyle());
        for (Object value : attributeValues) {
            attributeValueToBasicStyleMap.put(value, style);
        }
        return attributeValueToBasicStyleMap;
    }    

    void colorSchemeComboBox_actionPerformed(ActionEvent e) {
        if (initializing) {
            return;
        }
        stopCellEditing();
        layer.getLayerManager().getBlackboard().put(COLOR_SCHEME_KEY,
            colorSchemeComboBox.getSelectedItem());
        applyColorScheme();
        colorSchemeForInserts = null;
    }

    private ColorScheme getColorSchemeForInserts() {
        //Cache the colour-scheme; otherwise the first color will always
        //be used for inserts. [Jon Aquino]
        if ((colorSchemeForInserts == null) ||
                !colorSchemeForInserts.getName().equalsIgnoreCase((String) colorSchemeComboBox.getSelectedItem())) {
            colorSchemeForInserts = ColorScheme.create((String) colorSchemeComboBox.getSelectedItem());
        }

        return colorSchemeForInserts;
    }

    public void applyColorScheme() {
        stopCellEditing();
        state.applyColorScheme(state.filterColorScheme(ColorScheme.create(
                (String)colorSchemeComboBox.getSelectedItem())));
    }

    private void cancelCellEditing() {
        if (table.getCellEditor() instanceof DefaultCellEditor) {
            table.getCellEditor().cancelCellEditing();
        }
    }

    public String validateInput() {
        stopCellEditing();

        //Split into #validateInput and #internalValidateInput to avoid
        //infinite recursion (#stopCellEditing causes #fireTableChanged
        //which causes #validateInput which causes #stopCellEditing...)
        //[Jon Aquino]
        return internalValidateInput();
    }

    private String internalValidateInput() {
        if (!enableColorThemingCheckBox.isSelected()) {
            return null;
        }
        if (errorMessages.isEmpty()) {
            return null;
        }
        return errorMessages.iterator().next().toString();
    }

    /**
     * @return enabled
     */
    private boolean setErrorMessage(ErrorMessage message, boolean enabled) {
        //Always remove the original, because #add doesn't replace, but is
        //ignored if the message already exists. Need to replace because the
        //"specific part" of the error message may be new. [Jon Aquino]
        errorMessages.remove(message);
        if (enabled) {
            errorMessages.add(message);
        }
        updateErrorDisplay();
        return enabled;
    }

    private void updateErrorDisplay() {
        String errorMessage = internalValidateInput();
        if (errorMessage != null) {
            statusLabel.setText(errorMessage);
            statusLabel.setIcon(GUIUtil.toSmallIcon(IconLoader.icon(
                        "Delete.gif")));
        } else {
            statusLabel.setText(" ");
            statusLabel.setIcon(null);
        }
    }

    private void setState(State state) {
        this.state.deactivate();
        jPanel1.remove(this.state.getPanel());

        //Need to call #revalidate; otherwise, the component won't get
        //removed. [Jon Aquino]
        this.state = state;
        jPanel1.add(state.getPanel(),
            new GridBagConstraints(0, 3, 4, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        initializing = true;

        try {
            colorSchemeComboBox.setModel(new DefaultComboBoxModel<String>(
                    new Vector<String>(state.getColorSchemeNames())));
            //colorSchemeComboBox.setPrototypeDisplayValue("01234567890123456789012345678901");
            //The colour scheme may not be present if it was taken from the
            //set of discrete colour schemes but the combo box currently contains
            //the range colour schemes. [Jon Aquino]
            colorSchemeComboBox.setSelectedItem(layer.getLayerManager()
                                                     .getBlackboard().get(COLOR_SCHEME_KEY,
                    colorSchemeComboBox.getItemAt(0)));
        } finally {
            initializing = false;
        }

        updateComponents();
        this.state.activate();
        jPanel1.repaint();
    }

    void transparencySlider_stateChanged(ChangeEvent e) {
        repaint();
    }

    void lineWidthSlider_stateChanged(ChangeEvent e) {
        repaint();
    }

    void vertexStyleEnabled_stateChanged(ItemEvent e) {
        for (Object obj : tableModel().getAttributeValueToBasicStyleMap().values()) {
            if (obj instanceof XBasicStyle) {
                ((XBasicStyle)obj).getVertexStyle().setEnabled(vertexStyleEnableCheckBox.isSelected());
                // If vertex style is selected, setRenderingVertice is deactivated and the opposite
                ((XBasicStyle)obj).setRenderingVertices(!vertexStyleEnableCheckBox.isSelected());
            }
        }
        if (tableModel().getDefaultStyle() instanceof XBasicStyle) {
            ((XBasicStyle)tableModel().getDefaultStyle())
                    .getVertexStyle()
                    .setEnabled(vertexStyleEnableCheckBox.isSelected());
            tableModel().getDefaultStyle()
                    .setRenderingVertices(!vertexStyleEnableCheckBox.isSelected());
        }
        //repaint();
    }

    public JSlider getTransparencySlider() {
        return transparencySlider;
    }

    public JSlider getLineWidthSlider() {
        return lineWidthSlider;
    }

    public JTable getTable() {
        return table;
    }

    public static interface State {
        public String getAllOtherValuesDescription();

        public ColorScheme filterColorScheme(ColorScheme colorScheme);

        public void activate();

        public void deactivate();

        public Collection<String> getColorSchemeNames();

        public void applyColorScheme(ColorScheme scheme);

        public Collection<Object> filterAttributeValues(SortedSet<Object> attributeValues);

        public String getAttributeValueColumnTitle();

        public JComponent getPanel();

        /**
         * Performs any necessary modifications to the map before applying
         * it to the layer.
         */
        public Map toExternalFormat(Map attributeValueToObjectMap);

        public Map fromExternalFormat(Map attributeValueToObjectMap);
    }

    private abstract class MyPlugIn extends AbstractPlugIn {
        public abstract Icon getIcon();
    }

    // TODO this class does not depend on this package, it could be moved to
    // com.vividsolutions.jump.util
    private class ErrorMessage {
        final private String commonPart;
        final private String specificPart;

        public ErrorMessage(String commonPart) {
            this(commonPart, "");
        }

        public ErrorMessage(String commonPart, String specificPart) {
            this.commonPart = commonPart;
            this.specificPart = specificPart;
        }

        public int hashCode() {
            return commonPart.hashCode();
        }

        public boolean equals(Object obj) {
            return obj instanceof ErrorMessage &&
                    commonPart.equals(((ErrorMessage) obj).commonPart);
        }

        public String toString() {
            return commonPart + specificPart;
        }
    }

    //-- obedel [12/10/2005]
    // return an ordered map (value -> count of corresponding features)
    // Erwan Bocher [20/01/2005]
    // Add button to calculate range and populateTable
    public SortedMap<Object,Integer> getAttributeValuesCount() {
        TreeMap<Object,Integer> values = new TreeMap<Object,Integer>();

        for (Object obj : layer.getFeatureCollectionWrapper().getFeatures()) {
            Feature feature = (Feature)obj;

            if (feature.getAttribute(getAttributeName()) != null) {
                Object key = ColorThemingStyle.trimIfString(feature.getAttribute(getAttributeName()));
            	Integer count = values.get(key);
                if (count == null)
                	values.put(key, new Integer(1));
                else
                	values.put(key, new Integer(count.intValue()+1));
            }
        }
        return values;
    }
    
	
    private class ActionClassification implements ItemListener {
		public void itemStateChanged(ItemEvent arg0) {
			switch (classificationComboBox.getSelectedIndex()) {
			case UNIQUE_VALUE:
				setState(discreteColorThemingState);
				break;
			case EQUAL_INTERVAL:
			    setState(rangeColorThemingState);
				break;
			case QUANTILE: //i.e. equal number
				    setState(quantileColorThemingState);
				break;
			case MEAN_STDEV: 
				    setState(meanSTDevColorThemingState);
				break;
			case MAX_BREAKS:
				    setState(maxBreaksColorThemingState);
				break;
			case JENKS:
				    setState(jenksColorThemingState);
				break;
			}
			populateTable();
			applyColorScheme();
		}
	}

    final private ActionClassification classificationListener = new ActionClassification();

	private static final int UNIQUE_VALUE = 0;

	private static final int EQUAL_INTERVAL = 1;

	private static final int QUANTILE = 2;
	
	private static final int MEAN_STDEV = 3;
	
	private static final int MAX_BREAKS = 4;
	
	private static final int JENKS = 5;

	private void initClassificationComboBox(AttributeType type) {
	    classificationComboBox.removeItemListener(classificationListener);
	    classificationComboBox.removeAllItems();
		classificationComboBox.addItem(I18N.get("ui.renderer.style.ColorThemingStylePanel.Unique-value"));
		if (type == AttributeType.INTEGER || type == AttributeType.DOUBLE) {
		    classificationComboBox.addItem(I18N.get("ui.renderer.style.ColorThemingStylePanel.Equal-Interval"));
		    classificationComboBox.addItem(I18N.get("ui.renderer.style.ColorThemingStylePanel.Quantile-Equal-Number"));
		    // -- [sstein 15.Feb.2009]
		    classificationComboBox.addItem(I18N.get("ui.renderer.style.ColorThemingStylePanel.Mean-Standard-Deviation"));
		    classificationComboBox.addItem(I18N.get("ui.renderer.style.ColorThemingStylePanel.Maximal-Breaks"));
		    classificationComboBox.addItem(I18N.get("ui.renderer.style.ColorThemingStylePanel.Jenks-Optimal-Method"));
		}
		classificationComboBox.addItemListener(classificationListener);
		if (classificationComboBox.getItemCount() == 1 &&
		    (state != discreteColorThemingState)) {
		    setState(discreteColorThemingState);
		}
	}

}
