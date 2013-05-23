/*
 * Created on 03.05.2005 for PIROL
 *
 * CVS header information:
 *  $RCSfile: CategoryMover.java,v $
 *  $Revision: 1.3 $
 *  $Date: 2005/09/13 08:45:58 $
 *  $Source: D:/CVS/cvsrepo/pirolPlugIns/plugIns/CategoryTools/CategoryMover.java,v $
 */
package org.openjump.core.ui.plugin.mousemenu.category;

import java.util.List;

import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * Provides methods to move a category including the layers in it within the LayerNamePanel.
 *
 * @author Ole Rahn
 * @author FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * Project: PIROL (2005),
 * Subproject: Daten- und Wissensmanagement
 * 
 */
public class CategoryMover {

    protected PlugInContext context = null;

    public CategoryMover(PlugInContext context) {
        this.context = context;
    }
    
    /**
     * Gets category with spcified name, if exists.
     *@param name
     *@return category or null
     */
    protected Category getCategory(String name){
        return this.context.getLayerManager().getCategory(name);
    }
    
    /**
     * Moves the category with the given name to the given position
     * in the LayerNamePanel
     *@param name name of the category
     *@param pos desired position
     */
    public void moveCategoryToPosition(String name, int pos){
        this.moveCategoryToPosition(this.getCategory(name), pos);        
    }
    
    /**
     * Moves the given category to the given position
     * in the LayerNamePanel
     *@param cat category to move
     *@param pos desired position
     */
    public void moveCategoryToPosition(Category cat, int pos){
        LayerManager lm = this.context.getLayerManager();
        
        //boolean catIsVisible = SetCategoryVisibilityPlugIn.getInstance(this.context).isCategoryVisible(cat);
        
        lm.setFiringEvents(false);
        
        List categories = lm.getCategories();
        
        int currentPos = categories.indexOf(cat);
        
        if (pos < 0 || pos == currentPos) {
            lm.setFiringEvents(true);
            return;
        }
        
        // remove layer & category
        List layers = cat.getLayerables();
        
        Object[] layerArray = layers.toArray();
        
        for (int i=0; i<layerArray.length; i++){
            lm.remove((Layerable)layerArray[i]);
        }
        
        lm.setFiringEvents(true);
        lm.removeIfEmpty(cat);
        
        // rebuild category in its new place
        lm.addCategory(cat.getName(), Math.min(pos,categories.size()));
        
        //SetCategoryVisibilityPlugIn.getInstance(this.context).setCategoryVisibility(lm.getCategory(cat.getName()), catIsVisible );
        
        for (int i=0; i<layerArray.length; i++){
            lm.addLayerable(cat.getName(), (Layerable)layerArray[i]);
        }
    }

    
    /**
     * Moves the given category to the top position
     * in the LayerNamePanel
     *@param cat category to move
     */
    public void moveCategoryToTop(Category cat){
        if (this.context.getLayerManager().getCategories().size() < 2) return;
        this.moveCategoryToPosition(cat,0);
    }
    
    /**
     * Moves the given category to the bottom position
     * in the LayerNamePanel
     *@param cat category to move
     */
    public void moveCategoryToBottom(Category cat){
        List categories = this.context.getLayerManager().getCategories();
        if (categories.size() < 2) return;
        this.moveCategoryToPosition(cat,categories.size()-1);
    }
    
    /**
     * Moves the given category to the next higher position
     * in the LayerNamePanel
     *@param cat category to move
     */
    public void moveCategoryOneUp(Category cat){
        List categories = this.context.getLayerManager().getCategories();
        if (categories.size() < 2) return;
        int currentPos = categories.indexOf(cat);
        this.moveCategoryToPosition(cat, currentPos-1);        
    }
    
    /**
     * Moves the given category to the next lower position
     * in the LayerNamePanel
     *@param cat category to move
     */
    public void moveCategoryOneDown(Category cat){
        List categories = this.context.getLayerManager().getCategories();
        if (categories.size() < 2) return;
        int currentPos = categories.indexOf(cat);
        this.moveCategoryToPosition(cat, currentPos+1);        
    }
    
}
