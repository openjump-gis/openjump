/*
 * Created on 13.04.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package org.openjump.core.apitools;

import java.util.List;

/**
 * Class for more convenient use of Lists and Arrays.
 * 
 * @author Ole Rahn
 * 
 * FH Osnabrück - University of Applied Sciences Osnabrück
 * Project PIROL 2005
 * Daten- und Wissensmanagement
 * 
 */
public class CollectionsTools extends ToolToMakeYourLifeEasier {

    public static boolean addArrayToList( List toAddTo, Object[] arrayToBeAdded ){
        
        for ( int i=0; i<arrayToBeAdded.length; i++ ){
            toAddTo.add(arrayToBeAdded[i]);
        }
        
        return true;
    }

}
