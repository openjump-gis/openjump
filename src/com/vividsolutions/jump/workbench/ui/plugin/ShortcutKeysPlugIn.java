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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;

import org.apache.log4j.Logger;
import org.openjump.core.ui.plugin.edittoolbox.cursortools.ConstrainedMultiClickTool;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AttributeTab;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.DelegatingTool;
import com.vividsolutions.jump.workbench.ui.cursortool.LeftClickFilter;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool.ModifierKeySpec;
import com.vividsolutions.jump.workbench.ui.cursortool.ShortcutsDescriptor;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import de.soldin.awt.VerticalFlowLayout;

public class ShortcutKeysPlugIn extends AbstractPlugIn {
  public static final ImageIcon ICON = IconLoader.icon("keyboard.png");
  public static final String NAME = I18N.get(ShortcutKeysPlugIn.class.getName());
  private static Logger LOG = Logger.getLogger(ShortcutKeysPlugIn.class);

  public boolean execute(PlugInContext context) throws Exception {
    ShortcutKeysFrame dlg = ShortcutKeysFrame.instance();
    dlg.setVisible(true);
    return true;
  }
  
  public Icon getIcon() {
    return ICON;
  }

  public static String getClassName(){
    return ShortcutKeysPlugIn.class.getName();
  }
}

class ShortcutKeysFrame extends JFrame {
  private static ShortcutKeysFrame instance;
  JLabel shortsLabel = new JLabel();
  JPanel shortsPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();

  public static ShortcutKeysFrame instance() {
      // create everytime as tools are added during runtime to editingtoolbox
      //if (instance == null) {
          instance = new ShortcutKeysFrame();
      //}
      
      return instance;
  }

  private ShortcutKeysFrame() {
      super (/*JUMPWorkbench.getInstance().getFrame(), */ShortcutKeysPlugIn.NAME/*, true*/);
      // set a frame icon
      try {
          setIconImage(ShortcutKeysPlugIn.ICON.getImage());
      } catch (NoSuchMethodError e) {
          // IGNORE: this is 1.5 missing setIconImage()
      }

      try {
          jbInit();
          //pack();
          //GUIUtil.centreOnWindow(this);
      } catch (Exception ex) {
          ex.printStackTrace();
      }
  }

  void jbInit() throws Exception {
      //setLayout(new BoxLayout());
    
      VerticalFlowLayout l = new VerticalFlowLayout(VerticalFlowLayout.TOP);
      l.setHgap(20);
      l.setVgap(0);
      l.setDefaultRatio(1);
      shortsPanel.setLayout(l);
      
//      for (int i = 0; i <= 100; i++) {
//        String text = ((i % 5) == 0) ? "<html>" + i + "<br><br>some more</html>"
//            : i + "";
//        shortsPanel.add(new JLabel(text));
//      }
      //String content = buildOverview(); //new Scanner(new File("bin/com/vividsolutions/jump/workbench/ui/plugin/KeyboardPlugIn.html")).useDelimiter("\\Z").next();
      //shortsLabel.setText(content);
      //shortsPanel.add(shortsLabel);
      // add global shortcuts separated into menu categories
      for (String html : buildOverviews()) {
        for (JLabel lbl : buildTableLabels(html, 3)) {
          shortsPanel.add(lbl);
        }
        // space different overviews
        shortsPanel.add(Box.createRigidArea(new Dimension(10, 10)));
      }

      for (String html : buildQuasiModeOverviews()) {
        shortsPanel.add(buildTableLabel(html));
        // space different tables
        shortsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      }
      
      for (String html : buildEditingOverviews()) {
        shortsPanel.add(buildTableLabel(html, 300));
      }
      
      // add a nice padding border
      shortsPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
      
      JScrollPane scroll = new JScrollPane(shortsPanel);
      //scroll.setBorder(BorderFactory.createEmptyBorder());

//      scroll.setLayout(new ScrollPaneLayout() {
//        public void layoutContainer(Container parent) {
//          super.layoutContainer(parent);
//          Component view = viewport.getView();
//          if (view != null) {
//            Dimension viewPortSize = viewport.getSize();
//            Dimension viewSize = view.getSize();
//  
//            if ((viewPortSize.width > viewSize.width)
//                || (viewPortSize.height > viewSize.height)) {
////  System.out.println("bigger");
//              int spaceX = (viewPortSize.width - viewSize.width) / 2;
//              int spaceY = (viewPortSize.height - viewSize.height) / 2;
//              System.out.println(viewPortSize+"/"+viewSize);
//              if (spaceX < 0)
//                spaceX = 0;
//              if (spaceY < 0)
//                spaceY = 0;
//  
//              viewport.setLocation(spaceX, spaceY);
//              viewport.setSize(viewPortSize.width - spaceX, viewPortSize.height
//                  - spaceY);
//            }
//          }
//        }
//      });
      //scroll.add();
      //scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      //scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      // add 
      add(scroll, BorderLayout.CENTER);

      /* OK Button */
      okButton.setText(I18N.get("ui.OKCancelPanel.ok"));
      okButton.addActionListener(new java.awt.event.ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
          }
      });
      add(buttonPanel, BorderLayout.SOUTH);
      buttonPanel.add(okButton, null);
      
