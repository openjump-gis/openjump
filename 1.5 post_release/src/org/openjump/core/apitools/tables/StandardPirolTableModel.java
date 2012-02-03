/*
 * Created on 30.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2451 $
 *  $Date: 2006-09-12 15:07:53 +0200 (Di, 12 Sep 2006) $
 *  $Id: StandardPirolTableModel.java 2451 2006-09-12 13:07:53Z LBST-PF-3\orahn $
 */
package org.openjump.core.apitools.tables;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

/**
 * standard implementation for a table model.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2451 $
 * 
 */
public abstract class StandardPirolTableModel extends AbstractTableModel {
    /**
     * each element of this vector represents a single row of the table. Internally, a row is represented
     * by a Object[].
     */
    protected Vector rows = new Vector();
    
    /**
     * array that holds information on the names for the table columns
     */
    protected String[] colNames = null;

    public StandardPirolTableModel(String[] colNames) {
        super();
        this.colNames = colNames;
    }

    
    public int getColumnCount() {
        return colNames.length;
    }

    public int getRowCount() {
        return rows.size();
    }

    public String getColumnName(int column) {
        return this.colNames[column];
    }
    
    /**
     * deletes all data from the table (also imforms the GUI)
     */
    public void clearTable(){
        int numRows = this.getRowCount();
        this.rows.clear();
        if (numRows >= 1)
            this.fireTableRowsDeleted(0, numRows - 1);
    }
    
    /**
     * @param columnName name of column to get the index for 
     * @return the index of the column with the given name
     */
    public int findColumn(String columnName) {
        for ( int i=0; i<this.colNames.length; i++ ){
            if (this.colNames[i].toLowerCase().equals(columnName.toLowerCase())) return i;
        }
        return -1;
    }
    
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        ((Object[])this.rows.get(rowIndex))[columnIndex] = aValue;
        this.fireTableCellUpdated(rowIndex,columnIndex);
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        return ((Object[])this.rows.get(rowIndex))[columnIndex];
    }

    public abstract boolean isCellEditable(int rowIndex, int columnIndex);
    public abstract Class getColumnClass(int columnIndex);
    
    /**
     * simple method to add a row to the table.
     * (It better matches the given column nummer and column types!!)
     *@param newRow
     */
    public void addRow(Object[] newRow){
        this.rows.add(newRow);
        this.fireTableRowsInserted(this.getRowCount()-1,this.getRowCount()-1);
    }


    /**
     *@return array containing the names of the columns
     */
    public String[] getColNames() {
        return colNames;
    }


    /**
     * Setting new column names will flush the table, if the new array has not the same length as the old one!
     *@param colNames array containing new column names
     */
    protected void setColNames(String[] colNames) {
        if (this.colNames.length != colNames.length)
            this.clearTable();
        this.colNames = colNames;
    }
    
    

}
