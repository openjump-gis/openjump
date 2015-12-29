package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;

/**
 * A resizable textArea to edit layers WHERE clause.
 * From Netbeans Outline examples.
 * @author nicolas Ribot
 */
public class DataStoreLayerWhereEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

    JTextArea textArea = null;

    DataStoreLayerWhereEditor() {
        this.textArea = new ResizableTextArea();
        this.textArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        this.textArea.registerKeyboardAction(this, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_MASK), JComponent.WHEN_FOCUSED);

    }

    class ResizableTextArea extends JTextArea {

        @Override
        public void addNotify() {
            super.addNotify();
            getDocument().addDocumentListener(listener);
            this.updateBounds();
        }

        @Override
        public void removeNotify() {
            getDocument().removeDocumentListener(listener);
            super.removeNotify();
        }

        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateBounds();
            }

            public void removeUpdate(DocumentEvent e) {
                updateBounds();
            }

            public void changedUpdate(DocumentEvent e) {
                updateBounds();
            }
        };

        private void updateBounds() {
            if (getParent() instanceof JTable) {
                JTable table = (JTable)getParent();
                if (table.isEditing()) {
                    Rectangle cellRect = table.getCellRect(table.getEditingRow(), table.getEditingColumn(), false);
                    Dimension prefSize = getPreferredSize();
                    setBounds(getX(), getY(), Math.max(cellRect.width, prefSize.width), Math.max(cellRect.height, prefSize.height));
                    validate();
                }
            }
        }
    }

    /*--------------------------------[ clickCountToStart ]----------------------------------*/
    protected int clickCountToStart = 2;

    public int getClickCountToStart() {
        return clickCountToStart;
    }

    public void setClickCountToStart(int clickCountToStart) {
        this.clickCountToStart = clickCountToStart;
    }

    public boolean isCellEditable(EventObject e) {
        return !(e instanceof MouseEvent)
                || ((MouseEvent) e).getClickCount() >= clickCountToStart;
    }

    /*--------------------------------[ ActionListener ]------------------------*/
    public void actionPerformed(ActionEvent ae) {
        stopCellEditing();
    }

    /*---------------------------[ TableCellEditor ]------------------------*/
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        String text = value != null ? value.toString() : "";
        textArea.setText(text);
        return textArea;
    }

    public Object getCellEditorValue() {
        return textArea.getText();
    }

}
