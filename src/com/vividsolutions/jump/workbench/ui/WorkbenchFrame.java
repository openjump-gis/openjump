/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 *
 * (250)385-6040 www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultDesktopManager;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.openjump.core.CheckOS;
import org.openjump.core.model.TaskEvent;
import org.openjump.core.model.TaskListener;
import org.openjump.core.ui.plugin.view.ZoomToScalePlugIn;
import org.openjump.core.ui.swing.DetachableInternalFrame;
import org.openjump.core.ui.util.ScreenScale;
import org.openjump.swing.factory.component.ComponentFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.model.UndoableEditReceiver;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableChecked;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.SaveProjectPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ViewAttributesPlugIn.ViewAttributesFrame;
import com.vividsolutions.jump.workbench.ui.renderer.style.ChoosableStyle;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToCoordinatePlugIn;

/**
 * This class is responsible for the main window of the JUMP application.
 */
public class WorkbenchFrame extends JFrame implements LayerViewPanelContext,
        ViewportListener, ErrorHandler {

    private String EXIT_OPENJUMP = I18N.get("ui.WorkbenchFrame.exit-jump");
    private String CLOSE_PROJECT = I18N.get("ui.WorkbenchFrame.close-task");
    private String CLOSE_PROJECT_QUESTION = I18N
            .get("ui.WorkbenchFrame.do-you-want-to-close-project");
    private String SAVE_PROJECT = I18N
            .get("ui.WorkbenchFrame.save-project-before-closing-openjump");
    private String PROJECT_SAVED = I18N
            .get("ui.WorkbenchFrame.save-project-saved");
    // To translate
    public static String MEMORY_GC = I18N
            .get("ui.WorkbenchFrame.started-garbage-collection");
    public static String MEMORY_TIPS = I18N
            .get("ui.WorkbenchFrame.click-to-garbage-collector");
    public static String COORDINATES = I18N
            .get("ui.WorkbenchFrame.coordinates");
    public static String COORDINATES_TIPS = I18N
            .get("ui.WorkbenchFrame.click-to-zoom-to-coordinates");
    public static String SCALE = I18N.get("ui.WorkbenchFrame.scale");
    public static String SCALE_TIPS = I18N
            .get("ui.WorkbenchFrame.click-to-change-view-scale");

    BorderLayout borderLayout1 = new BorderLayout();

    JMenuBar menuBar = new JMenuBar() {
        public void processKeyEvent(KeyEvent e, MenuElement[] path,
                MenuSelectionManager manager) {
            super.processKeyEvent(e, path, manager);
        }

        // filter how we react on specific key events
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                int condition, boolean pressed) {
            // catch Alt release events which cause menubar to get focussed
            // during editing when quasimodes are used
            if (e.getKeyCode() == KeyEvent.VK_ALT
                    && e.getID() == KeyEvent.KEY_RELEASED)
                return true;
            // this disables the menu item accelerators to be executed
            // we simply do not let menubar route the key down to the items
            return false; // super.processKeyBinding(ks, e, condition, pressed);
        }
    };

    JMenu fileMenu = (JMenu) FeatureInstaller.installMnemonic(new JMenu(
            MenuNames.FILE), menuBar);

    public class ExitPlugin extends AbstractPlugIn {
        public ExitPlugin() {
            super(I18N.get("ui.WorkbenchFrame.exit"));
            this.setShortcutKeys(KeyEvent.VK_Q);
            this.setShortcutModifiers(KeyEvent.CTRL_MASK);
        }

        public void initialize(PlugInContext context) throws Exception {
            super.initialize(context);
            context.getFeatureInstaller().addMainMenuPlugin(this,
                    new String[] { MenuNames.FILE });
            // register and block menu accelerator action
            registerShortcuts(this);
        }

        public boolean execute(PlugInContext context) throws Exception {
            closeApplication();
            return true;
        }

        public Icon getIcon() {
            return IconLoader.icon("famfam/door_in.png");
        }
    };

    private TaskFrame activeTaskFrame = null;

    // StatusBar
    private JPanel statusPanel;
    private JTextArea messageText;
    private JLabel timeLabel, memoryLabel, scaleLabel, coordinateLabel;
    private DecimalFormat scaleFormat = new DecimalFormat("#,###");
    private DecimalFormat scaleFormatSmall = new DecimalFormat("#.####");
    private ZoomToScalePlugIn zoomToScalePlugin = new ZoomToScalePlugIn();
    private ZoomToCoordinatePlugIn zoomToCoordPlugin = new ZoomToCoordinatePlugIn();

    private String lastStatusMessage = "";

    // the four SplitPanes for the statusbar
    private JSplitPane statusPanelSplitPane1;
    private JSplitPane statusPanelSplitPane2;
    private JSplitPane statusPanelSplitPane3;
    private JSplitPane statusPanelSplitPane4;

    // the vertically resizeable statusbar
    private JSplitPane desktopStatusSplit;

    WorkbenchToolBar toolBar;

    private static Icon internalFrameDefaultIcon = null;
    private static boolean internalFrameDefaultIconSet = false;

    JMenu windowMenu = (JMenu) FeatureInstaller.installMnemonic(new JMenu(
            MenuNames.WINDOW), menuBar);

    private DecimalFormat memoryFormat = new DecimalFormat("###,###");

    private TitledPopupMenu categoryPopupMenu = new TitledPopupMenu() {
        {
            addPopupMenuListener(new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    LayerNamePanel panel = ((LayerNamePanelProxy) getActiveInternalFrame())
                            .getLayerNamePanel();
                    setTitle((panel.selectedNodes(Category.class).size() != 1) ? ("("
                            + panel.selectedNodes(Category.class).size() + " categories selected)")
                            : ((Category) panel.selectedNodes(Category.class)
                                    .iterator().next()).getName());
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }
    };

    private JDesktopPane desktopPane = new JDesktopPane() {
        {
            // Simple workaround for the following JUMP bug: if you maximize one
            // JInternalFrame, then all
            // JInternalFrames get maximized (including attribute windows,
            // undesirably). The workaround is
            // to use the DefaultDesktopManager instead of the one installed by
            // the
            // Windows L&F
            // (the WindowsDesktopManager). (Uwe Dalluege noticed that the
            // problem
            // occurred with the
            // Windows L&F but not the Metal L&F -- this observation led me to
            // the
            // solution).
            // [Jon Aquino 2005-07-04]
            setDesktopManager(new DefaultDesktopManager());
        }
    };

    // <<TODO:REMOVE>> Actually we're not using the three optimization
    // parameters
    // below. Remove. [Jon Aquino]
    private int envelopeRenderingThreshold = 500;

    private HTMLFrame outputFrame = null;

    private TitledPopupMenu layerNamePopupMenu = new TitledPopupMenu() {
        {
            addPopupMenuListener(new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    LayerNamePanel panel = ((LayerNamePanelProxy) getActiveInternalFrame())
                            .getLayerNamePanel();
                    setTitle((panel.selectedNodes(Layer.class).size() != 1) ? ("("
                            + panel.selectedNodes(Layer.class).size()
                            + " "
                            + I18N.get("ui.WorkbenchFrame.layers-selected") + ")")
                            : ((Layerable) panel.selectedNodes(Layer.class)
                                    .iterator().next()).getName());
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }
    };

    private TitledPopupMenu wmsLayerNamePopupMenu = new TitledPopupMenu() {
        {
            addPopupMenuListener(new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    LayerNamePanel panel = ((LayerNamePanelProxy) getActiveInternalFrame())
                            .getLayerNamePanel();
                    setTitle((panel.selectedNodes(WMSLayer.class).size() != 1) ? ("("
                            + panel.selectedNodes(WMSLayer.class).size()
                            + " "
                            + I18N.get("ui.WorkbenchFrame.wms-layers-selected") + ")")
                            : ((Layerable) panel.selectedNodes(WMSLayer.class)
                                    .iterator().next()).getName());
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }
    };

    private LayerNamePanelListener layerNamePanelListener = new LayerNamePanelListener() {
        public void layerSelectionChanged() {
            toolBar.updateEnabledState();
        }
    };

    // Here is a small patch to JUMP to avoid creating a StringBuffer every
    // coordinate change (which could be many thousands). Replace the innter
    // class in WorkbenchFrame.java with the following. I am assuming only one
    // thread can call the listener at a time. If that is untrue please
    // synchronize
    // cursorPositionChanged().
    //
    // Sheldon Young 2004-01-30
    private LayerViewPanelListener layerViewPanelListener = new LayerViewPanelListener() {
        // Avoid creating an expensive StringBuffer when the cursor position
        // changes.
        private StringBuffer positionStatusBuf = new StringBuffer("(");

        public void cursorPositionChanged(String x, String y) {
            positionStatusBuf.setLength(1);
            positionStatusBuf.append(x).append(" ; ").append(y).append(")");
            coordinateLabel.setText(positionStatusBuf.toString());
            coordinateLabel.setToolTipText("<html><body>"
                    + GUIUtil.escapeHTML(COORDINATES + ": "
                            + positionStatusBuf.toString()) + "<br><br>"
                    + GUIUtil.escapeHTML(COORDINATES_TIPS) + "</body></html>");
        }

        public void selectionChanged() {
            toolBar.updateEnabledState();
        }

        public void fenceChanged() {
            toolBar.updateEnabledState();
        }

        public void painted(Graphics graphics) {
        }
    };

    // <<TODO:NAMING>> This name is not clear [Jon Aquino]
    private int maximumFeatureExtentForEnvelopeRenderingInPixels = 10;

    // <<TODO:NAMING>> This name is not clear [Jon Aquino]
    private int minimumFeatureExtentForAnyRenderingInPixels = 2;

    // private StringBuffer log = new StringBuffer();

    private int taskSequence = 1;

    private WorkbenchContext workbenchContext;

    private Set choosableStyleClasses = new HashSet();

    private ArrayList<TaskListener> taskListeners = new ArrayList<TaskListener>();

    private Map nodeClassToLayerNamePopupMenuMap = CollectionUtil
            .createMap(new Object[] { Layer.class, layerNamePopupMenu,
                    WMSLayer.class, wmsLayerNamePopupMenu, Category.class,
                    categoryPopupMenu });

    private int positionIndex = -1;

    private int primaryInfoFrameIndex = -1;

    private int addedMenuItems = -1;

    private ComponentFactory<TaskFrame> taskFrameFactory;

    // RecursiveKeyListener easyKeyListener;
    MultiRecursiveKeyListener easyKeyListener;
    ShortcutPluginExecuteKeyListener shortcutListener;

    public WorkbenchFrame(String title, final WorkbenchContext workbenchContext)
            throws Exception {
        setTitle(title);

        this.workbenchContext = workbenchContext;

        // set icon for the app frame
        JUMPWorkbench.setIcon(this);

        // prevent loosing windows when frame is resized,
        // resize and move them back into if needed
        getDesktopPane().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for (JInternalFrame iframe : getInternalFrames()) {
                    legalize(iframe);
                }
            }
        });

        toolBar = new WorkbenchToolBar(workbenchContext);
        toolBar.setTaskMonitorManager(new TaskMonitorManager());
        try {
            jbInit();
            configureStatusBarComponent(messageText, 90);
            configureStatusBarComponent(timeLabel, 90);
            configureStatusBarComponent(memoryLabel, 90);
            configureStatusBarComponent(scaleLabel, 90); // [Giuseppe Aruta
                                                         // 2012-feb-18]
            configureStatusBarComponent(coordinateLabel, 90);

        } catch (Exception e) {
            e.printStackTrace();
        }

        new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String msg = getMBCommittedMemory() + " MB "
                        + I18N.get("ui.WorkbenchFrame.committed-memory");
                memoryLabel.setText(msg);
                memoryLabel.setToolTipText("<html><body>"
                        + GUIUtil.escapeHTML(msg) + "<br><br>"
                        + GUIUtil.escapeHTML(MEMORY_TIPS) + "</body></html>");

                // memoryLabel.setToolTipText(LayerManager.layerManagerCount() +
                // " "
                // + I18N.get("ui.WorkbenchFrame.layer-manager")
                // + StringUtil.s(LayerManager.layerManagerCount()));
            }
        }).start();

        // attach a multi listener (plugins use to get notified about key events
        // etc.)
        easyKeyListener = new MultiRecursiveKeyListener(this);
        // create a run plugin via shortcut listener
        shortcutListener = new ShortcutPluginExecuteKeyListener(
                workbenchContext);

        // these register handlers for mac menus, needs apple
        // stubs in lib/orange*.jar in classpath to compile
        if (CheckOS.isMacOsx()) {
            try {
                new AppleHandler().register();
            } catch (Throwable e) {
                // the whole handling above is optional, inform but don't fail
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                log(sw.toString());
            }
        }

        // intercept global key events via custom dispatcher, see
        // http://tips4java.wordpress.com/2009/09/06/global-event-dispatching/
        // this is a HACK. we intercept registered shortcuts here because
        // - this way we get keyinput regardless which component has focus
        // - menu item accelerators are disabled in menubar, we execute them
        // here
        // - we block ALT-char combinations which can be menu mnemonics by
        // consuming
        // the event here if the combination is actually an registered shortcut
        // TODO: we should check/reassign mnemonics on every shortcut assignment
        // to prevent dead mnemonics
        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                // hands up, who's got the focus
                Component focussed = KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getFocusOwner();
                // traverse through parents, see if we are in a valid one
                boolean valid = false;
                // System.out.println("START");
                Component c = focussed;
                while (c != null) {
                    // System.out.println(c.getClass());

                    // ignore statuspanel,
                    // copy/paste is available via textfield's own keyboard
                    // actions
                    if (c.equals(statusPanel)) {
                        break;
                    }

                    // ignore LayerTree textfields, we have currently no plugin
                    // w/ shortcuts
                    // doing anything w/ it, preserving layer name editing
                    // shortcuts this way
                    if (c instanceof TreeLayerNamePanel
                            && focussed instanceof JTextComponent) {
                        break;
                    }

                    if (c instanceof TaskFrame
                            || (c instanceof ToolboxDialog && c
                                    .equals(EditingPlugIn.getInstance()
                                            .getToolbox()))
                            || c instanceof WorkbenchFrame) {
                        valid = true;
                        break;
                    }

                    // we treat windows and jinternalframes as our ultimate
                    // parent, as pretty much everything would end up with
                    // WorkbenchFrame as parent in the end
                    if (c instanceof Window || c instanceof JInternalFrame)
                        break;

                    c = c.getParent();
                }

                // if we are not in one of the containers above
                // we do not use global shortcuts
                if (!valid) {
                    return false; // nothing dispatched
                }

                // TODO: eventually the whole filtering above should be
                // moved into ShortcutPluginExecuteKeyListener where plugins
                // define focussed ui components they feel responsible for
                switch (e.getID()) {
                case KeyEvent.KEY_PRESSED:
                    shortcutListener.keyPressed(e);
                    break;
                case KeyEvent.KEY_RELEASED:
                    shortcutListener.keyReleased(e);
                    break;
                case KeyEvent.KEY_TYPED:
                    shortcutListener.keyTyped(e);
                    break;
                }
                // signaling we dispatched it, or not
                return e.isConsumed();
            }
        };

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(dispatcher);
    }

    /**
     * Unlike #add(KeyListener), listeners registered using this method are
     * notified when KeyEvents occur on this frame's child components. Note:
     * Bug: KeyListeners registered using this method may receive events
     * multiple times.
     * 
     * @see #addKeyboardShortcut
     */
    public void addEasyKeyListener(KeyListener l) {
        // System.out.println("add "+l);
        // easyKeyListeners.add(l);
        easyKeyListener.addKeyListener(l);
    }

    public void removeEasyKeyListener(KeyListener l) {
        // System.out.println("rem "+l);
        // easyKeyListeners.remove(l);
        easyKeyListener.removeKeyListener(l);
    }

    public void addEasyKeyListenerToComp(Component c) {
        easyKeyListener.listenTo(c);
    }

    public void removeEasyKeyListenerFromComp(Component c) {
        easyKeyListener.ignore(c);
    }

    public String getMBCommittedMemory() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usedMemoryInMB = usedMemory / (1024 * 1024d);
        String memoryStr = memoryFormat.format(usedMemoryInMB);
        return memoryStr;
    }

    /**
     * @param newEnvelopeRenderingThreshold
     *            the number of on-screen features above which envelope
     *            rendering should occur
     */
    public void setEnvelopeRenderingThreshold(int newEnvelopeRenderingThreshold) {
        envelopeRenderingThreshold = newEnvelopeRenderingThreshold;
    }

    public void setMaximumFeatureExtentForEnvelopeRenderingInPixels(
            int newMaximumFeatureExtentForEnvelopeRenderingInPixels) {
        maximumFeatureExtentForEnvelopeRenderingInPixels = newMaximumFeatureExtentForEnvelopeRenderingInPixels;
    }

    /**
     * backward compatibility log method for legacy code
     * @deprecated - use com.vividsolutions.jump.workbench.Logger instead
     */
    @Deprecated
    public void log(String message) {
        log(message, null, new Exception().getStackTrace()[0]);
    }

    /**
     * backward compatibility log method for legacy code.
     * NOTE: parameter clazz is ignored
     * @deprecated - use com.vividsolutions.jump.workbench.Logger instead
     */
    @Deprecated
    public void log(String message, Class clazz) {
        log(message, null, new Exception().getStackTrace()[0]);
    }

    /**
     * backward compatibility log method for legacy code
     * @deprecated - use com.vividsolutions.jump.workbench.Logger instead
     */
    @Deprecated
    public void log(String message, Throwable t) {
        log(message, t, new Exception().getStackTrace()[0]);
    }

    private void log(String message, Throwable t, StackTraceElement calledFrom) {
        Logger.log(message, Logger.isDebugEnabled() ? t : null, Level.INFO, calledFrom);
    }

    public void setMinimumFeatureExtentForAnyRenderingInPixels(
            int newMinimumFeatureExtentForAnyRenderingInPixels) {
        minimumFeatureExtentForAnyRenderingInPixels = newMinimumFeatureExtentForAnyRenderingInPixels;
    }

    public void displayLastStatusMessage() {
        setStatusMessage(lastStatusMessage);
    }

    public void setStatusMessage(String message) {
        setStatusMessage(message, null);
    }

    // set message for a time only
    public void setStatusMessage(String message, int millis) {
        setStatusMessage(message, millis, null, false);
    }

    public void setStatusMessage(String message, Color color) {
        lastStatusMessage = message;
        setStatusBarText(message);
        boolean high = color != null;
        setStatusBarTextHighlighted(high, color);
    }

    Thread thread = null;

    public void setStatusMessage(final String msg, final int millis,
            final Color color, final boolean flash) {
        if (thread != null) {
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {
            public void run() {
                if (flash)
                    flashStatusMessage(msg, color);
                else {
                    setStatusMessage(msg, color);
                }
                if (millis > 0) {
                    try {
                        Thread.sleep(millis);
                    } catch (Exception e) {
                    }
                    if (getStatusBarText().equals(msg))
                        setStatusMessage("");
                }
            }
        });
        thread.start();
    }

    private void setStatusBarText(String message) {
        // Make message at least a space so that the label won't collapse [Jon
        // Aquino]
        // message = (message == null || message.equals("")) ? " " : message;
        messageText.setText(message);
        messageText.setToolTipText("<html><body>"
                + GUIUtil.escapeHTML(message)
                + "<br><br>"
                + GUIUtil.escapeHTML(I18N
                        .get("ui.WorkbenchFrame.copy-to-clipboard"))
                + "</body></html>");
    }

    private String getStatusBarText() {
        return messageText.getText();
    }

    /**
     * To highlight a message, call #warnUser.
     */
    private void setStatusBarTextHighlighted(boolean highlighted, Color color) {
        // Use #coordinateLabel rather than (unattached) dummy label because
        // dummy label's background does not change when L&F changes. [Jon
        // Aquino]
        messageText.setForeground(highlighted ? Color.black : coordinateLabel
                .getForeground());
        messageText.setBackground(highlighted ? color : coordinateLabel
                .getBackground());
    }

    public void setTimeMessage(String message) {
        // Make message at least a space so that the label won't collapse [Jon
        // Aquino]
        message = (message == null || message.equals("")) ? " " : message;
        timeLabel.setText(message);
        timeLabel.setToolTipText(message);
    }

    // Add new JLabel for scale //
    // [Giuseppe Aruta 2012-feb-18] //
    public void setScaleText(String message) {
        // Make message at least a space so that the label won't collapse [Jon
        // Aquino]
        message = (message == null || message.equals("")) ? " " : message;
        scaleLabel.setText(message);
        scaleLabel.setToolTipText("<html><body>"
                + GUIUtil.escapeHTML(SCALE + " " + message) + "<br><br>"
                + GUIUtil.escapeHTML(SCALE_TIPS) + "</body></html>");
    }

    // make really extra sure message textarea looks like the jlabel
    private boolean init_message_lnf = false;

    public void setVisible(boolean b) {
        if (b && !init_message_lnf) {
            messageText.setFont(coordinateLabel.getFont());
            messageText.setBackground(coordinateLabel.getBackground());
            messageText.setForeground(coordinateLabel.getForeground());
            init_message_lnf = true;
        }
        super.setVisible(b);
    }

    public JInternalFrame getActiveInternalFrame() {
        return desktopPane.getSelectedFrame();
    }

    public JInternalFrame[] getInternalFrames() {
        return desktopPane.getAllFrames();
    }

    public TitledPopupMenu getCategoryPopupMenu() {
        return categoryPopupMenu;
    }

    public WorkbenchContext getContext() {
        return workbenchContext;
    }

    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    public int getEnvelopeRenderingThreshold() {
        return envelopeRenderingThreshold;
    }

    public TitledPopupMenu getLayerNamePopupMenu() {
        return layerNamePopupMenu;
    }

    public TitledPopupMenu getWMSLayerNamePopupMenu() {
        return wmsLayerNamePopupMenu;
    }

    public LayerViewPanelListener getLayerViewPanelListener() {
        return layerViewPanelListener;
    }

    public Map getNodeClassToPopupMenuMap() {
        return nodeClassToLayerNamePopupMenuMap;
    }

    public LayerNamePanelListener getLayerNamePanelListener() {
        return layerNamePanelListener;
    }

    public int getMaximumFeatureExtentForEnvelopeRenderingInPixels() {
        return maximumFeatureExtentForEnvelopeRenderingInPixels;
    }

    public int getMinimumFeatureExtentForAnyRenderingInPixels() {
        return minimumFeatureExtentForAnyRenderingInPixels;
    }

    public HTMLFrame getOutputFrame() {
        if (outputFrame == null)
            outputFrame = new HTMLFrame(this) {
                public void setTitle(String title) {
                    // Don't allow the title of the output frame to be changed.
                }

                {
                    super.setTitle(I18N.get("ui.WorkbenchFrame.output"));
                }
            };
        return outputFrame;
    }

    public WorkbenchToolBar getToolBar() {
        return toolBar;
    }

    public void activateFrame(JInternalFrame frame) {
        try {
            if (frame.isIcon()) {
                frame.setIcon(false);
            }
            frame.moveToFront();
            frame.requestFocus();
            frame.setSelected(true);
            if (!(frame instanceof TaskFrame)) {
                frame.setMaximum(false);
            }
        } catch (PropertyVetoException e) {
            warnUser(StringUtil.stackTrace(e));
        }
    }

    /**
     * If internalFrame is a LayerManagerProxy, the close behaviour will be
     * altered so that the user is prompted if it is the last window on the
     * LayerManager.
     */
    public void addInternalFrame(final JInternalFrame internalFrame) {
        addInternalFrame(internalFrame, false, true);
    }

    public void addInternalFrame(final JInternalFrame internalFrame,
            boolean alwaysOnTop, boolean autoUpdateToolBar) {
        if (internalFrame instanceof LayerManagerProxy) {
            setClosingBehaviour((LayerManagerProxy) internalFrame);
            installTitleBarModifiedIndicator((LayerManagerProxy) internalFrame);
        }
        // <<TODO:IMPROVE>> Listen for when the frame closes, and when it does,
        // activate the topmost frame. Because Swing does not seem to do this
        // automatically. [Jon Aquino]
        // fetch icon on first addition after lnf is setup properly
        if (!internalFrameDefaultIconSet) {
            internalFrameDefaultIcon = new JInternalFrame().getFrameIcon();
            internalFrameDefaultIconSet = true;
        }
        // only set icon if none set already
        if (internalFrame.getFrameIcon() == null
                || internalFrame.getFrameIcon()
                        .equals(internalFrameDefaultIcon))
            JUMPWorkbench.setIcon(internalFrame);
        // Call JInternalFrame#setVisible before JDesktopPane#add; otherwise,
        // the
        // TreeLayerNamePanel starts too narrow (100 pixels or so) for some
        // reason.
        // <<TODO>>Investigate. [Jon Aquino]
        internalFrame.setVisible(true);
        desktopPane.add(internalFrame, alwaysOnTop ? JLayeredPane.PALETTE_LAYER
                : JLayeredPane.DEFAULT_LAYER);
        if (autoUpdateToolBar) {
            internalFrame.addInternalFrameListener(new InternalFrameListener() {
                public void internalFrameActivated(InternalFrameEvent e) {
                    toolBar.updateEnabledState();
                    if (internalFrame instanceof TaskFrameProxy) {
                        setActiveTaskFrame(((TaskFrameProxy) internalFrame)
                                .getTaskFrame());
                    }
                    // Associate current cursortool with the new frame [Jon
                    // Aquino]
                    // move to ActivateTaskFrame internal class
                    // toolBar.reClickSelectedCursorToolButton();
                }

                public void internalFrameClosed(InternalFrameEvent e) {
                    toolBar.updateEnabledState();
                }

                public void internalFrameClosing(InternalFrameEvent e) {
                    toolBar.updateEnabledState();
                }

                public void internalFrameDeactivated(InternalFrameEvent e) {
                    toolBar.updateEnabledState();
                }

                public void internalFrameDeiconified(InternalFrameEvent e) {
                    toolBar.updateEnabledState();
                }

                public void internalFrameIconified(InternalFrameEvent e) {
                    toolBar.updateEnabledState();
                }

                public void internalFrameOpened(InternalFrameEvent e) {
                    toolBar.updateEnabledState();
                }
            });
        }
        // prevent user to drag frame out of desktop pane (left and top)
        internalFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                // ignore detached frames, which are hidden in the minus area
                if (internalFrame instanceof DetachableInternalFrame
                        && ((DetachableInternalFrame) internalFrame)
                                .isDetached()) {
                    return;
                }

                int x = internalFrame.getX();
                int y = internalFrame.getY();
                int new_x = x < 0 ? 0 : x;
                int new_y = y < 0 ? 0 : y;
                if (x != new_x || y != new_y)
                    internalFrame.setLocation(new_x, new_y);
            }
        });
        // relocate in window when maximized is disabled
        internalFrame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                if (internalFrame.isMaximizable())
                    legalize(internalFrame);
                ;
            }
        });

        // Call #activateFrame *after* adding the listener. [Jon Aquino]
        position(internalFrame);
        activateFrame(internalFrame);
    }

    private void installTitleBarModifiedIndicator(
            final LayerManagerProxy internalFrame) {
        final JInternalFrame i = (JInternalFrame) internalFrame;
        new Block() {
            // Putting updatingTitle in a Block is better than making it an
            // instance variable, because this way there is one updatingTitle
            // for each
            // internal frame, rather than one for all internal frames. [Jon
            // Aquino]
            private boolean updatingTitle = false;

            private void updateTitle() {
                if (updatingTitle) {
                    return;
                }
                updatingTitle = true;
                try {
                    String newTitle = i.getTitle();
                    if (newTitle.length() > 0 && newTitle.charAt(0) == '*') {
                        newTitle = newTitle.substring(1);
                    }
                    if (!internalFrame.getLayerManager()
                            .getLayersWithModifiedFeatureCollections()
                            .isEmpty()) {
                        newTitle = '*' + newTitle;
                    }
                    i.setTitle(newTitle);
                } finally {
                    updatingTitle = false;
                }
            }

            public Object yield() {
                internalFrame.getLayerManager().addLayerListener(
                        new LayerListener() {
                            public void layerChanged(LayerEvent e) {
                                if ((e.getType() == LayerEventType.METADATA_CHANGED)
                                        || (e.getType() == LayerEventType.REMOVED)) {
                                    updateTitle();
                                }
                            }

                            public void categoryChanged(CategoryEvent e) {
                            }

                            public void featuresChanged(FeatureEvent e) {
                            }
                        });
                i.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY,
                        new PropertyChangeListener() {
                            public void propertyChange(PropertyChangeEvent e) {
                                updateTitle();
                            }
                        });
                return null;
            }
        }.yield();
    }

    private void setClosingBehaviour(final LayerManagerProxy proxy) {
        final JInternalFrame internalFrame = (JInternalFrame) proxy;
        internalFrame
                .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        internalFrame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent e) {
                internalFrameCloseHandler.close(internalFrame);
            }
        });
    }

    private Collection getInternalFramesAssociatedWith(LayerManager layerManager) {
        ArrayList internalFramesAssociatedWithLayerManager = new ArrayList();
        JInternalFrame[] internalFrames = getInternalFrames();
        for (int i = 0; i < internalFrames.length; i++) {
            if (internalFrames[i] instanceof LayerManagerProxy
                    && (((LayerManagerProxy) internalFrames[i])
                            .getLayerManager() == layerManager)) {
                internalFramesAssociatedWithLayerManager.add(internalFrames[i]);
            }
        }
        return internalFramesAssociatedWithLayerManager;
    }

    // added by [mmichaud 2007-06-03]
    // Return TaskFrame s using the same layerManager
    private Collection getTaskFramesAssociatedWith(LayerManager layerManager) {
        ArrayList taskFramesAssociatedWithLayerManager = new ArrayList();
        JInternalFrame[] internalFrames = getInternalFrames();
        for (int i = 0; i < internalFrames.length; i++) {
            if (internalFrames[i] instanceof TaskFrame
                    && (((TaskFrame) internalFrames[i]).getLayerManager() == layerManager)) {
                taskFramesAssociatedWithLayerManager.add(internalFrames[i]);
            }
        }
        return taskFramesAssociatedWithLayerManager;
    }

    // added by [mmichaud 2007-06-03]
    // Return every InternalFrame associated with taskFrame (taskFrame is
    // excluded)
    private Collection getInternalFramesAssociatedWith(TaskFrame taskFrame) {
        ArrayList internalFramesAssociatedWithTaskFrame = new ArrayList();
        JInternalFrame[] internalFrames = getInternalFrames();
        for (int i = 0; i < internalFrames.length; i++) {
            if (internalFrames[i] instanceof TaskFrameProxy
                    && (((TaskFrameProxy) internalFrames[i]).getTaskFrame() == taskFrame)
                    && internalFrames[i] != taskFrame) {
                internalFramesAssociatedWithTaskFrame.add(internalFrames[i]);
            }
        }
        return internalFramesAssociatedWithTaskFrame;
    }

    public TaskFrame addTaskFrame() {
        TaskFrame f = addTaskFrame(createTask());
        return f;
    }

    public Task createTask() {
        Task task = new Task();
        // LayerManager shouldn't automatically add categories in its
        // constructor.
        // Sometimes we want to create a LayerManager with no categories
        // (e.g. in OpenProjectPlugIn). [Jon Aquino]
        task.getLayerManager().addCategory(StandardCategoryNames.WORKING);
        task.getLayerManager().addCategory(StandardCategoryNames.SYSTEM);
        task.setName(I18N.get("ui.WorkbenchFrame.task") + " " + taskSequence++);
        return task;
    }

    public TaskFrame addTaskFrame(Task task) {
        TaskFrame taskFrame;
        if (taskFrameFactory != null) {
            taskFrame = taskFrameFactory.createComponent();
            taskFrame.setTask(task);
        } else {
            taskFrame = new TaskFrame(task, workbenchContext);
        }
        return addTaskFrame(taskFrame);
    }

    public TaskFrame addTaskFrame(TaskFrame taskFrame) {
        // resize to fill current desktoppane
        double factor = 0.95;
        int w = (int) (getDesktopPane().getWidth() * factor);
        int h = (int) (getDesktopPane().getHeight() * factor);
        taskFrame.setSize(w, h);
        // track which taskframe is activated [ede r2804]
        // move addInternalFrameListener to TaskFrame class (constructor)
        // to fix bugs id 3530414 and 3536708 [mmichaud r2978]
        // taskFrame.addInternalFrameListener(new ActivateTaskFrame());
        taskFrame.getTask().getLayerManager()
                .addLayerListener(new LayerListener() {
                    public void featuresChanged(FeatureEvent e) {
                    }

                    public void categoryChanged(CategoryEvent e) {
                        toolBar.updateEnabledState();
                    }

                    public void layerChanged(LayerEvent layerEvent) {
                        toolBar.updateEnabledState();
                    }
                });
        addInternalFrame(taskFrame);
        taskFrame.getLayerViewPanel().getLayerManager()
                .getUndoableEditReceiver()
                .add(new UndoableEditReceiver.Listener() {
                    public void undoHistoryChanged() {
                        toolBar.updateEnabledState();
                    }

                    public void undoHistoryTruncated() {
                        toolBar.updateEnabledState();
                        log(I18N.get("ui.WorkbenchFrame.undo-history-was-truncated"));
                    }
                });
        // fire TaskListener's
        Object[] listeners = getTaskListeners().toArray();
        for (int i = 0; i < listeners.length; i++) {
            TaskListener l = (TaskListener) listeners[i];
            l.taskAdded(new TaskEvent(this, taskFrame.getTask()));
        }
        return taskFrame;
    }

    // private class ActivateTaskFrame extends InternalFrameAdapter{
    // public void internalFrameActivated(InternalFrameEvent e) {
    // activeTaskFrame = (TaskFrame)e.getInternalFrame();
    // toolBar.reClickSelectedCursorToolButton();
    // }
    // }

    public TaskFrame getActiveTaskFrame() {
        return activeTaskFrame;
    }

    public void setActiveTaskFrame(TaskFrame taskFrame) {
        this.activeTaskFrame = taskFrame;
    }

    public void flash(final HTMLFrame frame) {
        final Color originalColor = frame.getBackgroundColor();
        new Timer(100, new ActionListener() {
            private int tickCount = 0;

            public void actionPerformed(ActionEvent e) {
                try {
                    tickCount++;
                    frame.setBackgroundColor(((tickCount % 2) == 0) ? originalColor
                            : Color.yellow);
                    if (tickCount == 2) {
                        Timer timer = (Timer) e.getSource();
                        timer.stop();
                    }
                } catch (Throwable t) {
                    handleThrowable(t);
                }
            }
        }).start();
    }

    private void flashStatusMessage(final String message, final Color color) {
        new Timer(100, new ActionListener() {
            private int tickCount = 0;

            public void actionPerformed(ActionEvent e) {
                tickCount++;
                // This message is important, so overwrite whatever is on the
                // status bar. [Jon Aquino]
                setStatusBarText(message);
                setStatusBarTextHighlighted((tickCount % 2) == 0, color);
                if (tickCount == 4) {
                    Timer timer = (Timer) e.getSource();
                    timer.stop();
                }
            }
        }).start();
    }

    /**
     * Can be called regardless of whether the current thread is the AWT event
     * dispatch thread.
     * 
     * @param t
     *            Description of the Parameter
     */
    public void handleThrowable(final Throwable t) {
        Component parent = this;
        Window[] ownedWindows = getOwnedWindows();
        for (int i = 0; i < ownedWindows.length; i++) {
            if (ownedWindows[i] instanceof Dialog
                    && ownedWindows[i].isVisible()
                    && ((Dialog) ownedWindows[i]).isModal()) {
                parent = ownedWindows[i];
                break;
            }
        }
        handleThrowable(t, parent);
    }

    /**
     * show a modal error dialog and log the issue
     */
    public void handleThrowable(final Throwable t, final Component parent) {
        // always log as error
        Logger.error(t);

        final String stack = StringUtil.stackTrace(t);
        // show it
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ErrorDialog.show(parent,
                        StringUtil.toFriendlyName(t.getClass().getName()),
                        toMessage(t), stack);
            }
        });
    }

    private ArrayList lastFiveThrowableDates = new ArrayList() {
        public boolean add(Object o) {
            if (size() == 5) {
                remove(0);
            }
            return super.add(o);
        }
    };

    public static String toMessage(Throwable t) {
        String message;
        if (t.getLocalizedMessage() == null) {
            message = I18N.get("ui.WorkbenchFrame.no-description-was-provided");
        } else if (t.getLocalizedMessage().toLowerCase()
                .indexOf(I18N.get("ui.WorkbenchFrame.side-location-conflict")) > -1) {
            message = t.getLocalizedMessage()
                    + " -- "
                    + I18N.get("ui.WorkbenchFrame.check-for-invalid-geometries");
        } else {
            message = t.getLocalizedMessage();
        }
        if (t instanceof SQLException) {
            Throwable nextT = ((SQLException) t).getNextException();
            if (nextT != null)
                message = message + "\n" + nextT.getLocalizedMessage();
        }
        return message + "\n\n ("
                + StringUtil.toFriendlyName(t.getClass().getName()) + ")";
    }

    public boolean hasInternalFrame(JInternalFrame internalFrame) {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == internalFrame) {
                return true;
            }
        }
        return false;
    }

    public void removeInternalFrame(JInternalFrame internalFrame) {
        // Looks like #closeFrame is the proper way to remove an internal frame.
        // It will activate the next frame. [Jon Aquino]
        desktopPane.getDesktopManager().closeFrame(internalFrame);
    }

    public void warnUser(String warning) {
        log(I18N.get("ui.WorkbenchFrame.warning") + ": " + warning);
        setStatusMessage(warning, 15000, Color.YELLOW, true);
    }

    public void zoomChanged(Envelope modelEnvelope) {
        toolBar.updateEnabledState();
        // May be null during a new project initialization
        if (getActiveTaskFrame() != null) {
          double scale = ScreenScale.getHorizontalMapScale(getActiveTaskFrame()
              .getLayerViewPanel().getViewport());
          DecimalFormat format = scale >= 10 ? scaleFormat : scaleFormatSmall;
          setScaleText("1:" + format.format(scale));
        }
    }

    void exitMenuItem_actionPerformed(ActionEvent e) {
        closeApplication();
    }

    void this_componentShown(ComponentEvent e) {
        try {
            // If the first internal frame is not a TaskWindow (as may be the
            // case in
            // custom workbenches), #updateEnabledState() will ensure that the
            // cursor-tool buttons are disabled. [Jon Aquino]
            toolBar.updateEnabledState();
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    void this_windowClosing(WindowEvent e) {
        closeApplication();
    }

    void windowMenu_menuSelected(MenuEvent e) {
        // If this is the first call get the number of added menu items.
        // After this point no new menus can be added
        if (addedMenuItems == -1) {
            addedMenuItems = windowMenu.getItemCount();
            // always add a separator between menu entries and frame list
            if (addedMenuItems > 0) {
                windowMenu.addSeparator();
                addedMenuItems++;
            }
        }
        while (windowMenu.getItemCount() > addedMenuItems) {
            windowMenu.remove(windowMenu.getItemCount() - 1);
        }
        final JInternalFrame[] frames = desktopPane.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            JMenuItem menuItem = new JMenuItem();
            // Increase truncation threshold from 20 to 40, for eziLink [Jon
            // Aquino]
            menuItem.setText(GUIUtil.truncateString(frames[i].getTitle(), 40));
            associate(menuItem, frames[i]);
            windowMenu.add(menuItem);
        }
        if (windowMenu.getItemCount() == addedMenuItems) {
            // For ezLink [Jon Aquino]
            windowMenu.add(new JMenuItem("("
                    + I18N.get("ui.WorkbenchFrame.no-windows") + ")"));
        }
    }

    private void associate(JMenuItem menuItem, final JInternalFrame frame) {
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    activateFrame(frame);
                } catch (Throwable t) {
                    handleThrowable(t);
                }
            }
        });
    }

    private void closeApplication() {
        applicationExitHandler.exitApplication(this);
    }

    private Collection getLayersWithModifiedFeatureCollections() {
        ArrayList layersWithModifiedFeatureCollections = new ArrayList();
        for (Iterator i = getLayerManagers().iterator(); i.hasNext();) {
            LayerManager layerManager = (LayerManager) i.next();
            layersWithModifiedFeatureCollections.addAll(layerManager
                    .getLayersWithModifiedFeatureCollections());
        }
        return layersWithModifiedFeatureCollections;
    }

    private Collection getGeneratedLayers() {
        ArrayList list = new ArrayList();
        for (Iterator i = getLayerManagers().iterator(); i.hasNext();) {
            LayerManager layerManager = (LayerManager) i.next();
            list.addAll(layerManager.getLayersWithNullDataSource());
            // Giuseppe Aruta 2016_01_02. Adds also temporary raster image
            // layers
            list.addAll(layerManager.getTemporaryRasterImageLayers());
            ;
        }
        return list;
    }

    private Collection getLayerManagers() {
        // Multiple windows may point to the same LayerManager, so use
        // a Set. [Jon Aquino]
        HashSet layerManagers = new HashSet();
        JInternalFrame[] internalFrames = getInternalFrames();
        for (int i = 0; i < internalFrames.length; i++) {
            if (internalFrames[i] instanceof LayerManagerProxy) {
                layerManagers.add(((LayerManagerProxy) internalFrames[i])
                        .getLayerManager());
            }
        }
        return layerManagers;
    }

    private void configureStatusBarComponent(JComponent component, int width) {
        component.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        if (width > 0) {
            component.setMinimumSize(new Dimension(width, (int) component
                    .getMinimumSize().getHeight()));
            // component.setMaximumSize(new Dimension(width,
            // (int)component.getMaximumSize()
            // .getHeight()));
            // component.setPreferredSize(new Dimension(width,
            // (int)component.getPreferredSize()
            // .getHeight()));
        }
    }

    private final int defaultDismissTimeout = ToolTipManager.sharedInstance()
            .getDismissDelay();
    private final int defaultInitialTimeout = ToolTipManager.sharedInstance()
            .getInitialDelay();
    private final int defaultReshowTimeout = ToolTipManager.sharedInstance()
            .getReshowDelay();

    private void jbInit() throws Exception {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                this_componentShown(e);
            }
        });
        this.getContentPane().setLayout(borderLayout1);
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowActivated(WindowEvent e) {
                TaskFrame f = getActiveTaskFrame();
                if (f != null) {
                    // have cursortool reactivated, in turn recheck the
                    // currently pressed keys
                    f.getLayerViewPanel().reSetCurrentCursorTool();
                }
            }

            public void windowClosing(WindowEvent e) {
                this_windowClosing(e);
            }
        });
        this.setJMenuBar(menuBar);
        // This size is chosen so that when the user hits the Info tool, the
        // window
        // fits between the lower edge of the TaskFrame and the lower edge of
        // the
        // WorkbenchFrame. See the call to #setSize in InfoFrame. [Jon Aquino]
        setSize(900, 665);
        // OUTLINE_DRAG_MODE is excruciatingly slow in JDK 1.4.1, so don't use
        // it.
        // (although it's supposed to be fixed in 1.4.2, which has not yet been
        // released). (see Sun Java Bug ID 4665237). [Jon Aquino]
        // desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        windowMenu.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(MenuEvent e) {
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuSelected(MenuEvent e) {
                windowMenu_menuSelected(e);
            }
        });

        messageText = new JTextArea(" ");
        timeLabel = new JLabel(" ");
        memoryLabel = new JLabel(" ");
        scaleLabel = new JLabel(" ");
        coordinateLabel = new JLabel(" ");

        // gc shortcut: a doubleclick on the memorylabel starts gc'ing
        memoryLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2)
                    return;

                Runtime.getRuntime().gc();
                setStatusMessage(MEMORY_GC, 2000);
            }
        });

        // [Giuseppe Aruta 2016_6_4] doubleclick to open Zoom to Scale plugin
        scaleLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2)
                    return;
                executePlugin(zoomToScalePlugin);
            }
        });

        // [Giuseppe Aruta 2016_6_4] doubleclick to open Zoom to Coordinates
        // plugin
        coordinateLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2)
                    return;

                executePlugin(zoomToCoordPlugin);
            }
        });

        // Tooltips can cover some labels avoiding the usage of
        // double click to open plugins.
        // I use TooltipManager to get a small delay and reduce the time of
        // display
        // From:
        // http://stackoverflow.com/questions/1190290/set-the-tooltip-delay-time-for-a-particular-component-in-java-swing
        // [Giuseppe Aruta 2016_06_16]
        coordinateLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent me) {
                ToolTipManager.sharedInstance().setInitialDelay(1000);
                ToolTipManager.sharedInstance().setDismissDelay(1800);
                // ToolTipManager.sharedInstance().setReshowDelay(1500);

            }

            public void mouseExited(MouseEvent me) {
                ToolTipManager.sharedInstance().setInitialDelay(
                        defaultInitialTimeout);
                ToolTipManager.sharedInstance().setDismissDelay(
                        defaultDismissTimeout);
                // ToolTipManager.sharedInstance().setReshowDelay(
                // defaultReshowTimeout);
            }
        });
        scaleLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent me) {
                ToolTipManager.sharedInstance().setInitialDelay(1000);
                ToolTipManager.sharedInstance().setDismissDelay(1800);
                // ToolTipManager.sharedInstance().setReshowDelay(1500);

            }

            public void mouseExited(MouseEvent me) {
                ToolTipManager.sharedInstance().setInitialDelay(
                        defaultInitialTimeout);
                ToolTipManager.sharedInstance().setDismissDelay(
                        defaultDismissTimeout);
                // ToolTipManager.sharedInstance().setReshowDelay(
                // defaultReshowTimeout);
            }
        });

        memoryLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent me) {
                ToolTipManager.sharedInstance().setInitialDelay(1000);
                ToolTipManager.sharedInstance().setDismissDelay(1800);
                // ToolTipManager.sharedInstance().setReshowDelay(1500);

            }

            public void mouseExited(MouseEvent me) {
                ToolTipManager.sharedInstance().setInitialDelay(
                        defaultInitialTimeout);
                ToolTipManager.sharedInstance().setDismissDelay(
                        defaultDismissTimeout);
                // ToolTipManager.sharedInstance().setReshowDelay(
                // defaultReshowTimeout);
            }
        });
        // this is important, else resizing in the splitpane is buggy, can only
        // make it larger, see
        // https://forums.oracle.com/forums/thread.jspa?threadID=1361066
        messageText.setMinimumSize(new Dimension(50, 1));

        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);

        // mimick a JLabel
        messageText.setEditable(false);
        // we do this now in setVisible() below
        // messageText.setBackground(coordinateLabel.getBackground());
        // messageText.setForeground(coordinateLabel.getForeground());
        // //messageText.setOpaque(coordinateLabel.isOpaque());
        // messageText.setFont(coordinateLabel.getFont());
        // init first message with tooltip
        setStatusBarText("");

        menuBar.add(fileMenu);
        menuBar.add(windowMenu);

        // a little padding from the left looks better, when the floating handle is missing
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        getContentPane().add(toolBar, BorderLayout.NORTH);

        // [Matthias Scholz 11. Dec 2010] new resizable statusbar
        statusPanel = new JPanel(new BorderLayout());

        int dividerSize = 3;
        statusPanelSplitPane4 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                true, scaleLabel, coordinateLabel);
        statusPanelSplitPane4.setDividerSize(dividerSize);
        statusPanelSplitPane4.setResizeWeight(0);

        statusPanelSplitPane3 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                true, memoryLabel, statusPanelSplitPane4);
        statusPanelSplitPane3.setDividerSize(dividerSize);
        statusPanelSplitPane3.setResizeWeight(0);

        statusPanelSplitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                true, timeLabel, statusPanelSplitPane3);
        statusPanelSplitPane2.setDividerSize(dividerSize);
        statusPanelSplitPane2.setResizeWeight(0);

        statusPanelSplitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                true, messageText, statusPanelSplitPane2);
        statusPanelSplitPane1.setDividerSize(dividerSize);
        statusPanelSplitPane1.setResizeWeight(1);

        // Workaround for java bug 4131528
        Border b = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        statusPanelSplitPane1.setBorder(b);
        statusPanelSplitPane2.setBorder(b);
        statusPanelSplitPane3.setBorder(b);
        statusPanelSplitPane4.setBorder(b);
        statusPanel.add(statusPanelSplitPane1, BorderLayout.CENTER);

        desktopStatusSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                desktopPane, statusPanel);
        desktopStatusSplit.setDividerSize(dividerSize);
        desktopStatusSplit.setResizeWeight(1d);
        getContentPane().add(desktopStatusSplit, BorderLayout.CENTER);

        // getContentPane().add(desktopPane, BorderLayout.CENTER);
        // getContentPane().add(statusPanel, BorderLayout.SOUTH);
    }

    private void position(JInternalFrame internalFrame) {
        final int STEP = 5;
        GUIUtil.Location location = null;
        // initially show infowindow, attributewindow on the left bottom
        // later on attributewindow saves/restore it's last postion from
        // persist.blackboard
        if (internalFrame instanceof InfoFrame
                || internalFrame instanceof ViewAttributesFrame) {
            primaryInfoFrameIndex++;
            int offset = (primaryInfoFrameIndex % 3) * STEP;
            location = new GUIUtil.Location(offset + 10, false, offset + 10,
                    true);
        }
        // ordinary int.frames start from top left
        else {
            positionIndex++;
            int offset = (positionIndex % 5) * STEP;
            location = new GUIUtil.Location(offset, false, offset, false);
        }
        // cut to desktoppane size, if bigger
        int x = location.x;
        int y = location.y;
        int w = internalFrame.getWidth();
        int h = internalFrame.getHeight();
        int pane_w = getDesktopPane().getWidth();
        int pane_h = getDesktopPane().getHeight();
        int new_w = w, new_h = h;
        if (x + w > pane_w)
            new_w = pane_w - x;
        if (y + h > pane_h)
            new_h = pane_h - y;

        GUIUtil.setBounds(internalFrame, location, new_w, new_h,
                getDesktopPane());
    }

    private void legalize(JInternalFrame iframe) {
        // protect detached frame's source frame
        if (iframe instanceof DetachableInternalFrame
                && ((DetachableInternalFrame) iframe).isDetached()) {
            return;
        }

        int x = iframe.getX();
        int y = iframe.getY();
        int w = iframe.getWidth();
        int h = iframe.getHeight();
        int pane_w = getDesktopPane().getWidth();
        int pane_h = getDesktopPane().getHeight();
        int new_x = x, new_y = y, new_w = w, new_h = h;

        if (x + w >= pane_w) {
            if (pane_w - w < 0) {
                new_x = 0;
                new_w = pane_w;
            } else {
                new_x = pane_w - w;
            }
        }
        if (y + h >= pane_h) {
            if (pane_h - h < 0) {
                new_y = 0;
                new_h = pane_h;
            } else {
                new_y = pane_h - h;
            }
        }

        // relocate/resize if needed
        if (x != new_x || y != new_y || w != new_w || h != new_h)
            iframe.setBounds(new_x, new_y, new_w, new_h);
    }

    /**
     * Fundamental Style classes (like BasicStyle, VertexStyle, and LabelStyle)
     * cannot be removed, and are thus excluded from the choosable Style
     * classes.
     */
    public Set getChoosableStyleClasses() {
        return Collections.unmodifiableSet(choosableStyleClasses);
    }

    public void addChoosableStyleClass(Class choosableStyleClass) {
        Assert.isTrue(ChoosableStyle.class
                .isAssignableFrom(choosableStyleClass));
        choosableStyleClasses.add(choosableStyleClass);
    }

    /**
     * Adds a keyboard shortcut for a plugin. logs plugin exceptions. note -
     * attaching to keyCode 'a', modifiers =1 will detect shift-A events. It
     * will *not* detect caps-lock-'a'. This is due to inconsistencies in
     * java.awt.event.KeyEvent. In the unlikely event you actually do want to
     * also also attach to caps-lock-'a', then make two shortcuts - one to
     * keyCode 'a' and modifiers =1 (shift-A) and one to keyCode 'A' and
     * modifiers=0 (caps-lock A). For more details, see the
     * java.awt.event.KeyEvent class - it has a full explaination.
     * 
     * @param keyCode
     *            What key to attach to (See java.awt.event.KeyEvent)
     * @param modifiers
     *            0= none, 1=shift, 2= cntrl, 8=alt, 3=shift+cntrl, etc... See
     *            the modifier mask constants in the Event class
     * @param plugIn
     *            What plugin to execute
     * 
     * @deprecated use addKeyboardShortcut(KeyStroke key, PlugIn plugIn) instead
     */
    public void addKeyboardShortcut(final int keyCode, final int modifiers,
            final PlugIn plugIn, EnableCheck check) {
        // warn on overwriting shortcuts, only to console "KISS"
        // if
        // (shortcutListener.containsDefinition(KeyStroke.getKeyStroke(keyCode,
        // modifiers)))
        // System.err.println("reassign shortcut "
        // + KeyEvent.getKeyModifiersText(modifiers) + "/"
        // + KeyEvent.getKeyText(keyCode)/* + "(" + modifiers + "/" + keyCode
        // + ") from " + shortcutListener.get(keyCode, modifiers)[0] + " to "
        // + plugIn + " -> " + shortcutListener*/);

        check = (check == null) ? AbstractPlugIn.getEnableCheck(plugIn) : check;
        // System.err.println("add shortcut "+keyCode+"/"+modifiers+
        // " -> "+plugIn.getName());
        shortcutListener.add(keyCode, modifiers, plugIn, check);
    }

    public void addKeyboardShortcut(KeyStroke stroke, PlugIn plugIn) {
        shortcutListener.add(stroke, plugIn);
    }

    public PlugIn getKeyboardShortcutPlugin(KeyStroke key) {
        PlugIn p = shortcutListener.getPlugIn(key);
        // if (p==null)
        // System.out.println("wbf getkscp "+keyCode+"/"+modifiers+" = "+p+" -> "+shortcutListener);
        return p;
    }

    public final Set<KeyStroke> getKeyboardShortcuts() {
        return shortcutListener.getAllKeyStrokes();
    }

    // ==========================================================================
    // Applications (such as EziLink) want to override the default JUMP
    // frame closing behaviour and application exit behaviour with their own
    // behaviours.
    //
    InternalFrameCloseHandler internalFrameCloseHandler = new DefaultInternalFrameCloser();

    ApplicationExitHandler applicationExitHandler = new DefaultApplicationExitHandler();

    public InternalFrameCloseHandler getInternalFrameCloseHandler() {
        return internalFrameCloseHandler;
    }

    public void setInternalFrameCloseHandler(InternalFrameCloseHandler value) {
        internalFrameCloseHandler = value;
    }

    public ApplicationExitHandler getApplicationExitHandler() {
        return applicationExitHandler;
    }

    public void setApplicationExitHandler(ApplicationExitHandler value) {
        applicationExitHandler = value;
    }

    private class DefaultInternalFrameCloser implements
            InternalFrameCloseHandler {
        public void close(JInternalFrame internalFrame) {
            if (internalFrame instanceof TaskFrame) {
                // delete reference to taskframe to be closed
                if (activeTaskFrame == internalFrame)
                    // activeTaskFrame = null;
                    closeTaskFrame((TaskFrame) internalFrame);
                JInternalFrame activeInternalFrame = getActiveInternalFrame();
                if (activeInternalFrame == null
                        || !(activeInternalFrame instanceof TaskFrameProxy)) {
                    activeTaskFrame = null;
                } else
                    activeTaskFrame = ((TaskFrameProxy) activeInternalFrame)
                            .getTaskFrame();
            } else {
                GUIUtil.dispose(internalFrame, desktopPane);
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    System.runFinalization();
                    System.gc();
                }
            });
        }
    }

  private class DefaultApplicationExitHandler implements ApplicationExitHandler {

    public void exitApplication(final JFrame mainFrame) {
      PlugInContext context = getContext().createPlugInContext();
      if (confirmClose(I18N.get("ui.WorkbenchFrame.exit-jump"),
          getLayersWithModifiedFeatureCollections(), getGeneratedLayers(),
          WorkbenchFrame.this)) {

        // Giuseppe Aruta -June 30 2015 - warning and save selected
        // project before closing Openjump
        try {
          int res = JOptionPane.showConfirmDialog(mainFrame, SAVE_PROJECT,
              EXIT_OPENJUMP, JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.DEFAULT_OPTION, null);

          if (res == JOptionPane.YES_OPTION) {

            SaveProjectPlugIn saveProjectPlugIn = new SaveProjectPlugIn();
            // [mmichaud] added on 2016-11-01 to fix #438
            UndoableEditReceiver undoableEditReceiver = workbenchContext
                .getLayerManager() != null ? workbenchContext.getLayerManager()
                .getUndoableEditReceiver() : null;
            if (undoableEditReceiver != null) {
              undoableEditReceiver.startReceiving();
            }
            // end of fix
            
            // save and show affirmation dialog on success
            saveProjectPlugIn.initialize(context);
            if (saveProjectPlugIn.execute(context)) {
              String projectName = context.getTask().getProjectFile()
                  .getAbsolutePath();

              JOptionPane.showMessageDialog(mainFrame, PROJECT_SAVED + projectName,
                  EXIT_OPENJUMP, JOptionPane.PLAIN_MESSAGE);
            }
            // failure to save is handled like a 'Cancel exit' for now 
            else {
              return;
            }
          } else if (res == JOptionPane.CANCEL_OPTION) {
            return;
          }

          // PersistentBlackboardPlugIn listens for when the workbench is
          // hidden [Jon Aquino]
          saveWindowState();
          setVisible(false);

        } catch (final Throwable t) {
          try {
            // always log message, log stack only if debug is enabled
            Logger.warn(t.getMessage(), Logger.isDebugEnabled() ? t : null);

            // show it instantly, before the main frame get's destroyed
            ErrorDialog.show(mainFrame,
                StringUtil.toFriendlyName(t.getClass().getName()),
                toMessage(t), StringUtil.stackTrace(t));
          } catch (Throwable t2) {
            // whatever happened here is not important enough to let it
            // break the application exiting. log it though.
            Logger.error(t2);
          }
        }

        // Invoke System#exit after all pending GUI events have been fired
        // (e.g. the hiding of this WorkbenchFrame) [Jon Aquino]
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            System.exit(0);
          }
        });
      }
    }
  }

    // Method completed by [mmichaud 2007-06-03] to close properly
    // internal frames depending on a TaskFrame.
    // Maybe this method should take place in TaskFrame instead...
    private void closeTaskFrame(TaskFrame taskFrame) {
        LayerManager layerManager = taskFrame.getLayerManager();
        Collection associatedFrames = getInternalFramesAssociatedWith(taskFrame);
        boolean lastTaskFrame = getTaskFramesAssociatedWith(layerManager)
                .size() == 1;
        if (lastTaskFrame) {
            Collection modifiedItems = layerManager
                    .getLayersWithModifiedFeatureCollections();
            Collection generatedItems = layerManager
                    .getLayersWithNullDataSource();
            if (confirmClose(I18N.get("ui.WorkbenchFrame.close-task"),
                    modifiedItems, generatedItems, taskFrame)) {

                // Giuseppe Aruta -June 30 2015 - simple warning that a project
                // is being closed
                try {
                    int res = JOptionPane.showConfirmDialog(this,
                            CLOSE_PROJECT_QUESTION, CLOSE_PROJECT,
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.DEFAULT_OPTION, null);
                    if (res == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                } catch (Exception e) {
                    log("", e);
                }

                // There are other internal frames associated with this task
                if (associatedFrames.size() != 0) {
                    // Confirm you want to close them first
                    if (confirmClose(
                            StringUtil.split(
                                    I18N.get("ui.WorkbenchFrame.other-internal-frames-depend-on-this-task-frame")
                                            + " "
                                            + I18N.get("ui.WorkbenchFrame.do-you-want-to-close-them-also"),
                                    60),
                            I18N.get("ui.WorkbenchFrame.close-all"))) {
                        for (java.util.Iterator it = associatedFrames
                                .iterator(); it.hasNext();) {
                            GUIUtil.dispose((JInternalFrame) it.next(),
                                    desktopPane);
                        }

                    } else
                        return; // finally, I don't want to close
                }
                layerManager.dispose();
                taskFrame.getLayerViewPanel().dispose();
                taskFrame.getLayerNamePanel().dispose();
                GUIUtil.dispose(taskFrame, desktopPane);
            } else
                return; // finally, I don't want to close
        } else {
            // There are other internal frames associated with this task
            if (associatedFrames.size() != 0) {
                // Confirm you want to close them first
                if (confirmClose(
                        StringUtil.split(
                                I18N.get("ui.WorkbenchFrame.other-internal-frames-depend-on-this-task-frame")
                                        + " "
                                        + I18N.get("ui.WorkbenchFrame.do-you-want-to-close-them-also"),
                                60), I18N.get("ui.WorkbenchFrame.close-all"))) {
                    for (java.util.Iterator it = associatedFrames.iterator(); it
                            .hasNext();) {
                        GUIUtil.dispose((JInternalFrame) it.next(), desktopPane);
                    }
                } else
                    return; // finally, I don't want to close
            }
            taskFrame.getLayerViewPanel().dispose();
            taskFrame.getLayerNamePanel().dispose();
            GUIUtil.dispose(taskFrame, desktopPane);
        }
    }

    /**
     * This method is used to confirm the close of a TaskFrame or the close of
     * the application. In both cases, we need to check there is no unsaved
     * layers.
     */
    private boolean confirmClose(String action, Collection modifiedLayers,
            Collection generatedLayers, Container container) {
        if (modifiedLayers.isEmpty()) {
            if (generatedLayers.isEmpty()) {
                return true;
            }
            JOptionPane pane = new JOptionPane();
            String message = null;

            /*
             * [2015-01-03] Giuseppe Aruta Generate a string with the list of
             * layers with no datasource
             * 
             * ///[2015-01-03] Giuseppe Aruta Deactivated as it works even if
             * there are no layers with no datasource
             * 
             * String numLayer = "\n (" + ((generatedLayers.size() > 3) ?
             * "e.g. " : "") + StringUtil.toCommaDelimitedString(new ArrayList(
             * generatedLayers).subList(0, Math.min(3, generatedLayers.size())))
             * + ")";
             */
            if (container instanceof WorkbenchFrame) {
                message = I18N
                        .getMessage(
                                "ui.WorkbenchFrame.do-you-really-want-to-close-openjump",
                                new Object[] { Integer.valueOf(generatedLayers
                                        .size()) });
            } else if (container instanceof TaskFrame) {
                message = I18N
                        .getMessage(
                                "ui.WorkbenchFrame.do-you-really-want-to-close-the-project",
                                new Object[] { Integer.valueOf(generatedLayers
                                        .size()) });
            }
            pane.setMessage(message);
            pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
            pane.setOptions(new String[] { action,
                    I18N.get("ui.WorkbenchFrame.cancel") });
            pane.createDialog(this, "OpenJUMP").setVisible(true);
            return pane.getValue().equals(action);
        }
        JOptionPane pane = new JOptionPane(
                StringUtil.split(
                        modifiedLayers.size()
                                + " "
                                + I18N.get("ui.WorkbenchFrame.dataset")
                                + StringUtil.s(modifiedLayers.size())
                                + " "
                                + ((modifiedLayers.size() > 1) ? I18N
                                        .get("ui.WorkbenchFrame.have-been-modified")
                                        : I18N.get("ui.WorkbenchFrame.has-been-modified"))
                                + " ("
                                + ((modifiedLayers.size() > 3) ? "e.g. " : "")
                                + StringUtil.toCommaDelimitedString(new ArrayList(
                                        modifiedLayers).subList(0,
                                        Math.min(3, modifiedLayers.size())))
                                + ").\n"
                                + I18N.get("ui.WorkbenchFrame.continue"), 80),
                JOptionPane.WARNING_MESSAGE);
        pane.setOptions(new String[] { action,
                I18N.get("ui.WorkbenchFrame.cancel") });
        pane.createDialog(this, "OpenJUMP").setVisible(true);
        return pane.getValue().equals(action);
    }

    private boolean confirmClose(String question, String action) {
        javax.swing.JOptionPane pane = new javax.swing.JOptionPane(question,
                javax.swing.JOptionPane.WARNING_MESSAGE);
        pane.setOptions(new String[] { action,
                com.vividsolutions.jump.I18N.get("ui.WorkbenchFrame.cancel") });
        pane.createDialog(this, "OpenJUMP").setVisible(true);
        return pane.getValue().equals(action);
    }

    /**
     * @param taskFrameFactory
     *            the taskFrameFactory to set
     */
    public void setTaskFrameFactory(ComponentFactory<TaskFrame> taskFrameFactory) {
        this.taskFrameFactory = taskFrameFactory;
    }

    public final static String MAXIMIZED_KEY = WorkbenchFrame.class.getName()
            + " - MAXIMIZED_KEY";
    public final static String HORIZONTAL_KEY = WorkbenchFrame.class.getName()
            + " - HORIZONTAL_KEY";
    public final static String VERTICAL_KEY = WorkbenchFrame.class.getName()
            + " - VERTICAL_KEY";
    public final static String WIDTH_KEY = WorkbenchFrame.class.getName()
            + " - WIDTH_KEY";
    public final static String HEIGHT_KEY = WorkbenchFrame.class.getName()
            + " - HEIGHT_KEY";
    public final static String STATUSPANEL_DIVIDER_LOCATION_1 = WorkbenchFrame.class
            .getName() + " - STATUSPANEL_DIVIDER_LOCATION_1";
    public final static String STATUSPANEL_DIVIDER_LOCATION_2 = WorkbenchFrame.class
            .getName() + " - STATUSPANEL_DIVIDER_LOCATION_2";
    public final static String STATUSPANEL_DIVIDER_LOCATION_3 = WorkbenchFrame.class
            .getName() + " - STATUSPANEL_DIVIDER_LOCATION_3";
    public final static String STATUSPANEL_DIVIDER_LOCATION_4 = WorkbenchFrame.class
            .getName() + " - STATUSPANEL_DIVIDER_LOCATION_4";
    public final static String DESKTOPSTATUS_DIVIDER_LOCATION = WorkbenchFrame.class
            .getName() + " - DESKTOPSTATUS_DIVIDER_LOCATION";

    public void saveWindowState() {
        boolean maximized = (this.getExtendedState() == MAXIMIZED_BOTH);
        Blackboard blackboard = PersistentBlackboardPlugIn
                .get(workbenchContext);
        blackboard.put(MAXIMIZED_KEY, maximized);
        Point p = this.getLocation(null);
        blackboard.put(HORIZONTAL_KEY, p.x);
        blackboard.put(VERTICAL_KEY, p.y);
        Dimension d = this.getSize();
        blackboard.put(WIDTH_KEY, d.width);
        blackboard.put(HEIGHT_KEY, d.height);
        // save the statuspanel divider locations
        blackboard.put(STATUSPANEL_DIVIDER_LOCATION_1, new Integer(
                statusPanelSplitPane1.getDividerLocation()));
        blackboard.put(STATUSPANEL_DIVIDER_LOCATION_2, new Integer(
                statusPanelSplitPane2.getDividerLocation()));
        blackboard.put(STATUSPANEL_DIVIDER_LOCATION_3, new Integer(
                statusPanelSplitPane3.getDividerLocation()));
        blackboard.put(STATUSPANEL_DIVIDER_LOCATION_4, new Integer(
                statusPanelSplitPane4.getDividerLocation()));
        blackboard.put(DESKTOPSTATUS_DIVIDER_LOCATION, new Integer(
                desktopStatusSplit.getDividerLocation()));
    }

    private boolean recallMaximizedState() {
        Blackboard blackboard = PersistentBlackboardPlugIn
                .get(workbenchContext);
        // on the very first start we are maximized, later we'll restore users
        // last state
        boolean maximized = true;
        if (blackboard.get(MAXIMIZED_KEY) == null) {
            blackboard.put(MAXIMIZED_KEY, maximized);
        }
        maximized = ((Boolean) blackboard.get(MAXIMIZED_KEY)).booleanValue();
        return maximized;
    }

    public Point initWindowLocation() {
        Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        return new Point((rect.width - getWidth()) / 2 + rect.x,
                (rect.height - getHeight()) / 2 + rect.y);
    }

    public Dimension initWindowSize() {
        Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        if (rect.width > 740)
            rect.width = 740;
        if (rect.height > 480)
            rect.height = 480;
        return rect.getSize();
    }

    private Point recallWindowLocation() {
        Blackboard blackboard = PersistentBlackboardPlugIn
                .get(workbenchContext);

        Point p;
        if (blackboard.get(HORIZONTAL_KEY) == null) {
            p = initWindowLocation();
            blackboard.put(HORIZONTAL_KEY, p.x);
            blackboard.put(VERTICAL_KEY, p.y);
        } else {
            p = new Point(
                    ((Integer) blackboard.get(HORIZONTAL_KEY)).intValue(),
                    ((Integer) blackboard.get(VERTICAL_KEY)).intValue());
        }
        return p;
    }

    private Dimension recallWindowSize() {
        Blackboard blackboard = PersistentBlackboardPlugIn
                .get(workbenchContext);
        // restore Statusbar divider locations
        statusPanelSplitPane1.setDividerLocation(blackboard.get(
                STATUSPANEL_DIVIDER_LOCATION_1, 200));
        statusPanelSplitPane2.setDividerLocation(blackboard.get(
                STATUSPANEL_DIVIDER_LOCATION_2, 200));
        statusPanelSplitPane3.setDividerLocation(blackboard.get(
                STATUSPANEL_DIVIDER_LOCATION_3, 100));
        statusPanelSplitPane4.setDividerLocation(blackboard.get(
                STATUSPANEL_DIVIDER_LOCATION_4, 100));
        desktopStatusSplit.setDividerLocation(blackboard.get(
                DESKTOPSTATUS_DIVIDER_LOCATION, -1));

        Dimension d;
        if (blackboard.get(WIDTH_KEY) == null) {
            d = initWindowSize();
            blackboard.put(WIDTH_KEY, d.width);
            blackboard.put(HEIGHT_KEY, d.height);
        }
        d = new Dimension();
        d.width = ((Integer) blackboard.get(WIDTH_KEY)).intValue();
        d.height = ((Integer) blackboard.get(HEIGHT_KEY)).intValue();
        return d;
    }

    public void restore() {
        setSize(recallWindowSize());
        setLocation(recallWindowLocation());
        if (recallMaximizedState())
            setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    /**
     * @return the taskListeners
     */
    public ArrayList<TaskListener> getTaskListeners() {
        return taskListeners;
    }

    /**
     * Add's a TaskListener, wich will be fired if a Task was added via the
     * WorkbenchFrame.addTaskFrame(TaskFrame taskFrame) or the a Task was loaded
     * completly with all his layers.
     * 
     * @param l
     *            - The TaskListener to add.
     */
    public void addTaskListener(TaskListener l) {
        getTaskListeners().add(l);
    }

    /**
     * Remove's a TaskListener.
     * 
     * @param l
     *            - The TaskListener to add.
     */
    public void removeTaskListener(TaskListener l) {
        getTaskListeners().remove(l);
    }

    private class AppleHandler {
        public void register() {
            // import com.apple.eawt.AboutHandler;
            // import com.apple.eawt.AppEvent.AboutEvent;
            // import com.apple.eawt.AppEvent.QuitEvent;
            // import com.apple.eawt.Application;
            // import com.apple.eawt.QuitHandler;
            // import com.apple.eawt.QuitResponse;

            com.apple.eawt.Application app = com.apple.eawt.Application
                    .getApplication();
            app.setQuitHandler(new com.apple.eawt.QuitHandler() {
                public void handleQuitRequestWith(
                        com.apple.eawt.AppEvent.QuitEvent e,
                        com.apple.eawt.QuitResponse resp) {
                    closeApplication();
                    // still here?, must have been cancelled
                    resp.cancelQuit();
                }
            });
            app.setAboutHandler(new com.apple.eawt.AboutHandler() {
                public void handleAbout(com.apple.eawt.AppEvent.AboutEvent e) {
                    AboutDialog.instance(getContext()).setVisible(true);
                }
            });
        }
    }

    // run a plugin internally, used for the statusbar
    private boolean executePlugin(PlugIn plugin) {
      try {
        String error = plugin instanceof EnableChecked ? ((EnableChecked) plugin)
            .getEnableCheck().check(new JLabel()) : "";
        if (!StringUtils.isEmpty(error)) {
          warnUser(error);
          return false;
        }
        return plugin.execute(workbenchContext.createPlugInContext());
      } catch (Exception er) {
        handleThrowable(er);
        return false;
      }
    }
    
}