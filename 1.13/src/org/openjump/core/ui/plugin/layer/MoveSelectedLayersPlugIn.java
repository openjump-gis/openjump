package org.openjump.core.ui.plugin.layer;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JComboBox;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.model.Category;

/**
 * Move selected layer to category plugin
 * @author Giuseppe Aruta 2013-11-24
 */
public class MoveSelectedLayersPlugIn extends AbstractPlugIn {

	// Adapted from Skyjump MoveSelectedLayers PlugIn to OpenJUMP (2013-11-24)
	// - Internationalized
	// - Extended the Plugin to WMS layers and Sextante Raster layers
	// TODO: make undoable
	
	private final static String CATEGORIES = I18N
		      .get("ui.plugin.MoveLayerablePlugIn.destination-category");

	private boolean moveToTop = true;

	public static final ImageIcon ICON = IconLoader.icon("bullet_arrow_up_down.png");

	public String getName() {
		return I18N.get("ui.plugin.MoveLayerablePlugIn.move-to-category");
	}

	// Initialized through through Default-Plugins.xml

    public boolean execute(PlugInContext context) throws Exception
    {
        try
        {
        	reportNothingToUndoYet(context);
        	LayerManager layerManager = context.getWorkbenchContext().getLayerManager();
        	MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
        	Collection categoryArray = new ArrayList();
        	      
            for (Iterator i = layerManager.getCategories().iterator(); i.hasNext();)
            	categoryArray.add(((Category)i.next()).getName());
    
        	JComboBox comboBox = dialog.addComboBox(CATEGORIES, "", categoryArray, I18N
        		      .get("ui.plugin.MoveLayerablePlugIn.destination-category"));
        	comboBox.setEditable(true);
        	comboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"); //to increase the with of the combo box
        	dialog.addRadioButton(I18N
        		      .get("ui.plugin.MoveLayerablePlugIn.move-to-top"), "position", moveToTop, "Insert at top of category");
        	dialog.addRadioButton(I18N
        		      .get("ui.plugin.MoveLayerablePlugIn.move-to-bottom"), "position", !moveToTop, "Insert at bottom of category");
			GUIUtil.centreOnWindow(dialog);
			dialog.setVisible(true);
    		if (! dialog.wasOKPressed()) 
    		{ 
    			return false; 
    		}
    		else
    		{
    			String categoryName = dialog.getText(CATEGORIES).trim();
    			moveToTop = dialog.getRadioButton(I18N
    				      .get("ui.plugin.MoveLayerablePlugIn.move-to-top")).isSelected();
    			if (categoryName.length() == 0) return false;
    			if (layerManager.getCategory(categoryName) == null) 
    				layerManager.addCategory(categoryName);
    			
    			
	            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
	            Collection layerCollection = context.getWorkbenchContext()
						.getLayerNamePanel().selectedNodes(Layerable.class);
	            Layerable[] selectedLayers = (Layerable[]) layerCollection.toArray(new Layerable[]{});

	            if (moveToTop)
	            {
		            for (int i = selectedLayers.length - 1; i >= 0; i--)
		            {
		            	Layerable layerable = selectedLayers[i];
		            	layerManager.remove(layerable);
		            	layerManager.addLayerable(categoryName, layerable);
		            }
	            }
	            else
	            {
	            	Category destCat = layerManager.getCategory(categoryName);
	            	int bottomIndex = destCat.getLayerables().size();
	            	
		            for (int i = selectedLayers.length - 1; i >= 0; i--)
		            {
		            	Layerable layerable = selectedLayers[i];
		            	Category layerCat = layerManager.getCategory(layerable);
		            	layerManager.remove(layerable);
		            	if (layerCat == destCat) 
		            		bottomIndex--;
		            	destCat.add(bottomIndex, layerable);
		            }
	            }
	            //Deactivate as setCelectedLayers dosn't work with Sextante Raster Layer
	            // TreeLayerNamePanel lnp = (TreeLayerNamePanel)context.getWorkbenchContext().getLayerNamePanel();
	            // lnp.setSelectedLayers( selectedLayers);
	            return true;
    		}
        }
        catch (Exception e)
        {
            context.getWorkbenchFrame().warnUser("Error: see output window");
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame().getOutputFrame().addText("MoveSelectedLayersPlugIn Exception:" + e.toString());
            return false;
        }
    }
    
    protected int index(Layerable layerable) {
        return layerable.getLayerManager().getCategory(layerable).indexOf(layerable);
    }
    
    
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, Layerable.class));
    
    
    }  
}

