package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;

public class HTMLPanel extends JPanel implements RecordPanelModel {
    private ArrayList history = new ArrayList();
    protected JButton okButton = new JButton();
    private RecordPanel recordPanel = new RecordPanel(this);
    private JPanel southPanel = new JPanel();
    private JScrollPane scrollPane = new JScrollPane();
    private JEditorPane editorPane = new JEditorPane();
    private int currentIndex = -1;
    private JButton saveButton = new JButton();

    public HTMLPanel() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        okButton.setVisible(false);
    }

    public RecordPanel getRecordPanel() {
        return recordPanel;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    @Override
    public int getCurrentIndex() {
        return currentIndex;
    }

    public void addField(String label, String value) {
        addField(label, value, "");
    }

    private void setLatestDocument(String document) {
        history.set(history.size() - 1, document);
    }

    private String getLatestDocument() {
        return (String) history.get(history.size() - 1);
    }

    protected void setEditorPaneText() {
        final String document = (String) history.get(currentIndex);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                editorPane.setText("<HTML>" + document + "</HTML>");
            }
        });
        scrollToTop();
    }

    /**
     * Appends HTML text to the frame.
     * 
     * @param html
     *            the HTML to append
     */
    public void append(final String html) {
        setLatestDocument(getLatestDocument() + html);
        goToLatestDocument();
    }

    /**
     * Appends non-HTML text to the frame. Text is assumed to be non-HTML, and
     * is HTML-escaped to avoid control-char conflict.
     * 
     * @param text
     */
    public void addText(String text) {
        append(GUIUtil.escapeHTML(text, false, true) + " <BR>\n");
    }

    public void addField(String label, String value, String units) {
        append("<B> " + label + " </B>" + value + " " + units + " <BR>\n");
    }

    /**
     * @param level
     *            1, 2, 3, ...
     */
    public void addHeader(int level, String text) {
        append("<H" + level + "> " + GUIUtil.escapeHTML(text, false, false)
                + " </H" + level + ">\n");
    }

    private JPanel fillerPanel = new JPanel();

    public JButton getOKButton() {
        return okButton;
    }

    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    private void jbInit() throws Exception {
        setLayout(new BorderLayout());
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        southPanel.setLayout(gridBagLayout1);
        scrollPane
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        okButton.setText("OK");

        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        southPanel.add(fillerPanel, new GridBagConstraints(1, 1, 1, 1, 1.0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        southPanel.add(recordPanel, new GridBagConstraints(0, 1, 1, 1, 0.0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        southPanel.add(okButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
                        0, 0, 0, 0), 0, 0));

        /*
         * Giuseppe Aruta 2015_01_03 Add Button to save view as HTML
         */
        saveButton = new JButton(
                I18N.get("deejump.plugin.SaveLegendPlugIn.Save")); //$NON-NLS-1$
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveButton_actionPerformed(e);
            }
        });
        southPanel.add(saveButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
                        0, 0, 0, 0), 0, 0));

        scrollPane.getViewport().add(editorPane, null);
    }

    public String lastString() {
        String text = "";
        for (int i = 0; i < history.size(); i++) {
            text = history.get(i).toString();
        }
        return text;

    }

    /*
     * Giuseppe Aruta 2015_01_03 Modified from code from Kosmo 2.0 to save to
     * HTML
     */
    protected void saveButton_actionPerformed(ActionEvent e) {
        JFileChooser chooser;
        File archivo = null;
        chooser = GUIUtil.createJFileChooserWithOverwritePrompting();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(GUIUtil.createFileFilter(
                I18N.get("org.openjump.core.ui.plugin.file.open.SelectFileLoaderPanel.file-type"), new String[] { "htm" })); //$NON-NLS-1$//$NON-NLS-2$
        int returned = chooser.showSaveDialog(JUMPWorkbench.getInstance()
                .getFrame());

        if (returned == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            chooser.getSelectedFile().delete();
            archivo = new File(path);
            archivo = FileUtil.addExtensionIfNone(archivo, "htm");//$NON-NLS-1$

            try {
                String texto = history.get(currentIndex).toString();
                String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                        .format(Calendar.getInstance().getTime());
                String all = texto + "<B>" + timeStamp + "</B>";
                FileUtil.setContents(archivo.getAbsolutePath(), all);
            } catch (Exception e1) {
                Logger.error(e1);
                JUMPWorkbench
                        .getInstance()
                        .getFrame()
                        .warnUser(
                                I18N.get("org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn.Error-See-Output-Window"));
                JUMPWorkbench.getInstance().getFrame().getOutputFrame()
                        .createNewDocument();
                JUMPWorkbench
                        .getInstance()
                        .getFrame()
                        .getOutputFrame()
                        .addText(
                                "SaveImageToRasterPlugIn Exception:"
                                        + new Object[] { e.toString() });
            }
        }
    }

    public void setRecordNavigationControlVisible(boolean visible) {
        southPanel.setVisible(visible);
    }

    public void scrollToTop() {
        // The text is set using #invokeLater, so the scroll-to-top must
        // also be done using #invokeLater. [Jon Aquino]
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                editorPane.setCaretPosition(0);
            }
        });
    }

    private void goToLatestDocument() {
        setCurrentIndex(history.size() - 1);
    }

    public void createNewDocument() {
        history.add("");
        goToLatestDocument();
        recordPanel.updateAppearance();
    }

    @Override
    public void setCurrentIndex(int index) {
        this.currentIndex = index;
        setEditorPaneText();
    }

    @Override
    public int getRecordCount() {
        return history.size();
    }

    public Color getBackgroundColor() {
        return editorPane.getBackground();
    }

    public void setBackgroundColor(Color color) {
        editorPane.setBackground(color);
    }
}
