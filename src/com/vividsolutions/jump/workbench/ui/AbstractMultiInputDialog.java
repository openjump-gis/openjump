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
package com.vividsolutions.jump.workbench.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;

/**
 * Flexible generic dialog to prompt the user typing input parameters. This
 * abstract class includes the logic from original JUMP MultiInputDialog but
 * does not layout components, which is done by implementing classes.
 * 
 * @author Micha&euml;l Michaud
 */
public abstract class AbstractMultiInputDialog extends JDialog {

    /** The label of the control is not displayed. */
    public static final int NO_LABEL = 0;

    /** The label is displayed on the left of the control. */
    public static final int LEFT_LABEL = 1;

    /** The label is displayed on the right of the control. */
    public static final int RIGHT_LABEL = 2;

    /** Flag indicating that the main component will fill the space. */
    public static final int HORIZONTAL = 2;

    /** Flag indicating that the main component will fill the space vertically. */
    public static final int VERTICAL = 3;

    /** Flag indicating that the main component will fill the space. */
    public static final int BOTH = 1;

    /**
     * Flag indicating that the main component will always use its preferred
     * size.
     */
    public static final int NONE = 0;

    /** Attribute combobox message displayed if no valid attribute is available. */
    public static final String NO_VALID_ATTRIBUTE = I18N
            .get("ui.MultiInputDialog.no-valid-attribute");

    /**
     * @param frame
     *            the frame on which to make this dialog modal and centred
     * @param title
     *            the title of the dialog box
     * @param modal
     *            set if the dialog box is modal or not
     */
    protected AbstractMultiInputDialog(final Frame frame, String title,
            boolean modal) {
        super(frame, title, modal);
    }

    // //////////////////////////////////////////////////////////////////////////
    // //
    // CREATE ENABLE CHECKS //
    // //
    // //////////////////////////////////////////////////////////////////////////

