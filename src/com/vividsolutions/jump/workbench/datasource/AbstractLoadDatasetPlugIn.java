package com.vividsolutions.jump.workbench.datasource;

import java.util.ArrayList;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import org.openjump.core.ui.util.ExceptionUtil;

public abstract class AbstractLoadDatasetPlugIn extends AbstractLoadSaveDatasetPlugIn {

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

        //Seamus Thomas Carroll [mailto:carrolls@cpsc.ucalgary.ca]
        //was concerned when he noticed that #getDataSourceQueries
        //was being called twice. So call it once only. [Jon Aquino 2004-02-05]
        Assert.isTrue(!getDataSourceQueries().isEmpty());

        boolean exceptionsEncountered = false;
        for (DataSourceQuery dataSourceQuery : getDataSourceQueries()) {
            ArrayList<Throwable> exceptions = new ArrayList<>();
            Assert.isTrue(dataSourceQuery.getDataSource().isReadable());
            monitor.report(I18N.get("datasource.LoadDatasetPlugIn.loading")+" " + dataSourceQuery.toString() + "...");

            Connection connection = dataSourceQuery.getDataSource().getConnection();

            try {
                FeatureCollection dataset = dataSourceQuery.getDataSource().installCoordinateSystem(
                        connection.executeQuery(dataSourceQuery.getQuery(),exceptions,monitor),
                        CoordinateSystemRegistry.instance(context.getWorkbenchContext().getBlackboard()));
                if (dataset != null) {
                    context.getLayerManager()
                           .addLayer(chooseCategory(context),
                        dataSourceQuery.toString(), dataset)
                           .setDataSourceQuery(dataSourceQuery)
                           .setFeatureCollectionModified(false);
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
        }
        if (exceptionsEncountered) {
            context.getWorkbenchFrame().warnUser(I18N.get("datasource.LoadDatasetPlugIn.problems-were-encountered"));
        }
    }

    private void reportExceptions(ArrayList<Throwable> exceptions,
        DataSourceQuery dataSourceQuery, PlugInContext context) {
        context.getOutputFrame().addHeader(1,
            exceptions.size() + " "+I18N.get("datasource.LoadDatasetPlugIn.problem") + StringUtil.s(exceptions.size()) +
            " "+ I18N.get("datasource.LoadDatasetPlugIn.loading")  + " " + dataSourceQuery.toString() + "." +
            ((exceptions.size() > 10) ? " "+I18N.get("datasource.LoadDatasetPlugIn.first-and-last-five") : ""));
        context.getOutputFrame().addText(I18N.get("datasource.LoadDatasetPlugIn.see-view-log"));

        ExceptionUtil.reportExceptions(context, exceptions);
    }

    private String chooseCategory(PlugInContext context) {
        return context.getLayerNamePanel().getSelectedCategories().isEmpty()
        ? StandardCategoryNames.WORKING
        : context.getLayerNamePanel().getSelectedCategories().iterator().next()
                 .toString();
    }

    public static MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck());
    }

}
