/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
package com.vividsolutions.jump.workbench.ui.cursortool;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomTool;

import java.awt.Cursor;
import java.awt.event.*;

import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.openjump.core.CheckOS;


/**
 * Delegates to different CursorTools depending on whether various modifier
 * keys are pressed (Ctrl, Shift, Alt). The term "quasimode" refers to a mode that
 * is only in existence as long as a key is held down -- the mode vanishes as soon
 * as the key is released. For more information, see the book "Humane Interfaces"
 * by Jef Raskin.
 */
public class QuasimodeTool extends DelegatingTool {

    //Sometimes when I try to use the Alt quasimode, the cursor becomes the default
    //cursor (arrow) and stays that way. This seems to have been fixed in JDK 1.4,
    //in which the default cursor stays only for a split second. [Jon Aquino]
    public QuasimodeTool(CursorTool defaultTool) {
        super(defaultTool);
        add(new ModifierKeySpec(false, false, false), defaultTool);
    }

    private CursorTool getDefaultTool() {
        return (CursorTool) keySpecToToolMap.get(new ModifierKeySpec(false, false, false));
    }

    private CursorTool getTool(KeyEvent e) {
        CursorTool tool =
            (CursorTool) keySpecToToolMap.get(
                new ModifierKeySpec(
                    e.isControlDown(),
                    e.isShiftDown(),
                    e.isAltDown() || e.isMetaDown()));
        return tool != null ? tool : getDefaultTool();
    }

    private KeyListener keyListener = new KeyListener() {
        private KeyEvent previous = null;
        private boolean previous_pressed = false;

        public void keyTyped(KeyEvent e) {}

        public void keyPressed(KeyEvent e) {
            keyStateChanged(e, true);
        }

        public void keyReleased(KeyEvent e) {
            keyStateChanged(e, false);
        }

      private void keyStateChanged(KeyEvent e, boolean pressed) {
        // filter out duplicate events (e.g. key stays pressed)
        if (previous != null && e.getKeyCode() == previous.getKeyCode()
            && previous_pressed == pressed)
          return;
        previous = e;
        previous_pressed = pressed;
        //altKeyDown = e.isAltDown();
        setTool(e);
      }
    };
//    /*
//    * [sstein: 17.Mar2007] added the Listener, to be able to remove the listener 
//    * if the cursortool is deactivated. Otherwise the listener still exists and
//    * the mousepointer is "flickering" through all previously used mouse-tools
//    * The modifications (see also #activate and #deactivate) are proposed by Bob and Larry
//    * [ede 12.2012] commented, see reasoning above in activate()
//    */
//    private WindowAdapter windowListener = new WindowAdapter()
//    {
//        public void windowActivated(WindowEvent e) {
//			super.windowActivated(e);
//			setTool(new KeyEvent(panel, KeyEvent.KEY_PRESSED, 
//                0, 0, KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED));
//        }
//    };
    
    private void setTool(KeyEvent e) {
      setDelegate(getTool(e));
      panel.setCursor(getCursor());
    }

    private LayerViewPanel panel;
    private WorkbenchFrame frame;

