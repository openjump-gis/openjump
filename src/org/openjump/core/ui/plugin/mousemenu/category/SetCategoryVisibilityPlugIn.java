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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;


import org.openjump.core.apitools.PlugInContextTools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.PopupNodeProxy;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * @author Ole Rahn
 * 
 * FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck
 * Project PIROL 2005
 * Daten- und Wissensmanagement
 * 
 */
public class SetCategoryVisibilityPlugIn extends AbstractPlugIn {
    
    protected Map layer2Visibility = new HashMap();
    protected PlugInContext context = null;
    
    protected JCheckBoxMenuItem menuItem = null;
    
    protected static SetCategoryVisibilityPlugIn instance= null;
    

    
    /**
     * Constructor needed to load PlugIn from classes, should NOT be used by any other
     * code --> use getInstance() method instead!!
     */
    public SetCategoryVisibilityPlugIn(){
        SetCategoryVisibilityPlugIn.instance = this;
    }
    
    public static SetCategoryVisibilityPlugIn getInstance(PlugInContext context){
        if (SetCategoryVisibilityPlugIn.instance == null){
            SetCategoryVisibilityPlugIn.instance = new SetCategoryVisibilityPlugIn();
            SetCategoryVisibilityPlugIn.instance.context = context;
        }
        
        return SetCategoryVisibilityPlugIn.instance;
    }

    public String getName() {
      return I18N
          .get("org.openjump.core.ui.plugin.mousemenu.category.SetCategoryVisibilityPlugIn.Set-Category-Visibility");
    }
    
    public void initialize(PlugInContext context) throws Exception {

        /// keep context for later
        this.context = context;
        
        JPopupMenu layerNamePopupMenu = context.getWorkbenchContext().getWorkbench().getFrame().getCategoryPopupMenu();
        FeatureInstaller featInst = context.getFeatureInstaller();
        
        this.menuItem = (JCheckBoxMenuItem) featInst.addPopupMenuPlugin(layerNamePopupMenu,
                this, this.getName() + "...", true,
                GUIUtil.toSmallIcon((ImageIcon) this.getIcon()),
                SetCategoryVisibilityPlugIn.createEnableCheck(context.getWorkbenchContext()));
        
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance();
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        
        multiEnableCheck.add( checkFactory.createAtLeastNCategoriesMustBeSelectedCheck(1) );

        // simple hook to switch menuitem states
        multiEnableCheck.add(new EnableCheck() {
          public String check(JComponent component) {
            SetCategoryVisibilityPlugIn.getInstance(workbenchContext.createPlugInContext()).updateMenuItem();
            return null;
          }
        });
        
        return multiEnableCheck;
	}
    
    public Icon getIcon() {
        return IconLoader.icon("eye.png");
    }
    
    public boolean execute(PlugInContext context) throws Exception {
        
        LayerNamePanel lnp = context.getLayerNamePanel();
        Collection selCats = lnp.getSelectedCategories();
        Iterator iter = selCats.iterator();

        Boolean visible = null;
        // get what to do from selected/clicked category
        Category cat_clicked = getClickedCategory();
        if (cat_clicked != null)
          visible = !isCategoryVisible(cat_clicked);
        Category cat;
        while(iter.hasNext()){
            cat = (Category)iter.next();
            // if still not set, first selected defines visible state to inverse
            // NOTE: getSelectedCategories() does not necessarily give 
            //       the last clicked category as first item
            if (visible==null)
              visible = !isCategoryVisible(cat);
            // set visibility only if necessary
            if (isCategoryVisible(cat)!=visible)
              this.setLayersVisibility( cat.getLayerables(), visible );
        }
        
        return true;
    }
    
    private Category getClickedCategory() {
      // refresh context
      PlugInContext context = PlugInContextTools.getContext(this.context);
      LayerNamePanel lnp = context.getLayerNamePanel();
      Collection selCats = lnp.getSelectedCategories();
      Iterator iter = selCats.iterator();

      // use clicked category or first selected
      if (lnp instanceof PopupNodeProxy) {
        Object o = ((PopupNodeProxy)lnp).getPopupNode();
        if (o instanceof Category)
          return (Category)o;
      }
      
      return selCats.isEmpty() ? null : (Category)selCats.iterator().next();
    }
    
    private boolean isCategoryVisible(Category cat) {
      // visible layers reset status to visible
      boolean lvisible = false;
      for (Layerable l : (List<Layerable>) cat.getLayerables()) {
        if ( l.isVisible() ) {
          lvisible = true;
          break;
        }
      }
  
      // cat is visible as soon as one layer is visible
      return lvisible;
    }

    private void setLayersVisibility(List layers, boolean visible) {
      Iterator iter = layers.iterator();
      // iterate over layers to switch on/off
      Layerable layer;
      boolean changed = false;
      while (iter.hasNext()) {
        layer = (Layerable) iter.next();
        
        // switch all layers off
        if (!visible) {
          // always save former state when switching off
          this.layer2Visibility.put(layer, new Boolean(layer.isVisible()));
          layer.setVisible(false);
        }
        // but restore only remembered layers that were visible before
        else {
          if ( this.layer2Visibility.containsKey(layer) ) {
            boolean lvisible = ((Boolean) this.layer2Visibility.get(layer)).booleanValue() ;
            if (lvisible){
              layer.setVisible(lvisible);
              changed = true;
            }
            // forget state
            this.layer2Visibility.remove(layer);
          }
        }
      }
      // ok, we might have a rare case of no remembered layers, so let's enable all then
      if ( visible && !changed ){
        for (Layerable l : (List<Layerable>)layers) {
          l.setVisible(true);
        }
      }

    }
    
    public void updateMenuItem() {
      // get what to do from selected/clicked category
      Category cat_clicked = getClickedCategory();
      if (cat_clicked == null) return;
        
      boolean visible = isCategoryVisible(cat_clicked);
      this.menuItem.setSelected( visible );
    }

}
