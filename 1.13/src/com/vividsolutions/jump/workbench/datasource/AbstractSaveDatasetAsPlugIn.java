package com.vividsolutions.jump.workbench.datasource;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public abstract class AbstractSaveDatasetAsPlugIn
        extends AbstractLoadSaveDatasetPlugIn {

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

        Assert.isTrue(getDataSourceQueries().size() == 1);

        DataSourceQuery dataSourceQuery = getDataSourceQueries().iterator().next();

        monitor.allowCancellationRequests();
        monitor.report(I18N.get("datasource.SaveDatasetAsPlugIn.saving") + " "
                + dataSourceQuery.toString() + "...");

        Connection connection = dataSourceQuery.getDataSource().getConnection();
        Layer layer = context.getSelectedLayer(0);
        FeatureCollection fc = (FeatureCollection)layer.getFeatureCollectionWrapper();
        try {
            connection.executeUpdate(
                    dataSourceQuery.getQuery(),
                    fc,
                    monitor);
            layer.setDataSourceQuery(dataSourceQuery).setFeatureCollectionModified(false);
        } finally {
            connection.close();
        }
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
    }
}
