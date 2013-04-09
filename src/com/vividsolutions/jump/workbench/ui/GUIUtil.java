/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

//<<TODO:NAMING>> Perhaps rename to WorkbenchUtilities and move to workbench
// package? [Jon Aquino]
public class GUIUtil {

    public final static String dbf = "dbf";

    public final static String dbfDesc = "DBF";

    public final static String fme = "fme";

    public final static String fmeDesc = "FME GML";

    public final static String gml = "gml";

    public final static String gmlDesc = "GML";

    //<<TODO:REFACTORING>> If these constants are only used by descendants of
    //AbstractDriver, they should be moved to AbstractDriver. GUIUtilities is
    //supposed to be very generic. [Jon Aquino]
    public final static String jml = "jml";

    public final static String jmlDesc = "JCS GML";

    public final static String shp = "shp";

    //<<TODO:NAMING>> "ESRI Shapefile" would be more precise. Is this what
    // they
    //are? [Jon Aquino]
    public final static String shpDesc = "ESRI Shapefile";

    public final static String shx = "shx";

    public final static String shxDesc = "SHX";

    public final static String wkt = "wkt";

    public final static String wktDesc = "Well Known Text";

    public final static String wktaDesc = "Well Known Text (Show Attribute)";

    public final static String xml = "xml";

    public final static String xmlDesc = "XML";

    public static final FileFilter ALL_FILES_FILTER = new FileFilter() {

        public boolean accept(File f) {
            return true;
        }

        public String getDescription() {
            return "All Files";
        }
    };

    public GUIUtil() {
    }

    /**
     * Convenience method by default escaping everything.
     * 
     * @param value
     * @return string or null
     */
    public final static String escapeHTML(String value){
      return escapeHTML( value, true, true );
    }

    
    /**
     * Returns a string suitable for embedding as HTML. That is, all characters
     * which have a special meaning in HTML are escaped as character codes.
     * 
     * <p>
     * Based on code from Jason Sherman. See
     * http://www.w3schools.com/html/html_asciiref.asp
     * </p>
     */
    public final static String escapeHTML(String value, boolean escapeSpaces,
            boolean escapeNewlines) {
        if (value == null) {
            return (null);
        }

        char[] content = new char[value.length()];
        value.getChars(0, value.length(), content, 0);

        StringBuffer result = new StringBuffer();

        for (int i = 0; i < content.length; i++) {
            switch (content[i]) {
            case ' ':
                result.append(escapeSpaces ? "&#32;" : " ");

                break;

            //Added \n [Jon Aquino]
            case '\n':
                result.append(escapeNewlines ? "<BR>" : "\n");

                break;

            case '!':
                result.append("&#33;");

                break;

            case '"':
                result.append("&#34;");

                break;

            case '#':
                result.append("&#35;");

                break;

            case '$':
                result.append("&#36;");

                break;

            case '%':
                result.append("&#37;");

                break;

            case '&':
                result.append("&#38;");

                break;

            case '\'':
                result.append("&#39;");

                break;

            case '(':
                result.append("&#40;");

                break;

            case ')':
                result.append("&#41;");

                break;

            case '*':
                result.append("&#42;");

                break;

            case '+':
                result.append("&#43;");

                break;

            case ',':
                result.append("&#44;");

                break;

            case '-':
                result.append("&#45;");

                break;

            case '.':
                result.append("&#46;");

                break;

            case '/':
                result.append("&#47;");

                break;

            case ':':
                result.append("&#58;");

                break;

            case ';':
                result.append("&#59;");

                break;

            case '<':
                result.append("&#60;");

                break;

            case '=':
                result.append("&#61;");

                break;

            case '>':
                result.append("&#62;");

                break;

            case '?':
                result.append("&#63;");

                break;

            case '@':
                result.append("&#64;");

                break;

            case '[':
                result.append("&#91;");

                break;

            case '\\':
                result.append("&#92;");

                break;

            case ']':
                result.append("&#93;");

                break;

            case '^':
                result.append("&#94;");

                break;

            case '_':
                result.append("&#95;");

                break;

            case '`':
                result.append("&#96;");

                break;

            case '{':
                result.append("&#123;");

                break;

            case '|':
                result.append("&#124;");

                break;

            case '}':
                result.append("&#125;");

                break;

            case '~':
                result.append("&#126;");

                break;

            default:
                result.append(content[i]);
            }
        }

        return (result.toString());
    }

