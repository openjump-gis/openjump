/*
 * Created on 30.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.ui.tools;

/**
 * Interface for a class that checks if the values in e.g. a dialog are ok, so we can proceed or not.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * @see de.fhOsnabrueck.jump.pirol.dialogs.OKCancelListener
 * 
 */
public interface ValueChecker {
    public boolean areValuesOk();
}
