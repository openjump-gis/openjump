package org.openjump.core.ui.plugin.wms;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.wms.MapLayerWizardPanel;
import com.vividsolutions.jump.workbench.ui.plugin.wms.OneSRSWizardPanel;
import com.vividsolutions.jump.workbench.ui.plugin.wms.SRSWizardPanel;
import com.vividsolutions.jump.workbench.ui.plugin.wms.URLWizardPanel;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;
import org.openjump.core.ui.plugin.file.open.ChooseProjectPanel;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AddWmsLayerWizard extends AbstractWizardGroup {
    
  private static final String CACHED_URL = "AddWMSQueryPlugin.CACHED_URL";

  public static final String KEY = AddWmsLayerWizard.class.getName();

  private WorkbenchContext workbenchContext;

  private String[] cachedURLs = new String[] {
    "http://demo.deegree.org/deegree-wms/services",
    "http://demo.opengeo.org/geoserver/wms",
    //"http://wiki.openstreetmap.org/wiki/WMS", // does not work (missing WMT_MS_Capabilities)
    //"http://openaerialmap.org/wms/",          // does not work (2013-06-09)
    "http://wms.jpl.nasa.gov/wms.cgi",
    //"http://wms.latlon.org/?",                // does not work (missing WMT_MS_Capabilities)
    "http://www.osmgb.org.uk/OSM-GB/wms?SERVICE=WMS&",
    // addresses added on 2013-06-19
    "http://www2.demis.nl/WMS/wms.ashx?wms=WorldMap",
    "http://gridca.grid.unep.ch/cgi-bin/mapserv?map=/www/geodataportal/htdocs/mod_map/geo_wms.map&",
    "http://demo.mapserver.org/cgi-bin/wms",
    "http://wms.pcn.minambiente.it/ogc?map=/ms_ogc/WMS_v1.3/raster/ortofoto_colore_06.map&",

  };

  private String lastWMSVersion = WMService.WMS_1_1_1;

  private ChooseProjectPanel chooseProjectPanel;

  public AddWmsLayerWizard(WorkbenchContext workbenchContext) {
    super(I18N.get("org.openjump.core.ui.plugin.wms.AddWmsLayerWizard.Add-WMS-Layer"), IconLoader.icon("globe3_16.png"),
      URLWizardPanel.class.getName());
    this.workbenchContext = workbenchContext;
  }

  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
    removeAllPanels();
    String urlString = (String)PersistentBlackboardPlugIn.get(workbenchContext)
      .get(CACHED_URL);
    if (urlString != null) {
      cachedURLs = urlString.split(",");
    }

    URLWizardPanel urlPanel = new URLWizardPanel(cachedURLs, lastWMSVersion);
    chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
      urlPanel.getID());
    addPanel(chooseProjectPanel);

    addPanel(urlPanel);
    addPanel(new MapLayerWizardPanel());
    addPanel(new SRSWizardPanel());
    addPanel(new OneSRSWizardPanel());
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
      List<MapLayer> mapLayers = (List<MapLayer>)dialog.getData(MapLayerWizardPanel.LAYERS_KEY);
      String title = mapLayers.get(0).getTitle();
      List<String> layerNames = toLayerNames(mapLayers);

      WMService service = (WMService)dialog.getData(URLWizardPanel.SERVICE_KEY);
      String srs = (String)dialog.getData(SRSWizardPanel.SRS_KEY);
      String format = ((String)dialog.getData(URLWizardPanel.FORMAT_KEY));
      WMSLayer layer = new WMSLayer(title, context.getLayerManager(), service,
        srs, layerNames, format);

      LayerNamePanel layerNamePanel = context.getLayerNamePanel();
      Collection<Category> selectedCategories = layerNamePanel.getSelectedCategories();
      LayerManager mgr = context.getLayerManager();
      String categoryName = StandardCategoryNames.WORKING;
      if (!selectedCategories.isEmpty()) {
        categoryName = selectedCategories.iterator().next().getName();
      }
      mgr.addLayerable(categoryName, layer);
      cachedURLs = (String[])dialog.getData(URLWizardPanel.URL_KEY);
      lastWMSVersion = (String)dialog.getData(URLWizardPanel.VERSION_KEY);

      PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).put(
        CACHED_URL, toCommaString(cachedURLs));
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
    ArrayList<String> names = new ArrayList<String>();
    for (MapLayer layer : mapLayers) {
      names.add(layer.getName());
    }
    return names;
  }

}
