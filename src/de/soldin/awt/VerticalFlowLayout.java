/**
 * This is based on several versions of VerticalFlowLayout on the net which 
 * all were initially based on java.awt.FlowLayout .
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package de.soldin.awt;

import java.awt.*;
import javax.swing.*;

/**
 * This flow layout arranges components in a directional flow, much like lines
 * of text in a paragraph but vertically. The flow direction is determined by
 * the container's <code>componentOrientation</code> property and may be one of
 * two values:
 * <ul>
 * <li><code>ComponentOrientation.LEFT_TO_RIGHT</code>
 * <li><code>ComponentOrientation.RIGHT_TO_LEFT</code>
 * </ul>
 * Don't be confused. These are simply interpreted as standard TOP_TO_BOTTOM
 * (LEFT_TO_RIGHT) or BOTTOM_TO_TOP (RIGHT_TO_LEFT) as ComponentOrientation does
 * not provide vertical orientation values.
 * <p>
 * We arrange components vertically until no more components fit on the same
 * column. The vertical alignment is determined by the <code>align</code>
 * property. The possible values are:
 * <ul>
 * <li>{@link #TOP TOP}
 * <li>{@link #BOTTOM BOTTOM}
 * <li>{@link #CENTER CENTER}
 * </ul>
 * <p>
 * See {@link #main main() method} in source code for an example on how to use
 * it.
 */
public class VerticalFlowLayout implements LayoutManager, java.io.Serializable {
  /**
   * This value indicates that each column of components should be
   * top-justified.
   */
  public static final int TOP = 0;

  /**
   * This value indicates that each column of components should be centered.
   */
  public static final int CENTER = 1;

  /**
   * This value indicates that each column of components should be
   * bottom-justified.
   */
  public static final int BOTTOM = 2;

  /**
   * <code>align</code> is the property that determines how each column
   * distributes empty space. It can be one of the following three values:
   * <ul>
   * <code>TOP</code> <code>BOTTOM</code> <code>CENTER</code>
   * </ul>
   * 
   * @see #getAlignment
   * @see #setAlignment
   */
  int align;

  /**
   * The flow layout manager allows a separation of components with gaps. The
   * horizontal gap will specify the space between components and between the
   * components and the borders of the <code>Container</code>.
   * 
   * @see #getHgap()
   * @see #setHgap(int)
   */
  int hgap;

  /**
   * The flow layout manager allows a seperation of components with gaps. The
   * vertical gap will specify the space between rows and between the the rows
   * and the borders of the <code>Container</code>.
   * 
   * @see #getHgap()
   * @see #setHgap(int)
   */
  int vgap;

  // cache preferred size of latest layout
  private Dimension preferred = null;
  // wrap mode constants
  public static final int WIDTH = 0;
  public static final int HEIGHT = 1;

  // wrap mode (default wrap by width)
  private int wrapMode = WIDTH;
  // default packing ratio, width divided by height
  // e.g. double as wide as high 2/1 = 2
  // 3 times as high as wide 1/3 = 0.33..
  // quadratic 1/1 = 1 (default setting)
  private double defRatio = 1;

  /**
   * Constructs a new <code>VerticalFlowLayout</code> with a centered alignment
   * and a default 5-unit horizontal and vertical gap.
   */
  public VerticalFlowLayout() {
    this(CENTER, 5, 5);
  }

  /**
   * Constructs a new <code>VerticalFlowLayout</code> with the specified
   * alignment and a default 5-unit horizontal and vertical gap. The value of
   * the alignment argument must be one of <code>VerticalFlowLayout.TOP</code>,
   * <code>VerticalFlowLayout.BOTTOM</code>, or
   * <code>VerticalFlowLayout.CENTER</code>
   * 
   * @param align
   *          the alignment value
   */
  public VerticalFlowLayout(int align) {
    this(align, 5, 5);
  }

