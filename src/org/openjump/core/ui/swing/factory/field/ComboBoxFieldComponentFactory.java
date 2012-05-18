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

package org.openjump.core.ui.swing.factory.field;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import org.openjump.core.ui.swing.ComboBoxComponentPanel;
import org.openjump.swing.factory.field.FieldComponentFactory;
import org.openjump.swing.listener.ValueChangeEvent;
import org.openjump.swing.listener.ValueChangeListener;

/**
 * Factory to build a combobox component. 
 * @author Michael Michaud
 * @version 0.1.0
 */
public class ComboBoxFieldComponentFactory implements FieldComponentFactory {

    private WorkbenchContext workbenchContext;
    private String option;
    private Object[] items;

    public ComboBoxFieldComponentFactory(final WorkbenchContext workbenchContext) {
        this.workbenchContext = workbenchContext;
    }

    public ComboBoxFieldComponentFactory(final WorkbenchContext workbenchContext,
        final String option, final Object[] items) {
        this.workbenchContext = workbenchContext;
        this.option = option;
        this.items = items;
    }

    public Object getValue(final JComponent component) {
        if (component instanceof ComboBoxComponentPanel) {
            ComboBoxComponentPanel chooser = (ComboBoxComponentPanel)component;
            return chooser.getSelectedItem();
        }
        return null;
    }

    public void setValue(JComponent component, Object value) {
        if (component instanceof ComboBoxComponentPanel) {
            ComboBoxComponentPanel chooser = (ComboBoxComponentPanel)component;
            if (value != null) {
                chooser.getComboBox().setSelectedItem(value);
            }
        }
    }

    public JComponent createComponent() {
        ComboBoxComponentPanel chooser = new ComboBoxComponentPanel(
            option,
            items,
            workbenchContext.getErrorHandler());
        return chooser;
    }

    public JComponent createComponent(final ValueChangeListener listener) {
        final ComboBoxComponentPanel chooser = new ComboBoxComponentPanel(
            option,
            items,
            workbenchContext.getErrorHandler());
        chooser.getComboBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object item = chooser.getSelectedItem();
                listener.valueChanged(new ValueChangeEvent(chooser, item));
            }
        });
        return chooser;
    }

}