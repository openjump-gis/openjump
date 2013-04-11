/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/exse/
 lat/lon Fitzke/Fretter/Poth GbR
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth
 lat/lon Fitzke/Fretter/Poth GbR
 Meckenheimer Allee 176
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Jens Fitzke
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: jens.fitzke@uni-bonn.de

 
 ---------------------------------------------------------------------------*/
package de.latlon.deejump.plugin.style;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.openjump.core.ui.util.ScreenScale;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei </a>
 *  
 */
public class LayerStyle2SLDPlugIn extends AbstractPlugIn {

    /**
     * The <code>Transformer</code> object used in the transformation of a task/project/layer xml
     * to sld.
     */
    protected static Transformer transformer = null;

    private static String WMS_LAYER_NAME = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"); 

    private static String STYLE_NAME = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"); 

    private static String STYLE_TITLE = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"); 
    
    private static String FEATURETYPE_STYLE = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style"); 
    
    private static String GEOTYPE = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.geoType"); 
    
    private static final String UTF_8 = "UTF-8";

    private static String GEOM_PROPERTY = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.geomProperty"); 

    private static String SCALE_MIN = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.minScale"); 

    private static String SCALE_MAX = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.maxScale"); 

    
    static{
        initTransformer();
    }
    
    private Java2XML java2Xml = new Java2XML();

    private JFileChooser fileChooser;
    
    private MultiInputDialog dialog;

    private String wmsLayerName = WMS_LAYER_NAME;
    
    private String styleName = STYLE_NAME; 
    
    private String styleTitle = STYLE_TITLE;
    
    private String featureTypeStyle = FEATURETYPE_STYLE; 
    
    private double scaleFactor = 1d;
    
    private String geoProperty = "GEOM";
    
    public String getName() {
        return I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Transform-layer-style-into-sld");
    }

    /**
     * use this method to install the LayerStyle2SLD plugin in the toolbar
     * @param context
     * @throws Exception
     */
    public void install( PlugInContext context ) throws Exception {

        context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().addPlugIn(
            getIcon(),
            this, 
            createEnableCheck(context.getWorkbenchContext()),
            context.getWorkbenchContext()
        );  	
    	
    }
    
