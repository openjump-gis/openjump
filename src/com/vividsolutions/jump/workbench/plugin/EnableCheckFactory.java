
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

package com.vividsolutions.jump.workbench.plugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewFrame;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.TaskFrameProxy;
import com.vividsolutions.jump.workbench.ui.warp.WarpingVectorLayerFinder;

/**
 * Creates basic EnableChecks.
 * @see EnableCheck
 */
public class EnableCheckFactory {
    private WorkbenchContext workbenchContext;

    public EnableCheckFactory(WorkbenchContext workbenchContext) {
        Assert.isTrue(workbenchContext != null);
        this.workbenchContext = workbenchContext;
    }

    //<<TODO:WORKAROUND>> I came across a bug in the JBuilder 4 compiler (bcj.exe)
    //that occurs when using the Java ternary operator ( ? : ). For it to
    //happen, [1] the middle operand must be null and [2] an inner class
    //must be nearby. For example, try using JBuilder to compile the following code:
    //
    //  import java.awt.event.WindowAdapter;
    //  public class TestClass {
    //    static public void main(String[] args) {
    //      System.out.println(true ? null : "FALSE");
    //      WindowAdapter w = new WindowAdapter() { };
    //    }
    //  }
    //
    //You'd expect it to print out "null", but "FALSE" is printed! And if you comment
    //out the line with the anonymous inner class (WindowAdapter w), it prints out
    //"null" as expected! I've submitted a bug report to Borland (case number 488569).
    //
    //So, if you're using JBuilder, don't use ?: if the middle operand could be null!
    //[Jon Aquino]
    public EnableCheck createTaskWindowMustBeActiveCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    !(workbenchContext.getWorkbench().getFrame().getActiveInternalFrame()
                        instanceof TaskFrame))
                    ? "A Task Window must be active"
                    : null;
            }
        };
    }

    public EnableCheck createWindowWithSelectionManagerMustBeActiveCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    !(workbenchContext.getWorkbench().getFrame().getActiveInternalFrame()
                        instanceof SelectionManagerProxy))
                    ? "A window with a selection manager must be active"
                    : null;
            }
        };
    }

    public EnableCheck createWindowWithLayerManagerMustBeActiveCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    !(workbenchContext.getWorkbench().getFrame().getActiveInternalFrame()
                        instanceof LayerManagerProxy))
                    ? "A window with a layer manager must be active"
                    : null;
            }
        };
    }

    public EnableCheck createWindowWithAssociatedTaskFrameMustBeActiveCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    !(workbenchContext.getWorkbench().getFrame().getActiveInternalFrame()
                        instanceof TaskFrameProxy))
                    ? "A window with an associated task frame must be active"
                    : null;
            }
        };
    }

    public EnableCheck createWindowWithLayerNamePanelMustBeActiveCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    !(workbenchContext.getWorkbench().getFrame().getActiveInternalFrame()
                        instanceof LayerNamePanelProxy))
                    ? "A window with a layer-name panel must be active"
                    : null;
            }
        };
    }

    public EnableCheck createWindowWithLayerViewPanelMustBeActiveCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    !(workbenchContext.getWorkbench().getFrame().getActiveInternalFrame()
                        instanceof LayerViewPanelProxy))
                    ? "A window with a layer-view panel must be active"
                    : null;
            }
        };
    }

    public EnableCheck createOnlyOneLayerMayHaveSelectedFeaturesCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                Collection layersWithSelectedFeatures =
                    ((SelectionManagerProxy) workbenchContext
                        .getWorkbench()
                        .getFrame()
                        .getActiveInternalFrame())
                        .getSelectionManager()
                        .getFeatureSelection()
                        .getLayersWithSelectedItems();

                return (layersWithSelectedFeatures.size() > 1)
                    ? "Only one layer may have selected features"
                    : null;
            }
        };
    }

    public EnableCheck createOnlyOneLayerMayHaveSelectedItemsCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                Collection layersWithSelectedItems =
                    ((SelectionManagerProxy) workbenchContext
                        .getWorkbench()
                        .getFrame()
                        .getActiveInternalFrame())
                        .getSelectionManager()
                        .getLayersWithSelectedItems();
                return (layersWithSelectedItems.size() > 1)
                    ? "Only one layer may have selected items"
                    : null;
            }
        };
    }

    public EnableCheck createSelectedItemsLayersMustBeEditableCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                for (Iterator i =
                    ((SelectionManagerProxy) workbenchContext
                        .getWorkbench()
                        .getFrame()
                        .getActiveInternalFrame())
                        .getSelectionManager()
                        .getLayersWithSelectedItems()
                        .iterator();
                    i.hasNext();
                    ) {
                    Layer layer = (Layer) i.next();

                    if (!layer.isEditable()) {
                        return "Selected items' layers must be editable (" + layer.getName() + ")";
                    }
                }

                return null;
            }
        };
    }

    public EnableCheck createExactlyNCategoriesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (n != workbenchContext.getLayerNamePanel().getSelectedCategories().size())
                    ? ("Exactly " + n + " categor" + StringUtil.ies(n) + " must be selected")
                    : null;
            }
        };
    }

    public EnableCheck createExactlyNLayerablesMustBeSelectedCheck(
        final int n,
        final Class layerableClass) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    n
                        != (workbenchContext.getLayerNamePanel())
                            .selectedNodes(layerableClass)
                            .size())
                    ? ("Exactly " + n + " layer" + StringUtil.s(n) + " must be selected")
                    : null;
            }
        };
    }

    public EnableCheck createExactlyNLayersMustBeSelectedCheck(final int n) {
        return createExactlyNLayerablesMustBeSelectedCheck(n, Layer.class);
    }

    public EnableCheck createAtLeastNCategoriesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (n > workbenchContext.getLayerNamePanel().getSelectedCategories().size())
                    ? ("At least " + n + " categor" + StringUtil.ies(n) + " must be selected")
                    : null;
            }
        };
    }

    public EnableCheck createAtLeastNLayerablesMustBeSelectedCheck(
        final int n,
        final Class layerableClass) {
        return new EnableCheck() {
            public String check(JComponent component) {
                LayerNamePanel layerNamePanel = workbenchContext.getLayerNamePanel();
                return (
                                layerNamePanel == null || 
                    n
                        > (workbenchContext.getLayerNamePanel())
                            .selectedNodes(layerableClass)
                            .size())
                    ? ("At least " + n + " layer" + StringUtil.s(n) + " must be selected")
                    : null;
            }
        };
    }

    public EnableCheck createAtLeastNLayersMustBeSelectedCheck(final int n) {
        return createAtLeastNLayerablesMustBeSelectedCheck(n, Layer.class);
    }

    public EnableCheck createAtLeastNLayersMustBeEditableCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (n > workbenchContext.getLayerManager().getEditableLayers().size())
                    ? ("At least " + n + " layer" + StringUtil.s(n) + " must be editable")
                    : null;
            }
        };
    }

    public EnableCheck createAtLeastNLayersMustExistCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                LayerManager layerManager = workbenchContext.getLayerManager();
                return (
                                layerManager == null ||
                                n > layerManager.size())
                    ? ("At least " + n + " layer" + StringUtil.s(n) + " must exist")
                    : null;
            }
        };
    }

    public EnableCheck createAtMostNLayersMustExistCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (n < workbenchContext.getLayerManager().size())
                    ? ("At most " + n + " layer" + StringUtil.s(n) + " must exist")
                    : null;
            }
        };
    }

    public EnableCheck createExactlyNVectorsMustBeDrawnCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (n != vectorCount())
                    ? ("Exactly " + n + " vector" + StringUtil.s(n) + " must be drawn")
                    : null;
            }
        };
    }

    //<<TODO:REFACTORING>> I wonder if we can refactor some of these methods [Jon Aquino]
    public EnableCheck createAtLeastNVectorsMustBeDrawnCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (n > vectorCount())
                    ? ("At least " + n + " vector" + StringUtil.s(n) + " must be drawn")
                    : null;
            }
        };
    }

    public EnableCheck createAtLeastNFeaturesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    n
                        > ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getFeatureSelection()
                            .getFeaturesWithSelectedItems()
                            .size())
                    ? ("At least " + n + " feature" + StringUtil.s(n) + " must be selected")
                    : null;
            }
        };
    }

    public EnableCheck createAtLeastNItemsMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                JInternalFrame iFrame = workbenchContext.getWorkbench()
                	.getFrame().getActiveInternalFrame();
                int selected = 0;
                try{//sstein [13. Aug. 2006]: 
                	selected = ((SelectionManagerProxy)iFrame).getSelectionManager().getSelectedItems().size();
                }
                catch(Exception e){
                	//-- sstein:
                	//== eat exception ==
                	System.out.println("eat exception @ EnableCheckFactory.createAtLeastNItemsMustBeSelectedCheck(i) if a non taskframe(or child) is selected");                	
                	//necessary if iFrame is OutputFrame or something 
                	//and i dont know how to test for alle iFrames which exist or rather i do not know
                	//which are the ones accessible to the SelectionManager
                }
                String retVal = null;
                if ((iFrame == null)  || (n > selected)){
                	retVal = ("At least " + n + " item" + StringUtil.s(n) + " must be selected");
                }
                else{
                	retVal = null;
                }
                return retVal;
            }
        };
    }

    public EnableCheck createExactlyNFeaturesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    n
                        != ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getFeatureSelection()
                            .getFeaturesWithSelectedItems()
                            .size())
                    ? ("Exactly " + n + " feature" + StringUtil.s(n) + " must be selected")
                    : null;
            }
        };
    }

    public EnableCheck createExactlyNItemsMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    n
                        != ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getSelectedItems()
                            .size())
                    ? ("Exactly " + n + " item" + StringUtil.s(n) + " must be selected")
                    : null;
            }
        };
    }

    public EnableCheck createExactlyNLayersMustHaveSelectedItemsCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    n
                        != ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getLayersWithSelectedItems()
                            .size())
                    ? ("Exactly " + n + " layer" + StringUtil.s(n) + " must have selected items")
                    : null;
            }
        };
    }
    
    public EnableCheck createExactlyNFeaturesMustHaveSelectedItemsCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    n
                        != ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getFeaturesWithSelectedItems()
                            .size())
                    ? ("Exactly " + n + " feature" + StringUtil.s(n) + " must have selected items")
                    : null;
            }
        };
    }    

    public EnableCheck createSelectedLayersMustBeEditableCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                for (Iterator i = Arrays.asList(workbenchContext.getLayerNamePanel().getSelectedLayers()).iterator(); i.hasNext(); ) {
                    Layer layer = (Layer) i.next();
                    if (!layer.isEditable()) { 
                        return "Selected layers must be editable (" + layer.getName() + ")";
                    }
                }
                return null;
            }
        };
    }

    public EnableCheck createFenceMustBeDrawnCheck() {
        return new EnableCheck() {
            public String check(JComponent component) {
                LayerViewPanel layerViewPanel = workbenchContext.getLayerViewPanel();  
                return ( layerViewPanel == null || //[UT] 20.10.2005 not quite the error mesg
                                null == layerViewPanel.getFence())
                    ? "A fence must be drawn"
                    : null;
            }
        };
    }

    public EnableCheck createBetweenNAndMVectorsMustBeDrawnCheck(final int min, final int max) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return ((vectorCount() > max) || (vectorCount() < min))
                    ? ("Between " + min + " and " + max + " vectors must be drawn")
                    : null;
            }
        };
    }

    private int vectorCount() {
        return new WarpingVectorLayerFinder(workbenchContext).getVectors().size();
    }


    
    public EnableCheck createAtLeastNFeaturesMustHaveSelectedItemsCheck(final int n) {
        return new EnableCheck() {
            public String check(JComponent component) {
                return (
                    n
                        > ((SelectionManagerProxy) workbenchContext
                            .getWorkbench()
                            .getFrame()
                            .getActiveInternalFrame())
                            .getSelectionManager()
                            .getFeaturesWithSelectedItems()
                            .size())
                    ? ("At least " + n + " feature" + StringUtil.s(n) + " must have selected items")
                    : null;
            }
        };
    }    
}