  /**
   * Creates a new flow layout manager with the indicated alignment and the
   * indicated horizontal and vertical gaps.
   * <p>
   * The value of the alignment argument must be one of
   * <code>VerticalFlowLayout.TOP</code>, <code>VerticalFlowLayout.BOTTOM</code>
   * , or <code>VerticalFlowLayout.CENTER</code>.
   * 
   * @param align
   *          the alignment value
   * @param hgap
   *          the horizontal gap between components and between the components
   *          and the borders of the <code>Container</code>
   * @param vgap
   *          the vertical gap between components and between the components and
   *          the borders of the <code>Container</code>
   */
  public VerticalFlowLayout(int align, int hgap, int vgap) {
    this.hgap = hgap;
    this.vgap = vgap;
    setAlignment(align);
  }

  /**
   * Gets the alignment for this layout. Possible values are
   * <code>VerticalFlowLayout.TOP</code>, <code>VerticalFlowLayout.BOTTOM</code>
   * or <code>VerticalFlowLayout.CENTER</code>,
   * 
   * @return the alignment value for this layout
   * @see java.awt.VerticalFlowLayout#setAlignment
   */
  public int getAlignment() {
    return align;
  }

  /**
   * Sets the alignment for this layout. Possible values are
   * <ul>
   * <li><code>VerticalFlowLayout.TOP</code>
   * <li><code>VerticalFlowLayout.BOTTOM</code>
   * <li><code>VerticalFlowLayout.CENTER</code>
   * </ul>
   * 
   * @param align
   *          one of the alignment values shown above
   * @see #getAlignment()
   */
  public void setAlignment(int align) {
    this.align = align;
  }

  /**
   * Sets the wrap mode for this layout
   * 
   * @return int wrap mode
   */
  public int getWrapMode() {
    return wrapMode;
  }

  /**
   * Sets the alignment for this layout. Possible values are
   * <ul>
   * <li><code>VerticalFlowLayout.WIDTH</code> (default)
   * <li><code>VerticalFlowLayout.HEIGHT</code>
   * </ul>
   * 
   * @param wrapMode
   */
  public void setWrapMode(int wrapMode) {
    this.wrapMode = wrapMode;
  }

  public double getDefaultRatio() {
    return defRatio;
  }

  /**
   * Set a default layout ratio. See {@link #defRatio} comment above.
   * 
   * @param defRatio
   */
  public void setDefaultRatio(double defRatio) {
    this.defRatio = defRatio;
  }

  /**
   * Gets the horizontal gap between components and between the components and
   * the borders of the <code>Container</code>
   * 
   * @return the horizontal gap between components and between the components
   *         and the borders of the <code>Container</code>
   * @see java.awt.VerticalFlowLayout#setHgap
   * @since JDK1.1
   */
  public int getHgap() {
    return hgap;
  }

  /**
   * Sets the horizontal gap between components and between the components and
   * the borders of the <code>Container</code>.
   * 
   * @param hgap
   *          the horizontal gap between components and between the components
   *          and the borders of the <code>Container</code>
   * @see java.awt.VerticalFlowLayout#getHgap
   * @since JDK1.1
   */
  public void setHgap(int hgap) {
    this.hgap = hgap;
  }

  /**
   * Gets the vertical gap between components and between the components and the
   * borders of the <code>Container</code>.
   * 
   * @return the vertical gap between components and between the components and
   *         the borders of the <code>Container</code>
   * @see java.awt.VerticalFlowLayout#setVgap
   * @since JDK1.1
   */
  public int getVgap() {
    return vgap;
  }

  /**
   * Sets the vertical gap between components and between the components and the
   * borders of the <code>Container</code>.
   * 
   * @param vgap
   *          the vertical gap between components and between the components and
   *          the borders of the <code>Container</code>
   * @see java.awt.VerticalFlowLayout#getVgap
   */
  public void setVgap(int vgap) {
    this.vgap = vgap;
  }

  /**
   * Adds the specified component to the layout. Not used by this class.
   * 
   * @param name
   *          the name of the component
   * @param comp
   *          the component to be added
   */
  public void addLayoutComponent(String name, Component comp) {
  }