    public void initialize(PlugInContext context) throws Exception {
    	   			
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,								//exe
	                new String[] {MenuNames.VIEW}, 	//menu path
	                this.getName() +"{pos:2}", //name methode .getName recieved by AbstractPlugIn 
	                false,			//checkbox
	                null,			//icon
	                createEnableCheck(context.getWorkbenchContext())); //enable check
    }

    private void initStrings(){

    	WMS_LAYER_NAME = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"); 
    	STYLE_NAME = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"); 
    	STYLE_TITLE = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"); 
        FEATURETYPE_STYLE = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style");         
        GEOTYPE = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.geoType"); 
        GEOM_PROPERTY = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.geomProperty"); 
        SCALE_MIN = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.minScale"); 
        String SCALE_MAX = I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.maxScale"); 

    }
    public boolean execute( PlugInContext context ) throws Exception {
        reportNothingToUndoYet( context );
        
    	this.initStrings();
    	
        Layer layer = context.getSelectedLayer( 0 );
        if ( layer == null ) {
            return false;
        }
        
        initDialog(context);
        
        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return false;
        }
        
        wmsLayerName = dialog.getText( WMS_LAYER_NAME );
        styleName = dialog.getText( STYLE_NAME );
        styleTitle = dialog.getText( STYLE_TITLE );
        featureTypeStyle = dialog.getText( FEATURETYPE_STYLE );
        geoProperty = dialog.getText( GEOM_PROPERTY );
        
        if ( fileChooser == null ) {
            fileChooser = new JFileChooser();
            fileChooser.setApproveButtonText( I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Save"));
            fileChooser.setDialogTitle( I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Save-style-as-SLD"));
        }
        
        if (JFileChooser.APPROVE_OPTION == 
            	fileChooser.showOpenDialog(context.getWorkbenchFrame())) {
            
            //FIXME no good: saving to file to transform...
            File file = File.createTempFile( "temptask", ".xml" );
            file.deleteOnExit();
            
            activateVertexStyle( layer );
            
            scaleFactor = calcScaleFactor( context.getLayerViewPanel() );
            
            transformXML( layer, file, fileChooser.getSelectedFile(), scaleFactor);
            
        }
        
        return true;
    }

    
    /**
     * When starting up, vertex styles might be deactivated, though they are "shown". So, if 
     * layer if of point type, and basic style is activated, activated vertex style too.
     * @param layer
     */
    private void activateVertexStyle( Layer layer ) {
        Iterator iter = layer.getFeatureCollectionWrapper().getUltimateWrappee()
    	.getFeatures().iterator();
        String type = "";
        if( iter.hasNext() ){
            Feature f = (Feature)iter.next();
            type = f.getGeometry().getGeometryType();
             
        }
        if ( layer.getBasicStyle().isEnabled() ){
            if( "MultiPoint".equals( type ) || "Point".equals( type ) ){
                layer.getVertexStyle().setEnabled( true );
            }
        }
    }

    private void initDialog(PlugInContext context) {
        if( dialog == null ){
            
	        dialog = new MultiInputDialog(context.getWorkbenchFrame(),I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.SLD-Parameters"), true);
	        
	        dialog.addSeparator();
	        
            dialog.addTextField( GEOM_PROPERTY, geoProperty, 25, null, I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Input-the-name-of-the-geometry-property") );
            
            dialog.addSeparator();
            String name = context.getCandidateLayer( 0 ).getName();
            
            dialog.addTextField( WMS_LAYER_NAME, name, 25, null, WMS_LAYER_NAME );
	        dialog.addTextField( STYLE_NAME, name, 25, null, STYLE_NAME );
	        dialog.addTextField( STYLE_TITLE, name, 25, null, STYLE_TITLE );
	        dialog.addTextField( FEATURETYPE_STYLE, name, 25, null, FEATURETYPE_STYLE );
	        GUIUtil.centreOnWindow(dialog);
        }
    }
    
    private void transformXML( Layer layer, File inputXML, File outputXML, double scaleFactor ) throws Exception{

        //TODO don't assume has 1 item!!!
        BasicFeature bf = (BasicFeature)layer.getFeatureCollectionWrapper().getFeatures().get( 0 );
        Geometry geo = bf.getGeometry();
        String geoType = geo.getGeometryType(); 
                
        java2Xml.write( layer,"layer", inputXML );
        
        FileInputStream input = new FileInputStream( inputXML );
        
        //FileWriter fw = new FileWriter( outputXML );
        OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream( outputXML ), UTF_8);
        
        HashMap map = new HashMap( 10 );
        map.put( WMS_LAYER_NAME, wmsLayerName );
        map.put( FEATURETYPE_STYLE, featureTypeStyle );
        map.put( STYLE_NAME, styleName );
        map.put( STYLE_TITLE, styleTitle );
        map.put( GEOTYPE, geoType );
        map.put( GEOM_PROPERTY, geoProperty );
        
        
        // ATENTION : note that min and max are swapped in JUMP!!!
        // will swap later, in transformContext
        Double d = layer.getMinScale();
        d = d != null ? d : new Double( 0 ); 

        map.put( SCALE_MIN, toRealWorldScale( scaleFactor, d.doubleValue()) );
        
        // using  Double.MAX_VALUE  is creating a large number - too many 0's
        // make it simple and hardcde a large number
        final double largeNumber = 99999999999d;
        d = layer.getMaxScale();
        d = d != null ? d : new Double( largeNumber );
        
        map.put( SCALE_MAX, toRealWorldScale( scaleFactor, d.doubleValue()) );
        
        
        fw.write( transformContext( input, map ) );
        fw.close();

    }
    
    public Icon getIcon() {
        return new ImageIcon(LayerStyle2SLDPlugIn.class.getResource("sldstyle.png"));
    }

    public void run( TaskMonitor monitor, PlugInContext context ) throws Exception {
        // will need this? extend threaded plug-in
    }

    public static String transformContext( InputStream layerXML , HashMap parMap)
        throws TransformerException, UnsupportedEncodingException {

        StringWriter sw = new StringWriter();
        StreamResult sr = new StreamResult( sw );
        
        InputStreamReader isr = new InputStreamReader( layerXML, UTF_8);
        StreamSource streamSource = new StreamSource( isr );

        //if you don't clear the pars, xalan throws a nasty NPE
        transformer.clearParameters();
        
        //TODO ths is getting too long -> iterate over pars and set them
        transformer.setParameter( "wmsLayerName", parMap.get( WMS_LAYER_NAME ) );
        transformer.setParameter( "featureTypeStyle", parMap.get( FEATURETYPE_STYLE ) );
        transformer.setParameter( "styleName", parMap.get( STYLE_NAME ) );
        transformer.setParameter( "styleTitle", parMap.get( STYLE_TITLE ) );
        transformer.setParameter( GEOTYPE, parMap.get( GEOTYPE ) );
        transformer.setParameter( GEOM_PROPERTY, parMap.get( GEOM_PROPERTY ) );
        transformer.setParameter( SCALE_MIN, parMap.get( SCALE_MIN ) );
        transformer.setParameter( SCALE_MAX, parMap.get( SCALE_MAX ) );
        
        transformer.transform( streamSource, sr );

        try {
            sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
//System.out.println( "sw.toString(): " + sw.toString() );
        
        return sw.toString();
    }

    private static void initTransformer( ) {

        try {

            URL xslUrl = LayerStyle2SLDPlugIn.class.getResource( "layerstyle2sld.xsl" );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            
            InputStreamReader isr = new InputStreamReader( xslUrl.openStream(), UTF_8);
            
            StreamSource streamSrc = new StreamSource( isr );
            
            transformer = transformerFactory.newTransformer( streamSrc );
        } 
 		//-- [sstein] evtl. comment out for test reasons, because it could be, that appears a problem
 		//            in initializing the exceptions while starting up OJUMP on WinXP? 
 		//			  (see email by Johannes Metzler, on Jump-User, 10.Aug.2006] 
         catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //-- used instead the simple form 
    }

    public EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory ecf = new EnableCheckFactory(workbenchContext);
        
        MultiEnableCheck mec = new MultiEnableCheck()
        	.add( ecf.createWindowWithLayerNamePanelMustBeActiveCheck())
    		.add( ecf.createExactlyNLayerablesMustBeSelectedCheck( 1, Layer.class) );
        
        return mec;
        
    }
    
    public static final Double toRealWorldScale( double scaleFactor, double jumpScale ){
        
        
        return new Double( jumpScale/scaleFactor );
    }
    
    private double calcScaleFactor( LayerViewPanel panel ){
        double internalScale = 1d / panel.getViewport().getScale();
		double realScale = ScreenScale.getHorizontalMapScale(panel.getViewport());
		return internalScale / realScale;
    }
    
    public static void main(String[] args){
        
        try {

        FileInputStream input = 
            new FileInputStream( new File("f:/temp/input_layer_style2.xml") );

        FileWriter sw = new FileWriter( "f:/temp/sldoutput.xml");

        HashMap map = new HashMap( 4 );
        map.put( WMS_LAYER_NAME, "mrh:sehenswert" );
        map.put( FEATURETYPE_STYLE, "feature_test");
        map.put( STYLE_NAME, "mrh:sehenswert2" );
        map.put( STYLE_TITLE, "mrh:sehenswert2" );
        map.put( GEOTYPE, "Point" );
        
//        StringWriter sw = new StringWriter();
        sw.write( transformContext( input, map ) );
        sw.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
}