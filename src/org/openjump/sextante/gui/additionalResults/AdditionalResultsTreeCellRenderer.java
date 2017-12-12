package org.openjump.sextante.gui.additionalResults;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

public class AdditionalResultsTreeCellRenderer extends JLabel implements
        TreeCellRenderer {

    /**
     * This frame is a refactoring of Sextante
     * es.unex.sextante.gui.additionalResults.AdditionalResultsTreeCellRenderer
     * 
     * @author Giuseppe Aruta [2017-12-12]
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTreeCellRendererComponent(final JTree tree,
            final Object value, final boolean sel, final boolean expanded,
            boolean leaf, final int row, final boolean hasFocus) {

        final String sName = tree.convertValueToText(value, sel, expanded,
                leaf, row, hasFocus);

        // setFont(tree.getFont());
        setEnabled(tree.isEnabled());
        setText(sName);

        if (!leaf) {
            // setFont(new java.awt.Font("Tahoma", 1, 11));
            setForeground(Color.black);
        } else {
            if (sel) {
                setForeground(Color.blue);
            } else {
                setForeground(Color.black);
            }
        }
        return this;

    }

}
