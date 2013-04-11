
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;


//JBuilder displays this component as a "Red Bean". There's a trick to
//displaying it -- see test.AbstractDriverPanelProxy and
//http://www.visi.com/~gyles19/fom-serve/cache/97.html. [Jon Aquino]
//<<TODO:REFACTORING>> This class duplicates code in BasicFileDriverPanel. I wonder
//how we can refactor them. [Jon Aquino]
public class GMLFileDriverPanel extends AbstractDriverPanel {
    private final static String TEMPLATE_FILE_CACHE_KEY = "TEMPLATE_FILE";
    BorderLayout borderLayout1 = new BorderLayout();
    OKCancelPanel okCancelPanel = new OKCancelPanel();
    JPanel centrePanel = new JPanel();
    JPanel innerCentrePanel = new JPanel();
    FileNamePanel templateFileNamePanel;
    JLabel whitespaceLabel = new JLabel();
    FileNamePanel gmlFileNamePanel;
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    private ArrayList possibleTemplateExtensions = new ArrayList();

    public GMLFileDriverPanel(ErrorHandler errorHandler) {
        templateFileNamePanel = new FileNamePanel(errorHandler);
        gmlFileNamePanel = new FileNamePanel(errorHandler);

        try {
            jbInit();
            gmlFileNamePanel.setFileMustExist(true);
            templateFileNamePanel.setFileMustExist(true);
            gmlFileNamePanel.addBrowseListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        findPossibleTemplateFile();
                    }
                });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setGMLFileMustExist(boolean gmlFileMustExist) {
        gmlFileNamePanel.setFileMustExist(gmlFileMustExist);
    }

    public void setTemplateFileDescription(String description) {
        templateFileNamePanel.setUpperDescription(description);
    }

    public void setCache(DriverPanelCache cache) {
        super.setCache(cache);

        if (cache.get(DriverPanelCache.FILE_CACHE_KEY) != null) {
            gmlFileNamePanel.setSelectedFile((File) cache.get(
            DriverPanelCache.FILE_CACHE_KEY));
        }

        if (cache.get(TEMPLATE_FILE_CACHE_KEY) != null) {
            templateFileNamePanel.setSelectedFile((File) cache.get(
                    TEMPLATE_FILE_CACHE_KEY));
        }
    }

    /**
     * Adds the extension to the list of extensions to use when searching for
     * a template file to use as the default
     * @param extension for example, ".jot"
     */
    public void addPossibleTemplateExtension(String extension) {
        possibleTemplateExtensions.add(extension);
    }

    public String getValidationError() {
        if (!gmlFileNamePanel.isInputValid()) {
            return gmlFileNamePanel.getValidationError();
        }

        if (!templateFileNamePanel.isInputValid()) {
            return templateFileNamePanel.getValidationError();
        }

        return null;
    }

    public File getGMLFile() {
        return gmlFileNamePanel.getSelectedFile();
    }

    public File getTemplateFile() {
        return templateFileNamePanel.getSelectedFile();
    }

    public DriverPanelCache getCache() {
        DriverPanelCache cache = super.getCache();
        cache.put(DriverPanelCache.FILE_CACHE_KEY, gmlFileNamePanel.getSelectedFile());
        cache.put(TEMPLATE_FILE_CACHE_KEY,
            templateFileNamePanel.getSelectedFile());

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
        templateFileNamePanel.setUpperDescription(
            "Template File Description Goes Here");
        whitespaceLabel.setText(" ");
        gmlFileNamePanel.setUpperDescription("GML File");
        gmlFileNamePanel.setFileFilter(new WorkbenchFileFilter(GUIUtil.gmlDesc));
        centrePanel.setLayout(gridBagLayout2);
        this.add(okCancelPanel, BorderLayout.SOUTH);
        this.add(centrePanel, BorderLayout.CENTER);
        centrePanel.add(innerCentrePanel,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 10, 10, 10), 0, 0));
        innerCentrePanel.add(gmlFileNamePanel,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        innerCentrePanel.add(templateFileNamePanel,
            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        innerCentrePanel.add(whitespaceLabel,
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    /**
     * Search for a template file using the extensions. If one is found, set
     * it as the value in the template edit box.
     */
    private void findPossibleTemplateFile() {
        String gmlFile = gmlFileNamePanel.getSelectedFile().toString();

        if (gmlFile.length() < "a.aaa".length()) {
            return;
        }

        for (Iterator i = possibleTemplateExtensions.iterator(); i.hasNext();) {
            String extension = (String) i.next();
            File templateFile = new File(gmlFile.substring(0,
                        gmlFile.length() - ".aaa".length()) + extension);

            if (templateFile.exists()) {
                templateFileNamePanel.setSelectedFile(templateFile);

                return;
            }
        }
    }
}
