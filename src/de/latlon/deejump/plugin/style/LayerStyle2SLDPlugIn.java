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

import static com.vividsolutions.jump.workbench.ui.MenuNames.LAYER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openjump.core.ui.util.ScreenScale;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
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
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei </a>
 * 
 */
public class LayerStyle2SLDPlugIn extends AbstractPlugIn {

    private static final Logger LOG = Logger
            .getLogger(LayerStyle2SLDPlugIn.class);

    /**
     * The <code>Transformer</code> object used in the transformation of a
     * task/project/layer xml to sld.
     */
    protected static Transformer transformer = null;

    private static final String UTF_8 = "UTF-8";

    static {
        initTransformer();
    }

    private Java2XML java2Xml = new Java2XML();

    private JFileChooser fileChooser;

    private MultiInputDialog dialog;

    private String styleTitle, geomProperty, styleName, wmsLayerName,
            namespace, prefix;

    private String featureTypeStyle;

    private double scaleFactor = 1d;

    @Override
    public String getName() {
        return I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Transform-layer-style-into-sld");
    }

    /**
     * use this method to install the LayerStyle2SLD plugin in the toolbar
     * 
     * @param context
     * @throws Exception
     */
    public void install(PlugInContext context) throws Exception {

        context.getWorkbenchContext().getWorkbench().getFrame().getToolBar()
                .addPlugIn(getIcon(), this,
                        createEnableCheck(context.getWorkbenchContext()),
                        context.getWorkbenchContext());

    }

    @Override
    public void initialize(PlugInContext context) throws Exception {

        FeatureInstaller featureInstaller = new FeatureInstaller(context
                .getWorkbenchContext());
        featureInstaller.addMainMenuItem(this, // exe
                new String[] { LAYER }, // menu path
                this.getName(), // name methode .getName
                // recieved by
                // AbstractPlugIn
                false, // checkbox
                null, // icon
                createEnableCheck(context.getWorkbenchContext())); // enable
        // check
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        Blackboard bb = PersistentBlackboardPlugIn.get(context
                .getWorkbenchContext());
        String fileName = (String) bb.get("LayerStyle2SLDPlugIn.filename");

        reportNothingToUndoYet(context);

        Layer layer = context.getSelectedLayer(0);
        if (layer == null) {
            return false;
        }

        geomProperty = "GEOM";

        FeatureSchema schema = layer.getFeatureCollectionWrapper()
                .getFeatureSchema();
        for (int i = 0; i < schema.getAttributeCount(); ++i) {
            if (schema.getAttributeType(i) == AttributeType.GEOMETRY) {
                geomProperty = schema.getAttributeName(i);
            }
        }

        initDialog(context);

        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return false;
        }

