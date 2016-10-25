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
package de.latlon.deejump.plugin.style;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.OKCancelApplyPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStylePanel;
import com.vividsolutions.jump.workbench.ui.style.DecorationStylePanel;
import com.vividsolutions.jump.workbench.ui.style.LabelStylePanel;
import com.vividsolutions.jump.workbench.ui.style.ScaleStylePanel;
import com.vividsolutions.jump.workbench.ui.style.StylePanel;

/**
 * <code>DeeChangeStylesPlugIn</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date: 2008-02-14 14:37:00 +0100 (Thu, 14 Feb
 *          2008) $
 */
public class DeeChangeStylesPlugIn extends AbstractPlugIn {
    private final static String LAST_TAB_KEY = DeeChangeStylesPlugIn.class.getName() + " - LAST TAB";

    @Override
    public String getName() {
        return I18N.get("ui.style.ChangeStylesPlugIn.change-styles");
    }

    @Override
    public boolean execute(final PlugInContext context) throws Exception {
        WorkbenchFrame wbframe = context.getWorkbenchFrame();
        WorkbenchContext wbcontext = context.getWorkbenchContext();
        Blackboard blackboard = wbcontext.getWorkbench().getBlackboard();
        Blackboard pb = PersistentBlackboardPlugIn.get(wbcontext);

        final Layer layer = context.getSelectedLayer(0);
        
        //[Giuseppe Aruta] June 3 2015 - added Layer name.
        final MultiInputDialog dialog = new MultiInputDialog(wbframe,
                I18N.get("ui.style.ChangeStylesPlugIn.change-styles") + " - "
                        + layer.getName(), true);
        dialog.setApplyVisible(true);
        dialog.setInset(0);
        dialog.setSideBarImage(IconLoader.icon("Symbology.gif"));
        dialog.setSideBarDescription(I18N
                .get("ui.style.ChangeStylesPlugIn.you-can-use-this-dialog-to-change-the-colour-line-width"));

        final ArrayList<StylePanel> stylePanels = new ArrayList<StylePanel>();
        final DeeRenderingStylePanel renderingStylePanel = new DeeRenderingStylePanel(blackboard, layer, pb);
        final Collection<?> oldStyles = layer.cloneStyles();
        stylePanels.add(renderingStylePanel);
        stylePanels.add(new ScaleStylePanel(layer, context.getLayerViewPanel()));

        // Only set preferred size for DecorationStylePanel or ColorThemingStylePanel;
        // otherwise they will grow to the height of the screen. But don't set
        // the preferred size of LabelStylePanel to (400, 300) -- in fact, it
        // needs
        // a bit more height -- any less, and its text boxes will shrink to
        // zero-width. I've found that if you don't give text boxes enough
        // height,
        // they simply shrink to zero-width. [Jon Aquino]
        DecorationStylePanel decorationStylePanel = new DecorationStylePanel(layer, wbframe.getChoosableStyleClasses());
        decorationStylePanel.setPreferredSize(new Dimension(400, 300));
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        int numberOfAttributesUsableInColorTheming = schema.getAttributeCount() - 1;
        if (schema.hasAttribute(BasicStyle.RGB_ATTRIBUTE_NAME)) {
            numberOfAttributesUsableInColorTheming--;
        }
        if (numberOfAttributesUsableInColorTheming >= 1) {
            ColorThemingStylePanel colorThemingStylePanel = new ColorThemingStylePanel(layer, wbcontext);
            colorThemingStylePanel.setPreferredSize(new Dimension(400, 300));
            stylePanels.add(colorThemingStylePanel);
            GUIUtil.sync(renderingStylePanel.getTransparencySlider(), colorThemingStylePanel.getTransparencySlider());
            GUIUtil.sync(renderingStylePanel.getSynchronizeCheckBox(), colorThemingStylePanel.getSynchronizeCheckBox());

        } else {
            stylePanels.add(new DummyColorThemingStylePanel());
        }

        stylePanels.add(new LabelStylePanel(layer, context.getLayerViewPanel(), dialog, context.getErrorHandler()));
        stylePanels.add(decorationStylePanel);

        JTabbedPane tabbedPane = new JTabbedPane();

        for (final StylePanel stylePanel : stylePanels) {
            tabbedPane.add((Component) stylePanel, stylePanel.getTitle());
            dialog.addEnableChecks(stylePanel.getTitle(), new EnableCheck() {
                public String check(JComponent component) {
                    return stylePanel.validateInput();
                }
            });
        }

        dialog.addRow(tabbedPane);

        String selectedTab = (String) blackboard.get(LAST_TAB_KEY, (stylePanels.iterator().next()).getTitle());
        
        // To apply styles without quiting the dialog, use a new actionListener
        dialog.addOKCancelApplyPanelActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (dialog.wasApplyPressed()) {
                    for (final StylePanel stylePanel : stylePanels) {
                        if (stylePanel.validateInput() != null) {
                            context.getWorkbenchFrame().handleThrowable(
                                    new Exception(stylePanel.validateInput()), dialog);
                            return;
                        }
                    }
                    if (dialog.wasApplyPressed()) {
                        testStyles(layer, stylePanels, context);
                    }
                }
            }
        }
        );

        tabbedPane.setSelectedComponent(find(stylePanels, selectedTab));
        dialog.pack();
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        blackboard.put(LAST_TAB_KEY, ((StylePanel) tabbedPane.getSelectedComponent()).getTitle());
        
        if (dialog.wasOKPressed()) {
            applyStyles(layer, stylePanels, oldStyles, context);
            return true;
        } else {
        	reportNothingToUndoYet(context);
        }
        return false;
    }
    
    // used to apply styles without quiting the ChangeStylesDialog.
    private void testStyles(final Layer layer, 
            final ArrayList<StylePanel> stylePanels, final PlugInContext context) {
        for (final StylePanel stylePanel : stylePanels) {
            stylePanel.updateStyles();
        }
        if (layer.getVertexStyle().isEnabled()) {
            layer.getBasicStyle().setRenderingVertices(false);
        }
    }
    
    private void applyStyles(final Layer layer, final ArrayList<StylePanel> stylePanels, 
            final Collection<?> oldStyles, final PlugInContext context) {
        layer.getLayerManager().deferFiringEvents(new Runnable() {
            public void run() {
                for (final StylePanel stylePanel : stylePanels) {
                    stylePanel.updateStyles();
                }
            }
        });
        
        // fix the problem with mixing styles
        layer.getLayerManager().deferFiringEvents(new Runnable() {
            public void run() {
                if (layer.getVertexStyle().isEnabled()) {
                    layer.getBasicStyle().setRenderingVertices(false);
                }
            }
        });

        final Collection<?> newStyles = layer.cloneStyles();
        execute(new UndoableCommand(getName()) {
            @Override
            public void execute() {
                layer.setStyles(newStyles);
            }

            @Override
            public void unexecute() {
                layer.setStyles(oldStyles);
            }
        }, context);
    }

    private Component find(Collection<StylePanel> stylePanels, String title) {
        for (final StylePanel stylePanel : stylePanels) {
            if (stylePanel.getTitle().equals(title)) {
                return (Component) stylePanel;
            }
        }

        Assert.shouldNeverReachHere();

        return null;
    }

    /**
     * @return the icon
     */
    public ImageIcon getIcon() {
        //return IconLoaderFamFam.icon("palette.png");
        return IconLoader.icon("Palette.png");
    }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        // ScaledStylePanel assumes that the active window has a LayerViewPanel. [Jon Aquino 2005-08-09]
        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck()).add(
                        checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
    }

    // DummyColorThemingStylePanel created when no attribute is available for classification
    private class DummyColorThemingStylePanel extends JPanel implements StylePanel {

        private static final long serialVersionUID = 2217457292163045134L;

        /**
         * 
         */
        public DummyColorThemingStylePanel() {
            // GridBagLayout so it gets centered. [Jon Aquino]
            super(new GridBagLayout());
            add(new JLabel(I18N.get("ui.style.ChangeStylesPlugIn.this-layer-has-no-attributes")));
        }

        public String getTitle() {
            return ColorThemingStylePanel.TITLE;
        }

        public void updateStyles() {
            // unused but defined in the interface
        }

        public String validateInput() {
            return null;
        }
    }
}