    /*
     * Get the extension of a file e.g. txt
     */
    public static String getExtension(File f) {
        return FileUtil.getExtension(f);
    }

    public static Color alphaColor(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                alpha);
    }

    /**
     * Centres the first component on the second
     * 
     * @param componentToMove
     *            Description of the Parameter
     * @param componentToCentreOn
     *            Description of the Parameter
     */
    public static void centre(Component componentToMove,
        Component componentToCentreOn) {
      int offset = 30;
      Dimension screendim = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension componentToCentreOnSize = componentToCentreOn.getSize();
      int newx = componentToCentreOn.getX()
          + ((componentToCentreOnSize.width - componentToMove.getWidth()) / 2);
      int newy = componentToCentreOn.getY()
          + ((componentToCentreOnSize.height - componentToMove.getHeight()) / 2);
      int neww = componentToMove.getWidth(), newh = componentToMove.getHeight();
      // move back to screen if placed outside or too close to the border
      // (offsetted by 30px for possible os menus e.g. taskbar)
      if (newx < offset) {
        newx = offset;
      }
      if (newy < offset) {
        newy = offset;
      }
      // resize smaller if bigger than screen
      if (newx+neww>screendim.width-offset){
        neww = screendim.width-offset-newx;
      }
      if (newy+newh>screendim.height-offset){
        newh = screendim.height-offset-newy;
      }
      componentToMove.setBounds(newx, newy, neww, newh);
    }

    /**
     * Centres the component on the screen
     * 
     * @param componentToMove
     *            Description of the Parameter
     */
    public static void centreOnScreen(Component componentToMove) {
        Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        componentToMove.setLocation((rect.width - componentToMove
                .getWidth()) / 2 + rect.x, (rect.height - componentToMove
                .getHeight()) / 2 + rect.y);
    }

    /**
     * Centres the component on its window
     * 
     * @param componentToMove
     *            Description of the Parameter
     */
    public static void centreOnWindow(Component componentToMove) {
        centre(componentToMove, SwingUtilities
                .windowForComponent(componentToMove));
    }
    
    /**
     * Centrer the window on its parent window
     * 
     * @param windowToMove window to be centered on its parent window
     */
    public static void centreOnWindow(Window windowToMove) {
        windowToMove.pack();
        centre(windowToMove, SwingUtilities
                .windowForComponent(windowToMove));
    }

    /**
     * Sets the column widths based on the first row.
     * 
     * @param table
     *            Description of the Parameter
     */
    public static void chooseGoodColumnWidths(JTable table) {
        //Without padding, columns are slightly narrow, and we get "...". [Jon
        // Aquino]
        final int PADDING = 5;

        if (table.getModel().getRowCount() == 0) {
            return;
        }

        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            double headerWidth = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table,
                            table.getModel().getColumnName(i), false, false, 0,
                            i).getPreferredSize().getWidth()
                    + PADDING;
            double valueWidth = 10; // default in case of error

            try {
                valueWidth = table.getCellRenderer(0, i)
                        .getTableCellRendererComponent(table,
                                table.getModel().getValueAt(0, i), false,
                                false, 0, i).getPreferredSize().getWidth()
                        + PADDING;
            } catch (Exception ex) {
                // ignore the exception, since we can easily choose a default
                // width
            }

            //Limit column width to 200 pixels.
            int width = Math.min(200, Math.max((int) headerWidth,
                    (int) valueWidth));
            column.setPreferredWidth(width);

            //Need to set the actual width too, otherwise actual width may end
            //up a bit less than the preferred width. [Jon Aquino]
            column.setWidth(width);
        }
    }

    public static JFileChooser createJFileChooserWithExistenceChecking() {
        return new JFCWithEnterAction() {

            public void approveSelection() {
                File[] files = selectedFiles(this);

                if (files.length == 0) {
                    return;
                }

                for (int i = 0; i < files.length; i++) {
                    if (!files[i].exists() && !files[i].isFile()) {
                        return;
                    }
                }

                super.approveSelection();
            }
        };
    }

    public static JFileChooser createJFileChooserWithOverwritePrompting() {
        return new FileChooserWithOverwritePrompting();
    }

    public static JFileChooser createJFileChooserWithOverwritePrompting(String ext) {
        return new FileChooserWithOverwritePrompting(ext);
    }

    public static class FileChooserWithOverwritePrompting extends JFCWithEnterAction {

	private String ext;
	
        /**
	 * @param ext the default extension for files
	 */
	public FileChooserWithOverwritePrompting(String ext) {
	    this.ext = ext;
	}

	public FileChooserWithOverwritePrompting() {
	    // no extension set
	}
	
	public void approveSelection() {
            if (selectedFiles(this).length != 1) {
                return;
            }

            File selectedFile = selectedFile();

            if (selectedFile.exists() && !selectedFile.isFile()) {
                return;
            }

            if (selectedFile.exists() ||
        	    (ext != null && (!selectedFile.toString().endsWith(ext)) &&
        		    new File(selectedFile.toString() + "." + ext).exists())) {
        	int response = JOptionPane.showConfirmDialog(this, 
        	    I18N.getMessage("ui.GUIUtil.overwrite-prompting", new Object[]{selectedFile.getName()}), 
        	    "JUMP", JOptionPane.YES_NO_OPTION);

                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            super.approveSelection();
        }

        protected File selectedFile() {
            return selectedFiles(this)[0];
        }
    }

    public static void doNotRoundDoubles(JTable table) {
        table.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {

            public void setValue(Object value) {
                setText((value == null) ? "" : ("" + value));
            }

            {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
    }

    /**
     * Workaround for Java Bug 4648654 "REGRESSION: Editable JComboBox focus
     * misbehaves under Windows look and feel, proposed by Kleopatra
     * (fastegal@addcom.de). Also see Java Bug 4673880 "REGRESSION: Modified
     * editable JComboBox in Windows LAF does not release focus." This bug
     * started occurring in Java 1.4.0.
     * 
     * @param cb
     *            Description of the Parameter
     */
    public static void fixEditableComboBox(JComboBox cb) {
        Assert.isTrue(cb.isEditable());

        if (!UIManager.getLookAndFeel().getName().equals("Windows")) {
            return;
        }

        cb.setEditor(new BasicComboBoxEditor() {

            public void setItem(Object item) {
                super.setItem(item);
                editor.selectAll();
            }
        });
    }

    public static void handleThrowable(final Throwable t, final Component parent) {
        try {
            //<<TODO:UI>> A humane interface does not pop up an error dialog,
            // as that interrupts
            //the user's work. Rather, error messages are displayed
            // modelessly. See the book
            //"Humane Interfaces" (Raskin 2000) [Jon Aquino]
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    t.printStackTrace(System.out);
                    JOptionPane.showMessageDialog(parent, StringUtil.split(t
                            .toString(), 80), "Exception",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (Throwable t2) {
            t2.printStackTrace(System.out);
        }
    }

    /**
     * GUI operations should be performed only on the AWT event dispatching
     * thread. Blocks until the Runnable is finished.
     */
    public static void invokeOnEventThread(Runnable r)
            throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeAndWait(r);
        }
    }

    public static String nameWithoutExtension(File file) {
        String name = file.getName();
        int dotPosition = name.indexOf('.');

        return (dotPosition < 0) ? name : name.substring(0, dotPosition);
    }

    public static void removeChoosableFileFilters(JFileChooser fc) {
        FileFilter[] filters = fc.getChoosableFileFilters();

        for (int i = 0; i < filters.length; i++) {
            fc.removeChoosableFileFilter(filters[i]);
        }

        return;
    }

    /**
     * @param extensions
     *            e.g. txt
     */
    public static FileFilter createFileFilter(final String description,
            final String[] extensions) {
        return new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }

                for (int i = 0; i < extensions.length; i++) {
                    if (GUIUtil.getExtension(f).equalsIgnoreCase(extensions[i])) {
                        return true;
                    }
                }

                return false;
            }

            public String getDescription() {
                ArrayList extensionStrings = new ArrayList();

                for (int i = 0; i < extensions.length; i++) {
                    extensionStrings.add("*." + extensions[i]);
                }

                return description
                        + " ("
                        + StringUtil.replaceAll(StringUtil
                                .toCommaDelimitedString(extensionStrings), ",",
                                ";") + ")";
            }
        };
    }

    /**
     * @param color
     *            a Color with possibly an alpha less than 255
     * @return a Color with alpha equal to 255, but equivalent to the original
     *         translucent colour on a white background
     */
    public static Color toSimulatedTransparency(Color color) {
        //My guess, but it seems to work! [Jon Aquino]
        return new Color(
                color.getRed()
                        + (int) (((255 - color.getRed()) * (255 - color
                                .getAlpha())) / 255d), color.getGreen()
                        + (int) (((255 - color.getGreen()) * (255 - color
                                .getAlpha())) / 255d), color.getBlue()
                        + (int) (((255 - color.getBlue()) * (255 - color
                                .getAlpha())) / 255d));
    }

    public static String truncateString(String s, int maxLength) {
        if (s.length() < maxLength) {
            return s;
        }

        return s.substring(0, maxLength - 3) + "...";
    }

    public static Point2D subtract(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public static Point2D add(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() + b.getX(), a.getY() + b.getY());
    }

    public static Point2D multiply(Point2D v, double x) {
        return new Point2D.Double(v.getX() * x, v.getY() * x);
    }

    /**
     * The JVM's clipboard implementation is buggy (see bugs 4644554 and 4522198
     * in Sun's Java bug database). This method is a workaround that returns
     * null if an exception is thrown, as suggested in the bug reports.
     */
    public static Transferable getContents(Clipboard clipboard) {
        try {
            return clipboard.getContents(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Returns the distance from the baseline to the top of the text's bounding
     * box. Unlike the usual ascent, which is independent of the actual text.
     * Note that "True ascent" is not a standard term.
     */
    public static double trueAscent(TextLayout layout) {
        return -layout.getBounds().getY();
    }

    public static ImageIcon resize(ImageIcon icon, int extent) {
        return new ImageIcon(icon.getImage().getScaledInstance(extent, extent,
                Image.SCALE_SMOOTH));
    }

    /**
     * Resizes icon to 16 x 16.
     */
    public static ImageIcon toSmallIcon(ImageIcon icon) {
        return resize(icon, 16);
    }
    
    /**
     * Resize icon to specified width
     * @param icon
     * @param width
     * @return
     */
    public static ImageIcon toSmallIcon(ImageIcon icon, int width) {
      return resize(icon, width);
    }

    public static int swingThreadPriority() {
        final Int i = new Int();

        try {
            invokeOnEventThread(new Runnable() {

                public void run() {
                    i.i = Thread.currentThread().getPriority();
                }
            });
        } catch (InvocationTargetException e) {
            Assert.shouldNeverReachHere();
        } catch (InterruptedException e) {
            Assert.shouldNeverReachHere();
        }

        return i.i;
    }

    /**
     * Fix for Sun Java Bug 4398733: if you click in an inactive JInternalFrame,
     * the mousePressed and mouseReleased events will be fired, but not the
     * mouseClicked event.
     */
    public static void fixClicks(final Component c) {
        //This is a time bomb because when (if?) Sun fixes the bug, this
        // method will
        //add an extra click. We should put an if statement here that
        // immediately
        //returns if the Java version is greater than or equal to that in
        // which the bug
        //is fixed. Problem is, we don't know what that version will be. [Jon
        // Aquino]
        c.addMouseListener(new MouseListener() {

            public void mousePressed(MouseEvent e) {
                add(e);
            }

            public void mouseExited(MouseEvent e) {
                add(e);
            }

            public void mouseClicked(MouseEvent e) {
                add(e);
            }

            public void mouseEntered(MouseEvent e) {
                add(e);
            }

            private MouseEvent event(int i) {
                return (MouseEvent) events.get(i);
            }

            public void mouseReleased(MouseEvent e) {
                add(e);

                if ((events.size() == 4)
                        && (event(0).getID() == MouseEvent.MOUSE_PRESSED)
                        && (event(1).getID() == MouseEvent.MOUSE_EXITED)
                        && (event(2).getID() == MouseEvent.MOUSE_ENTERED)) {
                    c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), e.getModifiers(), e
                                    .getX(), e.getY(), e.getClickCount(), e
                                    .isPopupTrigger()));
                }
            }

            private void add(MouseEvent e) {
                if (events.size() == 4) {
                    events.remove(0);
                }

                events.add(e);
            }

            private ArrayList events = new ArrayList();
        });
    }

    /**
     * Listens to all internal frames (current and future) in a JDesktopPane.
     */
    public static void addInternalFrameListener(JDesktopPane pane,
            final InternalFrameListener listener) {
        JInternalFrame[] frames = pane.getAllFrames();

        for (int i = 0; i < frames.length; i++) {
            frames[i].addInternalFrameListener(listener);
        }

        pane.addContainerListener(new ContainerAdapter() {

            public void componentAdded(ContainerEvent e) {
                if (e.getChild() instanceof JInternalFrame) {
                    ((JInternalFrame) e.getChild())
                            .removeInternalFrameListener(listener);
                    ((JInternalFrame) e.getChild())
                            .addInternalFrameListener(listener);
                }
            }
        });
    }

    public static DocumentListener toDocumentListener(
            final ActionListener listener) {
        return new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                listener.actionPerformed(new ActionEvent(e, 0, e.toString()));
            }

            public void removeUpdate(DocumentEvent e) {
                listener.actionPerformed(new ActionEvent(e, 0, e.toString()));
            }

            public void changedUpdate(DocumentEvent e) {
                listener.actionPerformed(new ActionEvent(e, 0, e.toString()));
            }
        };
    }

    public static ListDataListener toListDataListener(
            final ActionListener listener) {
        return new ListDataListener() {

            public void intervalAdded(ListDataEvent e) {
                listener.actionPerformed(new ActionEvent(e.getSource(), 0, e
                        .toString()));
            }

            public void intervalRemoved(ListDataEvent e) {
                listener.actionPerformed(new ActionEvent(e.getSource(), 0, e
                        .toString()));
            }

            public void contentsChanged(ListDataEvent e) {
                listener.actionPerformed(null);
            }
        };
    }

    public static InternalFrameListener toInternalFrameListener(
            final ActionListener listener) {
        return new InternalFrameListener() {

            private void fireActionPerformed(InternalFrameEvent e) {
                listener.actionPerformed(new ActionEvent(e.getSource(), e
                        .getID(), e.toString()));
            }

            public void internalFrameActivated(InternalFrameEvent e) {
                fireActionPerformed(e);
            }

            public void internalFrameClosed(InternalFrameEvent e) {
                fireActionPerformed(e);
            }

            public void internalFrameClosing(InternalFrameEvent e) {
                fireActionPerformed(e);
            }

            public void internalFrameDeactivated(InternalFrameEvent e) {
                fireActionPerformed(e);
            }

            public void internalFrameDeiconified(InternalFrameEvent e) {
                fireActionPerformed(e);
            }

            public void internalFrameIconified(InternalFrameEvent e) {
                fireActionPerformed(e);
            }

            public void internalFrameOpened(InternalFrameEvent e) {
                fireActionPerformed(e);
            }
        };
    }

    /**
     * Returns a Timer that fires once, after the delay. The delay can be
     * restarted by restarting the Timer.
     */
    public static Timer createRestartableSingleEventTimer(int delay,
            ActionListener listener) {
        Timer timer = new Timer(delay, listener);
        timer.setCoalesce(true);
        timer.setInitialDelay(delay);
        timer.setRepeats(false);

        return timer;
    }

    public static ValidatingTextField createSyncdTextField(JSlider s) {
        int columns = (int) Math.ceil(Math.log(s.getMaximum()) / Math.log(10));

        return createSyncdTextField(s, columns);
    }

    public static ValidatingTextField createSyncdTextField(JSlider s,
            int columns) {
        ValidatingTextField t = new ValidatingTextField(s.getValue() + "",
                columns, SwingConstants.RIGHT,
                ValidatingTextField.INTEGER_VALIDATOR,
                new ValidatingTextField.CompositeCleaner(
                        new ValidatingTextField.Cleaner[] {
                                new ValidatingTextField.BlankCleaner(""
                                        + s.getMinimum()),
                                new ValidatingTextField.MinIntCleaner(s
                                        .getMinimum()),
                                new ValidatingTextField.MaxIntCleaner(s
                                        .getMaximum()) }));
        sync(s, t);
        syncEnabledStates(s, t);

        return t;
    }

    /**
     * @see #createSyncdTextField(JSlider s, int columns)
     */
    public static void sync(final JSlider s, final ValidatingTextField t) {
        t.setText("" + s.getValue());

        final Boolean[] changing = new Boolean[] { Boolean.FALSE };
        s.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (changing[0] == Boolean.TRUE) {
                    return;
                }

                changing[0] = Boolean.TRUE;

                try {
                    t.setText("" + s.getValue());
                } finally {
                    changing[0] = Boolean.FALSE;
                }
            }
        });
        t.getDocument().addDocumentListener(new DocumentListener() {

            private void changed() {
                if (changing[0] == Boolean.TRUE) {
                    return;
                }

                changing[0] = Boolean.TRUE;

                try {
                    s.setValue(t.getInteger());
                } finally {
                    changing[0] = Boolean.FALSE;
                }
            }

            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }
        });
    }

    public static void syncEnabledStates(final JComponent c1,
            final JComponent c2) {
        c2.setEnabled(c1.isEnabled());
        c1.addPropertyChangeListener("enabled", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (c1.isEnabled() == c2.isEnabled()) {
                    return;
                }

                c2.setEnabled(c1.isEnabled());
            }
        });
        c2.addPropertyChangeListener("enabled", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (c1.isEnabled() == c2.isEnabled()) {
                    return;
                }

                c1.setEnabled(c2.isEnabled());
            }
        });
    }

    public static void sync(final JSlider s1, final JSlider s2) {
        s2.setValue(s1.getValue());
        Assert.isTrue(s1.getMinimum() == s2.getMinimum());
        Assert.isTrue(s1.getMaximum() == s2.getMaximum());

        final Boolean[] changing = new Boolean[] { Boolean.FALSE };
        s1.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (changing[0] == Boolean.TRUE) {
                    return;
                }

                changing[0] = Boolean.TRUE;

                try {
                    s2.setValue(s1.getValue());
                } finally {
                    changing[0] = Boolean.FALSE;
                }
            }
        });
        s2.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (changing[0] == Boolean.TRUE) {
                    return;
                }

                changing[0] = Boolean.TRUE;

                try {
                    s1.setValue(s2.getValue());
                } finally {
                    changing[0] = Boolean.FALSE;
                }
            }
        });
    }

    public static void sync(final JCheckBox c1, final JCheckBox c2) {
        c2.setSelected(c1.isSelected());

        final Boolean[] changing = new Boolean[] { Boolean.FALSE };
        c1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (changing[0] == Boolean.TRUE) {
                    return;
                }

                changing[0] = Boolean.TRUE;

                try {
                    c2.setSelected(c1.isSelected());
                } finally {
                    changing[0] = Boolean.FALSE;
                }
            }
        });
        c2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (changing[0] == Boolean.TRUE) {
                    return;
                }

                changing[0] = Boolean.TRUE;

                try {
                    c1.setSelected(c2.isSelected());
                } finally {
                    changing[0] = Boolean.FALSE;
                }
            }
        });
    }

    public static List items(JComboBox comboBox) {
        ArrayList items = new ArrayList();

        for (int i = 0; i < comboBox.getItemCount(); i++) {
            items.add(comboBox.getItemAt(i));
        }

        return items;
    }

    /**
     * Calls #doClick so that events are fired.
     */
    public static void setSelectedWithClick(JCheckBox checkBox, boolean selected) {
        checkBox.setSelected(!selected);
        checkBox.doClick();
    }

    public static void setLocation(Component componentToMove,
            Location location, Component other) {
        Point p = new Point(
                (int) other.getLocationOnScreen().getX()
                        + (location.fromRight ? (other.getWidth()
                                - componentToMove.getWidth() - location.x)
                                : location.x), (int) other
                        .getLocationOnScreen().getY()
                        + (location.fromBottom ? (other.getHeight()
                                - componentToMove.getHeight() - location.y)
                                : location.y));
        if (!(componentToMove instanceof Window)) {
            SwingUtilities.convertPointFromScreen(p, componentToMove
                    .getParent());
        }
        componentToMove.setLocation(p);
    }

    /**
     * Highlights a given component with a given color. Great for GridBagLayout
     * debugging.
     *  
     */
    public static void highlightForDebugging(JComponent component, Color color) {
        component.setBackground(color);
        component.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10,
                color));
    }

    public static Component topCard(Container c) {
        Assert.isTrue(c.getLayout() instanceof CardLayout);

        Component[] components = c.getComponents();

        for (int i = 0; i < components.length; i++) {
            if (components[i].isVisible()) {
                return components[i];
            }
        }

        Assert.shouldNeverReachHere();

        return null;
    }

    /**
     * Work around Java Bug 4437688 "JFileChooser.getSelectedFile() returns
     * nothing when a file is selected" [Jon Aquino]
     */
    public static File[] selectedFiles(JFileChooser chooser) {
        return ((chooser.getSelectedFiles().length == 0) && (chooser
                .getSelectedFile() != null)) ? new File[] { chooser
                .getSelectedFile() } : chooser.getSelectedFiles();
    }

    public static ImageIcon toDisabledIcon(ImageIcon icon) {
        return new ImageIcon(GrayFilter.createDisabledImage((icon).getImage()));
    }

    public static Component getDescendantOfClass(Class c, Container container) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            if (c.isInstance(container.getComponent(i))) {
                return container.getComponent(i);
            }

            if (container.getComponent(i) instanceof Container) {
                Component descendant = getDescendantOfClass(c,
                        (Container) container.getComponent(i));

                if (descendant != null) {
                    return descendant;
                }
            }
        }

        return null;
    }

    /**
     * Ensures that the next frame is activated when #dispose is called
     * explicitly, in JDK 1.4. JDK 1.3 didn't have this problem.
     */
    public static void dispose(final JInternalFrame internalFrame,
            JDesktopPane desktopPane) {
        desktopPane.getDesktopManager().closeFrame(internalFrame);
        internalFrame.dispose();
    }

    private static class Int {

        public volatile int i;
    }

    public static class Location {

        private int x;

        private int y;

        private boolean fromRight;

        private boolean fromBottom;

        /**
         * Constructor taking an initial location, offset hint.
         * 
         * @param fromBottom
         *            whether y is the number of pixels between the bottom edges
         *            of the toolbox and desktop pane, or between the top edges.
         */
        public Location(int x, boolean fromRight, int y, boolean fromBottom) {
            this.x = x;
            this.y = y;
            this.fromRight = fromRight;
            this.fromBottom = fromBottom;
        }
    }

    public static Cursor createCursorFromIcon(Image iconImage) {
        //Don't use GUIUtil#resize, which uses SCALE_SMOOTH, which
        //makes the check-mark icons chunky-looking.
        //[2004-02-27]
        ImageIcon icon = new ImageIcon(iconImage.getScaledInstance(12, 12,
                Image.SCALE_REPLICATE));
        ImageIcon basicCursor = IconLoader.icon("basic-cursor.png");
        BufferedImage image = new BufferedImage(basicCursor.getIconWidth(),
                basicCursor.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(basicCursor.getImage(), 0, 0, null);
        graphics.drawImage(icon.getImage(), 10, 10, null);
        return createCursor(image, new Point(0, 15));
    }

    public static Cursor createCursor(Image image, Point hotSpot) {
        if (null == image) {
            return Cursor.getDefaultCursor();
        }

        if (Toolkit.getDefaultToolkit().getBestCursorSize(32, 32).equals(
                new Dimension(0, 0))) {
            return Cursor.getDefaultCursor();
        }

        return Toolkit.getDefaultToolkit().createCustomCursor(image, hotSpot,
                I18N.get("ui.GUIUtil.jump-workbench-custom-cursor"));
    }

    /**
     * Based on Green, Roedy. "Java Glossary : focus". 
     * Available from http://mindprod.com/jgloss/focus.html.
     * Internet; accessed 8 March 2004.
     */
    public static JTextArea makeTabMoveFocus(JTextArea textArea) {
        textArea.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                new HashSet(Arrays.asList(new Object[] { KeyStroke
                        .getKeyStroke("TAB") })));
        textArea.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new HashSet(
                        Arrays.asList(new Object[] { KeyStroke
                                .getKeyStroke("shift TAB") })));
        return textArea;
    }

    public static void shrinkFont(JComponent component) {
        component.setFont(component.getFont().deriveFont(
                (float) component.getFont().getSize() - 2));
    }
}
