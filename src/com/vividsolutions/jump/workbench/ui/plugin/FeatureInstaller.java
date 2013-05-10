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
package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openjump.core.CheckOS;
import org.openjump.core.ui.plugin.AbstractUiPlugIn;
import org.openjump.core.ui.swing.listener.EnableCheckMenuItemShownListener;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.EnableChecked;
import com.vividsolutions.jump.workbench.plugin.Iconified;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.ShortcutEnabled;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.ShortcutPluginExecuteKeyListener;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

/**
 * Adds a plug-in to the JUMP Workbench as a menu item.
 */
// TODO - Refactoring: Rename this class to PlugInMenuInstaller [Jon Aquino
// 10/22/2003]
public class FeatureInstaller {
  private interface Menu {

    void insert(JMenuItem menuItem, int i);

    String getText();

    int getItemCount();

    void add(JMenuItem menuItem);

  }

  private static FeatureInstaller instance = null;

  private WorkbenchContext workbenchContext;

  private TaskMonitorManager taskMonitorManager = new TaskMonitorManager();

  private EnableCheckFactory checkFactory;

  private static Map plugin_EnableCheckMap = new HashMap();
  private static Map repeatMenuItemMap = new HashMap();
  private static RepeatableMenuItem[] RepeatableMenuItemArray = { null, null,
      null };

  public FeatureInstaller(WorkbenchContext workbenchContext) {
    this.workbenchContext = workbenchContext;
    checkFactory = new EnableCheckFactory(workbenchContext);
  }

  public static final FeatureInstaller getInstance() {
    if (instance == null)
      instance = new FeatureInstaller(JUMPWorkbench.getInstance().getContext());
    return instance;
  }

  /** @deprecated Use the EnableCheckFactory methods instead */
  public MultiEnableCheck createLayersSelectedCheck() {
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck()).add(
        checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
  }

  /** @deprecated Use the EnableCheckFactory methods instead */
  public MultiEnableCheck createOneLayerSelectedCheck() {
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck()).add(
        checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
  }

