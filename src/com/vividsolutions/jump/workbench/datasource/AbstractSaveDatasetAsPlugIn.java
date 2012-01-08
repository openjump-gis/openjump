package com.vividsolutions.jump.workbench.datasource;

import java.util.Collection;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public abstract class AbstractSaveDatasetAsPlugIn
        extends
            AbstractLoadSaveDatasetPlugIn {

    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        Assert.isTrue(getDataSourceQueries().size() == 1);

        DataSourceQuery dataSourceQuery = (DataSourceQuery) getDataSourceQueries().iterator().next();
        Assert.isTrue(dataSourceQuery.getDataSource().isWritable());
        monitor.report(I18N.get("datasource.SaveDatasetAsPlugIn.saving") + " "
                + dataSourceQuery.toString() + "...");

        Connection connection = dataSourceQuery.getDataSource().getConnection();
        try {
            connection
                    .executeUpdate(dataSourceQuery.getQuery(), context
                            .getSelectedLayer(0).getFeatureCollectionWrapper(),
                            monitor);
        } finally {
            connection.close();
        }
        context.getSelectedLayer(0).setDataSourceQuery(dataSourceQuery)
                .setFeatureCollectionModified(false);
    }

    public static MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
    }
}
