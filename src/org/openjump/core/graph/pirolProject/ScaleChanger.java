/*
 * Created on 09.12.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.openjump.core.graph.pirolProject;


public interface ScaleChanger {
	double scale( double value, int dimension );
	double unScale( double value, int dimension );
}
