package org.openjump.core.ui.plugin.file;

import org.openjump.core.ui.io.file.FileLayerLoader;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;

/**
 * Install a dummy reader which does not try to read files with xml or txt 
 * extension. This is useful to skip those files if there are included in a
 * zip file
 * 
 * @author Micha�l Michaud
 */
public class InstallDummyReaderPlugIn extends AbstractPlugIn {

    private static final String KEY = DummyDataSource.class.getName();
    public static final String DUMMY_READER_DESC = I18N.getInstance().get(KEY)+".description";

    /**
     * Construct the main Open File plug-in.
     */
    public InstallDummyReaderPlugIn() {
        super();
    }


    /**
     * Initialise the main instance of this plug-in, should not be called for the
     * Recent menu open file plug-ins.
     * 
     * @param context The plug-in context.
     * @exception Exception If there was an error initialising the plug-in.
     */
    public void initialize(final PlugInContext context) throws Exception {
//        context.getWorkbenchContext().getRegistry().createEntry(FileLayerLoader.KEY,
//            new DataSourceFileLayerLoader(
//                context.getWorkbenchContext(),
//                DummyDataSource.class, 
//                DUMMY_READER, 
//                Arrays.asList(new String[]{"*"})));
    }
    
    public static class DummyDataSource extends DataSource {
        public DummyDataSource() {
        }
        public Connection getConnection() {

            return new Connection() {

                @Override
                public void close() {}

                @Override
                public FeatureCollection executeQuery(String query, Collection<Throwable> exceptions, TaskMonitor monitor) {
                    return null;
                }

                @Override
                public FeatureCollection executeQuery(String query, TaskMonitor monitor) {
                    return null;
                }

                @Override
                public void executeUpdate(String query, FeatureCollection featureCollection, TaskMonitor monitor) {}
            };
        }
        public Map getProperties() {return new HashMap();}
        public FeatureCollection installCoordinateSystem(FeatureCollection queryResult, CoordinateSystemRegistry registry) {
            return null;
        }
        public boolean isReadable() {return false;}
        public boolean isWritable() {return false;}
        public void setProperties(Map properties) {}
    }

}
