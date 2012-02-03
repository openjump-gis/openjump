/*
 * Created on 17.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.debugOutput;

/**
 * 
 * Contains constants for "registered" logging user ids 
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * @see PersonalLogger
 */
public enum DebugUserIds implements DebugId {
	STEFAN("stefan_ostermann"),
	CARSTEN("carsten_schulz"),
	OLE("ole_rahn"),
	MICHAEL("michael_bruening"),
	HAYO("hayo_janssen"),
	ALL("alluser");
	
	protected String userName = null;
	
	private DebugUserIds(String name){
		this.userName = name;
	}

	/* (non-Javadoc)
	 * @see de.fhOsnabrueck.jump.pirol.utilities.debugOutput.DebugId#getUserName()
	 */
	public String getUserName() {
		return userName;
	}
	
	
}
