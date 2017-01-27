/* 
 * Kosmo - Sistema Abierto de Informaci�n Geogr�fica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2006, SAIG S.L.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, contact:
 * 
 * Sistemas Abiertos de Informaci�n Geogr�fica, S.L.
 * Avnda. Rep�blica Argentina, 28
 * Edificio Domocenter Planta 2� Oficina 7
 * C.P.: 41930 - Bormujos (Sevilla)
 * Espa�a / Spain
 *
 * Tel�fono / Phone Number
 * +34 954 788876
 * 
 * Correo electr�nico / Email
 * info@saig.es
 *
 */

/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */
/*
 * FormUtils.java
 *
 * Created on 6 dicembre 2003, 19.32
 */
package org.saig.core.gui.swing.sldeditor.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * 
 * 
 * @author wolf
 */
public class FormUtils {
    private static Insets defaultInsets = new Insets(3, 3, 3, 3);
    private static Dimension buttonDimension;
    private static Dimension colorButtonDimension;
    private static Dimension spinnerDimension;
    private static Dimension comboDimension;

    /**
     * Creates a new instance of FormUtils
     */
    private FormUtils() {
    }

    public static Insets getDefaultInsets() {
        return defaultInsets;
    }

    public static void addRowInGBL( JComponent parent, int row, int startCol, JComponent component ) {
        addRowInGBL(parent, row, startCol, component, true, true);
    }

    public static void addRowInGBL( JComponent parent, int row, int startCol, JComponent component,
            boolean fillRow, boolean insets ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        if (fillRow) {
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.weightx = 1.0; // Para que rellene en horizontal
        }

        if (insets) {
            gridBagConstraints.insets = getDefaultInsets();
        }

        parent.add(component, gridBagConstraints);
    }
    
    public static void addRowInGBL( JComponent parent, int row, int startCol, JComponent component,
            boolean fillRow, boolean lastComponentInRow, boolean insets ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        if (fillRow) {
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
        }
        if(lastComponentInRow)
        {
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
        }

        if (insets) {
            gridBagConstraints.insets = getDefaultInsets();
        }

        parent.add(component, gridBagConstraints);
    }

    public static void addRowInGBL( JComponent parent, int row, int startCol, JLabel label,
            JComponent component ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
    }
    
