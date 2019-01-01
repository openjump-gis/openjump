package org.openjump.core.ui.plugin.raster.color;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.styler.RasterStylesPlugIn;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * 16 sept. 2005
 *
 * @author  Paul PLOUY
 *  			Laboratoire RESO
 *  			universit� de Rennes 2
 *              FRANCE
 * 			modified by Stefan Steiniger (perriger@gmx.de)
 */
public class RasterColorEditorPlugIn extends AbstractPlugIn {

    private String sName = "Raster Color Editor";
    
	public RasterColorEditorPlugIn() {

	}

	public void initialize(PlugInContext context) throws Exception {

		super.initialize(context);

		this.sName = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Raster-Color-Editor");
		
		context
		.getFeatureInstaller()
		.addMainMenuItem(
				this,
				new String[] {MenuNames.RASTER},
				this.sName + "...", 
				false,
				IconLoader.icon("color_wheel.png"),

				new MultiEnableCheck()
				.add(
						new EnableCheckFactory(context
								.getWorkbenchContext())
						.createWindowWithLayerViewPanelMustBeActiveCheck())
						.add(
								new EnableCheckFactory(context
										.getWorkbenchContext())
								.createAtLeastNLayerablesMustBeSelectedCheck(
										1, RasterImageLayer.class))

		);

	}

	public boolean execute(PlugInContext context) throws Exception {

		 new RasterStylesPlugIn().execute(context);
			/*	RasterImageLayer actualLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context, RasterImageLayer.class);

				RasterColorEditorDialog rasterColorEditor = new RasterColorEditorDialog(
						context, actualLayer);

				rasterColorEditor.setVisible(true);*/

				//System.out.println(actualLayer);

		return true;
	}

}
