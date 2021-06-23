package org.openjump.core.ui.plugin.wms;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.wms.MapLayerWizardPanel;
import com.vividsolutions.jump.workbench.ui.plugin.wms.SRSWizardPanel;
import com.vividsolutions.jump.workbench.ui.plugin.wms.URLWizardPanel;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.MapStyle;
import com.vividsolutions.wms.WMService;
import org.openjump.core.ui.plugin.file.open.ChooseProjectPanel;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * WizardGroup used to collect parameters needed to add a WmsLayer.
 * WizardGroup contains
 * <ul>
 *   <li>URLWizardPanel</li>
 *   <li>MapLayerWizardPanel</li>
 *   <li>SRSWizardPanel</li>
 * </ul>
 */
public class AddWmsLayerWizard extends AbstractWizardGroup {
    
  public static final String CACHED_URL_KEY = "AddWMSQueryPlugin.CACHED_URL";

  public static final String KEY = AddWmsLayerWizard.class.getName();

  private final WorkbenchContext workbenchContext;

  public static final String[] DEFAULT_URLS = new String[] {
      "http://deegree3-demo.deegree.org/utah-workspace/services",
      "http://ows.terrestris.de/osm/service",
      "http://maps.omniscale.net/wms/demo/default/service",
      "http://www2.demis.nl/WMS/wms.ashx?wms=WorldMap",
      "http://demo.mapserver.org/cgi-bin/wms",
      "http://wms.pcn.minambiente.it/ogc?map=/ms_ogc/WMS_v1.3/raster/ortofoto_colore_12.map&service=wms&request=getCapabilities&version=1.3.0&",
      "http://www.gebco.net/data_and_products/gebco_web_services/web_map_service/mapserv? ",
      "http://www.gebco.net/data_and_products/gebco_web_services/north_polar_view_wms/mapserv?",
      "http://www.gebco.net/data_and_products/gebco_web_services/south_polar_view_wms/mapserv?",
      "http://magosm.magellium.com/geoserver/ows?"
  };

  private String lastWMSVersion = WMService.WMS_1_3_0;

  private ChooseProjectPanel chooseProjectPanel;

  public AddWmsLayerWizard(WorkbenchContext workbenchContext) {
    super(I18N.getInstance().get("org.openjump.core.ui.plugin.wms.AddWmsLayerWizard.Add-WMS-Layer"),
        IconLoader.icon("globe3_16.png"),
      URLWizardPanel.class.getName());
    this.workbenchContext = workbenchContext;
  }

  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
    removeAllPanels();


    URLWizardPanel urlPanel = URLWizardPanel.getInstance();
    chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
      urlPanel.getID());
    addPanel(chooseProjectPanel);

    addPanel(urlPanel);
    addPanel(new MapLayerWizardPanel());
    addPanel(new SRSWizardPanel());
    //addPanel(new OneSRSWizardPanel());
  }

  public String getFirstId() {
    String firstId = super.getFirstId();
    if (!chooseProjectPanel.hasActiveTaskFrame()
      && chooseProjectPanel.hasTaskFrames()) {
      chooseProjectPanel.setNextID(firstId);
      return chooseProjectPanel.getID();
    } else {
      return firstId;
    }
  }

  public void run(WizardDialog dialog, TaskMonitor monitor) {
    chooseProjectPanel.activateSelectedProject();
    try {
      PlugInContext context = workbenchContext.createPlugInContext();
      @SuppressWarnings("unchecked cast")
      List<MapLayer> mapLayers = (List<MapLayer>)dialog.getData(MapLayerWizardPanel.LAYERS_KEY);
      String title = mapLayers.get(0).getTitle();
      List<String> layerNames = toLayerNames(mapLayers);

      WMService service = (WMService)dialog.getData(URLWizardPanel.SERVICE_KEY);
      String srs = (String)dialog.getData(SRSWizardPanel.SRS_KEY);
      String format = ((String)dialog.getData(SRSWizardPanel.FORMAT_KEY));
      MapStyle style = (MapStyle)dialog.getData(SRSWizardPanel.STYLE_KEY);
      String moreParameters = (String)dialog.getData(SRSWizardPanel.ADDITIONAL_PARAMETERS_KEY);
      WMSLayer layer = new WMSLayer(title, context.getLayerManager(), service,
        srs, layerNames, format);
      // [mmichaud 2021-03] adding styles and more
      if (style != null && style.getName().length()>0)
        layer.setStyle(style);
      layer.setMoreParameters(moreParameters);

      LayerNamePanel layerNamePanel = context.getLayerNamePanel();
      Collection<Category> selectedCategories = layerNamePanel.getSelectedCategories();
      LayerManager mgr = context.getLayerManager();
      // zooming to the whole WMSLayer if this is the first Layerable
      if (mgr.getLayerables(Layerable.class).size() == 0) {
        try {
          workbenchContext.getLayerViewPanel().getViewport().zoom(layer.getEnvelope());
        } catch(NoninvertibleTransformException e) {
          Logger.warn(e);
        }
      }
      String categoryName = StandardCategoryNames.WORKING;
      if (!selectedCategories.isEmpty()) {
        categoryName = selectedCategories.iterator().next().getName();
      }
      mgr.addLayerable(categoryName, layer);
      String[] lastURLs = (String[])dialog.getData(URLWizardPanel.URL_KEY);
      lastWMSVersion = (String)dialog.getData(URLWizardPanel.VERSION_KEY);

      PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).put(
        CACHED_URL_KEY, toCommaString(lastURLs));
    } catch (IOException e) {
      monitor.report(e);
    }

  }

  private String toCommaString(String[] values) {
    StringBuilder string = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      string.append(values[i]);
      if (i != values.length - 1) {
        string.append(",");
      }
    }
    return string.toString();
  }

  private List<String> toLayerNames(List<MapLayer> mapLayers) {
    ArrayList<String> names = new ArrayList<>();
    for (MapLayer layer : mapLayers) {
      names.add(layer.getName());
    }
    return names;
  }

}
