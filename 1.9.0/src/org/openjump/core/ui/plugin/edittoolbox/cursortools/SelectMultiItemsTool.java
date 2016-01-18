package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.AbstractSelection;
import com.vividsolutions.jump.workbench.ui.FeatureSelection;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SelectTool;
import com.vividsolutions.jump.workbench.ui.cursortool.ShortcutsDescriptor;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.FeatureSelectionRenderer;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.NoninvertibleTransformException;
import java.util.*;

/**
 * A select tool opening a popup dialog to choose which feature to be selected when several
 * superimposed features are located under the mouse.
 */
public class SelectMultiItemsTool extends SelectTool implements ShortcutsDescriptor {

    final static String sSelectMultiItems = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectMultiItemsTool");

    private AbstractSelection selection;
    private Coordinate coordinate;
    private LayerViewPanel layerViewPanel;
    int x, y;

    public SelectMultiItemsTool() {
        super(FeatureSelectionRenderer.CONTENT_ID);
    }

    public Icon getIcon() {
        return IconLoader.icon("SelectMulti.png");
    }

    public String getName() {
        return sSelectMultiItems;
    }

    /**
     * @return true if this CursorTool uses the right mouse button; false
     * to allow the panel to show a popup-menu on right-clicks
     */
    public boolean isRightMouseButtonUsed() {
        return true;
    }

    public void activate(LayerViewPanel layerViewPanel) {
        this.layerViewPanel = layerViewPanel;
        super.activate(layerViewPanel);
        selection = layerViewPanel.getSelectionManager().getFeatureSelection();
    }


    protected void gestureFinished() throws NoninvertibleTransformException {
        reportNothingToUndoYet();

        if (!wasShiftPressed()) {
            getPanel().getSelectionManager().clear();
        }

        // Map layer to collections of features under the mouse
        final Map layerToFeaturesInFenceMap =
                getPanel().visibleLayerToFeaturesInFenceMap(
                        EnvelopeUtil.toGeometry(getBoxInModelCoordinates()));
        // Map features to their state (selected or not)
        final Map<Feature,Boolean> map = new HashMap<Feature, Boolean>();

        int count = 0;
        for (Map.Entry entry : (Set<Map.Entry>)layerToFeaturesInFenceMap.entrySet()) {
            Layer lyr = (Layer)entry.getKey();
            Collection collection = (Collection)entry.getValue();
            count += collection.size();
            for (Object obj : collection) {
                map.put((Feature)obj, selection.getFeaturesWithSelectedItems().contains(obj));
            }
            selection.unselectItems(lyr, collection);
        }
        if (count == 1) {
            Map.Entry entry = (Map.Entry)layerToFeaturesInFenceMap.entrySet().iterator().next();
            selection.selectItems((Layer) entry.getKey(),
                    (Feature) ((Collection) entry.getValue()).iterator().next());
        } else if (count > 40) {
            JOptionPane.showMessageDialog(getWorkbenchFrame(),
                    I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectMultiItemsTool.Too-many-features"),
                    I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectMultiItemsTool.Message"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final SelectionManager selectionManager = layerViewPanel.getSelectionManager();
        final FeatureSelection featureSelection = selectionManager.getFeatureSelection();

        final JPopupMenu popupMenu = new JPopupMenu();

        final JCheckBoxMenuItem jcbAll = new JCheckBoxMenuItem(
                "<html><b>" +
                I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectMultiItemsTool.All") +
                "</b></html>");
        jcbAll.setUI(new StayOpenCheckBoxMenuItemUI());
        jcbAll.setSelected(false);
        addAll(popupMenu, jcbAll, featureSelection, layerToFeaturesInFenceMap);
        popupMenu.add(jcbAll);

        for (Iterator i = layerToFeaturesInFenceMap.keySet().iterator(); i.hasNext();) {
            final Layer layer = (Layer) i.next();

            for (Iterator j = ((Collection)layerToFeaturesInFenceMap.get(layer)).iterator(); j.hasNext();) {
                final Feature feature = (Feature) j.next();
                final int fid = feature.getID();
                String label = layer.getName()+" (" + fid + ")";
                if (layer.isSelectable()) {
                    final JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(label);
                    jcb.setSelected(map.get(feature));
                    jcb.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            boolean selected = jcb.isSelected();
                            if (!selected) {
                                featureSelection.unselectItems(layer, feature);
                                layerViewPanel.fireSelectionChanged();
                                layerViewPanel.getSelectionManager().updatePanel();
                            } else {
                                featureSelection.selectItems(layer, feature);
                                layerViewPanel.fireSelectionChanged();
                                layerViewPanel.getSelectionManager().updatePanel();
                            }
                        }
                    });
                    jcb.setUI(new StayOpenCheckBoxMenuItemUI());
                    popupMenu.add(jcb);
                } else {
                    label = "<html><i>" + label + "</i></html>";
                    final JMenuItem jmi = new JMenuItem(label);
                    jmi.setUI(new StayOpenMenuItemUI());
                    popupMenu.add(jmi);
                }

            }
        }

