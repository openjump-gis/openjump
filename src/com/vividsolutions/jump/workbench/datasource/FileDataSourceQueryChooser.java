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
package com.vividsolutions.jump.workbench.datasource;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

/**
 * UI for picking datasets stored in files. Generates two properties: the filename
 * and the CoordinateSystem.
 * @see com.vividsolutions.jump.coordsys.CoordinateSystem
 */
public abstract class FileDataSourceQueryChooser implements DataSourceQueryChooser {

    private String description;
    private Class dataSourceClass;
    private FileFilter fileFilter;
    private JPanel southComponent1 = new JPanel();
    private JPanel southComponent2 = new JPanel();
    private String[] extensions;

    /**
     * @param extensions e.g. txt
     */
    public FileDataSourceQueryChooser(Class dataSourceClass,
        String description, String[] extensions) {
        this.dataSourceClass = dataSourceClass;
        this.description = description;
        this.extensions = extensions;
        fileFilter = GUIUtil.createFileFilter(description, extensions);
    }

    public String toString() {
        return description;
    }

    public boolean isInputValid() {
//      // [sstein 6 Nov 2011] the previous does not work for MacOSX, but I found
//      // the code below here:
//      // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4528663
//      boolean gotFileName = true;
//      try {
//        JFileChooser chooser = getFileChooserPanel().getChooser();
//        Method getFileName = chooser.getUI().getClass()
//            .getDeclaredMethod("getFileName");
//        String fn = (String) getFileName.invoke(chooser.getUI());
//        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), fn));
//      } catch (Exception e) {
//        /* log warning */
//        gotFileName = false;
//      }
//      return gotFileName;
      // [2016.07 ede] the following should be more generic as it runs the 
      // filechoosers own routine to fill up the internal selected files vars
      // properly
      JFileChooser jfc = getFileChooserPanel().getChooser();
      FileChooserUI fcui = jfc.getUI();
      if (fcui instanceof BasicFileChooserUI) {
        BasicFileChooserUI bfcui = (BasicFileChooserUI) fcui;
        bfcui.getApproveSelectionAction().actionPerformed(
            new ActionEvent(new JButton(), 0, "nix"));
      }

      return jfc.getSelectedFile() instanceof File;
    }

    public Collection<DataSourceQuery> getDataSourceQueries() {
        List<DataSourceQuery> queries = new ArrayList<>();
        File[] files = getSelectedFiles();

        for (File file : files) {
            queries.addAll(toDataSourceQueries(file));
        }

        return queries;
    }
    
  /**
   * override in implementations to postprocess the selected filenames eg. add
   * extensions for saving or such
   * should be used in isInputValid(), getDataSourceQueries() ...
   * 
   * @return array of file objects
   */
    public abstract File[] getSelectedFiles();

    //Overridden by IGDSDataSourceQueryChooser [Jon Aquino]
    protected Collection<DataSourceQuery> toDataSourceQueries(File file) {
        return Collections.singleton(toDataSourceQuery(file));
    }

    protected abstract FileChooserPanel getFileChooserPanel();

    public Component getComponent() {
        setFileFilters();

        if (getFileChooserPanel().getSouthComponent1() != getSouthComponent1()) {
            getFileChooserPanel().setSouthComponent1(getSouthComponent1());
        }

        if (getFileChooserPanel().getSouthComponent2() != getSouthComponent2()) {
            getFileChooserPanel().setSouthComponent2(getSouthComponent2());
        }

        getFileChooserPanel().revalidate();
        getFileChooserPanel().repaint();

        return getFileChooserPanel();
    }

    //
    // It's confusing having two comboboxes having formats (the one at the
    // top and the one at the bottom). To help things a bit, simplify the lower one
    // to always display *.*.  [Jon Aquino]
    //
    // JJ - eziLink users (and myself) found themselves clicking on a shapefile,
    // only to be told that the file couldn't be read becuase the menu at the
    // top of the dialog was set to a different format. It was also difficult
    // to pick out the shapefiles, because there are so many other files associated
    // with each shapefile. So we've re-enabled the file filters.
    //
    private void setFileFilters() {
        //
        // Only set the file filter if we have to, because refreshing the view
        // takes time. [Jon Aquino]
        //
        FileFilter[] filters = getFileChooserPanel().getChooser().getChoosableFileFilters();
        if ( ! CollectionUtil.containsReference( filters, getFileFilter() ) ) {
            GUIUtil.removeChoosableFileFilters(getFileChooserPanel().getChooser());
            addFileFilters(getFileChooserPanel().getChooser());
            getFileChooserPanel().getChooser().setFileFilter(getFileFilter());
        }
    }

    protected void addFileFilters(JFileChooser chooser) {
        chooser.addChoosableFileFilter(getFileFilter());
        chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    }

