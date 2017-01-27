package de.latlon.deejump.plugin.manager;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class DummyExtension extends Extension {
    
    public void configure(PlugInContext context) throws Exception {
        new DummyPlugIn().install( context );
    }
    public String getName() {
        return "Dummy extension";
    }
    static class DummyPlugIn extends AbstractPlugIn {
        
        public boolean execute(PlugInContext context) throws Exception {
            System.out.println("dummy has been clicked:");
            return true;
        }
        public void install( PlugInContext context ) throws Exception {

            context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().addPlugIn(
                getIcon(),
                this, 
                null,
                context.getWorkbenchContext()
            );          
        }
        public ImageIcon getIcon() {
            return new ImageIcon( getClass().getResource("refresh.png"));
        }
        
    }
    
}