        // why the field name given when constructing the dialog are used both
        // for the label and
        // for the key is beyond me
        wmsLayerName = dialog
                .getText(I18N
                        .get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"));
        styleName = dialog.getText(I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"));
        styleTitle = dialog.getText(I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"));
        featureTypeStyle = dialog
                .getText(I18N
                        .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style"));
        geomProperty = dialog
                .getText(I18N
                        .get("deejump.pluging.style.LayerStyle2SLDPlugIn.geomProperty"));
        namespace = dialog.getText(I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Namespace"));
        prefix = namespace.substring(namespace.lastIndexOf('/') + 1);

        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            if (fileName != null) {
                fileChooser.setCurrentDirectory(new File(fileName)
                        .getParentFile());
            }
            fileChooser.setApproveButtonText(I18N
                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Save"));
            fileChooser
                    .setDialogTitle(I18N
                            .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Save-style-as-SLD"));
        }

        if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(context
                .getWorkbenchFrame())) {

            File file = File.createTempFile("temptask", ".xml");
            file.deleteOnExit();
            bb.put("LayerStyle2SLDPlugIn.filename", fileChooser
                    .getSelectedFile().getAbsoluteFile().toString());

            scaleFactor = calcScaleFactor(context.getLayerViewPanel());

            transformXML(layer, file, fileChooser.getSelectedFile(),
                    scaleFactor);

        }

        return true;
    }

    private void initDialog(PlugInContext context) {
        if (dialog == null) {

            dialog = new MultiInputDialog(
                    context.getWorkbenchFrame(),
                    I18N
                            .get("deejump.pluging.style.LayerStyle2SLDPlugIn.SLD-Parameters"),
                    true);

            dialog.addSeparator();

            dialog
                    .addTextField(
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.geomProperty"),
                            geomProperty,
                            25,
                            null,
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Input-the-name-of-the-geometry-property"));

            dialog.addSeparator();
            String name = context.getCandidateLayer(0).getName();

            dialog
                    .addTextField(
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"),
                            name,
                            25,
                            null,
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"));
            dialog
                    .addTextField(
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"),
                            name,
                            25,
                            null,
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"));
            dialog
                    .addTextField(
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"),
                            name,
                            25,
                            null,
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"));
            dialog
                    .addTextField(
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style"),
                            name,
                            25,
                            null,
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style"));
            dialog
                    .addTextField(
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Namespace"),
                            "http://www.deegree.org/app",
                            25,
                            null,
                            I18N
                                    .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Namespace"));
            GUIUtil.centreOnWindow(dialog);
        }
    }

    private void transformXML(Layer layer, File inputXML, File outputXML,
            double scaleFactor) throws Exception {

        // TODO don't assume has 1 item!!!
        BasicFeature bf = (BasicFeature) layer.getFeatureCollectionWrapper()
                .getFeatures().get(0);
        Geometry geo = bf.getGeometry();
        String geoType = geo.getGeometryType();

        java2Xml.write(layer, "layer", inputXML);

        FileInputStream input = new FileInputStream(inputXML);

        // FileWriter fw = new FileWriter( outputXML );
        OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(
                outputXML), UTF_8);

        HashMap<String, String> map = new HashMap<String, String>(10);
        map.put("wmsLayerName", wmsLayerName);
        map.put("featureTypeStyle", featureTypeStyle);
        map.put("styleName", styleName);
        map.put("styleTitle", styleTitle);
        map.put("geoType", geoType);
        map.put("geomProperty", geomProperty);
        map.put("Namespace", namespace);
        map.put("NamespacePrefix", prefix + ":");
        map.put("NamespacePrefixWithoutColon", prefix);

        // ATENTION : note that min and max are swapped in JUMP!!!
        // will swap later, in transformContext
        Double d = layer.getMinScale();
        d = d != null ? d : new Double(0);

        map
                .put("minScale", ""
                        + toRealWorldScale(scaleFactor, d.doubleValue()));

        // using Double.MAX_VALUE is creating a large number - too many 0's
        // make it simple and hardcde a large number
        final double largeNumber = 99999999999d;
        d = layer.getMaxScale();
        d = d != null ? d : new Double(largeNumber);

        map
                .put("maxScale", ""
                        + toRealWorldScale(scaleFactor, d.doubleValue()));

        fw.write(transformContext(input, map));
        fw.close();

    }

    /**
     * @return the icon
     */
    public Icon getIcon() {
        return new ImageIcon(LayerStyle2SLDPlugIn.class
                .getResource("sldstyle.png"));
    }

    /**
     * @param layerXML
     * @param parMap
     * @return the transformed XML (?)
     * @throws TransformerException
     * @throws UnsupportedEncodingException
     */
    public static String transformContext(InputStream layerXML,
            Map<String, String> parMap) throws TransformerException,
            UnsupportedEncodingException {
        return transformContext(new InputStreamReader(layerXML, UTF_8), parMap);
    }

    /**
     * @param layerXML
     * @param parMap
     * @return the transformed XML (?)
     * @throws TransformerException
     */
    public static String transformContext(Reader layerXML,
            Map<String, String> parMap) throws TransformerException {

        StringWriter sw = new StringWriter();
        StreamResult sr = new StreamResult(sw);

        StreamSource streamSource = new StreamSource(layerXML);

        // if you don't clear the pars, xalan throws a nasty NPE
        transformer.clearParameters();

        for (String key : parMap.keySet()) {
            transformer.setParameter(key, parMap.get(key));
        }

        transformer.transform(streamSource, sr);

        try {
            sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (LOG.getLevel() == Level.DEBUG) {
            LOG.debug(sw.toString());
        }

        return sw.toString();
    }

    private static void initTransformer() {

        try {

            URL xslUrl = LayerStyle2SLDPlugIn.class
                    .getResource("layerstyle2sld.xsl");
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();

            InputStreamReader isr = new InputStreamReader(xslUrl.openStream(),
                    UTF_8);

            StreamSource streamSrc = new StreamSource(isr);

            transformer = transformerFactory.newTransformer(streamSrc);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param workbenchContext
     * @return the enable check
     */
    public EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory ecf = new EnableCheckFactory(workbenchContext);

        MultiEnableCheck mec = new MultiEnableCheck().add(
                ecf.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(
                        ecf.createExactlyNLayerablesMustBeSelectedCheck(1,
                                Layer.class));

        return mec;

    }

    /**
     * @param scaleFactor
     * @param jumpScale
     * @return the scale
     */
    public static final Double toRealWorldScale(double scaleFactor,
            double jumpScale) {

        return new Double(jumpScale / scaleFactor);
    }

    private double calcScaleFactor(LayerViewPanel panel) {
        double internalScale = 1d / panel.getViewport().getScale();
        double realScale = ScreenScale.getHorizontalMapScale(panel
                .getViewport());
        return internalScale / realScale;
    }

}
