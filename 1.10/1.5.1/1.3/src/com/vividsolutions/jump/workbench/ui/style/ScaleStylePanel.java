package com.vividsolutions.jump.workbench.ui.style;

//[sstein 01.10.2005] changed to be able to work with real scale values

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.MathUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.ValidatingTextField;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import javax.swing.JButton;

import org.openjump.core.ui.util.ScreenScale;

public class ScaleStylePanel extends JPanel implements StylePanel {

    private static final ImageIcon MAX_SCALE_ICON = IconLoader.icon("Atom.gif");

    private static final ImageIcon MIN_SCALE_ICON = GUIUtil.resize(IconLoader
            .icon("World2.gif"), 32);

    private JCheckBox enableScaleDependentRenderingCheckBox = null;

    private JLabel smallestScaleLabel = null;

    private JLabel largestScaleLabel = null;

    private JLabel smallestScale1Label = null;

    private JLabel largestScale1Label = null;

    private JLabel currentScale1Label = null;

    private ValidatingTextField smallestScaleTextField = null;

    private ValidatingTextField largestScaleTextField = null;

    private ValidatingTextField currentScaleTextField = null;

    private Layer layer;

    private LayerViewPanel panel;

    private JLabel currentScaleLabel = null;

    private JPanel fillerPanel = null;

    private JPanel spacerPanelInTopLeftCorner = null;

    private JLabel unitsPerPixelLabel = null;

    private JButton hideAboveCurrentScaleButton = null;

    private JButton hideBelowCurrentScaleButton = null;

    private static final Color TEXT_FIELD_BACKGROUND_COLOUR = new JTextField()
            .getBackground();

    private JLabel smallestScaleIconLabel = null;

    private JLabel largestScaleIconLabel = null;

    private JPanel spacerPanelBelowCurrentScale = null;

    private JButton showAtThisScaleButton = null;

    private double scaleFactor = 0; 
    
    private JPanel getFillerPanel() {
        if (fillerPanel == null) {
            fillerPanel = new JPanel();
        }
        return fillerPanel;
    }

    public ScaleStylePanel(Layer layer, LayerViewPanel panel) {
        super();
        initialize();
        this.layer = layer;
        this.panel = panel;
        
        /**
         * [sstein : 01.10.2005]
         * modifications to show the real scale and not the internal values 
         */
        double internalScale = this.currentScale();
		double realScale = ScreenScale.getHorizontalMapScale(panel.getViewport());
		this.scaleFactor = internalScale / realScale;
		
		Double internalMinScale=layer.getMinScale();
		Double realMinScale = null;
		if (internalMinScale != null){
			//-- not sure to use Math.floor (with respect to zoom into cm space
			//   but somehow necessary to avoid display like 6379.9999999 [sstein]
			realMinScale = new Double (Math.floor(internalMinScale.doubleValue()/this.scaleFactor));
		}
			
		Double internalMaxScale = layer.getMaxScale();
		Double realMaxScale = null;
		if(internalMaxScale != null){
			realMaxScale = new Double(Math.floor(internalMaxScale.doubleValue()/this.scaleFactor));
		}		
        smallestScaleTextField.setText(formatScaleLosslessly(realMinScale));
        largestScaleTextField
                .setText(formatScaleLosslessly(realMaxScale));
        currentScaleTextField.setText(formatScaleLossily(realScale));
        enableScaleDependentRenderingCheckBox.setSelected(layer
                .isScaleDependentRenderingEnabled());
        updateComponents();
    }

    protected double currentScale() {
        return 1d / panel.getViewport().getScale();
    }

    private String formatScaleLosslessly(Double scale) {
        return scale != null ? formatScaleLosslessly(scale.doubleValue()) : "";
    }

    private String formatScaleLosslessly(double scale) {
        // Unlike #formatCurrentScale, this method is lossless [Jon Aquino
        // 2005-03-31]
        return scale == (int) scale ? "" + (int) scale : "" + scale;
    }

