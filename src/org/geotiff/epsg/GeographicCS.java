
package org.geotiff.epsg;


  /**
   * Represents the Geographic (Datum) coordinate
   * system.
   *
   * @author Niles D. Ritter
   */

public class GeographicCS extends HorizontalCS {

	/**
	 * Protected Constructor; use the factory method
	 * in HorizontalCS to make this.
	 * @param code EPSG code of this GeographicCS
	 */
	protected GeographicCS(int code) { super(code); }

	/**
	 * Standard accessor.
	 * @return itself (the HorizontalCS of a GeographicCS is itself)
	 */
	public HorizontalCS getGeographicCS() {return this;}

}
