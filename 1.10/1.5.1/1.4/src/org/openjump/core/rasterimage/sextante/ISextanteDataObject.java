package org.openjump.core.rasterimage.sextante;

/**
 * Interface for data objects (layers and tables).
 * This interface should be used to wrap other data objects,
 * so they are compatible with SEXTANTE and thus can be used
 * as inputs to geoalgorithms
 * @author Victor Olaya volaya@unex.es
 *
 */
public interface ISextanteDataObject {

	/**
	 * Returns the base data object (i.e. the object that
	 * this class wraps, which contains the data itself)
	 * @return the base data object
	 */
	public Object getBaseDataObject();

	/**
	 * Returns the name of this data object
	 * @return the name of this data object
	 */
	public String getName();

	/**
	 * Sets a new name for this object
	 * @param sName the new name
	 */
	public void setName(String sName);

	/**
	 * Returns the filename associated to this data object.
	 * @return the filename associated to this data object.
	 * Can be null, since the object wrapped by this class doesn't
	 * have to be file-based.
	 */
	public String getFilename();

	/**
	 * This method post-processes the object after it has been created.
	 * If, for instance, data are kept in memory before they are dumped
	 * to file, this method should write that file.
	 */
	public void postProcess() throws Exception;

	/**
	 * This methods initialize the data object, so it is ready to be accessed
	 */
	public void open();

	/**
	 * This method closes the data object, which was opened using the
	 * open() method.
	 */
	public void close();

}
