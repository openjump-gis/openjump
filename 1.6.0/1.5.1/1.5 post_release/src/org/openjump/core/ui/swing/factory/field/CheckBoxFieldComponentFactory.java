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
import org.openjump.core.ui.swing.CheckBoxComponentPanel;
import org.openjump.swing.factory.field.FieldComponentFactory;
import org.openjump.swing.listener.ValueChangeEvent;
import org.openjump.swing.listener.ValueChangeListener;

/**
 * 
 * @author Michael Michaud
 * @version 0.1.0
 */
public class CheckBoxFieldComponentFactory implements FieldComponentFactory {

    private WorkbenchContext workbenchContext;
    private String option;

    public CheckBoxFieldComponentFactory(final WorkbenchContext workbenchContext) {
        this.workbenchContext = workbenchContext;
    }

    public CheckBoxFieldComponentFactory(final WorkbenchContext workbenchContext,
        final String option) {
        this.workbenchContext = workbenchContext;
        this.option = option;
    }

    public Object getValue(final JComponent component) {
        if (component instanceof CheckBoxComponentPanel) {
            CheckBoxComponentPanel checkBoxComponentPanel = (CheckBoxComponentPanel)component;
            if (checkBoxComponentPanel == null) return null;
            else if (checkBoxComponentPanel.isSelected()) return Boolean.TRUE;
            else return Boolean.FALSE;
        }
        return null;
    }

    public void setValue(JComponent component, Object value) {
        if (component instanceof CheckBoxComponentPanel) {
            CheckBoxComponentPanel checkBoxComponentPanel = (CheckBoxComponentPanel)component;
            if (value != null && value == Boolean.TRUE) {
                checkBoxComponentPanel.getCheckBox().setSelected(true);
            }
            else if (value != null && value == Boolean.FALSE) {
                checkBoxComponentPanel.getCheckBox().setSelected(false);
            }
        }
    }

    public JComponent createComponent() {
        CheckBoxComponentPanel checkBoxComponentPanel = new CheckBoxComponentPanel(
            option,
            workbenchContext.getErrorHandler());
        return checkBoxComponentPanel;
    }

    public JComponent createComponent(final ValueChangeListener listener) {
        final CheckBoxComponentPanel checkBoxComponentPanel = new CheckBoxComponentPanel(
            option,
            workbenchContext.getErrorHandler());
        checkBoxComponentPanel.getCheckBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object value = new Boolean(checkBoxComponentPanel.isSelected());
                listener.valueChanged(new ValueChangeEvent(checkBoxComponentPanel, value));
            }
        });
        return checkBoxComponentPanel;
    }

}