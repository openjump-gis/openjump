package jumptest.datastore;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Task;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.postgis.PostGISQueryUtil;
import org.openjump.core.ui.plugin.datastore.postgis2.PostGISDataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.postgis2.SaveToPostGISDataSourceQuery;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver.*;

/**
 * Created with IntelliJ IDEA.
 * User: Michaël
 * Date: 26/10/13
 * Time: 11:16
 * To change this template use File | Settings | File Templates.
 */
public class PostGISDriverTest {

    private final static WKBWriter WRITER2D = new WKBWriter(2, false);
    private final static WKBWriter WRITER2D_SRID = new WKBWriter(2, true);
    private final static WKBWriter WRITER3D = new WKBWriter(3, false);
    private final static WKBWriter WRITER3D_SRID = new WKBWriter(3, true);
    private final static GeometryFactory FACTORY = new GeometryFactory();

    Connection connection;
    WorkbenchContext context;
    ConnectionDescriptor connectionDescriptor;

    public static void main(String[] args) {
        PostGISDriverTest test = null;
        try {
            Properties properties = new Properties();
            properties.load(PostGISDriverTest.class.getResourceAsStream("PostGISDriver.properties"));
            PostGISDriverTest pgd = new PostGISDriverTest(properties);
            pgd.test1();
            pgd.test11();
            pgd.test2();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (test != null && test.getConnection() != null) test.getConnection().close();
            } catch(Exception e) {e.printStackTrace();}
        }
    }

    private PostGISDriverTest(Properties properties) throws Exception {

        System.out.println("Initialize database from properties file");
        JUMPWorkbench.main(new String[]{});
        //JUMPWorkbench jump = new JUMPWorkbench("title", new String[]{}, new JWindow(), new DummyTaskMonitor());
        JUMPWorkbench jump = JUMPWorkbench.getInstance();
        Task task = new Task();
        task.setName("Unit Tests");
        jump.getFrame().addTaskFrame(task);
        Thread.sleep(1000);
        context = jump.getContext();
        System.out.println("PostGISDriverTest: " + context);

        ParameterList parameters = new ParameterList(new ParameterListSchema(
                new String[] {PARAM_Server, PARAM_Port, PARAM_Instance, PARAM_User, PARAM_Password},
                new Class[]{String.class, Integer.class, String.class, String.class, String.class}
        ));

        String host = properties.getProperty("server");
        int port = Integer.parseInt(properties.getProperty("port"));
        String database = properties.getProperty("db");
        String user = properties.getProperty("user");
        String password = properties.getProperty("pwd");

        parameters.setParameter(PARAM_Instance, database);
        parameters.setParameter(PARAM_Server, host);
        parameters.setParameter(PARAM_Port, port);
        parameters.setParameter(PARAM_User, user);
        parameters.setParameter(PARAM_Password, password);
        connectionDescriptor = new ConnectionDescriptor(PostgisDataStoreDriver.class, parameters);

        String url = String.valueOf(new StringBuffer("jdbc:postgresql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(database));

        Driver driver = (Driver) Class.forName("org.postgresql.Driver").newInstance();
        DriverManager.registerDriver(driver);
        connection = DriverManager.getConnection(url, user, password);
    }

    // Create a layer from a table
    private void test1() throws Exception {
        System.out.println("test 1");
        createTable("test1", "identifier", "bigserial", "geom", 2154, "POINT", 2);
        addColumn("test1", "Nom", "varchar");
        insertData("test1", new String[]{"geom", "Nom"}, new Object[]{FACTORY.createPoint(new Coordinate(12.5, 13)), "toto"}, 2154, 2);
        createLayerFromTable("test1", "geom", "identifier");
        assert context.getLayerManager().getLayer("test1") != null : "Layer has not been created";
        assert context.getLayerManager().getLayer("test1").getFeatureCollectionWrapper().getFeatures().size() == 1 : "Layer is empty";
        assert ((Feature)context.getLayerManager().getLayer("test1").getFeatureCollectionWrapper()
                .getFeatures().iterator().next()).getGeometry().getCoordinate().equals(new Coordinate(12.5,13));
        context.getLayerManager().remove(context.getLayerManager().getLayer("test1"));
        System.out.println("End of test 1");
    }

    // Create a layer from a table and upload different attribute types
    private void test11() throws Exception {
        System.out.println("test 11");
        createTable("test1", "identifier", "bigserial", "geom", 2154, "POINT", 2/*, "Nom", "varchar"*/);
        addColumn("test1", "Nom", "varchar");
        addColumn("test1", "Age",    "integer");
        addColumn("test1", "Height", "double precision");
        addColumn("test1", "Date1",  "timestamp");
        addColumn("test1", "Date2",  "timestamp");
        addColumn("test1", "geom2",   "geometry");
        insertData("test1",
                new String[]{"geom", "Nom", "Age", "Height", "Date1", "Date2", "geom2"},
                new Object[]{
                        FACTORY.createPoint(new Coordinate(12.5, 13)),
                        "toto",
                        new Integer(12),
                        new Double(1.55),
                        new SimpleDateFormat("yyyy-MM-dd").parse("2013-01-01"),
                        new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2013-01-01 08:00:00"),
                        FACTORY.createPoint(new Coordinate(20.0, 20.0))}, 2154, 2);
        createLayerFromTable("test1", "geom", "identifier");
        assert context.getLayerManager().getLayer("test1") != null : "Layer has not been created";
        assert context.getLayerManager().getLayer("test1").getFeatureCollectionWrapper().getFeatures().size() == 1 : "Layer is empty";
        Feature feature = (Feature)context.getLayerManager().getLayer("test1").getFeatureCollectionWrapper().getFeatures().iterator().next();
        assert feature.getGeometry().getCoordinate().equals(new Coordinate(12.5,13));
        assert feature.getAttribute("Nom").equals("toto");
        assert feature.getAttribute("Age").equals("toto");
        assert feature.getAttribute("Nom").equals("toto");
        assert feature.getAttribute("Nom").equals("toto");
        assert feature.getAttribute("Nom").equals("toto");
        context.getLayerManager().remove(context.getLayerManager().getLayer("test11"));
        System.out.println("End of test 11");
    }

    // Create a table from a layer
    private void test2() throws Exception {
        System.out.println("test 2");

        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        schema.addAttribute("Nom", AttributeType.STRING);
        schema.addAttribute("Weight", AttributeType.DOUBLE);
        FeatureCollection dataset = new FeatureDataset(schema);

        Feature f = new BasicFeature(schema);
        f.setGeometry(FACTORY.createPoint(new Coordinate(12.5, 13)));
        f.setAttribute("Nom", "Michaël");
        f.setAttribute("Weight", 75.0);
        dataset.add(f);

        f = new BasicFeature(schema);
        f.setGeometry(FACTORY.createLineString(new Coordinate[]{new Coordinate(125, 13.5), new Coordinate(135, 13.5)}));
        f.setAttribute("Nom", "Peter");
        f.setAttribute("Weight", 70.0);
        dataset.add(f);

        context.getLayerManager().addLayer("Travail", "test2", dataset);
        System.out.println("Will create table");
        createTableFromLayer(context.getLayerManager().getLayer("test2"), "test2", true, 2154);
        System.out.println("End of test 2");
    }



    private void createLayerFromTable(String table, String geom, String pk) throws Exception {
        WritableDataStoreDataSource source = new PostGISDataStoreDataSource(connectionDescriptor, table, geom, pk);
        source.setWorkbenchContext(context);
        source.setTableAlreadyCreated(true);
        System.out.println(source);
        System.out.println("createLayerFromTable: " + context);
        System.out.println("createLayerFromTable: " + context.getLayerManager());
        context.getLayerManager().addLayer("Travail", table, source.getConnection().executeQuery(null, new DummyTaskMonitor()));
    }

    private void createTableFromLayer(Layer layer, String table, boolean createPK, int srid) throws Exception {
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        WritableDataStoreDataSource source = new PostGISDataStoreDataSource(connectionDescriptor, table,
                schema.getAttributeName(schema.getGeometryIndex()), null);
        source.setTableAlreadyCreated(false);
        Map map = source.getProperties();
        map.put(WritableDataStoreDataSource.SRID_KEY, srid);
        //map.put(WritableDataStoreDataSource.CREATE_TABLE, true);
        map.put(WritableDataStoreDataSource.CREATE_PK, createPK);
        if (createPK) map.put(WritableDataStoreDataSource.EXTERNAL_PK_KEY, WritableDataStoreDataSource.DEFAULT_PK_NAME);
        SaveToPostGISDataSourceQuery query = new SaveToPostGISDataSourceQuery(source, null, "source");
        query.setProperties(map);
        layer.setDataSourceQuery(query);
        source.getConnection().executeUpdate(null, layer.getFeatureCollectionWrapper(), new DummyTaskMonitor());
    }


    // --------------------------------------------------------------------------------------------
    // Execute SQL statement
    // --------------------------------------------------------------------------------------------
    private PostGISDriverTest createTable(String name) throws SQLException {
        connection.createStatement().execute("CREATE TABLE \"" + name + "\" ();");
        //connection.commit();
        return this;
    }
    private PostGISDriverTest dropTableIfExists(String name) throws SQLException {
        connection.createStatement().execute("DROP TABLE IF EXISTS \"" + name + "\";");
        //connection.commit();
        return this;
    }
    private PostGISDriverTest addGeometryColumn(String table, String column, int srid, String geomType, int dim) throws SQLException {
        connection.createStatement().execute("SELECT AddGeometryColumn('" + table + "', '" +
                column + "', " + srid + ", '" + geomType + "', " + dim + ");");
        //connection.commit();
        return this;
    }
    private PostGISDriverTest addPKColumn(String table, String column, String type) throws SQLException {
        connection.createStatement().execute("ALTER TABLE \"" + table + "\" ADD COLUMN \"" + column + "\" " + type + " PRIMARY KEY;");
        //connection.commit();
        return this;
    }
    private PostGISDriverTest addColumn(String table, String column, String type) throws SQLException {
        connection.createStatement().execute("ALTER TABLE \"" + table + "\" ADD COLUMN \"" + column + "\" " + type + ";");
        //connection.commit();
        return this;
    }
    private PostGISDriverTest createTable(String table, String key, String keyType,
            String geom, int srid, String geomType, int dim/*, String col, String type*/) throws SQLException {
        //connection.setAutoCommit(false);
        dropTableIfExists(table);
        createTable(table);
        addPKColumn(table, key, keyType);
        addGeometryColumn(table, geom, srid, geomType, dim);
        //addColumn(table, col, type);
        //connection.commit();
        //connection.setAutoCommit(true);
        return this;
    }

    private void insertData(String table, String[] columns, Object[] data, int srid, int dim) throws SQLException {

        WKBWriter writer = dim == 2 ? WRITER2D_SRID : WRITER3D_SRID;
        StringBuffer buffer = new StringBuffer("INSERT INTO " + table + "(");
        for (int i = 0 ; i < columns.length ; i++) {
            if (i == 0) buffer.append("\"" + columns[i] + "\"");
            else buffer.append(", \"" + columns[i] + "\"");
        }
        buffer.append(") VALUES(");
        for (int i = 0 ; i < data.length ; i++) {
            if (i == 0) buffer.append("?");
            else buffer.append(",?");
        }
        buffer.append(");");
        PreparedStatement ps;
        ps = connection.prepareStatement(buffer.toString());
        //System.out.println(Arrays.toString(data));
        for (int i = 0 ; i < data.length ; i++) {
            //System.out.println(data[i].getClass() + ": " + data[i]);
            if (data[i] instanceof String) ps.setString(i+1, (String)data[i]);
            else if (data[i] instanceof Integer) ps.setInt(i+1, (Integer)data[i]);
            else if (data[i] instanceof Double) ps.setDouble(i+1, (Double) data[i]);
            else if (data[i] instanceof java.util.Date) {
                ps.setTimestamp(i+1, new Timestamp(((java.util.Date) data[i]).getTime()));
            }
            else if (data[i] instanceof Geometry) {
                ((Geometry)data[i]).setSRID(2154);
                ps.setBytes(i+1, PostGISQueryUtil.getByteArrayFromGeometry((Geometry)data[i], true, dim));
            }
        }
        System.out.println(ps);
        ps.executeUpdate();
    }

    // --------------------------------------------------------------------------------------------
    // Accessors
    // --------------------------------------------------------------------------------------------
    private Connection getConnection() {return connection;}

}
