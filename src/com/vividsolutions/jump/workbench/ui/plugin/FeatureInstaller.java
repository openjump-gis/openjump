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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JPopupMenu.Separator;
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
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageContextMenu;
import org.openjump.core.ui.swing.listener.EnableCheckMenuItemShownListener;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.WorkbenchProperties;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.CheckBoxed;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.EnableChecked;
import com.vividsolutions.jump.workbench.plugin.Iconified;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.ShortcutEnabled;
import com.vividsolutions.jump.workbench.ui.AttributeTab;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.ShortcutPluginExecuteKeyListener;
import com.vividsolutions.jump.workbench.ui.TitledPopupMenu;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

/**
 * Adds a plug-in to the JUMP Workbench as a menu item.
 */
// TODO - Refactoring: Rename this class to PlugInMenuInstaller [Jon Aquino
// 10/22/2003]
public class FeatureInstaller {
  // generic interface to treat implementation differences of JMenu/JPopUpMenu
  public interface Menu {

    void add(JMenuItem menuItem);

    void insert(JMenuItem menuItem, int i);

    void remove(int i);

    String getText();

    int getComponentCount();

    Component getComponent(int i);

    JComponent getWrappee();

    void addSeparator();

    void insertSeparator(int i);
  }

  private static FeatureInstaller instance = null;

  private WorkbenchContext workbenchContext;

  private TaskMonitorManager taskMonitorManager = new TaskMonitorManager();

  private EnableCheckFactory checkFactory;

  private static HashMap<JMenuItem, PlugIn> menuItemRegistry = new HashMap();

  // private static Map repeatMenuItemMap = new HashMap();
  // private static RepeatableMenuItem[] RepeatableMenuItemArray = { null, null,
  // null };

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
    createMenusIfNecessary(wrapMenu(mainMenu), behead(menuPath)).addSeparator();
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
   * Create and add a sub menu entry. index < 0 adds it to the end.
   * 
   * @param featureInstaller
   * @param menuPath
   * @param menuName
   * @param index
   * @return
   */
  public static JMenu addMainMenu(FeatureInstaller featureInstaller,
      final String[] menuPath, String menuName, int pos) {
    if (menuPath == null)
      throw new IllegalArgumentException("menuPath must be a string array");

    JMenu menu = new JMenu(menuName);
    JMenuBar parent = FeatureInstaller.getInstance().menuBar();

    List fullPathList = new ArrayList(Arrays.asList(menuPath));
    fullPathList.add(menuName);
    Menu subMenu = createMenusIfNecessary(wrapMenu(parent),
        (String[]) fullPathList.toArray(new String[] {}));
    return (JMenu) subMenu.getWrappee();
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
   *      String menuItemName, final boolean checkBox, final Icon icon, final
   *      EnableCheck enableCheck, final int pos)
   */
  public JMenuItem addMainMenuPlugin(final PlugIn executable,
      final String[] menuPath, String menuItemName, final boolean checkBox,
      final Icon icon, final EnableCheck enableCheck) {
    return addMainMenuPlugin(executable, menuPath, menuItemName, checkBox,
        icon, enableCheck, -1);
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
      final String[] menuPath, String menuItemName, boolean checkBox,
      final Icon icon, final EnableCheck enableCheck, final int pos) {

    // get icon from workbench properties
    WorkbenchProperties wbProps = workbenchContext.getWorkbench()
        .getProperties();
    String checkboxSetting = wbProps.getSetting(new String[] {
        WorkbenchProperties.KEY_PLUGIN, executable.getClass().getName(),
        WorkbenchProperties.KEY_MENUS, WorkbenchProperties.KEY_MAINMENU,
        WorkbenchProperties.ATTR_CHECKBOX });
    if (checkboxSetting.equals(WorkbenchProperties.ATTR_VALUE_TRUE))
      checkBox = true;

    JMenuItem menuItem = createMenuItem(menuItemName, checkBox);
    menuItem = addMainMenuPluginItemWithPostProcessing(executable, menuPath,
        menuItem, icon, enableCheck, pos);
    return menuItem;
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

    // check and replace with checkbox item if necessary
    if (executable instanceof CheckBoxed
        && !(menuItem instanceof JCheckBoxMenuItem))
      menuItem = createMenuItem(menuItem.getText(), true);

    // we silently ignore the pos argument as positions are defined in xml or
    // not at all
    addMenuPluginItem(wrapMenu(menuBar()), executable, menuPath, menuItem, icon,
        enableCheck);

    return menuItem;
  }

  /**
   * a generic addMenu method, fetching all settings from workbench properties
   * and the plugin object. used in PluginManager
   * 
   * @param menuKey
   * @param plugin
   * @return menuItem
   */
  public JMenuItem addMenuPlugin(String menuKey, PlugIn plugin) {
    Menu menu = fetchMenuForKey(menuKey);
    if (menu == null)
      throw new IllegalArgumentException("Menu key '" + menuKey
          + "' does not point to a known menu.");

    boolean checkBox = false;
    // override via interface detection
    if (plugin instanceof CheckBoxed)
      checkBox = true;
    // override via props setting
    String checkBoxSetting = fetchPluginMenuSetting(menu.getWrappee(), plugin,
        WorkbenchProperties.ATTR_CHECKBOX);
    if (checkBoxSetting.equals(WorkbenchProperties.ATTR_VALUE_TRUE))
      checkBox = true;

    JMenuItem menuItem = createMenuItem(plugin.getName(), checkBox);
    return addMenuPluginItem(menu, plugin, new String[] {}, menuItem, null, null);

  }

