package org.openjump.core.ui.plugin.file;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.openjump.core.ui.plugin.file.open.ChooseProjectPanel;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

public class DataSourceQueryChooserOpenWizard extends AbstractWizardGroup {

  public static final String KEY = DataSourceQueryChooserOpenWizard.class.getName();

  private DataSourceQueryChooser chooser;

  private WorkbenchContext workbenchContext;

  private ChooseProjectPanel chooseProjectPanel;

  public DataSourceQueryChooserOpenWizard(WorkbenchContext workbenchContext,
    DataSourceQueryChooser chooser) {
    super(chooser.toString(), IconLoader.icon("Table.gif"), chooser.getClass()
      .getName());
    this.workbenchContext = workbenchContext;
    this.chooser = chooser;
  }

  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
    removeAllPanels();
    ComponentWizardPanel componentPanel = new ComponentWizardPanel(
      chooser.toString(), chooser.getClass().getName(), chooser.getComponent());
    chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
      componentPanel.getID());
    addPanel(chooseProjectPanel);
    addPanel(componentPanel);
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
    if (chooser.isInputValid()) {
      chooseProjectPanel.activateSelectedProject();
      PlugInContext context = workbenchContext.createPlugInContext();
      Collection dataSourceQueries = chooser.getDataSourceQueries();
      if (!dataSourceQueries.isEmpty()) {

        boolean exceptionsEncountered = false;
        for (Iterator i = dataSourceQueries.iterator(); i.hasNext();) {
          DataSourceQuery dataSourceQuery = (DataSourceQuery)i.next();
          ArrayList exceptions = new ArrayList();
          if (dataSourceQuery.getDataSource().isReadable()) {
            monitor.report("Loading " + dataSourceQuery.toString() + "...");

            Connection connection = dataSourceQuery.getDataSource()
              .getConnection();
            try {
              FeatureCollection dataset = dataSourceQuery.getDataSource()
                .installCoordinateSystem(
                  connection.executeQuery(dataSourceQuery.getQuery(),
                    exceptions, monitor),
                  CoordinateSystemRegistry.instance(workbenchContext.getBlackboard()));
              if (dataset != null) {
                context.getLayerManager().addLayer(chooseCategory(context),
                  dataSourceQuery.toString(), dataset).setDataSourceQuery(
                  dataSourceQuery).setFeatureCollectionModified(false);
              }
            } finally {
              connection.close();
            }
            if (!exceptions.isEmpty()) {
              if (!exceptionsEncountered) {
                context.getOutputFrame().createNewDocument();
                exceptionsEncountered = true;
              }
              reportExceptions(exceptions, dataSourceQuery, context);
            }
          } else {
            context.getWorkbenchFrame().warnUser(
              I18N.get("datasource.LoadDatasetPlugIn.query-not-readable"));
          }
        }
        if (exceptionsEncountered) {
          context.getWorkbenchFrame().warnUser(
            I18N.get("datasource.LoadDatasetPlugIn.problems-were-encountered"));
        }
      } else {
        context.getWorkbenchFrame().warnUser(
          I18N.get(KEY + ".no-queries-found"));
      }
    }
  }

  private String chooseCategory(PlugInContext context) {
    return context.getLayerNamePanel().getSelectedCategories().isEmpty() ? StandardCategoryNames.WORKING
      : context.getLayerNamePanel()
        .getSelectedCategories()
        .iterator()
        .next()
        .toString();
  }

  private void reportExceptions(ArrayList exceptions,
    DataSourceQuery dataSourceQuery, PlugInContext context) {
    context.getOutputFrame()
      .addHeader(
        1,
        exceptions.size()
          + " "
          + I18N.get("datasource.LoadDatasetPlugIn.problem")
          + StringUtil.s(exceptions.size())
          + " "
          + I18N.get("datasource.LoadDatasetPlugIn.loading")
          + " "
          + dataSourceQuery.toString()
          + "."
          + ((exceptions.size() > 10) ? I18N.get("datasource.LoadDatasetPlugIn.first-and-last-five")
            : ""));
    context.getOutputFrame().addText(
      I18N.get("datasource.LoadDatasetPlugIn.see-view-log"));
    context.getOutputFrame().append("<ul>");

    Collection exceptionsToReport = exceptions.size() <= 10 ? exceptions
      : CollectionUtil.concatenate(Arrays.asList(new Collection[] {
        exceptions.subList(0, 5),
        exceptions.subList(exceptions.size() - 5, exceptions.size())
      }));
    for (Iterator j = exceptionsToReport.iterator(); j.hasNext();) {
      Exception exception = (Exception)j.next();
      context.getWorkbenchFrame().log(StringUtil.stackTrace(exception));
      context.getOutputFrame().append("<li>");
      context.getOutputFrame().append(
        GUIUtil.escapeHTML(WorkbenchFrame.toMessage(exception), true, true));
      context.getOutputFrame().append("</li>");
    }
    context.getOutputFrame().append("</ul>");
  }
}
