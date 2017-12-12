package org.openjump.sextante.gui.additionalResults;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.ArrayList;
import java.util.Enumeration;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
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

import org.apache.log4j.Logger;
import org.math.plot.PlotPanel;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.swing.DetachableInternalFrame;
import org.openjump.sextante.core.ObjectAndDescription;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.OKCancelApplyPanel;
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

    private String name = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Result-viewer");
    private String sMenu = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Menu");
    private String sRemove = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Remove");
    private String sRename = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Rename");
    private String sSave = I18N.get("deejump.plugin.SaveLegendPlugIn.Save");
    private String sWriteName = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Write-name");
    private String sChangeName = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Change-name");
    private String sSaved = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private String SCouldNotSave = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");
    private String sProcessing = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Processing");
    private String sResult = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Result");

    private static final long serialVersionUID = 1L;
    private JSplitPane jSplitPane;
    private JTree jTree;
    private TreePath m_Path;
    private JScrollPane jScrollPane;
    private JMenuItem menuItemSave;
    private JPopupMenu popupMenu;
    private JMenuItem menuItemRemove;
    private JMenuItem menuItemRename;

    private static int FILE_BROWSER_WIDTH = 800;
    private static int FILE_BROWSER_HEIGHT = 600;
    private static String LAST_DIR = null;
    final protected OKCancelApplyPanel okCancelApplyPanel = new OKCancelApplyPanel();

    public AdditionalResultsFrame(final ArrayList<?> components) {

        initGUI(components);
        setTitle(name);
        setResizable(true);
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);

        setSize(800, 500);
        setLayer(JLayeredPane.MODAL_LAYER);
    }

    public Icon getColorIcon() {
        ImageIcon icon = new ImageIcon(getClass().getResource(
                "application_view.png"));
        return GUIUtil.toSmallIcon(icon);
    }

    private boolean initGUI(final ArrayList<?> components) {

        final JPanel panel = new JPanel();
        final BorderLayout thisLayout = new BorderLayout();
        panel.setLayout(thisLayout);
        this.setContentPane(panel);

        if (components.size() == 0) {
            return false;
        }
        try {
            {
                this.setPreferredSize(new java.awt.Dimension(700, 350));
                this.setSize(new java.awt.Dimension(700, 350));
                {
                    jSplitPane = new JSplitPane();

                    panel.add(jSplitPane, BorderLayout.CENTER);
                    panel.add(okCancelApplyPanel, BorderLayout.SOUTH);

                    {
                        jTree = new JTree();
                        jTree.setCellRenderer(new AdditionalResultsTreeCellRenderer());

                        final MouseListener ml = new MouseAdapter() {
                            @Override
                            public void mousePressed(MouseEvent e) {

                                m_Path = jTree.getPathForLocation(e.getX(),
                                        e.getY());
                                DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
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
                        jScrollPane.setMinimumSize(new Dimension(200, 450));
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

            okCancelApplyPanel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {

                    try {
                        dispose();
                    } catch (final Exception e) {
                    }

                }
            });
            okCancelApplyPanel.setApplyVisible(false);
            okCancelApplyPanel.setCancelVisible(false);
            okCancelApplyPanel.setOKEnabled(true);
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
        File filedir = new File((String) PersistentBlackboardPlugIn.get(
                JUMPWorkbench.getInstance().getContext()).get(
                FILE_CHOOSER_DIRECTORY_KEY));
        final File file;
        if (m_Path != null) {
            try {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
                        .getLastPathComponent();
                final ObjectAndDescription oad = (ObjectAndDescription) node
                        .getUserObject();
                final Component c = (Component) oad.getObject();
                if (c instanceof JScrollPane) {
                    final JScrollPane pane = (JScrollPane) c;
                    final Component view = pane.getViewport().getView();
                    if (view instanceof JTextPane) {
                        final JTextPane text = (JTextPane) pane.getViewport()
                                .getView();
                        fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                                FILE_BROWSER_HEIGHT));
                        if (LAST_DIR != null) {
                            fc.setCurrentDirectory(new File(LAST_DIR));
                        } else {
                            fc.setCurrentDirectory(filedir);
                        }
                        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                "HTML", "html");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        FILE_BROWSER_WIDTH = fc.getWidth();
                        FILE_BROWSER_HEIGHT = fc.getHeight();
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
                        fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                                FILE_BROWSER_HEIGHT));
                        if (LAST_DIR != null) {
                            fc.setCurrentDirectory(new File(LAST_DIR));
                        } else {
                            fc.setCurrentDirectory(filedir);
                        }
                        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                "HTML", "html");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        FILE_BROWSER_WIDTH = fc.getWidth();
                        FILE_BROWSER_HEIGHT = fc.getHeight();
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
                        fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                                FILE_BROWSER_HEIGHT));
                        if (LAST_DIR != null) {
                            fc.setCurrentDirectory(new File(LAST_DIR));
                        } else {
                            fc.setCurrentDirectory(filedir);
                        }
                        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                "HTML", "html");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        FILE_BROWSER_WIDTH = fc.getWidth();
                        FILE_BROWSER_HEIGHT = fc.getHeight();
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
                        fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                                FILE_BROWSER_HEIGHT));
                        if (LAST_DIR != null) {
                            fc.setCurrentDirectory(new File(LAST_DIR));
                        } else {
                            fc.setCurrentDirectory(filedir);
                        }
                        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                "Comma-Separated Values (csv)", "cvs");
                        fc.setFileFilter(filter);
                        fc.addChoosableFileFilter(filter);
                        final int returnVal = fc.showSaveDialog(this);
                        FILE_BROWSER_WIDTH = fc.getWidth();
                        FILE_BROWSER_HEIGHT = fc.getHeight();
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                file = new File(fc.getSelectedFile() + ".csv");
                                LAST_DIR = file.getParent();
                                FileWriter fw = new FileWriter(
                                        file.getAbsoluteFile());
                                BufferedWriter bw = new BufferedWriter(fw);

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
                    fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                            FILE_BROWSER_HEIGHT));
                    if (LAST_DIR != null) {
                        fc.setCurrentDirectory(new File(LAST_DIR));
                    } else {
                        fc.setCurrentDirectory(filedir);
                    }
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            "Portable Network Graphics (png)", "png");
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    FILE_BROWSER_WIDTH = fc.getWidth();
                    FILE_BROWSER_HEIGHT = fc.getHeight();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        file = new File(fc.getSelectedFile() + ".png");
                        LAST_DIR = file.getParent();
                        panel.toGraphicFile(file);
                        saved(file);
                    }
                } else if (c instanceof JTable) {
                    final JTable table = (JTable) c;

                    fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                            FILE_BROWSER_HEIGHT));
                    if (LAST_DIR != null) {
                        fc.setCurrentDirectory(new File(LAST_DIR));
                    } else {
                        fc.setCurrentDirectory(filedir);
                    }
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            "Comma-Separated Values (csv)", "cvs");
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    FILE_BROWSER_WIDTH = fc.getWidth();
                    FILE_BROWSER_HEIGHT = fc.getHeight();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            file = new File(fc.getSelectedFile() + ".csv");
                            LAST_DIR = file.getParent();
                            FileWriter fw = new FileWriter(
                                    file.getAbsoluteFile());
                            BufferedWriter bw = new BufferedWriter(fw);

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
                    String text = panel.lastString();
                    fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                            FILE_BROWSER_HEIGHT));
                    if (LAST_DIR != null) {
                        fc.setCurrentDirectory(new File(LAST_DIR));
                    } else {
                        fc.setCurrentDirectory(filedir);
                    }

                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            "HTML", "html");
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    FILE_BROWSER_WIDTH = fc.getWidth();
                    FILE_BROWSER_HEIGHT = fc.getHeight();
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
                    int w = panel.getWidth();
                    int h = panel.getHeight();
                    BufferedImage bi = new BufferedImage(w, h,
                            BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = bi.createGraphics();
                    panel.paint(g);
                    fc.setPreferredSize(new Dimension(FILE_BROWSER_WIDTH,
                            FILE_BROWSER_HEIGHT));
                    if (LAST_DIR != null) {
                        fc.setCurrentDirectory(new File(LAST_DIR));
                    } else {
                        fc.setCurrentDirectory(filedir);
                    }
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            "Portable Network Graphics (png)", "png");
                    fc.setFileFilter(filter);
                    fc.addChoosableFileFilter(filter);
                    final int returnVal = fc.showSaveDialog(this);
                    FILE_BROWSER_WIDTH = fc.getWidth();
                    FILE_BROWSER_HEIGHT = fc.getHeight();
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
        Logger LOG = Logger.getLogger(plugin);
        JUMPWorkbench
                .getInstance()
                .getFrame()
                .warnUser(
                        plugin.getSimpleName() + " Exception: " + e.toString());
        LOG.error(plugin.getName() + " Exception: ", e);
    }

    public void setApplyVisible(boolean applyVisible) {
        okCancelApplyPanel.setApplyVisible(applyVisible);
    }

    public void setCancelVisible(boolean cancelVisible) {
        okCancelApplyPanel.setCancelVisible(cancelVisible);
    }

    public void setOKVisible(boolean okVisible) {
        okCancelApplyPanel.setOKVisible(okVisible);
    }

    public void setApplyEnabled(boolean applyEnabled) {
        okCancelApplyPanel.setApplyEnabled(applyEnabled);
    }

    public void setCancelEnabled(boolean cancelEnabled) {
        okCancelApplyPanel.setCancelEnabled(cancelEnabled);
    }

    public void setOKEnabled(boolean okEnabled) {
        okCancelApplyPanel.setOKEnabled(okEnabled);
    }

    public boolean wasApplyPressed() {
        return okCancelApplyPanel.wasApplyPressed();
    }

    public boolean wasOKPressed() {
        return okCancelApplyPanel.wasOKPressed();
    }

}
