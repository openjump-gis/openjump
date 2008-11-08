/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui.plugin;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
public class ViewAttributesPlugIn extends AbstractPlugIn {
	public ViewAttributesPlugIn() {
	}
	public String getName() {
		return I18N.get("ui.plugin.ViewAttributesPlugIn.view-edit-attributes");
	}
	public boolean execute(final PlugInContext context) throws Exception {
		reportNothingToUndoYet(context);
		//Don't add GeometryInfoFrame because the HTML will probably be too
		// much for the editor pane (too many features). [Jon Aquino]
		final ViewAttributesFrame frame = new ViewAttributesFrame(context
				.getSelectedLayer(0), context);
		context.getWorkbenchFrame().addInternalFrame(frame);
		return true;
	}
	public MultiEnableCheck createEnableCheck(
			final WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(
				workbenchContext);
		return new MultiEnableCheck().add(
				checkFactory.createTaskWindowMustBeActiveCheck()).add(
				checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
	}
	public ImageIcon getIcon() {
		//return IconLoaderFamFam.icon("table.png");
		return IconLoader.icon("Row.gif");
	}
	public static class ViewAttributesFrame extends JInternalFrame
			implements
				LayerManagerProxy,
				SelectionManagerProxy,
				LayerNamePanelProxy,
				TaskFrameProxy,
				LayerViewPanelProxy {
		private LayerManager layerManager;
		private OneLayerAttributeTab attributeTab;
		public ViewAttributesFrame(Layer layer, PlugInContext context) {
			this.layerManager = context.getLayerManager();
			addInternalFrameListener(new InternalFrameAdapter() {
				public void internalFrameClosed(InternalFrameEvent e) {
					//Assume that there are no other views on the model [Jon
					// Aquino]
					attributeTab.getModel().dispose();
				}
			});
			setResizable(true);
			setClosable(true);
			setMaximizable(true);
			setIconifiable(true);
			getContentPane().setLayout(new BorderLayout());
			attributeTab = new OneLayerAttributeTab(context
					.getWorkbenchContext(), ((TaskFrameProxy) context
					.getActiveInternalFrame()).getTaskFrame(), this).setLayer(layer);
			addInternalFrameListener(new InternalFrameAdapter() {
				public void internalFrameOpened(InternalFrameEvent e) {
					attributeTab.getToolBar().updateEnabledState();
				}
			});
			getContentPane().add(attributeTab, BorderLayout.CENTER);
			setSize(500, 300);
			updateTitle(attributeTab.getLayer());
			context.getLayerManager().addLayerListener(new LayerListener() {
				public void layerChanged(LayerEvent e) {
					if (attributeTab.getLayer() != null) {
						updateTitle(attributeTab.getLayer());
					}
				}
				public void categoryChanged(CategoryEvent e) {
				}
				public void featuresChanged(FeatureEvent e) {
				}
			});
			Assert.isTrue(!(this instanceof CloneableInternalFrame),
					I18N.get("ui.plugin.ViewAttributesPlugIn.there-can-be-no-other-views-on-the-InfoModels"));
		}
		public LayerViewPanel getLayerViewPanel() {
			return getTaskFrame().getLayerViewPanel();
		}
		public LayerManager getLayerManager() {
			return layerManager;
		}
		private void updateTitle(Layer layer) {
			String editView;
			if (layer.isEditable()) {
				editView = I18N.get("ui.plugin.ViewAttributesPlugIn.edit");
			} else {
				editView = I18N.get("ui.plugin.ViewAttributesPlugIn.view");
			}
			
			setTitle(" "+I18N.get("ui.plugin.ViewAttributesPlugIn.attributes")
					+": "+ layer.getName());
		}
		public TaskFrame getTaskFrame() {
			return attributeTab.getTaskFrame();
		}
		public SelectionManager getSelectionManager() {
			return attributeTab.getPanel().getSelectionManager();
		}
		public LayerNamePanel getLayerNamePanel() {
			return attributeTab;
		}
	}
}