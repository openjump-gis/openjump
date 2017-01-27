
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;


public class JMLFileDriverPanel extends AbstractDriverPanel {
    //<<TODO:REFACTOR>> Eliminate this class; its code is very similar to
    //BasicFileDriverPanel. [Jon Aquino]
    //<<TODO>> This variable isn't used. Eliminate it. [Jon Aquino]
    private final static String TEMPLATE_FILE_CACHE_KEY = "TEMPLATE_FILE";
    BorderLayout borderLayout1 = new BorderLayout();
    OKCancelPanel okCancelPanel = new OKCancelPanel();
    JPanel centrePanel = new JPanel();
    JPanel innerCentrePanel = new JPanel();
    FileNamePanel templateFileNamePanel;
    FileNamePanel jmlFileNamePanel;
    JLabel whitespaceLabel = new JLabel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    GridBagLayout gridBagLayout2 = new GridBagLayout();

    public JMLFileDriverPanel(ErrorHandler errorHandler) {
        templateFileNamePanel = new FileNamePanel(errorHandler);
        jmlFileNamePanel = new FileNamePanel(errorHandler);

        try {
            jbInit();
            jmlFileNamePanel.setFileMustExist(true);

            /*jmlFileNamePanel.addBrowseListener(
              new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                  setTemplateFile();
                }
              });*/
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setGMLFileMustExist(boolean gmlFileMustExist) {
        jmlFileNamePanel.setFileMustExist(gmlFileMustExist);
    }

    public void setCache(DriverPanelCache cache) {
        super.setCache(cache);

        if (cache.get(DriverPanelCache.FILE_CACHE_KEY) != null) {
            jmlFileNamePanel.setSelectedFile((File) cache.get(
                    DriverPanelCache.FILE_CACHE_KEY));
        }

        if (cache.get(TEMPLATE_FILE_CACHE_KEY) != null) {
            templateFileNamePanel.setSelectedFile((File) cache.get(
                    TEMPLATE_FILE_CACHE_KEY));
        }
    }

    public String getValidationError() {
        if (!jmlFileNamePanel.isInputValid()) {
            return jmlFileNamePanel.getValidationError();
        }

        return null;
    }

    public File getJMLFile() {
        return jmlFileNamePanel.getSelectedFile();
    }

    public File getTemplateFile() {
        return templateFileNamePanel.getSelectedFile();
    }

    public DriverPanelCache getCache() {
        DriverPanelCache cache = super.getCache();
        cache.put(DriverPanelCache.FILE_CACHE_KEY, jmlFileNamePanel.getSelectedFile());

        //cache.put(TEMPLATE_FILE_CACHE_KEY, templateFileNamePanel.getSelectedFile());
        return cache;
    }

    public void addActionListener(ActionListener l) {
        okCancelPanel.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        okCancelPanel.removeActionListener(l);
    }

    public boolean wasOKPressed() {
        return okCancelPanel.wasOKPressed();
    }

    void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        innerCentrePanel.setLayout(gridBagLayout1);
        whitespaceLabel.setText(" ");
        jmlFileNamePanel.setUpperDescription("JML File");
        centrePanel.setLayout(gridBagLayout2);
        this.add(okCancelPanel, BorderLayout.SOUTH);
        this.add(centrePanel, BorderLayout.CENTER);
        centrePanel.add(innerCentrePanel,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 10, 10, 10), 0, 0));
        innerCentrePanel.add(jmlFileNamePanel,
            new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        innerCentrePanel.add(whitespaceLabel,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
    }
}
