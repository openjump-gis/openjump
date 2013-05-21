/*
 * Created on 16.03.2005 for PIROL
 *
 * CVS header information:
 *  $RCSfile: SetCategoryVisibilityPlugIn.java,v $
 *  $Revision: 1.6 $
 *  $Date: 2005/11/22 16:44:42 $
 *  $Source: D:/CVS/cvsrepo/pirolPlugIns/plugIns/CategoryTools/SetCategoryVisibilityPlugIn.java,v $
 */
package org.openjump.core.ui.plugin.mousemenu.category;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelListener;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.*;

/**
 * @author Ole Rahn
 * 
 * FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck
 * Project PIROL 2005
 * Daten- und Wissensmanagement
 * 
 */
public class SetCategoryVisibilityPlugIn extends AbstractPlugIn implements LayerNamePanelListener {
    
    protected Map<Category,Boolean> category2Visibility = new HashMap<Category,Boolean>();
    protected Map<Layerable,Boolean> layer2Visibility   = new HashMap<Layerable,Boolean>();
    protected PlugInContext context = null;
    
    protected JCheckBoxMenuItem menuItem = null;
    
    protected static SetCategoryVisibilityPlugIn instance = null;
    
    private static final Logger LOG = Logger.getLogger(SetCategoryVisibilityPlugIn.class);
    
    /**
     * Constructor needed to load PlugIn from classes, should NOT be used by any other
     * code --> use getInstance() method instead!!
     */
    private SetCategoryVisibilityPlugIn(){
        SetCategoryVisibilityPlugIn.instance = this;
    }
    
    public static SetCategoryVisibilityPlugIn getInstance(){
        if (SetCategoryVisibilityPlugIn.instance == null){
            SetCategoryVisibilityPlugIn.instance = new SetCategoryVisibilityPlugIn();
        }
        return SetCategoryVisibilityPlugIn.instance;
    }

    public String getName(){
    	return 	I18N.get("org.openjump.core.ui.plugin.mousemenu.category.SetCategoryVisibilityPlugIn.Set-Category-Visibility");
    }
    
    public void initialize(PlugInContext context) throws Exception {

        this.context = context;
        
        JPopupMenu layerNamePopupMenu = context.getWorkbenchContext().getWorkbench().getFrame().getCategoryPopupMenu();

        // set category visibility to true
        MenuElement[] elements = layerNamePopupMenu.getSubElements();
        for (int i=0; i<elements.length; i++){
            if ( JCheckBoxMenuItem.class.isInstance(elements[i]) ){
                if ( ((JCheckBoxMenuItem)elements[i]).getText().startsWith(getName()) ){
                    menuItem = (JCheckBoxMenuItem)elements[i];
                    menuItem.setSelected(true);
                }
            }
        }

    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        
        multiEnableCheck.add( checkFactory.createAtLeastNLayersMustExistCheck(1) );
        multiEnableCheck.add( checkFactory.createExactlyNLayerablesMustBeSelectedCheck(0, Layerable.class) );
        multiEnableCheck.add( checkFactory.createExactlyNCategoriesMustBeSelectedCheck(1) );
        
        // simple hook to switch menuitem states
        //multiEnableCheck.add(new EnableCheck() {
        //  public String check(JComponent component) {
        //    SetCategoryVisibilityPlugIn.getInstance().layerSelectionChanged();
        //    return null;
        //  }
        //});
        
        return multiEnableCheck;
	}
    
    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("eye.png"));
    }
    
    public boolean execute(PlugInContext context) throws Exception {
        
        Collection selCats = context.getLayerNamePanel().getSelectedCategories();
        
        Iterator iter = selCats.iterator();
        Category cat;
        Boolean visible;

        while(iter.hasNext()){
            cat = (Category)iter.next();
            
            if (!this.category2Visibility.containsKey(cat)){
                this.category2Visibility.put(cat, true);
            }
            
            visible = this.category2Visibility.get(cat);
            //this.category2Visibility.remove(cat); // useless
            // inverse category visibility
            this.category2Visibility.put(cat, !visible);
            this.setLayerVisibility( cat.getLayerables(), !visible);
            menuItem.setState(!visible);
        }
        
        return true;
    }
    
    protected void setLayerVisibility(List layers, boolean visible){
        Iterator iter = layers.iterator();
        
        Layerable layer;
        
        while(iter.hasNext()){
            layer = (Layerable)iter.next();

            // add layers made invisible in the layer2Visibility map
            if (!visible && !layer2Visibility.containsKey(layer)){
                layer2Visibility.put(layer, layer.isVisible());
            }
            
            if (layer.isVisible() != visible){
                // make layer invisible
                if (!visible){
                    layer.setVisible(visible);
                }
                else {
                    if (layer2Visibility.containsKey(layer)){
                        // set the previous layer visibility
                        layer.setVisible(layer2Visibility.get(layer));
                        layer2Visibility.remove(layer);
                    } else {
                        // set layer visible
                        layer.setVisible(visible);
                    }
                }
            }
            
        }
    }
    
    protected void checkAndFixInvisibility(){
        if (context == null){
            LOG.warn("SetCategoryVisibilityPlugIn: context == null!");
            return;
        }

        Iterator iter = context.getLayerManager().getCategories().iterator();
        Category cat;
        while (iter.hasNext()){
            cat = (Category)iter.next();
            // if category is set to false, keep layer visibility in memory and make them invisible
            if (category2Visibility.containsKey(cat) && !category2Visibility.get(cat)) {
                setLayerVisibility( cat.getLayerables(), false );
            }
        }
    }
    
    public boolean isCategoryVisible(Category cat){
        if (this.category2Visibility.containsKey(cat)) {
            return this.category2Visibility.get(cat);
        }
        // by default, categories are visible
        return true;
    }
    
    public void setCategoryVisibility(Category cat, boolean visible){
        this.category2Visibility.put(cat, visible);
        this.checkAndFixInvisibility();
    }

    public void layerSelectionChanged() {

        Collection selCats = context.getWorkbenchContext().getWorkbench().getFrame().getActiveTaskFrame().getLayerNamePanel().getSelectedCategories();
        if (selCats.isEmpty()) {
            return;
        }
        else {
            Iterator iter = context.getWorkbenchContext().getWorkbench().getFrame().getActiveTaskFrame().getLayerManager().getCategories().iterator();
            Category cat;

            // Categories which were not yet in category2Visibility
            // have their default value set to true
            while (iter.hasNext()){
                cat = (Category)iter.next();
                if (!category2Visibility.containsKey(cat)){
                    category2Visibility.put(cat, true);
                }
            }
        }
    }

}
