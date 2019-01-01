package com.vividsolutions.jump.workbench.datastore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.SimpleStringEncrypter;
import com.vividsolutions.jump.util.StringUtil;

/**
 * Contains a ParameterList and its associated DataStoreDriver.
 */
public class ConnectionDescriptor
{
  private static String getBasicClassName(String fullClassName)
  {
    int dotPos = fullClassName.lastIndexOf('.');
    String name = fullClassName;
    if (dotPos > 0) {
      name = fullClassName.substring(dotPos + 1);
    }
    return name;
  }

  private static String getDataStoreDriverClassName(String fullClassName)
  {
    String className = getBasicClassName(fullClassName);
    int dsdSuffixIndex = className.indexOf("DataStoreDriver");
    if (dsdSuffixIndex < 1)
      return className;
    return className.substring(0, dsdSuffixIndex);
  }

  // the display name of the connection
  private String name = null;
  private ParameterList parameterList;
  private String dataStoreDriverClassName;

  public ConnectionDescriptor() {
  }

  public ConnectionDescriptor(Class dataStoreDriverClass,
                              ParameterList parameterList) {
    this(null, dataStoreDriverClass, parameterList);
  }

  public ConnectionDescriptor(String name,
                              Class dataStoreDriverClass,
                              ParameterList parameterList) {
    this.name = name;
    setDataStoreDriverClassName(dataStoreDriverClass.getName());
    setParameterList(parameterList);
  }

  public void setName(String name)
  {
    this.name = name;
  }
  public String getName()
  {
    return name;
  }


  public DataStoreConnection createConnection(DataStoreDriver driver) throws Exception {
    return driver.createConnection(parameterList);
  }

  public int hashCode() {
    // Implement #hashCode so that ConnectionDescriptor works
    // as a HashMap key. But just set it to 0 for now, to
    // avoid the work of creating code to generate a proper hash.
    // This will unfortunately force a linear scan of the keys whenever
    // HashMap#get is used; however, this will not be a big problem, as
    // I don't expect there to be many keys in HashMaps of
    // ConnectionDescriptors [Jon Aquino 2005-03-07]
    return 0;
  }

  public boolean equals(Object other) {
    return other instanceof ConnectionDescriptor
      && equals((ConnectionDescriptor) other);
  }

  private boolean equals(ConnectionDescriptor other) {
    return other != null
            && getDataStoreDriverClassName().equals(
                    other.getDataStoreDriverClassName())
            && getParameterListWithoutPassword().equals(
                    other.getParameterListWithoutPassword());
    }

    public ParameterList getParameterList() {
        return parameterList;
    }

    public String toString()
    {
      if (name != null) {
        return name + "   (" + getParametersString() + ")";
      }
      return getParametersString();
    }

    public String getParametersString() {
        return getDataStoreDriverClassName(dataStoreDriverClassName)
                + ":"
                + StringUtil
                        .toCommaDelimitedString(
                                CollectionUtil
                                        .select(
                                                parameterValues(getParameterListWithoutPassword()),
                                                new Block() {
                                                    public Object yield(
                                                            Object name) {
                                                        // Don't include null
                                                        // parameters e.g.
                                                        // passwords [Jon Aquino
                                                        // 2005-03-15]
                                                        return name != null;
                                                    }
                                                })).replaceAll(", ", ":");
    }

    private List parameterValues(ParameterList parameterList) {
        List parameterValues = new ArrayList();
        for (String name : parameterList.getSchema().getNames()) {
            parameterValues.add(parameterList.getParameter(name));
        }
        return parameterValues;
    }

    public ParameterList getParameterListWithoutPassword() {
        ParameterList parameterListWithoutPassword = new ParameterList(
                parameterList);
        if (passwordParameterName(parameterList.getSchema()) != null) {
            parameterListWithoutPassword.setParameter(
                    passwordParameterName(parameterList.getSchema()), null);
        }
        return parameterListWithoutPassword;
    }

