/*
 * Created on 02.08.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.i18n;

import org.openjump.core.apitools.HandlerToMakeYourLifeEasier;

import com.vividsolutions.jump.I18N;

/**
 * Handles i18n stuff for PIROL plugIns.<br>
 * Class that Eclipse generates, if the "Externalize Strings" command is used.
 * Was renamed (from <code>Messages.java</code>) and modified to use the
 * openJump i18n plug - the interface stayed the same!<br>
 * Wrapper for the i18N to make work with PIROL labels easier.
 *
 * @author Ole Rahn <br>
 *         <br>
 *         FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 *         <br>
 *         Project: PIROL (2005), <br>
 *         Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 */
public class PirolPlugInMessages implements HandlerToMakeYourLifeEasier {
  /**
   * We don't need instances of the class!
   */
  private PirolPlugInMessages() {
  }

  /**
   * Get a translated PIROL text from the i18N system.
   * 
   * @param key the key (name) for the the text
   * @return the translated text
   */
  public static String getString(String key) {
    return I18N.getInstance().get(key);
  }

}
