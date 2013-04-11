/*
 * Created on 09.11.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.FeatureCollection;

/**
 * role for a feature collection containing gridded data
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 */
public class RoleApplicationGrid extends PirolFeatureCollectionRole {

    public RoleApplicationGrid() {
        super(PirolFeatureCollectionRoleTypes.GRID);
    }


    /**
     *@inheritDoc
     */
    public boolean containsGrid() {
        return true;
    }
    
    

}
