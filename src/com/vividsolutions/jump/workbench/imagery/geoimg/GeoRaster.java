package com.vividsolutions.jump.workbench.imagery.geoimg;

/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
import com.vividsolutions.jump.workbench.model.Prioritized;
import it.geosolutions.imageio.gdalframework.GDALImageReaderSpi;
import it.geosolutions.imageio.gdalframework.GDALUtilities;
import it.geosolutions.imageio.utilities.ImageIOUtilities;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RecyclingTileFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.util.ImagingListener;

import org.gdal.gdal.gdal;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.SeekableStream;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.model.Disposable;

public class GeoRaster implements Disposable {
  protected String imageFileLocation;
  private URI uri = null;
  protected Object fixed_reader;
  protected RenderedOp src = null;
  private ImageReader src_reader = null;
  private Object src_input = null;
  protected String type = null;
  protected Object used_loader = null;
  protected RenderingHints cache_hints;
  private TileCache cache;
  private RecyclingTileFactory rtf;

  // Image enhancement
  double[] min;
  double[] max;

  static {
    // System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    // we reroute JAI error messages to OJ log here
    JAI.getDefaultInstance().setImagingListener(new ImagingListener() {
      public boolean errorOccurred(String msg, Throwable thrown, Object where,
          boolean isRetryable) throws RuntimeException {
        Logger.error(thrown);
        return false;
      }
    });
  }

  static boolean areGDALClassesAvailable = false;
  static {
    try {
      Class.forName("it.geosolutions.imageio.gdalframework.GDALImageReaderSpi");
      Class.forName("it.geosolutions.imageio.gdalframework.GDALUtilities");
      Class.forName("it.geosolutions.imageio.utilities.ImageIOUtilities");
      areGDALClassesAvailable = true;
    } catch (NoClassDefFoundError|ClassNotFoundException e) {
      Logger.debug(e);
    } // eat it
  }

  public GeoRaster(String imageFileLocation) {
    this(imageFileLocation, null);
  }

  public GeoRaster(String imageFileLocation, Object fixed_reader) {
    this.imageFileLocation = imageFileLocation;
    this.fixed_reader = fixed_reader;
  }

  protected URI getURI() {
    return uri;
  }
  
