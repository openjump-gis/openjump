package org.openjump.core.rasterimage.styler.ui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Class that allows to add the GradientCanvas component to the items of a JComboBox.
 * @author GeomaticaEAmbiente
 */
public class GradientComboRenderer implements ListCellRenderer{
        
    private final float width;
    private final float height;
    
    public GradientComboRenderer(float width, float height) {
        
        this.width = width;
        this.height = height;
                
    }

    @Override
    public Component getListCellRendererComponent(JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        
        GradientCanvas canvas = (GradientCanvas) value;
        canvas.setPreferredSize(new Dimension((int) width, (int) height));

        return canvas;
    }
        
}  
