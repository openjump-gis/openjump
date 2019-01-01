/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.deegree.datatypes.QualifiedName;
import org.deegree.framework.util.StringTools;
import org.deegree.model.spatialschema.JTSAdapter;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.util.metaData.MetaInformationHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorV2;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.WorkbenchException;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import de.latlon.deejump.wfs.WFSExtension;
import de.latlon.deejump.wfs.client.AbstractWFSWrapper;
import de.latlon.deejump.wfs.client.WFSClientHelper;
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

  public final static String I18NPREFIX = WFSExtension.class.getPackage()
      .getName();

  private WFSDialog wfsDialog;

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

    if (wfsDialog == null) {
      wfsDialog = new WFSDialog(context.getWorkbenchContext(),
          context.getWorkbenchFrame(), i18n("mainDialogTitle"));
    }

    LayerViewPanel lvPanel = context.getLayerViewPanel();
    WFSPanel pnl = wfsDialog.getWFSPanel();
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
    GUIUtil.centreOnWindow(wfsDialog);
    wfsDialog.setVisible(true);
    if (!wfsDialog.canSearch()) {
      return false;
    }
    wfsUrl = pnl.getWfService().getGetFeatureURL();

    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    WorkbenchFrame wbframe = context.getWorkbenchFrame();
    WorkbenchContext wbcontext = context.getWorkbenchContext();

    WFSPanel panel = wfsDialog.getWFSPanel();
    String request = panel.getRequest();
    String crs = panel.getGMLGeometrySRS();

    // read response into temp file
    if (monitor instanceof TaskMonitorV2)
      ((TaskMonitorV2) monitor).setTitle(i18n("read-response-to-tempfile"));
    InputStream is = WFSClientHelper.createResponseStreamfromWFS(panel
        .getWfService().getGetFeatureURL(), request);
    File tmpFile = FileUtil.copyInputStreamToTempFile(is, "wfs", null, monitor);

    if (monitor instanceof TaskMonitorV2)
      ((TaskMonitorV2) monitor).setTitle(i18n("processing"));
    monitor.report(i18n("create-deegree-collection"));
    org.deegree.model.feature.FeatureCollection dfc;
    JUMPFeatureFactory jff = new JUMPFeatureFactory(monitor);
    // finally we do something constructive
    dfc = jff.createDeegreeFCfromFile(panel.getWfService(), tmpFile, null);
    // done? fine, cleanup
    tmpFile.delete();

    monitor.report(i18n("convert-to-OJ-feature-collection (size = {0})",
        dfc.size()));
    QualifiedName ftName = panel.getFeatureType();
    AbstractWFSWrapper wfs = panel.getWfService();

    FeatureCollection dataset;

    QualifiedName[] geoms = wfs.getGeometryProperties(ftName.getLocalName());

    if (geoms == null || geoms.length == 0) {
      Logger.info("No geometry was found, using default point at (0, 0).");
      Point point = new GeometryFactory().createPoint(new Coordinate(0, 0));
      dataset = jff.createFromDeegreeFC(dfc, point, wfs, ftName);
    } else {
      dataset = jff.createFromDeegreeFC(dfc, null, wfs, ftName);
    }

    monitor.report(i18n("adding-layer"));

    LayerManager layerManager = context.getLayerManager();

    QualifiedName geoQN = panel.getChosenGeoProperty();

    if (geoQN == null) {
      geoQN = new QualifiedName("GEOMETRY");
      Logger.warn("Could not determine the qualified name of the geometry property. Setting it to 'GEOMETRY'.");
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
      // [Giuseppe Aruta 4/21/2016, ede 25.4.2016]
      // Workaround to record WFS Layer SRS as SRIDStyle
      SRIDStyle sridStyle = (SRIDStyle) layer.getStyle(SRIDStyle.class);
      Pattern p = Pattern.compile("^.*epsg:(?:[\\d\\.]*:)?(\\d+)$",
          Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(crs != null ? crs : "");
      int srid = 0;
      // only set if we can interpret the given srs string
      if (m.matches()) {
        String epsgId = m.group(1);
        try {
          srid = Integer.parseInt(epsgId);
        } catch (NumberFormatException e) {
          Logger.error(e);
        }
      }
      sridStyle.setSRID(srid);
     
      // do not consider feature collection modified after just loading it
      layer.setFeatureCollectionModified(false);
    }

    if (dataset.size() >= jff.getMaxFeatures()) {
      wbframe.warnUser(i18n(
          "wfs-request-resulted-in-maxnumber-{0}-items-the-resultset-is-likely-bigger",
          String.format(Locale.US, "%d", dataset.size())));
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
      throw new WorkbenchException(i18n("invalid-geometry-type-geometrycollection"));
    }
    // TODO: fetch SRS info and use it
    org.deegree.model.spatialschema.Geometry geoObj = JTSAdapter
        .wrap(geo, null);

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
    String[] urlList = (urls == null) ? new String[0] : urls.split(",");

    if (urlList.length < 1)
      return DEFAULTURLS;

    return urlList;
  }

  /**
   * utility method for the whole package
   * 
   * @param key
   * @param arguments
   * @return
   */
  public static String i18n(String key, Object... arguments) {
    key = I18NPREFIX + "." + key;
    return I18N.get(key, arguments);
  }

}