  /**
   * Basic fetchRasters retrieves a raster from a file. To get a raster from
   * somewhere else, override this method in subclasses.
   *
   * @throws ReferencedImageException if an Exception occurs in File#canRead, in
   *    createJAIRenderedOP or if no reader wants to decode the Raster
   */
  protected void fetchRaster() throws ReferencedImageException {
    // we accept either URI strings or string file paths
    Logger.trace("imageFileLocation is -> "+imageFileLocation);
    try {
      uri = new URI(imageFileLocation);
      if (!uri.isAbsolute()) // means it has a scheme://
        throw new URISyntaxException(imageFileLocation, "missing scheme://");
    } catch (URISyntaxException e) {
      Logger.trace("not an URI, will treat as path -> "+imageFileLocation, e);
      File file = new File(imageFileLocation);
      uri = file.toURI();
    }
    Logger.trace("uri is now -> " + uri);
    // check availability early
    if (!new File(uri).canRead())
      throw new ReferencedImageException("cannot read file -> "+imageFileLocation);

    // prepare jai parameters
    final ParameterBlockJAI pbjImageRead;
    final ImageReadParam param = new ImageReadParam();
    pbjImageRead = new ParameterBlockJAI("ImageRead");
    pbjImageRead.setParameter("readParam", param);

    // route, if fixed_reader was set
    List<Object> affirmed_readers;
    fixed_reader = null;
    // default case, auto detection
    if (fixed_reader == null) {
      affirmed_readers = new ArrayList<>(listValidImageIOReaders(uri, null));
      //try {
      //  affirmed_readers = listValidReaders(uri);
      //} catch(IOException e) {
      //  throw new ReferencedImageException(e);
      //}
      //affirmed_readers.sort(Comparator.comparingInt(it -> GeoImageFactory.getPriority(it)));
      // sort readers by priority
      Collections.sort(affirmed_readers, new Comparator<Object>() {
        public int compare(final Object o1, final Object o2) {
          final Prioritized p1 = new Prioritized() {
            public int getPriority() {
              return GeoImageFactory.getPriority(o1);
            }
          };
          final Prioritized p2 = new Prioritized() {
            public int getPriority() {
              return GeoImageFactory.getPriority(o2);
            }
          };
          return Prioritized.COMPARATOR.compare(p1, p2);
        }
      });
      Logger.info("Available sorted readers : " + affirmed_readers);
    }
    // fixed reader is imageio reader
    else if (fixed_reader instanceof ImageReaderSpi) {
      affirmed_readers = Collections.singletonList((ImageReaderSpi) fixed_reader);
      Logger.info("Fixed reader : " + affirmed_readers);
    }
    else {
      // fixed reader is something else, hopefully jai codec ;)
      // simply define an empty imageio reader list here to skip to jai below
      affirmed_readers = Collections.emptyList();
    }

    // this is skipped if list is empty

    Logger.info("Available readers : " + affirmed_readers);
    for (Object reader : affirmed_readers) {

      Logger.info("Trying reader " + GeoImageFactory.loaderString(reader));

      if (reader instanceof ImageReaderSpi) {
        ImageReaderSpi readerSpi = (ImageReaderSpi) reader;
        try {
          src_input = createInput(uri, reader);

          src_reader = readerSpi.createReaderInstance(/* src_input */);

          src_reader.setInput(src_input);
          pbjImageRead.setParameter("Input", src_input);
          pbjImageRead.setParameter("Reader", src_reader);

          src = JAI.create("ImageRead", pbjImageRead, null);

          // success OR dispose & try plain JAI below
          if (src != null /*&& src.getWidth() > 0*/) {
            // set info vars
            type = src_reader.getFormatName();
            used_loader = src_reader;
            return;
          } else {
            dispose();
          }
        } catch (Exception e) {
          // if fixed_reader failed, it failed finally and we'll have to throw the reason
          if (fixed_reader != null && fixed_reader == reader)
            throw new ReferencedImageException(e);
          // ok, this didn't work try the next one
          Logger.debug(e);
          // clean up any residue
          dispose();
        }
      }
    }

    // try JAI codec as fallthrough or if defined
    try {
      if (fixed_reader == null || fixed_reader instanceof ImageCodec)
        createJAIRenderedOP( uri, (ImageCodec)fixed_reader);
    } catch (IOException e) {
      throw new ReferencedImageException(e);
    }

    // success OR dispose & fail
    if (src != null)
      return;
    else
      dispose();

    throw new ReferencedImageException("no one wants to decode me.");
  }

  protected void readRasterfile() throws ReferencedImageException {
    // ===========================
    // Load the image, any format.
    // ===========================
    fetchRaster();

    // ======================================
    // Image can be distorted, make it square
    // in modelspace.
    // ======================================
    normalize(src);
  }

  /**
   * This method must be overridden if an image is not a square image in
   * modelspace. It should be transformed to make it a square image in
   * modelspace.
   * 
   * @param image image to be normalized (squared)
   */
  protected void normalize(RenderedOp image) {
  }

  @Deprecated // use getRenderedOP() instead
  public RenderedOp getImage() throws ReferencedImageException {
    return getRenderedOp();
  }

  public RenderedOp getRenderedOp() throws ReferencedImageException {
    if (src == null)
      readRasterfile();
    return src;
  }

  public RenderedOp fullContrast() {
    int bands = src.getNumBands();
    double[] constants = new double[bands];
    double[] offsets = new double[bands];
    for (int i = 0; i < bands; i++) {
      constants[i] = 1.2 * 255 / (max[i] - min[i]);
      offsets[i] = 255 * min[i] / (min[i] - max[i]);
    }

    ParameterBlock pb = new ParameterBlock();
    pb.addSource(src);
    pb.add(constants);
    pb.add(offsets);
    return JAI.create("rescale", pb, null);
  }