    public static void addRowInGBL( JComponent parent, int row, int startCol, String label,
            JComponent component ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(new JLabel(label), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
    }

    public static void addRowInGBL( JComponent parent, int row, int startCol, JLabel label,
            JComponent component, boolean lastRowComponent ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        if(lastRowComponent)
        {
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
        }
        else
        {
            gridBagConstraints.fill = GridBagConstraints.NONE;
        }
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
    }
    
    public static void addRowInGBL( JComponent parent, int row, int startCol, String label,
            JComponent component, boolean lastRowComponent ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(new JLabel(label), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        if(lastRowComponent)
        {
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
        }
        else
        {
            gridBagConstraints.fill = GridBagConstraints.NONE;
        }
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
    }


    public static void addRowInGBL( JComponent parent, int row, int startCol, JComponent label,
            JComponent component ) {
        addRowInGBL(parent, row, startCol, label, component, 0.0, true);
    }
    
    public static void addRowInGBL( JComponent parent, int row, int startCol, JComponent label,
            JComponent component, boolean lastRowComponent ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        if(lastRowComponent)
        {
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
        }
        else
        {
            gridBagConstraints.fill = GridBagConstraints.NONE;
        }
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
    }
    
 
    public static void addRowInGBL( JComponent parent, int row, int startCol, JComponent label,
            JComponent component, double weigthy, boolean insets ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;

        if (weigthy > 0.0) {
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        } else {
            gridBagConstraints.anchor = GridBagConstraints.WEST;
        }

        if (insets) {
            gridBagConstraints.insets = getDefaultInsets();
        }

        parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;

        if (weigthy > 0.0) {
            gridBagConstraints.weighty = (float) weigthy;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
        }

        if (insets) {
            gridBagConstraints.insets = getDefaultInsets();
        }

        parent.add(component, gridBagConstraints);
    }

    //2015_03_11 Giuseppe Aruta: Add Icon to row 
    public static void addRowInGBL( JComponent parent, int row, int startCol, Icon icon,  
            JComponent component ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        JLabel iconlabel = new JLabel();
        iconlabel.setIcon(icon);
        parent.add(iconlabel, gridBagConstraints);
      //  parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
    }
    
    
    //2015_03_11 Giuseppe Aruta: Add Icon to row 
    public static void addRowInGBL( JComponent parent, int row, int startCol, Icon icon, 
            JComponent component, JComponent component2 ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        JLabel iconlabel = new JLabel();
        iconlabel.setIcon(icon);
        parent.add(iconlabel, gridBagConstraints);
       // parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 2;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component2, gridBagConstraints);
    }
    
    //2015_03_11 Giuseppe Aruta: Add Icon to row 
    public static void addRowInGBL( JComponent parent, int row, int startCol, Icon icon, 
             JLabel label, JComponent component, JLabel label2, JComponent component2 ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = getDefaultInsets();
        JLabel iconlabel = new JLabel();
        iconlabel.setIcon(icon);
        parent.add(iconlabel, gridBagConstraints);
       // parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 1;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 2;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        
        gridBagConstraints.gridx = startCol + 3;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(label2, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol + 4;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component2, gridBagConstraints);
    } 
    
    public static void addColInGBL( JComponent parent, int row, int startCol, JComponent label,
            JComponent component ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;

        gridBagConstraints.insets = getDefaultInsets();
        parent.add(label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = startCol;
        gridBagConstraints.gridy = row + 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        gridBagConstraints.insets = getDefaultInsets();
        parent.add(component, gridBagConstraints);
    }

    public static void addFiller( JComponent parent, int row, int col, JComponent component,
            double weight, boolean insets ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = col;
        gridBagConstraints.gridy = row;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = weight;
        gridBagConstraints.weighty = weight;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = GridBagConstraints.REMAINDER;

        if (insets) {
            gridBagConstraints.insets = getDefaultInsets();
        }

        parent.add(component, gridBagConstraints);
    }

    public static void addFiller( JComponent parent, int row, int col, JComponent component ) {
        addFiller(parent, row, col, component, 1000.0, true);
    }

    public static void addFiller( JComponent parent, int row, int col, JComponent component,
            boolean insets ) {
        addFiller(parent, row, col, component, 1000.0, insets);
    }

    public static void addFiller( JComponent parent, int row, int col ) {
        addFiller(parent, row, col, new JLabel(), false);
    }

    public static void addSingleRowWestComponent( JComponent parent, int row, JComponent component ) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        parent.add(component, gridBagConstraints);

        // make it stay on the west side
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = row;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 1.0;
        parent.add(new JLabel(), gridBagConstraints);
    }

    public static Dimension getButtonDimension() {
        if (buttonDimension == null) {
            JLabel label = new JLabel("w"); //$NON-NLS-1$
            buttonDimension = label.getPreferredSize();
            buttonDimension.width = buttonDimension.height = (int) (Math.max(buttonDimension.width,
                    buttonDimension.height) * 1.3);
        }

        return buttonDimension;
    }

    public static void forceButtonDimension( JButton button ) {
        button.setPreferredSize(getButtonDimension());
        button.setMinimumSize(getButtonDimension());
        button.setMaximumSize(getButtonDimension());
    }

    public static Dimension getColorButtonDimension() {
        if (colorButtonDimension == null) {
            JLabel label = new JLabel("w"); //$NON-NLS-1$
            colorButtonDimension = label.getPreferredSize();
            colorButtonDimension.height = (int) (Math.max(colorButtonDimension.width,
                    colorButtonDimension.height) * 1.3);
            colorButtonDimension.width = getComboDimension().width;
        }

        return colorButtonDimension;
    }

    public static Dimension getSpinnerDimension() {
        if (spinnerDimension == null) {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 0.1));
            spinnerDimension = spinner.getPreferredSize();
            spinnerDimension.width = getComboDimension().width;
        }

        return spinnerDimension;
    }

