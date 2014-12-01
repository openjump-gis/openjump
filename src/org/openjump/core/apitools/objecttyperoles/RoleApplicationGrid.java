/*
 * Created on 09.11.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2434 $
 *  $Date: 2006-09-12 12:31:50 +0200 (Di, 12 Sep 2006) $
 *  $Id: RoleApplicationGrid.java 2434 2006-09-12 10:31:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.apitools.objecttyperoles;

/**
 * role for a feature collection containing gridded data
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2434 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class RoleApplicationGrid extends FeatureCollectionRole {

    public RoleApplicationGrid() {
        super(FeatureCollectionRoleTypes.GRID);
    }


    /**
     *@inheritDoc
     */
    public boolean containsGrid() {
        return true;
    }
    
    

}