  public double[] getMinimumExtreme() {
    return min;
  }

  public double[] getMaximumExtreme() {
    return max;
  }

  protected String getType() {
    return type;
  }

  protected Object getLoader() {
    return used_loader;
  }

  // TODO: probably better moved to GeoImage where the rendering is actually handled
  public RenderingHints createCacheRenderingHints() {
    if (src != null && src.getWidth() > 2000
        && src.getHeight() > 2000 && cache_hints == null) {
      // use 64MB for images, default 16MB is kinda small
      cache = JAI.createTileCache(1024 * 1024 * 64L);
      // create hints
      cache_hints = new RenderingHints(JAI.KEY_TILE_CACHE, cache);
      rtf = new RecyclingTileFactory();
      cache_hints.put(JAI.KEY_TILE_CACHE, cache);
      cache_hints.put(JAI.KEY_TILE_FACTORY, rtf);
      cache_hints.put(JAI.KEY_TILE_RECYCLER, rtf);
      cache_hints.put(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED, Boolean.TRUE);
    }
    return cache_hints != null ? (RenderingHints) cache_hints.clone() : null;
  }

  protected void createJAIRenderedOP(URI uri, ImageCodec codec)
      throws IOException {
    // create an input
    Object input = createInput(uri);
    // create a temp stream to find all candidate codecs if codec was given
    String[] decs;
    if (codec != null) {
      SeekableStream is2 = SeekableStream.wrapInputStream(
          createInputStream(uri), true);
      decs = ImageCodec.getDecoderNames((SeekableStream) is2);
      disposeInput(is2);
    } else {
      decs = new String[] {};
    }

    List<ImageCodec> removed_codecs = new ArrayList<ImageCodec>();
    try {
      // remove all codecs except selected
      for (String name : decs) {
        ImageCodec candidate_codec = ImageCodec.getCodec(name);
        if (!codec.getClass().isAssignableFrom(candidate_codec.getClass())) {
          ImageCodec.unregisterCodec(name);
          removed_codecs.add(candidate_codec);
//          System.out.println("removed " + name);
        }
      }
//      SeekableStream is2 = SeekableStream.wrapInputStream(createInputStream(uri), true);
//      decs = ImageCodec.getDecoderNames((SeekableStream) is2);
//      System.out.println(Arrays.toString(decs));
//      disposeInput(is2);

      if (input instanceof InputStream) {
        if (!(input instanceof SeekableStream))
          input = SeekableStream.wrapInputStream((InputStream) input, true);
        src = JAI.create("stream", input);
      } else {
        src = JAI.create("fileload", uri.getPath());
      }
      // set info vars
      type = codec.getFormatName();
      used_loader = codec;
    } finally {
      // reregister removed codecs
      for (ImageCodec imageCodec : removed_codecs) {
        ImageCodec.registerCodec(imageCodec);
      }
    }
  }

  public void dispose() {
    if (src instanceof RenderedImage) {
      if (areGDALClassesAvailable)
        ImageIOUtilities.disposeImage(src);
      src = null;
    }
    if (src_reader instanceof ImageReader) {
      src_reader.reset();
      src_reader.dispose();
      src_reader = null;
    }
    disposeInput(src_input);
    
    if (cache instanceof TileCache)
      cache.flush();
    
    if (rtf instanceof RecyclingTileFactory)
      rtf.flush();
  }

  // static protected boolean canDecode(ImageReaderSpi provider, URI uri)
  // throws IOException {
  // Object input = createInput(uri);
  //
  // boolean canDec = false;
  // // some readers insist on a filestream for checking (e.g. tif, png)
  // if (input instanceof File) {
  // FileImageInputStream fis = new FileImageInputStream((File) input);
  // canDec = canDec || provider.canDecodeInput(fis);
  // disposeInput(fis);
  // }
  // // while some others like a file object (e.g. ecw, mrsid)
  // // we simply try both and regard only a successful answer
  // return provider.canDecodeInput(input) || canDec;
  // }

