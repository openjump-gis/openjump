
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

import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import com.vividsolutions.jump.I18N;


//JBuilder displays this component as a "Red Bean". There's a trick to
//displaying it -- see jcstest.AbstractDriverPanelProxy and
//http://www.visi.com/~gyles19/fom-serve/cache/97.html. [Jon Aquino]
//<<TODO:REFACTORING>> This class duplicates code in GMLFileDriverPanel. I wonder
//how we can refactor them. [Jon Aquino]

/**
 * Base for UI supporting File-based Driver.
 */

public class BasicFileDriverPanel extends AbstractDriverPanel {
    BorderLayout borderLayout1 = new BorderLayout();
    OKCancelPanel okCancelPanel = new OKCancelPanel();
    JPanel centrePanel = new JPanel();
    JPanel innerCentrePanel = new JPanel();
    protected FileNamePanel fileNamePanel;
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    GridBagLayout gridBagLayout2 = new GridBagLayout();

    public BasicFileDriverPanel(ErrorHandler errorHandler) {
        fileNamePanel = new FileNamePanel(errorHandler);

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        innerCentrePanel.setLayout(gridBagLayout1);
        fileNamePanel.setUpperDescription(I18N.get("ui.BasicFileDriverPanel.file-description-goes-here"));
        centrePanel.setLayout(gridBagLayout2);
        this.add(okCancelPanel, BorderLayout.SOUTH);
        this.add(centrePanel, BorderLayout.CENTER);
        centrePanel.add(innerCentrePanel,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 10, 10, 10), 0, 0));
        innerCentrePanel.add(fileNamePanel,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    public void setFileMustExist(boolean fileMustExist) {
        fileNamePanel.setFileMustExist(fileMustExist);
    }

    public String getValidationError() {
        if (!fileNamePanel.isInputValid()) {
            return fileNamePanel.getValidationError();
        }

        return null;
    }

    public File getSelectedFile() {
        return fileNamePanel.getSelectedFile();
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

    public void setFileDescription(String description) {
        fileNamePanel.setUpperDescription(description);
    }

    public void setCache(DriverPanelCache cache) {
        super.setCache(cache);
    
        if (cache.get(DriverPanelCache.FILE_CACHE_KEY) != null) {
            fileNamePanel.setSelectedFile((File) cache.get(DriverPanelCache.FILE_CACHE_KEY));
        }
    }

    public DriverPanelCache getCache() {
        DriverPanelCache cache = super.getCache();
        cache.put(DriverPanelCache.FILE_CACHE_KEY, fileNamePanel.getSelectedFile());

        return cache;
    }

    public void setFileFilter(FileFilter filter) {
        fileNamePanel.setFileFilter(filter);
    }
}
