package org.openjump.core.rasterimage.styler.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.openjump.core.rasterimage.styler.ColorMapEntry;
import org.openjump.core.rasterimage.styler.ColorUtils;

/**
 * A SWING table used to represent class intervals values and corresponding
 * colours. The colors can be changed using a color picker.
 * @author 
 */
public class ColorsTablePanel extends JPanel {
    
    private ColorsTablePanel() {
        super(new GridLayout(1,0));
    }
    
    /**
     * 
     * @param parent
     * @param tableType
     * @param colorMapEntries
     * @param noDataValue 
     * @param integerValues 
     */
    public ColorsTablePanel(Component parent, TableType tableType,
            ColorMapEntry[] colorMapEntries, Double noDataValue,
            boolean integerValues) {
        super(new GridLayout(1,0));
        
        if(tableType == TableType.INTERVALS) {
            columnNames = new String[]{
                java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle")
                        .getString("org.openjump.core.rasterimage.styler.ui.ColorsTablePanel.MinValue"),
                java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle")
                        .getString("org.openjump.core.rasterimage.styler.ui.ColorsTablePanel.Color")};
        } else if(tableType == TableType.VALUES) {
            columnNames = new String[]{
                java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle")
                        .getString("org.openjump.core.rasterimage.styler.ui.ColorsTablePanel.Value"),
                java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle")
                        .getString("org.openjump.core.rasterimage.styler.ui.ColorsTablePanel.Color")};
        }
        this.noDataValue = noDataValue;
        this.integerValues = integerValues;
        
        // Create table
        table = new JTable();
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Color renderer
        table.setDefaultRenderer(Color.class, new ColorRenderer(true));
        table.setDefaultEditor(Color.class, new ColorEditor(parent));        
        updateTable(colorMapEntries);
        add(scrollPane);
        
    }

    public final void updateTable(ColorMapEntry[] colorMapEntries) {
        
        /* Check if one entry == no data */
        int noDataEntry = -1;
            if(noDataValue != null) {
            for(int c=0; c<colorMapEntries.length; c++) {
                double value = colorMapEntries[c].getUpperValue();

                if(value == noDataValue ||
                        (Double.isInfinite(value) && Double.isInfinite(noDataValue)) ||
                        (Double.isNaN(value) && Double.isNaN(noDataValue))) {
                    noDataEntry = c;
                    break;
                }
            }
        }
        
        int numRows = colorMapEntries.length;
        if(noDataEntry > -1) {
            numRows--;
        }
        
        Object[][] data = new Object[numRows][columnNames.length];
        int r = 0;
        for(int cme=0; cme<colorMapEntries.length; cme++) {
            if(cme==noDataEntry) {
                continue;
            }
            for(int c=0; c<columnNames.length; c++) {
                if(c == COLUMN_VALUE) {
                    data[r][c] = colorMapEntries[cme].getUpperValue();
                    if(integerValues) {
                        data[r][c] = (int) colorMapEntries[cme].getUpperValue();
                    }
                } else if (c == COLUMN_COLOR) {
                    data[r][c] = colorMapEntries[cme].getColor();
                }
            }
            r++;
        }
        
        table.setModel(new DefaultTableModel(data, columnNames) {
        
            @Override
            public Class getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }
            
        });
                
        // Headers: central alignment
        ((DefaultTableCellRenderer)table.getTableHeader().getDefaultRenderer())
                .setHorizontalAlignment(JLabel.CENTER);
        
        // Labels: central alignment
        DefaultTableCellRenderer centralAlignment = new DefaultTableCellRenderer();
        centralAlignment.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(COLUMN_VALUE).setCellRenderer(centralAlignment);
        
        
        
    }
    
    public ColorMapEntry[] getColorMapEntries() throws Exception {
        
        if(table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        
        ColorMapEntry[] colorMapEntries;
        try {
            colorMapEntries = new ColorMapEntry[table.getRowCount()];
            for(int r=0; r<colorMapEntries.length; r++) {
                colorMapEntries[r] = new ColorMapEntry(
                        Double.parseDouble(table.getValueAt(r, COLUMN_VALUE).toString()),
                        (Color) table.getValueAt(r, COLUMN_COLOR));
            }
            return colorMapEntries;
        } catch (Exception ex) {
            throw new Exception(
                    java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle")
                            .getString("org.openjump.core.rasterimage.styler.ui.ColorsTablePanel.ErrorInTable") + ex);
        }
        
    }
    
    public void addRows() {

        int selRows [] = table.getSelectedRows();
        if (selRows.length > 0) {
            Color color = (Color) table.getValueAt(selRows[0], COLUMN_COLOR);
            ((DefaultTableModel)table.getModel())
                    .insertRow(selRows[0],new Object[]{null, color});
        } else {
            // Get last row colour
            Color color = (Color) table.getValueAt(table.getRowCount()-1, COLUMN_COLOR); 
            
            ((DefaultTableModel)table.getModel())
                    .addRow(new Object[]{null, color});
            // select the last row
            int lastRow = table.getRowCount() - 1;
            table.getSelectionModel().setSelectionInterval(lastRow, lastRow);
            table.scrollRectToVisible(table.getCellRect(lastRow, 1, true));
        }        

    }
    
    public void removeRow() {
        int selRows [] = table.getSelectedRows();
        for (int i = selRows.length - 1; i >= 0 ; --i) {
            ((DefaultTableModel)table.getModel()).removeRow(selRows[i]);
        }
    }
    
    public int getSelectedRowsCount() {
        return table.getSelectedRowCount();
    }
    
    public void rampColors() throws Exception {
    
        int[] selRows = table.getSelectedRows();
        if(selRows == null || selRows.length <= 1) {
            return;
        }
        
        int startRow = selRows[0];
        int endRow = selRows[1];
        
        if(startRow >= endRow) {
            return;
        }
        
        if(endRow == startRow+1) {
            return;
        }
        
        Color startColor = (Color) table.getValueAt(startRow, COLUMN_COLOR);
        Color endColor = (Color) table.getValueAt(endRow, COLUMN_COLOR);
        ColorUtils colorUtils = new ColorUtils();
        
        for(int r=startRow+1; r<endRow; r++) {
            double relDistance = (double) (r-startRow) / (double)(endRow - startRow);
            Color newColor = colorUtils.interpolateColor(startColor, endColor, relDistance);
            table.setValueAt(newColor, r, COLUMN_COLOR);
        }
        
        
    }
    
    public static final int COLUMN_VALUE = 0;
    public static final int COLUMN_COLOR = 1;
    
    private JTable table;
    private String[] columnNames;
    private Double noDataValue;
    private boolean integerValues;
    
    public enum TableType {
        INTERVALS, VALUES;
    }
    
    public JTable getTable(){
        return table;
    }
    
    
    
}
