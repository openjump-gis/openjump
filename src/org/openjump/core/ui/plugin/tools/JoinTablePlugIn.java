/**
 * @author Olivier BEDEL
 * Laboratoire RESO UMR 6590 CNRS
 * Bassin Versant du Jaudy-Guindy-Bizien
 * 26 oct. 2004
 * 
 */
package org.openjump.core.ui.plugin.tools;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.openjump.sigle.plugin.joinTable.JoinTable;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.ErrorDialog;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.SaveProjectAsPlugIn;

/**
 * @author Olivier BEDEL
 * Laboratoire RESO UMR 6590 CNRS
 * Bassin Versant du Jaudy-Guindy-Bizien
 * 26 oct. 2004
 * license Licence CeCILL http://www.cecill.info/
 * 
 * [sstein 25.March.2007] modified for using it in normal menu and not layerpopupmenu
 * 
 */
public class JoinTablePlugIn extends ThreadedBasePlugIn {
	protected static String name = "JoinTable"; 
	public static FileFilter JOIN_TABLE_FILE_FILTER = null; 
	private Layer layer;
	private JFileChooser fileChooser;
	private MultiInputDialog dialog;
	private MultiInputDialog layerdialog;
	private String LAYER_ATTRIBUTES = null;
	private String TABLE_ATTRIBUTES = null;
    private String sLAYER = GenericNames.LAYER;

	public void initialize(PlugInContext context) throws Exception {
		
				
		// initialisation du filtre de fichier
		JoinTablePlugIn.JOIN_TABLE_FILE_FILTER = GUIUtil.createFileFilter(I18N.get("org.openjump.sigle.plugin.joinTable.text_file"), new String[]{"txt", "text"});
		LAYER_ATTRIBUTES = I18N.get("org.openjump.sigle.plugin.joinTable.layer_field");
		TABLE_ATTRIBUTES = I18N.get("org.openjump.sigle.plugin.joinTable.table_field");
		
		/*
		context.getFeatureInstaller().addPopupMenuItem(
				context.getWorkbenchContext().getWorkbench().getFrame().getLayerNamePopupMenu(), 
			    this, 
				I18N.get("org.openjump.sigle.plugin.joinTable.Join_data")+"{pos:14}" , 
				false, 
				null, 
				new EnableCheckFactory(context.getWorkbenchContext()).createAtLeastNLayersMustExistCheck(1));
		*/
		
		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
		featureInstaller.addMainMenuItem(
				this,								//exe				
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES}, 	//menu path
				this.getName() + "...", //name methode .getName recieved by AbstractPlugIn 
				false,			//checkbox
				null,			//icon
				new EnableCheckFactory(context.getWorkbenchContext()).createAtLeastNLayersMustExistCheck(1)); //enable check        

		fileChooser = GUIUtil.createJFileChooserWithExistenceChecking();
		fileChooser.setDialogTitle(I18N.get("org.openjump.sigle.plugin.joinTable.Choose_file_data_to_join"));
		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setMultiSelectionEnabled(false);
		GUIUtil.removeChoosableFileFilters(fileChooser);
		fileChooser.addChoosableFileFilter(JOIN_TABLE_FILE_FILTER);
		fileChooser.addChoosableFileFilter(GUIUtil.ALL_FILES_FILTER);
		fileChooser.setFileFilter(JOIN_TABLE_FILE_FILTER);
	}
	
	public String getName(){
		return I18N.get("org.openjump.sigle.plugin.joinTable.Join_data");
	}
	
	public boolean execute(PlugInContext context) throws Exception{
		reportNothingToUndoYet(context);
		
		if (JFileChooser.APPROVE_OPTION != fileChooser.showOpenDialog(context
					.getWorkbenchFrame())) {
			return false;
		}
		
		//-- get Layer  -------------------------------------
		// load string again for correct language
        sLAYER = GenericNames.LAYER;
        //-- Unlike ValidatePlugIn, here we always call #initDialog because we want
        //   to update the layer comboboxes. [Jon Aquino]
        initLayerDialog(context);
        layerdialog.setVisible(true);

        if (!layerdialog.wasOKPressed()) {
            return false;
        }
        else{
        	layer = layerdialog.getLayer(sLAYER);
        }
		//-- check if attributes exist
		if (layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()==0){
			ErrorDialog.show(context.getWorkbenchFrame(), I18N.get("org.openjump.sigle.plugin.joinTable.Unable_to_join_data"), I18N.get("org.openjump.sigle.plugin.joinTable.Layer_has_no_field"), "");
			return false; 
		}

		JoinTable jt = new JoinTable(fileChooser.getSelectedFile().getAbsolutePath());
		
		//-- get unique field/attribute --------------------
		initDialog(context,jt, layer);
		dialog.setVisible(true);
		if (!dialog.wasOKPressed()) {
			jt.dispose();
			return false;
		}
		//System.out.println("indice du champ de la table : " + dialog.getComboBox(TABLE_ATTRIBUTES).getSelectedIndex() );
		//System.out.println("indice du champ de la couche : " + dialog.getComboBox(LAYER_ATTRIBUTES).getSelectedIndex() );
		jt.setKeyIndex(dialog.getComboBox(TABLE_ATTRIBUTES).getSelectedIndex());
		jt.build();
		//jointure sur la couche en memoire
		jt.join(layer,dialog.getComboBox(LAYER_ATTRIBUTES).getSelectedIndex());
		
		//liberation memoire
		jt.dispose();
		System.gc(); 
		return true;
	}
	
	public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
		//left emtpy
	}
	
	private void initDialog(PlugInContext context, JoinTable jt, Layer layer) {
		
		FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
		
		ArrayList layerAttributes = new ArrayList(schema.getAttributeCount());
		
		for (int i=0; i<schema.getAttributeCount(); i++)
			layerAttributes.add(i,schema.getAttributeName(i));	
		
		dialog = new MultiInputDialog(context.getWorkbenchFrame(), I18N.get("org.openjump.sigle.plugin.joinTable.Matching_fields"), true);
	
		dialog.setSideBarDescription(I18N.get("org.openjump.sigle.plugin.joinTable.Choose_fields_to_join"));
				
		dialog.addComboBox(LAYER_ATTRIBUTES,layerAttributes.get(0), layerAttributes, null);
		dialog.addComboBox(TABLE_ATTRIBUTES,jt.getFieldName(0), jt.getFieldNames(), null);
		GUIUtil.centreOnWindow(dialog);
	}
	
    private void initLayerDialog(PlugInContext context) {
        layerdialog = new MultiInputDialog(context.getWorkbenchFrame(), "",true);

        //dialog.setSideBarImage(IconLoader.icon("Overlay.gif"));
        layerdialog.setSideBarDescription(
        		I18N.get("org.openjump.sigle.plugin.joinTable.MenuName"));
        String fieldName = sLAYER;
        JComboBox addLayerComboBox = layerdialog.addLayerComboBox(fieldName, context.getCandidateLayer(0), null, context.getLayerManager());
        GUIUtil.centreOnWindow(layerdialog);
    }
}