    protected DataSourceQuery toDataSourceQuery(File file) {
        DataSource dataSource = (DataSource) LangUtil.newInstance(dataSourceClass);
        if (dataSource != null) {
            dataSource.setProperties(toProperties(file));
        }
        return new DataSourceQuery(dataSource, null,
            GUIUtil.nameWithoutExtension(file));
    }

    protected Map<String,Object> toProperties(File file) {
        HashMap<String,Object> properties = new HashMap<>();
        properties.put(DataSource.URI_KEY, file.toURI().toString());
        properties.put(DataSource.FILE_KEY, file.getPath());
        properties.put(DataSource.COORDINATE_SYSTEM_KEY,
            getFileChooserPanel().getSelectedCoordinateSystem().getName());

        return properties;
    }

    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser();
        JFrame f = new JFrame();
        f.getContentPane().add(chooser);
        f.pack();
        f.setVisible(true);
    }

    protected FileFilter getFileFilter() {
        return fileFilter;
    }

    protected Component getSouthComponent1() {
        return southComponent1;
    }

    protected Component getSouthComponent2() {
        return southComponent2;
    }

    protected static class FileChooserPanel extends JPanel {
        private JFileChooser chooser;
        private Component southComponent1;
        private Component southComponent2;
        private JComboBox coordinateSystemComboBox = new JComboBox();
        private JLabel coordinateSystemLabel = new JLabel(I18N.getInstance().get("datasource.FileDataSourceQueryChooser.coordinate-system-of-file")+" ") {

                {
                    setDisplayedMnemonic('r');
                    setLabelFor(coordinateSystemComboBox);
                }
            };

        private JPanel southComponent1Container = new JPanel(new BorderLayout());
        private JPanel southComponent2Container = new JPanel(new BorderLayout());

        public FileChooserPanel(JFileChooser chooser, Blackboard blackboard) {
            setLayout(new BorderLayout());
            List sortedSystems = new ArrayList<>(CoordinateSystemRegistry.instance(blackboard).getCoordinateSystems() );
            Collections.sort( sortedSystems );
            coordinateSystemComboBox.setModel(new DefaultComboBoxModel( new Vector(sortedSystems) ) );
            this.chooser = chooser;

            JPanel southPanel = new JPanel(new GridBagLayout());
            southPanel.add(coordinateSystemLabel,
                new GridBagConstraints(0, 0, 1, 1, 0, 0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            southPanel.add(coordinateSystemComboBox,
                new GridBagConstraints(1, 0, 1, 1, 0, 0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            southPanel.add(southComponent1Container,
                new GridBagConstraints(2, 0, 1, 1, 1, 0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 4, 0, 0), 0, 0));
            southPanel.add(southComponent2Container,
                new GridBagConstraints(0, 1, 3, 1, 1, 0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            add(chooser, BorderLayout.CENTER);
            add(southPanel, BorderLayout.SOUTH);
            coordinateSystemComboBox.setVisible(false);
            coordinateSystemLabel.setVisible(false);
            setSouthComponent1(new JPanel());
            setSouthComponent2(new JPanel());
        }

        private void setSouthComponent1(Component southComponent1) {
            southComponent1Container.removeAll();
            this.southComponent1 = southComponent1;
            southComponent1Container.add(southComponent1, BorderLayout.CENTER);
        }

        private void setSouthComponent2(Component southComponent2) {
            southComponent2Container.removeAll();
            this.southComponent2 = southComponent2;
            southComponent2Container.add(southComponent2, BorderLayout.CENTER);
        }

        public JFileChooser getChooser() {
            return chooser;
        }

        private Component getSouthComponent1() {
            return southComponent1;
        }

        private Component getSouthComponent2() {
            return southComponent2;
        }

        public void setCoordinateSystemComboBoxVisible(boolean visible) {
            coordinateSystemComboBox.setVisible(visible);
            coordinateSystemLabel.setVisible(visible);
        }

        public CoordinateSystem getSelectedCoordinateSystem() {
            return coordinateSystemComboBox.isVisible()
            ? (CoordinateSystem) coordinateSystemComboBox.getSelectedItem()
            : CoordinateSystem.UNSPECIFIED;
        }

        public void setSelectedCoordinateSystem(String name) {
            coordinateSystemComboBox.setSelectedItem(coordinateSystem(name));
        }

        private CoordinateSystem coordinateSystem(String name) {
            for (int i = 0; i < coordinateSystemComboBox.getItemCount(); i++) {
                if (((CoordinateSystem)coordinateSystemComboBox.getItemAt(i)).getName().equals(name)) {
                    return (CoordinateSystem)coordinateSystemComboBox.getItemAt(i);
                }
            }
            return null;
        }
    }

    public String[] getExtensions() {
        return extensions;
    }

    public String getDescription() {
        return description;
    }

    public Class getDataSourceClass() {
        return dataSourceClass;
    }
    
}
