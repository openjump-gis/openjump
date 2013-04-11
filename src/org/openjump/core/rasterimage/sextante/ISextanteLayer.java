package org.openjump.core.rasterimage.sextante;

import java.awt.geom.Rectangle2D;

/**
 * Interface for layers
 * @author volaya
 *
 */
public interface ISextanteLayer extends ISextanteDataObject{

	/**
	 * Returns the extent covered by the layer
	 * @return the extent of the layer
	 */
	public Rectangle2D getFullExtent();

	/**
	 * Returns an object with information about the CRS
	 * associated to this layer. The class of this object
	 * depends on the implementation of this interface
	 * @return An object with information about the CRS
	 * used for this layer (i.e. a string with a EPSG code)
	 */
	public Object getCRS();

}
