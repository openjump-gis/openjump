
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.util.Iterator;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.vividsolutions.jump.workbench.driver.AbstractDriver;
import com.vividsolutions.jump.workbench.model.Layer;

/**
 * Base Driver Dialog.
 */
//<<TODO:DESIGN>> Strange that every driver-panel must supply its own OK and Cancel
//buttons. But I remember there was a reason for this. Revisit this design. [Jon Aquino]
public class DriverDialog extends JDialog implements ActionListener {
    JPanel centrePanel = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    BorderLayout borderLayout2 = new BorderLayout();
    private java.util.List drivers;
    private AbstractDriverPanel dummyDriverPanel = new AbstractDriverPanel() {
            public boolean wasOKPressed() {
                return false;
            }

            public String getValidationError() {
                return null;
            }

            public void addActionListener(ActionListener l) {
            }

            public void removeActionListener(ActionListener l) {
            }
        };

    protected AbstractDriverPanel currentDriverPanel = dummyDriverPanel;
    JPanel northPanel = new JPanel();
    JPanel innerNorthPanel = new JPanel();
    JLabel driverLabel = new JLabel();
    JComboBox driverComboBox = new JComboBox();
    FlowLayout flowLayout1 = new FlowLayout();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    private boolean closeInitiatedByPanelButton;
    private boolean okPressed;
    private WeakHashMap layerToDriverPanelCacheMap = new WeakHashMap();
    private Layer layer;

    public DriverDialog(Frame frame, String title, boolean modal) {
        super(frame, title, modal);

        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public DriverDialog() {
        this(null, "", false);
    }

    public void initialize(java.util.List drivers) {
    	/* Moved this code into show() to ensure the comboBox is updated
    	 * if the list of drivers is updated during run-time
    	 */
    	/*
		for (Iterator i = drivers.iterator(); i.hasNext();) {
			AbstractDriver driver = (AbstractDriver) i.next();
			driverComboBox.addItem(driver);
			centrePanel.setPreferredSize(merge(centrePanel.getPreferredSize(),
			driver.getPanel().getPreferredSize()));
		}
		*/
		this.drivers = drivers;
    }

	public void show() {
		driverComboBox.removeAllItems();
		for (Iterator i = drivers.iterator(); i.hasNext();) {
			AbstractDriver driver = (AbstractDriver) i.next();
			driverComboBox.addItem(driver);
			centrePanel.setPreferredSize(merge(centrePanel.getPreferredSize(),
					driver.getPanel().getPreferredSize()));
		}
		pack();
		super.show();
	}
    private Dimension merge(Dimension envelopeA, Dimension envelopeB) {
        return new Dimension((int) Math.max(envelopeA.getWidth(),
                envelopeB.getWidth()),
            (int) Math.max(envelopeA.getHeight(), envelopeB.getHeight()));
    }

    void jbInit() throws Exception {
        centrePanel.setLayout(borderLayout1);
        this.getContentPane().setLayout(borderLayout2);
        innerNorthPanel.setLayout(flowLayout1);
        driverLabel.setText("Format:");
        driverComboBox.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    driverComboBox_itemStateChanged(e);
                }
            });
        northPanel.setLayout(gridBagLayout1);
        northPanel.setBorder(BorderFactory.createEtchedBorder());
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentShown(ComponentEvent e) {
                    this_componentShown(e);
                }

                public void componentHidden(ComponentEvent e) {
                    this_componentHidden(e);
                }
            });
        getContentPane().add(centrePanel, BorderLayout.CENTER);
        this.getContentPane().add(northPanel, BorderLayout.NORTH);
        northPanel.add(innerNorthPanel,
            new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        innerNorthPanel.add(driverLabel, null);
        innerNorthPanel.add(driverComboBox, null);
        centrePanel.add(currentDriverPanel, BorderLayout.CENTER);
    }

    public AbstractDriver getCurrentDriver() {
        return (AbstractDriver) driverComboBox.getSelectedItem();
    }

    public boolean wasOKPressed() {
        if (!closeInitiatedByPanelButton) {
            //The dialog's close button was pressed instead. [Jon Aquino]
            return false;
        }

        //Can't simply return currentDriverPanel.wasOKPressed() because the current
        //driver panel is reset in #this_componentHidden(Component)
        return okPressed;
    }

    void driverComboBox_itemStateChanged(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }

        updateCentrePanel(getCurrentDriver().getPanel());
    }

    private void updateCentrePanel(AbstractDriverPanel newDriverPanel) {
        currentDriverPanel.removeActionListener(this);
        centrePanel.remove(currentDriverPanel);
        currentDriverPanel = newDriverPanel;
        centrePanel.add(currentDriverPanel, BorderLayout.CENTER);
        currentDriverPanel.addActionListener(this);
        validateTree();
        repaint();
    }

    public void actionPerformed(ActionEvent e) {
        okPressed = currentDriverPanel.wasOKPressed();

        if (!currentDriverPanel.wasOKPressed() ||
                currentDriverPanel.isInputValid()) {
            closeInitiatedByPanelButton = true;
            setVisible(false);

            if (currentDriverPanel.wasOKPressed()) {
                updateDriverPanelCache();
            }

            return;
        }

        reportValidationError(currentDriverPanel.getValidationError());
    }

    private void reportValidationError(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage, "JUMP",
            JOptionPane.ERROR_MESSAGE);
    }

    void this_componentShown(ComponentEvent e) {
        closeInitiatedByPanelButton = false;

        AbstractDriver cachedDriver = (AbstractDriver) driverPanelCache(layer)
                                                           .get(DriverPanelCache.DRIVER_CACHE_KEY);

        if (cachedDriver != null) {
            driverComboBox.setSelectedItem(cachedDriver);
        }

        applyDriverPanelCache();
        updateCentrePanel(getCurrentDriver().getPanel());
    }

    void this_componentHidden(ComponentEvent e) {
        //Disconnect the currentDriverPanel, because other instances of DriverDialog
        //may use it [Jon Aquino]
        updateCentrePanel(dummyDriverPanel);
    }

    private void applyDriverPanelCache() {
        for (int i = 0; i < driverComboBox.getItemCount(); i++) {
            AbstractDriver driver = (AbstractDriver) driverComboBox.getItemAt(i);
            driver.getPanel().setCache(driverPanelCache(layer));
        }
    }

    private void updateDriverPanelCache() {
        driverPanelCache(layer).addAll(currentDriverPanel.getCache());
        driverPanelCache(layer).put(DriverPanelCache.DRIVER_CACHE_KEY,
            driverComboBox.getSelectedItem());
    }

    private DriverPanelCache driverPanelCache(Layer layer) {
        //e.g. open dialog will have no layer set. [Jon Aquino]
        if (layer == null) {
            return new DriverPanelCache();
        }

        DriverPanelCache cache = (DriverPanelCache) layerToDriverPanelCacheMap.get(layer);

        if (cache == null) {
            cache = new DriverPanelCache();
            cache.put(DriverPanelCache.DRIVER_CACHE_KEY, driverComboBox.getSelectedItem());
            layerToDriverPanelCacheMap.put(layer, cache);
        }

        return cache;
    }

    /**
     * This DriverDialog will attempt to retrieve the last values used for the
     * given Layer.
     * @see DriverPanelCache
     */
    public void setLayer(Layer layer) {
        this.layer = layer;
    }
}
