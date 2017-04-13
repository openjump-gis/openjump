package com.vividsolutions.jump.workbench.datasource;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;

import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public abstract class AbstractLoadSaveDatasetPlugIn extends ThreadedBasePlugIn {

    private WorkbenchContext context;
    protected String getLastFormatKey() { return getClass().getName() + " - LAST FORMAT"; }    
    protected String getLastDirectoryKey() { return getClass().getName() + " - LAST DIRECTORY"; }    

    public void initialize(final PlugInContext context) throws Exception {
        this.context = context.getWorkbenchContext();
        //Give other plug-ins a chance to add DataSourceQueryChoosers
        //before the dialog is realized. [Jon Aquino]
        context.getWorkbenchFrame().addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                String format = (String) PersistentBlackboardPlugIn.get(context.getWorkbenchContext())
                                                                   .get(getLastFormatKey());
                if (format != null) {
                    setSelectedFormat(format);
                }
            }
        });
    }

    protected abstract void setSelectedFormat(String format);
    protected abstract String getSelectedFormat();
    protected WorkbenchContext getContext() {
        return context;
    }
    private Collection<DataSourceQuery> dataSourceQueries;

    public boolean execute(PlugInContext context) throws Exception {
        dataSourceQueries = showDialog(context.getWorkbenchContext());
        if (dataSourceQueries != null) {
            PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).put(getLastFormatKey(),
                    getSelectedFormat());
        }
        return dataSourceQueries != null;
    }

    protected abstract Collection showDialog(WorkbenchContext context);

    protected Collection<DataSourceQuery> getDataSourceQueries() {
        return dataSourceQueries;
    }
}
