package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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
import javax.swing.table.TableRowSorter;

import org.openjump.core.apitools.IOTools;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class JTablePanel extends JPanel {

    private final String sSaved = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final String SCouldNotSave = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");
    /**
     * [Giuseppe Aruta 2018-07-06] A generic panel to show JTables. With an
     * option to save table to file ( to .csv) and a search panel. The style
     * (Renderer) of the panel reminds AttributePanel renderer
     */

    private static final long serialVersionUID = 1L;

    private JScrollPane pane = new JScrollPane();
    private final JPanel filterPanel = new JPanel(new BorderLayout());
    private JTable jTable = new JTable();
    private final JLabel jLabel = new JLabel();
    private DefaultTableModel model = new DefaultTableModel();
    private final Color LIGHT_GRAY = new Color(230, 230, 230);

    /**
     * how to use: 1) DefaultTableModel defaultTableModel; 2)
     * defaultTableModel.addColumn(...); 3) JTablePanel pan = new
     * JTablePanel(defaultTableModel).
     * 
     * @param defaultTableModel
     */
    public JTablePanel(DefaultTableModel defaultTableModel) {
        super();
        model = defaultTableModel;
        try {
            jbInit();
        } catch (final Exception e) {
            Logger.error(e);
        }
    }

    public JTable getTable() {
        return jTable;
    }

    private void jbInit() throws Exception {
        setLayout(new BorderLayout());
        jLabel.setText(I18N
                .get("org.openjump.core.ui.plugin.raster.DEMStatisticsPlugIn.rows")
                + ": "
                + model.getRowCount()
                + " - "
                + I18N.get("org.openjump.core.ui.plugin.raster.DEMStatisticsPlugIn.columns")
                + ": " + model.getColumnCount());
        jLabel.setFont(jLabel.getFont().deriveFont(Font.BOLD));
        setTableModel(model);
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
        jTable.setModel(model);
        jTable.setEnabled(true);

        add(jLabel, BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);
        add(southPanel(), BorderLayout.SOUTH);// Not yet activated
        add(savePanel(), BorderLayout.AFTER_LAST_LINE);
    }

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

                    final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting();
                    fc.setFileFilter(filter2);
                    fc.setFileFilter(filter2);
                    fc.addChoosableFileFilter(filter2);
                    final int returnVal = fc.showSaveDialog(JUMPWorkbench
                            .getInstance().getFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {

                        file = new File(fc.getSelectedFile() + ".csv");
                        IOTools.saveCSV(jTable, file.getAbsolutePath());
                        saved(file);

                    }

                } catch (final Exception ex) {
                    notsaved();
                }

            }
        });
        save.add(saveButton);
        return save;
    }

    private final JTable subtable = new JTable();

    // Experimental panel: here it can go some filters for further analysis (see
    // AttributeQueryPlugIn for example)
    // and a save button to export filtered results as layer
    // Right now it search only to match a string to every single record
    private JPanel southPanel() {
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

    public DefaultTableModel setTableModel(DefaultTableModel table) {
        return model;
    }

    /**
     * Gets the FeatureCollection added to this panel. Useful if user want to
     * save it as a layer
     * 
     * @return FeatureCollection
     */
    public DefaultTableModel getTableModel() {
        return model;
    }

    /**
     * Gets the lower panel where locating table tools
     * 
     * @return
     */
    public JPanel getCommandPanel() {
        return southPanel();
    }

    /**
     * Gets the lower panel where locating save button
     * 
     * @return
     */
    public JPanel getSavePanel() {
        return savePanel();
    }

    protected void saved(File file) {
        JUMPWorkbench.getInstance().getFrame()
                .setStatusMessage(sSaved + " :" + file.getAbsolutePath());
    }

    protected void notsaved() {
        JUMPWorkbench.getInstance().getFrame().warnUser(SCouldNotSave);

    }

}
