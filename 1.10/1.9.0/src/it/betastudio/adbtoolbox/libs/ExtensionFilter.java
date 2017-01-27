package it.betastudio.adbtoolbox.libs;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;

public class ExtensionFilter extends FileFilter {

    /*
     * 2015_03_18 Giuseppem Aruta <giuseppe_aruta[at]yahoo.it This class derives
     * from AdBToolbox 1.7
     */
    private static String TYPE_UNKNOWN = "Type Unknown";
    private static String HIDDEN_FILE = "Hidden File";

    private Hashtable<String, ExtensionFilter> filters = null;
    private String description = null;
    private String fullDescription = null;
    private boolean useExtensionsInDescription = true;

    /**
     * Creates a file filter. If no filters are added, then all files are
     * accepted.
     * 
     * @see #addExtension
     */
    public ExtensionFilter() {
        this.filters = new Hashtable<String, ExtensionFilter>();
    }

    /**
     * Creates a file filter that accepts files with the given extension.
     * Example: new ExtensionFilter("jpg");
     * 
     * @see #addExtension
     */
    public ExtensionFilter(String extension) {
        this(extension, null);
    }

    /**
     * Creates a file filter that accepts the given file type. Example: new
     * ExtensionFilter("jpg", "JPEG Image Images");
     * 
     * Note that the "." before the extension is not needed. If provided, it
     * will be ignored.
     * 
     * @see #addExtension
     */
    public ExtensionFilter(String extension, String description) {
        this();
        if (extension != null)
            addExtension(extension);
        if (description != null)
            setDescription(description);
    }

    /**
     * Creates a file filter from the given string array. Example: new
     * ExtensionFilter(String {"gif", "jpg"});
     * 
     * Note that the "." before the extension is not needed adn will be ignored.
     * 
     * @see #addExtension
     */
    public ExtensionFilter(String[] filters) {
        this(filters, null);
    }

    /**
     * Creates a file filter from the given string array and description.
     * Example: new ExtensionFilter(String {"gif", "jpg"},
     * "Gif and JPG Images");
     * 
     * Note that the "." before the extension is not needed and will be ignored.
     * 
     * @see #addExtension
     */
    public ExtensionFilter(String[] filters, String description) {
        this();
        for (int i = 0; i < filters.length; i++) {
            // add filters one by one
            addExtension(filters[i]);
        }
        if (description != null)
            setDescription(description);
    }

    /**
     * Return true if this file should be shown in the directory pane, false if
     * it shouldn't.
     * 
     * Files that begin with "." are ignored.
     * 
     * @see #getExtension
     * @see FileFilter#accepts
     */
    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if (extension != null && filters.get(getExtension(f)) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the extension portion of the file's name .
     * 
     * @see #getExtension
     * @see FileFilter#accept
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    /**
     * Adds a filetype "dot" extension to filter against.
     * 
     * For example: the following code will create a filter that filters out all
     * files except those that end in ".jpg" and ".tif":
     * 
     * ExtensionFilter filter = new ExtensionFilter();
     * filter.addExtension("jpg"); filter.addExtension("tif");
     * 
     * Note that the "." before the extension is not needed and will be ignored.
     */
    public void addExtension(String extension) {
        if (filters == null) {
            filters = new Hashtable<String, ExtensionFilter>(5);
        }
        filters.put(extension.toLowerCase(), this);
        fullDescription = null;
    }

    /**
     * Returns the human readable description of this filter. For example:
     * "JPEG and GIF Image Files (*.jpg, *.gif)"
     * 
     * @see setDescription
     * @see setExtensionListInDescription
     * @see isExtensionListInDescription
     * @see FileFilter#getDescription
     */
    public String getDescription() {
        if (fullDescription == null) {
            if (description == null || isExtensionListInDescription()) {
                fullDescription = description == null ? "(" : description
                        + " (";
                // build the description from the extension list
                Enumeration extensions = filters.keys();
                if (extensions != null) {
                    fullDescription += "." + (String) extensions.nextElement();
                    while (extensions.hasMoreElements()) {
                        fullDescription += ", ."
                                + (String) extensions.nextElement();
                    }
                }
                fullDescription += ")";
            } else {
                fullDescription = description;
            }
        }
        return fullDescription;
    }

    /**
     * Sets the human readable description of this filter. For example:
     * filter.setDescription("Gif and JPG Images");
     * 
     * @see setDescription
     * @see setExtensionListInDescription
     * @see isExtensionListInDescription
     */
    public void setDescription(String description) {
        this.description = description;
        fullDescription = null;
    }

    /**
     * Determines whether the extension list (.jpg, .gif, etc) should show up in
     * the human readable description.
     * 
     * Only relevent if a description was provided in the constructor or using
     * setDescription();
     * 
     * @see getDescription
     * @see setDescription
     * @see isExtensionListInDescription
     */
    public void setExtensionListInDescription(boolean b) {
        useExtensionsInDescription = b;
        fullDescription = null;
    }

    /**
     * Returns whether the extension list (.jpg, .gif, etc) should show up in
     * the human readable description.
     * 
     * Only relevent if a description was provided in the constructor or using
     * setDescription();
     * 
     * @see getDescription
     * @see setDescription
     * @see setExtensionListInDescription
     */
    public boolean isExtensionListInDescription() {
        return useExtensionsInDescription;
    }
}