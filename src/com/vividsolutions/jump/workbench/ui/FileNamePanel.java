
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;

public class FileNamePanel extends JPanel {
    /**
     * @deprecated
     */
    public void setDescription(String description) {
        upperDescriptionLabel.setText(description);
        if (description.equals(GUIUtil.fmeDesc)) {
            setFileFilter(new WorkbenchFileFilter(GUIUtil.fmeDesc));
        } else if (description.equals(GUIUtil.gmlDesc)) {
            setFileFilter(new WorkbenchFileFilter(GUIUtil.gmlDesc));
        } else if (description.equals(GUIUtil.jmlDesc)) {
            setFileFilter(new WorkbenchFileFilter(GUIUtil.jmlDesc));
        } else if (description.equals(GUIUtil.shpDesc)) {
            setFileFilter(new WorkbenchFileFilter(GUIUtil.shpDesc));
        } else if (description.equals(GUIUtil.wktDesc)) {
            setFileFilter(new WorkbenchFileFilter(GUIUtil.wktDesc));
        } else if (description.equals(GUIUtil.xmlDesc)) {
            setFileFilter(new WorkbenchFileFilter(GUIUtil.xmlDesc));
        }
    }

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel upperDescriptionLabel = new JLabel();
    JComboBox comboBox = new JComboBox();
    JButton browseButton = new JButton();
    private ErrorHandler errorHandler;
    private DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    private boolean fileMustExist;
    private ArrayList browseListeners = new ArrayList();

    //Specify a maximum size because eventually we may want to persist the
    //cache in the JCS Workbench properties file [Jon Aquino]
    private int MAX_CACHE_SIZE = 10;

    public FileNamePanel(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        GUIUtil.fixEditableComboBox(comboBox);
    }

    public void setFileMustExist(boolean fileMustExist) {
        this.fileMustExist = fileMustExist;
    }

    private JLabel leftDescriptionLabel = new JLabel("");