    public void setParameterListWithObfuscatedPassword(
            PersistentParameterList parameterListWithObfuscatedPassword) {
        ParameterList parameterList = new ParameterList(
                parameterListWithObfuscatedPassword);
        if (passwordParameterName(parameterList.getSchema()) != null) {
            parameterList.setParameter(passwordParameterName(parameterList
                    .getSchema()), unobfuscate(parameterList
                    .getParameterString(passwordParameterName(parameterList
                            .getSchema()))));
        }
        setParameterList(parameterList);
    }

    public PersistentParameterList getParameterListWithObfuscatedPassword() {
        ParameterList parameterListWithObfuscatedPassword = new ParameterList(
                parameterList);
        if (passwordParameterName(parameterList.getSchema()) != null) {
            parameterListWithObfuscatedPassword
                    .setParameter(
                            passwordParameterName(parameterList.getSchema()),
                            obfuscate(parameterList
                                    .getParameterString(passwordParameterName(parameterList
                                            .getSchema()))));
        }
        return new PersistentParameterList(parameterListWithObfuscatedPassword);
    }

    private String obfuscate(String s) {
        return s != null ? new SimpleStringEncrypter().encrypt(s) : null;
    }

    private String unobfuscate(String s) {
        return s != null ? new SimpleStringEncrypter().decrypt(s) : null;
    }

    public void setDataStoreDriverClassName(String dataStoreDriverClassName) {
        this.dataStoreDriverClassName = dataStoreDriverClassName;
    }

    public String getDataStoreDriverClassName() {
        return dataStoreDriverClassName;
    }

    public void setParameterList(ParameterList parameterList) {
        this.parameterList = parameterList;
    }

    public static String passwordParameterName(ParameterListSchema schema) {
        for (String name : schema.getNames()) {
            if (name.equalsIgnoreCase("password")) {
                return name;
            }
        }
        return null;
    }

    public static class PersistentParameterList extends ParameterList {
        public PersistentParameterList() {
            super(new ParameterListSchema(new String[] {}, new Class[] {}));
        }

        public PersistentParameterList(ParameterList parameterList) {
            this();
            setSchema(new PersistentParameterListSchema(parameterList
                    .getSchema()));
            setNameToValueMap(nameToValueMap(parameterList));
        }

        private static Map nameToValueMap(ParameterList parameterList) {
            Map nameToValueMap = new HashMap();
            for (String name : parameterList.getSchema().getNames()) {
                nameToValueMap.put(name, parameterList.getParameter(name));
            }
            return nameToValueMap;
        }

        public Map getNameToValueMap() {
            return nameToValueMap(this);
        }

        public void setNameToValueMap(Map nameToValueMap) {
            for (Iterator i = nameToValueMap.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                setParameter(name, nameToValueMap.get(name));
            }
        }

        public void setSchema(PersistentParameterListSchema schema) {
            initialize(schema);
        }
    }

    public static class PersistentParameterListSchema extends
            ParameterListSchema {
        public PersistentParameterListSchema() {
            super(new String[] {}, new Class[] {});
        }

        public PersistentParameterListSchema(ParameterListSchema schema) {
            this();
            initialize(schema.getNames(), schema.getClasses());
        }

        public List getPersistentNames() {
            return Arrays.asList(getNames());
        }

        public void addPersistentName(String name) {
            String[] newNames = new String[getNames().length + 1];
            System.arraycopy(getNames(), 0, newNames, 0, getNames().length);
            newNames[getNames().length] = name;
            initialize(newNames, getClasses());
        }

        public List getPersistentClasses() {
            return Arrays.asList(getClasses());
        }

        public void addPersistentClass(Class c) {
            Class[] newClasses = new Class[getClasses().length + 1];
            System.arraycopy(getClasses(), 0, newClasses, 0,
                    getClasses().length);
            newClasses[getClasses().length] = c;
            initialize(getNames(), newClasses);
        }
    }
}