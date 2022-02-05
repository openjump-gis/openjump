/**
 *
 * Copyright 2022 Edgar Soldin
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.soldin.jumpcore.geomconv;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.PointLocation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryEditor;
import org.locationtech.jts.geom.util.LinearComponentExtracter;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AttributeTab;
import com.vividsolutions.jump.workbench.ui.FeatureSelection;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog.Validator;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInfoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;

import de.soldin.jumpcore.UndoableSetGeometry;

/**
 * A jump plugin for the conversion of geometries into one another. Currently
 * supported are geometries of the type
 * <ul>
 * <li>{@link org.locationtech.jts.geom.LineString}</li>
 * <li>{@link org.locationtech.jts.geom.LinearRing}</li>
 * <li>{@link org.locationtech.jts.geom.MultiPoint}</li>
 * <li>{@link org.locationtech.jts.geom.Point}</li>
 * <li>{@link org.locationtech.jts.geom.Polygon}</li>
 * </ul>
 * 
 * 
 * @see com.vividsolutions.jump.workbench.plugin.PlugIn
 */
public class GCPlugin extends AbstractPlugIn {

  final private static I18N I18N = com.vividsolutions.jump.I18N.getInstance(new File("language/geomconv/gc"));
  private static GeometryFactory factory = new GeometryFactory();
  private static Map geoms = null;
  private static List<String> items = new ArrayList<String>();
  static {
    // add geometry keys in the order shown in gui, case sensitive as
    // key is used verbatim to resolve GeometryFactory.create...() methods
    items.add("Point");
    items.add("LineString");
    items.add("LinearRing");
    items.add("Polygon");
    items.add("MultiPoint");
    items.add("MultiLineString");
    items.add("MultiPolygon");
    items.add("GeometryCollection");
    // add tools
    items.add("separator");
    items.add("close-lines");
    items.add("remove-closing-segment");
  }
  private String target = null;
  private boolean youvebeenwarned = false;

  public GCPlugin() {
  }

  protected GCPlugin(String target) {
    this.target = target;
  }

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    // dont run for menu item plugins
    if (target != null)
      return;

    WorkbenchContext wbc = getWorkbenchContext();
    FeatureInstaller f = context.getFeatureInstaller();

