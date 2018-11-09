package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * 
 * 
 * /** Modified from
 * http://jfontchooser.osdn.jp/site/jfontchooser/project-summary.html MIT/X
 * common Licence, Copyright (c) <2004> <Masahiko SAWAI>
 * 
 * - The panel gets Font and Text preview from a selected Text component -
 * Preview panel works as text editor. Preview is saved as text -
 * 
 * How it works
 * 
 * <pre>
 *   TextEditor editor = new JFontChooser();
 *   editor.showDialog(Component.getParent(), "Title of this font/editor panel");
 *   
 *   // <Optional, set font, font size, font style and preview text in the TextEditor from a JTextComponent>
 *   //                                                                                    
 *   //   editor.setSelectedFont(JTextComponent.getFont());                            
 *   //   editor.setSelectedFontSize(JTextComponent.getFont().getSize());                   
 *   //   editor.setSelectedFontStyle(JTextComponent.getFont().getStyle());                   
 *   //   editor.setSelectedFontFamily(JTextComponent.getFont().getFamily());                  
 *   //   editor.setSampleTextField(JTextComponent.getText());                                
 * 
 *    if (editor.wasOKPressed()) {
 *        Font font = editor.getSelectedFont(); 
 *       System.out.println("Selected Font : " + font);
 *      
 *     // <Optional, get modified text String from preview panel>  
 *     //                                    
 *     //  String string = editor.getSampleTextField().getText() 
 *     //  System.out.println("Selected modified text: "string);
 *       
 *    } else {
 *    reportNothingToUndoYet(null);
 * }
 * 
 * <pre>
 * 
 * 
 * @author Giuseppe Aruta
 *
 */
