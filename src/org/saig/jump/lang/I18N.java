/* 
 * Kosmo - Sistema Abierto de Información Geográfica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2006, SAIG S.L.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, contact:
 * 
 * Sistemas Abiertos de Información Geográfica, S.L.
 * Avnda. República Argentina, 28
 * Edificio Domocenter Planta 2ª Oficina 7
 * C.P.: 41930 - Bormujos (Sevilla)
 * España / Spain
 *
 * Teléfono / Phone Number
 * +34 954 788876
 * 
 * Correo electrónico / Email
 * info@saig.es
 *
 */
package org.saig.jump.lang;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;

/**
 * kosmo i18n support patched over to use OJ i18n class
 */
public class I18N {

    /** Log */
    private final static Logger LOGGER = Logger.getLogger(I18N.class);

    /**
     * Acceso privado al único objeto de la clase
     */
    private static I18N instance = null;

    /**
     * Para permitir la extensión de la clase, el constructor se define como protegido.
     */
    protected I18N() {
        // ...
    }

    /**
     * Permite recuperar el unico objeto instanciable de la clase
     * 
     * @return El único objeto de la clase.
     */
    public static I18N getInstance() {
        if (null == instance) {
            instance = new I18N();
        }
        return instance;
    }

    public static String getString( String label ) {
        return com.vividsolutions.jump.I18N.get(label);
    }

    /**
     * Build a string from class object and append label to it. Then calls
     * {@link #getString(String)}
     * 
     * @param obj the object from what the string is to be built
     * @param label
     * @return i18n label If no resourcebundle is found, returns default string contained inside
     *         com.vividsolutions.jump.jump
     */
    public static String getString( Class< ? > obj, String label ) {
        return getString(getPrefix(obj) + label);
    }

    /**
     * Gets a string from the ojb's class
     * 
     * @param obj the object from what the string is to be built
     * @return an empty string if obj is null
     **/
    private static String getPrefix( Class< ? > obj ) {
        String prefix = ""; //$NON-NLS-1$
        if (obj != null) {
            // In order to avoid problems with inner classes
            while( obj != null && obj.getCanonicalName() == null ) {
                obj = obj.getEnclosingClass();
            }
            if (obj != null) {
                prefix = obj.getCanonicalName() + "."; //$NON-NLS-1$
            }
        }

        return prefix;
    }

    /**
     * Get the short signature for locale (letters extension :language 2 letters + "_" + country 2
     * letters)
     * 
     * @return string signature for locale
     */
    public static String getLocaleString() {
        return com.vividsolutions.jump.I18N.getLocale();
    }

    /**
     * Devuelve el locale asignado a las propiedades
     * 
     * @return
     */
    public static Locale getLocale() {
        return new Locale(getLocaleString());
    }

    /**
     * Get the short signature for language (letters extension :language 2 letters)
     * 
     * @return string signature for language
     */
    public static String getLanguage() {
        return com.vividsolutions.jump.I18N.getLanguage();
    }

    /**
     * Process text with the locale 'kosmo_<locale>.properties' file
     * 
     * @param label with argument insertion : {0}
     * @param objects
     * @return i18n label
     */
    public static String getMessage( String label, Object[] objects ) {
        return com.vividsolutions.jump.I18N.getMessage(label, objects);
    }

    /**
     * Build a string from object class and append label to it. Then calls
     * {@link #getMessage(String, Object[])}
     * 
     * @param obj the object from what the string is to be built
     * @param label with argument insertion : {0}
     * @param objects
     * @return i18n label
     */
    public static String getMessage( Class< ? > obj, String label, Object[] objects ) {
        return getMessage(getPrefix(obj) + label, objects);
    }

    /**
     * Process text with the locale 'pluginName_<locale>.properties' file
     * 
     * @param pluginClassName (path + name)
     * @param label
     * @return i18n label
     */

    public static String getString( String pluginClassName, String label ) {
        return getString(label);
    }

    /**
     * Process text with the locale 'pluginName_<locale>.properties' file
     * 
     * @param pluginClassName (path + name)
     * @param label with argument insertion : {0}
     * @param objects
     * @return i18n label
     */
    public static String getMessage( String pluginClassName, String label, Object[] objects ) {
        return getMessage(label, objects);
    }

}
