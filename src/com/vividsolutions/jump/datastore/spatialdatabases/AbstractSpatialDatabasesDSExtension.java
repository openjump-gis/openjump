package com.vividsolutions.jump.datastore.spatialdatabases;

import java.util.Map;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * basic implementation for db datastore extensions
 */
abstract public class AbstractSpatialDatabasesDSExtension extends Extension {

  static final String I18NPREFIX = AbstractSpatialDatabasesDSExtension.class
      .getName();
  public static final String VERSION = JUMPVersion.CURRENT_VERSION;

  protected String errorMessage = null;
  protected DataStoreDriver driver = null;

  protected Class[] dsDriverClasses = null;
  // a map w/ entries like "oracle.jdbc.driver.OracleDriver"->"ojdb6.jar"
  protected Map<String, String> classNameToJarName = null;

  /**
   * private, use {@link #AbstractSpatialDatabasesDSExtension(Class, Map)}
   * instead
   */
  private AbstractSpatialDatabasesDSExtension() {
  }

  /**
   * instantiate a new extension capable of registering a database datastore
   * driver
   * 
   * @param dsDriverClasses
   *          - the class implementing {@link DataStoreDriver}
   * @param classesToJar
   *          - a map with class name entries mapping to jar file names eg.
   *          "oracle.jdbc.driver.OracleDriver"->"ojdb6.jar"
   */
  public AbstractSpatialDatabasesDSExtension(Class[] dsDriverClasses,
      Map<String, String> classesToJar) {
    super();
    if (classesToJar == null
        || checkArray(dsDriverClasses, DataStoreDriver.class))
      throw new IllegalArgumentException();
    this.dsDriverClasses = dsDriverClasses;
    this.classNameToJarName = classesToJar;
  }

  /**
   * convenience method for
   * {@link #AbstractSpatialDatabasesDSExtension(Class[], Map)}
   * 
   * @param dsDriverClass the Dadabase Driver Class
   * @param classesToJar a map containing driver class names as keys
   *                     and the jar containing the class as values
   */
  public AbstractSpatialDatabasesDSExtension(Class dsDriverClass,
      Map<String, String> classesToJar) {
    this(new Class[] { dsDriverClass }, classesToJar);
  }

  private boolean checkArray(Class[] classes, Class derivedFrom) {
    if (!(dsDriverClasses instanceof Class[]))
      return false;

    for (Class clazz : classes) {
      if (!clazz.isAssignableFrom(derivedFrom))
        return false;
    }
    return true;
  }

  /**
   * implement to assign a readable name to the datastore driver extension
   * @return the name of this SpatialDatabase
   */
  @Override
  abstract public String getName();

  /**
   * the default version is 'svn revision (build date)'
   */
  public String getVersion() {
    return VERSION;
  }

  /**
   * override to check dependencies on your own
   * 
   * @return "" on success, "errormessage" on failure
   */
  protected String isAvailable() {
    // only run this test once
    if (errorMessage != null)
      return errorMessage;
    String msg = "";
    String others = "";
    for (Map.Entry<String, String> entry : classNameToJarName.entrySet()) {
      String clazz = entry.getKey();
      String jar = entry.getValue();
      // check for jar
      try {
        // [ede 202110] Class seems to ignore '-Djava.system.class.loader=plugin.class.loader' 
        // for unknown reasons, hence we need to enforce it here
        Class.forName(clazz, false, this.getClass().getClassLoader() );
      } catch (ClassNotFoundException e) {
        msg = msg.isEmpty() ? jar : msg + ", " + jar;
      } catch (Throwable t) {
        
        others = ( !others.isEmpty() ? others + "; " : "" )
            + t.getClass().getSimpleName() +" "+ t.getLocalizedMessage();
      }
    }
    if (!msg.isEmpty())
      msg = I18N.getInstance().get(I18NPREFIX + ".missing-dependency-jars", msg);

    if (!others.isEmpty())
      msg += (!msg.isEmpty() ? " " : "")
          + I18N.getInstance().get(I18NPREFIX + ".there-were-errors", others);

    return errorMessage = msg;
  }

  /**
   * by default show error messages or loaded JDBC driver versions
   */
  public String getMessage() {
    String msg = isAvailable();
    if (!msg.isEmpty())
      return msg;

    if (driver != null)
      return driver.getVersion();

    return "";
  }

  /**
   * installs the database datastore driver defined as first parameter in
   * {@link #AbstractSpatialDatabasesDSExtension(Class, Map)} if
   * {@link #isAvailable()} returns an empty String
   */
  public void configure(PlugInContext context) throws Exception {
    WorkbenchContext wbc = context.getWorkbenchContext();

    // conditionally register the DataStore drivers to the system:
    if (isAvailable().isEmpty()) {
      for (Class dsDriverClass : dsDriverClasses) {
        DataStoreDriver dsDriver = (DataStoreDriver) dsDriverClass
            .newInstance();

        // cache the first driver for version output above
        // all others are presumably aliases for different protocols
        if (driver == null)
          driver = dsDriver;

        // register the datastore
        wbc.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
            dsDriver);
      }
    } else {
      Logger.warn(I18N.getInstance().get(I18NPREFIX + ".datastore-disabled",
          getName(), isAvailable()));
    }
  }

}
