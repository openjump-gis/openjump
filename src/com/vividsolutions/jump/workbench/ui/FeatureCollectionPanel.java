package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
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
    private final JLabel jLabel = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel();
    private final Color LIGHT_GRAY = new Color(230, 230, 230);

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

        add(jLabel, BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);
        add(southPanel(), BorderLayout.SOUTH);
    }

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
            }
        });
        // Save Button, not yet implemented
        final JButton btSave = new JButton(IconLoader.icon("disk.png"));
        btSave.setToolTipText("Save search");
        btSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                JOptionPane.showMessageDialog(null, "not yet implemented", "",
                        JOptionPane.WARNING_MESSAGE);
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

}