    public static Dimension getComboDimension() {
        if (comboDimension == null) {
            JComboBox combo = new JComboBox(new String[]{"abcdefg"}); //$NON-NLS-1$
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 0.1));
            comboDimension = combo.getPreferredSize();
        }

        return comboDimension;
    }

    public static void show( JComponent component ) {
        JFrame frame = new JFrame("Testing component: " + component.getClass().getName()); //$NON-NLS-1$
        frame.setContentPane(component);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    public static void show( JFrame frame ) {
        frame.setTitle("Testing component: " + frame.getClass().getName()); //$NON-NLS-1$
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }

    public static Dimension getMaxDimension( Dimension d1, Dimension d2 ) {
        return new Dimension((int) Math.max(d1.width, d2.width), (int) Math.max(d1.height,
                d2.height));
    }

    public static Window getWindowForComponent( Component parentComponent ) {
        if (parentComponent == null) {
            return JOptionPane.getRootFrame();
        }

        if (parentComponent instanceof Frame) {
            return (Frame) parentComponent;
        }

        if (parentComponent instanceof Dialog) {
            return (Dialog) parentComponent;
        }

        return getWindowForComponent(parentComponent.getParent());
    }

    public static JLabel getTitleLabel( String title ) {
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));

        return label;
    }

    public static JComponent getExpandableTitleLabel( String title,
            final JComponent[] managedComponents, boolean collapsed ) {
        // create label and button
        JLabel label = new JLabel(title);

        final Icon expandedIcon = (Icon) UIManager.get("Tree.expandedIcon"); //$NON-NLS-1$
        final Icon collapsedIcon = (Icon) UIManager.get("Tree.collapsedIcon"); //$NON-NLS-1$

        final JButton button = new JButton(collapsed ? collapsedIcon : expandedIcon);

        // button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,0,3,3),
        // BorderFactory.createLineBorder(Color.BLACK)));
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));

        // int width = button.getPreferredSize().width;
        // button.setPreferredSize(new Dimension(width, width));
        button.setFocusPainted(false);

        // hide managed components
        for( int i = 0; i < managedComponents.length; i++ ) {
            managedComponents[i].setVisible(!collapsed);
        }

        // setup expand/collapse logic
        button.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                boolean visible = true;

                if (button.getIcon() == expandedIcon) {
                    button.setIcon(collapsedIcon);
                    visible = false;
                } else {
                    button.setIcon(expandedIcon);
                }

                for( int i = 0; i < managedComponents.length; i++ ) {
                    managedComponents[i].setVisible(visible);
                }

                getWindowForComponent(button).pack();
            }
        });

        // create the title panel
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        panel.setLayout(new BorderLayout());
        panel.add(button, BorderLayout.WEST);
        panel.add(label);

        return panel;
    }

    public static void repackParentWindow( Component component ) {
        Window window = getWindowForComponent(component);
        Dimension preferred = window.getPreferredSize();
        Dimension actual = window.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension newSize = new Dimension(actual);
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                window.getGraphicsConfiguration());
        Dimension freeScreen = new Dimension(screen.width - insets.left - insets.right,
                screen.height - insets.top - insets.bottom);

        if (actual.width < preferred.width) {
            if (preferred.width > freeScreen.width) {
                newSize.width = freeScreen.width;
            } else {
                newSize.width = preferred.width;
            }
        }

        if (actual.height < preferred.height) {
            if (preferred.height > freeScreen.height) {
                newSize.height = freeScreen.height;
            } else {
                newSize.height = preferred.height;
            }
        }

        if (!newSize.equals(actual)) {
            window.setSize(newSize);
        }
    }
}
