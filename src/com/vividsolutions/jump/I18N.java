/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump;

import java.io.File;
import java.security.InvalidParameterException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

import com.vividsolutions.jump.workbench.Logger;

/**
 * Singleton for the Internationalization (I18N)
 **/
public final class I18N {

  // a map of additional I18N instances
  private static Map<Object, I18N> instances = new HashMap<>();

  // set some defaults for the I18N instances
  private static ClassLoader classLoader;
  private String resourcePath = "language/jump";
  private static Locale locale = Locale.getDefault();
  private static boolean initialized = false;

  private ResourceBundle resourceBundle, resourceBundle2, resourceBundle3 = null;

  // remember missing strings, do not flood log
  private HashSet<String> missing = new HashSet<>();

  // the default instance used by OJ-Core (depends on the initializations above!)
  public static final I18N JUMP = new I18N();

  private I18N() {
    // using resourcePath as defined above
  }

  /**
   * Construct an I18N instance for the category.
   * 
   * @param categoryPrefix i18n files should be in category/language/jump files.
   */
  private I18N(final String categoryPrefix) {
    // prepend categoryPrefix
    resourcePath = categoryPrefix.replace('.', '/') + "/" + resourcePath;
  }

  /**
   * Create an instance for a concrete path without 'language/jump' appended
   * 
   * @param path path of the resource file
   */
  private I18N(final File path) {
    resourcePath = path.toString();
    // make sure the resource path is slash separated, File.toString()
    // returns it as separated by the OS which may contain eg. backslashes
    // on windows, leading to resources not found in zipped jars
    if (File.separatorChar != '/')
      resourcePath = resourcePath.replace(File.separatorChar, '/');
  }

  /**
   * reinitialize _all_ instances. should be called after language changes or such.
   */
  private static void initAll() {
    // nuffin to do
    if (initialized) return;

    // apply to instances
    for (I18N i18n : instances.values()) {
      i18n.init();
    }
    JUMP.init();

    initialized = true;
  }

  /**
  * everytime something important changes the resourcebundles have to be
  * recreated accordingly and the runtime should be updated as well
  */
  private void init() {
    //System.out.println("reinit -> "+locale.toString() +" "+this+ " "+resourcePath);

    // load several resourcebundles to allow overlaying "invalid"(commented/empty) 
    // translations with an entry from the next sensible translation file
    // order is: langCode_countryCode, langCode, empty/default (english)
    // loads selected locale, selected language, empty locale
    this.resourceBundle = locale.getCountry().isEmpty() ? null : getResourceBundle(locale);
    // loads lang only locale or empty
    this.resourceBundle2 = locale.getLanguage().isEmpty() ? null : getResourceBundle( new Locale(locale.getLanguage()) );
    // loads only empty default fallback locale (english in our case)
    this.resourceBundle3 = getResourceBundle(Locale.ROOT);

    // this indicates that something with the classpath is really wrong, hence log an error
    if (this.resourceBundle == null && this.resourceBundle2 ==null && this.resourceBundle3 == null)
      Logger.error("All resourcebundles for '"+this.resourcePath+"' returned NULL. This is most likely wrong! Check the classpath.");
  }

  /**
   * find the exactly matching resourcebundle or return null if not available
   */
  private ResourceBundle getResourceBundle(Locale loco) {
    // limit fetching to exactly this locale
    ResourceBundle.Control rbc = new ResourceBundle.Control() {
      @Override
      public List<Locale> getCandidateLocales(String name, Locale locale) {
        return Collections.singletonList(loco);
      }
      @Override
      public Locale getFallbackLocale(String baseName, Locale locale) {
        return null;
      }
    };
    ResourceBundle rb;
    ClassLoader cl = classLoader instanceof ClassLoader ? classLoader : getClass().getClassLoader();
    try {
      rb = ResourceBundle.getBundle(resourcePath, loco, cl, rbc);
    } catch (MissingResourceException e) {
      rb = null;
    }

    return rb;
  }

  public static void reset() {
    initialized = false;
  }

