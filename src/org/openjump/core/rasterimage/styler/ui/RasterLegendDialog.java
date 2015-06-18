package org.openjump.core.rasterimage.styler.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.TreeMap;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.openjump.core.rasterimage.RasterSymbology;

/**
 *
 * @author GeomaticaEAmbiente
 */
public class RasterLegendDialog extends JDialog{
    
    public RasterLegendDialog(java.awt.Frame parent, boolean modal, RasterSymbology rasterStyler, double noDataValue, String rasterName) throws Exception{
        super(parent, modal);
        
        this.rasterStyler = rasterStyler;
        this.noDataValue = noDataValue;
        this.rasterName = rasterName;
        
        fixComponents();
    }
    
    
    private void fixComponents() throws Exception{
        
        this.setTitle(bundle.getString("LegendDialog.Title.text"));       
     
        TreeMap<Double,Color> colorMapEntries = rasterStyler.getColorMapEntries_tm();
        
        RasterSymbology.ColorMapType type = rasterStyler.getColorMapType();
        
        JScrollPane scrollPane = null;

        if(type.equals(RasterSymbology.ColorMapType.INTERVALS)){
            scrollPane = getInterval(colorMapEntries);
        }else if (type.equals(RasterSymbology.ColorMapType.RAMP)){
            scrollPane = getGradient(colorMapEntries);
        }
        
                
        add(scrollPane);

    }
    
    private JScrollPane getInterval(TreeMap<Double,Color>  colorMapEntry_tm) throws Exception{
        
        ColorsLabelLegendComponent component = new ColorsLabelLegendComponent(colorMapEntry_tm, noDataValue, rasterName);
        component.setPreferredSize(new Dimension(200, 400));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(component);
        panel.setVisible(true);
        
        JScrollPane sc = new JScrollPane(panel);
        return sc;
        
    }
    
    private JScrollPane getGradient(TreeMap<Double,Color> colorMapEntry) throws Exception{
        
        GradientLabelLegendComponent component = new GradientLabelLegendComponent(colorMapEntry, noDataValue, rasterName);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(component);
        panel.setVisible(true);
        
        JScrollPane sc = new JScrollPane(panel);

        return sc;
        
    }
    
    private final RasterSymbology rasterStyler;
    private final double noDataValue;
    private final String rasterName;
    private final java.util.ResourceBundle bundle = 
            java.util.ResourceBundle.getBundle("org/openjump/core/rasterimage/styler/resources/Bundle"); // NOI18N
    
    
    
}
