/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs.plugin;

import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.deegree.datatypes.QualifiedName;
import org.deegree.framework.util.StringTools;
import org.deegree.model.spatialschema.JTSAdapter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.*;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import de.latlon.deejump.wfs.client.AbstractWFSWrapper;
import de.latlon.deejump.wfs.data.JUMPFeatureFactory;
import de.latlon.deejump.wfs.i18n.I18N;
import de.latlon.deejump.wfs.jump.WFSLayer;
import de.latlon.deejump.wfs.jump.WFSLayerListener;
import de.latlon.deejump.wfs.ui.WFSDialog;
import de.latlon.deejump.wfs.ui.WFSPanel;

/**
 * JUMP plug-in providing a GUI for complex filter operations. Whole process is
 * controlled by a FeatureResearchDialog. This contains two panel, one for
 * attribute-based (re-)search and the other allowing the user to choose the
 * spatial operation to be performed (when he has selected a geometry on the map
 * view).
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * 
 */
public class WFSPlugIn extends ThreadedBasePlugIn {

  private static Logger LOG = Logger.getLogger(WFSPlugIn.class);

  private WFSDialog rd;

  private String wfsUrl;

  private static final String[] DEFAULTURLS = new String[] {
      "http://ows.terrestris.de/geoserver/osm/wfs",
      "http://demo.opengeo.org/geoserver/wfs" };

  @Override
  public void initialize(PlugInContext context) throws Exception {
    WorkbenchContext wbcontext = context.getWorkbenchContext();

    // only active if there's a map panel
    MultiEnableCheck mec = createEnableCheck(wbcontext);

    wbcontext.getWorkbench().getFrame().getToolBar()
        .addPlugIn(getIcon(), this, mec, wbcontext);

    // also create menu item
    /*
     * Disabled due to incompatibility to Vivid solutions JUMP1.2 SH, 2007-05-08
     * context.getFeatureInstaller().addMainMenuItem(this, new String[] {
     * MenuNames.LAYER }, getName(), false, getIcon(), mec);
     */
  }

