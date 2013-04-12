package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.plugin.file.LayerPrinter2;
import org.openjump.core.ui.plugin.file.WorldFileWriter;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.ValidatingTextField;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Save the view to a PNG or a JPG image file.
 * The exported image can have a size different from the original view.
 */
public class SaveImageAsPlugIn extends ExportImagePlugIn {
    //ImageIO doesn't know about the "gif" format. I guess it's a copyright
    // issue [Jon Aquino 11/6/2003]
    //Don't use TYPE_INT_ARGB for jpegs -- they will turn pink [Jon Aquino
    // 11/6/2003]
    //ImageIO can probably write gif images from java 6, but we do we really 
    // need that ? [mmichaud 2012-09-02]
    
    private List myFileFilters = Arrays.asList(new Object[]{
        createFileFilter("PNG - Portable Network Graphics", "png",
                BufferedImage.TYPE_INT_ARGB),
        createFileFilter("JPEG - Joint Photographic Experts Group", "jpg",
                BufferedImage.TYPE_INT_RGB)});
                
                
    private JFileChooser fileChooser = null;
    private WorkbenchContext workbenchContext;
    private JCheckBox worldFileCheckBox = null;
    private JLabel pixelSizeLabel = new JLabel(I18N.get("ui.plugin.SaveImageAsPlugIn.width-in-pixels"));
    private final ImageIcon icon = IconLoader.icon("Box.gif");
    private Geometry fence = null;
    private boolean fenceFound = false;
    
    private ValidatingTextField pixelSizeField = new ValidatingTextField("9999",5,
    		new ValidatingTextField.Validator() {
    	public boolean isValid(String text) {
    		if (text.length() == 0) {
    			return true;
    		}
    		try {
    			int i = Integer.parseInt(text);
    			return i<=4000;
    		} catch (NumberFormatException e) {
    			return false;
    		}
    	}
    });

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new GUIUtil.FileChooserWithOverwritePrompting() {
                protected File selectedFile() {
                    return new File(addExtension(
                            super.selectedFile().getPath(),
                            ((MyFileFilter) getFileFilter()).getFormat()));
                }
            };
            fileChooser.setDialogTitle(I18N.get("ui.plugin.SaveImageAsPlugIn.save-image"));
            //Remove *.* [Jon Aquino 11/6/2003]
            GUIUtil.removeChoosableFileFilters(fileChooser);
            Map formatToFileFilterMap = new HashMap();
            for (Iterator i = myFileFilters.iterator(); i.hasNext(); ) {
                MyFileFilter fileFilter = (MyFileFilter) i.next();
                fileChooser.addChoosableFileFilter(fileFilter);
                formatToFileFilterMap.put(fileFilter.getFormat(), fileFilter);
            }
            String lastFilename = (String) PersistentBlackboardPlugIn
                    .get(workbenchContext).get(LAST_FILENAME_KEY);
            if (lastFilename != null) {
                fileChooser.setSelectedFile(new File(lastFilename));
            }
            fileChooser.setFileFilter((FileFilter) formatToFileFilterMap.get(
                    PersistentBlackboardPlugIn.get(workbenchContext)
                            .get(FORMAT_KEY, "png")));
            
            Box box = new Box(BoxLayout.Y_AXIS);
            JPanel jPanelSize = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JPanel jPanelWF   = new JPanel(new FlowLayout(FlowLayout.LEFT));
            worldFileCheckBox = new javax.swing.JCheckBox();
            worldFileCheckBox.setText(I18N.get("ui.plugin.SaveImageAsPlugIn.write-world-file"));
            if (fence != null){
            	JLabel fenceIcon = new JLabel(icon);
            	jPanelSize.add(fenceIcon);
            }
            jPanelSize.add(pixelSizeLabel);
            jPanelSize.add(pixelSizeField);
            jPanelWF.add(worldFileCheckBox);
            box.add(jPanelSize);
            box.add(jPanelWF);
            box.add(Box.createRigidArea(new Dimension(5,180)));
            fileChooser.setAccessory(box);
        }
        return fileChooser;
    }
    
    private int getPixelSize() {
    	String text = pixelSizeField.getText();
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return 800;  //some reasonable default
		}   	
    }
    
    private MyFileFilter createFileFilter(String description, String format,
            int bufferedmageType) {
        return new MyFileFilter(description, format);
    }
    
    
    private static class MyFileFilter extends FileFilter {
        private FileFilter fileFilter;
        private String format;
        public MyFileFilter(String description, String format) {
            fileFilter = GUIUtil.createFileFilter(description,
                    new String[]{format});
            this.format = format;
        }
        public boolean accept(File f) {
            return fileFilter.accept(f);
        }

        public String getDescription() {
            return fileFilter.getDescription();
        }

        public String getFormat() {
            return format;
        }
    }
    
    
    private static final String FORMAT_KEY = "FORMAT";
    private static final String LAST_FILENAME_KEY = "LAST FILENAME";
    
    
    public boolean execute(PlugInContext context) throws Exception {
        this.workbenchContext = context.getWorkbenchContext();
   		fence = context.getLayerViewPanel().getFence();	
   		fenceFound = (fence != null);
   		if (fenceFound){
   			pixelSizeField.setText("800");
   		}
   		else {
   			pixelSizeField.setText(context.getLayerViewPanel().getWidth() + "");}
        if (JFileChooser.APPROVE_OPTION != getFileChooser()
                .showSaveDialog(context.getWorkbenchFrame())) {
           fileChooser = null; //rebuild next invocation
           return false;
        }
        MyFileFilter fileFilter = (MyFileFilter) getFileChooser()
                .getFileFilter();
        BufferedImage image;
        LayerViewPanel viewPanel =context.getLayerViewPanel();
		Envelope envelope = null;
        if (!fenceFound && (getPixelSize() == context.getLayerViewPanel().getWidth())) {
            image = image(viewPanel);        	
        }
        else {
        	LayerPrinter2 layerPrinter = new LayerPrinter2();
 			if (fenceFound)
			{
 				envelope = fence.getEnvelopeInternal(); 
 				String fenceLayerName = I18N.get("model.FenceLayerFinder.fence");
 				Layer fenceLayer = workbenchContext.getLayerNamePanel().getLayerManager().getLayer(fenceLayerName);
				fenceLayer.setVisible(false);
			}
			else {
				envelope = workbenchContext.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();
			}
			image = layerPrinter.print(context.getLayerManager().getLayerables(Layerable.class), envelope, getPixelSize());
        	viewPanel = layerPrinter.getLayerViewPanel();
        }
        String filename = addExtension(getFileChooser().getSelectedFile()
                .getPath(), fileFilter.getFormat());
        File imageFile = new File(filename);
        save(image, fileFilter.getFormat(), imageFile);
        PersistentBlackboardPlugIn.get(workbenchContext)
                .put(FORMAT_KEY, fileFilter.getFormat());
        PersistentBlackboardPlugIn.get(workbenchContext)
                .put(LAST_FILENAME_KEY, filename);
        if ((worldFileCheckBox != null) && (worldFileCheckBox.isSelected()))
        	WorldFileWriter.writeWorldFile( imageFile,  viewPanel );
        fileChooser = null; //rebuild next invocation
        return true;
    }

    
    private void save(RenderedImage image, String format, File file)
            throws IOException {
        boolean writerFound = ImageIO.write(image, format, file);
        Assert.isTrue( writerFound, I18N.get("ui.plugin.SaveImageAsPlugIn.cannot-find-writer-for-image-format")+" '"
                + format + "'");
    }
    
    
    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());
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