/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.client;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deegree.datatypes.QualifiedName;
import org.deegree.datatypes.Types;
import org.deegree.framework.xml.DOMPrinter;
import org.deegree.model.feature.schema.FeatureType;
import org.deegree.model.feature.schema.GMLSchema;
import org.deegree.model.feature.schema.PropertyType;
import org.deegree.ogcwebservices.OWSUtils;
import org.deegree.ogcwebservices.wfs.capabilities.WFSFeatureType;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.workbench.Logger;

import de.latlon.deejump.wfs.DeeJUMPException;
import de.latlon.deejump.wfs.auth.UserData;
import de.latlon.deejump.wfs.deegree2mods.GMLSchemaDocument;

/**
 * Superclass that wraps the basic functionality of a (simple) WFS. This class
 * encapsulates the behaviour of a WFS, and allows subclasses to change
 * behaviour according to WFS version.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1438 $, $Date: 2008-05-29 15:00:02 +0200 (Do, 29 Mai
 *          2008) $
 */
public abstract class AbstractWFSWrapper {

  /**
     * 
     */
  public static final String WFS_PREFIX = "wfs";

  protected String baseURL;

  protected Map<String, WFSFeatureType> ftNameToWfsFT;

  private Map<String, GMLSchema> featureTypeToSchema;

  // hmmm, this is repeating the above, really...
  private Map<String, String> featureTypeToSchemaXML;

  private Map<String, QualifiedName[]> geoPropsNameToQNames;

  protected UserData logins;

  /**
   * @return the version as String
   */
  public abstract String getServiceVersion();

  /**
   * @return the list of feature types
   */
  public abstract String[] getFeatureTypes();

  /**
   * @return the GET GetFeature URL
   */
  public abstract String getGetFeatureURL();

  abstract protected String createDescribeFTOnlineResource();

  /**
   * @return the base WFS URL
   */
  public String getBaseWfsURL() {
    return this.baseURL;
  }

  /**
   * get login data
   */
  public UserData getLogins() {
    return logins;
  }

  /**
   * set login userdata
   */
  public void setLogins(UserData logins) {
    this.logins = logins;
  }

  /**
   * @return the capabilities as String
   */
  public abstract String getCapabilitesAsString();

  protected AbstractWFSWrapper(UserData logins, String baseUrl) {
    if (baseUrl == null || baseUrl.length() == 0) {
      throw new IllegalArgumentException(
          "The URL for the WFServer cannot be null or empty.");
    }
    this.baseURL = baseUrl;
    this.logins = logins;
    this.featureTypeToSchema = new HashMap<String, GMLSchema>();
    this.featureTypeToSchemaXML = new HashMap<String, String>();
    this.geoPropsNameToQNames = new HashMap<String, QualifiedName[]>();
  }

  /**
   * @return the capabilities URL
   */
  public String getCapabilitiesURL() {

    StringBuffer sb = new StringBuffer(
        OWSUtils.validateHTTPGetBaseURL(this.baseURL));
    sb.append("SERVICE=WFS&REQUEST=GetCapabilities&VERSION=");
    sb.append(getServiceVersion());
    // if ( logins != null && logins.getUsername() != null &&
    // logins.getPassword() != null ) {
    // sb.append( "&user=" + logins.getUsername() + "&password=" +
    // logins.getPassword() );
    // }
    //
    String url = sb.toString();
    if (logins != null && !logins.isEmpty())
      url = UriUtil.urlAddCredentials(url, logins.getUsername(),
          logins.getPassword());
    return url;
  }

  /**
   * @param baseURL
   *          the base URL to use
   * @param typename
   * @return the full request String
   */
  public String getDescribeTypeURL(String baseURL, QualifiedName typename) {
    String url;

    if (typename.getPrefix() == null || typename.getPrefix().length() == 0) {
      url = baseURL + "SERVICE=WFS&REQUEST=DescribeFeatureType&version="
          + getServiceVersion() + "&TYPENAME=" + typename.getLocalName();
    } else {
      url = baseURL + "SERVICE=WFS&REQUEST=DescribeFeatureType&version="
          + getServiceVersion() + "&TYPENAME=" + typename.getPrefix() + ":"
          + typename.getLocalName() + "&NAMESPACE=xmlns("
          + typename.getPrefix() + "=" + typename.getNamespace() + ")";
    }

    if (logins != null && !logins.isEmpty())
      url = UriUtil.urlAddCredentials(url, logins.getUsername(),
          logins.getPassword());

    return url;
  }

  /**
   * @param typename
   * @return the full request String
   */
  public String getDescribeTypeURL(QualifiedName typename) {
    String url = getDescribeTypeURL(
        OWSUtils.validateHTTPGetBaseURL(createDescribeFTOnlineResource()),
        typename);
    Logger.debug("Describe Feature Type request:\n" + url);

    return url;
  }