  /**
   * Set the class loader used to load resource bundles, must be called by the
   * PlugInManager (plugin jars are added to a child classloader there) to allow
   * plugins to make use of this I18N class.
   * 
   * @param cl the classLoader to set
   */
  public static void setClassLoader(ClassLoader cl) {
    if (cl == null)
      throw new IllegalArgumentException("Classloader must not be null.");
    classLoader = cl;
    // reinit rsbs later
    reset();
  }

  /**
   * Set locale from string and (re)init
   * 
   * @param localeNew the code for the lang and country locale to set
   */
  public static void setLocale(Locale localeNew) {
    if (localeNew == null)
      throw new InvalidParameterException("Parameter localeNew must not be Null.");

    locale = localeNew;
    // reinit rsbs later
    reset();
  }

  public static Locale getLocale() {
    return locale;
  }

  /**
   * Get the I18N instance for a category or a path. The resource files are
   * resolved and at least one must exist in the classpath.
   * 
   * Examples:
   * 
   * categoryPrefixOrPath = new String("org.openjump.myplugin") then
   * resourcebundle is looked up as
   * /org/openjump/myplugin/language/jump[_locale].properties
   * 
   * categoryPrefixOrPath = new File("language/wfs/messages") then resourcebundle
   * is looked up as /language/wfs/messages[_locale].properties
   * 
   * @param categoryPrefixOrPathOrI18N The category.
   * @return The instance.
   */
  private static I18N getInstance(final Object categoryPrefixOrPathOrI18N) {
    I18N instance = instances.get(categoryPrefixOrPathOrI18N);

    if (instance == null) {
      if (categoryPrefixOrPathOrI18N instanceof File) {
        instance = new I18N((File) categoryPrefixOrPathOrI18N);
      } else {
        instance = new I18N(categoryPrefixOrPathOrI18N.toString());
      }
      instance.init();
      instances.put(categoryPrefixOrPathOrI18N, instance);
    }

    return instance;
  }

  /**
   * Create & return the I18N instance for the given prefix
   * 
   * @param categoryPrefix e.g. "my.cool.extension"
   * @return I18N object for this category
   */
  public static I18N getInstance(String categoryPrefix) {
    return getInstance((Object) categoryPrefix);
  }

  /**
   * Create & return the I18N instance for the given path
   * 
   * @param path, some path in classpath e.g. new File('my/cool/extension/')
   * @return I18N object
   */
  public static I18N getInstance(File path) {
    return getInstance((Object) path);
  }

  /**
   * Return the default I18N singleton for OJ2
   * for use like I18N.getInstance().get()
   * 
   * @return singleton I18N object for OpenJUMP
   */
  public static I18N getInstance() {
    // is initialized statically above
    return JUMP;
  }

  /**
   * utility method to convert a simplified locale string e.g. 'en_US'
   * to a usable java.util.locale object
   * 
   * @param localeCode a locale code String
   * @return a Locale from the localeCode
   */
  public static Locale fromCode(final String localeCode) {
    // [Michael Michaud 2007-03-04] handle the case where lang is the only
    // variable instead of catching an ArrayIndexOutOfBoundsException
    String[] lc = localeCode.split("_");
    Locale locale = Locale.getDefault();
    if (lc.length > 1) {
      Logger.debug("lang:" + lc[0] + " " + "country:" + lc[1]);
      locale = new Locale(lc[0], lc[1]);
    } else if (lc.length > 0) {
      Logger.debug("lang:" + lc[0]);
      locale = new Locale(lc[0]);
    } else {
      Logger.error(localeCode + " is an illegal argument to define lang [and country]");
    }

    return locale;
  }

  /***
   * Utility method. Applies a given locale to the java runtime.
   * 
   * @param loc the Locale to apply
   */
  public static void applyToRuntime(Locale loc) {
    Locale.setDefault(loc);
    System.setProperty("user.language", loc.getLanguage());
    System.setProperty("user.country", loc.getCountry());
  }

