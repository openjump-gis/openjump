package org.openjump.core.ui.plugin.file;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TooManyListenersException;

import javax.swing.BorderFactory;
import javax.swing.JRootPane;
import javax.swing.border.Border;

import org.openjump.core.ui.plugin.AbstractUiPlugIn;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class FileDragDropPlugin extends AbstractUiPlugIn implements
  DropTargetListener {
  public static final Set<String> PROJECT_EXTENSIONS = new HashSet<String>(
    Arrays.asList(new String[] {
      "jmp", "jcs"
    }));

  private static final String ZERO_CHAR_STRING = "" + (char)0;

  private WorkbenchFrame frame;

  private Border border = BorderFactory.createMatteBorder(2, 2, 2, 2,
    new Color(0f, 0f, 1f, 0.25f));

  private Border savedBorder;

  /**
   * Initialise plug-in.
   * 
   * @param context The plug-in context.
   * @exception Exception If there was an error initialising the plug-in.
   */
  public void initialize(final PlugInContext context) throws Exception {
    super.initialize(context);
    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    frame = workbench.getFrame();

    makeDropTarget(frame, true);

  }

  public void dragEnter(DropTargetDragEvent event) {
    if (isDragOk(event)) {
      JRootPane rootPane = frame.getRootPane();
      savedBorder = rootPane.getBorder();
      rootPane.setBorder(border);
      event.acceptDrag(DnDConstants.ACTION_COPY);
    } else {
      event.rejectDrag();
    }
  }

  public void dragExit(DropTargetEvent event) {
    JRootPane rootPane = frame.getRootPane();
    rootPane.setBorder(savedBorder);
  }

  public void dragOver(DropTargetDragEvent event) {
  }

  public void drop(DropTargetDropEvent event) {
    try {
      Transferable tr = event.getTransferable();
      List<File> files = null;
      if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        event.acceptDrop(DnDConstants.ACTION_COPY);

        files = (List)tr.getTransferData(DataFlavor.javaFileListFlavor);
      } else {
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        boolean handled = false;
        for (DataFlavor flavor : flavors) {
          if (flavor.isRepresentationClassReader()) {
            event.acceptDrop(DnDConstants.ACTION_COPY);

            BufferedReader reader = new BufferedReader(
              flavor.getReaderForText(tr));
            handled = true;
            files = new ArrayList<File>();
            String fileName = null;
            while ((fileName = reader.readLine()) != null) {
              try {
                // kde seems to append a 0 char to the end of the reader
                if (!ZERO_CHAR_STRING.equals(fileName)) {

                  File file = new File(new URI(fileName));
                  files.add(file);
                }
              } catch (java.net.URISyntaxException e) {
                ErrorHandler errorHandler = workbenchContext.getErrorHandler();
                errorHandler.handleThrowable(e);
              }
            }
          }
        }
        if (!handled) {
          event.rejectDrop();
          return;
        }
      }
      if (files != null) {
        List<File> projectFiles = new ArrayList<File>();
        List<File> dataFiles = new ArrayList<File>();
        for (File file : files) {
          String extension = GUIUtil.getExtension(file);
          if (PROJECT_EXTENSIONS.contains(extension)) {
            projectFiles.add(file);
          } else {
            dataFiles.add(file);
          }
        }

        if (!dataFiles.isEmpty()) {
          OpenFilePlugIn filePlugin = new OpenFilePlugIn(workbenchContext,
            dataFiles.toArray(new File[0]));
          filePlugin.actionPerformed(new ActionEvent(this, 0, ""));
        }

        if (!projectFiles.isEmpty()) {
          OpenProjectPlugIn projectPlugin = new OpenProjectPlugIn(
            workbenchContext, projectFiles.toArray(new File[0]));
          projectPlugin.actionPerformed(new ActionEvent(this, 0, ""));
        }
      }
      event.getDropTargetContext().dropComplete(true);
    } catch (UnsupportedFlavorException e) {
      ErrorHandler errorHandler = workbenchContext.getErrorHandler();
      errorHandler.handleThrowable(e);
    } catch (IOException e) {
      ErrorHandler errorHandler = workbenchContext.getErrorHandler();
      errorHandler.handleThrowable(e);
    } finally {
      JRootPane rootPane = frame.getRootPane();
      rootPane.setBorder(savedBorder);
    }
  }

  public void dropActionChanged(DropTargetDragEvent event) {
    if (isDragOk(event)) {
      event.acceptDrag(DnDConstants.ACTION_COPY);
    } else {
      event.rejectDrag();
    }
  }

  private boolean isDragOk(final DropTargetDragEvent event) {
    DataFlavor[] flavors = event.getCurrentDataFlavors();
    for (DataFlavor flavor : flavors) {
      if (flavor.equals(DataFlavor.javaFileListFlavor)
        || flavor.isRepresentationClassReader()) {
        return true;
      }
    }
    return false;
  }

  private void makeDropTarget(final Component component, boolean recursive) {
    final DropTarget target = new DropTarget();
    try {
      target.addDropTargetListener(this);
    } catch (TooManyListenersException e) {
      ErrorHandler errorHandler = workbenchContext.getErrorHandler();
      errorHandler.handleThrowable(e);
    }

    component.addHierarchyListener(new java.awt.event.HierarchyListener() {
      public void hierarchyChanged(java.awt.event.HierarchyEvent evt) {
        java.awt.Component parent = component.getParent();
        if (parent == null) {
          component.setDropTarget(null);
        } else {
          new DropTarget(component, FileDragDropPlugin.this);
        }
      }
    });
    if (component.getParent() != null) {
      new DropTarget(component, this);
    }

    if (recursive && (component instanceof Container)) {
      Container cont = (Container)component;

      Component[] comps = cont.getComponents();
      for (Component child : comps) {
        makeDropTarget(child, recursive);
      }
    }
  }
}
