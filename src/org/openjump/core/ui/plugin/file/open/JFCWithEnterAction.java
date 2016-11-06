package org.openjump.core.ui.plugin.file.open;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.RecursiveKeyListener;

/**
 * Since jdk7, if control buttons are invisible, the chooser does not auto
 * approve (incl. globbing, chdir) on enter key anymore. This helper class is
 * meant to mitigate this.
 * 
 * @author ed
 *
 */
public class JFCWithEnterAction extends JFileChooser {

  public JFCWithEnterAction() {
    super();
    addKeyListener(new RecursiveKeyListener(this) {
      public void keyTyped(KeyEvent e) {
      }

      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          FileChooserUI ui = getUI();
          // emulate the action that is usually performed on the approve button
          if (ui instanceof BasicFileChooserUI) {
            BasicFileChooserUI bui = (BasicFileChooserUI) ui;
            bui.getApproveSelectionAction().actionPerformed(
                new ActionEvent(new JButton(), 0, "nix"));
          }
        }
      }

      public void keyReleased(KeyEvent e) {
      }
    });
  }

  /**
   * Work around Java Bug 4437688 "JFileChooser.getSelectedFile() returns
   * nothing when a file is selected" [Jon Aquino]
   */
  public File getSelectedFile() {
    File file;
    
    file = super.getSelectedFile();
    if (file != null)
      return file;

    File[] files = super.getSelectedFiles();
    if (files.length > 0)
      return files[0];

//    // if none was selected but there is text in the filename field
//    FileChooserUI ui = getUI();
//    // fetch a filename if manually entered in file name text field of chooser
//    if (ui instanceof BasicFileChooserUI) {
//      BasicFileChooserUI bui = (BasicFileChooserUI) ui;
//      String filename = ((BasicFileChooserUI) ui).getFileName();
//      if (!filename.isEmpty()) {
//        return new File(getCurrentDirectory(), filename);
//      }
//    }

    // a little more generic than the above
    try {
      Method getFileName = getUI().getClass().getDeclaredMethod("getFileName");
      String filename = (String) getFileName.invoke(getUI());
      if (filename != null && !filename.isEmpty()) {
        file = new File(filename);
        if (!file.isAbsolute())
          file = new File(getCurrentDirectory(), filename);
        return file;
      }
    } catch (Exception e) {
      Logger.error(e);
    }

    // ok, we give up! nothing was selected at all :)
    return null;
  }

  public File[] getSelectedFiles() {
    return ((super.getSelectedFiles().length == 0) && (super.getSelectedFile() != null)) ? new File[] { getSelectedFile() } : super.getSelectedFiles();
  }

}