  /**
   * Get the short signature for locale (letters extension :language 2 letters +
   * "_" + country 2 letters)
   * 
   * @return string signature for locale
   */
  public static String getLocaleString() {
    return locale.getLanguage() + "_" + locale.getCountry();
  }

  /**
   * Get the short signature for language (letters extension :language 2 letters)
   * of the default instance
   * 
   * @return string signature for language
   */
  public static String getLanguage() {
    return locale.getLanguage();
  }

  /**
   * Get the short signature for country (2 letter code) of the default instance
   * 
   * @return string signature for country
   */
  public static String getCountry() {
    return locale.getCountry();
  }

  /**
   * Get the internationalized text from the resource bundle associated with the
   * specified category or path. If no label is defined then a default string is
   * created from the last part of the key.
   * 
   * Supports re-usage of translations in default instance via '$J:' prefix. e.g.
   *   some.other.key = $J:reusable.generic.translation
   *  will be looked up and the content of
   *   reusable.generic.translation = some cool translation
   *  returned.
   * 
   * Examples:
   * 
   * categoryPrefixOrPathOrI18N instanceof I18N legacy option, mainly for the
   * instance method this.getText(String)
   * 
   * categoryPrefixOrPathOrI18N = new String("org.openjump.myplugin") then
   * resourcebundle is looked up as
   * /org/openjump/myplugin/language/jump[_locale].properties
   * 
   * categoryPrefixOrPath = new File("language/wfs/messages") then resourcebundle
   * is looked up as /language/wfs/messages[_locale].properties
   *
   * @param label                      Label with argument insertion : {0}
   * @param objects                    values of parameters contained in the key
   * 
   * @return i18n label
   */
  public String get(final String label, final Object... objects) {
    // refresh in case settings changed inbetween
    initAll();

    if (StringUtils.isBlank(label))
      throw new IllegalArgumentException("label must not be empty!");

    // IMPORTANT: trailing spaces break the Malayalam translation,
    // so we trim here, just to make sure
    String text = getValue(label).trim();

    // reread in case of reused i18n vars '$J:'
    if (text.startsWith("$J:"))
      text = getInstance().getValue(text.substring(3).trim());

    // no params, nothing to parse
    if (objects.length < 1)
      return text;

    // parse away
    final MessageFormat mformat = new MessageFormat(text);
    String res = mformat.format(objects);
    return res;
  }

  /**
   * Find value for the key, - respect validity and use next rb order being
   * 'lang_Country', 'lang', '' default - eventually return key based
   * value if all fails and issue a warning in log
   * 
   * @param key key to retrieve
   * @return the value for the key
   */
  private String getValue(final String key) {
    String text;
      // try lang_country resourcebundle
      if (resourceBundle != null && isValid(text = findKeyInResourceBundle(resourceBundle,key)))
        return text;
      // try language only resourcebundle
      if (resourceBundle2 != null && isValid(text = findKeyInResourceBundle(resourceBundle2,key)))
        return text;
      // eventually use base resourcebundle
      if (resourceBundle3 != null && isValid(text = findKeyInResourceBundle(resourceBundle3,key)))
          return text;

      // last resort fallback is to simply reuse the last key segment as value
      final String[] labelpath = key.split("\\.");
      text = labelpath[labelpath.length - 1];

      // only complain once
      if (!missing.contains(key)) {
        String msg = "No translation for key ''{0}'' in bundle ''{1}''.\nUsing last segment of key instead: ''{2}''";
        msg = MessageFormat.format(msg, key, resourcePath, text);

        Logger.warn(msg);

        // remember, so we don't flood the log
        missing.add(key);
      }

      return text;
  }

  private String findKeyInResourceBundle( ResourceBundle rb, String key) {
    String value = null;
    try {
      value = rb.getString(key);
    } catch (MissingResourceException e) {
      // yeah, it's not there, so what?!
    }
    return value;
  }
  
  /**
   * We ignore empty or untranslated strings when we find them
   * 
   * @param text internationalized string to check
   */
  private boolean isValid(String text) {
    return text != null && !text.trim().equals("") && !text.trim().startsWith("#T:");
  }
}
