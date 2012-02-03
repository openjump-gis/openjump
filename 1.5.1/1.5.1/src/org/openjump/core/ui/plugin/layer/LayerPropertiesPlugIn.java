/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2005 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */
package org.openjump.core.ui.plugin.layer;

import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.io.datasource.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.text.*;
import java.util.Map;

import org.openjump.core.ui.images.IconLoader;

public class LayerPropertiesPlugIn extends AbstractPlugIn 
{
    private final static String LAST_TAB_KEY = LayerPropertiesPlugIn.class.getName() +
        " - LAST TAB";
    private final static String LAYER_PROPERTIES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Layer-Properties");
    private final static String INFO = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Info");  //information
    private final static String LAYER_NAME = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Layer-Name");
    private final static String NUMBER_OF_LAYERS = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Layers");
    private final static String NUMBER_OF_FEATURES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Features");
    private final static String NUMBER_OF_POINTS = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Points"); 
    private final static String GEOMETRY_TYPE = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Geometry-Type");  //Polygon, Polyline, etc.
    private final static String NUMBER_OF_ATTRIBUTES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Number-of-Attributes");
    private final static String DATASOURCE_CLASS = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.DataSource-Class");  //class name
    private final static String SOURCE_PATH = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Source-Path");  //directory path of source layer
    private final static String NO_FEATURES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.No-Features"); //no features were found
    private final static String MULTIPLE_GEOMETRY_TYPES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-geometry-types");  //mixed
    private final static String MULTIPLE_SOURCE_TYPES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-Source-Types");
    private final static String NULL_GEOMETRIES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Null-Geometries");
    private final static String NOT_SAVED = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not-Saved"); //the layer is unsaved
    private final static String AVERAGE_PER_LAYER =  
    	" (" + I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.average-per-layer") +")";
    private final static String MULTIPLE_SOURCES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-Sources");
    private final static String STYLES = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Styles");
    private final static String PROPORTIONAL_TRANSPARENCY_ADJUSTER = 
    	I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Proportional-Transparency-Adjustment");
	private final static String CHARSET =
		I18N.get("org.openjump.core.ui.io.file.DataSourceFileLayerLoader.charset");
	private final static String EXTENT =
	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.extent");
    private final static String XMIN =
	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmin");
	private final static String YMIN =
	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymin");
	private final static String XMAX =
	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.xmax");
	private final static String YMAX =
	    I18N.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.ymax");
    private WorkbenchContext workbenchContext;
    private InfoPanel infoPanel;
    private StylePanel stylePanel;
    private Layer[] layers;
    private Envelope extent;
    private int[] currTransArray;
    private boolean styleChanged = false;
    
    public interface PropertyPanel {
    	public String getTitle();
    	public void updateStyles();
    	/**
    	 * @return an error message, or null if the input is valid
    	 */
    	public String validateInput();
    }
    
    public LayerPropertiesPlugIn() {}
    
    public ImageIcon getIcon() {
        return IconLoader.icon("info16_v.png");
    }
    
    public void initialize(PlugInContext context) throws Exception {
        this.workbenchContext = context.getWorkbenchContext();
        FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
        JPopupMenu layerNamePopupMenu = workbenchContext.getWorkbench()
                                                        .getFrame()
                                                        .getLayerNamePopupMenu();
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
            this, LAYER_PROPERTIES + "..." + "{pos:7}",
            false, getIcon(), 
            createEnableCheck(workbenchContext));        
    }
    
    public boolean execute(PlugInContext context) throws Exception {
    	styleChanged = false;
    	layers = context.getSelectedLayers();
    	extent = context.getSelectedLayerEnvelope();
    	currTransArray = new int[layers.length];
    	
        for (int i = 0; i < layers.length; i++)
        	currTransArray[i] = 255 - getAlpha(layers[i]);
        
		ArrayList oldStyleList = new ArrayList(layers.length);  //save to restore if cancel
		for (int i = 0; i < layers.length; i++) {
			Object layerable = layers[i];
			if (layerable instanceof Layer) {
				oldStyleList.add( ((Layer) layerable).cloneStyles()); // copy
			}
		}
        
        infoPanel = new InfoPanel();
        stylePanel = new StylePanel();
        MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(),
        		LAYER_PROPERTIES, true);
        //dialog.setInset(0);
        final ArrayList propertyPanels = new ArrayList();
        propertyPanels.add(infoPanel);
        propertyPanels.add(stylePanel);
        //infoPanel.setPreferredSize(new Dimension(350, 200));
        //stylePanel.setPreferredSize(new Dimension(350, 200));
        JTabbedPane tabbedPane = new JTabbedPane();

        for (Iterator i = propertyPanels.iterator(); i.hasNext();) {
            final PropertyPanel propertyPanel = (PropertyPanel) i.next();
            tabbedPane.add((Component) propertyPanel, propertyPanel.getTitle());
        }

        dialog.addRow(tabbedPane);
        tabbedPane.setSelectedComponent(find(propertyPanels,
                (String) context.getWorkbenchContext().getWorkbench()
                                .getBlackboard().get(LAST_TAB_KEY,
                    ((PropertyPanel) propertyPanels.iterator().next()).getTitle())));

        dialog.setVisible(true);
        context.getWorkbenchContext().getWorkbench().getBlackboard().put(LAST_TAB_KEY,
            ((PropertyPanel) tabbedPane.getSelectedComponent()).getTitle());

        
        if ((! dialog.wasOKPressed()) && styleChanged) {
        	if (oldStyleList != null) {  // restore the original styles
        		int j = 0;
        		for (int i = 0; i < layers.length; i++) {
        			Object layerable = layers[i];
        			if (layerable instanceof Layer) {
        				Layer layer = (Layer) layerable;
        				layer.setStyles( (Collection) oldStyleList.get(j++));
        			}
        		}
        	}       	
        }
        if ( !styleChanged /*existing variable of this class*/){
            reportNothingToUndoYet(context);
        }
        return true;
    }

    private Component find(Collection propertyPanels, String title) {
        for (Iterator i = propertyPanels.iterator(); i.hasNext();) {
        	PropertyPanel propertyPanel = (PropertyPanel) i.next();

            if (propertyPanel.getTitle().equals(title)) {
                return (Component) propertyPanel;
            }
        }

        Assert.shouldNeverReachHere();

        return null;
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);        
        return new MultiEnableCheck()
           .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
           .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
    }
    
    public static ColorThemingStyle getColorThemingStyleIfEnabled(Layer layer){
    	ColorThemingStyle someStyle = null;
    	final Collection currentStyles = layer.getStyles();
    	for (Iterator j = currentStyles.iterator(); j.hasNext();) {
    		Object style = j.next();
    		if (style instanceof ColorThemingStyle) {
    			if (((ColorThemingStyle) style).isEnabled())
    				someStyle = (ColorThemingStyle) style;
    		}
    	}
    	return someStyle;    	
    }
    
    public static int getAlpha(Layer layer) {
    	ColorThemingStyle cts = getColorThemingStyleIfEnabled(layer);
    	int alpha = 0;
    	if (cts == null)
    		alpha = layer.getBasicStyle().getAlpha();								
    	else 
    		alpha = cts.getDefaultStyle().getAlpha();
    	return alpha;
    }
    
    public static void setAlpha(Layer layer, int alpha) {
       	layer.getBasicStyle().setAlpha(alpha);								
    	ColorThemingStyle cts = getColorThemingStyleIfEnabled(layer);
    	if (cts != null) cts.setAlpha(alpha);
    }

    private class InfoPanel extends JPanel implements PropertyPanel {
    	//private BorderLayout borderLayout = new BorderLayout();
    	private JLabel label_Name_L = new JLabel();
        private JLabel label_NumItems_L = new JLabel();
        private JLabel label_NumPts_L = new JLabel();
        private JLabel label_GeoType_L = new JLabel();
        private JLabel label_NumAtts_L = new JLabel();
        private JLabel label_DSClass_L = new JLabel();
		private JLabel label_Charset_L = new JLabel();
        private JLabel label_Path_L = new JLabel();
        private JLabel label_Extent_L = new JLabel();
        
    	private JTextArea label_Name_R = new JTextArea();
        private JLabel label_NumItems_R = new JLabel();
        private JLabel label_NumPts_R = new JLabel();
        private JLabel label_GeoType_R = new JLabel();
        private JLabel label_NumAtts_R = new JLabel();
        private JLabel label_DSClass_R = new JLabel();
		private JLabel label_Charset_R = new JLabel();
        private JTextArea label_Path_R = new JTextArea();
		private JTextArea label_Extent_R = new JTextArea();

        private InfoPanel() {
            super(new GridBagLayout());
            
            label_Name_R.setFont(this.getFont());
            label_Name_R.setLineWrap(true);
            label_Name_R.setBackground(this.getBackground());
            label_Name_R.setSize(200,30);
            
            label_Path_R.setFont(this.getFont());
        	label_Path_R.setLineWrap(true);
        	label_Path_R.setBackground(this.getBackground());
        	label_Path_R.setSize(200,50);
        	
            label_Extent_R.setFont(this.getFont());
            label_Extent_R.setLineWrap(true);
            label_Extent_R.setBackground(this.getBackground());
            label_Extent_R.setSize(200,50);
            
            if (layers.length == 1) {
            	label_Name_L.setText(LAYER_NAME + ": ");
                label_Extent_L.setText(EXTENT + ": ");	
            }
            else
            	label_Name_L.setText(NUMBER_OF_LAYERS + ": ");
            
            label_NumItems_L.setText(NUMBER_OF_FEATURES + ": ");
            label_NumPts_L.setText(NUMBER_OF_POINTS + ": ");
            label_GeoType_L.setText(GEOMETRY_TYPE + ": ");
            label_NumAtts_L.setText(NUMBER_OF_ATTRIBUTES + ": ");
            label_DSClass_L.setText(DATASOURCE_CLASS + ": ");
			label_Charset_L.setText(CHARSET + ": ");
            label_Path_L.setText(SOURCE_PATH + ": ");
 
            
        	setInfo(layers);
        	
            //column one labels
            int row = 0;
            
            add(label_Name_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 5), 0, 0));
                
            add(label_NumItems_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 5), 0, 0));
            
            add(label_NumPts_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 5), 0, 0));
                
            add(label_GeoType_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 5), 0, 0));
                
            add(label_NumAtts_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 5), 0, 0));
                
            add(label_DSClass_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 5), 0, 0));

			// [Matthias Scholz 5.Sept.2010] Charset is only viewed if we have a Shapefile
          
			if (layers.length == 1 &&
			    layers[0].getDataSourceQuery() != null &&
			    layers[0].getDataSourceQuery().getDataSource().getClass().getName().equals("com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource$Shapefile")) {
				add(label_Charset_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 5), 0, 0));
			}
			

                
            add(label_Path_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
                    new Insets(10, 0, 0, 5), 0, 0));
            
			if (layers.length == 1){
				add(label_Extent_L, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 5), 0, 0));
			}
                
            //column two info
            row = 0;
            add(label_Name_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
                
            add(label_NumItems_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
                
            add(label_NumPts_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            
            add(label_GeoType_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
                
            add(label_NumAtts_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
                
            add(label_DSClass_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));

			// [Matthias Scholz 5.Sept.2010] Charset is only viewed if we have a Shapefile
			if (layers.length == 1 && 
			    layers[0].getDataSourceQuery() != null &&
			    layers[0].getDataSourceQuery().getDataSource().getClass().getName().equals("com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource$Shapefile")) {
				add(label_Charset_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 0));
				}
                
            add(label_Path_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                    new Insets(10, 0, 0, 0), 0, 0));
            
			if (layers.length == 1){
	            add(label_Extent_R, new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
	                    new Insets(10, 0, 0, 0), 0, 0));
			}
        }

        public String getTitle() {
            return INFO;
        }

        public void updateStyles() {
        }

        public String validateInput() {
            return null;
        }
        
        private void setInfo(Layer[] layers) {
        	if (layers.length == 1)
        		label_Name_R.setText(layers[0].getName());
        	else
        		label_Name_R.setText("" + layers.length);
        	
        	String sourcePath = NOT_SAVED;
        	String geoClass = "";
        	String sourceClass = "";
        	int numFeatures = 0;
        	int numPts = 0;
        	int numAtts = 0;
        	Geometry geo = null;
        	boolean multipleGeoTypes = false;
        	boolean multipleSourceTypes = false;
        	
        	for (int l = 0; l < layers.length; l++) {
	        	
	        	FeatureCollectionWrapper fcw = layers[l].getFeatureCollectionWrapper();
	        	numFeatures += fcw.size();
	        	numAtts += fcw.getFeatureSchema().getAttributeCount() - 1;
	        	
	        	for (Iterator i = fcw.getFeatures().iterator(); i.hasNext();) {
	        		geo = ((Feature)i.next()).getGeometry();
	        		
	        		if (geo != null) {
	        			numPts += geo.getNumPoints();
	        			
	        			if (geoClass.equals(""))
	        				geoClass = geo.getClass().getName();
	        			else if (! geo.getClass().getName().equals(geoClass))
	        				multipleGeoTypes = true;
	        		}
	        	}
	        		
	        	DataSourceQuery dsq = layers[l].getDataSourceQuery();
	        	
	        	if (dsq != null) {
	        		String dsqSourceClass = dsq.getDataSource().getClass().getName();
        			
	        		if (sourceClass.equals(""))
        				sourceClass = dsqSourceClass;
        			else if (! sourceClass.equals(dsqSourceClass))
        				multipleSourceTypes = true;
	        		
	         		Object fnameObj = dsq.getDataSource().getProperties().get("File");
	         		
	        		if (fnameObj != null)
	        			sourcePath = fnameObj.toString();
	        	}
        	} //end of looping through all the layers
        	
       	//determine the geoClass
        	if (numFeatures == 0)
        		geoClass = NO_FEATURES;
        	else if (multipleGeoTypes)
        		geoClass = MULTIPLE_GEOMETRY_TYPES;
        	else if (geoClass.equals(""))
        		geoClass = NULL_GEOMETRIES;
        	else {
	        	int dotPos = geoClass.lastIndexOf(".");
	        	
	        	if (dotPos > 0)
	        		geoClass = geoClass.substring(dotPos + 1);
        	}
        	
        	//determine the sourceClass
        	if (sourceClass.equals(""))
        		sourceClass = NOT_SAVED;
        	else if (multipleSourceTypes)
        		sourceClass = MULTIPLE_SOURCE_TYPES;
        	else {
	    		int dotPos = sourceClass.lastIndexOf(".");
	    		
	    		if (dotPos > 0)
	    			sourceClass = sourceClass.substring(dotPos + 1);
	    		
	    		dotPos = sourceClass.lastIndexOf("$");
	    		
	    		if (dotPos > 0)
	    			sourceClass = sourceClass.substring(dotPos + 1);
        	}
        	
        	label_GeoType_R.setText(geoClass);
        	
        	if (layers.length == 1) {
        		label_NumItems_R.setText("" + numFeatures);
        		label_NumPts_R.setText("" + numPts);
        		label_NumAtts_R.setText("" + numAtts);
        	}
        	else {
        		DecimalFormat df = new DecimalFormat("0.0");
        		double numLayers = layers.length;
        		double avgNumFeatures = numFeatures / numLayers;
        		double avgNumPts = numPts / numLayers;
        		double avgNumAtts = numAtts / numLayers;
        		label_NumItems_R.setText(numFeatures + "  " + df.format(avgNumFeatures) + AVERAGE_PER_LAYER);
        		label_NumPts_R.setText(numPts + "  " + df.format(avgNumPts) + AVERAGE_PER_LAYER);
        		label_NumAtts_R.setText(df.format(avgNumAtts) + AVERAGE_PER_LAYER);
        	}
         	
        	label_DSClass_R.setText(sourceClass);
			// fetch the charset from the layer properties
			String charsetName = null;
			DataSourceQuery dsq = layers[0].getDataSourceQuery();
			if (dsq != null) {
			    Map properties = dsq.getDataSource().getProperties();
			    charsetName = (String) properties.get("charset");
			    // if the layer do not have the charset property, set with default charset
			    if (charsetName == null) {
				    charsetName = Charset.defaultCharset().displayName();
				    properties.put("charset", charsetName);
			    }
			}
			else {
			    // if the layer does not come from a datasource
			    charsetName = Charset.defaultCharset().displayName();
			}
			// and finaly set the text of the label
			label_Charset_R.setText(charsetName);
        	label_Path_R.setText(sourcePath);
        	
        	if ((layers.length > 1) && (! sourcePath.equalsIgnoreCase(NOT_SAVED)))
        		label_Path_R.setText(MULTIPLE_SOURCES);
        	
        	if ((layers.length == 1)) {
        		String ext = " " + XMIN + ":" + extent.getMinX() +
        		           "\n " + YMIN + ":" + extent.getMinY() +
        		           "\n " + XMAX + ":" + extent.getMaxX() +
        		           "\n " + YMAX + ":" + extent.getMaxY();
        		label_Extent_R.setText(ext);
        		
        	}
        	
        }
    }
    
    private class StylePanel extends JPanel implements PropertyPanel {
    	private JSlider transparencySlider = new JSlider();

    	private StylePanel() {
	        Box box = new Box(BoxLayout.Y_AXIS);
	        JLabel transparencySliderLabel =
	            new JLabel(PROPORTIONAL_TRANSPARENCY_ADJUSTER, SwingConstants.CENTER);
	        transparencySliderLabel.setAlignmentX(CENTER_ALIGNMENT);
    		box.add(transparencySliderLabel);
	        Hashtable labelTable = new Hashtable();
	        labelTable.put(new Integer(0), new JLabel("100"));
	        labelTable.put(new Integer(10), new JLabel("80"));
	        labelTable.put(new Integer(20), new JLabel("60"));
	        labelTable.put(new Integer(30), new JLabel("40"));
	        labelTable.put(new Integer(40), new JLabel("20"));
	        labelTable.put(new Integer(50), new JLabel("0"));
	        labelTable.put(new Integer(60), new JLabel("20"));
	        labelTable.put(new Integer(70), new JLabel("40"));
	        labelTable.put(new Integer(80), new JLabel("60"));
	        labelTable.put(new Integer(90), new JLabel("80"));
	        labelTable.put(new Integer(100), new JLabel("100"));
	        transparencySlider.setPreferredSize(new Dimension(250, 50));
	        transparencySlider.setPaintLabels(true);
	        transparencySlider.setPaintTicks(true);
	        transparencySlider.setLabelTable(labelTable);
	        transparencySlider.setMajorTickSpacing(10);
	        transparencySlider.setMinimum(0);
	        transparencySlider.setMaximum(100);
	        transparencySlider.setValue(50);
	        box.add(transparencySlider);
            add(box);
	        transparencySlider.addChangeListener(new ChangeListener() {
	            public void stateChanged(ChangeEvent e) {
	                JSlider source = (JSlider)e.getSource();
	                
	                if (!source.getValueIsAdjusting()) {
	                    int sliderVal = (int)source.getValue();
	                    double percentChg;
	                    
		                for (int i = 0; i < layers.length; i++) {
			    	        Layer layer = layers[i];
			                int currTrans = currTransArray[i];
		                    double newTrans = currTrans;
		                    
		                    if (sliderVal < 50) {
		                    	percentChg = ((50 - sliderVal) / 50d);
		                    	newTrans = currTrans - (currTrans * percentChg);
		                    }
		                    else if (sliderVal > 50) {
		                    	percentChg = (sliderVal - 50) / 50d;
		                    	newTrans = currTrans + ((255 - currTrans) * percentChg);
		                    }
		                    
			    	        setAlpha(layer, 255 - (int)newTrans);
		                    layer.fireAppearanceChanged();
		                    styleChanged = true;
		                }
	                }    
	            }
	        });
	    }
	    
	    public String getTitle() {
	        return STYLES;
	    }
	
	    public void updateStyles() {
	    }
	
	    public String validateInput() {
	        return null;
	    }
    }
    
}
