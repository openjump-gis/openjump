package org.openjump.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vividsolutions.jump.workbench.Logger;

/**
 * Utility class for handline {@link URI}.
 * 
 * @author Paul Austin
 */
public final class UriUtil {
  /**
   * Private constructor for UriUtil.
   */
  private UriUtil() {
  }

  /**
   * Create a URI to the entry within a ZIP file.
   * 
   * @param file
   *          The ZIP file.
   * @param entry
   *          The ZIP entry.
   * @return The URI.
   */
  public static URI createZipUri(final File file, final String entry) {
    try {
      // final String entryName = entry.getName();
      final URI fileUri = file.toURI();
      final String filePath = fileUri.getPath();
      final URI uri = new URI("zip", null, filePath + "!/" + entry, null);
      return uri;
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException("Cannot create URI for " + file + "!/"
          + entry);
    }
  }

  public static URI createZipUri(final String file, final String entry) {
    return createZipUri(new File(file), entry);
  }

  public static URI createFileUri(final String file) {
    return new File(file).toURI();
  }

  /**
   * Get the ZIP file from a ZIP URI.
   * 
   * @param uri
   *          The URI.
   * @return The ZIP file.
   */
  public static File getZipFile(final URI uri) {
    try {
      final URI fileUri = new URI("file", null, getZipFilePath(uri), null);
      return new File(fileUri);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static String getZipFilePath(final URI uri) {
    final String path = uri.getPath();
    final int index = path.indexOf('!');
    return index < 0 ? path : path.substring(0, index);
  }

  public static String getZipFileName(final URI uri) {
    final String path = getZipFilePath(uri);
    final int index = path.lastIndexOf('/');
    return index < 0 ? path : path.substring(index + 1);
  }

  /**
   * Get the name of a ZIP file entry from a ZIP URI.
   * 
   * @param uri
   *          The URI.
   * @return The ZIP entry name.
   */
  public static String getZipEntryName(final URI uri) {
    final String path = uri.getPath();
    final int index = path.indexOf('!');
    if (index == -1) {
      return null;
    } else {
      return path.substring(index + 2);
    }
  }

  /**
   * Get the file extension from the URI path.
   * 
   * @param uri
   *          The URI.
   * @return The file extension.
   */
  public static String getFileExtension(final URI uri) {
    final String path = uri.getPath();
    final int dotIndex = path.lastIndexOf('.');
    if (dotIndex != -1) {
      return path.substring(dotIndex + 1);
    } else {
      return "";
    }
  }

  /**
   * Get the file name from the URI path.
   * 
   * @param uri
   *          The URI.
   * @return The file name.
   */
  public static String getFileName(final URI uri) {
    // make getFilename zip url safe
    return getZipFileName(uri);
  }

  /**
   * Get the file name without the extension from the URI path.
   * 
   * @param uri
   *          The URI.
   * @return The file name.
   */
  public static String getFileNameWithoutExtension(final URI uri) {
    final String name = getFileName(uri);
    final int dotIndex = name.lastIndexOf('.');
    if (dotIndex != -1) {
      return name.substring(0, dotIndex);
    } else {
      return "";
    }
  }

  /**
   * Get the path to an entry in a ZIP URI.
   * 
   * @param uri
   *          The zip URI
   * @return The entry's path.
   */
  public static String getZipEntryFilePath(final URI uri) {
    final String name = getZipEntryName(uri);
    final int slashIndex = name.lastIndexOf('/');
    if (slashIndex != -1) {
      return name.substring(0, slashIndex);
    } else {
      return null;
    }
  }

  public static String getFilePath(final URI uri) {
    return getZipFilePath(uri);
  }

  public static String getFileName(final String path) {
    final int slashIndex = path.lastIndexOf('/');
    return slashIndex > -1 ? path.substring(slashIndex) : path;
  }

  public static String getPath(final String path) {
    final int slashIndex = path.lastIndexOf('/');
    return slashIndex > -1 ? path.substring(0, slashIndex) : "";
  }

  public static String removeExtension(final String path) {
    final int slashIndex = path.lastIndexOf('/');
    final int dotIndex = path.lastIndexOf('.');
    return slashIndex < dotIndex ? path.substring(0, dotIndex) : path;
  }

  final static String charSet = "UTF-8";

  public static String urlEncode(String in) {
    try {
      return URLEncoder.encode(in, charSet);
    } catch (Exception e) {
      Logger.error(e);
      return in;
    }
  }

  public static String urlDecode(String in) {
    try {
      return URLDecoder.decode(in, charSet);
    } catch (Exception e) {
      Logger.error(e);
      return in;
    }
  }

  final static String regexp = "^([^:/]+://)(?:(([^@:/]*)(?:\\:([^@:/]*))?)@)?((.*?)(?::(\\d+))?(/.*))$";

  public static boolean isURL(String in) {
    Pattern p = Pattern.compile(regexp);
    Matcher m = p.matcher(in != null ? in : "");
    return m.matches();
  }

  public static String urlStripAuth(String url) {
    String clean = isURL(url) ? url.replaceFirst(regexp, "$1$5") : url;
    return clean;
  }

  public static String urlGetUser(String url) {
    if (isURL(url))
      return urlDecode(url.replaceFirst(regexp, "$3"));
    return "";
  }

  public static String urlGetPassword(String url) {
    if (isURL(url))
      return urlDecode(url.replaceFirst(regexp, "$4"));
    return "";
  }

  /**
   * userinfo is the prepared urlencoded string before the @ eg. user:pass
   * 
   * @param url URL string to get the user info from
   * @return a String representing user info of the URL
   */
  public static String urlGetUserInfo(String url) {
    if (isURL(url))
      return url.replaceFirst(regexp, "$2");
    return "";
  }

  public static String urlGetHost(String url) {
    if (isURL(url))
      return urlDecode(url.replaceFirst(regexp, "$6"));
    return "";
  }

  public static String urlGetPort(String url) {
    if (isURL(url))
      return urlDecode(url.replaceFirst(regexp, "$7"));
    return "";
  }

  public static String urlGetPath(String url) {
    if (isURL(url))
      return urlDecode(url.replaceFirst(regexp, "$8"));
    return "";
  }

  public static String urlStripPassword(String url) {
    String user = urlGetUser(url);
    if (!user.isEmpty())
      user += "@";
    String clean = isURL(url) ? url.replaceFirst(regexp, "$1" + user + "$5")
        : url;
    return clean;
  }

  public static String urlAddCredentials(String url, String user, String pass) {
    if (!isURL(url))
      return url;

    String urlCreds = user != null ? urlEncode(user) : "";
    if (!urlCreds.isEmpty()) {
      String urlPass = pass != null ? urlEncode(pass) : "";
      if (!urlPass.isEmpty())
        urlCreds += ":" + urlPass;
    }

    return urlAddUserInfo(url, urlCreds);
  }

  /**
   * Make sure the url string ends with '?' or '&' to safely append url parameters
   * 
   * @param url string
   * @return url string safe to append parameters
   */
  public static String urlMakeAppendSafe(String url) {
    String fixedURL = url.trim();

    if (!fixedURL.contains("?")) {
      fixedURL = fixedURL + "?";
    } else {
      if (fixedURL.endsWith("?")) {
        // ok
      } else {
        // it must have other parameters
        if (!fixedURL.endsWith("&")) {
          fixedURL = fixedURL + "&";
        }
      }
    }

    return fixedURL;
  }

  /**
   * userinfo is the prepared urlencoded string before the @ eg. user:pass
   * 
   * @param url url String to add user info to
   * @param userinfo user info to add
   * @return url with added user info
   */
  public static String urlAddUserInfo(String url, String userinfo) {
    if (!isURL(url))
      return url;

    if (userinfo == null || userinfo.isEmpty())
      return url;

    return url.replaceFirst(regexp, "$1" + userinfo + "@$5");
  }
}