  static protected boolean hasFileExtension(ImageReaderSpi provider, URI uri) {
    String path = CompressedFile.getTargetFileWithPath(uri);
    String ext = CompressedFile.getExtension(path);
    return hasFileExtension(provider,ext);
  }

  static protected boolean hasFileExtension(ImageReaderSpi provider, String ext) {
    return Arrays.asList(provider.getFileSuffixes()).contains(ext);
  }

  static protected boolean hasNoFileExtensions(ImageReaderSpi provider) {
    String[] exts = provider.getFileSuffixes();
    return exts.length == 0
        || (exts.length == 1 && exts[0] instanceof String && exts[0].trim()
            .isEmpty());
  }
  
  // limit cache to last 10 entries
  static private LinkedHashMap<URI, List<ImageReaderSpi>> validIOReaderCache = new LinkedHashMap<URI, List<ImageReaderSpi>>() {
    protected boolean removeEldestEntry(Entry<URI, List<ImageReaderSpi>> arg0) {
      return size() > 10;
    }
  };

  /**
   * create a list of ImageReaderSpi's supposedly able to open the URI
   */
  static protected List<ImageReaderSpi> listValidImageIOReaders(URI uri,
      Class filter) {

    resetGDALReaderSelection();

    // fetch all readers
    // Don't know why this one is not automatically finded
    // It is useful as it does not break on some wrong TiffTags like byte array nodata
    // which breaks geosolutions imageio-ext reader
    IIORegistry.getDefaultInstance().registerServiceProvider(
        new com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi(), ImageReaderSpi.class
    );
    final Iterator<? extends ImageReaderSpi> iter = IIORegistry
     .getDefaultInstance().getServiceProviders(ImageReaderSpi.class, false);
    
    //Logger.trace("IIORegistry holds "+Arrays.toString(Lists.newArrayList(iter).toArray()));
    
    // iterate all readers and return only valid ones
    ImageReaderSpi provider;
    List<ImageReaderSpi> affirmed_readers = new Vector<>();
    while (iter.hasNext()) {
      provider = iter.next();
      //Logger.trace("test "+GeoImageFactory.loaderString(provider));

      if (filter != null && !(filter.isInstance(provider)))
        continue;

      // imageio-ext is botched here. actually it registers loaders, that don't
      // check if they are really existing in the underlying gdal build
      // no prob, we simply do the checking here then
      if (areGDALClassesAvailable && provider instanceof GDALImageReaderSpi
          && !((GDALImageReaderSpi) provider).isAvailable() ) {
        continue;
      }

      Object input = null;
      boolean canDec = false;
      try {
        input = createInput(uri, provider);
        canDec = /*provider instanceof GDALImageReaderSpi ||*/
            provider.canDecodeInput(input);
      } catch (Exception e) {
        // hmm, failing to create an input is fatal for this reader
        // some providers insist on a physical file, which we cannot deliver for compressed sources
        Logger.debug(e);
        continue;
      } finally {
        disposeInput(input);
      }

      boolean hasNoExts = hasNoFileExtensions(provider);
      boolean hasExt = hasFileExtension(provider, uri);

      // either decoding or extension suffice for our purposes
      if (!canDec && !hasExt /*&& !hasNoExts*/)
        continue;

      affirmed_readers.add(provider);
    }
    // make list readonly for cache
    affirmed_readers = Collections.unmodifiableList(affirmed_readers);

    validIOReaderCache.put(uri, affirmed_readers);

    // return a copy of the cached list
    return affirmed_readers;
  }

  /**
   * Create a list of ImageCodec's supposedly able to open URI.
   * From com.sun.media.jai.codec.ImageCodec
   */
  static protected List<ImageCodec> listValidJAICodecs(URI uri)
      throws IOException {
    InputStream is = createInputStream(uri);
    SeekableStream ss = SeekableStream.wrapInputStream(is, true);
    String[] decs = ImageCodec.getDecoderNames(ss);
    disposeInput(ss);
    disposeInput(is);
    List l = new ArrayList();
    for (String dec : decs) {
      ImageCodec c = ImageCodec.getCodec(dec);
      l.add(c);
    }
    return l;
  }

