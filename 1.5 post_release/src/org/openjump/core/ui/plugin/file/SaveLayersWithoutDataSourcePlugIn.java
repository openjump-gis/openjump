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

package org.openjump.core.ui.plugin.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import org.openjump.core.ui.images.IconLoader;

/**
* Subclass this to implement a 'Save Project' plugin.
*/
public class SaveLayersWithoutDataSourcePlugIn extends AbstractPlugIn {
    
    private static final String KEY = SaveLayersWithoutDataSourcePlugIn.class.getName();
    
    //public static final String SAVE_LAYERS_WITHOUT_DATASOURCE = I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.save-layers-without-datasource");
    public static final String LAYERS_WITHOUT_DATASOURCE = I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.layers-without-datasource-management");
    
    public static final String DONOTSAVE = I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.do-not-save");
    public static final String SAVEASJML = I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.save-as-jml");
    public static final String SAVEASSHP = I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.save-as-shp");
    
    public static final String FILECHOOSER = I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.directory-chooser");
    
    public static final String WARN_USER = I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.every-layer-has-a-datasource");
    
    
    private JFileChooser fileChooser;
    
    public SaveLayersWithoutDataSourcePlugIn() {
    }
    
    public String getName() {
        return I18N.get(KEY);
    }
    
    public void initialize(PlugInContext context) throws Exception {
        WorkbenchContext workbenchContext = context.getWorkbenchContext();
        
	    EnableCheck enableCheck = createEnableCheck(workbenchContext);
	    FeatureInstaller installer = new FeatureInstaller(workbenchContext);
	    installer.addMainMenuItem(
            this, new String[] {MenuNames.FILE},
            new JMenuItem(getName(), IconLoader.icon("disk_multiple.png")),
            createEnableCheck(context.getWorkbenchContext()), 9);
        
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle(FILECHOOSER);
    }
    
    public boolean execute(PlugInContext context) throws Exception {
        Collection<Layer> layersWithoutDataSource = layersWithoutDataSource(context.getTask());
        if (layersWithoutDataSource.size() == 0) {
            context.getWorkbenchFrame().warnUser(WARN_USER);
            return false;
        }
        else {
            int ret = fileChooser.showSaveDialog(context.getWorkbenchFrame());
            if (ret == JFileChooser.APPROVE_OPTION) {
                return execute(context, layersWithoutDataSource, fileChooser.getSelectedFile());
            }
        }
        return false;
    }
    
    public boolean execute(PlugInContext context, Collection<Layer> collection, File dir) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
            context.getWorkbenchFrame(),
            LAYERS_WITHOUT_DATASOURCE,
            true);
        
        String tooltip = "<html>" +
                         java.util.Arrays.toString(collection.toArray(new Object[0])).replaceAll(",","<br>") +
                         "</html>";
        
        dialog.addSubTitle(I18N.getMessage("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.layers-without-datasource", 
            new Object[]{new Integer(collection.size())}))
            .setToolTipText(tooltip);
        dialog.addLabel(I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.hover-the-label-to-see-the-list"))
            .setToolTipText(tooltip);
        dialog.addRadioButton(DONOTSAVE, "ACTION", true, "");
        dialog.addRadioButton(SAVEASJML, "ACTION", false, "");
        dialog.addRadioButton(SAVEASSHP, "ACTION", false, "");
        
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (dialog.wasOKPressed()) {
            if (dialog.getBoolean(DONOTSAVE)) {
                return false;
            }
            else {
                //File dir = FileUtil.removeExtensionIfAny(task);
                dir.mkdir();
                for (Layer layer : collection) {
                    String ext = null;
                    DataSource dataSource = null;
                    if (dialog.getBoolean(SAVEASJML)) {
                        ext = "jml";
                        dataSource = new com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource.JML();
                    }
                    else if (dialog.getBoolean(SAVEASSHP)) {
                        ext = "shp";
                        dataSource = new com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource.Shapefile();
                    }
                    saveLayer(layer, dir, dataSource, ext);
                }
                return true;
            }
        }
        else return false;   
    }
    
    private void saveLayer(Layer layer, File dir, DataSource dataSource, String ext) throws Exception {
        String name = FileUtil.getFileNameFromLayerName(layer.getName());
        // remove extension if any (ex. for layer image.png, will remove png
        int dotPos = name.indexOf(".");
        if (dotPos > 0) name = name.substring(0, dotPos);
        File fileName = FileUtil.addExtensionIfNone(new File(name), ext);
        String path = new File(dir, fileName.getName()).getAbsolutePath();
        
        DriverProperties dp = new DriverProperties();
        dp.set("File", path);
        dataSource.setProperties(dp);
                
        DataSourceQuery dsq = new DataSourceQuery(dataSource, path, path);
        layer.setDataSourceQuery(dsq).setFeatureCollectionModified(false);
                
        dataSource.getConnection().executeUpdate("", layer.getFeatureCollectionWrapper(), new DummyTaskMonitor());
    } 

    private Collection<Layer> layersWithoutDataSource(Task task) {
        ArrayList<Layer> layersWithoutDataSource = new ArrayList<Layer>();
        for (Iterator i = task.getLayerManager().getLayers().iterator(); i.hasNext();) {
            Layer layer = (Layer)i.next();
            if (!layer.hasReadableDataSource()) {
                layersWithoutDataSource.add(layer);
            }
        }
        return layersWithoutDataSource;
    }
    
    /**
     * @param workbenchContext
     * @return an enable check
     */
    public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
	    final WorkbenchContext wc = workbenchContext;
	    EnableCheckFactory enableCheckFactory = new EnableCheckFactory(workbenchContext);
	    MultiEnableCheck enableCheck = new MultiEnableCheck();
	    enableCheck.add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck());
	    enableCheck.add(new EnableCheck(){
	        public String check(javax.swing.JComponent component) {
	            return layersWithoutDataSource(wc.getTask()).size() > 0 ? null :
	                I18N.get("org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn.a-layer-without-datasource-must-exist");
	        }
	    });
	    return enableCheck;
    }

}