  /** @deprecated Use the EnableCheckFactory methods instead */
  public MultiEnableCheck createVectorsExistCheck() {
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck()).add(
        checkFactory.createAtLeastNVectorsMustBeDrawnCheck(1));
  }

  /** @deprecated Use the EnableCheckFactory methods instead */
  public MultiEnableCheck createFenceExistsCheck() {
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck()).add(
        checkFactory.createFenceMustBeDrawnCheck());
  }

  public void addMenuSeparator(String menu) {
    addMenuSeparator(new String[] { menu });
  }

  public void addMenuSeparator(String[] menuPath) {
    JMenu mainMenu = menuBarMenu(menuPath[0]);
    addMenuSeparator(createMenusIfNecessary(mainMenu, behead(menuPath)));
  }

  public void addMenuSeparator(JMenu menu) {
    int pos = -1;
    // protect the first two FILE menu entries, separator + exit item
    int count = menu.getMenuComponentCount();
    if (menu.getText().equals(MenuNames.FILE) && (pos < 0 || pos >= count - 2))
      pos = count - 2;

    if (pos < 0)
      menu.addSeparator();
    else
      menu.insertSeparator(pos);
  }

  private void associate(JMenuItem menuItem, PlugIn plugIn) {
    menuItem.addActionListener(AbstractPlugIn.toActionListener(plugIn,
        workbenchContext, taskMonitorManager));
  }

  private String[] behead(String[] a1) {
    String[] a2 = new String[a1.length - 1];
    System.arraycopy(a1, 1, a2, 0, a2.length);
    return a2;
  }

  /**
   * @deprecated
   */
  public void addLayerViewMenuItem(PlugIn executable, String menuName,
      String menuItemName) {
    addLayerViewMenuItem(executable, new String[] { menuName }, menuItemName);
  }

  /**
   * @deprecated
   */
  public void addLayerNameViewMenuItem(PlugIn executable, String menuName,
      String menuItemName) {
    addLayerNameViewMenuItem(executable, new String[] { menuName },
        menuItemName);
  }

  /**
   * Add a menu item to the main menu that is enabled only if the active
   * internal frame is a LayerViewPanelProxy.
   * 
   * @deprecated
   */
  public void addLayerViewMenuItem(PlugIn executable, String[] menuPath,
      String menuItemName) {
    addMainMenuItem(executable, menuPath, menuItemName, false, null,
        checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());
  }

  /**
   * Add a menu item to the main menu that is enabled only if the active
   * internal frame is a LayerViewPanelProxy and a LayerNamePanelProxy.
   * 
   * @deprecated
   */
  public void addLayerNameViewMenuItem(PlugIn executable, String[] menuPath,
      String menuItemName) {
    addMainMenuItem(
        executable,
        menuPath,
        menuItemName,
        false,
        null,
        new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck()));
  }

  /**
   * @deprecated
   */
  public void addMainMenuItem(PlugIn executable, String menuName,
      String menuItemName, Icon icon, EnableCheck enableCheck) {
    addMainMenuPlugin(executable, new String[] { menuItemName }, menuItemName,
        false, icon, enableCheck);
  }

  /**
   * @param menuPath
   *          separate items with slashes; items will be created if they do not
   *          already exist
   * @param menuItemName
   *          name of the menu item
   * @param checkBox
   *          whether to create a JCheckBoxMenuItem or a JMenuItem
   * @param icon
   *          an Icon or null
   * @param enableCheck
   *          conditions to make the plugin available to the user
   * @see GUIUtil#toSmallIcon
   * 
   * @deprecated
   */
  public void addMainMenuItem(PlugIn executable, String[] menuPath,
      String menuItemName, boolean checkBox, Icon icon, EnableCheck enableCheck) {
    Map properties = extractProperties(menuItemName);
    int pos = -1;
    if (properties.get("pos") != null) {
      pos = Integer.parseInt((String) properties.get("pos"));
    }
    menuItemName = removeProperties(menuItemName);
    addMainMenuPlugin(executable, menuPath, menuItemName, checkBox, icon,
        enableCheck, pos);
  }

  /**
   * Replacement for the retired methods above and below. Lot's of existing
   * plugins depend on these methods. Rationale was to have the methods return
   * the menuitem to the plugin for further manipulation & attaching listeners
   * etc. Unfortunately the return type is not part of the java method
   * footprint, so we couldn't just change the return type but had to modify the
   * name as well. Also the position parameter was added as it was needed
   * anyway.
   * 
   * @param executable
   * @param menuPath
   *          string array of sub menu entries to place the entry in
   * @param menuItemName
   *          name of the menu item
   * @param checkBox
   *          whether to create a JCheckBoxMenuItem or a JMenuItem
   * @param icon
   *          an Icon or null
   * @param enableCheck
   *          conditions to make the plugin available to the user
   * @param pos
   *          defines the position of the menu item in the menu
   * 
   * @see GUIUtil#toSmallIcon
   */
  public JMenuItem addMainMenuPlugin(final PlugIn executable,
      final String[] menuPath, String menuItemName, final boolean checkBox,
      final Icon icon, final EnableCheck enableCheck, final int pos) {

    JMenuItem menuItem = createMenuItem(menuItemName, checkBox);
    menuItem = addMainMenuPluginItemWithPostProcessing(executable, menuPath,
        menuItem, icon, enableCheck, pos);
    return menuItem;
  }

  /**
   * this is merely decorative. the keys are intercepted in WorkbenchFrame and
   * handled by an global key listener which also consumes them thereafter.
   * 
   * @param menuItem
   * @param executable
   */
  private void assignShortcut(JMenuItem menuItem, PlugIn executable) {
    if (executable instanceof ShortcutEnabled) {
      KeyStroke st = ((ShortcutEnabled) executable).getShortcutKeyStroke();
      if (st == null || st.getKeyCode() < 1)
        return;
      // filter according to platform first
      st = ShortcutPluginExecuteKeyListener.getPlatformKeyStroke(st);
      menuItem.setAccelerator(st);
      // register with workbench (usually done in JumpConfiguration, Pluginmanager)
      // for cases where plugins initialize themselves and "forgot" to do it
      if (JUMPWorkbench.getInstance().getFrame().getKeyboardShortcutPlugin(st)==null)
        AbstractPlugIn.registerShortcuts(executable);
    }
  }

  private JMenuItem createMenuItem(String menuItemName, boolean checkBox) {
    return checkBox ? new JCheckBoxMenuItem(menuItemName) : 
                      new JMenuItem(menuItemName);
  }

  public JMenuItem addMainMenuPlugin(final PlugIn executable,
      final String[] menuPath) {
    // icon and check are fetched in addmainMenuPluginItemWithPostProcessing()
    return addMainMenuPlugin(executable, menuPath, executable.getName(), false,
        null, null, -1);
  }

  /**
   * Convenience method without position parameter.
   * 
   * @see #addMainMenuPlugin(final PlugIn executable, final String[] menuPath,
   * String menuItemName, final boolean checkBox, final Icon icon, final
   * EnableCheck enableCheck, final int pos)
   */
  public JMenuItem addMainMenuPlugin(final PlugIn executable,
      final String[] menuPath, String menuItemName, final boolean checkBox,
      final Icon icon, final EnableCheck enableCheck) {
    return addMainMenuPlugin(executable, menuPath, menuItemName, checkBox,
        icon, enableCheck, -1);
  }

  /**
   * The catch all for all methods. It tries to add icon and shortcut.
   * 
   * @param executable
   * @param menuPath
   * @param menuItem
   * @param icon
   * @param enableCheck
   * @param pos
   * @return menu item
   */
  private JMenuItem addMainMenuPluginItemWithPostProcessing(PlugIn executable,
      String[] menuPath, JMenuItem menuItem, Icon icon,
      EnableCheck enableCheck, int pos) {
    // fetch a check
    enableCheck = enableCheck == null
        && executable instanceof EnableChecked ? ((EnableChecked) executable)
        .getEnableCheck() : enableCheck;
    addMainMenuPluginItem(executable, menuPath, menuItem, enableCheck, pos);
    // icon
    addMenuItemIcon(
        menuItem,
        icon == null && executable instanceof Iconified ? ((Iconified) executable)
            .getIcon(16) : icon);
    // shortcut
    assignShortcut(menuItem, executable);
    return menuItem;
  }

  /**
   * Generic addMainMenu method. This is an internal utility method. If you want
   * to finetune your MenuItem consider adding it via addMainMenuPlugin() and
   * modifying the returned MenuItem.
   * 
   * @param plugin
   *          the plugin to execute with this item
   * @param menuPath
   *          the menu path made of the menu and submenu names
   * @param menuItem
   *          the JMenuItem (or JCheckBoxMenuItem or JRadioButtonMenuItem) to
   *          the parent menu
   * @param enableCheck
   *          conditions making the plugin enabled
   * @param pos
   *          defines the position of menuItem in the menu -1 adds menuItem at
   *          the end of the menu except for FILE menu where -1 adds menuItem
   *          before the separator preceding exit menuItem
   */
  private JMenuItem addMainMenuPluginItem(PlugIn plugin, String[] menuPath,
      JMenuItem menuItem, EnableCheck enableCheck, int pos) {

    JMenu menu = menuBarMenu(menuPath[0]);
    if (menu == null) {
      menu = (JMenu) installMnemonic(new JMenu(menuPath[0]), menuBar());
      addToMenuBar(menu);
    }
    JMenu parent = createMenusIfNecessary(menu, behead(menuPath));

    if (menuItem.getText().trim().length() == 0) {
      menuItem.setText(plugin.getName());
    }
    installMnemonic(menuItem, parent);
    associate(menuItem, plugin);

    // System.out.println("fi "+plugin.getName()+" pos "+pos+" count "+parent.getMenuComponentCount());
    // protect the first two FILE menu entries, separator + exit item
    int count = menu.getMenuComponentCount();
    if (parent.getText().equals(MenuNames.FILE)
        && (pos < 0 || pos >= count - 2)) {
      pos = count - 2;
      // System.out.println("fi "+pos);
    }

    if (pos >= 0) {
      parent.insert(menuItem, pos);
    } else {
      parent.add(menuItem);
    }
    if (enableCheck != null) {
      addMenuItemShownListener(menuItem, toMenuItemShownListener(enableCheck));
    }
    return menuItem;
  }

  /**
   * Add a Plugin as a JMenuItem with enableCheck conditions.
   * 
   * @param menuPath
   *          path from the main menu to the menu item
   * @param plugin
   *          the plugin associated to this menu item
   *          
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(String[] menuPath, AbstractUiPlugIn plugin) {
    return addMainMenuPlugin(plugin, menuPath);
  }

  /**
   * Add a Plugin as a JMenuItem with enableCheck conditions.
   * 
   * @param menuPath
   *          path from the main menu to the menu item
   * @param plugin
   *          the plugin associated to this menu item
   * @param pos
   *          defines the position of the menu item in the menu -1 adds menuItem
   *          at the end except for FILE menu where -1 adds menuItem before the
   *          separator preceding exit menu item
   *          
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(final String[] menuPath,
      final AbstractUiPlugIn plugin, final int pos) {
    return addMainMenuPlugin(plugin, menuPath, plugin.getName(), false,
        plugin.getIcon(), plugin.getEnableCheck(), pos);
  }

  /**
   * Add a Plugin as a JMenuItem with enableCheck conditions.
   * 
   * @param menuPath
   *          path from the main menu to the menu item
   * @param plugin
   *          the plugin associated to this menu item
   * @param enableCheck
   *          conditions making the plugin enabled
   * 
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(final String[] menuPath,
      final AbstractUiPlugIn plugin, final EnableCheck enableCheck) {
    return addMainMenuPlugin(plugin, menuPath, plugin.getName(), false,
        plugin.getIcon(), enableCheck);
  }

  /**
   * Add a Plugin as a JMenuItem with enableCheck
   * 
   * @param menuPath
   *          path from the main menu to the menu item
   * @param plugin
   *          the plugin associated to this menu item
   * @param enableCheck
   *          conditions making the plugin enabled
   * @param pos
   *          defines the position of the menu item in the menu -1 adds menuItem
   *          at the end except for FILE menu where -1 adds menuItem before the
   *          separator preceding exit menu item
   * 
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(final String[] menuPath,
      final AbstractUiPlugIn plugin, final EnableCheck enableCheck,
      final int pos) {
    return addMainMenuPlugin(plugin, menuPath, plugin.getName(), false,
        plugin.getIcon(), enableCheck, pos);
  }

  /**
   * Add a Plugin as a JMenuItem or a subclass of JMenuItem to the main menu
   * 
   * @param menuPath
   *          path from the main menu to the menu item
   * @param plugin
   *          the plugin associated to this menu item
   * @param menuItem
   *          the menu item (JMenuItem, JCheckBoxMenuItem, JMenu,
   *          JRadioButtonMenuItem)
   * @param pos
   *          defines the position of the menu item in the menu -1 adds menuItem
   *          at the end except for FILE menu where -1 adds menuItem before the
   *          separator preceding exit menu item
   * 
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(final String[] menuPath,
      final AbstractUiPlugIn plugin, final JMenuItem menuItem, final int pos) {
    return addMainMenuPluginItemWithPostProcessing(plugin, menuPath, menuItem,
        null, null, pos);
  }

  /**
   * New generic addMainMenuItem method using AbstractUiPlugIn.
   * 
   * @param menuPath
   *          the menu path made of the menu and submenu names
   * @param plugin
   *          the plugin to execute with this item
   * @param menuItem
   *          the JMenuItem (or JCheckBoxMenuItem or JRadioButtonMenuItem) to
   *          the parent menu
   * @param enableCheck
   *          conditions making the plugin enabled
   * @param pos
   *          defines the position of the menu item in the menu -1 adds menuItem
   *          at the end except for FILE menu where -1 adds menuItem before the
   *          separator preceding exit menu item
   * 
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(final String[] menuPath,
      final AbstractUiPlugIn plugin, final JMenuItem menuItem,
      final EnableCheck enableCheck, final int pos) {

    return addMainMenuPluginItemWithPostProcessing(plugin, menuPath, menuItem,
        null, enableCheck, pos);
  }

  /**
   * New generic addMainMenuItem method. Adds menuItem at the end of the menu
   * except for FILE menu where menuItem is added before the separator preceding
   * exit menu item
   * 
   * @param plugin
   *          the plugin to execute with this item
   * @param menuPath
   *          the menu path made of the menu and submenu names
   * @param menuItem
   *          the JMenuItem (or JCheckBoxMenuItem or JRadioButtonMenuItem) to
   *          the parent menu
   * @param enableCheck
   *          conditions making the plugin enabled
   * 
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(PlugIn plugin, String[] menuPath,
      JMenuItem menuItem, EnableCheck enableCheck) {
    return addMainMenuPluginItemWithPostProcessing(plugin, menuPath, menuItem,
        null, enableCheck, -1);
  }

  /**
   * New generic addMainMenuItem method.
   * 
   * @param plugin
   *          the plugin to execute with this item
   * @param menuPath
   *          the menu path made of the menu and submenu names
   * @param menuItem
   *          the JMenuItem (or JCheckBoxMenuItem or JRadioButtonMenuItem) to
   *          the parent menu
   * @param enableCheck
   *          conditions making the plugin enabled
   * @param pos
   *          defines the position of menuItem in the menu -1 adds menuItem at
   *          the end of the menu except for FILE menu where -1 adds menuItem
   *          before the separator preceding exit menuItem
   * 
   * @deprecated use addMainMenuPlugin() instead
   */
  public JMenuItem addMainMenuItem(PlugIn plugin, String[] menuPath,
      JMenuItem menuItem, EnableCheck enableCheck, int pos) {
    return addMainMenuPluginItemWithPostProcessing(plugin, menuPath, menuItem,
        null, enableCheck, pos);
  }

  // workaround for checkbox tick missing in windows laf on windows vista/7
  // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7122141
  // we simply leave out the icon, so the tick is displayed instead of the
  // icon with a blue background
  private boolean vista_checkbox_workaround = CheckOS.isWindows()
      && Float.valueOf(System.getProperty("os.version")) >= 6
      && UIManager.getLookAndFeel().getClass().getName()
          .equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")
      // check windows vista/7 really uses native l&f, not Windows Classic 
      && UIManager.get("CheckBoxMenuItem.checkIconFactory")!=null;

  private void addMenuItemIcon(JMenuItem menuItem, Icon icon) {
    // no icons for radio/checkbox on windows laf on vista+, the "highlighted"
    // icon is too close to the normal icon so we stay with the radio or tick
    // TODO: this obviously does not work when skin is switched during runtime,
    // as items are created with or w/o icon during startup. however it will
    // work correctly when the new skin is restored after a restart
    // [ ede 5.4.2012 ]
    if (vista_checkbox_workaround
        && (menuItem instanceof JRadioButtonMenuItem || menuItem instanceof JCheckBoxMenuItem))
      return;
    // ignore null values
    if (icon instanceof Icon)
      menuItem.setIcon(icon);
  }

  private Menu createMenu(final JMenu menu) {
    return new Menu() {

      public void insert(JMenuItem menuItem, int i) {
        menu.insert(menuItem, i);
      }

      public String getText() {
        return menu.getText();
      }

      public int getItemCount() {
        return menu.getItemCount();
      }

      public void add(JMenuItem menuItem) {
        menu.add(menuItem);
      }
    };
  }

  private void insert(final JMenuItem menuItem, Menu parent, Map properties) {
    if (properties.get("pos") != null) {
      parent.insert(menuItem, Integer.parseInt((String) properties.get("pos")));
    } else if (parent.getText().equals(MenuNames.FILE)) {
      // If menu is File, insert menu item just before Exit and its
      // separator [Jon Aquino]
      parent.insert(menuItem, parent.getItemCount() - 2);
    } else {
      parent.add(menuItem);
    }
  }

  private Map extractProperties(String menuItemName) {
    if (menuItemName.indexOf('{') == -1) {
      return new HashMap();
    }
    Map properties = new HashMap();
    String s = menuItemName.substring(menuItemName.indexOf('{') + 1,
        menuItemName.indexOf('}'));
    for (Iterator i = StringUtil.fromCommaDelimitedString(s).iterator(); i
        .hasNext();) {
      String property = (String) i.next();
      properties.put(property.substring(0, property.indexOf(':')).trim(),
          property.substring(property.indexOf(':') + 1, property.length())
              .trim());
    }
    return properties;
  }

  public static String removeProperties(String menuItemName) {
    return menuItemName.indexOf('{') > -1 ? menuItemName.substring(0,
        menuItemName.indexOf('{')) : menuItemName;
  }

  public static JMenuItem installMnemonic(JMenuItem menuItem, MenuElement parent) {
    String text = menuItem.getText();
    StringUtil.replaceAll(text, "&&", "##");
    int ampersandPosition = text.indexOf('&');
    if (-1 < ampersandPosition && ampersandPosition + 1 < text.length()) {
      menuItem.setMnemonic(text.charAt(ampersandPosition + 1));
      text = StringUtil.replace(text, "&", "", false);
    } else {
      installDefaultMnemonic(menuItem, parent);
    }
    // Double-ampersands get converted to single-ampersands. [Jon Aquino]
    StringUtil.replaceAll(text, "##", "&");
    menuItem.setText(text);
    return menuItem;
  }

  private static void installDefaultMnemonic(JMenuItem menuItem,
      MenuElement parent) {
    outer: for (int i = 0; i < menuItem.getText().length(); i++) {
      // Swing stores mnemonics in upper case [Jon Aquino]
      char candidate = Character.toUpperCase(menuItem.getText().charAt(i));
      if (!Character.isLetter(candidate)) {
        continue;
      }
      for (Iterator j = menuItems(parent).iterator(); j.hasNext();) {
        JMenuItem other = (JMenuItem) j.next();
        if (other.getMnemonic() == candidate) {
          continue outer;
        }
      }
      menuItem.setMnemonic(candidate);
      return;
    }
    menuItem.setMnemonic(menuItem.getText().charAt(0));
  }

  private static Collection menuItems(MenuElement element) {
    ArrayList menuItems = new ArrayList();
    if (element instanceof JMenuBar) {
      for (int i = 0; i < ((JMenuBar) element).getMenuCount(); i++) {
        CollectionUtil.addIfNotNull(((JMenuBar) element).getMenu(i), menuItems);
      }
    } else if (element instanceof JMenu) {
      for (int i = 0; i < ((JMenu) element).getItemCount(); i++) {
        CollectionUtil.addIfNotNull(((JMenu) element).getItem(i), menuItems);
      }
    } else if (element instanceof JPopupMenu) {
      MenuElement[] children = ((JPopupMenu) element).getSubElements();
      for (int i = 0; i < children.length; i++) {
        if (children[i] instanceof JMenuItem) {
          menuItems.add(children[i]);
        }
      }
    } else {
      Assert.shouldNeverReachHere(element.getClass().getName());
    }
    return menuItems;
  }

  private MenuItemShownListener toMenuItemShownListener(
      final EnableCheck enableCheck) {
    return new EnableCheckMenuItemShownListener(workbenchContext, enableCheck);
  }

  /**
   * @return the leaf
   */
  public JMenu createMenusIfNecessary(JMenu parent, String[] menuPath) {
    if (menuPath.length == 0) {
      return parent;
    }
    JMenu child = (JMenu) childMenuItem(menuPath[0], parent);
    if (child == null) {
      child = (JMenu) installMnemonic(new JMenu(menuPath[0]), parent);
      parent.add(child);
    }
    return createMenusIfNecessary(child, behead(menuPath));
  }

  public void addMenuItemShownListener(final JMenuItem menuItem,
      final MenuItemShownListener menuItemShownListener) {
    JMenu menu = (JMenu) ((JPopupMenu) menuItem.getParent()).getInvoker();
    // wraps MenuListener in the JumpMenuListener wrapper class so that
    // EasyPanel can determine which menu items had EnableChecks [Larry Becker]
    menu.addMenuListener(new JumpMenuListener(menuItemShownListener, menuItem));
  }

  /**
   * Ultimate convenience method for attaching a plugin to a popummenu.
   * 
   * @param popupMenu
   * @param executable
   * @return menu item
   */
  public JMenuItem addPopupMenuPlugin(JPopupMenu popupMenu, PlugIn executable) {
    return addPopupMenuPlugin(popupMenu, executable, new String[]{},
        null, false, null, null);
  }

  /**
   * Convenience method for entries with menupath
   * 
   * @return menu item
   */
  public JMenuItem addPopupMenuPlugin(JPopupMenu popupMenu,
      PlugIn executable, String[] menuPath) {
    return addPopupMenuPlugin(popupMenu, executable, menuPath, null, false,
        null, null);
  }
  
  /**
   * Convenience method for entries without menupath
   * 
   * @param checkBox
   *          true for item to be checkable
   * @param enableCheck
   *          , icon null to leave unspecified
   * @return menu item
   */
  public JMenuItem addPopupMenuPlugin(JPopupMenu popupMenu, PlugIn executable,
      String menuItemName, boolean checkBox, Icon icon, EnableCheck enableCheck) {
    return addPopupMenuPlugin(popupMenu, executable, new String[] {},
        menuItemName, checkBox, icon, enableCheck);
  }

  /**
   * Analogue to addMainMenuPlugin(). Adds a plugin to a popup menu.
   * 
   * @param popupMenu
   * @param executable
   * @param menuPath
   * @param menuItemName
   * @param checkBox
   * @param icon
   * @param enableCheck
   * @return menu item
   */
  public JMenuItem addPopupMenuPlugin(JPopupMenu popupMenu, PlugIn executable,
      String[] menuPath, String menuItemName, boolean checkBox, Icon icon,
      EnableCheck enableCheck) {

    // ensure we got a name
    if (menuItemName == null)
      menuItemName = executable.getName();
    // icon
    icon = (icon == null) && (executable instanceof Iconified) ? ((Iconified) executable)
        .getIcon(16) : icon;
    // create item
    JMenuItem menuItem = createPopupMenuItem(popupMenu, menuItemName, checkBox,
        icon);
    // add plugin action to it
    associate(menuItem, executable);
    // fetch a check if none
    enableCheck = (enableCheck == null)
        && (executable instanceof EnableChecked) ? ((EnableChecked) executable)
        .getEnableCheck() : enableCheck;
    // add
    addPopupMenuItem(popupMenu, executable, menuPath, menuItem,
            extractProperties(menuItemName), enableCheck);

    // shortcut
    assignShortcut(menuItem, executable);
    return menuItem;
  }

  /**
   * @deprecated use addPopupMenuPlugin instead
   */
  public void addPopupMenuItem(JPopupMenu popupMenu, PlugIn executable,
      String menuItemName, boolean checkBox, Icon icon, EnableCheck enableCheck) {
    addPopupMenuPlugin(popupMenu, executable, menuItemName, checkBox, icon,
        enableCheck);
  }

  /**
   * @deprecated use addPopupMenuPlugin instead
   */
  public void addPopupMenuItem(JPopupMenu popupMenu, PlugIn executable,
      String[] menuPath, String menuItemName, boolean checkBox, Icon icon,
      EnableCheck enableCheck) {
    addPopupMenuPlugin(popupMenu, executable, menuPath, menuItemName, checkBox,
        icon, enableCheck);
  }

  /**
   * Creates a popup menu item for the methods above TODO: the whole Mnemonic
   * attachment routing can be done better
   */
  private JMenuItem createPopupMenuItem(JPopupMenu popupMenu,
      String menuItemName, boolean checkBox, Icon icon) {
    Map properties = extractProperties(menuItemName);
    menuItemName = removeProperties(menuItemName);
    JMenuItem menuItem = installMnemonic(
        createMenuItem(menuItemName, checkBox), popupMenu);
    addMenuItemIcon(menuItem, icon);
    return menuItem;
  }

  private void addPopupMenuItem(JPopupMenu popupMenu, final PlugIn executable,
      final String[] menuPath, final JMenuItem menuItem, Map properties,
      final EnableCheck enableCheck) {

    if (menuPath == null || menuPath.length == 0) {
      insert(menuItem, createMenu(popupMenu), properties);
    } else {
      JMenu menu = popupMenu(popupMenu, menuPath[0]);
      if (menu == null) {
        menu = (JMenu) popupMenu.add(new JMenu(menuPath[0]));
      }
      JMenu parent = createMenusIfNecessary(menu, behead(menuPath));
      insert(menuItem, createMenu(parent), properties);
    }
    if (enableCheck != null) {
      popupMenu.addPopupMenuListener(new PopupMenuListener() {
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
          toMenuItemShownListener(enableCheck).menuItemShown(menuItem);
        }
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }
        public void popupMenuCanceled(PopupMenuEvent e) {
        }
      });
    }
  }

  public void addPopupMenuSeparator(JPopupMenu popupMenu, String[] menuPath) {
    if (menuPath == null || menuPath.length == 0) {
      popupMenu.addSeparator();
    } else {
      JMenu menu = popupMenu(popupMenu, menuPath[0]);
      if (menu == null) {
        menu = (JMenu) popupMenu.add(new JMenu(menuPath[0]));
      }
      JMenu parent = createMenusIfNecessary(menu, behead(menuPath));
      parent.addSeparator();
    }
  }

  /**
   * @return the menu with the given name, or null if no such menu exists
   */
  public static JMenu popupMenu(JPopupMenu popupMenu, String childName) {
    MenuElement[] subElements = popupMenu.getSubElements();
    for (int i = 0; i < subElements.length; i++) {
      if (!(subElements[i] instanceof JMenuItem)) {
        continue;
      }
      JMenuItem menuItem = (JMenuItem) subElements[i];
      if (menuItem.getText().equals(childName)) {
        return (JMenu) menuItem;
      }
    }
    return null;
  }

  private Menu createMenu(final JPopupMenu popupMenu) {
    return new Menu() {

      public void insert(JMenuItem menuItem, int i) {
        popupMenu.insert(menuItem, i);
      }

      public String getText() {
        return "";
      }

      public int getItemCount() {
        return popupMenu.getComponentCount();
      }

      public void add(JMenuItem menuItem) {
        popupMenu.add(menuItem);
      }
    };
  }

  public JMenuBar menuBar() {
    return workbenchContext.getWorkbench().getFrame().getJMenuBar();
  }

  /**
   * @return the menu with the given name, or null if no such menu exists
   */
  public JMenu menuBarMenu(String childName) {
    MenuElement[] subElements = menuBar().getSubElements();
    for (int i = 0; i < subElements.length; i++) {
      if (!(subElements[i] instanceof JMenuItem)) {
        continue;
      }
      JMenuItem menuItem = (JMenuItem) subElements[i];
      if (menuItem.getText().equals(childName)) {
        return (JMenu) menuItem;
      }
    }
    return null;
  }

  private void addToMenuBar(JMenu menu) {
    menuBar().add(menu);
    // Ensure Window and Help are placed at the end. Remove #windowMenu and
    // #helpMenu
    // *after* adding #menu, because #menu might be the Window or Help menu!
    // [Jon Aquino]
    JMenu windowMenu = menuBarMenu(MenuNames.WINDOW);
    JMenu helpMenu = menuBarMenu(MenuNames.HELP);
    // Customized workbenches may not have Window or Help menus [Jon Aquino]
    if (windowMenu != null) {
      menuBar().remove(windowMenu);
    }
    if (helpMenu != null) {
      menuBar().remove(helpMenu);
    }
    if (windowMenu != null) {
      menuBar().add(windowMenu);
    }
    if (helpMenu != null) {
      menuBar().add(helpMenu);
    }
  }

  /**
   * Find the first occurence of a menu item with the given name and return it.
   * 
   * @param childName
   * @param menu
   * @return JMenuItem
   */
  public static JMenuItem childMenuItem(String childName, MenuElement menu) {
    if (menu instanceof JMenu) {
      return childMenuItem(childName, ((JMenu) menu).getPopupMenu());
    }
    MenuElement[] childMenuItems = menu.getSubElements();
    for (int i = 0; i < childMenuItems.length; i++) {
      if (childMenuItems[i] instanceof JMenuItem
          && ((JMenuItem) childMenuItems[i]).getText().equals(childName)) {
        return ((JMenuItem) childMenuItems[i]);
      }
    }
    return null;
  }

  // OJ is java 1.5 since a while now, commented this [ede 1. April 2012]

  // /**
  // * Workaround for Java Bug 4809393: "Menus disappear prematurely after
  // * displaying modal dialog" Evidently fixed in Java 1.5. The workaround is
  // to
  // * wrap #actionPerformed with SwingUtilities#invokeLater.
  // */
  // public void addMainMenuItemWithJava14Fix(PlugIn executable,
  // String[] menuPath, String menuItemName, boolean checkBox, Icon icon,
  // EnableCheck enableCheck) {
  // addMainMenuItem(executable, menuPath, menuItemName, checkBox, icon,
  // enableCheck);
  // JMenuItem menuItem = FeatureInstaller.childMenuItem(
  // FeatureInstaller.removeProperties(menuItemName),
  // ((JMenu)createMenusIfNecessary(menuBarMenu(menuPath[0]),
  // behead(menuPath))));
  // final ActionListener listener =
  // abstractPlugInActionListener(menuItem.getActionListeners());
  // menuItem.removeActionListener(listener);
  // menuItem.addActionListener(new ActionListener() {
  // public void actionPerformed(final ActionEvent e) {
  // SwingUtilities.invokeLater(new Runnable() {
  // public void run() {
  // listener.actionPerformed(e);
  // }
  // });
  // }
  // });
  // }

  private ActionListener abstractPlugInActionListener(
      ActionListener[] actionListeners) {
    for (int i = 0; i < actionListeners.length; i++) {
      if (actionListeners[i].getClass().getName()
          .indexOf(AbstractPlugIn.class.getName()) > -1) {
        return actionListeners[i];
      }
    }
    Assert.shouldNeverReachHere();
    return null;
  }

  /**
   * Create and add a sub menu entry. index < 0 adds it to the end. 
   * @param featureInstaller
   * @param menuPath
   * @param menuName
   * @param index
   * @return
   */
  public static JMenu addMainMenu(FeatureInstaller featureInstaller,
      final String[] menuPath, String menuName, int index) {
    JMenu menu = new JMenu(menuName);
    JMenu parent = featureInstaller.createMenusIfNecessary(
        featureInstaller.menuBarMenu(menuPath[0]),
        featureInstaller.behead(menuPath));
    if (index>=0)
      parent.insert(menu, index);
    else
      parent.add(menu);
    return menu;
  }

  public void associateWithRepeat(PlugIn executable) {
    JPopupMenu popupMenu = LayerViewPanel.popupMenu();

    if (!plugin_EnableCheckMap.containsKey(executable))
      return;

    // if this executable already associated with a repeat menu then nothing to
    // do
    for (int i = 0; i < RepeatableMenuItemArray.length; i++) {
      if (RepeatableMenuItemArray[i] != null)
        if (RepeatableMenuItemArray[i].isSetTo(executable))
          return;
    }

    // we are going to be moving executables to different repeatMenuItems
    // so remove all menuListeners
    for (int i = 0; i < RepeatableMenuItemArray.length; i++) {
      if (RepeatableMenuItemArray[i] == null)
        break;
      PopupMenuListener menuListener = RepeatableMenuItemArray[i]
          .getMenuListener();
      if (menuListener != null)
        popupMenu.removePopupMenuListener(menuListener);
    }

    // this plugin not on list of repeats
    // so pull down existing repeats and add this to top

    for (int i = RepeatableMenuItemArray.length - 1; i > 0; i--) {
      if (RepeatableMenuItemArray[i - 1] == null) // nothing to move
        continue;

      // at this point we have one above to pull down
      PlugIn prevExec = RepeatableMenuItemArray[i - 1].getExecutable();
      EnableCheck prevEnableCheck = (EnableCheck) plugin_EnableCheckMap
          .get(prevExec);

      if (RepeatableMenuItemArray[i] == null) {
        RepeatableMenuItem repeatMenuItem = new RepeatableMenuItem(prevExec,
            prevEnableCheck);
        popupMenu.insert(repeatMenuItem.getMenuItem(), i + 3);
        RepeatableMenuItemArray[i] = repeatMenuItem;
      } else {
        RepeatableMenuItemArray[i].setExecutable(prevExec, prevEnableCheck);
      }
    }

    EnableCheck enableCheck = (EnableCheck) plugin_EnableCheckMap
        .get(executable);

    if (RepeatableMenuItemArray[0] == null) {
      RepeatableMenuItem repeatMenuItem = new RepeatableMenuItem(executable,
          enableCheck);
      popupMenu.insert(repeatMenuItem.getMenuItem(), 2);
      popupMenu.insert(new JPopupMenu.Separator(), 2);
      RepeatableMenuItemArray[0] = repeatMenuItem;
    } else {
      RepeatableMenuItemArray[0].setExecutable(executable, enableCheck);
    }

    // now add all listeners to popupMenu
    for (int i = 0; i < RepeatableMenuItemArray.length; i++) {
      if (RepeatableMenuItemArray[i] == null)
        break;
      PopupMenuListener menuListener = RepeatableMenuItemArray[i]
          .getMenuListener();
      if (menuListener != null)
        popupMenu.addPopupMenuListener(menuListener);
    }
  }

  private class RepeatableMenuItem {
    private PlugIn executable = null;
    private JMenuItem menuItem = null;
    private PopupMenuListener menuListener = null;

    public RepeatableMenuItem(PlugIn executable, final EnableCheck enableCheck) {
      this.menuItem = new JMenuItem("Repeat");
      setExecutable(executable, enableCheck);
    }

    public void setExecutable(PlugIn executable, final EnableCheck enableCheck) {
      this.executable = executable;
      ActionListener[] al = menuItem.getActionListeners();

      for (int i = 0; i < al.length; i++)
        menuItem.removeActionListener(al[0]);

      menuItem.addActionListener(AbstractPlugIn.toActionListener(executable,
          workbenchContext, taskMonitorManager));

      this.menuItem.setText("Repeat: " + executable.getName());

      if (enableCheck == null) {
        this.menuListener = null;
      } else {
        this.menuListener = new PopupMenuListener() {
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            toMenuItemShownListener(enableCheck).menuItemShown(menuItem);
          }

          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
          }

          public void popupMenuCanceled(PopupMenuEvent e) {
          }
        };
      }
    }

    JMenuItem getMenuItem() {
      return menuItem;
    }

    PlugIn getExecutable() {
      return executable;
    }

    PopupMenuListener getMenuListener() {
      return menuListener;
    }

    boolean isSetTo(PlugIn executable) {
      return this.executable == executable;
    }
  }

  /**
   * @author Larry Becker Needed this class to be not anonymous so it's type
   *         could be determined at runtime.
   */
  public class JumpMenuListener implements MenuListener {
    MenuItemShownListener menuItemShownListener;
    JMenuItem menuItem;

    public JumpMenuListener(MenuItemShownListener menuItemShownListener,
        JMenuItem menuItem) {
      super();
      this.menuItemShownListener = menuItemShownListener;
      this.menuItem = menuItem;
    }

    public void menuSelected(MenuEvent e) {
      menuItemShownListener.menuItemShown(menuItem);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

  }
}
