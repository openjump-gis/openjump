

/** 
 * created:  		21. Sept.2020
 * 
 * @author Giuseppe Aruta
 * @TODO clip the vectors to the envelope before
 * validating them. THis can make the process faster
 * 
 * 
 * @description: A tool to rasterize a vector layer.
 * - make valid the layer
 * - union by the value selected for rasterizing
 * - rasterize the vectors limiting into an envelope
 * 	
 */

package org.openjump.core.ui.plugin.tools.generate;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.algorithms.GenericRasterAlgorithm;
import org.openjump.core.rasterimage.algorithms.RasterizeAlgorithm;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.sigle.utilities.geom.FeatureCollectionUtil;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import de.latlon.deejump.wfs.jump.WFSLayer;
import it.betastudio.adbtoolbox.libs.FileOperations;

 
 
public class RasterizePlugIn extends AbstractPlugIn
        implements ThreadedPlugIn {

	 	private Layer layer;
	    private JTextField cellYextFiels;
	    private JComboBox<Object> layerComboBox = new JComboBox<Object>();
	   
	    private JLabel cutLayerLabel;
	    private DefaultComboBoxModel<Object> sourceLayerComboBoxModel = new DefaultComboBoxModel<Object>();
	    private String selAttribute = null;
	    private String ATTRIBUTE = GenericNames.SELECT_ATTRIBUTE;
	    private String path;
	    double cellValue;
	    private final ImageIcon icon16 = IconLoader
	            .icon("fugue/folder-horizontal-open_16.png");
	    JTextField jTextField_RasterOut = new JTextField();
	    private final String OUTPUT_FILE = I18N
	            .get("driver.DriverManager.file-to-save");
	    private final String CHECK = I18N.get("ui.GenericNames.chech-field");
	    private final static String LAYER             = I18N.get("ui.plugin.analysis.DissolvePlugIn.source-layer");
	    private final static String TARGET_LAYER = I18N
	            .get("ui.plugin.raster.CropWarpPlugIn.target-layer");
	    private final static String  CELL_SIZE = I18N
	            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_cell");
	    public static final Icon ICON = IconLoader.icon("rasterize.png");   
	  @Override
	 public boolean execute(PlugInContext context) throws Exception {
	        MultiInputDialog dialog = new MultiInputDialog(
	                context.getWorkbenchFrame(), getName(), true);
	        initDialog(dialog, context);
	        dialog.setVisible(true);
	        if (!dialog.wasOKPressed()) {
	            return false;
	        }
	        getDialogValues(dialog);
	        return true;
	    }
	  
	  
	 @Override
	public String getName() {
	        return "Rasterize vector layer";
	    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.TOOLS,MenuNames.TOOLS_GENERATE},
                getName(), false,ICON,
                createEnableCheck(context.getWorkbenchContext()), -1);
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createTaskWindowMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
    
    private final EnableCheck[] saveCheck = new EnableCheck[] { new EnableCheck() {
        @Override
        public String check(JComponent component) {
            return jTextField_RasterOut.getText().isEmpty() ? CHECK
                    .concat(": ").concat(OUTPUT_FILE) : null;
        }
    } };
  
	@SuppressWarnings("unchecked")
	private void initDialog(final MultiInputDialog dialog, PlugInContext context) {
		 dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0),
	                context.getLayerManager());

	     final List<String> list = AttributeTypeFilter.NUMERIC_FILTER
	             .filter(context.getCandidateLayer(0));
	     final String val = list.size() > 0 ? list.get(0) : null;
	     final JComboBox<String> jcb_attribute = dialog.addComboBox(ATTRIBUTE,
	                val, list, ATTRIBUTE);
	     cellYextFiels =dialog.addDoubleField(CELL_SIZE, 5, 8);
	     sourceLayerComboBoxModel.removeAllElements();
	     layerComboBox.setModel(sourceLayerComboBoxModel);
	     LayerNameRenderer layerListCellRenderer = new LayerNameRenderer();
	     layerListCellRenderer.setCheckBoxVisible(false);
	     layerListCellRenderer.setProgressIconLabelVisible(false);
	     layerComboBox.setRenderer(layerListCellRenderer);
	     final List<Layerable> layerables = JUMPWorkbench.getInstance()
	                .getContext().getLayerManager().getLayerables(Layerable.class);
	     for (Iterator<Layerable> i = layerables.iterator(); i.hasNext();) {
	        	Layerable layer = i.next();
                sourceLayerComboBoxModel.addElement(layer);
            }
	     layerComboBox.setSelectedItem(layerables.get(0));
	     layerComboBox.setSize(200, layerComboBox.getPreferredSize().height);
	     cutLayerLabel = new JLabel(TARGET_LAYER);
	     JPanel pan = new JPanel(new GridBagLayout());
	     FormUtils.addRowInGBL(pan, 0, 0, cutLayerLabel, layerComboBox);
	     dialog.addRow("base", pan, null, null);
	     dialog.getComboBox(LAYER).addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                final List<String> list = AttributeTypeFilter.NUMERIC_FILTER
	                        .filter(dialog.getLayer(LAYER));
	               jcb_attribute.setModel(new DefaultComboBoxModel<>(list
	                        .toArray(new String[0])));
	            }
	      });
	     layerComboBox.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	            	final Layerable slayer = (Layerable) layerComboBox
	                        .getSelectedItem();
	            	if (slayer instanceof RasterImageLayer) {
	            		cellYextFiels.setText(""+((RasterImageLayer) slayer).getMetadata().getOriginalCellSize());
	            	}
	            }
	      });
	   
         final FileNameExtensionFilter filter;
          filter = new FileNameExtensionFilter("TIF", "tif");
         dialog.addRow("Save", createOutputFilePanel(filter), saveCheck, null);
         GUIUtil.centreOnWindow(dialog);
    }

	Envelope envWanted, fix;
	
	private void getCroppedEnvelope(Layer layer) {
        envWanted = new Envelope();
        final Layerable slayer = (Layerable) layerComboBox
                    .getSelectedItem();
            if (slayer instanceof WMSLayer) {
                envWanted.expandToInclude(((WMSLayer) slayer).getEnvelope());
            } else if (slayer instanceof WFSLayer) {
                envWanted.expandToInclude(((WFSLayer) slayer)
                        .getFeatureCollectionWrapper().getEnvelope());
            } else if (slayer instanceof Layer) {
                envWanted.expandToInclude(((Layer) slayer)
                        .getFeatureCollectionWrapper().getEnvelope());
            } else if (slayer instanceof RasterImageLayer) {
                envWanted.expandToInclude(((RasterImageLayer) slayer)
                        .getWholeImageEnvelope());
            }
          fix = envWanted.intersection(layer.getFeatureCollectionWrapper().getEnvelope());
    }
	
	
	
	 private void getDialogValues(MultiInputDialog dialog) {
	        layer = dialog.getLayer(LAYER);
	        cellValue = dialog.getDouble(CELL_SIZE);
	        selAttribute = dialog.getText(ATTRIBUTE);
	        path = getOutputFilePath();
	        final int i = path.lastIndexOf('.');
	        if (i > 0) {
	            path = path.substring(0, path.length() - path.length() + i);
	        } 
	   }

	 
	   public String getOutputFilePath() {
	        return jTextField_RasterOut.getText();
	    }
	 
	 
    
    
	@Override
	public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
		   monitor.allowCancellationRequests();
           getCroppedEnvelope(layer);
	       final File outFile = FileUtil.addExtensionIfNone(new File(path), "tif");  
	       FeatureCollection fCollection = layer.getFeatureCollectionWrapper();
	       Collection<Feature> inputC = fCollection.getFeatures();
	       FeatureSchema schema = fCollection.getFeatureSchema();
		   FeatureDataset inputFC = new FeatureDataset(inputC, schema);
	       monitor.report(I18N.get("org.openjump.core.ui.plugin.tools.MakeValidPlugIn")+"...");
	       FeatureCollectionUtil.validFeatureCollection(inputFC) ;
	       monitor.report(I18N.get("ui.plugin.analysis.DissolvePlugIn")+"...");
	       FeatureCollectionUtil.unionByAttributeValue(inputFC, selAttribute);
	       monitor.report("Rasterize...");
	       RasterizeAlgorithm.Rasterize_Sextante(outFile, fix, inputFC, selAttribute, cellValue, -99999.0D);
		   GenericRasterAlgorithm IO = new GenericRasterAlgorithm();
	       String catName = StandardCategoryNames.WORKING;
	       try {
	           catName = ((Category) context.getLayerNamePanel()
	                    .getSelectedCategories().toArray()[0]).getName();
	       } catch (final RuntimeException e1) {
	       }
	      IO.load(outFile, catName);
	     }
	
	
	
	
	
    public JPanel createOutputFilePanel(FileNameExtensionFilter filter) {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel = new javax.swing.JPanel();
        final JLabel jLabel3 = new javax.swing.JLabel();
        jTextField_RasterOut = new JTextField();
        final JButton jButton_Dir = new JButton();
        jTextField_RasterOut.setText("");
        jButton_Dir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                File outputPathFile = null;
                final JFileChooser chooser = new GUIUtil.FileChooserWithOverwritePrompting();
                chooser.setDialogTitle(getName());
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                chooser.setSelectedFile(FileOperations.lastVisitedFolder);
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                GUIUtil.removeChoosableFileFilters(chooser);
                chooser.setFileFilter(filter);
                final int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    outputPathFile = FileUtil.removeExtensionIfAny(chooser
                            .getSelectedFile());
                    jTextField_RasterOut.setText(outputPathFile.getPath()
                            .concat(".tif"));
                    FileOperations.lastVisitedFolder = outputPathFile;
                }
            }
        });
        jLabel3.setText(OUTPUT_FILE);
        jTextField_RasterOut.setEditable(false);
        jButton_Dir.setIcon(icon16);
        jTextField_RasterOut.setPreferredSize(new Dimension(250, 20));
        FormUtils.addRowInGBL(jPanel, 3, 0, OUTPUT_FILE, jTextField_RasterOut);
        FormUtils.addRowInGBL(jPanel, 3, 2, jButton_Dir);
        return jPanel;
    }
	
}