  /**
   * The addMenu method. it adds a given menu item to a menu path
   * associates the given plugin and generally does everything so
   * the pluging works as a menu entry.
   * additionally it consults the workbench properties for an order
   * to insert the menu item, icon to add, different name etc. and 
   * runs updateSeperators() after each insert, so the menus are 
   * neatly separated according to the workbench props definitions.
   * 
   * @param menu
   *          a wrapped menu in which to add the item and attach the plugin
   * @param plugin
   *          the plugin to execute with this item
   * @param menuPath
   *          the menu path made of the menu and submenu names
   * @param menuItem
   *          the JMenuItem (or JCheckBoxMenuItem or JRadioButtonMenuItem) to
   *          the parent menu
   * @param icon
   *          an icon to use
   * @param enableCheck
   *          conditions making the plugin enabled
   */
  private JMenuItem addMenuPluginItem(Menu menu, PlugIn plugin,
      String[] menuPath, JMenuItem menuItem, Icon icon, EnableCheck enableCheck) {

    // get and overwrite icon from workbench properties
    String iconSetting = fetchPluginMenuSetting(menu.getWrappee(), plugin,
        WorkbenchProperties.ATTR_ICON);
    if (!iconSetting.isEmpty())
      icon = IconLoader.icon(iconSetting);

    // make sure the icon is max 16x16
    if (icon instanceof ImageIcon && icon.getIconHeight() > 16) {
      icon = GUIUtil.resize((ImageIcon) icon, 16);
    }
    
    // add icon or get/add from Iconified interfaced plugin
    addMenuItemIcon(
        menuItem,
        icon == null && plugin instanceof Iconified ? ((Iconified) plugin)
            .getIcon(16) : icon);

    // shortcut
    assignShortcut(menuItem, plugin);

    // replace name if defined in workbenchproperties
    String nameSetting = fetchPluginMenuSetting(menu.getWrappee(), plugin,
        WorkbenchProperties.ATTR_NAME);
    if (!nameSetting.isEmpty())
      menuItem.setText(computeName(nameSetting));
    // set name if none so far
    else if (menuItem.getText().trim().length() == 0) {
      menuItem.setText(plugin.getName());
    }

//    if (plugin.getClass().getName().contains("Printer"))
//      System.out.println(plugin.getClass().getName());

    // regex pattern to strip position setting from strings
    Pattern posPattern = Pattern.compile("^(.*)\\{(?:pos\\:)?(\\d+)\\}$");
    
    // strip pospattern from menuitem name e.g. foo{pos:3}
    // we ignore them, positions are determined by position in *.xml config
    // anything not listed in there will be appended.
    Matcher m = posPattern.matcher(menuItem.getText().trim());
    if (m.matches()) {
      menuItem.setText(m.toMatchResult().group(1));
    }
    
    // update menupath{pos} from wbprops
    String pathSetting = fetchPluginMenuSetting(menu.getWrappee(), plugin,
        WorkbenchProperties.ATTR_MENUPATH);
    Object[] menuPathPositions = new String[menuPath.length];
    if (!pathSetting.isEmpty()) {
      menuPath = pathSetting.split("/");
      menuPathPositions = new String[menuPath.length];
      for (int i = 0; i < menuPath.length; i++) {
        String pathEntry = menuPath[i].trim();

        // find an remove positional argument e.g. foo{3}
        m = posPattern.matcher(pathEntry);
        if (m.matches()) {
          MatchResult mr = m.toMatchResult();
          menuPath[i] = pathEntry = mr.group(1);
          menuPathPositions[i] = mr.group(2);
        }
        // find and replace MenuNames var or I18N string
        menuPath[i] = computeName(pathEntry);
      }
    }

//    if (plugin.getClass().getName().contains("PasteSchema"))
//      System.out.println(fetchKeyForMenu(menu.getWrappee()));

//    System.out.println(plugin.getName()+"-> "+Arrays.toString(menuPath));
    List<String> posListFromProps = calculateNewPosition(menu,
        new ArrayList<String>(Arrays.asList(menuPath)), menu, plugin);

    // cut to menupath length
    if (posListFromProps.size()>0) {
      menuPathPositions = new String[menuPath.length+1];
      for (int i = 0; i < menuPathPositions.length && i < posListFromProps.size(); i++) {
        menuPathPositions[i] = posListFromProps.get(i);
      }
    }

    // associate plugin with the menuitem early, so addMenuItem()'s
    // updateSeparators() will find us in registry
    associate(menuItem, plugin);

    // add to menu
    Menu itemRoot = createMenusIfNecessary(menu, menuPath, menuPathPositions);
    
    // recalculate item position in item root
    List posList = calculateNewPosition(menu, new ArrayList(), itemRoot, plugin);
    int pos = posList.isEmpty() ? -1 : Integer.parseInt(posList.get(0).toString());
    
//    // we got a forced position?
//    if (menuPathPositions != null && menuPathPositions.length > menuPath.length
//        && menuPathPositions[menuPathPositions.length - 1] != null) {
//      pos = Integer.parseInt(menuPathPositions[menuPathPositions.length - 1]
//          .toString());
//    }

    installMnemonic(menuItem, (MenuElement) itemRoot.getWrappee());

    // always protect the last two FILE menu entries, separator + exit item's
    // position
    int count = itemRoot.getComponentCount();
    if (itemRoot.getWrappee() == FeatureInstaller.getInstance().menuBarMenu(
        MenuNames.FILE)
        && (pos < 0 || pos >= count - 2)) {
      pos = count - 2;
    }
    // protect titled popup header
    else if (pos < 2 && itemRoot instanceof TitledPopupMenu) {
      pos = +2;
    }

    // add or insert item
    if (pos < 0)
      itemRoot.add(menuItem);
    else
      itemRoot.insert(menuItem, pos);

    // update separators
    if (isSeparatingEnabled())
      updateSeparatorsFromProps(menu, menu);

    // fetch a check, if none already
    enableCheck = enableCheck == null && plugin instanceof EnableChecked ? ((EnableChecked) plugin)
        .getEnableCheck() : enableCheck;

    if (enableCheck != null) {
      // deal with popup menus
      if (menu.getWrappee() instanceof JPopupMenu) {
        final JMenuItem item = menuItem;
        final EnableCheck check = enableCheck;
        ((JPopupMenu) menu.getWrappee())
            .addPopupMenuListener(new PopupMenuListener() {
              public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                toMenuItemShownListener(check).menuItemShown(item);
              }

              public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
              }

              public void popupMenuCanceled(PopupMenuEvent e) {
              }
            });
      }
      // deal with plain menus
      else {
        addMenuItemShownListener(menuItem, toMenuItemShownListener(enableCheck));
      }
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
    int pos = -1;
    addMainMenuPlugin(executable, menuPath, menuItemName, checkBox, icon,
        enableCheck, pos);
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

