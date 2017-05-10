package org.openjump.core.ui.plugin.task;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.xml.namespace.QName;

import org.openjump.core.ccordsys.utils.SRSInfo;
import org.openjump.core.ccordsys.utils.SridLookupTable;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.MultiTabInputDialog;
import com.vividsolutions.jump.workbench.ui.SuggestTreeComboBox;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class TaskPropertiesPlugIn extends AbstractPlugIn {

  //Giuseppe Aruta [2017-5-10] plugin to read and modify project properties
    @Override
    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);
        FeatureInstaller featureInstaller = context.getFeatureInstaller();
        featureInstaller.addMainMenuPlugin(this,
                new String[] { MenuNames.EDIT });
    }

    public static String NAME = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.name");
    public static String PROJ_METADATA = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.proj-metadata");
    public static String PROJ_STATUS = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.proj-status");
    public static String NOT_SAVED = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.not-saved");
    public static String NOT_SET = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.not-set");
    public static String LAST_MODIFICATION = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.last-modification");
    public static String PROJ_DESCRIPTION = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.srs-description");
    public static String SEARCH_SRID = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.search-srid");
    public static String NUMBER_LAYERS = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.number-of-layers");
    public static String EDIT_METADATA = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.edit-metadata");
    public static String TOOLTIP = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.tooltip");
    public static String MODIFIED_LAYERS = "(*)"
            + I18N.get("ui.GenericNames.MODIFIED-LAYERS");
    public static String TEMPORARY_LAYERS = "(**)"
            + I18N.get("ui.GenericNames.TEMPORARY-LAYERS");

    private boolean editInfo = false;
    public static String FILE = I18N.get("ui.MenuNames.FILE");
    public static String INFO = I18N.get("ui.AboutDialog.info");

    public static String XMIN = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmin");
    public static String YMIN = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymin");
    public static String XMAX = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmax");
    public static String YMAX = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymax");
    public static String EXTENT = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.extent");
    public static String SOURCE_PATH = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Source-Path");
    public static String DATASOURCE_CLASS = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.DataSource-Class");
    private static String LAYER_NAME = I18N
            .get("jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel.Name");
    public static String SRS = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.SRS");

    JLabel fileT = new JLabel(FILE);
    JLabel sridLabel = new JLabel(SEARCH_SRID);
    JLabel dateT = new JLabel(LAST_MODIFICATION);
    JLabel metadataT = new JLabel(INFO);
    JLabel descriptionT = new JLabel(PROJ_DESCRIPTION);
    JLabel extensionT = new JLabel(EXTENT);
    JLabel layersT = new JLabel(NUMBER_LAYERS);

    private JTextArea projArea, fileArea, infoArea;

    private JTextField textFieldXMin, textFieldYMin, textFieldXMax,
            textFieldYMax, textFielddate, textFieldnumLyr;

    private MultiTabInputDialog dialog;
    final Map<String, String> codes = new LinkedHashMap<String, String>(64);
    public static SuggestTreeComboBox localSuggestTreeComboBox;

    private LayersPanel layersPanel;

    String srs = "";
    String srsCode = "";
    String info = "";
    String srsDescription = NOT_SET;

    private String xmax = "";
    private String xmin = "";
    private String ymax = "";
    private String ymin = "";

    @Override
    public String getName() {
        return NAME;
    }

    public Icon getIcon() {
        return IconLoader.icon("information_16x16.png");
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck().add(checkFactory
                .createWindowWithLayerNamePanelMustBeActiveCheck());
    }

    private final String bgColor1 = "\"#EAEAEA\"";
    private final String bgColor3 = "\"#FBFFE1\"";
    private final Font currentFont = new JLabel().getFont();
    private final String fontName = currentFont.getFontName();

    @Override
    public boolean execute(final PlugInContext context) throws Exception {
        final Task selectedTask = context.getTask();
        this.dialog = new MultiTabInputDialog(context.getWorkbenchFrame(),
                getName(), PROJ_METADATA, true);
        dialog.setSize(new Dimension(400, 270));
        dialog.setMinimumSize(new Dimension(400, 270));
        dialog.setCancelVisible(false);
        dialog.setResizable(true);
        this.codes.clear();
        this.codes.putAll(Utils.mapSRIDS());
        localSuggestTreeComboBox = new SuggestTreeComboBox(this.codes.keySet()
                .toArray(new String[this.codes.size()]), 40);
        if (selectedTask.getProperties().containsKey(
                new QName(Task.PROJECT_SRS_KEY))) {
            this.srsCode = selectedTask.getProperty(
                    new QName(Task.PROJECT_SRS_KEY)).toString();
        } else {
            this.srsCode = "0";
        }
        UIManager.put("ComboBox.disabledForeground", Color.black);
        localSuggestTreeComboBox.setSelectedItem(this.srsCode);
        localSuggestTreeComboBox.setPreferredSize(new Dimension(150, 20));
        localSuggestTreeComboBox.setEditable(false);
        localSuggestTreeComboBox.setEnabled(false);
        localSuggestTreeComboBox.setBackground(dialog.getBackground());
        Utils.removeButton(localSuggestTreeComboBox);
        localSuggestTreeComboBox
                .setPrototypeDisplayValue("abcdefghijklmnpqrstuvwxyz/0123456789");

        String file;
        try {
            file = selectedTask.getProjectFile().getAbsolutePath();
        } catch (Exception ex) {
            file = NOT_SAVED;
        }
        SRSInfo srid = SridLookupTable
                .getSrsAndUnitFromCode(localSuggestTreeComboBox
                        .getSelectedItem().toString());
        srid.complete();
        srsDescription = srid.toString();

        this.dialog.addRow("source", fileT, filePanel(file),
                new EnableCheck[0], "");
        textFielddate = new JTextField();
        textFielddate.setToolTipText("");
        textFielddate.setMinimumSize(new Dimension(50, 20));
        textFielddate.setPreferredSize(new Dimension(150, 20));
        textFielddate.setText(dateString(selectedTask));
        textFielddate.setEditable(false);

        this.dialog.addRow("source", dateT, textFielddate, new EnableCheck[0],
                "");

        String numLyr = "" + Utils.getNamesOfLayerableList(context).size();

        textFieldnumLyr = new JTextField(numLyr);
        textFieldnumLyr.setToolTipText("");
        textFieldnumLyr.setPreferredSize(new Dimension(150, 20));
        textFieldnumLyr.setEditable(false);
        this.dialog.addRow("source", layersT, textFieldnumLyr,
                new EnableCheck[0], "");
        this.dialog.addRow("source", extensionPanel(context),
                new EnableCheck[0], "");
        this.dialog.addRow("source", descriptionT,
                projectionPanel(srsDescription), new EnableCheck[0], "");
        this.dialog.addRow("source", sridLabel, localSuggestTreeComboBox,
                new EnableCheck[0], TOOLTIP);
        this.dialog.addRow("source", infoPanel(context), new EnableCheck[0],
                TOOLTIP);
        this.dialog.addCheckBox(EDIT_METADATA, this.editInfo, TOOLTIP);
        dialog.getCheckBox(EDIT_METADATA).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        updateControls(context, dialog);
                        dialog.repaint();
                    }
                });
        layersPanel = new LayersPanel(context);
        layersPanel.setPreferredSize(new Dimension(400, 400));
        dialog.addPane(PROJ_STATUS);
        dialog.addRow(layersPanel);
        dialog.pack();

        // commands OK and Apply perform changes to project properties
        // TODO: add a button to restore properties from project file
        dialog.addOKCancelApplyPanelActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyModifications(context, dialog);
            }
        });
        GUIUtil.centreOnWindow(this.dialog);
        this.dialog.setVisible(true);
        return true;
    }

    // Updates dialog if "Edit metadata" checkbox is ebnabled
    protected void updateControls(PlugInContext context,
            final MultiInputDialog dialog) {
        if (dialog.getCheckBox(EDIT_METADATA).isSelected()) {
            dialog.setApplyVisible(true);
            localSuggestTreeComboBox.setEnabled(true);
            localSuggestTreeComboBox.setEditable(dialog.getCheckBox(
                    EDIT_METADATA).isSelected());
            localSuggestTreeComboBox.setBackground(Color.WHITE);
            infoArea.setEditable(dialog.getCheckBox(EDIT_METADATA).isSelected());
            infoArea.setBackground(Color.WHITE);
            infoArea.repaint();
            dialog.repaint();
        } else {
            dialog.setApplyVisible(false);
            UIManager.put("ComboBox.disabledForeground", Color.black);
            localSuggestTreeComboBox.setSelectedItem(this.srsCode);
            localSuggestTreeComboBox.setEditable(false);
            localSuggestTreeComboBox.setEnabled(false);
            localSuggestTreeComboBox.setBackground(dialog.getBackground());
            infoArea.setBackground(dialog.getBackground());
            infoArea.repaint();
            localSuggestTreeComboBox.repaint();
            dialog.repaint();
        }
    }

    protected void applyModifications(PlugInContext context,
            final MultiInputDialog dialog) {
        if (dialog.wasApplyPressed()) {
            try {
                SRSInfo sridTableInfo = SridLookupTable
                        .getSrsAndUnitFromCode(localSuggestTreeComboBox
                                .getSelectedItem().toString());
                sridTableInfo.complete();
                srsDescription = sridTableInfo.toString();
                projArea.setText(srsDescription);
                String epsg = localSuggestTreeComboBox.getSelectedItem()
                        .toString();
                String unit = sridTableInfo.getUnit().toString();
                Task selectedeTask = context.getTask();
                selectedeTask
                        .setProperty(new QName(Task.PROJECT_SRS_KEY), epsg);
                selectedeTask.setProperty(new QName(Task.PROJECT_UNIT), unit);
                selectedeTask.setProperty(new QName(Task.PROJECT_METAINFO_KEY),
                        infoArea.getText());
                // Utils.SaveProject(context);
                dialog.pack();
                dialog.repaint();
            } catch (Exception e1) {
                dialog.repaint();
                e1.printStackTrace();
            }
        }
    }

    // Return last modification time of a project file
    private String dateString(Task selectedTask) {
        String time;
        try {
            File f = new File(selectedTask.getProjectFile().getAbsolutePath());
            if (selectedTask.getProperties().containsKey(
                    new QName(Task.PROJECT_TIME_KEY))) {
                time = selectedTask.getProperty(
                        new QName(Task.PROJECT_TIME_KEY)).toString();
            } else {
                Path path = f.toPath();
                BasicFileAttributes attr = null;
                try {
                    attr = Files
                            .readAttributes(path, BasicFileAttributes.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DateFormat dateFormat = new SimpleDateFormat(
                        "yyyy/MM/dd HH:mm:ss");
                dateFormat.format(attr.creationTime().toMillis());
                time = dateFormat.format(attr.creationTime().toMillis());
            }
        } catch (Exception ex) {
            time = "---";
        }
        return time;
    }

    // Info panel
    private JPanel infoPanel(PlugInContext context) {
        infoArea = new JTextArea();
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(EXTENT));
        Task selectedeTask = context.getTask();
        if (selectedeTask.getProperties().containsKey(
                new QName(Task.PROJECT_METAINFO_KEY))) {
            this.info = selectedeTask.getProperty(
                    new QName(Task.PROJECT_METAINFO_KEY)).toString();
        } else {
            this.info = "";
        }
        infoArea.setBackground(dialog.getBackground());
        infoArea.setText(this.info);
        infoArea.setCaretPosition(0);
        infoArea.setFont(new JLabel().getFont());
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setColumns(45);
        infoArea.setRows(7);
        infoArea.setPreferredSize(infoPanel.getSize());
        JScrollPane layerPane = new JScrollPane(infoArea, 20, 31);
        infoPanel.setBorder(BorderFactory.createTitledBorder(INFO));
        infoPanel.add(layerPane);
        return infoPanel;
    }

    // Layer envelope extension panel Modified
    // from Kosmo 3.0 <author: Sergio Banos Calvo>

    private JPanel extensionPanel(PlugInContext context) {
        Task selectedeTask = context.getTask();
        Envelope env = selectedeTask.getLayerManager().getEnvelopeOfAllLayers();
        JPanel extensionPanel = new JPanel(new GridBagLayout());
        extensionPanel.setBorder(BorderFactory.createTitledBorder(EXTENT));
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        DecimalFormat formatter = (DecimalFormat) nf;
        formatter.applyPattern("##0.##");
        if (env != null) {
            xmax = formatter.format(env.getMaxX());
            xmin = formatter.format(env.getMinX());
            ymax = formatter.format(env.getMaxY());
            ymin = formatter.format(env.getMinY());
        }
        textFieldXMin = new JTextField();
        textFieldXMin.setMinimumSize(new Dimension(75, 20));
        textFieldXMin.setPreferredSize(new Dimension(90, 20));
        textFieldXMin.setText(xmin);
        textFieldXMin.setEditable(false);

        textFieldYMin = new JTextField();
        textFieldYMin.setMinimumSize(new Dimension(75, 20));
        textFieldYMin.setPreferredSize(new Dimension(90, 20));
        textFieldYMin.setText(ymin);
        textFieldYMin.setEditable(false);

        textFieldXMax = new JTextField();
        textFieldXMax.setMinimumSize(new Dimension(75, 20));
        textFieldXMax.setPreferredSize(new Dimension(90, 20));
        textFieldXMax.setText(xmax);
        textFieldXMax.setEditable(false);

        textFieldYMax = new JTextField();
        textFieldYMax.setMinimumSize(new Dimension(75, 20));
        textFieldYMax.setPreferredSize(new Dimension(90, 20));
        textFieldYMax.setText(ymax);
        textFieldYMax.setEditable(false);

        FormUtils.addRowInGBL(extensionPanel, 0, 30, new JLabel(YMAX + ":"),
                textFieldYMax, false);
        FormUtils.addRowInGBL(extensionPanel, 1, 0, new JLabel(XMIN + ":"),
                textFieldXMin, false);
        FormUtils.addFiller(extensionPanel, 1, 2);
        FormUtils.addRowInGBL(extensionPanel, 1, 61, textFieldXMax, false,
                false);
        FormUtils.addRowInGBL(extensionPanel, 1, 60, new JLabel(XMAX + ":"),
                false, false);
        FormUtils.addRowInGBL(extensionPanel, 2, 30, new JLabel(YMIN + ":"),
                textFieldYMin, false);

        return extensionPanel;
    }

    // Projection Panel "SRS description"
    private JScrollPane projectionPanel(String projection) {
        JPanel pan = new JPanel(new GridBagLayout());
        projArea = new JTextArea();
        projArea.setBackground(dialog.getBackground());
        projArea.setCaretPosition(0);
        projArea.setLineWrap(true);
        projArea.setWrapStyleWord(true);
        projArea.setToolTipText(""); //$NON-NLS-1$
        projArea.setRows(2);
        projArea.setColumns(40);
        projArea.setFont(pan.getFont());
        projArea.setText(projection);
        projArea.setEditable(false);
        JScrollPane areaScrollPane = new JScrollPane(projArea);
        return areaScrollPane;
    }

    // Project file panel
    private JScrollPane filePanel(String projection) {
        JPanel pan = new JPanel(new GridBagLayout());
        fileArea = new JTextArea();
        fileArea.setBackground(dialog.getBackground());
        fileArea.setCaretPosition(0);
        fileArea.setLineWrap(true);
        fileArea.setWrapStyleWord(true);
        fileArea.setToolTipText("");
        fileArea.setRows(2);
        fileArea.setColumns(40);
        fileArea.setFont(pan.getFont());
        fileArea.setText(projection);
        fileArea.setEditable(false);
        JScrollPane areaScrollPane = new JScrollPane(fileArea);
        return areaScrollPane;
    }

    // Layers list panel
    private class LayersPanel extends HTMLPanel  {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        private LayersPanel(PlugInContext context) throws Exception {
            String info = "";
            info = info
                    + header(LAYER_NAME, DATASOURCE_CLASS, SOURCE_PATH, SRS);
            Collection<Layerable> layerables = context.getTask()
                    .getLayerManager().getLayerables(Layerable.class);
            for (Iterator<Layerable> i = layerables.iterator(); i.hasNext();) {
                Layerable layer = i.next();
                info = info
                        + text(Utils.getLayerableName(layer),
                                Utils.getLayerableType(layer),
                                Utils.getLayerablePath(layer), "");
                // Deactivate search layerable SRS
                // Utils.getLayerableSRS(layer));
            }
            String a = "<table width=\"450\" bgcolor=\"#000000\" cellpadding=\"10\" cellspacing=\"1\">";
            String b = "</table>";
            String layers = a + info + b;
            String temp = "";
            if (Utils.checkTemporaryLayerables(context)) {
                temp = "<table border='0.1'>" + text2(TEMPORARY_LAYERS)
                        + "</table>";
                layers = layers + temp;
            }
            String mod = "";
            if (Utils.checkModifiedLayers(context)) {
                mod = "<table border='0.1'>" + text2(MODIFIED_LAYERS)
                        + "</table>";
                layers = layers + mod;
            }
            String infotext = "<html>" + layers + "</html>";
            getRecordPanel().removeAll();
            getSaveButton().setVisible(false);
            createNewDocument();
            append(infotext);
        }

      }

    // Values for Layerable Table
    public String header(String layer, String type, String path, String proj) {
        String head = "  <tr valign=\"top\">"
                + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor1
                + "align=\"center\"><font face=" + fontName
                + " size=\"2\" align=\"right\"><b>" + layer
                + "</b></font></td>"
                + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor1
                + "align=\"center\"><font face=" + fontName + " size=\"2\"><b>"
                + type + "</b></font></td>"
                + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor1
                + "align=\"center\"><font face=" + fontName + " size=\"2\"><b>"
                + path + "</b></font></td>"
                /*
                 * + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor1
                 * + "align=\"center\"><font face=" + fontName +
                 * " size=\"2\"><b>" + proj + "</b></font></td>"
                 */
                + "  </tr>";
        return head;
    }

    public String text(String layer, String type, String path, String proj) {
        String prop = "  <tr valign=\"top\">"
                + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor3
                + "align=\"right\"><font face=" + fontName
                + " size=\"2\" align=\"right\">" + layer + "</font></td>"
                + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor3
                + "align=\"left\"><font face=" + fontName + " size=\"2\" >"
                + type + "</font></td>"
                + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor3
                + "align=\"left\"><font face=" + fontName + " size=\"2\" >"
                + path + "</font></td>"
                /*
                 * + "     <td width=\"550\" height=\"12\" bgcolor=" + bgColor3
                 * + "align=\"left\"><font face=" + fontName + " size=\"2\" >" +
                 * proj + "</font></td>"
                 */
                + "  </tr>";
        return prop;
    }

    public String text2(String type) {
        String prop = "  <tr valign=\"top\">"
                + "     <td width=\"550\" height=\"12\" bgcolor="
                + bgColor3
                + "align=\"right\"><font face="
                + fontName
                + " size=\"2\" align=\"right\">"
                + ""
                + "</font></td>"
                + "     <td width=\"1586\" height=\"12\" bgcolor="
                + bgColor3
                + "align=\"left\"><font face="
                + fontName
                + " size=\"2\" ><i>"
                + ""
                + "</i></font></td>"
                + "     <td width=\"1586\" height=\"12\" bgcolor="
                + bgColor3
                + "align=\"left\"><font face="
                + fontName
                + " size=\"2\" ><i>"
                + "<font color=\"red\">"
                + type
                + "</i></font></td>"
                + "  </tr>";
        return prop;
    }

}