    private void initialize() {
        GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
        smallestScaleIconLabel = new JLabel();
        largestScaleIconLabel = new JLabel();
        unitsPerPixelLabel = new JLabel();
        GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
        largestScaleLabel = new JLabel();
        smallestScaleLabel = new JLabel();
        largestScale1Label = new JLabel();
        currentScale1Label = new JLabel();
        smallestScale1Label = new JLabel();
        currentScaleLabel = new JLabel();
        GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        gridBagConstraints1.gridx = 1;
        gridBagConstraints1.gridy = 4;
        smallestScaleLabel.setText(I18N.get("ui.style.ScaleStylePanel.smallest-scale"));
        //smallestScaleLabel.setToolTipText(I18N.get("ui.style.ScaleStylePanel.larger-units-pixel")); //[sstein] 8.Mar.2009 
        smallestScaleIconLabel.setIcon(MIN_SCALE_ICON);
        largestScaleIconLabel.setIcon(MAX_SCALE_ICON);
        gridBagConstraints5.gridx = 3;
        gridBagConstraints5.gridy = 6;
        smallestScale1Label.setText("1:");
        currentScaleLabel.setText(I18N.get("ui.style.ScaleStylePanel.current-scale"));
        gridBagConstraints6.gridx = 3;
        gridBagConstraints6.gridy = 8;
        gridBagConstraints16.gridx = 3;
        gridBagConstraints16.gridy = 2;
        largestScale1Label.setText("1:");
        currentScale1Label.setText("1:");
        gridBagConstraints7.gridx = 4;
        gridBagConstraints7.gridy = 6;
        gridBagConstraints7.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints7.insets = new java.awt.Insets(0, 2, 0, 0);
        gridBagConstraints17.gridx = 4;
        gridBagConstraints17.gridy = 2;
        gridBagConstraints17.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints17.insets = new java.awt.Insets(0, 2, 0, 0);
        gridBagConstraints9.gridx = 4;
        gridBagConstraints9.gridy = 8;
        gridBagConstraints9.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints9.insets = new java.awt.Insets(0, 2, 0, 0);
        gridBagConstraints1.gridwidth = 5;
        gridBagConstraints1.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints3.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints3.insets = new java.awt.Insets(5, 5, 5, 0);
        gridBagConstraints4.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints4.insets = new java.awt.Insets(5, 5, 5, 0);
        gridBagConstraints5.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints6.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints16.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints16.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints10.gridx = 2;
        gridBagConstraints10.gridy = 2;
        gridBagConstraints10.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints10.insets = new java.awt.Insets(0, 5, 0, 0);
        gridBagConstraints10.gridwidth = 1;
        gridBagConstraints3.gridx = 2;
        gridBagConstraints3.gridy = 6;
        gridBagConstraints4.gridx = 2;
        gridBagConstraints4.gridy = 8;
        gridBagConstraints11.gridx = 7;
        gridBagConstraints11.gridy = 16;
        gridBagConstraints11.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints11.weightx = 1.0D;
        gridBagConstraints11.weighty = 1.0D;
        gridBagConstraints12.gridx = 0;
        gridBagConstraints12.gridy = 0;
        gridBagConstraints12.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints13.gridx = 4;
        gridBagConstraints13.gridy = 5;
        //unitsPerPixelLabel.setText(I18N.get("ui.style.ScaleStylePanel.units-pixel")); //[sstein] 8.Mar.2009 
        gridBagConstraints14.gridx = 7;
        gridBagConstraints14.gridy = 6;
        gridBagConstraints14.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints14.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints15.gridx = 7;
        gridBagConstraints15.gridy = 8;
        gridBagConstraints15.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints15.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getShowAtThisScaleButton().setMargin(new Insets(0, 0, 0, 0));
        getHideAboveCurrentScaleButton().setMargin(new Insets(0, 0, 0, 0));
        getHideBelowCurrentScaleButton().setMargin(new Insets(0, 0, 0, 0));
        getShowAtThisScaleButton().setMargin(new Insets(0, 0, 0, 0));
        GUIUtil.shrinkFont(getHideAboveCurrentScaleButton());
        GUIUtil.shrinkFont(getHideBelowCurrentScaleButton());
        GUIUtil.shrinkFont(getShowAtThisScaleButton());
        GUIUtil.shrinkFont(unitsPerPixelLabel);
        largestScaleLabel.setText(I18N.get("ui.style.ScaleStylePanel.largest-scale"));
        //largestScaleLabel.setToolTipText(I18N.get("ui.style.ScaleStylePanel.smaller-units-pixel")); //[sstein] 8.Mar.2009 
        gridBagConstraints2.gridx = 1;
        gridBagConstraints2.gridy = 6;
        gridBagConstraints31.gridx = 1;
        gridBagConstraints31.gridy = 8;
        gridBagConstraints2.insets = new java.awt.Insets(5, 40, 5, 0);
        gridBagConstraints31.insets = new java.awt.Insets(5, 40, 5, 0);
        gridBagConstraints18.gridx = 2;
        gridBagConstraints18.gridy = 3;
        gridBagConstraints21.gridx = 7;
        gridBagConstraints21.gridy = 2;
        gridBagConstraints21.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints21.insets = new java.awt.Insets(4, 4, 4, 4);
        this.add(getSpacerPanelInTopLeftCorner(), gridBagConstraints12);
        this.add(getShowAtThisScaleButton(), gridBagConstraints21);
        this.add(getEnableScaleDependentRenderingCheckBox(),
                gridBagConstraints1);
        this.add(currentScaleLabel, gridBagConstraints10);
        this.add(smallestScaleLabel, gridBagConstraints3);
        this.add(largestScaleLabel, gridBagConstraints4);
        this.add(getFillerPanel(), gridBagConstraints11);
        this.add(smallestScale1Label, gridBagConstraints5);
        this.add(largestScale1Label, gridBagConstraints6);
        this.add(unitsPerPixelLabel, gridBagConstraints13);
        this.add(getHideAboveCurrentScaleButton(), gridBagConstraints14);
        this.add(getHideBelowCurrentScaleButton(), gridBagConstraints15);
        this.add(currentScale1Label, gridBagConstraints16);
        this.add(smallestScaleIconLabel, gridBagConstraints2);
        this.add(largestScaleIconLabel, gridBagConstraints31);
        this.add(getSpacerPanelBelowCurrentScale(), gridBagConstraints18);
        this.add(getSmallestScaleTextField(), gridBagConstraints7);
        this.add(getLargestScaleTextField(), gridBagConstraints9);
        this.add(getCurrentScaleTextField(), gridBagConstraints17);
    }