        final JMenuItem jcbValid = new JMenuItem(
                "<html><b><i><font color=\"green\">" +
                I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectMultiItemsTool.Validate") +
                "</font></i></b></html>"
        );
        jcbValid.setSelected(true);
        jcbValid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupMenu.setVisible(false);
                popupMenu.setEnabled(false);
            }
        });
        popupMenu.add(jcbValid);

        if (count > 1) {
            layerViewPanel.add(popupMenu);
            popupMenu.show(layerViewPanel, (int)getViewSource().getX(), (int)getViewSource().getY());
            popupMenu.setVisible(true);
        }
    }

    private void addAll(final JPopupMenu popup,
                        final JCheckBoxMenuItem item,
                        final FeatureSelection featureSelection,
                        final Map layerToFeaturesInFenceMap) {
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = item.isSelected();

                if (!selected) {
                    for (Iterator i = layerToFeaturesInFenceMap.keySet().iterator(); i.hasNext();) {
                        Layer lyr = (Layer)i.next();
                        featureSelection.unselectItems(lyr, (Collection)layerToFeaturesInFenceMap.get(lyr));
                    }
                    layerViewPanel.fireSelectionChanged();
                    layerViewPanel.getSelectionManager().updatePanel();
                } else {
                    for (Iterator i = layerToFeaturesInFenceMap.keySet().iterator(); i.hasNext();) {
                        Layer lyr = (Layer)i.next();
                        if (!lyr.isSelectable()) continue;
                        featureSelection.selectItems(lyr, (Collection) layerToFeaturesInFenceMap.get(lyr));
                    }
                    layerViewPanel.fireSelectionChanged();
                    layerViewPanel.getSelectionManager().updatePanel();
                }
                MenuElement[] elements = popup.getSubElements();
                for (int i = 1 ; i < elements.length ; i++) {
                    if (elements[i] instanceof  JCheckBoxMenuItem) {
                        ((JCheckBoxMenuItem)elements[i]).setSelected(selected);
                    }
                }

            }
        });
    }


    // override SelectTool shortcut, not supported
    public Map<QuasimodeTool.ModifierKeySpec, String> describeShortcuts() {
        return null;
    }

    // tip from
    // http://stackoverflow.com/questions/3759379/how-to-prevent-jpopupmenu-disappearing-when-checking-checkboxes-in-it
    public static class StayOpenCheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {

        @Override
        protected void doClick(MenuSelectionManager msm) {
            menuItem.doClick(0);
        }

        public static ComponentUI createUI(JComponent c) {
            return new StayOpenCheckBoxMenuItemUI();
        }
    }

    public static class StayOpenMenuItemUI extends BasicMenuItemUI {

        @Override
        protected void doClick(MenuSelectionManager msm) {
            menuItem.doClick(0);
        }

        public static ComponentUI createUI(JComponent c) {
            return new StayOpenCheckBoxMenuItemUI();
        }
    }

}