  private MenuItemShownListener toMenuItemShownListener(
      final EnableCheck enableCheck) {
    return new EnableCheckMenuItemShownListener(workbenchContext, enableCheck);
  }

  public void addMenuItemShownListener(final JMenuItem menuItem,
      final MenuItemShownListener menuItemShownListener) {
    JMenu menu = (JMenu) ((JPopupMenu) menuItem.getParent()).getInvoker();
//    System.out.println("--> " + menu);
    // wraps MenuListener in the JumpMenuListener wrapper class so that
    // EasyPanel can determine which menu items had EnableChecks [Larry Becker]
    menu.addMenuListener(new JumpMenuListener(menuItemShownListener, menuItem));
  }

  /**
   * Ultimate convenience method for attaching a plugin to a popupmenu.
   * 
   * @param popupMenu
   * @param executable
   * @return menu item
   */
  public JMenuItem addPopupMenuPlugin(JPopupMenu popupMenu, PlugIn executable) {
    return addPopupMenuPlugin(popupMenu, executable, new String[] {}, null,
        false, null, null);
  }

  /**
   * Convenience method for entries with menupath
   * 
   * @return menu item
   */
  public JMenuItem addPopupMenuPlugin(JPopupMenu popupMenu, PlugIn executable,
      String[] menuPath) {
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

    checkBox = checkBox || executable instanceof CheckBoxed;
    JMenuItem menuItem = createMenuItem(menuItemName, checkBox);

    return addMenuPluginItem(wrapMenu(popupMenu), executable, menuPath,
        menuItem, icon, enableCheck);
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

//  private void stripAllSeparatorsRecursively(Menu menu) {
//    // if (menu.getWrappee() instanceof JMenu &&
//    // ((JMenu)menu.getWrappee()).getText().equals("File"))
//    // System.out.println("FILE MENU");;
//
//    // strip all separators
//    int start = 0;
//    int end_offset = 0;
//    // protect title of titled popup menu
//    if (menu.getWrappee() instanceof TitledPopupMenu)
//      start = 2;
//    else if (menu.getWrappee() == menuBarMenu(MenuNames.FILE))
//      end_offset = 2;
//    for (int i = start; i < menu.getComponentCount() - end_offset; i++) {
//      Component c = menu.getComponent(i);
//      // System.out.println(c);
//      if (c instanceof JMenu) {
//        stripAllSeparatorsRecursively(wrapMenu((JMenu) c));
//      }
//      else if (c instanceof Separator) {
//        menu.remove(i);
//        i--;
//      }
//    }
//  }

//  private void updateSeparatorsFromProps(Menu menuRoot, Menu menuWrap) {
//    List<String> list = getMenuListFromSettings(menuRoot.getWrappee());
//    // unconfigured menu
//    if (list.size() < 1)
//      return;
//
//    // stripAllSeparatorsRecursively(menuRoot);
//
//    // add per setting
//    int count = menuWrap.getComponentCount();
//    for (int i = 0; i < count; i++) {
//      Component c1 = menuWrap.getComponent(i);
//      System.out.println(fetchKeyForMenu(menuWrap.getWrappee()) + "/"
//          + c1.getClass().getName() + "/" + i + "/" + count + "/"
//          + menuWrap.getComponentCount());
//
//      int item1Pos = -1;
//      if (c1 instanceof JMenu) {
//        // get pos value
//        item1Pos = getHighestMenuItemPositionRecursive(list,
//            wrapMenu((JMenu) c1));
//        // update separators in submenu
//        updateSeparatorsFromProps(menuRoot, wrapMenu((JMenu) c1));
//      }
//      else if (c1 instanceof JMenuItem) {
//        PlugIn p1 = pluginFromMenuItem((JMenuItem) c1);
//        // non plugin component
//        if (p1 == null)
//          continue;
//        item1Pos = getPositionFromList(list, p1.getClass().getName());
//      }
//      else {
//        continue;
//      }
//
//      // unconfigured item
//      if (item1Pos < 0)
//        continue;
//
//      // search next positioned item
//      int item2Pos = -1;
//      int j = i + 1;
//      for (; j < menuWrap.getComponentCount(); j++) {
//        Component c2 = menuWrap.getComponent(j);
//        if (c2 instanceof JMenu)
//          item2Pos = getHighestMenuItemPositionRecursive(list,
//              wrapMenu((JMenu) c2));
//        else if (c2 instanceof JMenuItem) {
//          PlugIn p2 = pluginFromMenuItem((JMenuItem) c2);
//          // non plugin component
//          if (p2 == null)
//            continue;
//          item2Pos = getPositionFromList(list, p2.getClass().getName());
//        }
//        else if (c2 instanceof Separator) {
//          // ups! already separated here? continue with next item alltogether
//          break;
//        }
//
//        // j item is configured, found our 2nd pos
//        if (item2Pos >= 0)
//          break;
//      }
//
//      // find a separator between those two & insert it
//      for (int k = item1Pos + 1; k < item2Pos; k++) {
//        String key = list.get(k);
//        if (key.equals(WorkbenchProperties.KEY_SEPARATOR)) {
//          menuWrap.insertSeparator(i + 1);
//          count++;
//          break;
//        }
//      }
//
//    }
//  }
  
  // disabled by default, autoseparating is enabled via PluginMgr during startup
  private static boolean separatingEnabled = false;

  public boolean isSeparatingEnabled() {
    return separatingEnabled;
  }

  public void setSeparatingEnabled(boolean onoff) {
    separatingEnabled = onoff;
  }

  public void updateSeparatorsInAllMenus(){
    for (Iterator iterator = getAllMenus().iterator(); iterator.hasNext();) {
      Menu menu = (Menu) iterator.next();
      updateSeparatorsFromProps(menu, menu);
    }
  }

  private void updateSeparatorsFromProps(Menu menuRoot, Menu menuWrap) {

    WorkbenchProperties wbProps = workbenchContext.getWorkbench()
        .getProperties();

    // add per setting
    int count = menuWrap.getComponentCount();
    for (int i = 0; i < count; i++) {
      Component c1 = menuWrap.getComponent(i);
//      System.out.println(fetchKeyForMenu(menuWrap.getWrappee()) + "/"
//          + c1.getClass().getName() + "/" + i + "/" + count + "/"
//          + menuWrap.getComponentCount());

      int item1Pos = -1;
      if (c1 instanceof JMenu) {
        // get pos value
        item1Pos = getHighestMenuItemPositionRecursive(menuRoot,
            wrapMenu((JMenu) c1));
        // update separators in submenu
        updateSeparatorsFromProps(menuRoot, wrapMenu((JMenu) c1));
      }
      else if (c1 instanceof JMenuItem) {
        PlugIn p1 = pluginFromMenuItem((JMenuItem) c1);
        // non plugin component
        if (p1 == null)
          continue;
        String posSetting = fetchPluginMenuSetting(menuRoot.getWrappee(), p1, WorkbenchProperties.ATTR_ORDERID);
        item1Pos = posSetting.isEmpty() ? -1 : Integer.parseInt(posSetting);
      }
      else {
        continue;
      }

      // unconfigured item
      if (item1Pos < 0)
        continue;

      // search next positioned item
      int item2Pos = -1;
      int j = i + 1;
      for (; j < menuWrap.getComponentCount(); j++) {
        Component c2 = menuWrap.getComponent(j);
        if (c2 instanceof JMenu) {
          item2Pos = getLowestMenuItemPositionRecursive(menuRoot,
              wrapMenu((JMenu) c2));
        }
        else if (c2 instanceof JMenuItem) {
          PlugIn p2 = pluginFromMenuItem((JMenuItem) c2);
          // non plugin component
          if (p2 == null)
            continue;
          String posSetting = fetchPluginMenuSetting(menuRoot.getWrappee(), p2,
              WorkbenchProperties.ATTR_ORDERID);
          item2Pos = posSetting.isEmpty() ? -1 : Integer.parseInt(posSetting);
        }
        else if (c2 instanceof Separator) {
          // ups! already separated here? continue with next item alltogether
          break;
        }

        // j item is configured, found our 2nd pos
        if (item2Pos >= 0)
          break;
      }
      
      // unconfigured item
      if (item2Pos < 0)
        continue;

//      System.out.println("FI updSep2: "+item1Pos+"/"+item2Pos);
      
      // search for defined separator inbetween both configured items
      Map sepList = wbProps
          .getSettings(new String[]{WorkbenchProperties.KEY_SEPARATOR});
      for (Map attribs : (Collection<Map>) sepList.values()) {
        // fetch a wanted position via order id
        Object sepPosSetting = attribs.get(WorkbenchProperties.ATTR_ORDERID);
        int sepPos = sepPosSetting == null ? -1 : Integer
            .parseInt(sepPosSetting.toString());
//        System.out.println("FI updSep2 sep: "+sepPos);
        // plugin and next item are separated via at least one separator
        // protect JMenuBars not supporting separators
        if (sepPos > item1Pos && sepPos < item2Pos
            && !(menuWrap.getWrappee() instanceof JMenuBar)) {
          menuWrap.insertSeparator(i + 1);
          count++;
          break;
        }
      }

    }
  }
  
  private int getHighestMenuItemPositionRecursive(Menu root, Menu menu) {
    int pos = -1;
    for (int j = 0; j < menu.getComponentCount(); j++) {
      Component c = menu.getComponent(j);
      if (c instanceof JMenu) {
        int subPos = getHighestMenuItemPositionRecursive(root,
            wrapMenu((JMenu) c));
        pos = subPos > pos ? subPos : pos;
      }
      else if (c instanceof JMenuItem) {
        PlugIn p = pluginFromMenuItem((JMenuItem) c);
        if (p instanceof PlugIn) {
          String posSetting = fetchPluginMenuSetting(root.getWrappee(), p, WorkbenchProperties.ATTR_ORDERID);
          int subPos = posSetting.isEmpty() ? -1 : Integer.parseInt(posSetting);
          pos = subPos > pos ? subPos : pos;
        }
      }
    }

    return pos;
  }
  
  private int getLowestMenuItemPositionRecursive(Menu root, Menu menu) {
    int pos = -1;
    for (int j = 0; j < menu.getComponentCount(); j++) {
      Component c = menu.getComponent(j);
      int subPos = -1;
      if (c instanceof JMenu) {
        subPos = getLowestMenuItemPositionRecursive(root,
            wrapMenu((JMenu) c));
      }
      else if (c instanceof JMenuItem) {
        PlugIn p = pluginFromMenuItem((JMenuItem) c);
        if (p instanceof PlugIn) {
//          if ( ((PlugIn)p).getName().toLowerCase().contains("beanshelleditor") )
//            System.out.println();;
          String posSetting = fetchPluginMenuSetting(root.getWrappee(), p, WorkbenchProperties.ATTR_ORDERID);
          //System.out.println(((PlugIn)p).getName()+"='"+posSetting+"'"+posSetting.isEmpty());
          if (!posSetting.isEmpty())
            subPos = Integer.parseInt(posSetting);
        }
      }
      
      if ( subPos >= 0 && ( pos < 0 || subPos < pos ) )
          pos = subPos;
    }

    return pos;
  }

//  private List getPositionListFromProps(Menu menu, PlugIn executable) {
//    String menuKey = fetchKeyForMenu(menu.getWrappee());
//    // unconfigured menu ?
//    if (menuKey.isEmpty())
//      return new ArrayList();
//
//    WorkbenchProperties wbProps = workbenchContext.getWorkbench()
//        .getProperties();
//    List<String> list = getMenuListFromSettings(menu.getWrappee());
//
//    List posList = getPositionListRecursive(list, menu, executable);
//
//    return posList;
//  }
  
//  /**
//   * calculates a list of ints signalling where to insert a given plugin
//   */
//  private List<Object> getPositionListRecursive(List configPosList, Menu menu,
//      PlugIn p) {
//
//    int pluginPos = getPositionFromList(configPosList, p.getClass().getName());
//    // this plugin's pos is unconfigured
//    if (pluginPos < 0)
//      return new ArrayList();
//
////    if (p.getClass().getName().contains("MoveCat"))
////      System.out.println("check " + p.getName());
//
//    // look for components with bigger position values than us
//    // return the bigger position list to insert us in
//    List computedPosList = new ArrayList();
//    int lastPos = -1;
//    for (int i = 0; i < menu.getComponentCount(); i++) {
//      Component c = menu.getComponent(i);
//      if (c instanceof JMenu) {
//        List computedPosSubList = getPositionListRecursive(configPosList,
//            wrapMenu((JMenu) c), p);
//        if (computedPosSubList.size() > 0) {
//          computedPosList.add(i);
//          computedPosList.addAll(computedPosSubList);
//          break;
//        }
//      }
//
//      PlugIn p2 = pluginFromMenuItem(c);
//      // pluginless item e.g. separator
//      if (p2 == null)
//        continue;
//
//      int itemPos = getPositionFromList(configPosList, p2.getClass().getName());
//      // unconfigured item
//      if (itemPos < 0)
//        continue;
//
//      // actually add the item
//      if (itemPos > pluginPos) {
//        // search for defined separator inbetween both configured items
//        boolean isConfigSeparated = configPosList.subList(pluginPos, itemPos)
//            .contains(WorkbenchProperties.KEY_SEPARATOR);
//        // search for actual separators inbetween menu candidate positions
//        boolean foundSeparators = false;
//        for (int j = lastPos; lastPos >= 0 && j < i; j++) {
//          if (menu.getComponent(j) instanceof Separator) {
//            foundSeparators = true;
//            break;
//          }
//        }
//        // add either here or there
//        int pos = foundSeparators && isConfigSeparated ? lastPos + 1 : i;
//        computedPosList.add(pos);
//        break;
//      }
//      else {
//        // when did we last see a configured item, we might want to be inserted
//        // after
//        // it but before any separators, unconfgd. items between the two items
//        lastPos = i;
//      }
//    }
//
//    return computedPosList;
//  }

  /**
   * calculates the plugin's position and return a position list as follows
   * assuming the plugin would go under File->Print->PrinterPlugin it could
   * return [0,3,0] which would mean that File should be inserted at the 
   * beginning, Print at position 3 and the plugin at the beginning of their
   * respective parent menu.
   * an empty list means the plugin and it's sub paths should be appended to 
   * their respective parent menus.
   * 
   * @param parent - the absolute parent menu
   * @param menuPath - the relative menupath list in the menu to be placed in
   * @param menu - the menu to be placed in
   * @param p - the plugin
   * @return List<String> - list of positions or empty list
   */
  private List<String> calculateNewPosition(Menu parent, List menuPath, Menu menu,
      PlugIn p) {
    
    WorkbenchProperties wbProps = workbenchContext.getWorkbench()
        .getProperties();

    // fetch a wanted position via order id
    String posSetting = fetchPluginMenuSetting(parent.getWrappee(), p, WorkbenchProperties.ATTR_ORDERID);
    int pluginPos = posSetting.isEmpty() ? -1 : Integer.parseInt(posSetting);
    // this plugin's pos is unconfigured
    if (pluginPos < 0)
      return new ArrayList();

//    if (p.getClass().getName().contains("Printer"))
//      System.out.println("check " + p.getName());

    // look for components with bigger position values than us
    // return the bigger position list to insert us in
    List computedPosList = new ArrayList();
    int lastPos = -1;
    int itemPos = -1;
    for (int i = 0; i < menu.getComponentCount(); i++) {
      Component c = menu.getComponent(i);
      
      // check if this menu contains a later item
      if (c instanceof JMenu) {
        List menuSubPath = new ArrayList<String>(menuPath);
        if (!menuSubPath.isEmpty()){
          menuSubPath.remove(0);
        }
        List computedPosSubList = calculateNewPosition(parent, menuSubPath,
            wrapMenu((JMenu) c), p);
        if (computedPosSubList.size() > 0) {
          
          // search for actual separators between menu candidate positions
          boolean foundSeparators = false;
          for (int j = lastPos; lastPos >= 0 && j < i; j++) {
            if (menu.getComponent(j) instanceof Separator) {
              foundSeparators = true;
              break;
            }
          }
          
          computedPosList.add(Integer.toString(foundSeparators ? lastPos + 1
              : i));
          computedPosList.addAll(computedPosSubList);
          break;
        }
        
        // look no further, we are to inserted into _this_ menu, hence if we found no
        // pos placed behind us thus far it is because we're the last one for this menu
        if ( ! menuPath.isEmpty() && ((JMenu) c).getText().equals(menuPath.get(0)) )
          break;
      }
      else {
        // is this item a plugin?
        PlugIn p2 = pluginFromMenuItem(c);
        // pluginless item e.g. separator
        if (p2 == null)
          continue;

        // fetch a wanted position via order id
        posSetting = fetchPluginMenuSetting(parent.getWrappee(), p2,
            WorkbenchProperties.ATTR_ORDERID);
        itemPos = posSetting.isEmpty() ? -1 : Integer.parseInt(posSetting);
      }
      
      // unconfigured item
      if (itemPos < 0)
        continue;

      // actually add the item
      if (itemPos > pluginPos) {
        // search for defined separator inbetween both configured items
        boolean isConfigSeparated = false;
        Map sepList = wbProps
            .getSettings(new String[]{WorkbenchProperties.KEY_SEPARATOR});
        for (Map attribs : (Collection<Map>) sepList.values()) {
          // fetch a wanted position via order id
          Object sepPosSetting = attribs.get(WorkbenchProperties.ATTR_ORDERID);
          int sepPos = sepPosSetting == null ? -1 : Integer
              .parseInt(sepPosSetting.toString());
          // plugin and next item are separated via at least one separator
          if (sepPos > pluginPos && sepPos < itemPos) {
            isConfigSeparated = true;
            break;
          }
        }
        
        // search for actual separators between menu candidate positions
        boolean foundSeparators = false;
        for (int j = lastPos; lastPos >= 0 && j < i; j++) {
          if (menu.getComponent(j) instanceof Separator) {
            foundSeparators = true;
            break;
          }
        }
        // add either here or there
        int pos = foundSeparators && isConfigSeparated ? lastPos + 1 : i;
        computedPosList.add(0, Integer.toString(pos));
        break;
      }
      else {
        // when did we last see a configured item, we might want to be inserted
        // after it but before any separators or unconfigured items
        lastPos = i;
      }
    }
    
//    if (p.getName().contains("Printer"))
//      System.out.println(p.getName()+"-> "+computedPosList);

    return computedPosList;
  }
  
  public String fetchKeyForMenu(Object menu) {
    String menuKey = "";
    if (menu == menuBar())
      menuKey = WorkbenchProperties.KEY_MAINMENU;
    else if (menu == workbenchContext.getWorkbench().getFrame()
        .getCategoryPopupMenu())
      menuKey = WorkbenchProperties.KEY_CATEGORYPOPUP;
    else if (menu == workbenchContext.getWorkbench().getFrame()
        .getLayerNamePopupMenu())
      menuKey = WorkbenchProperties.KEY_LAYERNAMEPOPUP;
    else if (menu == LayerViewPanel.popupMenu())
      menuKey = WorkbenchProperties.KEY_LAYERVIEWPOPUP;
    else if (menu == workbenchContext.getWorkbench().getFrame()
        .getWMSLayerNamePopupMenu())
      menuKey = WorkbenchProperties.KEY_LAYERNAMEPOPUP_WMS;
    else if (menu == AttributeTab.popupMenu(workbenchContext))
      menuKey = WorkbenchProperties.KEY_ATTRIBUTETABPOPUP;
    else if (menu == RasterImageContextMenu.getInstance(workbenchContext
        .createPlugInContext()))
      menuKey = WorkbenchProperties.KEY_LAYERNAMEPOPUP_RASTER;
    else if (menu instanceof JMenu)
      menuKey = ((JMenu) menu).getText();

    return menuKey;
  }

  public Menu fetchMenuForKey(String key) {
    Menu menu = null;
    if (key.equals(WorkbenchProperties.KEY_MAINMENU))
      menu = wrapMenu(menuBar());
    else if (key.equals(WorkbenchProperties.KEY_CATEGORYPOPUP))
      menu = wrapMenu(workbenchContext.getWorkbench().getFrame()
          .getCategoryPopupMenu());
    else if (key.equals(WorkbenchProperties.KEY_LAYERNAMEPOPUP))
      menu = wrapMenu(workbenchContext.getWorkbench().getFrame()
          .getLayerNamePopupMenu());
    else if (key.equals(WorkbenchProperties.KEY_LAYERVIEWPOPUP))
      menu = wrapMenu(LayerViewPanel.popupMenu());
    else if (key.equals(WorkbenchProperties.KEY_LAYERNAMEPOPUP_WMS))
      menu = wrapMenu(workbenchContext.getWorkbench().getFrame()
          .getWMSLayerNamePopupMenu());
    else if (key.equals(WorkbenchProperties.KEY_ATTRIBUTETABPOPUP))
      menu = wrapMenu(AttributeTab.popupMenu(workbenchContext));
    else if (key.equals(WorkbenchProperties.KEY_LAYERNAMEPOPUP_RASTER))
      menu = wrapMenu(RasterImageContextMenu.getInstance(workbenchContext
          .createPlugInContext()));

    return menu;
  }
  
  private List<Menu> getAllMenus(){
    List<Menu> menus = new Vector();
    String[] keys = new String[] { WorkbenchProperties.KEY_MAINMENU,
        WorkbenchProperties.KEY_CATEGORYPOPUP,
        WorkbenchProperties.KEY_LAYERNAMEPOPUP,
        WorkbenchProperties.KEY_LAYERVIEWPOPUP,
        WorkbenchProperties.KEY_LAYERNAMEPOPUP_WMS,
        WorkbenchProperties.KEY_ATTRIBUTETABPOPUP,
        WorkbenchProperties.KEY_LAYERNAMEPOPUP_RASTER };
    for (int i = 0; i < keys.length; i++) {
      menus.add(fetchMenuForKey(keys[i]));
    }
    return menus;
  }

  private String fetchPluginMenuSetting(Object menu, PlugIn plugin,
      String attribute_key) {
    String menu_key = fetchKeyForMenu(menu);
    if (menu_key.isEmpty())
      return "";

    WorkbenchProperties wbProps = workbenchContext.getWorkbench()
        .getProperties();
    // get setting if defined in workbenchproperties
    String setting = wbProps.getSetting(new String[] {
        WorkbenchProperties.KEY_PLUGIN, plugin.getClass().getName(),
        WorkbenchProperties.KEY_MENUS, menu_key, attribute_key });
    
    // retry with plugin orderid if menu is unconfigured
    if (setting.isEmpty() && attribute_key.equals(WorkbenchProperties.ATTR_ORDERID)){
      setting = wbProps.getSetting(new String[] {
          WorkbenchProperties.KEY_PLUGIN, plugin.getClass().getName(), attribute_key });
    }

    return setting;
  }

  private int getPositionFromList(List<String> list, String key) {
    int i = 0;
    for (String string : list) {
      if (string.equals(key))
        return i;

      i++;
    }
    return -1;
  }
  
  private List getMenuListFromSettings(Object menu) {
    String menuKey = fetchKeyForMenu(menu);
    if (menuKey.isEmpty())
      return new ArrayList();

    WorkbenchProperties wbProps = workbenchContext.getWorkbench()
        .getProperties();
    return wbProps.getSettingsList(new String[] {
        WorkbenchProperties.KEY_LAYOUT, menuKey,
        WorkbenchProperties.ATTR_TYPE_VALUE_LIST });
  }

  public void addPopupMenuSeparator(JPopupMenu popupMenu, String[] menuPath) {
    if (menuPath == null || menuPath.length == 0) {
      popupMenu.addSeparator();
    }
    else {
      JMenu menu = popupMenu(popupMenu, menuPath[0]);
      if (menu == null) {
        menu = (JMenu) popupMenu.add(new JMenu(menuPath[0]));
      }
      Menu parent = createMenusIfNecessary(wrapMenu(menu), behead(menuPath));
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

  public static Menu wrapMenu(final JPopupMenu popupMenu) {
    return new Menu() {

      public void insert(JMenuItem menuItem, int i) {
        popupMenu.insert(menuItem, i);
      }

      public String getText() {
        return "";
      }

      public int getComponentCount() {
        return popupMenu.getComponentCount();
      }

      public void add(JMenuItem menuItem) {
        popupMenu.add(menuItem);
      }

      public Component getComponent(int i) {
        return popupMenu.getComponent(i);
      }

      public JComponent getWrappee() {
        return popupMenu;
      }

      public void addSeparator() {
        popupMenu.addSeparator();
      }

      public void insertSeparator(int i) {
        popupMenu.insert(new JPopupMenu.Separator(), i);
      }

      public void remove(int i) {
        popupMenu.remove(i);
      }
    };
  }

  public static Menu wrapMenu(final JMenu menu) {
    return new Menu() {

      public void insert(JMenuItem menuItem, int i) {
        menu.insert(menuItem, i);
      }

      public String getText() {
        return menu.getText();
      }

      public int getComponentCount() {
        return menu.getMenuComponentCount();
      }

      public void add(JMenuItem menuItem) {
        menu.add(menuItem);
      }

      public Component getComponent(int i) {
        return menu.getMenuComponent(i);
      }

      public JComponent getWrappee() {
        return menu;
      }

      public void addSeparator() {
        menu.addSeparator();
      }

      public void insertSeparator(int i) {
        menu.insertSeparator(i);
      }

      public void remove(int i) {
        menu.remove(i);
      }
    };
  }

  public static Menu wrapMenu(final JMenuBar menu) {
    return new Menu() {

      public String getText() {
        throw new UnsupportedOperationException();
      }

      public int getComponentCount() {
        return menu.getMenuCount();
      }

      public Component getComponent(int i) {
        return menu.getMenu(i);
      }

      public void add(JMenuItem menuItem) {
        menu.add((JMenu) menuItem);
      }

      public void insert(JMenuItem menuItem, int i) {
        menu.add(menuItem, i);
      }

      public JComponent getWrappee() {
        return menu;
      }

      public void addSeparator() {
        throw new UnsupportedOperationException();
      }

      public void insertSeparator(int i) {
        throw new UnsupportedOperationException();
      }

      public void remove(int i) {
        menu.remove(i);
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
    // // the main menu entry might not be created yet
    // JMenu menu = (JMenu) installMnemonic(new JMenu(childName), menuBar());
    // addToMenuBar(menu);
    return null;
  }

  // private void addToMenuBar(JMenu menu) {
  // menuBar().add(menu);
  // // Ensure Window and Help are placed at the end. Remove #windowMenu and
  // // #helpMenu
  // // *after* adding #menu, because #menu might be the Window or Help menu!
  // // [Jon Aquino]
  // JMenu windowMenu = menuBarMenu(MenuNames.WINDOW);
  // JMenu helpMenu = menuBarMenu(MenuNames.HELP);
  // // Customized workbenches may not have Window or Help menus [Jon Aquino]
  // if (windowMenu != null) {
  // menuBar().remove(windowMenu);
  // }
  // if (helpMenu != null) {
  // menuBar().remove(helpMenu);
  // }
  // // and (re)add them again at the end
  // if (windowMenu != null) {
  // menuBar().add(windowMenu);
  // }
  // if (helpMenu != null) {
  // menuBar().add(helpMenu);
  // }
  // }

  public static Menu createMenusIfNecessary(Menu parent, String[] menuPath) {
    return createMenusIfNecessary(parent, menuPath, new String[menuPath.length]);
  }

  /**
   * @return the leaf
   */
  public static Menu createMenusIfNecessary(Menu parent, String[] menuPath,
      Object[] menuPathPositions) {
    if (menuPath == null || menuPath.length == 0) {
      return parent;
    }

    if (menuPath[0].equals(MenuNames.ZOOM)
        && FeatureInstaller.getInstance().fetchKeyForMenu(parent.getWrappee())
            .equals(WorkbenchProperties.KEY_LAYERVIEWPOPUP))
      System.out.println();

    JMenu child = (JMenu) childMenuItem(menuPath[0], parent);
    if (child == null) {
      child = (JMenu) installMnemonic(new JMenu(menuPath[0]),
          (MenuElement) parent.getWrappee());
      // retrieve position information
      int pos = -1;
      if (menuPathPositions != null && menuPathPositions.length > 0)
        try {
          pos = Integer.parseInt(menuPathPositions[0].toString());
        }
        catch (NumberFormatException e) {
        }
        catch (NullPointerException e) {
        }
      // protect the last two FILE menu entries, separator + exit item's
      // position
      int count = parent.getComponentCount();

//      String name2 = FeatureInstaller.getInstance().fetchKeyForMenu(
//          parent.getWrappee());
//      System.out.println("FI cMin parent count: " + name2 + "=" + count);

      if (parent.getWrappee() == FeatureInstaller.getInstance().menuBarMenu(
          MenuNames.FILE)
          && (pos < 0 || pos >= count - 2)) {
        pos = count - 2;
      }
      // protect windows/help entries positions at the end of the main menu
      else if (parent.getWrappee() == FeatureInstaller.getInstance().menuBar()
          && !child.getText().equals(MenuNames.HELP)
          && !child.getText().equals(MenuNames.WINDOW)) {
        Menu menu = wrapMenu(FeatureInstaller.getInstance().menuBar());
        for (int i = 0; i < menu.getComponentCount(); i++) {
          Component c = menu.getComponent(i);
          if (c instanceof JMenu) {
            String name = ((JMenu) c).getText();
            if ((pos > i || pos < 0)
                && (name.equals(MenuNames.WINDOW) || name
                    .equals(MenuNames.HELP))) {
              pos = i;
              break;
            }
          }
        }
      }

      // add entry
      if (pos < 0)
        parent.add(child);
      else
        parent.insert(child, pos);

    }
    return createMenusIfNecessary(wrapMenu(child), behead(menuPath),
        behead(menuPathPositions));
  }

  /**
   * Find the first occurrence of a menu item with the given name and return it.
   * 
   * @param childName
   * @param menu
   * @return JMenuItem
   */
  public static JMenuItem childMenuItem(String childName, Menu menu) {
    if (menu.getWrappee() instanceof JMenu) {
      Menu popup = wrapMenu(((JMenu) menu.getWrappee()).getPopupMenu());
      return childMenuItem(childName, popup);
    }
    // MenuElement[] childMenuItems = menu.getSubElements();
    // for (int i = 0; i < childMenuItems.length; i++) {
    // if (childMenuItems[i] instanceof JMenuItem
    // && ((JMenuItem) childMenuItems[i]).getText().equals(childName)) {
    // return ((JMenuItem) childMenuItems[i]);
    // }
    // }

    for (int i = 0; i < menu.getComponentCount(); i++) {
      Component c = menu.getComponent(i);
      if (c instanceof JMenuItem && ((JMenuItem) c).getText().equals(childName))
        return ((JMenuItem) c);
    }

    return null;
  }

  private static String[] strip(String[] a1) {
    String[] a2 = new String[a1.length - 1];
    System.arraycopy(a1, 0, a2, 0, a2.length);
    return a2;
  }
  
  private static String[] behead(String[] a1) {
    String[] a2 = new String[a1.length - 1];
    System.arraycopy(a1, 1, a2, 0, a2.length);
    return a2;
  }

  private static Object[] behead(Object[] a1) {
    Object[] a2 = new Object[a1.length - 1];
    System.arraycopy(a1, 1, a2, 0, a2.length);
    return a2;
  }

  private String computeName(String name) {
    // find and replace MenuNames var or I18N string
    if (name.startsWith("MenuNames.")) {
      try {
        Field field = MenuNames.class.getDeclaredField(name.replaceFirst(
            "MenuNames\\.", ""));
        name = field.get(MenuNames.class.newInstance()).toString();
      }
      catch (Exception e) {
      }
    }
    else if (name.startsWith("I18N.")) {
      name = I18N.get(name.replaceFirst("I18N\\.", ""));
    }
    return name;
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
      // register with workbench (usually done in JumpConfiguration,
      // Pluginmanager)
      // for cases where plugins initialize themselves and "forgot" to do it
      if (JUMPWorkbench.getInstance().getFrame().getKeyboardShortcutPlugin(st) == null)
        AbstractPlugIn.registerShortcuts(executable);
    }
  }

  private JMenuItem createMenuItem(String menuItemName, boolean checkBox) {
    return checkBox ? new JCheckBoxMenuItem(menuItemName) : new JMenuItem(
        menuItemName);
  }

  private void associate(JMenuItem menuItem, PlugIn plugIn) {
    // attach actionlistener
    menuItem.addActionListener(AbstractPlugIn.toActionListener(plugIn,
        workbenchContext, taskMonitorManager));
    // remember the association via registry
    menuItemRegistry.put(menuItem, plugIn);
  }

  private PlugIn pluginFromMenuItem(Component menuItem) {
    return menuItemRegistry.get(menuItem);
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
      && UIManager.get("CheckBoxMenuItem.checkIconFactory") != null;

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

  public static JMenuItem installMnemonic(JMenuItem menuItem, MenuElement parent) {
    String text = menuItem.getText();
    StringUtil.replaceAll(text, "&&", "##");
    int ampersandPosition = text.indexOf('&');
    if (-1 < ampersandPosition && ampersandPosition + 1 < text.length()) {
      menuItem.setMnemonic(text.charAt(ampersandPosition + 1));
      text = StringUtil.replace(text, "&", "", false);
    }
    else {
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
    }
    else if (element instanceof JMenu) {
      for (int i = 0; i < ((JMenu) element).getItemCount(); i++) {
        CollectionUtil.addIfNotNull(((JMenu) element).getItem(i), menuItems);
      }
    }
    else if (element instanceof JPopupMenu) {
      MenuElement[] children = ((JPopupMenu) element).getSubElements();
      for (int i = 0; i < children.length; i++) {
        if (children[i] instanceof JMenuItem) {
          menuItems.add(children[i]);
        }
      }
    }
    else {
      Assert.shouldNeverReachHere(element.getClass().getName());
    }
    return menuItems;
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
