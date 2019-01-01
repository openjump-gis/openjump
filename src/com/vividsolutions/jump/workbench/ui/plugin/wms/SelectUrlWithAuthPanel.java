package com.vividsolutions.jump.workbench.ui.plugin.wms;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.JTextComponent;

import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;

public class SelectUrlWithAuthPanel extends JPanel {

  private JLabel urlLabel, userLabel, passLabel;

  private JComboBox urls;
  private JTextField user;
  private JPasswordField pass;
  private JTextComponent url;

  private String[] initialUrls;

  public SelectUrlWithAuthPanel(String[] initialUrls) {
    super();
    this.initialUrls = initialUrls;
    createUrlPanel();
  }

  public JPanel createUrlPanel() {
    urlLabel = new JLabel();
    urlLabel.setText(I18N.get("ui.GenericNames.url"));
    userLabel = new JLabel();
    userLabel.setText(I18N.get("ui.GenericNames.user"));
    passLabel = new JLabel();
    passLabel.setText(I18N.get("ui.GenericNames.password"));
    JLabel showLabel = new JLabel();
    showLabel.setText(I18N.get("ui.GenericNames.show"));

    user = new JTextField();
    pass = new JPasswordField();
    final char echoChar = pass.getEchoChar();
    final JCheckBox show = new JCheckBox();
    show.setBorder(null);
    show.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        char c = show.isSelected() ? 0 : echoChar;
        pass.setEchoChar(c);
      }
    });

    this.setLayout(new GridBagLayout());

    urls = new JComboBox(initialUrls) {

      // height seems to be miscalculated in very narrow spaces
      private int limitH(int h) {
        return Math.min(h, 50);
      }

      @Override
      public Dimension getMinimumSize() {
        int w = super.getMinimumSize().width;
        int h = this.getEditor().getEditorComponent().getMinimumSize().height;
        Dimension dim = new Dimension(w, limitH(h));
        return dim;
      }

      @Override
      public Dimension getPreferredSize() {
        int w = super.getPreferredSize().width;
        int h = this.getEditor().getEditorComponent().getPreferredSize().height;
        Dimension pref = new Dimension(w, limitH(h));
        return pref;
      }

    };
    urls.setEditable(true);
    urls.getEditor().selectAll();

    // do not show password in dropdown list
    final ListCellRenderer rendi = urls.getRenderer();
    urls.setRenderer(new ListCellRenderer() {
      public Component getListCellRendererComponent(JList list, Object value,
          int index, boolean isSelected, boolean cellHasFocus) {
        Component c = rendi.getListCellRendererComponent(list, value, index,
            isSelected, cellHasFocus);
        if (c instanceof JLabel) {
          String url = ((JLabel) c).getText();
          ((JLabel) c).setText(UriUtil.urlStripPassword(url));
        }
        return c;
      }
    });

    URLComboBoxEditor ed = new URLComboBoxEditor(user, pass);
    urls.setEditor(ed);
    // for direct interaction with the url textarea
    this.url = (JTextComponent) ed.getEditorComponent();

    Insets insets = new Insets(3, 3, 3, 3);
    add(userLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    add(user, new GridBagConstraints(1, 0, 1, 1, 0, 0,
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));

    add(passLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    add(pass, new GridBagConstraints(1, 1, 1, 1, 1, 0,
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));

    add(show, new GridBagConstraints(2, 1, 1, 1, 0, 0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
    add(showLabel, new GridBagConstraints(3, 1, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insets, 0, 0));

    add(urlLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    add(urls, new GridBagConstraints(1, 2, 4, 1, 0, 0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));

    return this;
  }

  public String getUser() {
    return user.getText();
  }

  public String getPass() {
    return pass.getText();
  }

  public void setUrl(final String url) {
    urls.getEditor().setItem(url);
  }

  public String getUrl() {
    return url.getText();
  }

  public List<String> getUrlsList() {
    List list = new ArrayList<String>();
    for (int i = 0; i < urls.getItemCount(); ++i) {
      if (i != urls.getSelectedIndex()) {
        list.add(urls.getItemAt(i).toString());
      }
    }
    return list;
  }

  public void setUrlsList(String[] urls) {
    this.urls.setModel(new JComboBox(urls).getModel());
  }

  // we keep the full url internally but show only the cleaned out info
  class FilteredURLString {
    private String url = "";

    public FilteredURLString(String url) {
      this.url = url;
    }

    public String filtered() {
      return UriUtil.urlStripAuth(url);
    }

    public String unFiltered() {
      return url;
    }

    public String toString() {
      return filtered();
    }

    public boolean equals(Object obj) {
      return url.equals(obj);
    }
  }

  class TextAreaComboBoxEditor implements ComboBoxEditor {
    final protected JTextArea editor;
    protected Object item = null;

    public TextAreaComboBoxEditor() {
      // create an editable textfield blueprint
      JTextField dummy = new JTextField();
      dummy.setEnabled(true);
      dummy.setEditable(true);
      // now mimick editable textfield look
      final JTextArea area = new JTextArea();
      area.setLineWrap(true);
      area.setFont(dummy.getFont());
      area.setForeground(dummy.getForeground());
      area.setBackground(dummy.getBackground());
      Insets insets = dummy.getBorder().getBorderInsets(dummy);
      area.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left,
          insets.bottom, insets.right));

      this.editor = area;
    }

    public void addActionListener(ActionListener l) {
    }

    public Component getEditorComponent() {
      return editor;
    }

    public Object getItem() {
      return item;
    }

    public void removeActionListener(ActionListener l) {
    }

    public void selectAll() {
    }

    public void setItem(Object newValue) {
      item = newValue;
      this.editor.setText(item.toString());
    }
  }

  class URLComboBoxEditor extends TextAreaComboBoxEditor {

    private JTextField user;
    private JTextField pass;
    private FilteredURLString item;

    public URLComboBoxEditor(final JTextField user, final JTextField pass) {
      super();
      this.user = user;
      this.pass = pass;
      final JTextComponent editor = ((JTextComponent) this.getEditorComponent());
      editor.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
          // fetchAuthIntoFields(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
          fetchAuthIntoFields(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          fetchAuthIntoFields(e);
        }

        boolean enabled = true;

        private void fetchAuthIntoFields(final DocumentEvent e) {
          if (!enabled)
            return;

          // we don't want to listen to the events we trigger ourselfs
          enabled = false;
          // save editor content for later
          final String edText = editor.getText();

          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              FilteredURLString url = new FilteredURLString(edText);
              String text = url.toString();
              // remove auth in textarea, only if there is auth in entered url
              if (!text.equals(url.unFiltered())) {
                int posdiff = url.unFiltered().length() - text.length();
                int curpos = editor.getCaretPosition();
                int maxpos = text.length();
                editor.setText(text);
                editor.setCaretPosition(curpos > maxpos ? maxpos : curpos
                    - posdiff);
              }
              // add auth, if set, to appropriate fields in ui
              String userText = UriUtil.urlGetUser(url.unFiltered());
              if (!userText.isEmpty())
                user.setText(userText);
              String passText = UriUtil.urlGetPassword(url.unFiltered());
              if (!passText.isEmpty())
                pass.setText(passText);

              enabled = true;
            }
          });
        }
      });
    }

    @Override
    public void setItem(Object anObject) {
      final FilteredURLString url = (anObject instanceof FilteredURLString) ? (FilteredURLString) anObject
          : new FilteredURLString(anObject.toString());

      this.item = url;
      final JTextComponent editor = ((JTextComponent) this.getEditorComponent());
      // reset auth fields, content will be set from url pasted into editor
      user.setText("");
      pass.setText("");
      editor.setText(url.unFiltered());
    }

    @Override
    public Object getItem() {
      FilteredURLString item = new FilteredURLString(UriUtil.urlAddCredentials(
          editor.getText(), user.getText(), pass.getText()));
      return item;
    }
  }
}