  /**
   * Removes the specified component from the layout. Not used by this class.
   * 
   * @param comp
   *          the component to remove
   * @see java.awt.Container#removeAll
   */
  public void removeLayoutComponent(Component comp) {
  }

  /**
   * Returns the preferred dimensions for this layout given the <i>visible</i>
   * components in the specified target container.
   * 
   * @param target
   *          the container that needs to be laid out
   * @return the preferred dimensions to lay out the subcomponents of the
   *         specified container
   * @see Container
   * @see #minimumLayoutSize
   * @see java.awt.Container#getPreferredSize
   */
  public Dimension preferredLayoutSize(Container target) {
    if (preferred == null){
      layoutContainer(target);
      Dimension dim = padSize(target, preferred);
      return new Dimension(dim.width+20,dim.height+20);
    }
      
    // we only start working around the scrollpane bug after
    // the first request return the proper size
    return padScroll(target, padSize(target, preferred));
  }

  /**
   * Returns the minimum dimensions needed to layout the <i>visible</i>
   * components contained in the specified target container.
   * 
   * Not supported by this class! Returns
   * {@link #preferredLayoutSize(Container)} instead.
   */
  public Dimension minimumLayoutSize(Container target) {
    return new Dimension(100,100);//preferredLayoutSize(target);
  }

  /**
   * Lays out the container. This method lets each <i>visible</i> component take
   * its preferred size by reshaping the components in the target container in
   * order to satisfy the alignment of this <code>VerticalFlowLayout</code>
   * object.
   * 
   * @param target
   *          the specified component being laid out
   * @see Container
   * @see java.awt.Container#doLayout
   */
  public void layoutContainer(Container target) {
    synchronized (target.getTreeLock()) {
      Insets insets = target.getInsets();
      int targetHeight = target.getSize().height;
      int targetWidth = target.getSize().width;

      if (targetHeight <= 0)
        targetHeight = Integer.MAX_VALUE;
      if (targetWidth <= 0)
        targetWidth = Integer.MAX_VALUE;

      //System.out.println("t: "+targetWidth+"/"+targetHeight);
      
      int maxHeight = Integer.MAX_VALUE, maxWidth = Integer.MAX_VALUE;

      Dimension scrollFix = scrollFix(target);
      if (targetWidth == Integer.MAX_VALUE && wrapMode == WIDTH) {
        Dimension dim = calculateSizeByRatio(target);
        maxWidth = dim.width + scrollFix.width;
      } else if (targetHeight == Integer.MAX_VALUE && wrapMode == HEIGHT) {
        Dimension dim = calculateSizeByRatio(target);
        maxHeight = dim.height + scrollFix.height;
      } else {
        maxHeight = targetHeight - (insets.top + insets.bottom);
        maxWidth = targetWidth - (insets.left + insets.right);
      }

      //System.out.println("m: "+maxWidth+"/"+maxHeight);
      
      int nmembers = target.getComponentCount();
      int x = 0; // insets.left/* + hgap*/;
      int y = 0;
      int columnWidth = 0;
      int start = 0;

      boolean ttb = target.getComponentOrientation().isLeftToRight();

      if (wrapMode == HEIGHT) {
        int maxColHeight = 0;
        for (int i = 0; i < nmembers; i++) {
          Component m = target.getComponent(i);

          if (m.isVisible()) {
            Dimension d = m.getPreferredSize();
            m.setSize(d.width, d.height);
//            if (m instanceof JLabel)
//              System.out.println(((JLabel) m).getText());
            if ((y == 0) || ((y + d.height) <= maxHeight)) {
              if (y > 0) {
                y += vgap;
              }

              y += d.height;
              columnWidth = Math.max(columnWidth, d.width);
              maxColHeight = Math.max(maxColHeight, y);
            } else {
              //System.out.println("move " + start + "/" + i);
              moveComponents(target, x, 0, columnWidth, maxHeight - y, start,
                  i, ttb);
              y = d.height;
              x += hgap + columnWidth;
              columnWidth = d.width;
              start = i;
            }
          }
        }

        //System.out.println("final move " + start);
        moveComponents(target, x, 0, columnWidth, maxHeight - y, start,
            nmembers, ttb);
        // cache preferred size
        preferred = new Dimension(x + columnWidth, maxColHeight);

      } else {
        int maxColHeight = 0;
        boolean tooWide = false;
        for (int j = 0; j < nmembers && (maxColHeight == 0 || tooWide); j++) {
          tooWide = false;
          // System.out.println("maxh/w/j "+maxColHeight+"/"+maxWidth+"/"+j);
          x = 0;
          y = 0;
          columnWidth = 0;
          start = 0;
          for (int i = 0; i < nmembers && x < maxWidth; i++) {
            Component m = target.getComponent(i);

            if (m.isVisible()) {
              Dimension d = m.getPreferredSize();
              m.setSize(d.width, d.height);
              // if (m instanceof JLabel) System.out.println(((JLabel)
              // m).getText()+"/"+x);

              if ((y == 0) || (i <= j) || ((y + d.height) <= maxColHeight)) {
                if (y > 0) {
                  y += vgap;
                }
                y += d.height;
                columnWidth = Math.max(columnWidth, d.width);
                if (i <= j)
                  maxColHeight = y;
              } else {
                // System.out.println("move " + start + "/" + i);
                moveComponents(target, x, 0, columnWidth, maxHeight - y, start,
                    i, ttb);
                y = d.height;
                x += hgap + columnWidth;
                columnWidth = d.width;
                start = i;

              }
            }
          }
          moveComponents(target, x, 0, columnWidth, maxHeight - y, start,
              nmembers, ttb);
          x += columnWidth + hgap;

          // System.out.println("tw.mw.x.mc "+targetWidth+"/"+maxWidth+"/"+x+"/"+maxColHeight);
          // tooWide = (targetWidth!=Integer.MAX_VALUE) ? x>maxWidth :
          // x>maxColHeight*1.5;
          tooWide = x > maxWidth;
        }
        int scrollBug = (preferred==null) ? 0 : 0;
        // cache preferred size
        preferred = new Dimension(x + scrollBug, maxColHeight + scrollBug);
        //target.setSize(preferred);
      }
    }
//    System.out.println("set pref " + preferred);
//    System.out.println("get pref " + preferredLayoutSize(target));
  }

