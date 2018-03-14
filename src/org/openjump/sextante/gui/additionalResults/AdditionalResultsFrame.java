package org.openjump.sextante.gui.additionalResults;

import it.betastudio.adbtoolbox.libs.DxfExport;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.math.plot.PlotPanel;
import org.math.plot.plots.Plot;
import org.openjump.core.apitools.IOTools;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.swing.DetachableInternalFrame;
import org.openjump.core.ui.util.LayerableUtil;
import org.openjump.sextante.core.ObjectAndDescription;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.ui.FeatureCollectionPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
//-da rimuovere
//import com.vividsolutions.jump.workbench.ui.OKCancelApplyPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class AdditionalResultsFrame extends DetachableInternalFrame {

    /**
     * Flexible generic frame for prompting the results in several objects. This
     * frame is a refactoring of Sextante
     * es.unex.sextante.gui.additionalResults.AdditionalResultsDialog from
     * library SextanteGUI.jar. Methods to promping AdditionalResultsFrame are
     * located to the class AdditionalResults
     * 
     * @author Giuseppe Aruta [2017-12-12]
     */

    // Main components of a AdditionalResultsFrame

    private final String name = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Result-viewer");
    private final String sMenu = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Menu");
    private final String sRemove = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Remove");
    private final String sRename = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Rename");
    private final String sSave = I18N
            .get("deejump.plugin.SaveLegendPlugIn.Save");
    private final String sWriteName = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Write-name");
    private final String sChangeName = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Change-name");
    private final String sSaved = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final String SCouldNotSave = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");
    private final String sProcessing = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Processing");
    private final String sResult = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Result");
    private final String SAVE = I18N
            .get("deejump.plugin.SaveLegendPlugIn.Save");
    private final String CLOSE = I18N
            .get("ui.plugin.imagery.ImageLayerManagerDialog.Close");

    private static final long serialVersionUID = 1L;
    private JSplitPane jSplitPane;
    private JTree jTree;
    private TreePath m_Path;
    private JScrollPane jScrollPane;
    private JMenuItem menuItemSave;
    private JPopupMenu popupMenu;
    private JMenuItem menuItemRemove;
    private JMenuItem menuItemRename;

    private static String LAST_DIR = null;

    // --da rimuovere
    // final protected OKCancelApplyPanel okCancelApplyPanel = new
    // OKCancelApplyPanel();

    public AdditionalResultsFrame(final ArrayList<?> components) {

        initGUI(components);
        setTitle(name);
        setResizable(true);
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);

        setSize(900, 700);
        setLayer(JLayeredPane.MODAL_LAYER);
    }

    public Icon getColorIcon() {
        final ImageIcon icon = new ImageIcon(getClass().getResource(
                "application_view.png"));
        return GUIUtil.toSmallIcon(icon);
    }

    private boolean initGUI(final ArrayList<?> components) {

        final JPanel panel = new JPanel();
        final BorderLayout thisLayout = new BorderLayout();
        panel.setLayout(thisLayout);
        setContentPane(panel);

        if (components.size() == 0) {
            return false;
        }
        try {
            {
                setPreferredSize(new Dimension(900, 450));
                this.setSize(new Dimension(900, 450));
                {
                    jSplitPane = new JSplitPane();
                    jSplitPane.setDividerSize(9);
                    jSplitPane.setContinuousLayout(true);
                    jSplitPane.setOneTouchExpandable(true);
                    jSplitPane.setDividerLocation(200);
                    panel.add(jSplitPane, BorderLayout.CENTER);

                    panel.add(getOKSavePanel(), BorderLayout.SOUTH);

                    {
                        jTree = new JTree();
                        jTree.setCellRenderer(new AdditionalResultsTreeCellRenderer());

                        final MouseListener ml = new MouseAdapter() {
                            @Override
                            public void mousePressed(MouseEvent e) {

                                m_Path = jTree.getPathForLocation(e.getX(),
                                        e.getY());
                                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                                        .getLastPathComponent();
                                if (node.getUserObject() instanceof ObjectAndDescription) {
                                    showComponent();
                                    if ((e.getButton() == MouseEvent.BUTTON3)
                                            && (m_Path != null)) {

                                        showPopupMenu(e);
                                    }
                                }
                            }
                        };
                        jTree.addMouseListener(ml);

                        fillTree(components);

                        if (components.size() > 0) {
                            final DefaultMutableTreeNode node = findNode((ObjectAndDescription) components
                                    .get(components.size() - 1));
                            final DefaultTreeModel model = (DefaultTreeModel) jTree
                                    .getModel();
                            final TreePath path = new TreePath(
                                    model.getPathToRoot(node));
                            jTree.setSelectionPath(path);
                            jTree.scrollPathToVisible(path);
                            m_Path = path;
                            showComponent();
                        }
                        jScrollPane = new JScrollPane(
                                jTree,
                                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                        jScrollPane.setPreferredSize(new Dimension(200, 450));
                        // jScrollPane.setMinimumSize(new Dimension(200, 450));
                        jScrollPane.setMaximumSize(new Dimension(200, 450));
                    }
                    {
                        jSplitPane.add(jScrollPane, JSplitPane.LEFT);
                    }
                }
            }

            popupMenu = new JPopupMenu(sMenu);
            menuItemSave = new JMenuItem(sSave);
            menuItemSave.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {
                    if (m_Path != null) {
                        try {
                            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                                    .getLastPathComponent();
                            if (node.getUserObject() instanceof ObjectAndDescription) {
                                save();
                            }
                        } catch (final Exception e) {
                        }
                    }
                }
            });
            popupMenu.add(menuItemSave);
            menuItemRemove = new JMenuItem(sRemove);
            menuItemRemove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {
                    if (m_Path != null) {
                        try {
                            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                                    .getLastPathComponent();
                            if (node.getUserObject() instanceof ObjectAndDescription) {
                                remove();
                            }
                        } catch (final Exception e) {
                        }
                    }
                    ;
                }
            });
            popupMenu.add(menuItemRemove);
            menuItemRename = new JMenuItem(sRename);
            menuItemRename.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {
                    if (m_Path != null) {
                        try {
                            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                                    .getLastPathComponent();
                            if (node.getUserObject() instanceof ObjectAndDescription) {
                                rename();
                            }
                        } catch (final Exception e) {
                        }
                    }
                    ;
                }

            });
            popupMenu.add(menuItemRename);

            // ---Da rimuovere
            // okCancelApplyPanel.addActionListener(new ActionListener() {
            // @Override
            // public void actionPerformed(final ActionEvent evt) {

            // try {
            // dispose();
            // } catch (final Exception e) {
            // }
            //
            // }
            // });
            // okCancelApplyPanel.setApplyVisible(false);
            // okCancelApplyPanel.setCancelVisible(false);
            // okCancelApplyPanel.setOKEnabled(true);
            panel.updateUI();
            return true;
        } catch (final Exception e) {
            Logger(this.getClass(), e);
            return false;
        }

    }

    public Component getLeftPanel() {
        return jSplitPane.getLeftComponent();

    }

    public Component getRightPanel() {
        return jSplitPane.getRightComponent();
    }

    public JPopupMenu getPopupMen() {
        return popupMenu;
    }

    protected void showPopupMenu(final MouseEvent e) {
        jTree.setSelectionPath(m_Path);

        menuItemSave.setEnabled(true);
        popupMenu.show(e.getComponent(), e.getX(), e.getY());

    }

    private void rename() {

        if (m_Path != null) {
            try {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                        .getLastPathComponent();
                final ObjectAndDescription oad = (ObjectAndDescription) node
                        .getUserObject();
                final String sName = oad.getDescription();
                final JOptionPane pane = new JOptionPane();
                pane.setMessage(sWriteName);
                pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
                pane.setWantsInput(true);
                pane.setInitialSelectionValue(sName);
                pane.setInputValue(sName);
                final JDialog dlg = pane.createDialog(null, sChangeName);
                dlg.setModal(true);
                dlg.setVisible(true);
                final String sNewName = pane.getInputValue().toString().trim();

                if ((sNewName != null) && (sNewName.length() != 0)) {
                    oad.setDescription(sNewName);
                }
                update();
            } catch (final Exception e) {
            }
        }

    }

    protected void remove() {
        if (m_Path != null) {
            try {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                        .getLastPathComponent();
                final ObjectAndDescription oad = (ObjectAndDescription) node
                        .getUserObject();
                AdditionalResults.removeComponent(oad);
                update();
            } catch (final Exception e) {
            }
        }

    }

    private static final String FILE_CHOOSER_DIRECTORY_KEY = SaveFileDataSourceQueryChooser.class
            .getName() + " - FILE CHOOSER DIRECTORY";

    protected void save() {
        final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting();
        // fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
        // FILE_BROWSER_HEIGHT));
        final File filedir = new File((String) PersistentBlackboardPlugIn.get(
                JUMPWorkbench.getInstance().getContext()).get(
                FILE_CHOOSER_DIRECTORY_KEY));
        FileNameExtensionFilter filter;
        if (LAST_DIR != null) {
            fc.setCurrentDirectory(new File(LAST_DIR));
        } else {
            fc.setCurrentDirectory(filedir);
        }
        final File file;
        if (m_Path != null) {
            try {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                        .getLastPathComponent();
                final ObjectAndDescription oad = (ObjectAndDescription) node
                        .getUserObject();
                final Component c = (Component) oad.getObject();
                if (c instanceof FeatureCollectionPanel) {
                    final FeatureCollectionPanel panel = (FeatureCollectionPanel) c;
                    final FeatureCollection fcoll = panel
                            .getFeatureCollection();
                    if (LayerableUtil.isMixedGeometryType(fcoll)) {
                        filter = new FileNameExtensionFilter("JML", "jml");
                    } else {
                        filter = new FileNameExtensionFilter("SHP", "shp");
                    }
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    // FILE_BROWSER_WIDTH = fc.getWidth();
                    // FILE_BROWSER_HEIGHT = fc.getHeight();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        if (LayerableUtil.isMixedGeometryType(fcoll)) {
                            file = new File(fc.getSelectedFile() + ".jml");
                            IOTools.saveJMLFile(fcoll, file.getAbsolutePath());
                        } else {
                            file = new File(fc.getSelectedFile() + ".shp");
                            IOTools.saveShapefile(fcoll, file.getAbsolutePath());
                        }
                        saved(file);
                    }
                } else if (c instanceof JScrollPane) {
                    final JScrollPane pane = (JScrollPane) c;
                    final Component view = pane.getViewport().getView();
                    if (view instanceof JTextPane) {
                        final JTextPane text = (JTextPane) pane.getViewport()
                                .getView();
                        filter = new FileNameExtensionFilter("HTML", "html");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        fc.getWidth();
                        fc.getHeight();
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                file = new File(fc.getSelectedFile() + ".html");
                                LAST_DIR = file.getParent();
                                final FileWriter fileWriter = new FileWriter(
                                        file);
                                final BufferedWriter bufferedWriter = new BufferedWriter(
                                        fileWriter);
                                bufferedWriter.write(text.getText());
                                bufferedWriter.close();
                                saved(file);
                            } catch (final Exception e) {
                                notsaved();
                                Logger(this.getClass(), e);
                            }
                        }
                    } else if (view instanceof JLabel) {
                        final String text = ((JLabel) view).getText();
                        filter = new FileNameExtensionFilter("HTML", "html");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        fc.getWidth();
                        fc.getHeight();
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                file = new File(fc.getSelectedFile() + ".html");
                                LAST_DIR = file.getParent();
                                final FileWriter fileWriter = new FileWriter(
                                        file);
                                final BufferedWriter bufferedWriter = new BufferedWriter(
                                        fileWriter);
                                bufferedWriter.write(text);
                                bufferedWriter.close();
                                saved(file);
                            } catch (final Exception e) {
                                notsaved();
                                Logger(this.getClass(), e);
                            }
                        }
                    } else if (view instanceof JTextArea) {
                        final String text = ((JLabel) view).getText();
                        filter = new FileNameExtensionFilter("HTML", "html");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        fc.getWidth();
                        fc.getHeight();
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                file = new File(fc.getSelectedFile() + ".html");
                                LAST_DIR = file.getParent();
                                final FileWriter fileWriter = new FileWriter(
                                        file);
                                final BufferedWriter bufferedWriter = new BufferedWriter(
                                        fileWriter);
                                bufferedWriter.write(text);
                                bufferedWriter.close();
                                saved(file);
                            } catch (final Exception e) {
                                notsaved();
                                Logger(this.getClass(), e);
                            }
                        }
                    } else if (view instanceof JTable) {
                        final JTable table = (JTable) pane.getViewport()
                                .getView();
                        filter = new FileNameExtensionFilter(
                                "Comma-Separated Values (csv)", "csv");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        fc.getWidth();
                        fc.getHeight();
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                file = new File(fc.getSelectedFile() + ".csv");
                                LAST_DIR = file.getParent();
                                final FileWriter fw = new FileWriter(
                                        file.getAbsoluteFile());
                                final BufferedWriter bw = new BufferedWriter(fw);

                                for (int j = 0; j < table.getColumnCount(); j++) {
                                    bw.write(table.getModel().getColumnName(j)
                                            + ",");
                                }
                                bw.write("\n");
                                for (int i = 0; i < table.getRowCount(); i++) {
                                    for (int j = 0; j < table.getColumnCount(); j++) {
                                        bw.write(table.getModel().getValueAt(i,
                                                j)
                                                + ",");
                                    }
                                    bw.write("\n");
                                }
                                bw.close();
                                fw.close();
                                saved(file);
                            } catch (final Exception e) {
                                notsaved();
                                Logger(this.getClass(), e);
                            }
                        } else if (returnVal == JFileChooser.CANCEL_OPTION) {
                            return;
                        }
                    }
                } else if (c instanceof PlotPanel) {
                    final PlotPanel panel = (PlotPanel) c;
                    // final JFileChooser chooser =
                    // SaveGUI.getFileChooser(panel);
                    // chooser.showSaveDialog(this);

                    filter = new FileNameExtensionFilter(
                            "Portable Network Graphics (png)", "png");
                    final FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
                            "Drawing Interchange Format(dxf)", "dxf");

                    if (oad.getDescription()
                            .contains(
                                    I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot"))) {
                        fc.setFileFilter(filter2);
                    }
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    fc.getWidth();
                    fc.getHeight();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        if (fc.getFileFilter().equals(filter)) {
                            file = new File(fc.getSelectedFile() + ".png");
                            LAST_DIR = file.getParent();
                            panel.toGraphicFile(file);
                            saved(file);
                        } else if (fc.getFileFilter().equals(filter2)) {
                            file = new File(fc.getSelectedFile() + ".dxf");
                            LAST_DIR = file.getParent();
                            double[][] pointsOfProfile = null;
                            for (final Plot plot : panel.getPlots()) {
                                pointsOfProfile = plot.getData();
                            }
                            setCursor(new Cursor(Cursor.WAIT_CURSOR));
                            exportToDxf(file.getAbsolutePath(), pointsOfProfile);
                            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            saved(file);
                        }
                    }

                } else if (c instanceof JTable) {
                    final JTable table = (JTable) c;
                    filter = new FileNameExtensionFilter(
                            "Comma-Separated Values (csv)", "cvs");
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    fc.getWidth();
                    fc.getHeight();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            file = new File(fc.getSelectedFile() + ".csv");
                            LAST_DIR = file.getParent();
                            final FileWriter fw = new FileWriter(
                                    file.getAbsoluteFile());
                            final BufferedWriter bw = new BufferedWriter(fw);

                            for (int j = 0; j < table.getColumnCount(); j++) {
                                bw.write(table.getModel().getColumnName(j)
                                        + ",");
                            }
                            bw.write("\n");
                            for (int i = 0; i < table.getRowCount(); i++) {
                                for (int j = 0; j < table.getColumnCount(); j++) {
                                    bw.write(table.getModel().getValueAt(i, j)
                                            + ",");
                                }
                                bw.write("\n");
                            }
                            bw.close();
                            fw.close();
                            saved(file);
                        } catch (final Exception e) {
                            notsaved();
                            Logger(this.getClass(), e);
                        }

                    } else if (returnVal == JFileChooser.CANCEL_OPTION) {
                        return;
                    }
                } else if (c instanceof HTMLPanel) {
                    final HTMLPanel panel = (HTMLPanel) c;
                    final String text = panel.lastString();
                    filter = new FileNameExtensionFilter("HTML", "html");
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    fc.getWidth();
                    fc.getHeight();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            file = new File(fc.getSelectedFile() + ".html");
                            LAST_DIR = file.getParent();
                            final FileWriter fileWriter = new FileWriter(file);
                            final BufferedWriter bufferedWriter = new BufferedWriter(
                                    fileWriter);
                            bufferedWriter.write(text);
                            bufferedWriter.close();
                            saved(file);
                        } catch (final Exception e) {
                            notsaved();
                            Logger(this.getClass(), e);
                        }
                    }
                } else if (c instanceof JPanel) {
                    final JPanel panel = (JPanel) c;
                    final int w = panel.getWidth();
                    final int h = panel.getHeight();
                    final BufferedImage bi = new BufferedImage(w, h,
                            BufferedImage.TYPE_INT_RGB);
                    final Graphics2D g = bi.createGraphics();
                    panel.paint(g);
                    filter = new FileNameExtensionFilter(
                            "Portable Network Graphics (png)", "png");
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    fc.getWidth();
                    fc.getHeight();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            file = new File(fc.getSelectedFile() + ".png");
                            ImageIO.write(bi, "png", file);
                            saved(file);
                        } catch (final Exception e) {
                            notsaved();
                            Logger(this.getClass(), e);
                        }
                    }
                }

            } catch (final Exception e) {
                Logger(this.getClass(), e);
            }
        }
    }

    protected void saved(File file) {
        JUMPWorkbench.getInstance().getFrame()
                .setStatusMessage(sSaved + " :" + file.getAbsolutePath());
    }

    protected void notsaved() {
        JOptionPane.showMessageDialog(null, SCouldNotSave, I18N.get(name),
                JOptionPane.WARNING_MESSAGE);
    }

    protected void showComponent() {
        if (m_Path != null) {
            try {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                        .getLastPathComponent();
                final ObjectAndDescription oad = (ObjectAndDescription) node
                        .getUserObject();
                final Component c = (Component) oad.getObject();
                c.setMinimumSize(new Dimension(300, 200));
                jSplitPane.setRightComponent(c);
            } catch (final Exception e) {
                Logger(this.getClass(), e);
            }
        }
    }

    public void fillTree(final ArrayList<?> components) {
        DefaultMutableTreeNode node;
        final DefaultMutableTreeNode mainNode = new DefaultMutableTreeNode(
                sProcessing);
        final DefaultMutableTreeNode componentsNode = new DefaultMutableTreeNode(
                sResult);

        for (int i = 0; i < components.size(); i++) {
            node = new DefaultMutableTreeNode(components.get(i));
            componentsNode.add(node);
        }
        mainNode.add(componentsNode);
        jTree.setModel(new DefaultTreeModel(mainNode));
    }

    public void update() {
        if (!initGUI(AdditionalResults.getComponents())) {
            dispose();
            setVisible(false);
        }
    }

    private DefaultMutableTreeNode findNode(final ObjectAndDescription oad) {
        Object ob;
        final DefaultTreeModel data = (DefaultTreeModel) jTree.getModel();
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) data
                .getRoot();
        DefaultMutableTreeNode node = null;
        if (root != null) {
            for (final Enumeration<?> e = root.breadthFirstEnumeration(); e
                    .hasMoreElements();) {
                final DefaultMutableTreeNode current = (DefaultMutableTreeNode) e
                        .nextElement();
                ob = current.getUserObject();
                if (ob instanceof ObjectAndDescription) {
                    if (ob == oad) {
                        node = current;
                        break;
                    }
                }
            }
        }
        return node;
    }

    public static void Logger(Class<?> plugin, Exception e) {
        final Logger LOG = Logger.getLogger(plugin);
        JUMPWorkbench
                .getInstance()
                .getFrame()
                .warnUser(
                        plugin.getSimpleName() + " Exception: " + e.toString());
        LOG.error(plugin.getName() + " Exception: ", e);
    }

    protected JPanel getOKSavePanel() {
        final JPanel okPanel = new JPanel();
        final JButton saveButton = new JButton(SAVE) {

            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };
        final JButton closeButton = new JButton(CLOSE) {
            private static final long serialVersionUID = 2L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };

        saveButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
                // frame.dispose();
                return;
            }
        });
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                dispose();

                return;
            }
        });
        okPanel.add(saveButton, BorderLayout.WEST);
        okPanel.add(closeButton, BorderLayout.EAST);
        return okPanel;

    }

    // [Giuseppe Aruta 2018-3-14] The following code derives from AdbToolbox
    // Raster>Topography>Section code.
    // see also class it.betastudio.adbtoolbox.libs.DxfExport

    public static void exportToDxf(String fileName, double[][] points) {

        try {

            double minX = Double.MAX_VALUE;
            double maxX = -minX;
            double minY = Double.MAX_VALUE;
            double maxY = -minY;

            // Find max and min vals
            for (final double[] point : points) {
                if (point[0] < minX) {
                    minX = point[0];
                }
                if (point[0] > maxX) {
                    maxX = point[0];
                }
                if (point[1] < minY) {
                    minY = point[1];
                }
                if (point[1] > maxY) {
                    maxY = point[1];
                }
            }
            final DecimalFormatSymbols dfs = new DecimalFormatSymbols(
                    Locale.ENGLISH);
            DecimalFormat twoPlaces = null;
            final String twoPlacesS = "0.00";
            twoPlaces = new DecimalFormat(twoPlacesS, dfs);

            final int pointsCount = points.length;
            int interPointsCount = 0;
            final int txtHight = 10;
            double baseElev = 0.0D;
            final int sepSpacing = 10 * txtHight;
            final String layNameProf = "PROFILE";
            final String layNameNeatLines = "BASE";
            final String layNameText = "TEXT";

            if (baseElev < minY) {
                minY = baseElev;
            } else {
                baseElev = minY;
            }

            // Main points coords
            final double sep1Y = minY - txtHight * 2;
            final double sep2Y = sep1Y - sepSpacing;
            final double sep3Y = sep2Y - sepSpacing;

            final double legX = minX - (txtHight * 30);
            final double leg1Y = minY + 0.5 * txtHight;
            final double leg2Y = sep1Y + txtHight;
            final double leg3Y = sep2Y + 0.5 * sepSpacing;
            final double leg4Y = sep3Y + 0.5 * sepSpacing;

            final DxfExport dxfExp = new DxfExport();

            // Write header
            // --------------------------------------------------------
            dxfExp.writeHeader(legX, sep3Y, maxX, maxY);

            // Write tables
            dxfExp.writeStartSec();
            dxfExp.writeTablesStart();

            dxfExp.writeTableStart();
            dxfExp.writeVPort((maxX + legX) / 2, (maxY + sep3Y) / 2, 0, 0, 1, 1);
            dxfExp.writeTableEnd();

            dxfExp.writeTableStart();
            dxfExp.writeAppId();
            dxfExp.writeTableEnd();

            dxfExp.writeTableStart();
            dxfExp.writeLayersStart();
            dxfExp.writeLayer(layNameProf, 1);
            dxfExp.writeLayer(layNameNeatLines, 5);
            dxfExp.writeLayer(layNameText, 7);
            dxfExp.writeTableEnd();

            dxfExp.writeEndSec();

            // Write section
            dxfExp.writeStartSec();
            dxfExp.writeEntStart();
            dxfExp.writePolyline(layNameProf, points);

            // Write bounding lines
            // dxfExp.writeLine(layNameNeatLines, minX, minY, minX,
            // points[0][1]);
            dxfExp.writeLine(layNameNeatLines, legX, minY, maxX, minY);
            // dxfExp.writeLine(layNameNeatLines, maxX, minY, maxX,
            // points[pointsCount-1][1]);

            // Write separators
            dxfExp.writeLine(layNameNeatLines, legX, sep1Y, maxX, sep1Y);
            dxfExp.writeLine(layNameNeatLines, legX, sep2Y, maxX, sep2Y);
            dxfExp.writeLine(layNameNeatLines, legX, sep3Y, maxX, sep3Y);

            // Write legend

            final Task selectedTask = workbenchContext.getTask();
            String unitsDistLabel = "";

            if (selectedTask.getProperties().containsKey(
                    new QName(Task.PROJECT_UNIT_KEY))) {
                unitsDistLabel = selectedTask.getProperty(
                        new QName(Task.PROJECT_UNIT_KEY)).toString();
            } else {
                unitsDistLabel = "";
            }
            dxfExp.writeText(layNameText, legX, leg1Y, 0, 0, 0, 0, txtHight, 0,
                    0, 0, "Reference height: " + twoPlaces.format(baseElev)
                            + " " + unitsDistLabel);
            dxfExp.writeText(layNameText, 0, 0, 0, legX, leg2Y, 0, txtHight, 0,
                    0, 2, "Partial distance" + unitsDistLabel);
            dxfExp.writeText(layNameText, 0, 0, 0, legX, leg3Y, 0, txtHight, 0,
                    0, 2, "Progressive distance" + unitsDistLabel);
            dxfExp.writeText(layNameText, 0, 0, 0, legX, leg4Y, 0, txtHight, 0,
                    0, 2, "Height" + unitsDistLabel);

            // Write interpoints labels and ticks
            double p1x = 0;
            double p1y = 0;
            double p2x = 0;
            double p2y = 0;
            double alPt1x = 0;
            double alPt1y = 0;
            double alPt2y = 0;
            String labelText = null;

            // Count interpoints
            double[] interStepD = null;

            interStepD = new double[1];
            // Define 100 meters for length/height steps
            interStepD[0] = 100;
            interPointsCount = (int) ((maxX - minX) / interStepD[0]) + 2;

            // Prepare x positions
            final double[] interPointsDists = new double[interPointsCount];
            final double[] interPointsElev = new double[interPointsCount];
            for (int ip = 0; ip < interPointsCount; ip++) {

                if (ip < interPointsCount - 1) {
                    interPointsDists[ip] = interStepD[0] * ip;
                } else {
                    interPointsDists[ip] = maxX;
                }

            }

            // Prepare points IDs
            final int[] interPointsIds = new int[interPointsCount];
            int ipId = 0;
            for (int p = 1; p < pointsCount; p++) {
                if (points[p][0] >= interPointsDists[ipId]) {
                    if (Math.abs(points[p][0] - interPointsDists[ipId]) <= Math
                            .abs(interPointsDists[ipId] - points[p - 1][0])) {
                        interPointsIds[ipId] = p;
                    } else {
                        interPointsIds[ipId] = p - 1;
                    }
                    ipId++;
                }
            }

            // Boh
            if (interPointsIds[interPointsCount - 1] == 0) {
                interPointsIds[interPointsCount - 1] = pointsCount - 1;
            }

            // Prepare y positions
            ipId = 0;
            for (int p = 1; p < pointsCount; p++) {
                if (points[p][0] >= interPointsDists[ipId]) {
                    if (Math.abs(points[p][0] - interPointsDists[ipId]) <= Math
                            .abs(interPointsDists[ipId] - points[p - 1][0])) {
                        interPointsIds[ipId] = p;
                    } else {
                        interPointsIds[ipId] = p - 1;
                    }

                    interPointsElev[ipId] = (interPointsDists[ipId] - points[p - 1][0])
                            / (points[p][0] - points[p - 1][0])
                            * (points[p][1] - points[p - 1][1])
                            + points[p - 1][1];
                    ipId++;
                }
            }

            for (int ip = 0; ip < interPointsCount; ip++) { // OKKIO

                // Vertical lines
                p1x = interPointsDists[ip];
                p1y = interPointsElev[ip];
                p2x = interPointsDists[ip];
                p2y = minY;
                dxfExp.writeLine(layNameNeatLines, p1x, p1y, p2x, p2y);

                if (ip < interPointsCount - 1) {
                    // Partial distance labels
                    alPt1x = (interPointsDists[ip] + interPointsDists[ip + 1]) / 2;
                    alPt1y = sep1Y + txtHight;
                    labelText = twoPlaces.format(interPointsDists[ip + 1]
                            - interPointsDists[ip]);
                    dxfExp.writeText(layNameText, 0, 0, 0, alPt1x, alPt1y, 0,
                            txtHight, 0, 4, 2, labelText);
                }

                // Progressive distance labels
                alPt1x = interPointsDists[ip];
                alPt1y = sep2Y + txtHight;
                labelText = twoPlaces.format(interPointsDists[ip]);
                dxfExp.writeText(layNameText, 0, 0, 0, alPt1x, alPt1y, 0,
                        txtHight, 90, 0, 2, labelText);

                // Elevation lables
                alPt1x = interPointsDists[ip];
                alPt1y = sep3Y + txtHight;
                labelText = twoPlaces.format(points[interPointsIds[ip]][1]);
                dxfExp.writeText(layNameText, 0, 0, 0, alPt1x, alPt1y, 0,
                        txtHight, 90, 0, 2, labelText);

                // Ticks
                alPt1x = interPointsDists[ip];
                alPt1y = sep2Y;
                alPt2y = sep2Y + (txtHight / 2);
                dxfExp.writeLine(layNameNeatLines, alPt1x, alPt1y, alPt1x,
                        alPt2y);
                alPt1y = sep3Y;
                alPt2y = sep3Y + (txtHight / 2);
                dxfExp.writeLine(layNameNeatLines, alPt1x, alPt1y, alPt1x,
                        alPt2y);

            }

            // Finalize DXF
            dxfExp.writeEnding();
            final int ret = dxfExp.exportDxf(fileName);
            if (ret == 0) {
                return;
            } else {
                return;
            }
        } catch (final Exception ex) {
            workbenchContext.getWorkbench().getFrame()
                    .warnUser("Errore durante l'esportazione: ");
            return;
        }
    }

    public static WorkbenchContext workbenchContext = JUMPWorkbench
            .getInstance().getContext();
}
