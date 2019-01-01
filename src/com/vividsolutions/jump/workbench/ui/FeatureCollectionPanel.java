package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.openjump.core.apitools.IOTools;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.util.LayerableUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class FeatureCollectionPanel extends JPanel {

    /**
     * [Giuseppe Aruta 2017-14-12] A panel to show a collection of features To
     * create a panel with a table showing a FeatureCollection:
     * "FeatureCollectionPanel pan = new FeatureCollectionPanel(FeatureCollection)"
     * The style (Renderer) of the table reminds AttributePanel renderer
     */
    private static final long serialVersionUID = 1L;
    private final FeatureCollection featureCollection;

    private JScrollPane pane = new JScrollPane();
    private final JPanel filterPanel = new JPanel(new BorderLayout());
    private JTable jTable = new JTable();
    private final JTable subtable = new JTable();
    private final JLabel jLabel = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel();
    private final Color LIGHT_GRAY = new Color(230, 230, 230);
    private JPanel southPanel = new JPanel();
    private final String sSaved = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final String SCouldNotSave = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");

    public FeatureCollectionPanel(FeatureCollection featureCollection) {
        super();
        this.featureCollection = featureCollection;
        try {
            jbInit();
        } catch (final Exception e) {
            Logger.error(e);
        }
    }

    /**
     * gets the JTable associate to the panel
     * 
     * @return JTable
     */
    public JTable getTable() {
        return jTable;
    }

    private void jbInit() throws Exception {
        setLayout(new BorderLayout());
        jLabel.setText(I18N.get("ui.AttributeTablePanel.featurecollection")
                + " - (" + featureCollection.size() + " "
                + I18N.get("ui.AttributeTablePanel.features") + ")");
        jLabel.setFont(jLabel.getFont().deriveFont(Font.BOLD));
        setFeatureCollection(featureCollection);
        jTable = new JTable();
        jTable.setGridColor(Color.WHITE);
        jTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                final Component c = super.getTableCellRendererComponent(table,
                        value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? Color.white : LIGHT_GRAY);
                if (isSelected) {
                    c.setBackground(Color.black);
                }
                return c;
            };
        });
        pane = new JScrollPane(jTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jTable.setModel(setTableModelFromFeatureCollection());
        jTable.setEnabled(true);

        // JPanel upperPanel = new JPanel(new BorderLayout());
        southPanel = new JPanel(new BorderLayout());

        southPanel.add(commandPanel(), BorderLayout.NORTH);
        southPanel.add(savePanel(), BorderLayout.CENTER);
        add(jLabel, BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

    }

    /**
     * Save panel
     * 
     * @return
     */

    private JPanel savePanel() {
        final JPanel save = new JPanel();
        save.setLayout(new FlowLayout(FlowLayout.RIGHT));
        final JButton saveButton = new JButton(
                I18N.get("deejump.plugin.SaveLegendPlugIn.Save"));
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File file;
                    final FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
                            "Comma-Separated Values (csv)", "csv");
                    final FileNameExtensionFilter filter3 = new FileNameExtensionFilter(
                            "JUMP Markup Language (JML)", "jml");
                    final FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            "ESRI Shapefile (SHP)", "shp");
                    final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting();
                    if (!LayerableUtil.isMixedGeometryType(featureCollection)) {
                        fc.setFileFilter(filter);
                    }
                    fc.setFileFilter(filter3);
                    fc.setFileFilter(filter2);
                    fc.addChoosableFileFilter(filter2);
                    final int returnVal = fc.showSaveDialog(JUMPWorkbench
                            .getInstance().getFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        if (fc.getFileFilter().equals(filter3)) {
                            file = new File(fc.getSelectedFile() + ".jml");
                            IOTools.saveJMLFile(featureCollection,
                                    file.getAbsolutePath());
                            saved(file);

                        } else if (fc.getFileFilter().equals(filter)) {
                            file = new File(fc.getSelectedFile() + ".shp");
                            IOTools.saveShapefile(featureCollection,
                                    file.getAbsolutePath());
                            saved(file);

                        } else if (fc.getFileFilter().equals(filter2)) {

                            file = new File(fc.getSelectedFile() + ".csv");
                            IOTools.saveCSV(jTable, file.getAbsolutePath());
                            saved(file);
                        }
                    }

                } catch (final Exception ex) {
                    notsaved();
                }

            }
        });
        save.add(saveButton);
        return save;
    }

    // Experimental panel: here it can go some filters for further analysis (see
    // AttributeQueryPlugIn for example)
    // and a save button to export filtered results as layer
    // Right now it search only to match a string to every single record
    private JPanel commandPanel() {
        // Sorter
        final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(
                model);
        jTable.setRowSorter(sorter);
        // Filter Field
        final JTextField txtFilter = new JTextField();

        // Search Button
        final JButton btnOK = new JButton(IconLoader.icon("search.png"));
        btnOK.setToolTipText("Search");
        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                final String expr = txtFilter.getText();
                sorter.setRowFilter(RowFilter.regexFilter(expr));
                sorter.setSortKeys(null);
                subtable.setRowSorter(sorter);
            }
        });
        // Save Button, not yet implemented
        final JButton btSave = new JButton(IconLoader.icon("disk.png"));
        btSave.setToolTipText("Save search");
        btSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File file;
                    final FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
                            "Comma-Separated Values (csv)", "csv");

                    final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting();
                    fc.setFileFilter(filter2);
                    fc.setFileFilter(filter2);
                    fc.addChoosableFileFilter(filter2);
                    final int returnVal = fc.showSaveDialog(JUMPWorkbench
                            .getInstance().getFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {

                        file = new File(fc.getSelectedFile() + ".csv");
                        IOTools.saveCSV(subtable, file.getAbsolutePath());
                        saved(file);

                    }

                } catch (final Exception ex) {
                    notsaved();
                }

            }
        });
        // btnOK.setBounds(336, 144, 59, 23);
        final JPanel jbuttonpan = new JPanel();
        jbuttonpan.add(btnOK, BorderLayout.WEST);
        jbuttonpan.add(btSave, BorderLayout.EAST);
        filterPanel.add(txtFilter, BorderLayout.CENTER);
        filterPanel.add(jbuttonpan, BorderLayout.EAST);
        return filterPanel;
    }

    /**
     * Set a FeatureCollection to add to the panel
     * 
     * @param featurecollection
     * @return
     */

    public FeatureCollection setFeatureCollection(
            FeatureCollection featurecollection) {
        return featureCollection;
    }

    /**
     * Gets the FeatureCollection added to this panel. Useful if user want to
     * save it as a layer
     * 
     * @return FeatureCollection
     */
    public FeatureCollection getFeatureCollection() {
        return featureCollection;
    }

    private TableModel setTableModelFromFeatureCollection() {
        String[] fields;
        int iCount;
        iCount = featureCollection.getFeatures().size();
        final FeatureSchema schema = featureCollection.getFeatureSchema();
        final ArrayList<String> ar = new ArrayList<String>();
        String name;
        for (int j = 0; j < schema.getAttributeNames().size(); j++) {
            name = schema.getAttributeName(j).toString();
            ar.add(name);
        }
        fields = ar.toArray(new String[0]);
        final String[][] data = new String[iCount][fields.length];
        for (int i = 0; i < featureCollection.size(); i++) {
            final Feature feat = featureCollection.getFeatures().get(i);
            for (int j = 0; j < schema.getAttributeCount(); j++) {
                if (feat.getSchema().getAttributeType(j) != AttributeType.GEOMETRY) {
                    data[i][j] = feat.getAttribute(j).toString();
                } else {
                    String geomType = feat.getGeometry().getClass().getName();
                    final int dotPos = geomType.lastIndexOf(".");
                    if (dotPos > 0) {
                        geomType = geomType.substring(dotPos + 1);
                    }
                    data[i][j] = geomType.toUpperCase();
                }
            }
        }
        model.setDataVector(data, fields);
        return model;
    }

    /**
     * Gets the lower panel where locating table tools
     * 
     * @return
     */
    public JPanel getCommandPanel() {
        return commandPanel();
    }

    /**
     * Gets the lower panel where locating save button
     * 
     * @return
     */
    public JPanel getSavePanel() {
        return savePanel();
    }

    public JPanel getSouthPanel() {
        return southPanel;
    }

    protected void saved(File file) {
        JUMPWorkbench.getInstance().getFrame()
                .setStatusMessage(sSaved + " :" + file.getAbsolutePath());
    }

    protected void notsaved() {
        JUMPWorkbench.getInstance().getFrame().warnUser(SCouldNotSave);

    }

}
