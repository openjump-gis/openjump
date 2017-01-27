package org.openjump.core.rasterimage.styler.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.openjump.core.rasterimage.styler.ColorMapEntry;
import org.openjump.core.rasterimage.styler.ColorUtils;

import com.vividsolutions.jump.workbench.Logger;

/**
 *
 * @author GeomaticaEAmbiente
 */
public class GradientTablePanel extends ColorsTablePanel implements TableModelListener{

    /**
     * Constructor for creating the panel that contains the table with the gradient's colors.
     * @param parent
     * @param tableType INTERVALS
     * @param colorMapEntries The colors that form the gradient
     * @param noDataValue
     * @param integerValues
     * @param panel Panel in which to add the table.
     */
    public GradientTablePanel(Component parent, TableType tableType,
            ColorMapEntry[] colorMapEntries, Double noDataValue, boolean integerValues, JPanel panel) {
        super(parent, tableType, colorMapEntries, noDataValue, integerValues);
               
        this.panel = panel;
        table = getTable();
        table.setDefaultEditor(Color.class, new ColorEditor(parent));
        table.getModel().addTableModelListener(this);
        
        
    }
    
    @Override
    public void addRows(){
        
        //updateGradient preventing the update of gradient panel when a row is added
        updateGradient = false;
        try{
            int selRows [] = table.getSelectedRows();
            if (selRows.length > 0) {
                
                Double value;
                Color startColor;
                Color endColor;
               
                //Check if it is selected the first row
                if(selRows[0] == 0){
                    //Check if the first row value is 0. If 0 also the new line is 0.
                    startColor = (Color) table.getValueAt(selRows[0], COLUMN_COLOR);
                    endColor = (Color) table.getValueAt(selRows[0], COLUMN_COLOR);
                    
                    value = (Double) table.getValueAt(selRows[0], COLUMN_VALUE);
                    if(value != 0){
                        value = GUIUtils.round((Double) table.getValueAt(selRows[0], COLUMN_VALUE)/2,2); //sarebbe (0 + valore) /2
                    }                    
                  
                } else {
                    
                    startColor = (Color) table.getValueAt(selRows[0] - 1, COLUMN_COLOR);
                    endColor = (Color) table.getValueAt(selRows[0], COLUMN_COLOR);
                    
                    value = GUIUtils.round(((Double) table.getValueAt(selRows[0] -1, COLUMN_VALUE) + 
                            (Double) table.getValueAt(selRows[0] , COLUMN_VALUE))/2,2);
                }

                ColorUtils colorUtils = new ColorUtils();        
                Color newColor = colorUtils.interpolateColor(startColor, endColor, 0.5);

                ((DefaultTableModel)table.getModel())
                        .insertRow(selRows[0],new Object[]{value, newColor});

            } else {
                
                // Get last row colour
                Color startColor = (Color) table.getValueAt(table.getRowCount()-1, COLUMN_COLOR); 
                Color endColor = (Color) table.getValueAt(table.getRowCount()-1, COLUMN_COLOR);
                
                ColorUtils colorUtils = new ColorUtils();        
                Color newColor = colorUtils.interpolateColor(startColor, endColor, 0.5);
                
                Double value = (Double) table.getValueAt(table.getRowCount()-1, COLUMN_VALUE); 

                ((DefaultTableModel)table.getModel())
                        .addRow(new Object[]{value, newColor});                

                // select the last row
                int lastRow = table.getRowCount() - 1;
                table.getSelectionModel().setSelectionInterval(lastRow, lastRow);
                table.scrollRectToVisible(table.getCellRect(lastRow, 1, true));
            }
            
        }catch(Exception ex){
            updateGradient = true;
            Logger.error(ex);
        }
        
        updateGradient = true;
        
    }
    
    

    @Override
    public void tableChanged(TableModelEvent e) {
        
        int rows = table.getRowCount();
        //Check if the numbero of rows are more than 0
        if(updateGradient == true && rows > 0){
                        
            Color[] colors = new Color[rows];
            Double[] values = new Double[rows];
            for(int n=0; n<rows; n++){
                colors[n] = (Color) table.getValueAt(n, COLUMN_COLOR);
                values[n] = (Double) table.getValueAt(n, COLUMN_VALUE);
            }
            
            double tempValue = values[0];
            
            for(int v=1; v<values.length; v++){
                if(values[v] <= tempValue ||  values[v]>1)return;
                
                tempValue = values[v];
            }
            
            if(values.length < 2) return;  

            List<ColorMapEntry> cme_list = new ArrayList<ColorMapEntry>();
            for(int n=0; n<rows; n++){
                if(values[n]!=null){
                    cme_list.add(new ColorMapEntry(values[n], colors[n]));
                }
            }
            
            ColorMapEntry[] cme = cme_list.toArray(new ColorMapEntry[cme_list.size()]);

            GUIUtils gui = new GUIUtils();
            gui.setGradientPanel(panel, cme);

            this.updateUI();
        
        }
    }
    
    private final JPanel panel;
    private final JTable table;
    private boolean updateGradient = true;
    
    
}
