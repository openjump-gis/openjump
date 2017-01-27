/*
 * Created on 03.07.2005
 *
 * CVS information:
 *  $Author: LBST-PF-3\orahn $
 *  $Date: 2006-09-12 12:57:25 +0000 (Di, 12 Sep 2006) $
 *  $ID$
 *  $Rev: 2446 $
 *  $Id: RasterImageLayerControllPanel.java 2446 2006-09-12 12:57:25Z LBST-PF-3\orahn $
 *
 */
package org.openjump.core.ui.plugin.layer.pirolraster.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.swing.ValueChecker;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.style.StylePanel;

/**
 * 
 * Panel that contains controls to customize the appearance of a RasterImage
 * layer.
 * 
 * @author Ole Rahn <br>
 * <br>
 *         FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck, <br>
 *         Project: PIROL (2005), <br>
 *         Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2446 $ [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 * 
 * @version $Rev: 4221 $ [Giuseppe Aruta] Dic 23 2014- Derived from
 *          ChangeRasterImageStyleDialog
 */
public class RasterTransparencyPanel extends JPanel implements ValueChecker,
        ActionListener, StylePanel {
    private static final long serialVersionUID = 619781257815627447L;

    protected JColorChooser colorChooser = new JColorChooser();
    protected RasterImageLayer rasterImageLayer = null;
    protected JSlider transparencySlider = new JSlider(),
            speedSlider = new JSlider();
    protected Dictionary sliderLabelDictionary = new Hashtable();
    protected JCheckBox useTransCB = null;

    // protected JButton useTransCB;
    // JButton useTransCB = new JButton("Click");

    public RasterTransparencyPanel(RasterImageLayer rasterImageLayer) {
        super(new BorderLayout());

        this.rasterImageLayer = rasterImageLayer;

        this.setupGui();
    }

    @SuppressWarnings("unchecked")
    public void setupGui() {

        JPanel transparencyOnOffPanel = new JPanel();

        transparencyOnOffPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        transparencyOnOffPanel
                .add(new JLabel(
                        I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageLayerControllPanel.Do-you-want-a-color-to-be-transparent")));
        // this.useTransCB =new JButton(IconLoader.icon("color_wheel.png"));

        this.useTransCB = new JCheckBox();
        transparencyOnOffPanel.add(this.useTransCB);
        this.useTransCB.addActionListener(this);

        this.colorChooser.getSelectionModel().setSelectedColor(
                this.rasterImageLayer.getTransparentColor());
        this.colorChooser
                .setToolTipText(I18N
                        .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageLayerControllPanel.Choose-transparent-color"));
        this.colorChooser.setBorder(BorderFactory.createEtchedBorder());
        JPanel slidersPanel = new JPanel();
        slidersPanel.setLayout(new GridLayout(2, 1));

        JPanel transparencySliderPanel = new JPanel();
        transparencySliderPanel.setLayout(new GridLayout(2, 1));
        transparencySliderPanel
                .add(new JLabel(
                        " "     + I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageLayerControllPanel.set-overall-transparency")));

        for (int i = 0; i <= 100; i += 25) {
            this.sliderLabelDictionary.put(new Integer(i), new JLabel(i + "%"));
        }
        this.transparencySlider.setLabelTable(this.sliderLabelDictionary);
        this.transparencySlider.setPaintLabels(true);

        this.transparencySlider.setMaximum(100);
        this.transparencySlider.setMinimum(0);
        this.transparencySlider.setMajorTickSpacing(10);
        this.transparencySlider.setMinorTickSpacing(5);
        this.transparencySlider.setPaintTicks(true);

        this.transparencySlider.setMinimumSize(new Dimension(150, 20));
        this.transparencySlider.setValue((int) (this.rasterImageLayer
                .getTransparencyLevel() * 100));

        transparencySliderPanel.add(this.transparencySlider);

        slidersPanel.add(transparencySliderPanel);

        JPanel speedSliderPanel = new JPanel();
        speedSliderPanel.setLayout(new GridLayout(2, 1));
        speedSliderPanel
                .add(new JLabel(
                        I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageLayerControllPanel.processing-speed")));

        this.speedSlider.setLabelTable(this.sliderLabelDictionary);
        this.speedSlider.setPaintLabels(true);

        this.speedSlider.setMaximum(85);
        this.speedSlider.setMinimum(15);
        this.speedSlider.setMajorTickSpacing(10);
        this.speedSlider.setMinorTickSpacing(5);
        this.speedSlider.setPaintTicks(true);

        this.speedSlider.setMinimumSize(new Dimension(150, 20));
        this.speedSlider.setValue((int) ((1.0 - RasterImageLayer
                .getFreeRamFactor()) * 100));

        speedSliderPanel.add(this.speedSlider);

        slidersPanel.add(speedSliderPanel);

        this.add(slidersPanel, BorderLayout.SOUTH);
        if (rasterImageLayer.getNumBands() > 1) {
            this.setSize(new Dimension(400, 500));
            this.setPreferredSize(new Dimension(400, 500));
            this.add(this.colorChooser, BorderLayout.CENTER);
            this.add(transparencyOnOffPanel, BorderLayout.NORTH);
            this.add(slidersPanel, BorderLayout.SOUTH);
        } else {
            this.add(slidersPanel, BorderLayout.NORTH);
        }

        // this.setPreferredSize(new Dimension(300,
        // this.colorChooser.getHeight() + 50));

        this.doLayout();

        // setup checkbox (and color chooser)
        this.useTransCB
                .setSelected(this.rasterImageLayer.getTransparentColor() != null);
        this.actionPerformed(null);
    }

    /**
     * @inheritDoc
     */
    public boolean areValuesOk() {

        this.rasterImageLayer.setFiringAppearanceEvents(false);

        if (this.useTransCB.isSelected()) {
            this.rasterImageLayer.setTransparentColor(this.colorChooser
                    .getColor());
        } else {
            this.rasterImageLayer.setTransparentColor(null);
        }
        int newTransparencyValue = this.transparencySlider.getValue();
        this.rasterImageLayer
                .setTransparencyLevelInPercent(newTransparencyValue);

        int newFreeRamValue = this.speedSlider.getValue();
        RasterImageLayer.setFreeRamFactor(1.0 - newFreeRamValue / 100d);

        this.rasterImageLayer.setFiringAppearanceEvents(true);

        return true;
    }

    /**
     * @inheritDoc
     */
    public void actionPerformed(ActionEvent event) {
        // the checkbox was toogled

        // diabling the color chooser has no effect!
        this.colorChooser.setEnabled(this.useTransCB.isSelected());
        this.colorChooser.setVisible(this.useTransCB.isSelected());
    }

    public String getTitle() {
        return I18N.get("ui.renderer.style.ColorThemingPanel.transparency");

    }

    public void updateStyles() {
        this.rasterImageLayer.setFiringAppearanceEvents(false);

        if (this.useTransCB.isSelected()) {
            this.rasterImageLayer.setTransparentColor(this.colorChooser
                    .getColor());
        } else {
            this.rasterImageLayer.setTransparentColor(null);
        }
        int newTransparencyValue = this.transparencySlider.getValue();
        this.rasterImageLayer
                .setTransparencyLevelInPercent(newTransparencyValue);

        int newFreeRamValue = this.speedSlider.getValue();
        RasterImageLayer.setFreeRamFactor(1.0 - newFreeRamValue / 100d);

        this.rasterImageLayer.setFiringAppearanceEvents(true);

        return;

    }

    public String validateInput() {
        // TODO Auto-generated method stub
        return null;
    }

    public void okPressed() {
        return;
    }
}
