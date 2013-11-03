package org.openjump.core.ui.plugin.window;

import java.awt.Dimension;
import java.beans.PropertyVetoException;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ArrangeViewsPlugIn extends AbstractPlugIn {

  public static final String NAME = I18N
      .get("org.openjump.core.ui.plugin.window.ArrangeViewsPlugIn.name");

  public static final Icon HORIZONTAL_ICON = IconLoader
      .icon("application_tile_horizontal.png");
  public static final Icon VERTICAL_ICON = IconLoader
      .icon("application_tile_vertical.png");
  public static final Icon CASCADE_ICON = IconLoader
      .icon("application_cascade.png");
  public static final Icon ARRANGE_ICON = IconLoader
      .icon("application_distribute.png");

  public static final int HORIZONTAL = 1;
  public static final int VERTICAL = 2;
  public static final int CASCADE = 3;
  public static final int ARRANGE = 4;
  private static JDesktopPane desktopPane;
  private int arrangeType = 0;

  public ArrangeViewsPlugIn() {
  }

  public ArrangeViewsPlugIn(int type) {
    this.arrangeType = type;
  }

  public void initialize(PlugInContext context) throws Exception {
    if (desktopPane == null)
      desktopPane = context.getWorkbenchFrame().getDesktopPane();

    for (int i = 1; i <= 4; i++) {
      context.getFeatureInstaller().addMainMenuPlugin(
          new ArrangeViewsPlugIn(i), new String[] { MenuNames.WINDOW });
    }
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);

    if (this.arrangeType != 0)
      tileFrames(this.arrangeType);

    return true;
  }

  public String getName() {
    String name = "";

    switch (this.arrangeType) {
    case 1:
      name = I18N
          .get("org.openjump.core.ui.plugin.window.ArrangeViewsPlugIn.distribute-views-horizontally");
      break;
    case 2:
      name = I18N
          .get("org.openjump.core.ui.plugin.window.ArrangeViewsPlugIn.distribute-views-vertically");
      break;
    case 3:
      name = I18N
          .get("org.openjump.core.ui.plugin.window.ArrangeViewsPlugIn.cascade-views");
      break;
    case 4:
      name = I18N
          .get("org.openjump.core.ui.plugin.window.ArrangeViewsPlugIn.arrange-views");
      break;
    default:
      name = NAME;
    }

    return name;
  }

  public static EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    MultiEnableCheck check = new MultiEnableCheck();
    check.add(checkFactory
        .createWindowWithAssociatedTaskFrameMustBeActiveCheck());

    return check;
  }

  private void tileFrames(int style) {
    Dimension deskDim = desktopPane.getSize();
    int deskWidth = deskDim.width;
    int deskHeight = deskDim.height;
    JInternalFrame[] frames = desktopPane.getAllFrames();
    int frameCount = frames.length;
    int frameWidth = 0;
    int frameHeight = 0;
    int xpos = 0;
    int ypos = 0;
    double scale = 0.6D;
    int spacer = 30;
    int frameCounter = 0;
    Vector frameVec = new Vector(1, 1);
    boolean areIcons = false;
    for (int i = 0; i < frameCount; i++) {
      if ((!frames[i].isResizable()) && (frames[i].isMaximum()))
        try {
          frames[i].setMaximum(false);
        } catch (PropertyVetoException localPropertyVetoException) {
        }
      if ((frames[i].isVisible()) && (!frames[i].isIcon())
          && (frames[i].isResizable())) {
        frameVec.addElement(frames[i]);
        frameCounter++;
      } else if (frames[i].isIcon()) {
        areIcons = true;
      }
    }
    if (areIcons)
      deskHeight -= 50;
    switch (style) {
    case 2:
      for (int i = 0; i < frameCounter; i++) {
        JInternalFrame temp = (JInternalFrame) frameVec.elementAt(i);
        frameWidth = deskWidth;
        frameHeight = deskHeight / frameCounter;
        temp.reshape(xpos, ypos, frameWidth, frameHeight);
        ypos += frameHeight;
        temp.moveToFront();
      }
      break;
    case 1:
      for (int i = 0; i < frameCounter; i++) {
        JInternalFrame temp = (JInternalFrame) frameVec.elementAt(i);
        frameWidth = deskWidth / frameCounter;
        frameHeight = deskHeight;
        if (temp.isResizable())
          temp.reshape(xpos, ypos, frameWidth, frameHeight);
        else
          temp.setLocation(xpos, ypos);
        xpos += frameWidth;
        temp.moveToFront();
      }
      break;
    case 3:
      for (int i = 0; i < frameCounter; i++) {
        JInternalFrame temp = (JInternalFrame) frameVec.elementAt(i);
        frameWidth = (int) (deskWidth * scale);
        frameHeight = (int) (deskHeight * scale);
        if (temp.isResizable())
          temp.reshape(xpos, ypos, frameWidth, frameHeight);
        else
          temp.setLocation(xpos, ypos);
        temp.moveToFront();
        xpos += spacer;
        ypos += spacer;
        if ((xpos + frameWidth > deskWidth)
            || (ypos + frameHeight > deskHeight - 50)) {
          xpos = 0;
          ypos = 0;
        }
      }
      break;
    case 4:
      int row = new Long(Math.round(Math.sqrt(frameCounter))).intValue();
      if (row == 0)
        break;
      int col = frameCounter / row;
      if (col == 0)
        break;
      int rem = frameCounter % row;
      int rowCount = 1;
      frameWidth = deskWidth / col;
      frameHeight = deskHeight / row;
      int i = 0;
      while (true) {
        JInternalFrame temp = (JInternalFrame) frameVec.elementAt(i);
        if (rowCount <= row - rem) {
          if (temp.isResizable())
            temp.reshape(xpos, ypos, frameWidth, frameHeight);
          else
            temp.setLocation(xpos, ypos);
          if (xpos + 10 < deskWidth - frameWidth) {
            xpos += frameWidth;
          } else {
            ypos += frameHeight;
            xpos = 0;
            rowCount++;
          }
        } else {
          frameWidth = deskWidth / (col + 1);
          if (temp.isResizable())
            temp.reshape(xpos, ypos, frameWidth, frameHeight);
          else
            temp.setLocation(xpos, ypos);
          if (xpos + 10 < deskWidth - frameWidth) {
            xpos += frameWidth;
          } else {
            ypos += frameHeight;
            xpos = 0;
          }
        }
        i++;
        if (i >= frameCounter) {
          break;
        }
      }
    }
  }

  public Icon getIcon() {
    Icon icon = null;
    switch (this.arrangeType) {
    case 1:
      icon = HORIZONTAL_ICON;
      break;
    case 2:
      icon = VERTICAL_ICON;
      break;
    case 3:
      icon = CASCADE_ICON;
      break;
    case 4:
      icon = ARRANGE_ICON;
      break;
    default:
      icon = null;
    }

    return icon;
  }

  public static class ArrangeHorizontalPlugIn extends ArrangeViewsPlugIn {
    public ArrangeHorizontalPlugIn() {
      super(1);
    }
  }

  public static class ArrangeVerticalPlugIn extends ArrangeViewsPlugIn {
    public ArrangeVerticalPlugIn() {
      super(2);
    }
  }

  public static class ArrangeCascadePlugIn extends ArrangeViewsPlugIn {
    public ArrangeCascadePlugIn() {
      super(3);
    }
  }

  public static class ArrangeAllPlugIn extends ArrangeViewsPlugIn {
    public ArrangeAllPlugIn() {
      super(4);
    }
  }
}