//      final JDialog dlg = this;
//      addComponentListener(new ComponentListener() {
//        public void componentShown(ComponentEvent e) {
//        }
//        public void componentResized(ComponentEvent e) {
//          dlg.getContentPane().invalidate();
//        }
//        public void componentMoved(ComponentEvent e) {
//        }
//        public void componentHidden(ComponentEvent e) {
//        }
//      });
  }

  public void setVisible(boolean b) {
    if (b){
      pack();
      GUIUtil.centre(instance, JUMPWorkbench.getInstance().getFrame());
    }

    super.setVisible(b);
  }
  
  private static MenuElement[] getAllMenuElements( MenuElement menu_in ){
    Vector<MenuElement> elements = new Vector();
    for (MenuElement menu_element : menu_in.getSubElements()) {

      if (menu_element.getSubElements().length > 0)
        elements.addAll(Arrays.asList(getAllMenuElements(menu_element)));
      else
        elements.add(menu_element);
    }
    return (MenuElement[]) elements.toArray(new MenuElement[0]);
  }
  
  private List<String> buildOverviews(){
    JUMPWorkbench wb = JUMPWorkbench.getInstance();
    FeatureInstaller finst = FeatureInstaller.getInstance();
    JMenuBar mainMenu = finst.menuBar();
    // fetch all menus
    JPopupMenu layerview_popup = LayerViewPanel.popupMenu();
    JPopupMenu layername_popup = wb.getFrame().getLayerNamePopupMenu();
    JPopupMenu layernamecategory_popup = wb.getFrame().getCategoryPopupMenu();
    JPopupMenu attribute_popup = AttributeTab.popupMenu(JUMPWorkbench
        .getInstance().getContext());
    // fetch the toolbars
    WorkbenchToolBar toolbar = wb.getFrame().getToolBar();
    WorkbenchToolBar edittoolbar = ((EditingPlugIn) JUMPWorkbench.getInstance()
        .getContext().getBlackboard().get(EditingPlugIn.KEY)).getToolbox(
        wb.getContext()).getToolBar();

    // a hashmap to store them all
    HashMap<String, HashMap<KeyStroke, PlugIn>> categories = new LinkedHashMap<String, HashMap<KeyStroke, PlugIn>>();
    
    // iterate over (main/popup)menus
    for (MenuElement main_menu_entry : mainMenu.getSubElements()) {
      if (!(main_menu_entry instanceof JMenuItem)) {
        continue;
      }
      JMenuItem main_menu_item = ((JMenuItem)main_menu_entry);
      Vector<MenuElement> menu_elements = new Vector();
      // add this menu's elements
      menu_elements.addAll(Arrays.asList(getAllMenuElements(main_menu_entry)));

      // add elements of appropriate popup menus
      if (main_menu_item.getText() == MenuNames.EDIT) {
        menu_elements.addAll(Arrays.asList(getAllMenuElements(layerview_popup)));
        menu_elements.addAll(Arrays.asList(getAllMenuElements(attribute_popup)));
      }
      else if (main_menu_item.getText() == MenuNames.LAYER) {
        menu_elements.addAll(Arrays.asList(getAllMenuElements(layername_popup)));
        menu_elements.addAll(Arrays.asList(getAllMenuElements(layernamecategory_popup)));
      }

      // loop through all main menu categories (file, edit,...)
      HashMap shortcut_plugins = new LinkedHashMap();
      // assuming main menu categories are unique
      categories.put(main_menu_item.getText(), shortcut_plugins);
      for (MenuElement menu_entry : menu_elements) {
        // look for items with accelerator
        if (menu_entry instanceof JMenuItem) {
          JMenuItem menu_item = (JMenuItem)menu_entry;
          // get the accelerator shortcut
          KeyStroke stroke = ((JMenuItem)menu_entry).getAccelerator();
          if (stroke!=null){
            // it's difficult to fetch the plugin from a menu item, but easy to ask
            // the workbench which plugin registered the matching accelerator
            PlugIn plugin = wb.getFrame().getKeyboardShortcutPlugin(stroke);
            if (plugin!=null){
              // we don't show double entries, first entry is from a main menu and wins
              if (!strokeInMap(stroke, categories))
                shortcut_plugins.put(getKeyStrokeText(stroke), plugin);
            }else{
              System.err.println(getClass().getSimpleName()+" menuitem '"+menu_item.getText()+"' has a shortcut which is not registered with the workbench.");
            }
          }
        }
      }
    }
    // add leftover (non)menu shortcuts
    HashMap shortcut_plugins = new LinkedHashMap();
    // assuming main menu categories are unique
    categories.put(I18N.get(ShortcutKeysPlugIn.getClassName()+".more"), shortcut_plugins);
    for (KeyStroke key : wb.getFrame().getKeyboardShortcuts()) {
      if (!strokeInMap(key, categories))
        shortcut_plugins.put(getKeyStrokeText(key), wb.getFrame().getKeyboardShortcutPlugin(key));
    }
    // add global cursortools shortcuts (defined in quasimode tool)
//    shortcut_plugins = new LinkedHashMap();
//    categories.put(I18N.get(this.getClass().getName()+".cursortools-quasimodes"), shortcut_plugins);
//    for (QuasimodeTool.ModifierKeySpec key : QuasimodeTool.getDefaultKeyboardShortcuts()) {
//      // create a pseudo plugin that spits out the name only
//      final CursorTool ct = QuasimodeTool.getDefaultKeyboardShortcutTool(key);
//      // no empties or double entries ( like shift modificators )
//      if (ct==null || shortcut_plugins.values().contains(ct)){
//        //System.out.println(key);
//        continue;
//      }
//      shortcut_plugins.put(key.toString(), ct);
//    }

    // create html outputstring
    ArrayList overviews = new ArrayList();
    for (String name : categories.keySet()) {
      String out = "";
      HashMap<String, Object> entries = (HashMap) categories.get(name);
      for (Map.Entry<String, Object> entry : entries.entrySet()) {
        // compute description
        String description;
        if (entry.getValue() instanceof PlugIn)
          description = ((PlugIn)entry.getValue()).getName();
        else if (entry.getValue() instanceof CursorTool)
          description = ((CursorTool)entry.getValue()).getName();
        else
          description = entry.getValue().toString();
        
        out += "<tr><td>" + GUIUtil.escapeHTML(description) + "</td><td width=100>"
            + GUIUtil.escapeHTML(entry.getKey()) + "</td></tr>\n";
      }
      if (out.length() > 0){
//        overview += "<tr><td colspan=2><b><center>"
//            + (overview.length()<1 ? "":"&nbsp;<br>") + 
//            "<u>" + name + "</u></center></b></td></tr>\n" + out;
        overviews.add("<tr><td colspan=2><b><center><u>" + GUIUtil.escapeHTML(name)
            + "</u></center></b></td></tr>\n" + out);
      }
    }

    return overviews;
  }

  private List<String> buildQuasiModeOverviews(){
    HashMap<String, HashMap> categories = new HashMap();
    // add global cursortools shortcuts (defined in quasimode tool)
    LinkedHashMap shortcut_plugins = new LinkedHashMap();
    categories.put(I18N.get(ShortcutKeysPlugIn.getClassName()+".cursortools-quasimodes"), shortcut_plugins);
    for (QuasimodeTool.ModifierKeySpec key : QuasimodeTool.getDefaultKeyboardShortcuts()) {
      // create a pseudo plugin that spits out the name only
      final CursorTool ct = QuasimodeTool.getDefaultKeyboardShortcutTool(key);
      // no empties or double entries ( like shift modificators )
      if (ct==null || shortcut_plugins.values().contains(ct)){
        //System.out.println(key);
        continue;
      }
      shortcut_plugins.put(key.toString(), ct);
    }

    // create html outputstring
    ArrayList<String> overviews = new ArrayList();
    for (String name : categories.keySet()) {
      String out = "";
      HashMap<String, Object> entries = (HashMap) categories.get(name);
      for (Map.Entry<String, Object> entry : entries.entrySet()) {
        // compute description
        String description;
        if (entry.getValue() instanceof PlugIn)
          description = ((PlugIn)entry.getValue()).getName();
        else if (entry.getValue() instanceof CursorTool)
          description = ((CursorTool)entry.getValue()).getName();
        else
          description = entry.getValue().toString();
        
        out += "<tr><td>" + GUIUtil.escapeHTML(description) + "</td><td>"
            + GUIUtil.escapeHTML(entry.getKey()) + "</td></tr>\n";
      }
      overviews.add("<tr><td colspan=2><b><center><u>" + GUIUtil.escapeHTML(name)
          + "</u></center></b></td></tr>\n" + out);
    }

    return overviews;
  }

  @SuppressWarnings("unchecked")
  private List<String> buildEditingOverviews(){
    // add doc for each cursortools option in editing toolbar
    LinkedHashMap<Object, LinkedHashMap> editing_options = new LinkedHashMap();
    String tools_out = "";
    List tools = EditingPlugIn.getInstance().getToolbox()
        .getPluginsTools();
    // add an all tools describing e.g. snapping options
    tools.add(0, new ShortcutsDescriptor() {
      public String getName() {
        return I18N.get(ShortcutKeysPlugIn.getClassName() + ".all-editing-tools");
      }
      
      public Map<ModifierKeySpec, String> describeShortcuts() {
        Map map = new HashMap();
        map.put(new ModifierKeySpec(new int[] { KeyEvent.VK_SPACE }),
            I18N.get(ShortcutKeysPlugIn.getClassName() + ".temporarily-switch-off-snapping"));
        return map;
      }
    });
    // find multiclicktools and add their common shortcuts 
    tools.add(1, new ShortcutsDescriptor() {
      public String getName() {
        List tools = EditingPlugIn.getInstance().getToolbox()
            .getPluginsTools();
        String out = "";
        for (Object tool : tools) {
          tool = unWrapTool(tool);
          if ( tool instanceof ConstrainedMultiClickTool
              || (tool instanceof MultiClickTool && 
                  !(tool instanceof NClickTool && 
                  ((NClickTool) tool).numClicks() < 2)) )
          {
            out += (out.length() > 0 ? ", " : "")
                + ((CursorTool) tool).getName();
          }
        }
        return out;
      }
      
      public Map<ModifierKeySpec, String> describeShortcuts() {
        Map map = new HashMap();
        map.put(new ModifierKeySpec(new int[] { KeyEvent.VK_BACK_SPACE }),
            I18N.get(MultiClickTool.class.getName() + ".erase-last-segment-or-point"));
        map.put(new ModifierKeySpec(new int[] { KeyEvent.VK_ESCAPE }),
            I18N.get(MultiClickTool.class.getName() + ".cancel-drawing"));
        map.put(new ModifierKeySpec(new int[] { KeyEvent.VK_ENTER }),
            I18N.get(MultiClickTool.class.getName() + ".finish-drawing"));
        return map;
      }
    });
    int i = 0;
    ArrayList<String> entries = new ArrayList();
    for (Object tool : tools) {
      String tool_out = "";
      // unwrap tool
      tool = unWrapTool(tool);
      // finally let's hit it
      i++;
      if (tool instanceof ShortcutsDescriptor) {
        ShortcutsDescriptor shorty = ((ShortcutsDescriptor) tool);
        Map description = shorty.describeShortcuts();
        if (description==null)
          continue;
        tool_out += "<tr><td colspan=3><b>"
            + (tool_out.length()<1 ? "":"&nbsp;<br>") + 
            "" + GUIUtil.escapeHTML(shorty.getName()) + "</b></td></tr>\n";
        Map<QuasimodeTool.ModifierKeySpec, String> options = description;
        for (QuasimodeTool.ModifierKeySpec sc : options.keySet()) {
          tool_out += "<tr><td>&nbsp;</td><td>"
              + GUIUtil.escapeHTML(options.get(sc)) + "</td><td>"
              + GUIUtil.escapeHTML(sc.toString()) + "</td></tr>\n";
        }
      }
      // title glued to first entry so they will not get separated by layout
      if (i==1)
        tool_out = "<tr><td colspan=2><b><center><u>"
            + GUIUtil.escapeHTML(I18N.get(ShortcutKeysPlugIn.getClassName()
                + ".editing-tools-options")) + "</u></center></b></td></tr>\n"
            + tool_out;
      if (i<=2){
        entries.add(tool_out);
      }else
        tools_out += tool_out;
    }
//    tools_out = //tools_out.length() <= 0 ? "" : "<html><body><table>\n" +
//        "<tr><td colspan=2><b><center><u>"
//        + I18N.get(this.getClass().getName() + ".editing-tools-options")
//        + "</u></center></b></td></tr>\n" + tools_out;
//        //+ "\n</table></body></html>";

    entries.add(tools_out);
    return entries;
  }

  private JLabel buildTableLabel( String in ){
    return buildTableLabel(in, 300);
  }
  
  private JLabel buildTableLabel( String in, int width){
    return new JLabel("<html><body><table width="+width+">\n"+in+"\n</table></body></html>");
  }
  
  // split categories in multiple jlabels (per linecount) so the layout can distribute columns more evenly
  private List<JLabel> buildTableLabels( String in, int linecount ){
    String[] lines = in.split("\n");
    Vector<JLabel> labels = new Vector<JLabel>();
    String buf = "";
    for (int i = 0; i < lines.length; i++) {
      buf += lines[i]+"\n";
      if ( (i+1)%linecount == 0){
        labels.add(buildTableLabel(buf));
        buf = "";
      }
    }
    if (buf.length()>0)
      labels.add(buildTableLabel(buf));

    return labels;
  }

  private String getKeyStrokeText(KeyStroke stroke) {
    String mod = KeyEvent.getModifiersExText(stroke.getModifiers());
    String out = (mod.length() > 0 ? mod + "+" : "")
        + KeyEvent.getKeyText(stroke.getKeyCode());
    return out;
  }
  
  private boolean strokeInMap( KeyStroke st, HashMap<String, HashMap<KeyStroke, PlugIn>> map ){
    for (HashMap submap : map.values()) {
      if (submap.containsKey(getKeyStrokeText(st)))
        return true;
    }
    return false;
  }
  
  private Object unWrapTool( Object tool ){
    // unwrap tools to get the original tool to tell us
    boolean unwrapped;
    do {
      unwrapped = false;
      if (tool instanceof QuasimodeTool) {
        tool = ((QuasimodeTool) tool).getDefaultTool();
        unwrapped = true;
      }
      if (tool instanceof LeftClickFilter) {
        tool = ((LeftClickFilter) tool).getWrappee();
        unwrapped = true;
      }
      if (tool instanceof DelegatingTool) {
        tool = ((DelegatingTool) tool).getDelegate();
        unwrapped = true;
      }
    } while (unwrapped);
    return tool;
  }
  
}