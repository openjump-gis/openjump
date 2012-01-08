package jumptest.io;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.WKTReader;
import com.vividsolutions.jump.io.WKTWriter;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.ReaderWriterFileDataSource;
import com.vividsolutions.jump.task.DummyTaskMonitor;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Playground {
    public static void main(String[] args) throws Exception {
        DataSource ds = new ReaderWriterFileDataSource(new WKTReader(), new WKTWriter());
        ds.setProperties(Collections.singletonMap(DataSource.FILE_KEY, "C:/junk/a.wkt"));

        Connection conn = ds.getConnection();

        try {
            print(conn.executeQuery(null, new ArrayList(), new DummyTaskMonitor()));
        } finally {
            conn.close();
        }
    }

    private static void print(FeatureCollection fc) {
        for (Iterator i = fc.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            System.out.println(feature.getGeometry().toString());
        }
    }
}
