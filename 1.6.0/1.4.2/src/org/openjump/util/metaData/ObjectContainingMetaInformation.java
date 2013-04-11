/*
 * Created on 05.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2434 $
 *  $Date: 2006-09-12 12:31:50 +0200 (Di, 12 Sep 2006) $
 *  $Id: ObjectContainingMetaInformation.java 2434 2006-09-12 10:31:50Z LBST-PF-3\orahn $
 */
package org.openjump.util.metaData;

/**
 * TODO: comment class
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2434 $
 * 
 */
public interface ObjectContainingMetaInformation {
    
    public MetaDataMap getMetaInformation();

    public void setMetaInformation(MetaDataMap metaInformation);

}
