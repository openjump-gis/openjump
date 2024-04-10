package com.vividsolutions.jump.workbench.imagery.openjpeg;

import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;
import com.vividsolutions.jump.workbench.model.Prioritized;
import de.digitalcollections.openjpeg.imageio.OpenJp2ImageReaderSpi;

import javax.imageio.ImageReader;
import java.io.IOException;

public class OpenJpegImageFactory implements ReferencedImageFactory, Prioritized {
    static final String TYPE_NAME = "JPEG2000";
    static final String DESCRIPTION = "JPEG 2000 (via openjpeg and imageio-jnr)";
    static final String[] EXTENSIONS = new String[]{ "jp2","j2k","j2c","jpc","jpx","jpf" };

    //final static String sNotInstalled = I18N.getInstance().get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.not-installed");
    final static String sNotInstalled = "OpenJpeg JP2 driver is not installed";
    private static Boolean available = null;

    public String getTypeName() {
        return TYPE_NAME;
    }
    
    public String getDescription() {
        return DESCRIPTION;
    }

    public String[] getExtensions() {
        return EXTENSIONS;
    }

    public int getPriority() {
        return 5;
    }

    public OpenJpegImageFactory() {
    }


    public ReferencedImage createImage(String location) throws Exception {
        return new OpenJpegImage(location);
    }

    public boolean isEditableImage(String location) {
        return false;
    }

    // cache availability
    public boolean isAvailable(WorkbenchContext context) {
        if (available != null)
            return available;

        available = _isAvailable(context);
        if (!available)
            Logger.info("OpenJpeg loader will be unavailable.");

        return available;
    }

    private boolean _isAvailable(WorkbenchContext context) {
        try {
            ImageReader reader = new OpenJp2ImageReaderSpi().createReaderInstance();
            return reader != null;
        } catch(IOException exception) {
            Logger.error(exception);
            return false;
        }
    }

}
