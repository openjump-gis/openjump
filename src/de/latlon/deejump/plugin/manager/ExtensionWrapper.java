package de.latlon.deejump.plugin.manager;

import java.util.List;

import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/* renamed into ExtensionModel, as JUMP tries to configure() all classes found, which end with "Extension"*/
public class ExtensionWrapper  {
    
    private String name;
    private String title;
    private String author;
    private String version;
    private String jumpVersion;
    private String category;
    private String description;
    private List resourcesList;
    
    private boolean installed = false;
    
    public ExtensionWrapper( 
            String name,
            String title,
            String author,
            String version,
            String jumpVersion,
            String category,
            String description,
            List resourcesList) 
    {
        this.name = name; 
        this.title = title;
        this.author = author;
        this.version = version;
        this.jumpVersion = jumpVersion;
        this.category = category;
        this.description = description;
        this.resourcesList = resourcesList;
    }

    public void configure( PlugInContext context ) throws Exception {
        //dummy
    }
    
    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getJumpVersion() {
        return jumpVersion;
    }

    public String getName() {
        return name;
    }

    public List getResourceList() {
        return resourcesList;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append( "CataloguedExtension { " )
        .append( name ).append( ", ")
        .append( title ).append( ", ")
        .append( author ).append( ", ")
        .append( "version: " ).append( version ).append( ", ")
        .append( "JUMP version: " ).append( jumpVersion ).append( ", ")
        .append( "description: '" ).append( description ).append( "', ")
        .append( "resources = " ).append( resourcesList );
        sb.append( "}" );
        
        return sb.toString();
            
    }
    
    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean selected) {
        this.installed = selected;
    }

}
