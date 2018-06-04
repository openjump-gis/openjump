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

import static com.vividsolutions.jump.I18N.get;
import static com.vividsolutions.jump.I18N.getMessage;

import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImagesLayer;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerableNamePanel;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.TaskFrameProxy;
import com.vividsolutions.jump.workbench.ui.warp.WarpingVectorLayerFinder;

/**
 * Creates basic EnableChecks.
 * 
 * @see EnableCheck
 */
public class EnableCheckFactory {

    private final WorkbenchContext workbenchContext;
    private static EnableCheckFactory instance = null;

    public EnableCheckFactory(WorkbenchContext workbenchContext) {
        Assert.isTrue(workbenchContext != null);
        this.workbenchContext = workbenchContext;
    }

    public static EnableCheckFactory getInstance() {
        if (instance == null) {
            instance = new EnableCheckFactory(JUMPWorkbench.getInstance()
                    .getContext());
        }
        return instance;
    }

    // <<TODO:WORKAROUND>> I came across a bug in the JBuilder 4 compiler
    // (bcj.exe)
    // that occurs when using the Java ternary operator ( ? : ). For it to
    // happen, [1] the middle operand must be null and [2] an inner class
    // must be nearby. For example, try using JBuilder to compile the following
    // code:
    //
    // import java.awt.event.WindowAdapter;
    // public class TestClass {
    // static public void main(String[] args) {
    // System.out.println(true ? null : "FALSE");
    // WindowAdapter w = new WindowAdapter() { };
    // }
    // }
    //
    // You'd expect it to print out "null", but "FALSE" is printed! And if you
    // comment
    // out the line with the anonymous inner class (WindowAdapter w), it prints
    // out
    // "null" as expected! I've submitted a bug report to Borland (case number
    // 488569).
    //
    // So, if you're using JBuilder, don't use ?: if the middle operand could be
    // null!
    // [Jon Aquino]
    public EnableCheck createTaskWindowMustBeActiveCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                return (!(workbenchContext.getWorkbench().getFrame()
                        .getActiveInternalFrame() instanceof TaskFrame)) ? get("com.vividsolutions.jump.workbench.plugin.A-Task-Window-must-be-active")
                        : null;
            }
        };
    }

    public EnableCheck createWindowWithSelectionManagerMustBeActiveCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                return (!(workbenchContext.getWorkbench().getFrame()
                        .getActiveInternalFrame() instanceof SelectionManagerProxy)) ? get("com.vividsolutions.jump.workbench.plugin.A-window-with-a-selection-manager-must-be-active")
                        : null;
            }
        };
    }

    public EnableCheck createWindowWithLayerManagerMustBeActiveCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                return (!(workbenchContext.getWorkbench().getFrame()
                        .getActiveInternalFrame() instanceof LayerManagerProxy)) ? get("com.vividsolutions.jump.workbench.plugin.A-window-with-a-layer-manager-must-be-active")
                        : null;
            }
        };
    }

    public EnableCheck createWindowWithAssociatedTaskFrameMustBeActiveCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                return (!(workbenchContext.getWorkbench().getFrame()
                        .getActiveInternalFrame() instanceof TaskFrameProxy)) ? get("com.vividsolutions.jump.workbench.plugin.A-window-with-an-associated-task-frame-must-be-active")
                        : null;
            }
        };
    }

    public EnableCheck createWindowWithLayerNamePanelMustBeActiveCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                return (!(workbenchContext.getWorkbench().getFrame()
                        .getActiveInternalFrame() instanceof LayerNamePanelProxy)) ? get("com.vividsolutions.jump.workbench.plugin.A-window-with-a-layer-name-panel-must-be-active")
                        : null;
            }
        };
    }

    public EnableCheck createWindowWithLayerViewPanelMustBeActiveCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                return (!(workbenchContext.getWorkbench().getFrame()
                        .getActiveInternalFrame() instanceof LayerViewPanelProxy)) ? get("com.vividsolutions.jump.workbench.plugin.A-window-with-a-layer-view-panel-must-be-active")
                        : null;
            }
        };
    }

    public EnableCheck createOnlyOneLayerMayHaveSelectedFeaturesCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final Collection layersWithSelectedFeatures = ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager().getFeatureSelection()
                        .getLayersWithSelectedItems();

                return (layersWithSelectedFeatures.size() > 1) ? get("com.vividsolutions.jump.workbench.plugin.Only-one-layer-may-have-selected-features")
                        : null;
            }
        };
    }

    public EnableCheck createOnlyOneLayerMayHaveSelectedItemsCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final Collection layersWithSelectedItems = ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager().getLayersWithSelectedItems();
                return (layersWithSelectedItems.size() > 1) ? get("com.vividsolutions.jump.workbench.plugin.Only-one-layer-may-have-selected-items")
                        : null;
            }
        };
    }

    public EnableCheck createSelectedItemsLayersMustBeEditableCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                for (final Layer layer : ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager().getLayersWithSelectedItems()) {
                    if (!layer.isEditable()) {
                        return getMessage(
                                "com.vividsolutions.jump.workbench.plugin.Selected-items-layers-must-be-editable",
                                layer.getName());
                    }
                }

                return null;
            }
        };
    }

    public EnableCheck createExactlyNCategoriesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-category-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.Exactly-n-categories-must-be-selected",
                            n);
                }
                return (n != workbenchContext.getLayerableNamePanel()
                        .getSelectedCategories().size()) ? msg : null;
            }
        };
    }

    public EnableCheck createExactlyNLayerablesMustBeSelectedCheck(final int n,
            final Class layerableClass) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final LayerNamePanel lnp = workbenchContext
                        .getLayerableNamePanel();
                if (lnp instanceof LayerNamePanel
                        && n == lnp.selectedNodes(layerableClass).size()) {
                    return null;
                }

                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.Exactly-n-layers-must-be-selected",
                            n);
                }

                return msg;
            }
        };
    }

    public EnableCheck createExactlyNLayersMustBeSelectedCheck(final int n) {
        return createExactlyNLayerablesMustBeSelectedCheck(n, Layer.class);
    }

    public EnableCheck createAtLeastNCategoriesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-category-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-categories-must-be-selected",
                            n);
                }
                return (n > workbenchContext.getLayerableNamePanel()
                        .getSelectedCategories().size()) ? msg : null;
            }
        };
    }

    public EnableCheck createAtLeastNLayerablesMustBeSelectedCheck(final int n,
            final Class layerableClass) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final LayerNamePanel layerNamePanel = workbenchContext
                        .getLayerableNamePanel();
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-layer-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-layers-must-be-selected",
                            n);
                }
                return (layerNamePanel == null || n > (workbenchContext
                        .getLayerableNamePanel()).selectedNodes(layerableClass)
                        .size()) ? msg : null;
            }
        };
    }

    public EnableCheck createAtLeastNLayersMustBeSelectedCheck(final int n) {
        return createAtLeastNLayerablesMustBeSelectedCheck(n, Layer.class);
    }

    public EnableCheck createAtLeastNLayersMustBeEditableCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-layer-must-be-editable");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-layers-must-be-editable",
                            n);
                }
                return (n > workbenchContext.getLayerManager()
                        .getEditableLayers().size()) ? msg : null;
            }
        };
    }

    public EnableCheck createAtLeastOneVisibleLayersMustBeEditableCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                for (final Layer layer : workbenchContext.getLayerManager()
                        .getLayers()) {
                    if (layer.isVisible() && layer.isEditable()) {
                        return null;
                    }
                }
                return get("plugin.EnableCheckFactory.at-least-one-visible-layer-must-be-editable");
            }
        };
    }

    public EnableCheck createExactlyOneSelectedLayerMustBeEditableCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final String msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-selected-layer-must-be-editable");
                final Layer[] layers = workbenchContext.getLayerableNamePanel()
                        .getSelectedLayers();
                int countSelectedEditable = 0;
                for (final Layer layer : layers) {
                    if (layer.isEditable()) {
                        countSelectedEditable++;
                    }
                }
                return 1 != countSelectedEditable ? msg : null;
            }
        };
    }

    public EnableCheck createAtLeastNLayerablesMustExistCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final LayerManager layerManager = workbenchContext
                        .getLayerManager();
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-layerables-must-exist");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-layerables-must-exist",
                            n);
                }
                return (layerManager == null || n > layerManager.getLayerables(
                        Layerable.class).size()) ? msg : null;
            }
        };
    }

    public EnableCheck createAtLeastNLayersMustExistCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final LayerManager layerManager = workbenchContext
                        .getLayerManager();
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-layer-must-exist");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-layers-must-exist",
                            n);
                }
                return (layerManager == null || n > layerManager.size()) ? msg
                        : null;
            }
        };
    }

    public EnableCheck createAtMostNLayersMustExistCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-most-one-layer-must-exist");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-most-n-layers-must-exist",
                            n);
                }
                return (n < workbenchContext.getLayerManager().size()) ? msg
                        : null;
            }
        };
    }

    public EnableCheck createExactlyNVectorsMustBeDrawnCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-vector-must-be-drawn");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.Exactly-n-vectors-must-be-drawn",
                            n);
                }
                return (n != vectorCount()) ? msg : null;
            }
        };
    }

    // <<TODO:REFACTORING>> I wonder if we can refactor some of these methods
    // [Jon Aquino]
    public EnableCheck createAtLeastNVectorsMustBeDrawnCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-vector-must-be-drawn");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-vectors-must-be-drawn",
                            n);
                }
                return (n > vectorCount()) ? msg : null;
            }
        };
    }

    public EnableCheck createAtLeastNFeaturesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-feature-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-features-must-be-selected",
                            n);
                }

                final JInternalFrame f = workbenchContext.getWorkbench()
                        .getFrame().getActiveInternalFrame();

                return (f != null && f instanceof SelectionManagerProxy && n > ((SelectionManagerProxy) f)
                        .getSelectionManager()
                        .getFeaturesWithSelectedItemsCount()) ? msg : null;
            }
        };
    }

    public EnableCheck createAtLeastNItemsMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final JInternalFrame iFrame = workbenchContext.getWorkbench()
                        .getFrame().getActiveInternalFrame();
                int selected = 0;
                try {
                    // Modified bu sstein [13. Aug. 2006], [13. Mar. 2008]
                    // mmichaud [11. Dec. 2011]
                    // It should now works homogeneously for ViewPnale,
                    // ViewAttributes and InfoPanel
                    selected = ((SelectionManagerProxy) iFrame)
                            .getSelectionManager().getSelectedItems().size();
                } catch (final Exception e) {
                    // -- sstein:
                    // == eat exception ==
                    System.out
                            .println("eat exception @ EnableCheckFactory.createAtLeastNItemsMustBeSelectedCheck(i) if a non taskframe(or child) is selected");
                    // necessary if iFrame is OutputFrame or something
                    // and i dont know how to test for alle iFrames which exist
                    // or rather i do not know
                    // which are the ones accessible to the SelectionManager
                }
                String retVal;
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-item-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-items-must-be-selected",
                            n);
                }
                if ((iFrame == null) || (n > selected)) {
                    retVal = msg;
                } else {
                    retVal = null;
                }
                return retVal;
            }
        };
    }

    public EnableCheck createExactlyNFeaturesMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-feature-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.Exactly-n-features-must-be-selected",
                            n);
                }
                return (n != ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager()
                        .getFeaturesWithSelectedItemsCount()) ? msg : null;
            }
        };
    }

    public EnableCheck createExactlyNItemsMustBeSelectedCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-item-must-be-selected");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.Exactly-n-items-must-be-selected",
                            n);
                }
                return (n != ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager().getSelectedItemsCount()) ? msg
                        : null;
            }
        };
    }

    public EnableCheck createExactlyNLayersMustHaveSelectedItemsCheck(
            final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-have-selected-items");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.Exactly-n-layers-must-have-selected-items",
                            n);
                }
                return (n != ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager().getLayersWithSelectedItems()
                        .size()) ? msg : null;
            }
        };
    }

    public EnableCheck createExactlyNFeaturesMustHaveSelectedItemsCheck(
            final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.Exactly-one-feature-must-have-selected-items");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.Exactly-n-features-must-have-selected-items",
                            n);
                }
                return (n != ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager()
                        .getFeaturesWithSelectedItemsCount()) ? msg : null;
            }
        };
    }

    public EnableCheck createSelectedLayersMustBeEditableCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                for (final Layer layer : workbenchContext
                        .getLayerableNamePanel().getSelectedLayers()) {
                    if (!layer.isEditable()) {
                        return getMessage(
                                "com.vividsolutions.jump.workbench.plugin.Selected-layers-must-be-editable",
                                layer.getName());
                    }
                }
                return null;
            }
        };
    }

    public EnableCheck createFenceMustBeDrawnCheck() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final LayerViewPanel layerViewPanel = workbenchContext
                        .getLayerViewPanel();
                return (layerViewPanel == null || // [UT] 20.10.2005 not quite
                                                  // the error mesg
                null == layerViewPanel.getFence()) ? get("com.vividsolutions.jump.workbench.plugin.A-fence-must-be-drawn")
                        : null;
            }
        };
    }

    public EnableCheck createBetweenNAndMVectorsMustBeDrawnCheck(final int min,
            final int max) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                return ((vectorCount() > max) || (vectorCount() < min)) ? getMessage(
                        "com.vividsolutions.jump.workbench.plugin.Between-and-vectors-must-be-drawn",
                        min, max)
                        : null;
            }
        };
    }

    int vectorCount() {
        return new WarpingVectorLayerFinder(workbenchContext).getVectors()
                .size();
    }

    public EnableCheck createAtLeastNFeaturesMustHaveSelectedItemsCheck(
            final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-feature-must-have-selected-items");
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-features-must-have-selected-items",
                            n);
                }
                return (n > ((SelectionManagerProxy) workbenchContext
                        .getWorkbench().getFrame().getActiveInternalFrame())
                        .getSelectionManager()
                        .getFeaturesWithSelectedItemsCount()) ? msg : null;
            }
        };
    }

    /**
     * Check the current selection in layernamepanel against 2 lists of
     * layerable classes. Returns an error message if at least one layerable is
     * not an instance of the first class array or if at least one layerable is
     * an instance of the second class array.
     * 
     * @param classes
     *            layerables must all be instances of one of these classes
     * @param excluded
     *            no layerable must be an instances of one of these classes
     * @return error message or null
     */
    public EnableCheck createSelectedLayerablesMustBeEither(
            final Class[] classes, final Class[] excluded) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final StringBuilder types = new StringBuilder("[");
                for (final Class clz : classes) {
                    final String clzName = get(clz.getCanonicalName());
                    types.append(types.length() > 1 ? ", " + clzName : clzName);
                }
                types.append("]");
                final StringBuilder exclusion = new StringBuilder("[");
                for (final Class clz : excluded) {
                    final String clzName = get(clz.getCanonicalName());
                    exclusion.append(exclusion.length() > 1 ? ", " + clzName
                            : clzName);
                }
                exclusion.append("]");

                final String msg = getMessage(
                        "plugin.EnableCheckFactory.selected-layers-must-be-of-type",
                        types, exclusion);

                // fetch layer(ables)
                final LayerNamePanel lnp = workbenchContext.getLayerNamePanel();
                Layerable[] layerables;
                if (lnp instanceof LayerableNamePanel) {
                    layerables = ((LayerableNamePanel) lnp)
                            .getSelectedLayerables().toArray(new Layerable[0]);
                } else {
                    layerables = lnp.getSelectedLayers();
                }

                for (final Layerable layerable : layerables) {
                    boolean ok = false;
                    for (final Class clz : classes) {
                        if (clz.isAssignableFrom(layerable.getClass())) {
                            ok = true;
                            for (final Class exc : excluded) {
                                if (exc.isAssignableFrom(layerable.getClass())) {
                                    ok = false;
                                    break;
                                }
                            }
                        }
                        if (ok) {
                            break;
                        }
                    }
                    if (!ok) {
                        return msg;
                    }
                }
                return null;
            }
        };
    }

    public EnableCheck createSelectedLayerablesMustBeEither(
            final Class[] classes) {
        return createSelectedLayerablesMustBeEither(
                new Class[] { Layer.class }, new Class[0]);
    }

    /**
     * checks if selected layerables are vector layers. unfortunately
     * {@link ReferencedImagesLayer}s are derived from OJ vector {@link Layer}.
     * hence this method internally includes {@link Layer} but excludes
     * {@link ReferencedImagesLayer}.
     * 
     * @see EnableCheckFactory#createSelectedLayerablesMustBeEither(Class[],
     *      Class[])
     * @return error message or null
     */
    public EnableCheck createSelectedLayerablesMustBeVectorLayers() {
        return createSelectedLayerablesMustBeEither(
                new Class[] { Layer.class },
                new Class[] { ReferencedImagesLayer.class });
    }

    public EnableCheck createSelectedLayerablesMustBeWMSLayers() {
        return createSelectedLayerablesMustBeEither(new Class[] { WMSLayer.class });
    }

    public EnableCheck createSelectedLayerablesMustBeRasterImageLayers() {
        return createSelectedLayerablesMustBeEither(new Class[] { RasterImageLayer.class });
    }

    public EnableCheck createSelectedLayerablesMustBeReferencedImagesLayers() {
        return createSelectedLayerablesMustBeEither(new Class[] { ReferencedImagesLayer.class });
    }

    /**
     * Giuseppe Aruta - 2015-01-13 RasterImageLayer.class checks how many bands
     * a Raster Image Layer (Sextante) has
     */
    public EnableCheck createRasterImageLayerExactlyNBandsMustExistCheck(
            final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {

                final RasterImageLayer rLayer = (RasterImageLayer) LayerTools
                        .getSelectedLayerable(workbenchContext.getWorkbench()
                                .getContext(), RasterImageLayer.class);

                final int numbands = rLayer.getNumBands();

                String msg;
                if (n == 1) {
                    msg = get("plugin.EnableCheckFactory.exactly-1-band-must-exist-on-selected-raster-layer");
                } else {
                    msg = getMessage(
                            "plugin.EnableCheckFactory.exactly-{0}-bands-must-exist-on-selected-raster-layer",
                            n);
                }
                return (n != numbands) ? msg : null;
            }
        };
    }

    /**
     * Check that at least n RasterImageLayer.class layers have been loaded
     * 
     * @param number
     *            of RasterImageLayer.class layers
     * @return error message or null
     */
    public EnableCheck createAtLeastNRasterImageLayersMustExistCheck(final int n) {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                final LayerManager layerManager = workbenchContext
                        .getLayerManager();
                String msg;
                if (n == 1) {
                    msg = get("com.vividsolutions.jump.workbench.plugin.At-least-one-layerables-must-exist")
                            + " ("
                            + get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Sextante-Raster-Image")
                            + ")";
                } else {
                    msg = getMessage(
                            "com.vividsolutions.jump.workbench.plugin.At-least-n-layerables-must-exist",
                            new Object[] { n })
                            + " ("
                            + get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Sextante-Raster-Image")
                            + ")";
                    ;
                }
                return (layerManager == null || n > layerManager
                        .getRasterImageLayers().size()) ? msg : null;
            }
        };
    }

}