  /**
   * Centers the elements in the specified row, if there is any slack.
   * 
   * @param target
   *          the component which needs to be moved
   * @param x
   *          the x coordinate
   * @param y
   *          the y coordinate
   * @param width
   *          the width dimensions
   * @param height
   *          the height dimensions
   * @param columnStart
   *          the beginning of the column
   * @param columnEnd
   *          the the ending of the column
   */
  private void moveComponents(Container target, int x, int y, int width,
      int height, int columnStart, int columnEnd, boolean ttb) {
    Insets insets = target.getInsets();
    x += insets.left;
    y += ttb ? insets.top : insets.bottom;
    switch (align) {
    case TOP:
      y += ttb ? 0 : height;
      break;
    case CENTER:
      y += height / 2;
      break;
    case BOTTOM:
      y += ttb ? height : 0;
      break;
    }

    for (int i = columnStart; i < columnEnd; i++) {
      Component m = target.getComponent(i);

      if (m.isVisible()) {
        int cx;
        cx = x + (width - m.getSize().width) / 2;

        if (ttb) {
          m.setLocation(cx, y);
        } else {
          m.setLocation(cx, target.getSize().height - y - m.getSize().height);
        }

        y += m.getSize().height + vgap;
      }
    }
  }

  private Dimension calculateSizeByRatio(Container target) {
    boolean tooHigh = true;
    int maxAllowedColHeight = 0, columnWidth = 0, x = 0, y = 0;
    while (tooHigh) {
      x = 0;
      y = 0;
      int firstColHeightMinusOne = 0;
      for (int i = 0; i < target.getComponentCount(); i++) {
        Component m = target.getComponent(i);
        Dimension d = m.getPreferredSize();

        if (m.isVisible()) {
          if (maxAllowedColHeight <= 0 || y < maxAllowedColHeight) {
            if (y > 0) {
              y += vgap;
            }
            y += d.height;
            columnWidth = Math.max(columnWidth, d.width);
          } else {
            y = d.height;
            x += hgap + columnWidth;
            columnWidth = d.width;
          }
          // save alternative height of first col to try next
          if (x == 0)
            firstColHeightMinusOne = y - vgap - d.height;
        }
      }

      x += hgap + columnWidth;
      tooHigh = firstColHeightMinusOne > 0
          && (maxAllowedColHeight <= 0 || this.defRatio * maxAllowedColHeight > x);
      if (tooHigh)
        maxAllowedColHeight = firstColHeightMinusOne;
    }
    return new Dimension(x, maxAllowedColHeight);
  }