    void jbInit() throws Exception {
        upperDescriptionLabel.setText(I18N.get("ui.FileNamePanel.description-text-goes-here"));
        this.setLayout(gridBagLayout1);
        browseButton.setText(I18N.get("ui.FileNamePanel.browse"));
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browseButton_actionPerformed(e);
            }
        });
        comboBox.setPreferredSize(new Dimension(300, 21));
        comboBox.setEditable(true);
        comboBox.setModel(comboBoxModel);
        this.add(
            upperDescriptionLabel,
            new GridBagConstraints(
                0,
                0,
                3,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
            leftDescriptionLabel,
            new GridBagConstraints(
                0,
                1,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 8),
                0,
                0));
        this.add(
            comboBox,
            new GridBagConstraints(
                1,
                1,
                1,
                1,
                1,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 8),
                0,
                0));
        this.add(
            browseButton,
            new GridBagConstraints(
                2,
                1,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
    }

    /**
     * Side effect: adds file to the combobox list of recently selected files.
     */
    public File getSelectedFile() {
        Assert.isTrue(isInputValid(), getValidationError());

        //Store file in local variable, because we will lose the combobox text
        //when we call #addToComboBox. [Jon Aquino]
        File file = new File(getComboBoxText());
        addToComboBox(file, comboBoxModel);

        return file;
    }

    public boolean isInputValid() {
        return null == getValidationError();
    }

    private String getComboBoxText() {
        return (String) comboBox.getEditor().getItem();
    }

    /**
     * @param file the initial value for the GML file field, or null to clear the field
     */
    public void setSelectedFile(File file) {
        if (file == null) {
            comboBox.getEditor().setItem("");

            return;
        }

        comboBox.getEditor().setItem(file.getAbsolutePath());
    }

    public String getValidationError() {
        if (getComboBoxText().trim().equals("")) {
            return I18N.get("ui.FileNamePanel.no-file-was-specified");
        }

        File file = new File(getComboBoxText());

        if (fileMustExist && !file.exists()) {
            return I18N.get("ui.FileNamePanel.specified-file-does-not-exist") +" "+ getComboBoxText();
        }

        if (fileMustExist && file.isDirectory()) {
            return I18N.get("ui.FileNamePanel.specified-file-is-a-directory")+" "+  getComboBoxText();
        }

        if (fileMustExist && !file.isFile()) {
            return I18N.get("ui.FileNamePanel.specified-file-is-not-normal") +" "+ getComboBoxText();
        }

        if (!fileMustExist && (file.getParentFile() == null)) {
            return I18N.get("ui.FileNamePanel.specified-parent-directory-is-not-specified")+" "
                + getComboBoxText();
        }

        if (!fileMustExist && !file.getParentFile().exists()) {
            return I18N.get("ui.FileNamePanel.specified-parent-directory-does-not-exist")+" " + getComboBoxText();
        }

        if (!fileMustExist && !file.getParentFile().isDirectory()) {
            return I18N.get("ui.FileNamePanel.specified-parent-is-not-a-directory")+" "
                + getComboBoxText();
        }

        return null;
    }

    void browseButton_actionPerformed(ActionEvent e) {
        try {
            File file = browse();

            if (file == null) {
                return;
            }

            comboBox.getEditor().setItem(file.getAbsolutePath());
            fireBrowseEvent(e);
        } catch (Throwable t) {
            errorHandler.handleThrowable(t);
        }
    }

    private void fireBrowseEvent(ActionEvent e) {
        for (Iterator i = browseListeners.iterator(); i.hasNext();) {
            ActionListener l = (ActionListener) i.next();
            l.actionPerformed(e);
        }
    }

    /**
     * Notify the ActionListener whenever the user picks a file using the
     * Browse button
     */
    public void addBrowseListener(ActionListener l) {
        browseListeners.add(l);
    }

    private FileFilter fileFilter = null;

    //<<TODO:FIX>> Sometimes after the Browse button is pressed, the file dialog
    //takes a minute or so to open. [Jon Aquino]
    private File browse() {
        JFileChooser fileChooser = fileMustExist ? GUIUtil.createJFileChooserWithExistenceChecking() : new JFCWithEnterAction();
        fileChooser.setDialogTitle(I18N.get("ui.FileNamePanel.browse"));

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        GUIUtil.removeChoosableFileFilters(fileChooser);
        fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());        
        if (fileFilter != null) {
            fileChooser.addChoosableFileFilter(fileFilter);
            fileChooser.setFileFilter(fileFilter);
        }

        File initialFile = getInitialFile();

        if (initialFile.exists() && initialFile.isFile()) {
            fileChooser.setSelectedFile(initialFile);
        } else if (initialFile.exists() && initialFile.isDirectory()) {
            fileChooser.setCurrentDirectory(initialFile);
        } else if (initialFile.getParentFile() != null && initialFile.getParentFile().exists()) {
            fileChooser.setCurrentDirectory(initialFile.getParentFile());
        }

        fileChooser.setMultiSelectionEnabled(false);

        if (JFileChooser.APPROVE_OPTION
            != fileChooser.showOpenDialog(SwingUtilities.windowForComponent(this))) {
            return null;
        }

        //We used to have code here to check if the file exists, but I removed it
        //because we'll save that for #getValidationError. At this point, we just
        //want to get some path information into the text area to save the user
        //some keystrokes. [Jon Aquino]

        return fileChooser.getSelectedFile();
    }

    protected File getInitialFile() {
        return new File(getComboBoxText());
    }

    private void addToComboBox(File file, DefaultComboBoxModel comboBoxModel) {
        //First do a removeElement so that if the file is already in the list it
        //gets moved to the top [Jon Aquino]
        comboBoxModel.removeElement(file.getAbsolutePath());
        comboBoxModel.insertElementAt(file.getAbsolutePath(), 0);

        if (comboBoxModel.getSize() > MAX_CACHE_SIZE) {
            comboBoxModel.removeElementAt(comboBoxModel.getSize() - 1);
        }

        comboBox.setSelectedIndex(0);
    }

    public void setUpperDescription(String description) {
        upperDescriptionLabel.setText(description);
    }

    public void setLeftDescription(String description) {
        leftDescriptionLabel.setText(description);
    }

    public void setFileFilter(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public List<File> getFiles() {
      List<File> files = new ArrayList<File>();
      for (int i = 0; i <comboBoxModel.getSize(); i++) {
        String path = (String)comboBox.getItemAt(i);
        files.add(new File(path));
      }
      return files;
    }

    public void setFiles(List<File> files) {
      
      List<File> reverseFiles = new ArrayList<File>(files);
      Collections.reverse(reverseFiles);
      for (File file : reverseFiles) {
        addToComboBox(file, comboBoxModel);
      }
    }

}
