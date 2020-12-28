
package org.geotiff.epsg;


  /**
   * Represents the Projected  coordinate
   * system.
   *
   * @author Niles D. Ritter
   */

public class ProjectedCS extends HorizontalCS {

	GeographicCS geographicCS;

	/**
	 * Protected Constructor; use the factory method
	 * in HorizontalCS to make this.
	 * @param code EPSG code of this ProjectedCS
	 */
	protected ProjectedCS(int code) { 
		super(code); 

		//NB: Must construct the GeographicCS
		//associated with this PCS!!
	}

	/**
	 * Standard accessor.
	 * @return the GeographicCS of this ProjectedCS
	 */
	public HorizontalCS getGeographicCS() {return geographicCS;}

}
