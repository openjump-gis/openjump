/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import javax.swing.text.Utilities;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.deegree.enterprise.WebUtils;
import org.deegree.ogcwebservices.wfs.operation.GetFeature;

import com.vividsolutions.jump.workbench.JUMPWorkbench;

import de.latlon.deejump.wfs.client.WFSClientHelper;
import de.latlon.deejump.wfs.client.WFSHttpClient;
import de.latlon.deejump.wfs.client.WFSPostMethod;
import de.latlon.deejump.wfs.deegree2mods.XMLFragment;
import de.latlon.deejump.wfs.i18n.I18N;

/**
 * Shows the GetFeature requests.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1516 $, $Date: 2008-07-17 11:32:16 +0200 (Do, 17 Jul
 *          2008) $
 */
class RequestPanel extends JPanel {

  private static final long serialVersionUID = 8173462624638666293L;

  final WFSPanel wfsPanel;

  JTextArea requestTextArea;

  private JButton createReqButton, responseButton;

  private JButton validateReq;

  RequestPanel(WFSPanel wfsPanel) {
    this.wfsPanel = wfsPanel;

    createRequestButton();

    setLayout(new GridBagLayout());

    requestTextArea = new JTextArea();
    requestTextArea.setLineWrap(true);
    requestTextArea.setWrapStyleWord(true);
    requestTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    TextLineNumber tln = new TextLineNumber(requestTextArea);
    JScrollPane jsp = new JScrollPane(requestTextArea);
    jsp.setRowHeaderView(tln);

    add(jsp, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
            0), 0, 0));

    JPanel buttonPanel = new JPanel(new GridBagLayout());
    buttonPanel.add(createReqButton, new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
            0, 0, 0), 0, 0));
    buttonPanel.add(createValidationButton(), new GridBagConstraints(1, 0, 1,
        1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    buttonPanel.add(createResponseButton(), new GridBagConstraints(2, 0, 1, 1,
        1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));

    add(buttonPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
            0), 0, 0));
  }

  private JComponent createValidationButton() {
    validateReq = new JButton(I18N.get("FeatureResearchDialog.validateRequest"));

    validateReq.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        String reqTxt = requestTextArea.getText();
        if (reqTxt == null || reqTxt.length() == 0) {
          return;
        }
        try {

          // simple test for well-formedness
          XMLFragment xf = new de.latlon.deejump.wfs.deegree2mods.XMLFragment();
          xf.load(new StringReader(reqTxt), "http://empty");

          if ("1.1.0".equals(wfsPanel.wfService.getServiceVersion())) {
            // use deegree to validate request
            GetFeature.create(null, xf.getRootElement());
          }
        } catch (Exception ex) {
          // ex.printStackTrace();
          // JOptionPane.showMessageDialog( wfsPanel, ex.getMessage(), I18N.get(
          // "error" ),
          // JOptionPane.ERROR_MESSAGE );
          JUMPWorkbench.getInstance().getContext().getErrorHandler()
              .handleThrowable(ex);
        }
      }
    });
    return validateReq;
  }

  private JComponent createResponseButton() {
    responseButton = new JButton(I18N.get("FeatureResearchDialog.response"));
    responseButton.setEnabled(false);

    responseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        final String reqTxt = requestTextArea.getText();
        if (reqTxt == null || reqTxt.length() == 0) {
          return;
        }

        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            // simply read resonse into the appropriate tab
            try {
              HttpClient client = new WFSHttpClient();
              String wfsUrl = wfsPanel.wfService.getGetFeatureURL();
              PostMethod post = new WFSPostMethod(wfsUrl);
              post.setRequestEntity(new StringRequestEntity(reqTxt, "text/xml",
                  "UTF-8"));

              WebUtils.enableProxyUsage(client, new URL(wfsUrl));
              int code = client.executeMethod(post);

              // detect xml encoding
              PushbackInputStream pbis = new PushbackInputStream(post
                  .getResponseBodyAsStream(), 1024);
              String encoding = WFSClientHelper.readEncoding(pbis);
              InputStreamReader isrd = new InputStreamReader(pbis, encoding);

              BufferedReader in = new BufferedReader(isrd);
              StringBuffer body = new StringBuffer();
              String readLine;
              while ((readLine = in.readLine()) != null) {
                body.append(readLine + "\n");
              }

              wfsPanel.setResponseText(Arrays.toString(post.getRequestHeaders())
                  + "\n\n"
                  + Arrays.toString(post.getResponseHeaders())
                  + "\n\n" + body);
            } catch (Exception ex) {
              JUMPWorkbench.getInstance().getFrame().handleThrowable(ex);
            }
          }
        });

      }
    });
    return responseButton;
  }

  private JComponent createRequestButton() {
    createReqButton = new JButton(
        I18N.get("FeatureResearchDialog.createWFSRequest"));
    createReqButton.setBounds(260, 20, 80, 20);
    createReqButton.setAlignmentX(0.5f);
    createReqButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setRequestText(wfsPanel.createRequest());
        requestTextArea.setCaretPosition(0);
        responseButton.setEnabled(true);
      }
    });
    return createReqButton;
  }

  void setRequestText(String txt) {
    this.requestTextArea.setText(txt.replaceAll(">", ">\n"));
  }

  String getText() {
    return this.requestTextArea.getText();
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    this.createReqButton.setEnabled(enabled);
    this.validateReq.setEnabled(enabled);
    this.requestTextArea.setEnabled(enabled);
  }

}

