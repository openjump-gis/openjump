/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.datasource;

import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.*;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser.FileChooserPanel;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.util.Blackboard;

import java.awt.event.ComponentAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JFileChooser;
import java.awt.event.*;


/**
 * Prompts the user to pick a dataset to load.
 * @see DataSourceQueryChooserDialog
 */
public class LoadDatasetPlugIn extends ThreadedBasePlugIn {
    private static String LAST_FORMAT_KEY = LoadDatasetPlugIn.class.getName() +
        " - LAST FORMAT";

    private DataSourceQueryChooserDialog getDialog(PlugInContext context) {
        Blackboard blackboard = context.getWorkbenchContext().getWorkbench().getBlackboard();
        String KEY = getClass().getName() + " - DIALOG";

        if (null == blackboard.get(KEY)) {
            DataSourceQueryChooserDialog dlg = new DataSourceQueryChooserDialog(
                    DataSourceQueryChooserManager.get(blackboard).getLoadDataSourceQueryChoosers(),
                    context.getWorkbenchFrame(),
                    getName(),
                    true);
            blackboard.put(KEY,dlg);

            //
            // JJ - If a user double clicks on a file then we want the same behaviour
            // as if they hit the OK button.
            // I'm not too famialiar with how people use the LoadDatasetPlugIn.
            // I have the impression that developers can add/remove choosers -
            // hence this "if" stuff.
            //
            Object obj = blackboard.get(LoadFileDataSourceQueryChooser.FILE_CHOOSER_KEY);
            if ( obj != null && obj instanceof JFileChooser ) {
                JFileChooser chooser = (JFileChooser) obj;
                chooser.addActionListener(new DoubleClickActionListener(dlg));
            }
        }
        ((DataSourceQueryChooserDialog) blackboard.get(KEY)).setDialogTask(DataSourceQueryChooserDialog.LOADDIALOG);
        return (DataSourceQueryChooserDialog) blackboard.get(KEY);
    }


    class DoubleClickActionListener implements ActionListener {
         private DataSourceQueryChooserDialog dlg;

         public DoubleClickActionListener( DataSourceQueryChooserDialog dlg ) {
             this.dlg = dlg;
         }

         public void actionPerformed(ActionEvent e) {
             //
             // The dailog's setOKPressed method calls a isInputValid method
             // which calls actionPerformed and we get into an infinite loop
             // unless we add some extra logic here and carefully order the
             // method calls in setOKPressed.
             //
             if ( !dlg.wasOKPressed() ) {
                dlg.setOKPressed();
             }
        }
    }

    public String getName() {
        //Suggest that multiple datasets may be loaded [Jon Aquino 11/10/2003]
        return I18N.get("datasource.LoadDatasetPlugIn.load-dataset");
    }

    public void initialize(final PlugInContext context) throws Exception {
        //Give other plug-ins a chance to add DataSourceQueryChoosers
        //before the dialog is realized. [Jon Aquino]
        context.getWorkbenchFrame().addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                String format = (String) PersistentBlackboardPlugIn.get(context.getWorkbenchContext())
                                                                   .get(LAST_FORMAT_KEY);
                if (format != null) {
                    getDialog(context).setSelectedFormat(format);
                }
			}
        });
    }

    public boolean execute(PlugInContext context) throws Exception {

        //Rescan current directory, otherwise, if the directory is updated, one has
        //to select the parent direcory then to come back to the current directory
        //in order to update the ui.
        FileChooserPanel fcp =
            (FileChooserPanel)context.getWorkbenchContext()
                                     .getBlackboard()
                                     .get(LoadFileDataSourceQueryChooser.FILE_CHOOSER_PANEL_KEY);
        if (fcp != null) {
            JFileChooser jfc = fcp.getChooser();
            jfc.rescanCurrentDirectory();
        }
        
        GUIUtil.centreOnWindow(getDialog(context));
        getDialog(context).setVisible(true);

        if (getDialog(context).wasOKPressed()) {
            PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).put(LAST_FORMAT_KEY,
			getDialog(context).getSelectedFormat());
        }

        return getDialog(context).wasOKPressed();
    }

    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception {
        //Seamus Thomas Carroll [mailto:carrolls@cpsc.ucalgary.ca]
        //was concerned when he noticed that #getDataSourceQueries
        //was being called twice. So call it once only. [Jon Aquino 2004-02-05]
        Collection dataSourceQueries = getDialog(context).getCurrentChooser()
                              .getDataSourceQueries();
        Assert.isTrue(!dataSourceQueries.isEmpty());

        boolean exceptionsEncountered = false;
        for (Iterator i = dataSourceQueries.iterator(); i.hasNext();) {
            DataSourceQuery dataSourceQuery = (DataSourceQuery) i.next();
            ArrayList exceptions = new ArrayList();
            Assert.isTrue(dataSourceQuery.getDataSource().isReadable());
            monitor.report("Loading " + dataSourceQuery.toString() + "...");

            Connection connection = dataSourceQuery.getDataSource()
                                                   .getConnection();
            try {
                FeatureCollection dataset = dataSourceQuery.getDataSource().installCoordinateSystem(connection.executeQuery(dataSourceQuery.getQuery(),
                        exceptions, monitor), CoordinateSystemRegistry.instance(context.getWorkbenchContext().getBlackboard()));
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

    private void reportExceptions(ArrayList exceptions,
        DataSourceQuery dataSourceQuery, PlugInContext context) {
        context.getOutputFrame().addHeader(1,
            exceptions.size() + " " + I18N.get("datasource.LoadDatasetPlugIn.problem") + StringUtil.s(exceptions.size()) +
            " "+ I18N.get("datasource.LoadDatasetPlugIn.loading") +" "+ dataSourceQuery.toString() + "." +
            ((exceptions.size() > 10) ? I18N.get("datasource.LoadDatasetPlugIn.first-and-last-five") : ""));
        context.getOutputFrame().addText(I18N.get("datasource.LoadDatasetPlugIn.see-view-log"));
        context.getOutputFrame().append("<ul>");

        Collection exceptionsToReport = exceptions.size() <= 10 ? exceptions
                                                                : CollectionUtil.concatenate(Arrays.asList(
                    new Collection[] {
                        exceptions.subList(0, 5),
                        exceptions.subList(exceptions.size() - 5,
                            exceptions.size())
                    }));
        for (Iterator j = exceptionsToReport.iterator(); j.hasNext();) {
            Exception exception = (Exception) j.next();
            context.getWorkbenchFrame().log(StringUtil.stackTrace(exception));
            context.getOutputFrame().append("<li>");
            context.getOutputFrame().append(GUIUtil.escapeHTML(
                    WorkbenchFrame.toMessage(exception), true, true));
            context.getOutputFrame().append("</li>");
        }
        context.getOutputFrame().append("</ul>");
    }

    private String chooseCategory(PlugInContext context) {
        return context.getLayerNamePanel().getSelectedCategories().isEmpty()
        ? StandardCategoryNames.WORKING
        : context.getLayerNamePanel().getSelectedCategories().iterator().next()
                 .toString();
    }

    //[sstein 26.08.2006] added for toolbar
    public static MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck());
    }
    
    //[sstein 26.08.2006] added for toolbar
    public static ImageIcon getIcon() {
        return IconLoader.icon("Plus.gif");
    }
}
