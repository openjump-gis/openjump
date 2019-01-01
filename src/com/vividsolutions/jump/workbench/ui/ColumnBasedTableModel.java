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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.vividsolutions.jts.util.Assert;

/**
 * Provides a column based {@link TableModel}.
 */

public abstract class ColumnBasedTableModel implements TableModel {
    private ArrayList columns = new ArrayList();
    private ArrayList listeners = new ArrayList();
    //I got a strange error from JBuilder when I tried to make Column protected:
    //"ColorThemingTableModel.java": Error #: 300 : method Column(com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingTableModel, java.lang.String, java.lang.Class) not found in class com.vividsolutions.jump.workbench.ui.ColumnBasedTableModel.Column at line 32, column 52
    //"ColorThemingTableModel.java": Error #: 300 : constructor Column(com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingTableModel) not found in class com.vividsolutions.jump.workbench.ui.ColumnBasedTableModel.Column at line 32, column 72
    //Funny -- Eclipse didn't give me an error. [Jon Aquino]
    public abstract class Column {
        private String name;
        private Class dataClass;

        public Column(String name, Class dataClass) {
            this.name = name;
            this.dataClass = dataClass;
        }

        public String getName() {
            return name;
        }

        public Class getDataClass() {
            return dataClass;
        }

        public abstract Object getValueAt(int rowIndex);

        public abstract void setValueAt(Object value, int rowIndex);
    }

    protected Column getColumn(int column) {
        return (Column) columns.get(column);
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public int getColumnCount() {
        return columns.size();
    }

    public String getColumnName(int columnIndex) {
        return getColumn(columnIndex).getName();
    }

    public int indexOfColumn(String name) {
        for (int i = 0; i < columns.size(); i++) {
            Column column = (Column) columns.get(i);

            if (column.getName().equals(name)) {
                return i;
            }
        }

        Assert.shouldNeverReachHere(name);

        return -1;
    }

    protected void setColumns(Collection columns) {
        this.columns.clear();
        this.columns.addAll(columns);
    }

    public Class getColumnClass(int columnIndex) {
        return getColumn(columnIndex).getDataClass();
    }

    private boolean firingEvents = true;
    protected void setFiringEvents(boolean firingEvents) {
        this.firingEvents = firingEvents;
    }

    protected boolean isFiringEvents() {
        return firingEvents;
    }

    protected void fireTableChanged(TableModelEvent e) {
        if (!firingEvents) {
            return;
        }
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            TableModelListener listener = (TableModelListener) i.next();
            listener.tableChanged(e);
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getColumn(columnIndex).getValueAt(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        getColumn(columnIndex).setValueAt(aValue, rowIndex);
    }

}