    public String getTitle() {
        return I18N.get("ui.style.ScaleStylePanel.scale");
    }

    public void updateStyles() {
        layer.getLayerManager().deferFiringEvents(new Runnable() {
            public void run() {
                layer.setMinScale(getSmallestScale());
                layer.setMaxScale(getLargestScale());
                layer
                        .setScaleDependentRenderingEnabled(enableScaleDependentRenderingCheckBox
                                .isSelected());
            }
        });
        layer.fireAppearanceChanged();
    }

    public String validateInput() {
        if (getSmallestScale() != null
                && getLargestScale() != null
                && getLargestScale().doubleValue() > getSmallestScale()
                        .doubleValue()) {
            return I18N.get("ui.style.ScaleStylePanel.units-pixel-at-smallest-scale-must-be-larger-than-units-pixel-at-largest-scale");
        }
        if (getLargestScale() != null && getLargestScale().doubleValue() == 0) {
            return I18N.get("ui.style.ScaleStylePanel.units-pixel-at-largest-scale-must-be-greater-than-0");
        }
        if (getSmallestScale() != null && getSmallestScale().doubleValue() == 0) {
            return I18N.get("ui.style.ScaleStylePanel.units-pixel-at-smallest-scale-must-be-greater-than-0");
        }
        return null;
    }

    private Double getLargestScale() {
    	//[sstein 01.10.2005] 
    	// change to be able to work with real scale values
        return largestScaleTextField.getText().trim().length() > 0 ? new Double(
            largestScaleTextField.getDouble()*this.scaleFactor)
                : null;
    }

    private Double getSmallestScale() {
    	//[sstein 01.10.2005] 
    	// change to be able to work with real scale values
        return smallestScaleTextField.getText().trim().length() > 0 ? new Double(
            smallestScaleTextField.getDouble() * this.scaleFactor)
                : null;
    }