  /**
   * Postprocess a preferred dimension.
   * @param target
   * @param in
   * @return dim
   */
  private Dimension padSize(Container target, Dimension in) {
    Dimension dim = new Dimension(in);
    Insets insets = target.getInsets();
    dim.width += insets.left + insets.right;
    dim.height += insets.top + insets.bottom;

    return dim;
  }

  private Dimension padScroll(Container target, Dimension dim) {
    Dimension scrollFix = scrollFix(target);
    dim.width -= scrollFix.width;
    dim.height -= scrollFix.height;
    return dim;
  }
  
  private Dimension scrollFix(Container target) {
    // When using a scroll pane or the DecoratedLookAndFeel we need to
    // make sure the preferred size is less than the size of the
    // target container so shrinking the container size works
    // correctly. Seems to be a bug in ScrollPane.
    // Merely underreporting seems to workaround that. Add some border
    // if your content gets clipped.
    int height = 0, width = 0;
    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
        JScrollPane.class, target);
    if (scrollPane != null) {
      //if (scrollPane.getHorizontalScrollBar().getHeight()>0)
      height = scrollPane.getHorizontalScrollBar().getPreferredSize().height;
      //if (scrollPane.getVerticalScrollBar().getWidth()>0)
      width = scrollPane.getVerticalScrollBar().getPreferredSize().width;
    }
    return new Dimension(width, height);
  }
  
  /**
   * Returns a string representation of this <code>VerticalFlowLayout</code>
   * object and its values.
   * 
   * @return a string representation of this layout
   */
  public String toString() {
    String str = "";

    switch (align) {
    case TOP:
      str = ",align=top";
      break;
    case CENTER:
      str = ",align=center";
      break;
    case BOTTOM:
      str = ",align=bottom";
      break;
    }

    return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + str + "]";
  }

  public static void main(String[] args) {
    JPanel main = new JPanel(new BorderLayout());

    VerticalFlowLayout l = new VerticalFlowLayout(VerticalFlowLayout.TOP, 5, 10);
    // l.setWrapMode(VerticalFlowLayout3.HEIGHT);
    // l.setDefaultRatio(1.33);

    JPanel p = new JPanel(l);
    // p.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    p.setBorder(BorderFactory.createMatteBorder(10, 10, 20, 20,
        Color.LIGHT_GRAY));
    for (int i = 0; i <= 100; i++) {
      String text = ((i % 5) == 0) ? "<html>" + i + "<br><br>some more</html>"
          : i + "";
      p.add(new JLabel(text));
    }
    JScrollPane scroll = new JScrollPane(p);
    // scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    // scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    main.add(scroll, BorderLayout.CENTER);

    JFrame frame = new JFrame();

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(main);
    // frame.setSize(300, 300);
    // frame.setLocationRelativeTo(null);
    frame.pack();
    frame.setVisible(true);
    // frame.pack();
  }

}