/*
 * Created on 30.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author: michaudm $
 *  $Rev: 1559 $
 *  $Date: 2008-10-05 16:54:14 -0600 (So, 05 Okt 2008) $
 *  $Id: ValueChecker.java 1559 2008-10-05 22:54:14Z michaudm $
 */
package org.openjump.core.ui.swing;

/**
 * Interface for a class that checks if the values in e.g. a dialog are ok, so we can proceed or not.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 1559 $
 * @see org.openjump.core.ui.swing.listener.OKCancelListener
 * 
 */
public interface ValueChecker {
    public boolean areValuesOk();
}