  @Override
  public synchronized boolean execute(PlugInContext context) throws Exception {

    if (rd == null) {
      rd = new WFSDialog(context.getWorkbenchContext(),
          context.getWorkbenchFrame(),
          I18N.get("WFSResearchPlugIn.mainDialogTitle"));
    }

    LayerViewPanel lvPanel = context.getLayerViewPanel();
    WFSPanel pnl = rd.getWFSPanel();
    // get selected geometry(ies)
    Collection<?> geoCollec = lvPanel.getSelectionManager()
        .getFeatureSelection().getSelectedItems();
    // then make GML out of it
    org.deegree.model.spatialschema.Geometry selectedGeom;

    try {
      selectedGeom = getSelectedGeoAsGML(geoCollec);
    } catch (WorkbenchException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(context.getWorkbenchFrame(),
          e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    // get the view envelope to perform BBOX operations
    Envelope env = lvPanel.getViewport().getEnvelopeInModelCoordinates();
    pnl.setEnvelope(env);

    // sets set selected geometry
    // this geometry is used for spatial filter operations
    pnl.setComparisonGeometry(selectedGeom);
    GUIUtil.centreOnWindow(rd);
    rd.setVisible(true);
    if (!rd.canSearch()) {
      return false;
    }
    wfsUrl = pnl.getWfService().getGetFeatureURL();

    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    WorkbenchFrame wbframe = context.getWorkbenchFrame();
    WorkbenchContext wbcontext = context.getWorkbenchContext();

    monitor.report(I18N.get("WFSSearch.searching"));

    WFSPanel panel = rd.getWFSPanel();
    String request = panel.getRequest();
    String crs = panel.getGMLGeometrySRS();

    org.deegree.model.feature.FeatureCollection dfc;
    dfc = JUMPFeatureFactory.createDeegreeFCfromWFS(panel.getWfService(),
        request, panel.getFeatureType());

    monitor.report("Parsing feature collection (size = " + dfc.size() + ")");
    QualifiedName ftName = panel.getFeatureType();
    AbstractWFSWrapper wfs = panel.getWfService();

    FeatureCollection dataset;

    QualifiedName[] geoms = wfs.getGeometryProperties(ftName.getLocalName());

    if (geoms == null || geoms.length == 0) {
      LOG.info("No geometry was found, using default point at (0, 0).");
      Point point = new GeometryFactory().createPoint(new Coordinate(0, 0));
      dataset = JUMPFeatureFactory.createFromDeegreeFC(dfc, point, wfs, ftName);
    } else {
      dataset = JUMPFeatureFactory.createFromDeegreeFC(dfc, null, wfs, ftName);
    }

    monitor.report("Adding Layer");

    LayerManager layerManager = context.getLayerManager();

    QualifiedName geoQN = panel.getChosenGeoProperty();

    if (geoQN == null) {
      geoQN = new QualifiedName("GEOMETRY");
      LOG.warn("Could not determine the qualified name of the geometry property. Setting it to 'GEOMETRY'.");
    }
    geoQN = new QualifiedName(ftName.getPrefix(), geoQN.getLocalName(),
        ftName.getNamespace());

    String displayName = AbstractWFSWrapper.WFS_PREFIX + ":"
        + ftName.getLocalName();
    WFSLayer layer = new WFSLayer(displayName,
        layerManager.generateLayerFillColor(), dataset, layerManager, ftName,
        geoQN, crs, wfs);

    synchronized (layerManager) {
      WFSLayerListener listener = new WFSLayerListener(layer);
      layer.setLayerListener(listener);
      layerManager.addLayerListener(listener);
      layer.setServerURL(this.wfsUrl);
      layerManager.addLayer(StandardCategoryNames.SYSTEM, layer);
      layer.setEditable(true);
      // do not consider feature collection modified after just loading it
      layer.setFeatureCollectionModified(false);
    }

    if (dataset.size() == JUMPFeatureFactory.getMaxFeatures()) {
      wbframe.warnUser(I18N.get("WFSPlugin.maxnumber") + " "
          + JUMPFeatureFactory.getMaxFeatures());
    }

    String[] urls = panel.getWFSList().toArray(new String[] {});
    PersistentBlackboardPlugIn.get(wbcontext).put(WFSDialog.WFS_URL_LIST,
        StringTools.arrayToString(urls, ','));
  }

  /**
   * Make a GMLGeometry out of geometries inside a collection
   * 
   * @param geoCollec
   *          the Collection containing geometries
   * @return the geometries encoded as GML
   * @throws Exception
   *           if something went wrong when building or wrapping the geometries
   */
  private org.deegree.model.spatialschema.Geometry getSelectedGeoAsGML(
      Collection<?> geoCollec) throws Exception {

    if (geoCollec.size() == 0) {
      return null;
    }

    GeometryFactory gf = new GeometryFactory();
    Geometry geo = gf.buildGeometry(geoCollec);
    if (geo instanceof GeometryCollection) {
      throw new WorkbenchException(
          I18N.get("WFSResearchPlugIn.invalideGeomType"));
    }
    org.deegree.model.spatialschema.Geometry geoObj = JTSAdapter.wrap(geo);

    return geoObj;
  }

  @Override
  public String getName() {
    return "WFS Dialog";
  }

  /**
   * @return the icon
   */
  public ImageIcon getIcon() {
    return new ImageIcon(WFSPlugIn.class.getResource("wfs.png"));
  }

  private MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    MultiEnableCheck mec = new MultiEnableCheck();
    mec.add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());
    mec.add(new EnableCheck() {
      public String check(JComponent component) {
        component.setToolTipText(getName());
        return null;
      }
    });

    return mec;
  }

  public static String[] createUrlList(boolean forceDefaults) {

    if (forceDefaults)
      return DEFAULTURLS;
    
    String urls = (String) PersistentBlackboardPlugIn.get(
        JUMPWorkbench.getInstance().getContext()).get(WFSDialog.WFS_URL_LIST);
    String[] urlList = (urls == null) ? null : urls.split(",");

    if (urlList == null)
      urlList = new String[0];

    return urlList;
  }

}
