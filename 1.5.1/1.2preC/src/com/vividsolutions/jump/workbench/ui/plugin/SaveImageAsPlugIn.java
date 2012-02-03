package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class SaveImageAsPlugIn extends ExportImagePlugIn {
    //ImageIO doesn't know about the "gif" format. I guess it's a copyright
    // issue [Jon Aquino 11/6/2003]
    //Don't use TYPE_INT_ARGB for jpegs -- they will turn pink [Jon Aquino
    // 11/6/2003]
    
    private List myFileFilters = Arrays.asList(new Object[]{
        createFileFilter("PNG - Portable Network Graphics", "png",
                BufferedImage.TYPE_INT_ARGB),
        createFileFilter("JPEG - Joint Photographic Experts Group", "jpg",
                BufferedImage.TYPE_INT_RGB)});
                
                
    private JFileChooser fileChooser = null;
    private WorkbenchContext workbenchContext;
    
    
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
        }
        return fileChooser;
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
        if (JFileChooser.APPROVE_OPTION != getFileChooser()
                .showSaveDialog(context.getWorkbenchFrame())) {
            return false;
        }
        MyFileFilter fileFilter = (MyFileFilter) getFileChooser()
                .getFileFilter();
        BufferedImage image = image(context.getLayerViewPanel());
        String filename = addExtension(getFileChooser().getSelectedFile()
                .getPath(), fileFilter.getFormat());
        save(image, fileFilter.getFormat(), new File(filename));
        PersistentBlackboardPlugIn.get(workbenchContext)
                .put(FORMAT_KEY, fileFilter.getFormat());
        PersistentBlackboardPlugIn.get(workbenchContext)
                .put(LAST_FILENAME_KEY, filename);
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
                .add(checkFactory
                        .createWindowWithLayerViewPanelMustBeActiveCheck()).add(new EnableCheck() {
                    public String check(JComponent component) {
                        //Need Java 1.4's ImageIO class [Jon Aquino 11/6/2003]
                    return !java14OrNewer()
                            ? "This feature requires Java 1.4 or newer"
                            : null;
                }
                });
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