    private JCheckBox getEnableScaleDependentRenderingCheckBox() {
        if (enableScaleDependentRenderingCheckBox == null) {
            enableScaleDependentRenderingCheckBox = new JCheckBox();
            enableScaleDependentRenderingCheckBox
                    .setText(I18N.get("ui.style.ScaleStylePanel.only-show-layer-when-scale-is-between"));
            enableScaleDependentRenderingCheckBox
                    .addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            updateComponents();
                        }
                    });
        }
        return enableScaleDependentRenderingCheckBox;
    }

    private void updateComponents() {
        smallestScaleTextField
                .setBackground(enableScaleDependentRenderingCheckBox
                        .isSelected() ? TEXT_FIELD_BACKGROUND_COLOUR
                        : getBackground());
        largestScaleTextField
                .setBackground(enableScaleDependentRenderingCheckBox
                        .isSelected() ? TEXT_FIELD_BACKGROUND_COLOUR
                        : getBackground());
        unitsPerPixelLabel.setEnabled(enableScaleDependentRenderingCheckBox
                .isSelected());
        hideAboveCurrentScaleButton
                .setEnabled(enableScaleDependentRenderingCheckBox.isSelected());
        smallestScaleLabel.setEnabled(enableScaleDependentRenderingCheckBox
                .isSelected());
        smallestScale1Label.setEnabled(enableScaleDependentRenderingCheckBox
                .isSelected());
        smallestScaleTextField.setEnabled(enableScaleDependentRenderingCheckBox
                .isSelected());
        hideBelowCurrentScaleButton
                .setEnabled(enableScaleDependentRenderingCheckBox.isSelected());
        largestScaleLabel.setEnabled(enableScaleDependentRenderingCheckBox
                .isSelected());
        largestScale1Label.setEnabled(enableScaleDependentRenderingCheckBox
                .isSelected());
        largestScaleTextField.setEnabled(enableScaleDependentRenderingCheckBox
                .isSelected());
    }

    private ValidatingTextField getSmallestScaleTextField() {
        if (smallestScaleTextField == null) {
            smallestScaleTextField = createValidatingTextField();
        }
        return smallestScaleTextField;
    }

    private ValidatingTextField getLargestScaleTextField() {
        if (largestScaleTextField == null) {
            largestScaleTextField = createValidatingTextField();
        }
        return largestScaleTextField;
    }

    private ValidatingTextField getCurrentScaleTextField() {
        if (currentScaleTextField == null) {
            currentScaleTextField = createValidatingTextField();
            currentScaleTextField.setEditable(false);
            currentScaleTextField.setBackground(getBackground());
        }
        return currentScaleTextField;
    }

    private ValidatingTextField createValidatingTextField() {
        // Use GreaterThanOrEqualValidator rather than GreaterThanValidator,
        // which won't let the user type a "0" even if they want to enter "0.1"
        // [Jon Aquino 2005-03-22]
        return new ValidatingTextField(
                "",
                7,
                SwingConstants.LEFT,
                new ValidatingTextField.CompositeValidator(
                        new ValidatingTextField.Validator[] {
                                ValidatingTextField.DOUBLE_VALIDATOR,
                                new ValidatingTextField.GreaterThanOrEqualValidator(
                                        0) }),
                ValidatingTextField.DUMMY_CLEANER);
    }

    /**
     * Nicer formatting, but the expense of possibly losing precision.
     */
    private String formatScaleLossily(double x) {
        if (1 <= x && x <= 1E6) {
            return new DecimalFormat("#").format(x);
        }
        if (1E-6 <= x && x <= 1) {
            return new DecimalFormat("0.000000").format(x);
        }
        return new DecimalFormat("0.#E0").format(x);
    }

    /**
     * This method initializes spacerPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getSpacerPanelInTopLeftCorner() {
        if (spacerPanelInTopLeftCorner == null) {
            spacerPanelInTopLeftCorner = new JPanel();
            spacerPanelInTopLeftCorner.setLayout(new GridBagLayout());
        }
        return spacerPanelInTopLeftCorner;
    }

    private JButton getHideAboveCurrentScaleButton() {
        if (hideAboveCurrentScaleButton == null) {
            hideAboveCurrentScaleButton = new JButton();
            hideAboveCurrentScaleButton.setText(I18N.get("ui.style.ScaleStylePanel.hide-above-current-scale"));
            hideAboveCurrentScaleButton
                    .addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                        	//[sstein] changed to show real scale instead currentScale
                            double realScale = 1/scaleFactor*currentScale();  
                            smallestScaleTextField
                                    .setText(formatScaleLossily(roundFirstSignificantFigureUp(realScale)));
                        }
                    });
        }
        return hideAboveCurrentScaleButton;
    }

    /**
     * Round the first significant figure up
     */
    private double roundFirstSignificantFigureUp(double x) {
        return roundFirstSignificantFigure(x, 1);
    }

    /**
     * Round the first significant figure down
     */
    private double roundFirstSignificantFigureDown(double x) {
        return roundFirstSignificantFigure(x, 0);
    }

    private static double roundFirstSignificantFigure(double x, int i) {
        double scale = Math.pow(10, Math.floor(MathUtil.base10Log(x)));
        int firstSignificantFigure = (int) Math.floor(x / scale);
        return (firstSignificantFigure + i) * scale;
    }

    private JButton getHideBelowCurrentScaleButton() {
        if (hideBelowCurrentScaleButton == null) {
            hideBelowCurrentScaleButton = new JButton();
            hideBelowCurrentScaleButton.setText(I18N.get("ui.style.ScaleStylePanel.hide-below-current-scale"));
            hideBelowCurrentScaleButton
                    .addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                        	//[sstein] changed to show real scale instead currentScale
                            double realScale = 1/scaleFactor*currentScale();                            
                            largestScaleTextField
                                    .setText(formatScaleLossily(roundFirstSignificantFigureDown(realScale)));
                        }
                    });
        }
        return hideBelowCurrentScaleButton;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getSpacerPanelBelowCurrentScale() {
        if (spacerPanelBelowCurrentScale == null) {
            spacerPanelBelowCurrentScale = new JPanel();
            spacerPanelBelowCurrentScale.setLayout(new GridBagLayout());
            spacerPanelBelowCurrentScale
                    .setPreferredSize(new java.awt.Dimension(0, 20));
        }
        return spacerPanelBelowCurrentScale;
    }

    /**
     * This method initializes showAtThisScaleButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getShowAtThisScaleButton() {
        if (showAtThisScaleButton == null) {
            showAtThisScaleButton = new JButton();
            showAtThisScaleButton.setText(I18N.get("ui.style.ScaleStylePanel.show-at-this-scale"));
            showAtThisScaleButton
                    .addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            if (!enableScaleDependentRenderingCheckBox
                                    .isSelected()) {
                                enableScaleDependentRenderingCheckBox.doClick();
                            }
                            if (getSmallestScale() != null
                                    && currentScale() > getSmallestScale()
                                            .doubleValue()) {
                                getHideAboveCurrentScaleButton().doClick();
                            }
                            if (getLargestScale() != null
                                    && currentScale() < getLargestScale()
                                            .doubleValue()) {
                                getHideBelowCurrentScaleButton().doClick();
                            }
                        }
                    });
        }
        return showAtThisScaleButton;
    }

    public static void main(String[] args) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException {
        /*
         * System.out.println(roundFirstSignificantFigure(123, 1));
         * System.out.println(roundFirstSignificantFigure(123, 0));
         * System.out.println(roundFirstSignificantFigure(789, 1));
         * System.out.println(roundFirstSignificantFigure(789, 0));
         * System.out.println(roundFirstSignificantFigure(.00123, 1));
         * System.out.println(roundFirstSignificantFigure(.00123, 0));
         * System.out.println(roundFirstSignificantFigure(.00789, 1));
         * System.out.println(roundFirstSignificantFigure(.00789, 0));
         */
        /*
         * System.out.println(new DecimalFormat("0.#E0").format(0.00000123));
         * System.exit(0);
         */
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JDialog dialog = new JDialog();
        dialog.getContentPane().add(new ScaleStylePanel(new Layer() {
            {
                setMinScale(new Double(2));
                setMaxScale(new Double(1));
            }
        }, null) {
            protected double currentScale() {
                return .000000123;
            }
        });
        dialog.pack();
        dialog.setVisible(true);
    }
} //  @jve:decl-index=0:visual-constraint="10,10"
