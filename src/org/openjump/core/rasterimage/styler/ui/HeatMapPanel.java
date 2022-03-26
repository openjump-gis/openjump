package org.openjump.core.rasterimage.styler.ui;

import com.vividsolutions.jump.workbench.Logger;
import org.openjump.core.rasterimage.RasterImageLayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    GridBagConstraints c = new GridBagConstraints();
    this.setLayout(new GridBagLayout());
    //c.gridx = 0;
    c.gridy = 0;
    for (int i = 0 ; i < numbands ; i++) {
      c.gridx = 0;
      sliders[i] = new JSlider(0,255);
      add(sliders[i], c);
      c.gridx = 1;
      colorButtons[i] = new JButton();
      c.gridy++;
    }
  }


  private void fixComponents() {

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
      this.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JColorChooser jcc = new JColorChooser();
          jcc.setVisible(true);
          ColorButton.this.setBackground(jcc.getColor());
        }
      });
    }
  }
}