/**
 * This class will display line numbers for a related text component. The text
 * component must use the same line height for each line. TextLineNumber
 * supports wrapped lines and will highlight the line number of the current line
 * in the text component.
 *
 * This class was designed to be used as a component added to the row header of
 * a JScrollPane.
 */
class TextLineNumber extends JPanel implements CaretListener, DocumentListener,
    PropertyChangeListener {
  public final static float LEFT = 0.0f;
  public final static float CENTER = 0.5f;
  public final static float RIGHT = 1.0f;

  private final static Border OUTER = new MatteBorder(0, 0, 0, 2, Color.GRAY);

  private final static int HEIGHT = Integer.MAX_VALUE - 1000000;

  // Text component this TextTextLineNumber component is in sync with

  private JTextComponent component;

  // Properties that can be changed

  private boolean updateFont;
  private int borderGap;
  private Color currentLineForeground;
  private float digitAlignment;
  private int minimumDisplayDigits;

  // Keep history information to reduce the number of times the component
  // needs to be repainted

  private int lastDigits;
  private int lastHeight;
  private int lastLine;

  private HashMap<String, FontMetrics> fonts;

  /**
   * Create a line number component for a text component. This minimum display
   * width will be based on 3 digits.
   *
   * @param component
   *          the related text component
   */
  public TextLineNumber(JTextComponent component) {
    this(component, 3);
  }

  /**
   * Create a line number component for a text component.
   *
   * @param component
   *          the related text component
   * @param minimumDisplayDigits
   *          the number of digits used to calculate the minimum width of the
   *          component
   */
  public TextLineNumber(JTextComponent component, int minimumDisplayDigits) {
    this.component = component;

    setFont(component.getFont());

    setBorderGap(5);
    setCurrentLineForeground(Color.RED);
    setDigitAlignment(RIGHT);
    setMinimumDisplayDigits(minimumDisplayDigits);

    component.getDocument().addDocumentListener(this);
    component.addCaretListener(this);
    component.addPropertyChangeListener("font", this);
  }

  /**
   * Gets the update font property
   *
   * @return the update font property
   */
  public boolean getUpdateFont() {
    return updateFont;
  }

  /**
   * Set the update font property. Indicates whether this Font should be updated
   * automatically when the Font of the related text component is changed.
   *
   * @param updateFont
   *          when true update the Font and repaint the line numbers, otherwise
   *          just repaint the line numbers.
   */
  public void setUpdateFont(boolean updateFont) {
    this.updateFont = updateFont;
  }

  /**
   * Gets the border gap
   *
   * @return the border gap in pixels
   */
  public int getBorderGap() {
    return borderGap;
  }

  /**
   * The border gap is used in calculating the left and right insets of the
   * border. Default value is 5.
   *
   * @param borderGap
   *          the gap in pixels
   */
  public void setBorderGap(int borderGap) {
    this.borderGap = borderGap;
    Border inner = new EmptyBorder(0, borderGap, 0, borderGap);
    setBorder(new CompoundBorder(OUTER, inner));
    lastDigits = 0;
    setPreferredWidth();
  }

  /**
   * Gets the current line rendering Color
   *
   * @return the Color used to render the current line number
   */
  public Color getCurrentLineForeground() {
    return currentLineForeground == null ? getForeground()
        : currentLineForeground;
  }

  /**
   * The Color used to render the current line digits. Default is Coolor.RED.
   *
   * @param currentLineForeground
   *          the Color used to render the current line
   */
  public void setCurrentLineForeground(Color currentLineForeground) {
    this.currentLineForeground = currentLineForeground;
  }

  /**
   * Gets the digit alignment
   *
   * @return the alignment of the painted digits
   */
  public float getDigitAlignment() {
    return digitAlignment;
  }

  /**
   * Specify the horizontal alignment of the digits within the component. Common
   * values would be:
   * <ul>
   * <li>TextLineNumber.LEFT
   * <li>TextLineNumber.CENTER
   * <li>TextLineNumber.RIGHT (default)
   * </ul>
   * 
   * @param currentLineForeground
   *          the Color used to render the current line
   */
  public void setDigitAlignment(float digitAlignment) {
    this.digitAlignment = digitAlignment > 1.0f ? 1.0f
        : digitAlignment < 0.0f ? -1.0f : digitAlignment;
  }

  /**
   * Gets the minimum display digits
   *
   * @return the minimum display digits
   */
  public int getMinimumDisplayDigits() {
    return minimumDisplayDigits;
  }

  /**
   * Specify the mimimum number of digits used to calculate the preferred width
   * of the component. Default is 3.
   *
   * @param minimumDisplayDigits
   *          the number digits used in the preferred width calculation
   */
  public void setMinimumDisplayDigits(int minimumDisplayDigits) {
    this.minimumDisplayDigits = minimumDisplayDigits;
    setPreferredWidth();
  }

  /**
   * Calculate the width needed to display the maximum line number
   */
  private void setPreferredWidth() {
    Element root = component.getDocument().getDefaultRootElement();
    int lines = root.getElementCount();
    int digits = Math.max(String.valueOf(lines).length(), minimumDisplayDigits);

    // Update sizes when number of digits in the line number changes

    if (lastDigits != digits) {
      lastDigits = digits;
      FontMetrics fontMetrics = getFontMetrics(getFont());
      int width = fontMetrics.charWidth('0') * digits;
      Insets insets = getInsets();
      int preferredWidth = insets.left + insets.right + width;

      Dimension d = getPreferredSize();
      d.setSize(preferredWidth, HEIGHT);
      setPreferredSize(d);
      setSize(d);
    }
  }

  /**
   * Draw the line numbers, courtesy of
   * https://tips4java.wordpress.com/2009/05/23/text-component-line-number/
   */
  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Determine the width of the space available to draw the line number

    FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
    Insets insets = getInsets();
    int availableWidth = getSize().width - insets.left - insets.right;

    // Determine the rows to draw within the clipped bounds.

    Rectangle clip = g.getClipBounds();
    int rowStartOffset = component.viewToModel(new Point(0, clip.y));
    int endOffset = component.viewToModel(new Point(0, clip.y + clip.height));

    while (rowStartOffset <= endOffset) {
      try {
        if (isCurrentLine(rowStartOffset))
          g.setColor(getCurrentLineForeground());
        else
          g.setColor(getForeground());

        // Get the line number as a string and then determine the
        // "X" and "Y" offsets for drawing the string.

        String lineNumber = getTextLineNumber(rowStartOffset);
        int stringWidth = fontMetrics.stringWidth(lineNumber);
        int x = getOffsetX(availableWidth, stringWidth) + insets.left;
        int y = getOffsetY(rowStartOffset, fontMetrics);
        g.drawString(lineNumber, x, y);

        // Move to the next row

        rowStartOffset = Utilities.getRowEnd(component, rowStartOffset) + 1;
      } catch (Exception e) {
        break;
      }
    }
  }

  /*
   * We need to know if the caret is currently positioned on the line we are
   * about to paint so the line number can be highlighted.
   */
  private boolean isCurrentLine(int rowStartOffset) {
    int caretPosition = component.getCaretPosition();
    Element root = component.getDocument().getDefaultRootElement();

    if (root.getElementIndex(rowStartOffset) == root
        .getElementIndex(caretPosition))
      return true;
    else
      return false;
  }

  /*
   * Get the line number to be drawn. The empty string will be returned when a
   * line of text has wrapped.
   */
  protected String getTextLineNumber(int rowStartOffset) {
    Element root = component.getDocument().getDefaultRootElement();
    int index = root.getElementIndex(rowStartOffset);
    Element line = root.getElement(index);

    if (line.getStartOffset() == rowStartOffset)
      return String.valueOf(index + 1);
    else
      return "";
  }

  /*
   * Determine the X offset to properly align the line number when drawn
   */
  private int getOffsetX(int availableWidth, int stringWidth) {
    return (int) ((availableWidth - stringWidth) * digitAlignment);
  }

  /*
   * Determine the Y offset for the current row
   */
  private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics)
      throws BadLocationException {
    // Get the bounding rectangle of the row

    Rectangle r = component.modelToView(rowStartOffset);
    int lineHeight = fontMetrics.getHeight();
    int y = r.y + r.height;
    int descent = 0;

    // The text needs to be positioned above the bottom of the bounding
    // rectangle based on the descent of the font(s) contained on the row.

    if (r.height == lineHeight) // default font is being used
    {
      descent = fontMetrics.getDescent();
    } else // We need to check all the attributes for font changes
    {
      if (fonts == null)
        fonts = new HashMap<String, FontMetrics>();

      Element root = component.getDocument().getDefaultRootElement();
      int index = root.getElementIndex(rowStartOffset);
      Element line = root.getElement(index);

      for (int i = 0; i < line.getElementCount(); i++) {
        Element child = line.getElement(i);
        AttributeSet as = child.getAttributes();
        String fontFamily = (String) as.getAttribute(StyleConstants.FontFamily);
        Integer fontSize = (Integer) as.getAttribute(StyleConstants.FontSize);
        String key = fontFamily + fontSize;

        FontMetrics fm = fonts.get(key);

        if (fm == null) {
          Font font = new Font(fontFamily, Font.PLAIN, fontSize);
          fm = component.getFontMetrics(font);
          fonts.put(key, fm);
        }

        descent = Math.max(descent, fm.getDescent());
      }
    }

    return y - descent;
  }

  //
  // Implement CaretListener interface
  //
  @Override
  public void caretUpdate(CaretEvent e) {
    // Get the line the caret is positioned on

    int caretPosition = component.getCaretPosition();
    Element root = component.getDocument().getDefaultRootElement();
    int currentLine = root.getElementIndex(caretPosition);

    // Need to repaint so the correct line number can be highlighted

    if (lastLine != currentLine) {
      repaint();
      lastLine = currentLine;
    }
  }

  //
  // Implement DocumentListener interface
  //
  @Override
  public void changedUpdate(DocumentEvent e) {
    documentChanged();
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    documentChanged();
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    documentChanged();
  }

  /*
   * A document change may affect the number of displayed lines of text.
   * Therefore the lines numbers will also change.
   */
  private void documentChanged() {
    // View of the component has not been updated at the time
    // the DocumentEvent is fired

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          int endPos = component.getDocument().getLength();
          Rectangle rect = component.modelToView(endPos);

          if (rect != null && rect.y != lastHeight) {
            setPreferredWidth();
            repaint();
            lastHeight = rect.y;
          }
        } catch (BadLocationException ex) { /* nothing to do */
        }
      }
    });
  }

  //
  // Implement PropertyChangeListener interface
  //
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getNewValue() instanceof Font) {
      if (updateFont) {
        Font newFont = (Font) evt.getNewValue();
        setFont(newFont);
        lastDigits = 0;
        setPreferredWidth();
      } else {
        repaint();
      }
    }
  }
}
