package org.openjump.core.ui.plugin.file.open;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import com.vividsolutions.jump.workbench.ui.RecursiveKeyListener;

/**
 * Since jdk7, if control buttons are invisible, the chooser does not
 * auto approve (incl. globbing, chdir) on enter key anymore.
 * This helper class is meant to mitigate this.
 * 
 * @author ed
 *
 */
public class JFCWithEnterAction extends JFileChooser{

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
  
}
