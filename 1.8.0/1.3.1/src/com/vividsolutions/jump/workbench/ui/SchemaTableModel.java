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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.TableModelEvent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.model.Layer;

public class SchemaTableModel extends ColumnBasedTableModel {
	
    public static class Field {
        //null name and type means that it's one of the blank fields. Blank fields are
        //of course ignored when the changes are committed. [Jon Aquino]
        private String name = null;
        private AttributeType type = null;
        private int originalIndex = -1;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setType(AttributeType type) {
            this.type = type;
        }

        public AttributeType getType() {
            return type;
        }

        public void setOriginalIndex(int originalIndex) {
            this.originalIndex = originalIndex;
        }

        public int getOriginalIndex() {
            return originalIndex;
        }
    }
    private ArrayList fields = new ArrayList();
    public int getRowCount() {
        return fields.size();
    }

    public List getFields() { return Collections.unmodifiableList(fields); }

    public Field get(int row) {
        return (Field) fields.get(row);
    }

    public final static String FIELD_NAME_COLUMN_NAME = I18N.get("ui.SchemaTableModel.field-name");
    public final static String DATA_TYPE_COLUMN_NAME = I18N.get("ui.SchemaTableModel.data-type");
    public SchemaTableModel(Layer layer) {
        this.layer = layer;
        for (int i = 0; i < layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount(); i++) {
            Field field = new Field();
            field.setName(layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeName(i));
            field.setType(layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(i));
            field.setOriginalIndex(i);
            fields.add(field);
        }
        addBlankRows();
        ArrayList columns = new ArrayList();
        columns.add(new Column(FIELD_NAME_COLUMN_NAME, String.class) {
            public Object getValueAt(int row) { return get(row).getName(); }
            public void setValueAt(Object value, int row) {
                if ((value == null || ((String)value).length() == 0) && get(row).getType() == null) {
                    //User double-clicked on the field but didn't enter anything.
                    //(Note that the above "if" checks that the type is null too).
                    //Don't set the name and especially don't set a default value for the type. [Jon Aquino]
                    return;
                }
                get(row).setName(((String) value).trim());
                if (get(row).getType() == null) {
                    get(row).setType(AttributeType.STRING);
                }
                fieldsModified(new int[]{row});
            }
        });
        columns.add(new Column(DATA_TYPE_COLUMN_NAME, AttributeType.class) {
            public Object getValueAt(int row) { return get(row).getType(); }
            public void setValueAt(Object value, int row) {
                if (value == null) {
                    //User clicked on the combobox, but not on any of the possible values.
                    //Don't set the type and especially don't set a default value for the name. [Jon Aquino]
                    return;
                }
                get(row).setType((AttributeType) value);
                if (get(row).getName() == null) {
                    get(row).setName(createName());
                }
                fieldsModified(new int[]{row});
            }
        });
        setColumns(columns);
    }

    private void fieldsModified(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            fireTableChanged(new TableModelEvent(this, rows[i]));
            addBlankRowsIfNecessary(rows[i]);
        }
    }

    private String createName() {
        int i = 1;
        while (hasFieldNamed(I18N.get("ui.SchemaTableModel.field")+i)) {
            i++;
        }
        return I18N.get("ui.SchemaTableModel.field")+i;
    }

    private boolean hasFieldNamed(String name) {
        //Existing fields are already trimmed. [Jon Aquino]
        for (int i = 0; i < getRowCount(); i++) {
            if (get(i).getName() == null) {
                //One of the blank rows. [Jon Aquino]
                continue;
            }
            if (get(i).getName().equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    private void addBlankRowsIfNecessary(int indexOfModifiedField) {
        if (fields.size() - indexOfModifiedField < BLANK_ROWS) {
            int firstRow = fields.size();
            addBlankRows();
            fireTableChanged(new TableModelEvent(this, firstRow, fields.size()-1));
        }
    }

    private void addBlankRows() {
        for (int i = 0; i < BLANK_ROWS; i++) {
            fields.add(new Field());
        }
    }

    private final static int BLANK_ROWS = 30;

    private Layer layer;

    public boolean isCellEditable(int row, int column) {
        return layer.isEditable();
    }

    private void removeField(int row) {
        removeFields(new int[]{row});
    }

    public void removeFields(int[] rows) {
        for (Iterator i = CollectionUtil.reverseSortedSet(rows).iterator(); i.hasNext(); ) {
            Integer row = (Integer) i.next();
            fields.remove(row.intValue());
            fieldsModified(rows);
            fireTableChanged(new TableModelEvent(this, row.intValue(), row.intValue(), TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
        }
    }

    public void removeBlankRows() {
        for (Iterator i = fields.iterator(); i.hasNext(); ) {
            Field field = (Field) i.next();
            if (field.getName() == null) {
                //Can't use #removeField because we don't want #addBlankRowsIfNecessary
                //to be called. [Jon Aquino]
                i.remove();
            }
        }
    }

    public void insertBlankRow(int location) {
        insertField(location, new Field());
    }

    private void insertField(int location, Field field) {
        fields.add(location, field);
        fireTableChanged(new TableModelEvent(this, location, location, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
        fieldsModified(new int[]{location});
    }

    public void move(Collection fieldsToMove, int displacement) {
        for (Iterator i = fieldsToMove.iterator(); i.hasNext();) {
            Field field = (Field) i.next();
            int index = fields.indexOf(field);
            removeField(index);
            insertField(index + displacement, field);
        }
    }

    public int indexOf(Field field) {
        return fields.indexOf(field);
    }

}
