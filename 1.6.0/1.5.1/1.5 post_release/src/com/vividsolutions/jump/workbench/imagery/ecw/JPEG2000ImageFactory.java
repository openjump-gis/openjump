package com.vividsolutions.jump.workbench.imagery.ecw;

public class JPEG2000ImageFactory extends ECWImageFactory{
    static final String TYPE_NAME = "JPEG2000";
    static final String DESCRIPTION = "JPEG 2000 (via ecw3.3)";
    static final String[] EXTENSIONS = new String[]{ "jp2","j2k","j2c","jpc","jpx","jpf" };
    
    public String getTypeName() {
        return TYPE_NAME;
    }
    
    public String getDescription() {
        return DESCRIPTION;
    }

    public String[] getExtensions() {
        return EXTENSIONS;
    }
}
