package de.latlon.deejump.plugin.manager;

import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class ExtensionManagerExtension extends Extension {
    
    public void configure(PlugInContext context) throws Exception {
        new ExtensionManagerPlugIn().install( context );
    }
    public String getName() {
        return "Extension Manager Extension";
    }
    
}
