/***********************************************
 * created on 		10.05.2005
 * last modified:	31.05.2005 	
 * 
 * @author neun and sstein
 * 
 * Saves the actual map window as svg graphics
 * 
 ***********************************************/
package org.openjump.core.ui.plugin.file;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.renderer.LayerRenderer;
import com.vividsolutions.jump.workbench.ui.renderer.Renderer;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.swing.JFileChooser;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;

/**
 * Saves the actual map window as svg graphics
 * 
 * @author neun and sstein
 */
public class SaveImageAsSVGPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

	File selFile = null;
	
	public SaveImageAsSVGPlugIn() {
	}

	public void initialize(PlugInContext context) throws Exception {
    	
		context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,		        
      	      new String[] {
		          MenuNames.FILE, MenuNames.FILE_SAVEVIEW
		        }, 
		        I18N.get("org.openjump.core.ui.plugin.file.SaveImageAsSVGPlugIn.save-image-in-svg-format") + "..." + "{pos:10}",
				false, 
				null, 
                createEnableCheck(context.getWorkbenchContext())); //enable check
	}

	public boolean execute(PlugInContext context) throws Exception {
		JFileChooser fc = GUIUtil.createJFileChooserWithOverwritePrompting("svg");
		// Show save dialog; this method does not return until the dialog is closed
		fc.showSaveDialog(context.getWorkbenchFrame());
		File file = fc.getSelectedFile();
		try{
			String name = file.getPath();		
			name = this.addExtension(name,"svg");
			File newFile = new File(name);
			this.selFile = newFile;
			return true;
		}
		catch(Exception e){			
			return false;
		}
	}

	public void run(TaskMonitor monitor, PlugInContext context)
			throws Exception {
		//Get a DOMImplementation
		DOMImplementation domImpl = GenericDOMImplementation
				.getDOMImplementation();

		//Create an instance of org.w3c.dom.Document
		Document document = domImpl.createDocument(null, "svg", null);

		//Create an instance of the SVG Generator
		/*
		 SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
		 ctx.setEmbeddedFontsOn(true);
		 SVGGraphics2D svgGenerator = new SVGGraphics2D(ctx, true);
		 */
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
	
		
		// --- Test with changed classes of Openjump and the new maxFeatures 
		//     field in FeatureCollectionRenderer.class

		LayerViewPanel lvp = context.getLayerViewPanel();
		RenderingManager rms = lvp.getRenderingManager();
		List layers = context.getLayerManager().getVisibleLayers(false);		
		for (int i=0; i < layers.size(); i++) {
			Layer layer = (Layer)layers.get(i);		
			Renderer myR = rms.getRenderer(layer);
			if( myR instanceof LayerRenderer){
				LayerRenderer myRnew = (LayerRenderer)myR;
				myRnew.setMaxFeatures(10000);
			}
		}
		lvp.repaint();
		lvp.paintComponent(svgGenerator);
		//------------------------------
		//reset the old state of 100 features 
		for (int i=0; i < layers.size(); i++) {
			Layer layer = (Layer)layers.get(i);		
			Renderer myR = rms.getRenderer(layer);
			if( myR instanceof LayerRenderer){
				LayerRenderer myRnew = (LayerRenderer)myR;
				myRnew.setMaxFeatures(100);
			}
		}		
		//------------------------------

		/** old working code for original jump
		// paint using new renderer		
		LayerViewPanel lvp = context.getLayerViewPanel();
		RenderingManager rm = lvp.getRenderingManager();
        Class[] types1 = {Object.class, Renderer.class};
		List layers = context.getLayerManager().getVisibleLayers(false);
		for (int i=0; i < layers.size(); i++) {
			Layer layer = (Layer)layers.get(i);
			//-- do now this:
			//   rm.setRenderer(layer,new SvgRenderer(layer, context.getLayerViewPanel()));
			// but for an invisible method setRenderer()
			SvgRenderer sr = new SvgRenderer(layer, context.getLayerViewPanel());
	        Object[] params1 ={layer, sr};
	        AccessToPrivateMethods.invokePrivateMethod("setRenderer",rm,RenderingManager.class,params1,types1);
	        sr.createRunnable(); //do paint (also on screen)
		}				
		//paint the layerview into the svgGenerator
		lvp.paintComponent(svgGenerator);
		**/
		
		//Finally, stream out SVG to the your file
		//Writer out = new FileWriter("MyMoMap.svg");
		//FileWriter out = new FileWriter(selFile);
		try{			
			FileOutputStream fos = new FileOutputStream(this.selFile, false);
			OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
			svgGenerator.stream(out, true);
			out.close();
		}
		catch(Exception e){
			context.getWorkbenchFrame().warnUser("error:" + e.getMessage());
		}
	}
	
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(0));
    }
 
    private String addExtension(String path, String extension) {
        if (path.toUpperCase().endsWith(extension.toUpperCase())) {
            return path;
        }
        if (path.endsWith(".")) {
            return path + extension;
        }
        return path + "." + extension;
    }
}