  /**
   * create a list of JAI ImageCodec's and ImageIO readers supposedly able to open URI
   */
  static public List<Object> listValidReaders(URI uri) throws IOException {
    List<Object> l = new ArrayList<>(listValidImageIOReaders(uri, null));
    l.addAll(listValidJAICodecs(uri));
    return l;
  }
  
  /**
   * list all JAI ImageCodec's and ImageIO readers available in this jre
   */
  static public List<Object> listAllReaders() {
    List<Object> loaders = new ArrayList<>();
    // add imageio readers
    Iterator<? extends ImageReaderSpi> iter = IIORegistry
        .getDefaultInstance().getServiceProviders(ImageReaderSpi.class, true);
    while (iter.hasNext()) {
      ImageReaderSpi provider = iter.next();
      Logger.trace("GeoRaster.listAllReaders(): "+provider.toString());
      // imageio-ext is botched here. actually it registers loaders, that don't
      // check if they are really existing in the loaded gdal build
      if (areGDALClassesAvailable && provider instanceof GDALImageReaderSpi
          && !((GDALImageReaderSpi) provider).isAvailable() ) {
        Logger.trace("unavailable");
        continue;
      }
      Logger.trace("added");
      loaders.add(provider);
    }
    // add JAI codecs
    loaders.addAll(Collections.list(ImageCodec.getCodecs()));
    return loaders;
  }

  static protected Object createInput(URI uri) throws IOException {
    return createInput(uri, null);
  }

  /**
   * @param uri the URI to read
   * @param loader optional ImageReaderSpi
   * @return the input as an InputStream, a File or a ImageInputStream
   * @throws IOException
   */
  static protected Object createInput(URI uri, Object loader)
      throws IOException {

    Object input;
    if (CompressedFile.isArchive(uri) || CompressedFile.isCompressed(uri)) {
      InputStream src_is;
      src_is = CompressedFile.openFile(uri);
      src_is = new BufferedInputStream(src_is);
      input = src_is;
    } else {
      // create a File object, native loaders like ecw, mrsid seem to insist on
      // it, error was:
      // "Unable to create a valid ImageInputStream for the provided input:"
      // took me two days to debug this.. pfffhhhh
      // if you find this workaround because of the full error string above,
      // that was intentional, enjoy ede
      // UPDATE: check below, turns out ImageReaderSpi's actually tell you what
      // input they'd like to have
      input = new File(uri);
    }

    if (loader == null)
      return input;

    if (loader instanceof ImageReaderSpi) {
      // how may i serve you today?
      Class[] clazzes = ((ImageReaderSpi) loader).getInputTypes();
      List<Class> intypes = clazzes != null ? Arrays.asList(clazzes)
          : new ArrayList<>();
      //System.out.println("GR in types: " + intypes);
      for (Class clazz : intypes) {
        // already reader compliant? off you f***
        if (clazz.isInstance(input))
          return input;
        // want an ImageInputStream? try to build one..
        if (ImageInputStream.class.equals(clazz)) {
          // this returns null if it can't build one from given input
          ImageInputStream iis = ImageIO.createImageInputStream(input);
          if (iis != null)
            return iis;
        }
      }

      throw new IOException("Couldn't create an input for '" + uri
          + "' accepted ("+StringUtil.toCommaDelimitedString(intypes)+")by reader '" + loader + "'");
    }

    return input;
  }

  static protected InputStream createInputStream(URI uri) throws IOException {
    Object in = createInput(uri);
    if (in instanceof String)
      in = new File((String) in);
    if (in instanceof File)
      in = new FileInputStream((File) in);
    return (InputStream) in;
  }

  static protected void disposeInput(Object input) {
    if (input instanceof Closeable)
      FileUtil.close((Closeable) input);
  }

  static void resetGDALReaderSelection() {
    if (!areGDALClassesAvailable || !GDALUtilities.isGDALAvailable())
      return;
    gdal.SetConfigOption("GDAL_SKIP", "");
    gdal.AllRegister();
  }
}