public class TextEditor extends JComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    // class variables
    /**
     * Return value from <code>showDialog()</code>.
     * 
     * @see #showDialog
     **/
    public static final int OK_OPTION = 0;
    /**
     * Return value from <code>showDialog()</code>.
     * 
     * @see #showDialog
     **/
    public static final int CANCEL_OPTION = 1;
    /**
     * Return value from <code>showDialog()</code>.
     * 
     * @see #showDialog
     **/

    public static final int ERROR_OPTION = -1;
    private static final Font DEFAULT_SELECTED_FONT = new Font("Serif",
            Font.PLAIN, 12);
    private final static JLabel label = new JLabel();
    // private static final Font DEFAULT_FONT = new Font("Dialog", Font.PLAIN,
    // 10);
    private static final Font DEFAULT_FONT = new Font(
            label.getFont().getName(), Font.PLAIN, label.getFont().getSize());
    private static final int[] FONT_STYLE_CODES = { Font.PLAIN, Font.BOLD,
            Font.ITALIC, Font.BOLD | Font.ITALIC };
    private static final String[] DEFAULT_FONT_SIZE_STRINGS = { "8", "9", "10",
            "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36",
            "48", "72", };

    // instance variables
    protected int dialogResultValue = ERROR_OPTION;

    private String[] fontStyleNames = null;
    private String[] fontFamilyNames = null;
    private String[] fontSizeStrings = null;
    private JTextField fontFamilyTextField = null;
    private JTextField fontStyleTextField = null;
    private JTextField fontSizeTextField = null;
    private JList<String> fontNameList = null;
    private JList<String> fontStyleList = null;
    private JList<String> fontSizeList = null;
    private JPanel fontNamePanel = null;
    private JPanel fontStylePanel = null;
    private JPanel fontSizePanel = null;
    private JPanel samplePanel = null;
    private JTextField sampleText = null;

    /**
     * Constructs a <code>JFontChooser</code> object.
     **/
    public TextEditor() {
        this(DEFAULT_FONT_SIZE_STRINGS);
    }

    /**
     * Constructs a <code>JFontChooser</code> object using the given font size
     * array.
     * 
     * @param fontSizeStrings
     *            the array of font size string.
     **/
    public TextEditor(String[] fontSizeStrings) {
        if (fontSizeStrings == null) {
            fontSizeStrings = DEFAULT_FONT_SIZE_STRINGS;
        }
        this.fontSizeStrings = fontSizeStrings;

        final JPanel selectPanel = new JPanel();

        selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.X_AXIS));
        selectPanel.add(getFontFamilyPanel());
        selectPanel.add(getFontStylePanel());
        selectPanel.add(getFontSizePanel());
        final JPanel contentsPanel = new JPanel();
        contentsPanel.setLayout(new GridLayout(2, 1));
        contentsPanel.add(selectPanel, BorderLayout.NORTH);

        contentsPanel.add(getSamplePanel(), BorderLayout.CENTER);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(contentsPanel);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setSelectedFont(DEFAULT_SELECTED_FONT);
    }

    public JTextField getFontFamilyTextField() {
        if (fontFamilyTextField == null) {
            fontFamilyTextField = new JTextField();
            fontFamilyTextField.setEditable(true);
            fontFamilyTextField
                    .addFocusListener(new TextFieldFocusHandlerForTextSelection(
                            fontFamilyTextField));
            fontFamilyTextField
                    .addKeyListener(new TextFieldKeyHandlerForListSelectionUpDown(
                            getFontFamilyList()));
            fontFamilyTextField.getDocument()
                    .addDocumentListener(
                            new ListSearchTextFieldDocumentHandler(
                                    getFontFamilyList()));
            fontFamilyTextField.setFont(DEFAULT_FONT);

        }
        return fontFamilyTextField;
    }

    public JTextField getFontStyleTextField() {
        if (fontStyleTextField == null) {
            fontStyleTextField = new JTextField();
            fontStyleTextField.setEditable(true);
            fontStyleTextField
                    .addFocusListener(new TextFieldFocusHandlerForTextSelection(
                            fontStyleTextField));
            fontStyleTextField
                    .addKeyListener(new TextFieldKeyHandlerForListSelectionUpDown(
                            getFontStyleList()));
            fontStyleTextField.getDocument().addDocumentListener(
                    new ListSearchTextFieldDocumentHandler(getFontStyleList()));
            fontStyleTextField.setFont(DEFAULT_FONT);
        }
        return fontStyleTextField;
    }

    public JTextField getFontSizeTextField() {
        if (fontSizeTextField == null) {
            fontSizeTextField = new JTextField();
            fontSizeTextField
                    .addFocusListener(new TextFieldFocusHandlerForTextSelection(
                            fontSizeTextField));
            fontSizeTextField
                    .addKeyListener(new TextFieldKeyHandlerForListSelectionUpDown(
                            getFontSizeList()));
            fontSizeTextField.getDocument().addDocumentListener(
                    new ListSearchTextFieldDocumentHandler(getFontSizeList()));
            fontSizeTextField.setFont(DEFAULT_FONT);
        }
        return fontSizeTextField;
    }

    public JList<String> getFontFamilyList() {
        if (fontNameList == null) {
            fontNameList = new JList<String>(getFontFamilies());
            fontNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fontNameList.addListSelectionListener(new ListSelectionHandler(
                    getFontFamilyTextField()));
            fontNameList.setSelectedIndex(0);
            fontNameList.setFont(DEFAULT_FONT);
            fontNameList.setFocusable(false);
        }
        return fontNameList;
    }

    public JList<String> getFontStyleList() {
        if (fontStyleList == null) {
            fontStyleList = new JList<String>(getFontStyleNames());
            fontStyleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fontStyleList.addListSelectionListener(new ListSelectionHandler(
                    getFontStyleTextField()));
            fontStyleList.setSelectedIndex(0);
            fontStyleList.setFont(DEFAULT_FONT);
            fontStyleList.setFocusable(false);
        }
        return fontStyleList;
    }

    public JList<String> getFontSizeList() {
        if (fontSizeList == null) {
            fontSizeList = new JList<String>(fontSizeStrings);
            fontSizeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fontSizeList.addListSelectionListener(new ListSelectionHandler(
                    getFontSizeTextField()));
            fontSizeList.setSelectedIndex(0);
            fontSizeList.setFont(DEFAULT_FONT);
            fontSizeList.setFocusable(false);
        }
        return fontSizeList;
    }

    /**
     * Get the family name of the selected font.
     * 
     * @return the font family of the selected font.
     *
     * @see #setSelectedFontFamily
     **/
    public String getSelectedFontFamily() {
        final String fontName = getFontFamilyList().getSelectedValue();
        return fontName;
    }

    /**
     * Get the style of the selected font.
     * 
     * @return the style of the selected font. <code>Font.PLAIN</code>,
     *         <code>Font.BOLD</code>, <code>Font.ITALIC</code>,
     *         <code>Font.BOLD|Font.ITALIC</code>
     *
     * @see java.awt.Font#PLAIN
     * @see java.awt.Font#BOLD
     * @see java.awt.Font#ITALIC
     * @see #setSelectedFontStyle
     **/
    public int getSelectedFontStyle() {
        final int index = getFontStyleList().getSelectedIndex();
        return FONT_STYLE_CODES[index];
    }

    /**
     * Get the size of the selected font.
     * 
     * @return the size of the selected font
     *
     * @see #setSelectedFontSize
     **/
    public int getSelectedFontSize() {
        int fontSize = 1;
        String fontSizeString = getFontSizeTextField().getText();
        while (true) {
            try {
                fontSize = Integer.parseInt(fontSizeString);
                break;
            } catch (final NumberFormatException e) {
                fontSizeString = getFontSizeList().getSelectedValue();
                getFontSizeTextField().setText(fontSizeString);
            }
        }

        return fontSize;
    }

    /**
     * Get the selected font.
     * 
     * @return the selected font
     *
     * @see #setSelectedFont
     * @see java.awt.Font
     **/
    public Font getSelectedFont() {
        final Font font = new Font(getSelectedFontFamily(),
                getSelectedFontStyle(), getSelectedFontSize());
        return font;
    }

    /**
     * Set the family name of the selected font.
     * 
     * @param name
     *            the family name of the selected font.
     *
     * @see getSelectedFontFamily
     **/
    public void setSelectedFontFamily(String name) {
        final String[] names = getFontFamilies();
        for (int i = 0; i < names.length; i++) {
            if (names[i].toLowerCase().equals(name.toLowerCase())) {
                getFontFamilyList().setSelectedIndex(i);
                break;
            }
        }
        updateSampleFont();
    }

    /**
     * Set the style of the selected font.
     * 
     * @param style
     *            the size of the selected font. <code>Font.PLAIN</code>,
     *            <code>Font.BOLD</code>, <code>Font.ITALIC</code>, or
     *            <code>Font.BOLD|Font.ITALIC</code>.
     *
     * @see java.awt.Font#PLAIN
     * @see java.awt.Font#BOLD
     * @see java.awt.Font#ITALIC
     * @see #getSelectedFontStyle
     **/
    public void setSelectedFontStyle(int style) {
        for (int i = 0; i < FONT_STYLE_CODES.length; i++) {
            if (FONT_STYLE_CODES[i] == style) {
                getFontStyleList().setSelectedIndex(i);
                break;
            }
        }
        updateSampleFont();
    }

    public void setSampleTextField(String a) {
        if (sampleText == null) {
            final Border lowered = BorderFactory.createLoweredBevelBorder();

            sampleText = new JTextField(a);
            sampleText.setBorder(lowered);
            sampleText.setPreferredSize(new Dimension(300, 60));
        }
        getSampleTextField().setText(a);
        updateSampleFont();
    }

    /**
     * Set the size of the selected font.
     * 
     * @param size
     *            the size of the selected font
     *
     * @see #getSelectedFontSize
     **/
    public void setSelectedSampleTextField(int size) {
        final String sizeString = String.valueOf(size);
        for (int i = 0; i < fontSizeStrings.length; i++) {
            if (fontSizeStrings[i].equals(sizeString)) {
                getFontSizeList().setSelectedIndex(i);
                break;
            }
        }
        getSampleTextField().setText(sizeString);
        updateSampleFont();
    }

    /**
     * Set the size of the selected font.
     * 
     * @param size
     *            the size of the selected font
     *
     * @see #getSelectedFontSize
     **/
    public void setSelectedFontSize(int size) {
        final String sizeString = String.valueOf(size);
        for (int i = 0; i < fontSizeStrings.length; i++) {
            if (fontSizeStrings[i].equals(sizeString)) {
                getFontSizeList().setSelectedIndex(i);
                break;
            }
        }
        getFontSizeTextField().setText(sizeString);
        updateSampleFont();
    }

    /**
     * Set the selected font.
     * 
     * @param font
     *            the selected font
     *
     * @see #getSelectedFont
     * @see java.awt.Font
     **/
    public void setSelectedFont(Font font) {
        setSelectedFontFamily(font.getFamily());
        setSelectedFontStyle(font.getStyle());
        setSelectedFontSize(font.getSize());
    }

    public String getVersionString() {
        return ("Version");
    }

    final protected OKCancelApplyPanel okCancelApplyPanel = new OKCancelApplyPanel();

    /**
     * Show font selection dialog.
     * 
     * @param parent
     *            Dialog's Parent component.
     * @return OK_OPTION, CANCEL_OPTION or ERROR_OPTION
     *
     * @see #OK_OPTION
     * @see #CANCEL_OPTION
     * @see #ERROR_OPTION
     **/
    public int showDialog(Component parent, String title) {
        dialogResultValue = ERROR_OPTION;
        final JDialog dialog = createDialog(parent, title);
        dialog.setIconImage(IconLoader.image("oj_16_Kplain2oj.png"));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialogResultValue = CANCEL_OPTION;
            }
        });
        okCancelApplyPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (okCancelApplyPanel.wasApplyPressed()) {
                    return;
                } else {
                    dialog.dispose();
                    return;
                }
            }
        });
        dialog.setVisible(true);
        dialog.dispose();
        return dialogResultValue;
    }

    public void addOKCancelApplyPanelActionListener(
            ActionListener actionListener) {
        okCancelApplyPanel.addActionListener(actionListener);
    }

    void okCancelApplyPanel_actionPerformed(ActionEvent e) {
        if (okCancelApplyPanel.wasApplyPressed()) {
            return;
        } else if (!okCancelApplyPanel.wasOKPressed()) {
            setVisible(false);
            return;
        }

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

    protected class ListSelectionHandler implements ListSelectionListener {
        private final JTextComponent textComponent;

        ListSelectionHandler(JTextComponent textComponent) {
            this.textComponent = textComponent;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting() == false) {
                final JList list = (JList) e.getSource();
                final String selectedValue = (String) list.getSelectedValue();

                final String oldValue = textComponent.getText();
                textComponent.setText(selectedValue);
                if (!oldValue.equalsIgnoreCase(selectedValue)) {
                    textComponent.selectAll();
                    textComponent.requestFocus();
                }

                updateSampleFont();
            }
        }
    }

    protected class TextFieldFocusHandlerForTextSelection extends FocusAdapter {
        private final JTextComponent textComponent;

        public TextFieldFocusHandlerForTextSelection(
                JTextComponent textComponent) {
            this.textComponent = textComponent;
        }

        @Override
        public void focusGained(FocusEvent e) {
            textComponent.selectAll();
        }

        @Override
        public void focusLost(FocusEvent e) {
            textComponent.select(0, 0);
            updateSampleFont();
        }
    }

    protected class TextFieldKeyHandlerForListSelectionUpDown extends
            KeyAdapter {
        @SuppressWarnings("rawtypes")
        private final JList targetList;

        public TextFieldKeyHandlerForListSelectionUpDown(
                @SuppressWarnings("rawtypes") JList list) {
            targetList = list;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int i = targetList.getSelectedIndex();
            switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                i = targetList.getSelectedIndex() - 1;
                if (i < 0) {
                    i = 0;
                }
                targetList.setSelectedIndex(i);
                break;
            case KeyEvent.VK_DOWN:
                final int listSize = targetList.getModel().getSize();
                i = targetList.getSelectedIndex() + 1;
                if (i >= listSize) {
                    i = listSize - 1;
                }
                targetList.setSelectedIndex(i);
                break;
            default:
                break;
            }
        }
    }

    protected class ListSearchTextFieldDocumentHandler implements
            DocumentListener {
        @SuppressWarnings("rawtypes")
        JList targetList;

        public ListSearchTextFieldDocumentHandler(
                @SuppressWarnings("rawtypes") JList targetList) {
            this.targetList = targetList;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update(e);
        }

        private void update(DocumentEvent event) {
            String newValue = "";
            try {
                final Document doc = event.getDocument();
                newValue = doc.getText(0, doc.getLength());
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }

            if (newValue.length() > 0) {
                int index = targetList.getNextMatch(newValue, 0,
                        Position.Bias.Forward);
                if (index < 0) {
                    index = 0;
                }
                targetList.ensureIndexIsVisible(index);

                final String matchedName = targetList.getModel()
                        .getElementAt(index).toString();
                if (newValue.equalsIgnoreCase(matchedName)) {
                    if (index != targetList.getSelectedIndex()) {
                        SwingUtilities.invokeLater(new ListSelector(index));
                    }
                }
            }
        }

        public class ListSelector implements Runnable {
            private final int index;

            public ListSelector(int index) {
                this.index = index;
            }

            @Override
            public void run() {
                targetList.setSelectedIndex(index);
            }
        }
    }

    protected JDialog createDialog(Component parent, String title) {
        final Frame frame = parent instanceof Frame ? (Frame) parent
                : (Frame) SwingUtilities
                        .getAncestorOfClass(Frame.class, parent);
        final JDialog dialog = new JDialog(frame, title, true);

        dialog.getContentPane().add(this, BorderLayout.CENTER);

        dialog.getContentPane().add(okCancelApplyPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        return dialog;
    }

    protected void updateSampleFont() {
        final Font font = getSelectedFont();
        getSampleTextField().setFont(font);
    }

    protected JPanel getFontFamilyPanel() {
        if (fontNamePanel == null) {
            fontNamePanel = new JPanel();
            fontNamePanel.setLayout(new BorderLayout());
            fontNamePanel
                    .setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            fontNamePanel.setPreferredSize(new Dimension(180, 130));

            final JScrollPane scrollPane = new JScrollPane(getFontFamilyList());

            scrollPane.getVerticalScrollBar().setFocusable(false);
            scrollPane
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(getFontFamilyTextField(), BorderLayout.NORTH);
            p.add(scrollPane, BorderLayout.CENTER);

            final JLabel label = new JLabel(I18N.get("ui.FontChooser.font"));
            label.setHorizontalAlignment(JLabel.LEFT);
            label.setHorizontalTextPosition(JLabel.LEFT);
            label.setLabelFor(getFontFamilyTextField());
            label.setDisplayedMnemonic('F');
            fontNamePanel.setBorder(new TitledBorder(new EtchedBorder(), " "
                    + I18N.get("ui.FontChooser.font") + " "));
            fontNamePanel.add(p, BorderLayout.CENTER);

        }
        return fontNamePanel;
    }

    protected JPanel getFontStylePanel() {
        if (fontStylePanel == null) {
            fontStylePanel = new JPanel();
            fontStylePanel.setLayout(new BorderLayout());
            fontStylePanel.setBorder(BorderFactory
                    .createEmptyBorder(5, 5, 5, 5));
            fontStylePanel.setPreferredSize(new Dimension(140, 130));

            final JScrollPane scrollPane = new JScrollPane(getFontStyleList());
            scrollPane.getVerticalScrollBar().setFocusable(false);
            scrollPane
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(getFontStyleTextField(), BorderLayout.NORTH);
            p.add(scrollPane, BorderLayout.CENTER);

            final JLabel label = new JLabel(I18N.get("ui.FontChooser.style"));
            label.setHorizontalAlignment(JLabel.LEFT);
            label.setHorizontalTextPosition(JLabel.LEFT);
            label.setLabelFor(getFontStyleTextField());
            label.setDisplayedMnemonic('Y');
            fontStylePanel.setBorder(new TitledBorder(new EtchedBorder(), " "
                    + I18N.get("ui.FontChooser.style") + " "));

            fontStylePanel.add(p, BorderLayout.CENTER);
        }
        return fontStylePanel;
    }

    protected JPanel getFontSizePanel() {
        if (fontSizePanel == null) {
            fontSizePanel = new JPanel();
            fontSizePanel.setLayout(new BorderLayout());
            fontSizePanel.setPreferredSize(new Dimension(120, 130));
            fontSizePanel
                    .setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            final JScrollPane scrollPane = new JScrollPane(getFontSizeList());
            scrollPane.getVerticalScrollBar().setFocusable(false);
            scrollPane
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(getFontSizeTextField(), BorderLayout.NORTH);
            p.add(scrollPane, BorderLayout.CENTER);

            final JLabel label = new JLabel(
                    I18N.get("ui.style.LabelStylePanel.height"));
            label.setHorizontalAlignment(JLabel.LEFT);
            label.setHorizontalTextPosition(JLabel.LEFT);
            label.setLabelFor(getFontSizeTextField());
            label.setDisplayedMnemonic('S');
            fontSizePanel.setBorder(new TitledBorder(new EtchedBorder(), " "
                    + I18N.get("ui.FontChooser.size") + " "));
            fontSizePanel.add(p, BorderLayout.CENTER);
        }
        return fontSizePanel;
    }

    protected JPanel getSamplePanel() {
        if (samplePanel == null) {
            samplePanel = new JPanel();
            samplePanel.setLayout(new BorderLayout());
            samplePanel
                    .setBorder(new TitledBorder(
                            new EtchedBorder(),
                            " "
                                    + I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.string")
                                    + " "));
            samplePanel.add(getSampleTextField(), BorderLayout.CENTER);
        }
        return samplePanel;
    }

    public JTextField getSampleTextField() {
        if (sampleText == null) {
            final Border lowered = BorderFactory.createLoweredBevelBorder();

            sampleText = new JTextField(I18N.get("ui.FontChooser.sampletext"));
            sampleText.setBorder(lowered);
            sampleText.setPreferredSize(new Dimension(300, 40));
        }
        return sampleText;
    }

    protected String[] getFontFamilies() {
        if (fontFamilyNames == null) {
            final GraphicsEnvironment env = GraphicsEnvironment
                    .getLocalGraphicsEnvironment();
            fontFamilyNames = env.getAvailableFontFamilyNames();
        }
        return fontFamilyNames;
    }

    protected String[] getFontStyleNames() {
        if (fontStyleNames == null) {
            int i = 0;
            fontStyleNames = new String[4];
            fontStyleNames[i++] = ("Plain");
            fontStyleNames[i++] = ("Bold");
            fontStyleNames[i++] = ("Italic");
            fontStyleNames[i++] = ("BoldItalic");
        }
        return fontStyleNames;
    }

}