    public void activate(final LayerViewPanel panel) {
        super.activate(panel);
        this.panel = panel;
        //Cache WorkbenchFrame because in JDK 1.3 when I minimize an internal
        //frame, SwingUtilities#windowForComponent returns null for that frame.
        //A Swing bug. [Jon Aquino]
        frame = AbstractCursorTool.workbenchFrame(panel);
        if (frame != null) {
            frame.addEasyKeyListener(keyListener);
            //Workaround for the following:
            // * Use WorkbenchFrame#addKeyboardShortcut for a plug-in that
            //   pops up a dialog (or could pop up an error dialog). Assign it to Ctrl-A,
            //   for example.
            // * Press Ctrl-A. The Ctrl quasimode happens. But also the dialog pops up.
            // * Release Ctrl. Close the dialog. Note that the cursor shows we're still 
            //    in the Ctrl quasimode! This is because the dialog consumed the
            //    key-up event.
            //So we're working around this by clearing the quasimode when the
            //WorkbenchFrame is activated (e.g. when a dialog is closed). [Jon Aquino]
            
            //-- [sstein : ] deactivated and repalced by line above see comment 
            //	             on windowListener above
            // [ede 12.2012] deactivated as it interferes with QuasiModeTools switched
            // by key pressed while toolbox is still active, tool got switched but was
            // reset to the default tool by the next line
            //frame.addWindowListener(windowListener);
            
            //Need to do the following so that the delegate gets set to the LeftClickFilter
            //This fixes a problem where the selection was being lost on a right click
            //when using the selector tool on the tool bar.
            //The following code is executed during windowActivated event in WindowAdapter windowListener above.
            //This is why the problem fixed itself when returning to the window after bringing
            //another application forward.
            //This event was done when clicking the selection tool on the toolbox so
            //that is why it was always working.
            // [ede 12.2012] deactivated, keep tool even if we loose focus
            // setTool(new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0,
            //      KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED));

        }
    }

    public void deactivate() {
      // [ede 12.2012] deactivated, as it makes no sense not to deactivate the tool if ALT is not pressed
      // if (!altKeyDown)
      // {
      super.deactivate();
      if (frame != null) {
        frame.removeEasyKeyListener(keyListener);
        // [ede 12.2012] deactivated, keep tool even if we loose focus
        //frame.removeWindowListener(windowListener);
      }
      // }
    }

    private HashMap keySpecToToolMap = new HashMap();

    public QuasimodeTool add(ModifierKeySpec keySpec, CursorTool tool) {
        if (keySpecToToolMap.containsKey(keySpec)) {
            return this;
        }
        keySpecToToolMap.put(
            keySpec,
            tool != null
                ? (tool.isRightMouseButtonUsed() ? tool : new LeftClickFilter(tool))
                : null);
        return this;
    }

    public static class ModifierKeySpec {
        public ModifierKeySpec(boolean needsControl, boolean needsShift, boolean needsAltOrMeta) {
            this.needsControl = needsControl;
            this.needsShift = needsShift;
            this.needsAltOrMeta = needsAltOrMeta;
        }
        private boolean needsShift, needsAltOrMeta, needsControl;
        public int hashCode() {
            //Map will be small anyway. [Jon Aquino]
            return 0;
        }
        public boolean equals(Object obj) {
            if (!(obj instanceof ModifierKeySpec)) {
                return false;
            }
            ModifierKeySpec other = (ModifierKeySpec) obj;
            return needsControl == other.needsControl
                && needsShift == other.needsShift
                && needsAltOrMeta == other.needsAltOrMeta;
        }
    }

    public static QuasimodeTool addStandardQuasimodes(CursorTool tool) {
        QuasimodeTool quasimodeTool =
            tool instanceof QuasimodeTool ? (QuasimodeTool) tool : new QuasimodeTool(tool);
        quasimodeTool.add(new ModifierKeySpec(false, false, true), new ZoomTool());
        quasimodeTool.add(new ModifierKeySpec(false, true, true), new PanTool());
        SelectFeaturesTool selectFeaturesTool = new SelectFeaturesTool() {
            protected boolean selectedLayersOnly() {
                return false;
            }
        };
        //using Shift we can actually add to the selction or deselct
        quasimodeTool.add(new ModifierKeySpec(true, false, false), selectFeaturesTool);
        quasimodeTool.add(new ModifierKeySpec(true, true, false), selectFeaturesTool);
        //[sstein 7.Nov.2011] we do not want the tool on MacOSX as the mouse pointer change
        //    	 interferes with the Edit Feature Context menu
        if(CheckOS.isMacOsx() == false){
        	quasimodeTool.add(new ModifierKeySpec(true, false, true), new FeatureInfoTool());
        }
        return quasimodeTool;
    }

}
