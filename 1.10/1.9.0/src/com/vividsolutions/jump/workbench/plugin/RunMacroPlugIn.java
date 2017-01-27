package com.vividsolutions.jump.workbench.plugin;

import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.java2xml.XML2Java;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * Run a macro composed of a sequence of plugins along with their parameters.
 *
 * @author Micha&euml;l Michaud
 */
public class RunMacroPlugIn extends AbstractThreadedUiPlugIn implements MacroManager {

    private static final String P_FILE_NAME = "MacroFileName";


    public RunMacroPlugIn() {
    }


    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.CUSTOMIZE, "Macro"},
                getName() + "...", false, null,
                null, -1);
    }


    public boolean execute(PlugInContext context) throws Exception {
        JFileChooser jfc = new JFileChooser("lib/ext/macro");
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setFileFilter(createMacroFileFilter());
        jfc.setMultiSelectionEnabled(false);
        if (jfc.showOpenDialog(context.getActiveInternalFrame()) == JFileChooser.APPROVE_OPTION) {
            addParameter(P_FILE_NAME, jfc.getSelectedFile().getPath());
            return true;
        }
        else {
            return false;
        }
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        process(monitor, context);
    }

    public void process(TaskMonitor monitor, PlugInContext context) throws Exception {
        XML2Java xml2Java = new XML2Java();
        context.getWorkbenchContext().getBlackboard().put(MACRO_RUNNING, true);
        Macro processes = (Macro)xml2Java.read(new File(getStringParam(P_FILE_NAME)), Macro.class);
        for (Recordable process : processes.getProcesses()) {
            System.out.println("    execute " + process.getClass());
            if (process instanceof ThreadedPlugIn) {
                ((ThreadedPlugIn)process).run(new DummyTaskMonitor(), context);
            }
            else if (process instanceof AbstractPlugIn) {
                ((PlugIn)process).execute(context);
            }
            else if (process instanceof DataSourceFileLayerLoader) {
                DataSourceFileLayerLoader dsLoader = (DataSourceFileLayerLoader)process;
                dsLoader.setContext(context.getWorkbenchContext());
                dsLoader.process(new DummyTaskMonitor());
            }
            else {
                throw new Exception("");
            }
        }
        context.getWorkbenchContext().getBlackboard().put(MACRO_RUNNING, false);
    }

    private FileFilter createMacroFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".ojm");
            }

            @Override
            public String getDescription() {
                return "OpenJUMP Macro file";
            }
        };
    }
}