  /**
   * @param featureType
   * @return the GMLSchema object for the feature type
   */
  public synchronized GMLSchema getSchemaForFeatureType(String featureType) {
    GMLSchema res = this.featureTypeToSchema.get(featureType);
    if (res != null) {
      return res;
    }

    createSchemaForFeatureType(featureType);
    return this.featureTypeToSchema.get(featureType);
  }

  /**
   * @param featureType
   * @return a String encoding of the Schema for the feature type
   */
  public String getRawSchemaForFeatureType(String featureType) {
    return this.featureTypeToSchemaXML.get(featureType);
  }

  protected String loadSchemaForFeatureType(String featureType)
      throws DeeJUMPException {

    String descrFtUrl = createDescribeFTOnlineResource();

    if (descrFtUrl == null) {
      throw new RuntimeException(
          "Service does not have a DescribeFeatureType operation accessible by HTTP GET or POST.");
    }

    WFSFeatureType wfsFt = getFeatureTypeByName(featureType);
    if (wfsFt == null) {
      return null;
    }

    QualifiedName ft = wfsFt.getName();
    String serverReq = getDescribeTypeURL(ft);

    try {
      GMLSchemaDocument xsdDoc = new de.latlon.deejump.wfs.deegree2mods.GMLSchemaDocument();
      xsdDoc.load(new URL(serverReq));
      return DOMPrinter.nodeToString(xsdDoc.getRootElement(), null);
    } catch (Exception e) {
      e.printStackTrace();
      String mesg = "Error fetching FeatureType description";
      Logger.error(mesg + " for " + featureType + " using " + serverReq);
      throw new DeeJUMPException(mesg, e);
    }

  }

  /**
   * Creates an String[] containing the attributes of a given feature type
   * 
   * @param featureTypeName
   *          the name of the feature type
   */
  protected synchronized void createSchemaForFeatureType(String featureTypeName) {

    try {
      // GMLSchema xsd = loadSchemaForFeatureType( featureTypeName );
      String rawXML = loadSchemaForFeatureType(featureTypeName);
      if (rawXML == null) {
        return;
      }
      GMLSchemaDocument xsdDoc = new GMLSchemaDocument();
      xsdDoc.load(new StringReader(rawXML), "http://empty");
      GMLSchema xsd = xsdDoc.parseGMLSchema();

      this.featureTypeToSchema.put(featureTypeName, xsd);
      this.featureTypeToSchemaXML.put(featureTypeName, rawXML);

      QualifiedName[] geoProp = guessGeomProperty(xsd, featureTypeName);

      this.geoPropsNameToQNames.put(featureTypeName, geoProp);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @param featureType
   * @return a list of property names
   */
  public String[] getProperties(String featureType) {

    List<String> propsList = new ArrayList<String>();
    try {
      createSchemaForFeatureType(featureType);

      GMLSchema schema = featureTypeToSchema.get(featureType);
      if (schema != null) {
        FeatureType[] fts = schema.getFeatureTypes();
        for (int i = 0; i < fts.length; i++) {
          if (fts[i].getName().getLocalName().equals(featureType)) {
            PropertyType[] props = fts[i].getProperties();
            for (int j = 0; j < props.length; j++) {
              if (!(props[j].getType() == Types.GEOMETRY || props[j].getType() == 10014)) {
                propsList.add(props[j].getName().getPrefixedName());
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      propsList = new ArrayList<String>();
    }

    return propsList.toArray(new String[propsList.size()]);
  }

  /**
   * @param ftName
   * @return the WFSFeatureType object
   */
  public synchronized WFSFeatureType getFeatureTypeByName(String ftName) {
    if (ftNameToWfsFT == null) {
      getFeatureTypes(); // side effects in functions that return lists are
                         // wonderful
    }
    return ftNameToWfsFT.get(ftName);
  }

  private static QualifiedName[] guessGeomProperty(GMLSchema schema,
      String featureTypeName) {
    QualifiedName[] geoPropNames = null;
    List<QualifiedName> tmpList = new ArrayList<QualifiedName>(20);

    FeatureType[] fts = schema.getFeatureTypes();
    for (int i = 0; i < fts.length; i++) {
      if (!fts[i].getName().getLocalName().equals(featureTypeName)) {
        continue;
      }

      PropertyType[] props = fts[i].getProperties();
      for (int j = 0; j < props.length; j++) {
        if (props[j].getType() == Types.GEOMETRY || props[j].getType() == 10014) {
          tmpList.add(props[j].getName());

        }
      }
    }

    geoPropNames = tmpList.toArray(new QualifiedName[tmpList.size()]);

    return geoPropNames;
  }

  /**
   * @param featureType
   * @return a list of geometry property names
   */
  public QualifiedName[] getGeometryProperties(String featureType) {
    return this.geoPropsNameToQNames.get(featureType);
  }

}
