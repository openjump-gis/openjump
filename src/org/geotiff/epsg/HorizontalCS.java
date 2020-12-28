
package org.geotiff.epsg;


  /**
   * Represents the base class of the EPSG  
   * horizontal coordinate systems. It is also
   * contains the factory method for constructing 
   * these things.
   *
   * @author Niles D. Ritter
   */

public abstract class HorizontalCS {

	// registered EPGS codes
	public static int WGS84=4326;

	private int code;

	protected HorizontalCS(int code) { setCode(code); }

	protected void setCode(int aCode) {
	  code = aCode;
	}

	public int getCode() {
	  return code;
	}

	/**
	 * This method must be implemented by the concrete class
	 * to return the underlying geographic coordinate system.
	 * @return the GeographicCS for this HorizontalCS
	 */
	public abstract HorizontalCS getGeographicCS();

	/**
	 * Factory method for coordinate systems.
	 * @param code EPSG code of this Coordinate Reference System
	 * @return a Horizontal Coordinate Reference System
	 * @throws InvalidCodeException if code is not valid
	 */
	public static HorizontalCS create(int code) 
	 throws InvalidCodeException
	 {
	    if (code < 0) throw new InvalidCodeException("whatever");
	
	    if (code < 5000)
		return new GeographicCS(code);
	    else
		return new ProjectedCS(code);
	 }
}
