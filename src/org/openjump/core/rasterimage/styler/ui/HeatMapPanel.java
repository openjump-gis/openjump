package org.openjump.core.rasterimage.styler.ui;

import com.vividsolutions.jump.workbench.Logger;
import org.openjump.core.rasterimage.RasterHeatmapSymbology;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterSymbology;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class HeatMapPanel extends JPanel {

  private final Component parent;
  private final RasterImageLayer rasterImageLayer;
  private final int numbands;
  private final JSlider[] sliders;
  private final JButton[] colorButtons;

  public HeatMapPanel(Component parent, RasterImageLayer rasterImageLayer, int numbands) {
    this.parent = parent;
    this.rasterImageLayer = rasterImageLayer;
    this.numbands = numbands;
    this.sliders = new JSlider[numbands];
    this.colorButtons = new JButton[numbands];
    initComponents();
    fixComponents();
  }

  private void initComponents() {
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(365, 160));
    JPanel panel = new JPanel(new GridBagLayout());
    JScrollPane jsp = new JScrollPane(panel);
    jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    add(jsp, BorderLayout.CENTER);

    GridBagConstraints c = new GridBagConstraints();
    //c.gridx = 0;
    c.gridy = 0;
    for (int i = 0 ; i < numbands ; i++) {
      c.gridx = 0;
      c.gridwidth = 2;
      panel.add(new JLabel("Band " + (i+1)), c);
      c.gridx = 2;
      c.gridwidth = 8;
      sliders[i] = new JSlider(0,255);
      sliders[i].setMajorTickSpacing(32);
      sliders[i].setMinorTickSpacing(8);
      sliders[i].setPaintLabels(true);
      sliders[i].setPaintTicks(true);
      panel.add(sliders[i], c);
      c.gridx = 10;
      c.gridwidth = 1;
      colorButtons[i] = new ColorButton();
      panel.add(colorButtons[i], c);
      c.gridy++;
    }
  }


  private void fixComponents() {

  }

  public RasterSymbology getRasterStyler() throws Exception{
    java.util.List<Color> colors = new ArrayList<>();
    java.util.List<Integer> thresholds = new ArrayList<>();
    for (int i = 0 ; i < numbands ; i++) {
      colors.add(colorButtons[i].getBackground());
      thresholds.add(sliders[i].getValue());
    }
    return new RasterHeatmapSymbology(colors, thresholds, 255);
  }

  public void reset() {
    try {
      for (int i = 0 ; i < numbands ; i++) {
        sliders[i].setValue(127);
      }
    } catch (Exception ex) {
      Logger.error(ex);
    }
  }

  static class ColorButton extends JButton {
    ColorButton() {
      super();
      setBackground(new java.awt.Color(204, 204, 204));
      setBorder(BorderFactory.createEtchedBorder());
      setContentAreaFilled(false);
      setDoubleBuffered(true);
      setOpaque(true);
      setPreferredSize(new Dimension(40,25));
      this.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Color color = JColorChooser.showDialog(ColorButton.this, "Choose a color", getBackground());
          if(color != null){
            setBackground(color);
          }
        }
      });
    }
  }
}
