/**
 * @author Olivier BEDEL
 * 	Laboratoire RESO UMR 6590 CNRS
 * 	Bassin Versant du Jaudy-Guindy-Bizien
 * 	26 oct. 2004
 * 
 */
package org.openjump.sigle.plugin.joinTable;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * @author Olivier BEDEL
 * Laboratoire RESO UMR 6590 CNRS
 * Bassin Versant du Jaudy-Guindy-Bizien
 * 26 oct. 2004
 * license Licence CeCILL http://www.cecill.info/
 * 
 */
public interface JoinTableDataSource {
	
	public ArrayList getFieldNames();
	
	public ArrayList getFieldTypes();
	
	public Hashtable buildTable (int keyIndex);
	
}
