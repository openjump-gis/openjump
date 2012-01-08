/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2009 Micha&euml;l Michaud

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/

package org.openjump.core.ui.swing;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.vividsolutions.jump.workbench.ui.ErrorHandler;

/**
 * Component including a Label and a ComboBox
 * @author Michael Michaud
 * @version 0.1.0
 */
public class ComboBoxComponentPanel extends JPanel {

    private JLabel label;
    private JComboBox comboBox;
    private ErrorHandler errorHandler;

    public ComboBoxComponentPanel(String descriptionLabel,
                                  Object[] items, ErrorHandler errorHandler) {
        this.label = new JLabel(descriptionLabel);
        this.errorHandler = errorHandler;
        try {
            comboBox = new JComboBox(new DefaultComboBoxModel(items));
            this.add(label);
            this.add(comboBox);
        } catch (Throwable t) {
            errorHandler.handleThrowable(t);
        }
    }

    public JComboBox getComboBox() {
        return comboBox;
    }

    public Object getSelectedItem() {
        return comboBox==null?null:comboBox.getSelectedItem();
    }

}