    /** Check if the control contains a valid Double. */
    public EnableCheck createDoubleCheck(final String fieldName) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                try {
                    Double.parseDouble(getText(fieldName).trim());
                    return null;
                } catch (final NumberFormatException e) {
                    return "\""
                            + getText(fieldName).trim()
                            + "\" "
                            + I18N.get("ui.MultiInputDialog.is-an-invalid-double")
                            + " (" + fieldName + ")";
                }
            }
        };
    }

    /** Check if the control contains a valid Integer. */
    public EnableCheck createIntegerCheck(final String fieldName) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                try {
                    Integer.parseInt(getText(fieldName).trim());
                    return null;
                } catch (final NumberFormatException e) {
                    return "\""
                            + getText(fieldName).trim()
                            + "\" "
                            + I18N.get("ui.MultiInputDialog.is-an-invalid-integer")
                            + " (" + fieldName + ")";
                }
            }
        };
    }

    /** Check if the control contains a positive Number. */
    public EnableCheck createPositiveCheck(final String fieldName) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                if (Double.parseDouble(getText(fieldName).trim()) > 0) {
                    return null;
                }
                return "\"" + getText(fieldName).trim() + "\" "
                        + I18N.get("ui.MultiInputDialog.must-be") + " > 0"
                        + " (" + fieldName + ")";
            }
        };
    }

    /** Check if the control contains a non-negative Number. */
    public EnableCheck createNonNegativeCheck(final String fieldName) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                if (Double.parseDouble(getText(fieldName).trim()) >= 0) {
                    return null;
                }
                return "\"" + getText(fieldName).trim() + "\" "
                        + I18N.get("ui.MultiInputDialog.must-be") + " >= 0"
                        + " (" + fieldName + ")";
            }
        };
    }

    // //////////////////////////////////////////////////////////////////////////
    // //
    // MAPPINGS //
    // //
    // //////////////////////////////////////////////////////////////////////////

    // Map containing associations between field names and their component
    private final HashMap<String, Component> fieldNameToComponentMap = new HashMap<String, Component>();

    // Map containing associations between field names and their label
    private final HashMap<String, Component> fieldNameToLabelMap = new HashMap<>();

    // Map containing associations between field names and ButtonGroup
    private final Map buttonGroupMap = new HashMap();

    // Map containing associations between field names and EnableChecks
    private final CollectionMap fieldNameToEnableCheckListMap = new CollectionMap();

    protected void addComponent(String fieldName, Component label,
            Component component) {
        if (label != null) {
            fieldNameToLabelMap.put(fieldName, label);
        }
        if (component != null) {
            fieldNameToComponentMap.put(fieldName, component);
        }
    }

    protected Component getComponent(String fieldName) {
        return fieldNameToComponentMap.get(fieldName);
    }

    // //////////////////////////////////////////////////////////////////////////
    // //
    // COMPONENT GETTERS //
    // //
    // //////////////////////////////////////////////////////////////////////////

    /**
     * Gets JLabel matching this fieldName.
     */
    public JLabel getLabel(String fieldName) {
        return (JLabel) fieldNameToLabelMap.get(fieldName);
    }

    /**
     * Gets JComboBox component matching this fieldName.
     */
    public JComboBox getComboBox(String fieldName) {
        return (JComboBox) getComponent(fieldName);
    }

    /**
     * Gets JCheckBox component matching this fieldName.
     */
    public JCheckBox getCheckBox(String fieldName) {
        return (JCheckBox) getComponent(fieldName);
    }

    /**
     * Gets JRadioButton component matching this fieldName.
     */
    public JRadioButton getRadioButton(String fieldName) {
        return (JRadioButton) getComponent(fieldName);
    }

    /**
     * Gets JFileChooser component matching this fieldName.
     */
    public JFileChooser getFileChooser(String fieldName) {
        return (JFileChooser) getComponent(fieldName);
    }

    // //////////////////////////////////////////////////////////////////////////
    // //
    // VALUE GETTERS //
    // //
    // //////////////////////////////////////////////////////////////////////////

    /**
     * Fetch the current value held by a component given. Semi intelligent it
     * returns - String text for text components and scrollpanes - selected
     * Objects for combo boxes - Boolean states for buttons and checkboxes -
     * List of files for file choosers
     * 
     * @param component
     * @return object
     */
    public Object getValue(Component component) {
        // textfield, textarea
        if (component instanceof JTextComponent) {
            return ((JTextComponent) component).getText();
        }
        // combobox
        if (component instanceof JComboBox) {
            final Object selObj = ((JComboBox) component).getSelectedItem();
            return selObj;
        }
        // scrollpane
        if (component instanceof JScrollPane) {
            component = ((JScrollPane) component).getViewport().getView();
            return getValue(component);
        }
        // button
        if (component instanceof JButton) {
            final boolean state = ((JButton) component).isEnabled();
            return new Boolean(state);
        }
        // radiobutton, checkbox
        if (component instanceof JToggleButton) {
            final boolean state = ((JToggleButton) component).isSelected();
            return new Boolean(state);
        }
        // filechooser
        if (component instanceof JFileChooser) {
            final JFileChooser fc = ((JFileChooser) component);
            File[] files;
            if (fc.isMultiSelectionEnabled()) {
                files = fc.getSelectedFiles();
            } else {
                final File file = fc.getSelectedFile();
                files = file == null ? new File[0] : new File[] { file };
            }

            return Arrays.asList(files);
        }

        return null;
    }

    /**
     * Convenience method for {@link #getValue(Component)}
     * 
     * @param fieldName
     * @return object
     */
    public Object getValue(String fieldName) {
        final Component component = fieldNameToComponentMap.get(fieldName);
        if (component == null) {
            Logger.error("No component labelled '" + fieldName
                    + "' in this dialog.");
            return null;
        }

        return getValue(component);
    }

    /**
     * Gets the string value of a control
     * 
     * @param fieldName
     *            control to read
     * @return the string value of the control
     */
    public String getText(String fieldName) {
        final Object value = getValue(fieldName);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }

    /**
     * Returns selected state for checkboxes, radio buttons.
     */
    public boolean getBoolean(String fieldName) {
        return (Boolean) getValue(fieldName);
    }

    /**
     * Returns double value from a JTextField control.
     */
    public double getDouble(String fieldName) {
        return Double.parseDouble(getValue(fieldName).toString().trim());
    }

    /**
     * Returns integer value from a JTextField control.
     */
    public int getInteger(String fieldName) {
        return Integer.parseInt(getValue(fieldName).toString().trim());
    }

    /**
     * Returns a Layer from a control.
     */
    public Layer getLayer(String fieldName) {
        return (Layer) getValue(fieldName);
    }

    /**
     * Returns a RasterImageLayer from a control.
     */
    public RasterImageLayer getRasterLayer(String fieldName) {
        return (RasterImageLayer) getValue(fieldName);
    }

    /**
     * Returns a Layerable from a control.
     */
    public Layerable getLayerable(String fieldName) {
        return (Layerable) getValue(fieldName);
    }

    /**
     * Returns a File Collection from a JFilechooser control.
     */
    public List getFiles(String fieldName) {
        return (List) getValue(fieldName);
    }

    // //////////////////////////////////////////////////////////////////////////
    // //
    // ADD ENABLE CHECKS //
    // //
    // //////////////////////////////////////////////////////////////////////////

    /**
     * Adding enableChecks to the fieldNameToEnableCheckListMap CollectionMap.
     * 
     * @param fieldName
     *            fieldName of the control
     * @param enableChecks
     *            EnableCheck array to validate this control input
     */
    public void addEnableChecks(String fieldName,
            Collection<? extends EnableCheck> enableChecks) {
        fieldNameToEnableCheckListMap.addItems(fieldName, enableChecks);
    }

    public void addEnableChecks(String fieldName, EnableCheck... enableChecks) {
        fieldNameToEnableCheckListMap.addItems(fieldName,
                Arrays.asList(enableChecks));
    }

    // //////////////////////////////////////////////////////////////////////////
    // //
    // ADD COMPONENTS //
    // //
    // //////////////////////////////////////////////////////////////////////////

    /**
     * Adds a JTextField control to this dialog.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @param enableChecks
     *            checks to validate the input value
     * @param toolTipText
     *            tool tip to help the user
     * @return the JTextField control added to this dialog
     */
    public JTextField addTextField(String fieldName, String initialValue,
            int approxWidthInChars, EnableCheck[] enableChecks,
            String toolTipText) {
        final JTextField textField = new JTextField(initialValue,
                approxWidthInChars);
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4247013
        textField.setMinimumSize(textField.getPreferredSize());
        addRow(fieldName, new JLabel(fieldName), textField, enableChecks,
                toolTipText, LEFT_LABEL, HORIZONTAL);
        return textField;
    }

    /**
     * Adds a JComboBox control to this dialog.
     * 
     * @param fieldName
     *            field name of the control
     * @param selectedItem
     *            initial selected item
     * @param items
     *            items displayed in the JComboBox
     * @param toolTipText
     *            tool tip to help the user
     * @return the JComboBox control added to this dialog
     */
    public <T> JComboBox<T> addComboBox(String fieldName, Object selectedItem,
            Collection<T> items, String toolTipText) {
        final JComboBox<T> comboBox = new JComboBox<>(new Vector<>(items));
        comboBox.setSelectedItem(selectedItem);
        addRow(fieldName, new JLabel(fieldName), comboBox, null, toolTipText,
                LEFT_LABEL, NONE);
        return comboBox;
    }

    /**
     * Adds a JLabel to this dialog. The label can contain html (e.g.
     * "<html><b>Bold label for a subtitle</b></html>")
     * 
     * @param text
     *            text to display in the JLabel
     * @return the JLabel added to this dialog
     */
    public JLabel addLabel(String text) {
        // Take advantage of #addRow's special rule for JLabels: they span all
        // the columns of the GridBagLayout. [Jon Aquino]
        final JLabel lbl = new JLabel(text);
        addRow(lbl);
        return lbl;
    }

    /**
     * Adds a JLabel to display a subtitle in the dialog.
     * 
     * @param text
     *            text to display in the JLabel
     * @return the JLabel added to this dialog
     */
    // added by mmichaud
    public JLabel addSubTitle(String text) {
        // Take advantage of #addRow's special rule for JLabels: they span all
        // the columns of the GridBagLayout. [Jon Aquino]
        final JLabel lbl = new JLabel("<html><b>" + text + "</b></html>");
        addRow(lbl);
        return lbl;
    }

    /**
     * Adds a JButton to this dialog. Action associated to this JButton must be
     * defined.
     * 
     * @param fieldName
     *            will be used for the label text on the left of the button
     * @param text
     *            text to display in the JButton
     * @param toolTipText
     *            tooltip text associated to the JButton
     * @return the JButton added to this dialog
     */
    public JButton addButton(String fieldName, String text, String toolTipText) {
        final JButton button = new JButton(text);
        addRow(fieldName, new JLabel(fieldName), button, null, toolTipText,
                LEFT_LABEL, NONE);
        return button;
    }

    /**
     * Adds a JButton to this dialog. Action associated to this JButton must be
     * defined.
     * 
     * @param text
     *            text to display in the JButton
     * @return the JButton added to this dialog
     */
    public JButton addButton(String text) {
        // Take advantage of #addRow's special rule for JLabels: they span all
        // the columns of the GridBagLayout. [Jon Aquino]
        final JButton button = new JButton(text);
        addRow("DUMMY", new JLabel(""), button, null, null, LEFT_LABEL, NONE);
        return button;
    }

    /**
     * Adds a horizontal separator between two set of controls.
     */
    public void addSeparator() {
        final JPanel separator = new JPanel();
        separator.setBackground(Color.black);
        // Setting the minimum size avoid to get a wide black line
        // when the dialog box is schrinked to a too small width.
        separator.setMinimumSize(new Dimension(1, 1));
        separator.setPreferredSize(new Dimension(1, 1));
        addRow(separator);
    }

    /**
     * Adds a JTextField control for numeric inputs. Values input in the
     * JTextField control are right aligned.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @param enableChecks
     *            checks to validate the input value
     * @param toolTipText
     *            tool tip to help the user
     * @return the JTextField control added to this dialog
     */
    private JTextField addNumericField(String fieldName, String initialValue,
            int approxWidthInChars, EnableCheck[] enableChecks,
            String toolTipText) {
        final JTextField textField = new JTextField(initialValue,
                approxWidthInChars);
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4247013
        textField.setMinimumSize(textField.getPreferredSize());
        textField.setHorizontalAlignment(JTextField.RIGHT);
        addRow(fieldName, new JLabel(fieldName), textField, enableChecks,
                toolTipText, LEFT_LABEL, NONE);
        return textField;
    }

    /**
     * Adds a JTextField control for integer inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @param toolTipText
     *            tool tip to help the user
     * @return the JTextField control added to this dialog
     */
    public JTextField addIntegerField(String fieldName, int initialValue,
            int approxWidthInChars, String toolTipText) {
        return addNumericField(fieldName, String.valueOf(initialValue),
                approxWidthInChars,
                new EnableCheck[] { createIntegerCheck(fieldName) },
                toolTipText);
    }

    /**
     * Adds a JTextField control for positive integer inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @param toolTipText
     *            tool tip to help the user
     * @return the JTextField control added to this dialog
     */
    public JTextField addPositiveIntegerField(String fieldName,
            int initialValue, int approxWidthInChars, String toolTipText) {
        return addNumericField(fieldName, String.valueOf(initialValue),
                approxWidthInChars, new EnableCheck[] {
                        createIntegerCheck(fieldName),
                        createPositiveCheck(fieldName) }, toolTipText);
    }

    /**
     * Adds a JTextField control for positive integer inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @return the JTextField control added to this dialog
     */
    public JTextField addPositiveIntegerField(String fieldName,
            int initialValue, int approxWidthInChars) {
        return addPositiveIntegerField(fieldName, initialValue,
                approxWidthInChars, null);
    }

    /**
     * Adds a JTextField field for double values. Allows limiting the number of
     * decimals, defaulting to 0. Always shows the number as decimal and _never_
     * uses the scientific notation like "1.234E15"
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param decimals
     *            number of decimals to round to
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @return the JTextField control added to this dialog
     */
    public JTextField addDoubleField(String fieldName, double initialValue,
            int decimals, int approxWidthInChars, String toolTipText) {
        if (decimals < 0) {
            decimals = 0;
        }

        return addNumericField(fieldName,
                String.format("%." + decimals + "f", initialValue),
                approxWidthInChars,
                new EnableCheck[] { createDoubleCheck(fieldName) }, toolTipText);
    }

    /**
     * Adds a JTextField control for double values.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @return the JTextField control added to this dialog
     */
    public JTextField addDoubleField(String fieldName, double initialValue,
            int approxWidthInChars) {
        return addDoubleField(fieldName, initialValue, approxWidthInChars, null);
    }

    /**
     * Adds a JTextField control for double value inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @param toolTipText
     *            tool tip to help the user
     * @return the JTextField control added to this dialog
     */
    public JTextField addDoubleField(String fieldName, double initialValue,
            int approxWidthInChars, String toolTipText) {
        return addNumericField(fieldName, StringUtil.toString(initialValue),
                approxWidthInChars,
                new EnableCheck[] { createDoubleCheck(fieldName) }, toolTipText);
    }

    /**
     * Adds a JTextField control for positive double value inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @return the JTextField control added to this dialog
     */
    public JTextField addPositiveDoubleField(String fieldName,
            double initialValue, int approxWidthInChars, String toolTipText) {
        return addNumericField(fieldName, StringUtil.toString(initialValue),
                approxWidthInChars, new EnableCheck[] {
                        createDoubleCheck(fieldName),
                        createPositiveCheck(fieldName) }, toolTipText);
    }

    /**
     * Adds a JTextField control for positive double value inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @return the JTextField control added to this dialog
     */
    public JTextField addPositiveDoubleField(String fieldName,
            double initialValue, int approxWidthInChars) {
        return addPositiveDoubleField(fieldName, initialValue,
                approxWidthInChars, null);
    }

    /**
     * Adds a JTextField control for positive double value inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @param toolTipText
     *            tool tip text associated with this text field
     * @return the JTextField control added to this dialog
     */
    public JTextField addNonNegativeDoubleField(String fieldName,
            double initialValue, int approxWidthInChars, String toolTipText) {
        return addNumericField(fieldName, StringUtil.toString(initialValue),
                approxWidthInChars, new EnableCheck[] {
                        createDoubleCheck(fieldName),
                        createNonNegativeCheck(fieldName) }, toolTipText);
    }

    /**
     * Adds a JTextField control for positive double value inputs.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            initial value of the control
     * @param approxWidthInChars
     *            approximative width of the control in characters
     * @return the JTextField control added to this dialog
     */
    public JTextField addNonNegativeDoubleField(String fieldName,
            double initialValue, int approxWidthInChars) {
        return addNonNegativeDoubleField(fieldName, initialValue,
                approxWidthInChars, null);
    }

    /**
     * Add a JComboBox containing any collection of Layers.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default layer visible in the combo box
     * @param toolTipText
     *            tool tip text associated with this combo box
     * @param layers
     *            layers to be proposed in the combo box
     * @return the JComboBox
     */
    public JComboBox<Layer> addLayerComboBox(String fieldName,
            Layer initialValue, String toolTipText, Collection<Layer> layers) {
        final JComboBox<Layer> comboBox = addComboBox(fieldName, initialValue,
                layers, toolTipText);
        final LayerNameRenderer layerListCellRenderer = new LayerNameRenderer();
        layerListCellRenderer.setCheckBoxVisible(false);
        layerListCellRenderer.setProgressIconLabelVisible(false);
        comboBox.setRenderer(layerListCellRenderer);
        comboBox.invalidate();
        // return getComboBox(fieldName);
        return comboBox;
    }

    /**
     * Add a JComboBox containing available layers from the current
     * LayerManager.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default layer visible in the combo box
     * @param layerManager
     *            the LayerManager providing layers to the combo box
     * @return the JComboBox
     */
    public JComboBox addLayerComboBox(String fieldName, Layer initialValue,
            LayerManager layerManager) {
        return addLayerComboBox(fieldName, initialValue, null, layerManager);
    }

    /**
     * Add a JComboBox containing available layers from the current
     * LayerManager.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default layer visible in the combo box
     * @param toolTipText
     *            tool tip text associated with this combo box
     * @param layerManager
     *            the LayerManager providing layers to the combo box
     * @return the JComboBox
     */
    public JComboBox addLayerComboBox(String fieldName, Layer initialValue,
            String toolTipText, LayerManager layerManager) {
        return addLayerComboBox(fieldName, initialValue, toolTipText,
                layerManager.getLayers());
    }

    /**
     * Add a JComboBox containing editable layers of a LayerManager.
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default layer visible in the combo box
     * @param toolTipText
     *            tool tip text associated with this combo box
     * @param layerManager
     *            the LayerManager providing layers to the combo box
     * @return the JComboBox
     */
    public JComboBox addEditableLayerComboBox(String fieldName,
            Layer initialValue, String toolTipText, LayerManager layerManager) {
        return addLayerComboBox(fieldName, initialValue, toolTipText,
                layerManager.getEditableLayers());
    }

    /**
     * Add a JComboBox containing layers containing the specified AttributeType.
     * 
     * @param fieldName
     *            field name of the control
     * @param toolTipText
     *            tool tip text associated with this combo box
     * @param layerManager
     *            the LayerManager providing layers to the combo box
     * @param filter
     *            a filter to select layers with specified AttributeTypes
     * @return the JComboBox
     */
    public JComboBox addLayerComboBox(String fieldName, String toolTipText,
            LayerManager layerManager, AttributeTypeFilter filter) {
        final List<Layer> layerList = new ArrayList<>();
        for (final Layer layer : layerManager.getLayers()) {
            final FeatureSchema schema = layer.getFeatureCollectionWrapper()
                    .getFeatureSchema();
            if (filter.filter(schema).size() > 0) {
                layerList.add(layer);
            }
        }
        final Layer initialLayer = layerList.size() > 0 ? layerList.get(0)
                : null;
        return addLayerComboBox(fieldName, initialLayer, toolTipText, layerList);
    }

    /**
     * Add a JComboBox containing attributes of the Layer selected in
     * layerFieldName
     * 
     * @param fieldName
     *            field name for the attribute
     * @param layerFieldName
     *            field name of the ComboBox used to choose the layer
     * @param filter
     *            filter valid attributes from their type
     * @param toolTipText
     *            a toolTip for this JComboBox
     * @return the JComboBox
     */
    public JComboBox<String> addAttributeComboBox(final String fieldName,
            final String layerFieldName, final AttributeTypeFilter filter,
            final String toolTipText) {

        final JComboBox layerComboBox = getComboBox(layerFieldName);

        final JComboBox<String> attributeComboBox = addComboBox(fieldName,
                null, new ArrayList<String>(), toolTipText);

        final ComboBoxModel<String> DEFAULT = new DefaultComboBoxModel<>(
                new String[] { NO_VALID_ATTRIBUTE });

        final Layer layer = (Layer) layerComboBox.getSelectedItem();
        if (layer != null) {
            final FeatureSchema schema = layer.getFeatureCollectionWrapper()
                    .getFeatureSchema();
            final List<String> attributes = filter.filter(schema);
            if (attributes.size() > 0) {
                attributeComboBox.setModel(new DefaultComboBoxModel<>(
                        attributes.toArray(new String[attributes.size()])));
            } else {
                attributeComboBox.setModel(DEFAULT);
            }
        } else {
            attributeComboBox.setModel(DEFAULT);
        }

        final ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Layer lyr = (Layer) layerComboBox.getSelectedItem();
                final FeatureSchema schema = lyr.getFeatureCollectionWrapper()
                        .getFeatureSchema();
                final List<String> attributes = filter.filter(schema);
                if (attributes.size() > 0) {
                    final String oldAttr = (String) attributeComboBox
                            .getSelectedItem();
                    attributeComboBox.setModel(new DefaultComboBoxModel<>(
                            attributes.toArray(new String[attributes.size()])));
                    if (attributes.contains(oldAttr)) {
                        attributeComboBox.setSelectedItem(oldAttr);
                    }
                } else {
                    attributeComboBox.setModel(DEFAULT);
                }
            }
        };
        layerComboBox.addActionListener(listener);

        return attributeComboBox;
    }

    /**
     * Create a CheckBox to get a boolean value from the user.
     *
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default boolean value
     * @return the JCheckBox
     */
    public JCheckBox addCheckBox(String fieldName, boolean initialValue) {
        return addCheckBox(fieldName, initialValue, null);
    }

    /**
     * Create a CheckBox to get a boolean value from the user.
     *
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default boolean value
     * @param toolTipText
     *            tool tip text associated with this check box
     * @return the JCheckBox
     */
    public JCheckBox addCheckBox(String fieldName, boolean initialValue,
            String toolTipText) {
        final JCheckBox checkBox = new JCheckBox(fieldName, initialValue);
        addRow(fieldName, new JLabel(""), checkBox, null, toolTipText,
                NO_LABEL, HORIZONTAL);
        return checkBox;
    }

    /**
     * Adds a RadioButton to the buttonGroupName group.
     *
     * @param fieldName
     *            field name of the control
     * @param buttonGroupName
     *            buttonGroupName of this RadioButton
     * @param initialValue
     *            default boolean value
     * @param toolTipText
     *            tool tip text associated with this check box
     * @return the JRadioButton
     */
    public JRadioButton addRadioButton(String fieldName,
            String buttonGroupName, boolean initialValue, String toolTipText) {
        final JRadioButton radioButton = new JRadioButton(fieldName,
                initialValue);
        addRow(fieldName, new JLabel(""), radioButton, null, toolTipText,
                NO_LABEL, HORIZONTAL);

        // add to button group, if specified (and create one if it doesn't
        // exist)
        if (buttonGroupName != null) {
            ButtonGroup group = (ButtonGroup) buttonGroupMap
                    .get(buttonGroupName);
            if (group == null) {
                group = new ButtonGroup();
                buttonGroupMap.put(buttonGroupName, group);
            }
            group.add(radioButton);
        }
        return radioButton;
    }

    /**
     * Adds a TextArea field.
     *
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default boolean value
     * @param rowNumber
     *            initial row number
     * @param columnNumber
     *            initial column number
     * @param scrollable
     *            if true, the textArea is embeded into a JScrollPane
     * @param enableChecks
     *            checks to validate the input value
     * @param toolTipText
     *            tool tip text associated with this check box
     * @return the JTextArea
     */
    public JTextArea addTextAreaField(String fieldName, String initialValue,
            int rowNumber, int columnNumber, boolean scrollable,
            EnableCheck[] enableChecks, String toolTipText) {
        JComponent component;
        final JTextArea textArea = new JTextArea(initialValue, rowNumber,
                columnNumber);
        if (scrollable) {
            component = new JScrollPane(textArea);
        } else {
            component = textArea;
        }
        addRow(fieldName, new JLabel(fieldName), component, enableChecks,
                toolTipText, LEFT_LABEL, BOTH);
        return textArea;
    }

    /**
     * Adds a row (containing either a control or a label) to the Dialog.
     *
     * @param fieldName
     *            field name of the control (used as a key)
     * @param label
     *            label of the control
     * @param component
     *            the control itself (may also be a label or a separator)
     * @param enableChecks
     *            checks to validate inputs
     * @param toolTipText
     *            ToolTip text
     * @param labelPos
     *            0, 1 or 2 depending on whether the label is hidden, on the
     *            left side or on the right side of the component
     * @param fillMode
     *            true if the component must fill the available space
     */
    protected abstract void addRow(String fieldName, JComponent label,
            JComponent component, EnableCheck[] enableChecks,
            String toolTipText, int labelPos, int fillMode);

    protected abstract void addRow(JComponent c);

    protected abstract void addRow();

    public void setFieldEnabled(String fieldName, boolean enable) {
        final Component component = getComponent(fieldName);
        if (component != null) {
            component.setEnabled(enable);
        }
    }

    public void setFieldVisible(String fieldName, boolean visible) {
        if (getComponent(fieldName) != null) {
            getComponent(fieldName).setVisible(visible);
        }
        if (getLabel(fieldName) != null) {
            getLabel(fieldName).setVisible(visible);
        }
    }

    protected void reportValidationError(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage, "JUMP",
                JOptionPane.ERROR_MESSAGE);
    }

    protected boolean isInputValid() {
        return firstValidationErrorMessage() == null;
    }

    protected String firstValidationErrorMessage() {
        for (final Iterator i = fieldNameToEnableCheckListMap.keySet()
                .iterator(); i.hasNext();) {
            final String fieldName = (String) i.next();
            for (final Iterator j = fieldNameToEnableCheckListMap.getItems(
                    fieldName).iterator(); j.hasNext();) {
                final EnableCheck enableCheck = (EnableCheck) j.next();
                final String message = enableCheck.check(null);
                if (message != null) {
                    return message;
                }
            }
        }
        return null;
    }

    /**
     * generic method to load collection of layerable into JComboBox with
     * renderer of layerable (layer icon)
     * 
     * @param fieldName
     *            field name of the control
     * @param initialValue
     *            default layer visible in the combo box
     * @param toolTipText
     *            tool tip text associated with this combo box
     * @param layerable
     *            layers to be proposed in the combo box
     * @return the JComboBox
     */

    public <T> JComboBox<T> addLayerableComboBox(String fieldName,
            Object initialValue, String toolTipText, Collection<T> layerable) {

        final JComboBox<T> comboBox = addComboBox(fieldName, initialValue,
                layerable, toolTipText);
        final LayerNameRenderer layerListCellRenderer = new LayerNameRenderer();
        layerListCellRenderer.setCheckBoxVisible(false);
        layerListCellRenderer.setProgressIconLabelVisible(false);
        comboBox.setRenderer(layerListCellRenderer);
        comboBox.invalidate();

        return comboBox;
    }
}