    // create menu items
    String[] menuchain = new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY, getName() };
    addToMainMenu(context, menuchain);
    JMenuItem mainMenuItem = FeatureInstaller.childMenuItem(getName(), FeatureInstaller.wrapMenu(f.menuBar()),
        Arrays.copyOf(menuchain, menuchain.length-1));
    if (mainMenuItem != null)
      mainMenuItem.setIcon(getIcon());

    JPopupMenu popupMenu = LayerViewPanel.popupMenu();
    String[] popupchain = new String[] { getName() };
    addToPopupMenu(context, popupMenu, popupchain);
    JMenuItem popupItem = FeatureInstaller.childMenuItem(getName(), FeatureInstaller.wrapMenu(popupMenu));
    if (popupItem != null)
      popupItem.setIcon(getIcon());
    // addToPopupMenu(context, AttributeTab.popupMenu(wbc), popupchain);
  }

  // this can be used to attach it to another popup menu as well
  public void addToPopupMenu(PlugInContext context, JPopupMenu popupMenu, String[] popupchain) throws Exception {
    // one checker to rule them all
    EnableCheck checker = createEnableCheck();

    for (String menuentry : items) {
      // add one plugin per menuitem
      GCPlugin plugin = new GCPlugin(menuentry);
      plugin.initialize(context);
      // add separator
      if (menuentry.equals("separator")) {
        getSubMenu(popupMenu, popupchain).addSeparator();
      } else {
        // the layer popup menu
        // context.getFeatureInstaller().addPopupMenuItem(popupMenu, plugin, popupchain,
        // _m(menuentry.toLowerCase()),
        // false, null, checker);
        context.getFeatureInstaller().addPopupMenuPlugin(popupMenu, plugin, popupchain, _m(menuentry.toLowerCase()),
            false, null, checker);
      }
    }
  }

  public void addToMainMenu(PlugInContext context, String[] menuchain) throws Exception {
    // one checker to rule them all
    EnableCheck checker = createEnableCheck();
    for (Iterator iter = items.iterator(); iter.hasNext();) {
      String menuentry = (String) iter.next();
      // add one plugin per menuitem
      GCPlugin plugin = new GCPlugin(menuentry);
      plugin.initialize(context);
      // add separator
      if (menuentry.equals("separator")) {
        context.getFeatureInstaller().addMenuSeparator(menuchain);
      } else {
        // one plugin per menuentry
        context.getFeatureInstaller().addMainMenuItem(plugin, menuchain, _m(menuentry.toLowerCase()), false, null,
            checker);
      }
    }
  }

  public boolean execute(PlugInContext context) throws Exception {

    if (layerMode()) {
      Collection layers = getLayers();
      StringBuffer buf = new StringBuffer();
      for (Iterator i = layers.iterator(); i.hasNext();) {
        Layer layer = (Layer) i.next();
        String name = layer.getName();
        buf.append(buf.length() == 0 ? name : ", " + name);
      }
      String message = (target.equalsIgnoreCase("close-lines") || target.equalsIgnoreCase("remove-closing-segment"))
          ? "treat-all-with-tools"
          : "convert-all-to";
      if (!okCancel(_m("are-you-sure"), _m(message, buf, _m(target.toLowerCase()))))
        return false;
    }

    context.getLayerManager().getUndoableEditReceiver().reportNothingToUndoYet();

    // Method method = (Method) getCreatableGeoms().get(target);
    // Class[] cparams = ( method instanceof Method ) ? method.getParameterTypes() :
    // null;

    Collection layers = getLayers();
    // undo for all layers
    UndoableSetGeometry action = new UndoableSetGeometry(getName());

    for (Iterator li = layers.iterator(); li.hasNext();) {
      Layer layer = (Layer) li.next();
      Collection feats = getFeatures(layer);
      // create undo for layer
      UndoableSetGeometry layeraction = new UndoableSetGeometry(layer, getName());

      // System.out.println("exec " + target + " on " + layer.getName()
      // + " feats#" + feats.size());

      for (Iterator i = feats.iterator(); i.hasNext();) {
        try {
          Feature feat = (Feature) i.next();
          // keep for comparision with geom_new
          Geometry geom_orig = layeraction.getGeom(feat);
          // might be split in lines later
          Geometry geom_src = geom_orig;
          Geometry geom_new = null;
          // reset warning monitor
          youvebeenwarned = false;

          // TOOLS
          if (target.equalsIgnoreCase("close-lines") || target.equalsIgnoreCase("remove-closing-segment")) {

            // these tools make only sense with line segmented geometries
            // that not insist on being closed rings
            if (geom_src instanceof LinearRing || geom_src instanceof Polygon || geom_src instanceof MultiPolygon)
              geom_src = factory.createMultiLineString(getLines(geom_src));

            int count = geom_src.getNumGeometries();
            boolean changed = false;
            Geometry[] geoms_new = new Geometry[count];
            GeometryEditor editor = new GeometryEditor();

            for (int j = 0; j < count; j++) {
              Geometry geom = geom_src.getGeometryN(j);
              // weirdly closed linestrings always end up to be linearrings
              if (geom instanceof LinearRing)
                geom = factory.createLineString(geom.getCoordinates());
              // get coordinates for check
              // Coordinate[] points = geom.getCoordinates();

              if (target.equalsIgnoreCase("close-lines")) {
                geoms_new[j] = editor.edit(geom, new CloseRing());
              } else if (target.equalsIgnoreCase("remove-closing-segment")) {
                geoms_new[j] = editor.edit(geom, new RemoveClosing());
              }

              // System.out.println("geom: "+geom);
              // System.out.println("new : "+geoms_new[j]);

              // did we receive a changed geometry?
              if (!geoms_new[j].equalsExact(geom))
                changed = true;
            }
            // only create a new geometry if the old was changed
            if (changed && count > 1) {
              geom_new = factory.createGeometryCollection(geoms_new);
              // restore multigeometrytype of geom_src collection
              geom_new = convert(geom_new, geom_src.getGeometryType());
            } else if (changed && count == 1)
              geom_new = geoms_new[0];
            else
              warnUser(_m("nothing-to-do", feat.getID()));

          }
          // CONVERSIONS (moved to external method)
          else {
            geom_new = convert(geom_src, target);
          }

          // System.out.println(geom_new);
          // System.out.println(geom_orig);

          if (geom_new != null && !geom_new.equalsExact(geom_orig)) {
            layeraction.setGeom(feat, geom_new);
          }

        } catch (IllegalArgumentException e) {
          warnUser(_e(e.getMessage()));
        } catch (InvocationTargetException ie) {
          if (ie.getCause() != null)
            warnUser(_e(ie.getCause().getLocalizedMessage()));
        }
      }

      if (!layeraction.isEmpty())
        action.add(layeraction);

    }

    // don't execute & register empty actions
    if (!action.isEmpty()) {
      action.execute();
      getWorkbenchContext().getLayerManager().getUndoableEditReceiver().receive(action);
      return true;
    }
    // don't mask other warnings here
    else if (!youvebeenwarned) {
      warnUser(_m("nothing-changed"));
    }

    // operation failed
    return false;
  }

  public String getName() {
    return _m("convert-selected-to");
  }

  @Override
  public Icon getIcon() {
    // private ImageIcon gc = IconLoader.icon("EditGeometryCollection.gif");
    // private ImageIcon point = IconLoader.icon("EditPoint.gif");
    // private ImageIcon mpoint = IconLoader.icon("EditMultiPoint.gif");
    // private ImageIcon line = IconLoader.icon("EditLineString.gif");
    // private ImageIcon mline = IconLoader.icon("EditMultiLineString.gif");
    // private ImageIcon poly = IconLoader.icon("EditPolygon.gif");
    // private ImageIcon mpoly = IconLoader.icon("EditMultiPolygon.gif");
    // private ImageIcon lring = IconLoader.icon("EditLinearRing.gif");

    if (target == null)
      return IconLoader.icon("geometry_converter.png");

    if (getCreateGeometryMethods().containsKey(target)) {
      ImageIcon icon = IconLoader.icon("Edit" + target + ".gif");
      return GUIUtil.pad(icon, 3);
    } else if (target.equals("close-lines"))
      return IconLoader.icon("geometry_converter_closering.png");
    else if (target.equals("remove-closing-segment"))
      return IconLoader.icon("geometry_converter_openring.png");

    return null;
  }

  private JMenu getSubMenu(MenuElement menu, String[] keys) {
    MenuElement[] ms = menu.getSubElements();
    String key = keys[0];
    for (int i = 0; i < ms.length; i++) {
      MenuElement m = ms[i];
      if (m == null)
        continue;
      if (m instanceof JMenu && ((JMenu) m).getText().equals(key)) {
        // snip first
        if (keys.length > 1) {
          String[] subkeys = new String[keys.length - 1];
          for (int j = 1; j < keys.length; j++) {
            subkeys[j] = keys[j];
          }
          m = getSubMenu(menu, subkeys);
        } else {
          return (JMenu) m;
        }
      }
    }
    return null;
  }

  public EnableCheck createEnableCheck() {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(getWorkbenchContext());
    MultiEnableCheck checker = new MultiEnableCheck();

    // taskframe must be active
    checker.add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());

    // are there selected layers OR layers with selected features
    // are these layers editable?
    checker.add(new EnableCheck() {
      public String check(JComponent component) {
        Collection layers = getLayers();
        if (layers == null || layers.isEmpty())
          return _m("select-geometries-or-layers");

        for (Iterator iterator = layers.iterator(); iterator.hasNext();) {
          Layer layer = (Layer) iterator.next();
          // System.out.println(layer.getName()
          // +"->"+(layer.isEditable()?"ja":"nein"));
          if (!layer.isEditable()) {
            return _m("layer-not-editable", layer.getName());
          }
        }

        // reached here? all is well
        return null;
      }
    });

    return checker;
  }

  private boolean layerMode() {
    FeatureSelection sel = ((SelectionManagerProxy) getWorkbenchContext().getWorkbench().getFrame()
        .getActiveInternalFrame()).getSelectionManager().getFeatureSelection();

    // user hand picked geometries (features)
    if (!sel.getFeaturesWithSelectedItems().isEmpty()) {
      return false;
    }
    // user selected layers
    else {
      return true;
    }
  }

  private Collection getFeatures(Layer layer) {
    FeatureSelection sel = ((SelectionManagerProxy) getWorkbenchContext().getWorkbench().getFrame()
        .getActiveInternalFrame()).getSelectionManager().getFeatureSelection();

    Collection feats;
    // user hand picked geometries (features)
    if (!sel.getFeaturesWithSelectedItems().isEmpty()) {
      feats = sel.getFeaturesWithSelectedItems(layer);
    }
    // user only selected layers
    else {
      feats = layer.getFeatureCollectionWrapper().getFeatures();
    }

    return feats;
  }

  private Collection getLayers() {
    // all layers with selected items (parts of geometries)
    Collection layers = ((SelectionManagerProxy) getWorkbenchContext().getWorkbench().getFrame()
        .getActiveInternalFrame()).getSelectionManager().getFeatureSelection().getLayersWithSelectedItems();
    return layers.isEmpty() ? Arrays.asList(getWorkbenchContext().getLayerableNamePanel().getSelectedLayers()) : layers;
  }

  private Collection<String> getTypes(GeometryCollection geom) {
    Collection types = new Vector();
    for (int i = 0; i < geom.getNumGeometries(); i++) {
      String type = geom.getGeometryN(i).getGeometryType();
      if (!types.contains(type))
        types.add(type);
    }
    return types;
  }

  private static Map getCreateGeometryMethods() {
    if (geoms != null)
      return geoms;

    geoms = new Hashtable();
    for (String key : items) {
      if (key.equals("separator"))
        break;

      Method m = null;
      if (key.equals("Point")) {
        m = findMethod("create" + key, Coordinate.class);
      } else if (key.equals("Polygon")) {
        m = findMethod("create" + key, new Class[] { LinearRing.class, LinearRing[].class });
      } else if (key.equals("MultiPoint")) {
        m = findMethod("createMultiPointFromCoords", Coordinate[].class);
      } else if (key.equals("MultiLineString")) {
        m = findMethod("create" + key, LineString[].class);
      } else if (key.equals("MultiPolygon")) {
        m = findMethod("create" + key, Polygon[].class);
      } else if (key.equals("GeometryCollection")) {
        m = findMethod("create" + key, Geometry[].class);
      }
      // the rest can work with coord arrays
      else {
        m = findMethod("create" + key, Coordinate[].class);
      }
      if (m != null)
        geoms.put(key, m);
    }

    return geoms;
  }

  private static Method findMethod(String name, Class... params) {
    try {
      return GeometryFactory.class.getDeclaredMethod(name, params);
    } catch (Exception e) {
      Logger.error(e);
      return null;
    }
  }

  private static boolean validType(Class clazz) {
    String name;
    if (clazz.isArray())
      name = clazz.getComponentType().getName();
    else
      name = clazz.getName();
    return name.equals("org.locationtech.jts.geom.Coordinate") || name.equals("org.locationtech.jts.geom.LinearRing")
        || name.equals("org.locationtech.jts.geom.LineString") || name.equals("org.locationtech.jts.geom.Polygon")
        || name.equals("org.locationtech.jts.geom.Geometry");
  }

  private void warnUser(final String message) {
    this.youvebeenwarned = true;
    getWorkbenchContext().getLayerViewPanel().getContext().warnUser(message);
  }

  private Geometry convert(Geometry geom_src, String type) throws Exception {
    Geometry geom_new = null;

    Method method = (Method) getCreateGeometryMethods().get(type);
    // do we have to && can we convert?
    if (!(method instanceof Method)) {
      // ups we've got ourself no conversion
      warnUser(_m("no-conversion-method", _m(type.toLowerCase()), type));
      return null;
    }

    /*
     * // Multi*s and polygon always get line segments separated
     * // Points contain _zero_ line segments
     * LineString[] lines = null;
     * if ( (type.toLowerCase().startsWith("multi") ||
     * type.equalsIgnoreCase("polygon")) ){
     * lines = getLines( geom_src, false );
     * if (lines.length > 0)
     * geom_src = factory.createMultiLineString( lines );
     * }
     */

    Class[] cparams = method.getParameterTypes();

    // convert empty geometries
    if (geom_src.isEmpty()) {
      List<Object> params = new ArrayList<Object>();
      for (Class clazz : cparams) {
        params.add(null);
      }
      geom_new = (Geometry) method.invoke(factory, params.toArray());
      return geom_new;
    }

    boolean isArray = cparams[0].isArray();
    String name = isArray ? cparams[0].getComponentType().getName() : cparams[0].getName();
    // made from one coord, probably point ;)
    if (!isArray && name.equals("org.locationtech.jts.geom.Coordinate")) {
      // factory method needs exactly one coord or null
      if (geom_src.getCoordinates().length > 1) {
        warnUser(_m("only-one-coordinate", type));
      } else {
        Object coord = geom_src.getCoordinates().length < 1 ? null : geom_src.getCoordinates()[0];
        geom_new = (Geometry) method.invoke(factory, new Object[] { coord });
      }
    }
    // simple geometries made from coord[]
    else if (isArray && name.equals("org.locationtech.jts.geom.Coordinate")) {
      // System.out.println("mehrere koordinaten");
      geom_new = (Geometry) method.invoke(factory, new Object[] { geom_src.getCoordinates() });
    }
    // multilinestring
    else if (isArray && name.equals("org.locationtech.jts.geom.LineString")) {
      Coordinate[] coords = geom_src.getCoordinates();

      geom_new = (Geometry) method.invoke(factory, new Object[] { getLines(geom_src) });

    }
    // polygon
    else if (cparams.length == 2 && name.equals("org.locationtech.jts.geom.LinearRing") && cparams[1].isArray()
        && name.equals(cparams[1].getComponentType().getName())) {

      geom_new = constructPolygon(geom_src);

    }
    // multipolygon
    else if (isArray && name.equals("org.locationtech.jts.geom.Polygon")) {
      // feed the line separated geom into algorithm
      Polygon[] polys = constructPolygons(geom_src);
      if (polys != null)
        geom_new = (Geometry) method.invoke(factory, new Object[] { polys });
    }
    // geometrycollection
    else if (isArray && name.equals("org.locationtech.jts.geom.Geometry")) {
      // get all geoms and feed them to create
      Geometry[] geoms = new Geometry[geom_src.getNumGeometries()];
      for (int j = 0; j < geom_src.getNumGeometries(); j++) {
        geoms[j] = geom_src.getGeometryN(j);
      }
      geom_new = (Geometry) method.invoke(factory, new Object[] { geoms });
    }
    // what ends here is based on parameters that are not implemented yet
    else {
      warnUser(_m("conversion-not-implemented", _m(type.toLowerCase()), type));
    }

    return geom_new;
  }

  /**
   * borrowed from org.geotools.shapefile.PolygonHandler
   * try to construct _one_ polygon from the given geometry
   */
  private Polygon constructPolygon(Geometry src) {

    // multigeometries with just points are tried to form one linearring
    // for further processing, else multipoint -> polygon is pointless
    if (src instanceof GeometryCollection) {
      Collection<String> types = getTypes((GeometryCollection) src);
      if (types.size() == 1 && types.iterator().next().equalsIgnoreCase("point"))
        src = factory.createLinearRing(src.getCoordinates());
    }

    ArrayList<LinearRing> shells = new ArrayList<LinearRing>();
    ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
    try {
      // split by direction shell are cw, holes ccw
      for (int i = 0; i < src.getNumGeometries(); i++) {
        Geometry geom = src.getGeometryN(i);

        LinearRing ring = factory.createLinearRing(geom.getCoordinates());
        if (Orientation.isCCW(ring.getCoordinates())) {
          holes.add(ring);
        } else {
          shells.add(ring);
        }
      }

      // something went wrong, detect holes now
      if (shells.size() != 1) {
        // throw it all together and let findCWHoles find it
        shells.addAll(holes);
        holes.clear();
        // some shells may be CW holes - esri tolerates this
        ArrayList<LinearRing> foundholes = findCWHoles(shells);

        if (foundholes.size() > 0) {
          shells.removeAll(foundholes);
          for (int j = 0; j < foundholes.size(); j++) {
            LinearRing hole = (LinearRing) foundholes.get(j);
            // reverse cw holes
            if (!Orientation.isCCW(hole.getCoordinates()))
              hole = reverseRing(hole);
            holes.add(hole);
          }
        }
      }

      // ups, don't convert this
      if (shells.size() != 1) {
        warnUser(_m("missing-exactly-one-shell"));
        return null;
      }

      return factory.createPolygon((LinearRing) shells.get(0),
          (LinearRing[]) ((ArrayList) holes).toArray(new LinearRing[0]));

    } catch (IllegalArgumentException e) {
      warnUser(_e(e.getMessage()));
    }

    return null;
  }

  /**
   * borrowed from org.geotools.shapefile.PolygonHandler
   * try to construct multiple polygons from the given geometry
   * 
   * @param src source Geometry
   * @return an array of Polygons
   */
  public Polygon[] constructPolygons(Geometry src) {

    // split src to ring components early
    src = factory.createMultiLineString(getRings(src));

    GeometryFactory geometryFactory = this.factory;
    ArrayList<LinearRing> shells = new ArrayList<LinearRing>();
    ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
    // Bad rings are CCW rings not nested in another ring
    // and rings with more than 0 and less than 4 points
    // ArrayList<LineString> badRings = new ArrayList<LineString>();
    // resulting polygons
    Polygon[] polygons;

    for (int i = 0; i < src.getNumGeometries(); i++) {
      Geometry geom = src.getGeometryN(i);

      Coordinate[] points = geom.getCoordinates();
      try {
        LinearRing ring = geometryFactory.createLinearRing(points);
        if (Orientation.isCCW(points)) {
          holes.add(ring);
        } else {
          shells.add(ring);
        }
      } catch (IllegalArgumentException e) {
        // badrings means loss, means error, means stop here
        warnUser(_m("bad-rings"));
        return null;

        // crashes on simple points
        /*
         * try {
         * LineString ring = geometryFactory.createLineString(points);
         * badRings.add(ring);
         * } catch (IllegalArgumentException e2) {
         * warnUser(_(e2.getMessage()));
         * }
         */
      }
    }

    if ((shells.size() > 1) && (holes.size() == 0)) {
      // some shells may be CW holes - esri tolerates this
      holes = findCWHoles(shells); // find all rings contained in others
      if (holes.size() > 0) {
        shells.removeAll(holes);
        ArrayList ccwHoles = new ArrayList(holes.size());
        for (int i = 0; i < holes.size(); i++) {
          ccwHoles.add(reverseRing((LinearRing) holes.get(i)));
        }
        holes = ccwHoles;
      }
    }

    // now we have a list of all shells and all holes
    ArrayList holesForShells = new ArrayList(shells.size());
    ArrayList holesWithoutShells = new ArrayList();

    for (int i = 0; i < shells.size(); i++) {
      holesForShells.add(new ArrayList());
    }

    // find holes
    for (int i = 0; i < holes.size(); i++) {
      LinearRing testRing = (LinearRing) holes.get(i);
      LinearRing minShell = null;
      Envelope minEnv = null;
      Envelope testEnv = testRing.getEnvelopeInternal();
      Coordinate testPt = testRing.getCoordinateN(0);
      LinearRing tryRing;
      for (int j = 0; j < shells.size(); j++) {
        tryRing = (LinearRing) shells.get(j);
        Envelope tryEnv = tryRing.getEnvelopeInternal();
        if (minShell != null)
          minEnv = minShell.getEnvelopeInternal();
        boolean isContained = false;
        Coordinate[] coordList = tryRing.getCoordinates();

        if (tryEnv.contains(testEnv) && (PointLocation.isInRing(testPt, coordList)))
          isContained = true;
        // check if this new containing ring is smaller than the current
        // minimum ring
        if (isContained) {
          if (minShell == null || minEnv.contains(tryEnv)) {
            minShell = tryRing;
          }
        }
      }

      if (minShell == null) {
        holesWithoutShells.add(testRing);
      } else {
        ((ArrayList) holesForShells.get(shells.indexOf(minShell))).add(testRing);
      }
    }

    polygons = new Polygon[shells.size() + holesWithoutShells.size()];

    for (int i = 0; i < shells.size(); i++) {
      polygons[i] = geometryFactory.createPolygon((LinearRing) shells.get(i),
          (LinearRing[]) ((ArrayList) holesForShells.get(i)).toArray(new LinearRing[0]));
    }

    // add ccw holes without shells as cw shells
    for (int i = 0; i < holesWithoutShells.size(); i++) {
      polygons[shells.size() + i] = geometryFactory.createPolygon(reverseRing(((LinearRing) holesWithoutShells.get(i))),
          null);
    }

    holesForShells = null;
    holesWithoutShells = null;
    shells = null;
    holes = null;

    return polygons;
  }

  /**
   * finds lr contained in other lr's (holes), returns all holes
   */
  private ArrayList<LinearRing> findCWHoles(ArrayList shells) {
    ArrayList holesCW = new ArrayList(shells.size());
    LinearRing[] noHole = new LinearRing[0];
    for (int i = 0; i < shells.size(); i++) {
      LinearRing iRing = (LinearRing) shells.get(i);
      Envelope iEnv = iRing.getEnvelopeInternal();
      Coordinate[] coordList = iRing.getCoordinates();
      LinearRing jRing;
      for (int j = 0; j < shells.size(); j++) {
        if (i == j)
          continue;
        jRing = (LinearRing) shells.get(j);
        Envelope jEnv = jRing.getEnvelopeInternal();
        Coordinate jPt = jRing.getCoordinateN(0);
        Coordinate jPt2 = jRing.getCoordinateN(1);
        if (iEnv.contains(jEnv)
            // && (CGAlgorithms.isPointInRing(jPt, coordList) ||
            // pointInList(jPt, coordList))
            // && (CGAlgorithms.isPointInRing(jPt2, coordList) ||
            // pointInList(jPt2, coordList))) {
            && (PointLocation.isInRing(jPt, coordList)) && (PointLocation.isInRing(jPt2, coordList))) {
          if (!holesCW.contains(jRing)) {
            Polygon iPoly = factory.createPolygon(iRing, noHole);
            Polygon jPoly = factory.createPolygon(jRing, noHole);
            if (iPoly.contains(jPoly))
              holesCW.add(jRing);
          }
        }
      }
    }
    return holesCW;
  }

  /**
   * reverses the order of points in lr (is CW -> CCW or CCW->CW)
   */
  private LinearRing reverseRing(LinearRing lr) {
    int numPoints = lr.getNumPoints();
    Coordinate[] newCoords = new Coordinate[numPoints];
    for (int t = 0; t < numPoints; t++) {
      newCoords[t] = lr.getCoordinateN(numPoints - t - 1);
    }
    return factory.createLinearRing(newCoords);
  }

  /**
   * separate line components in a (multi)geometry
   */
  private LineString[] getLines(Geometry geom_src) {
    return (LineString[]) getLines(geom_src, false);
  }

  private LinearRing[] getRings(Geometry geom_src) {
    return (LinearRing[]) getLines(geom_src, true);
  }

  private Geometry[] getLines(Geometry geom_src, boolean asRings) {
    // List linescol = new ArrayList();
    // geom_src.apply(new LineFilter( linescol ));

    List linescol = new ArrayList();
    for (int i = 0; i < geom_src.getNumGeometries(); i++) {
      Geometry g = geom_src.getGeometryN(i);
      // System.out.println(g.getNumGeometries()+","+g.getGeometryType());
      if (g.getNumGeometries() > 1) {
        linescol.addAll(Arrays.asList(getLines(g, asRings)));
      } else if (g.getGeometryType().toLowerCase().endsWith("polygon"))
        LinearComponentExtracter.getLines(g, linescol, !asRings);
      else if (asRings)
        linescol.add(factory.createLinearRing(g.getCoordinates()));
      else
        linescol.add(factory.createLineString(g.getCoordinates()));
    }

    return asRings ? factory.toLinearRingArray(linescol) : factory.toLineStringArray(linescol);
  }

  private boolean okCancel(final String title, final String message) {
    JPanel panel = new JPanel();
    JLabel label = new JLabel("<html> " + message + " </html>");
    Font xx = label.getFont();
    int fontHeight = label.getFontMetrics(xx).getHeight();
    int stringWidth = label.getFontMetrics(xx).stringWidth(label.getText());
    int linesCount = (int) Math.floor(stringWidth / 300);
    linesCount = Math.max(1, linesCount + 2);
    label.setPreferredSize(new Dimension(300, (fontHeight) * linesCount));
    FlowLayout f = new FlowLayout(FlowLayout.CENTER, 10, 10);
    panel.setLayout(f);
    panel.add(label);

    OKCancelDialog dlg = new OKCancelDialog(getWorkbenchContext().getWorkbench().getFrame(), title, true, panel,
        new Validator() {
          public String validateInput(Component component) {
            return null;
          }
        });

    dlg.setVisible(true);

    return dlg.wasOKPressed() ? true : false;
  }

  private class CloseRing extends GeometryEditor.CoordinateOperation {
    public Coordinate[] edit(Coordinate[] coordinates, Geometry geometry) {
      // ignore already closed
      Coordinate first = coordinates[0];
      Coordinate last = coordinates[coordinates.length - 1];

      if (first.equals(last))
        return coordinates;

      Coordinate[] coordinates_new = new Coordinate[coordinates.length + 1];
      for (int i = 0; i < coordinates.length; i++) {
        coordinates_new[i] = coordinates[i];
      }
      coordinates_new[coordinates_new.length - 1] = coordinates[0];
      return coordinates_new;
    }
  }

  private class RemoveClosing extends GeometryEditor.CoordinateOperation {
    public Coordinate[] edit(Coordinate[] coordinates, Geometry geometry) {
      // ignore points, nothing to remove here
      if (coordinates.length < 2)
        return coordinates;
      Coordinate first = coordinates[0];
      Coordinate last = coordinates[coordinates.length - 1];
      while (first.equals(last)) {
        coordinates = removeLast(coordinates);
        first = coordinates[0];
        last = coordinates[coordinates.length - 1];
      }
      return coordinates;
    }

    private Coordinate[] removeLast(Coordinate[] coordinates) {
      Coordinate[] coordinates_new = new Coordinate[coordinates.length - 1];
      for (int i = 0; i < coordinates.length - 1; i++) {
        coordinates_new[i] = coordinates[i];
      }
      return coordinates_new;
    }
  }

  private class LineFilter implements GeometryComponentFilter {
    private Collection lines;

    public LineFilter(List lines) {
      this.lines = lines;
    }

    public void filter(Geometry geom) {
      List linescol = new ArrayList();
      // convert each subgeom to linestring, jts throws exceptions on impossibles
      for (int i = 0; i < geom.getNumGeometries(); i++) {
        LineString line = geom.getFactory().createLineString(geom.getGeometryN(i).getCoordinates());
        lines.add(line);
      }
    }
  }

  // translate
  private String _m(String string, Object... objects) {
    return I18N.get(string, objects);
  }

  private String _e(final String string) {
    return getMessageByMessage(string);
  }

  // lookup translated jts error messages
  private String getMessageByMessage(final String string) {
    Pattern pattern = Pattern.compile("(\\b)[\\d]+(\\b)");
    Matcher matcher = pattern.matcher(string);

    // Find all the matches.
    Collection numbers = new Vector();
    StringBuffer result = new StringBuffer();
    int i = 0;
    while (matcher.find()) {
      numbers.add(matcher.group());
      matcher.appendReplacement(result, "{" + (i++) + "}");
    }
    matcher.appendTail(result);

    // generate a properties-save key
    final String key = result.toString().toLowerCase().replaceAll("[\\s:=-]+", "-");
    String message = _m(key, numbers.toArray());

    // no message? use delivered
    return message.equals(key) ? string : message;
  }
}