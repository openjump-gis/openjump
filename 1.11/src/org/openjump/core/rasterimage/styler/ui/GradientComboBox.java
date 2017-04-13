package org.openjump.core.rasterimage.styler.ui;

import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import org.openjump.core.rasterimage.styler.ColorMapEntry;


/**
 * This class create a combo box for gradients.
 * @author Paola
 */
public class GradientComboBox extends JComboBox implements ActionListener {

    DefaultComboBoxModel model = new DefaultComboBoxModel();;
    
    /**
     * Constructor to create a combobox for gradient.  
     */
    public GradientComboBox() {
        super();
        setModel(model);
        setModel(model);
    }

    @Override
    public void addItem(Object anObject) {

        if(anObject instanceof GradientCanvas) {
            
            GradientCanvas gradientCanvas = (GradientCanvas) anObject;
            
            int width = gradientCanvas.getWidth();
            int height = gradientCanvas.getHeight();
            GradientCanvas.GradientType type = gradientCanvas.getType();
            ColorMapEntry[] colorMapEntries = gradientCanvas.getColorMapEntries();
            GradientCanvas gradient = new GradientCanvas(colorMapEntries, width, height, type);
            model.addElement(gradient);
            
        } else {
            super.addItem(anObject);
        }        
        
    }

            
    Object object  = model.getSelectedItem();
    GradientCanvas canvas = (GradientCanvas) object;
    
}