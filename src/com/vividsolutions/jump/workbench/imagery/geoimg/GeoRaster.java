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
import it.geosolutions.imageio.utilities.ImageIOUtilities;

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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.util.ImagingListener;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.SeekableStream;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.model.Disposable;

public abstract class GeoRaster implements Disposable {
  protected String imageFileLocation;
  protected Object fixed_reader = null;
  protected RenderedOp src = null;
  private ImageReader src_reader = null;
  private Object src_input = null;
  // XTIFFDirectory dir = null;
  protected String type = null;
  protected String loader = null;

  // Image enhancement
  double[] min;
  double[] max;

  static {
    // System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    // we reroute JAI error messages to OJ log here
    JAI.getDefaultInstance().setImagingListener(new ImagingListener() {
      public boolean errorOccurred(String arg0, Throwable arg1, Object arg2,
          boolean arg3) throws RuntimeException {
        JUMPWorkbench.getInstance().getFrame().log("JAI Error: " + arg0);
        return false;
      }
    });
  }

  public GeoRaster(String imageFileLocation) {
    this(imageFileLocation, null);
  }

  public GeoRaster(String imageFileLocation, Object fixed_reader) {
    this.imageFileLocation = imageFileLocation;
    this.fixed_reader = fixed_reader;

  }

  /**
   * Basic fetchRasters retrieves a raster from a file. To get a raster from
   * somewhere else, override this method in subclasses.
   * 
   * @throws URISyntaxException
   * @throws IOException
   * @throws ReferencedImageException
   */
  protected void fetchRaster() throws ReferencedImageException {
    URI uri;
    try {
      uri = new URI(imageFileLocation);
    } catch (URISyntaxException e) {
      throw new ReferencedImageException(e);
    }

    // prepare jai parameters
    final ParameterBlockJAI pbjImageRead;
    final ImageReadParam param = new ImageReadParam();
    pbjImageRead = new ParameterBlockJAI("ImageRead");
    pbjImageRead.setParameter("readParam", param);

    try {
      // route, if fixed_reader was set
      List affirmed_readers;
      // default case, auto detection
      if (fixed_reader == null)
        affirmed_readers = listValidImageIOReaders(uri, null);
      // fixed reader is imageio reader
      else if (fixed_reader instanceof ImageReaderSpi)
        affirmed_readers = Arrays
            .asList(new ImageReaderSpi[] { (ImageReaderSpi) fixed_reader });
      // fixed reader is something else, hopefully jai codec ;)
      // simply define an empty imageio reader list here to skip to jai below
      else
        affirmed_readers = new ArrayList();

      // TODO: not sure looping makes sense here as image is
      // actually rendered much later
      for (Iterator<ImageReaderSpi> i = affirmed_readers.listIterator(); i
          .hasNext();) {

        src_input = createInput(uri);
        pbjImageRead.setParameter("Input", src_input);
        ImageReaderSpi readerSpi = ((ImageReaderSpi) i.next());
        // enforce a reader for TESTING
        // reader = (new
        // it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi()).createReaderInstance(input);
        src_reader = readerSpi.createReaderInstance(src_input);
        // src_reader.setInput(src_input);
        pbjImageRead.setParameter("reader", src_reader);

        // set info vars
        type = src_reader.getFormatName();
        loader = src_reader.getClass().getCanonicalName();

        System.out.println("G2RR: " + src_reader);
        try {
          src = JAI.create("ImageRead", pbjImageRead);

          // success OR dispose & try plain JAI below
          if (src != null)
            return;
          else
            dispose();
        } catch (Exception e) {
          // TODO: handle more gracefully, continue with next reader on error
          e.printStackTrace();
          dispose();
        }
      }
    } catch (IOException e) {
      throw new ReferencedImageException(e);
    }

    // The following is a fall through option, in case none of the above
    // readers apply, OK it is unlikely, but what the hell
    try {
      if (fixed_reader == null || fixed_reader instanceof ImageCodec)
        createJAIRenderedOP(uri);
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
   * @param image
   */
  protected void normalize(RenderedOp image) {
  }

  public RenderedOp getImage() throws ReferencedImageException {
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

  protected String getLoader() {
    return loader;
  }

  protected void createJAIRenderedOP(URI uri) throws IOException {
    // JAI loading streams is slower than fileload, hence we check if we really
    // try to open a compressed file first
    Object input = createInput(uri);
    if (input instanceof InputStream) {
      if (!(input instanceof SeekableStream))
        input = SeekableStream.wrapInputStream((InputStream) input, true);
      src = JAI.create("stream", input);
    } else {
      src = JAI.create("fileload", uri.getPath());
    }
  }

  public void dispose() {
    if (src instanceof RenderedImage) {
      ImageIOUtilities.disposeImage(src);
      src = null;
    }
    if (src_reader instanceof ImageReader) {
      src_reader.reset();
      src_reader.dispose();
      src_reader = null;
    }
    disposeInput(src_input);
  }

  public void log(String msg) {
    JUMPWorkbench.getInstance().getFrame().log(msg, this.getClass());
  }

  static protected boolean canDecode(ImageReaderSpi provider, URI uri)
      throws IOException {
    Object input = createInput(uri);

    boolean canDec = false;
    // some readers insist on a filestream for checking (e.g. tif, png)
    if (input instanceof File) {
      FileImageInputStream fis = new FileImageInputStream((File) input);
      canDec = canDec || provider.canDecodeInput(fis);
      disposeInput(fis);
    }
    // while some others like a file object (e.g. ecw, mrsid)
    // we simply try both and regard only a successful answer
    return provider.canDecodeInput(input) || canDec;
  }

  static protected boolean hasFileExtension(ImageReaderSpi provider, URI uri) {
    return hasFileExtension(provider,
        FileUtil.getExtension(CompressedFile.getTargetFileWithPath(uri)));
  }

  static protected boolean hasFileExtension(ImageReaderSpi provider, String ext) {
    return Arrays.asList(provider.getFileSuffixes()).contains(ext);
  }

  static protected List<ImageReaderSpi> listValidImageIOReaders(URI uri,
      Class filter) throws IOException {
    // fetch all readers
    final Iterator<? extends ImageReaderWriterSpi> iter = IIORegistry
        .getDefaultInstance().getServiceProviders(ImageReaderSpi.class, true);
    // iterate all readers and return only valid ones
    ImageReaderSpi provider;
    Vector<ImageReaderSpi> affirmed_readers = new Vector<ImageReaderSpi>();
    while (iter.hasNext()) {
      provider = (ImageReaderSpi) iter.next();

      boolean canDec = canDecode(provider, uri);
      boolean hasExt = hasFileExtension(provider, uri);
      String canRead = canDec ? "jupp" : "noe";
      if (canDec || hasExt)
        System.out.println(provider + "(" + canRead + "/" + hasExt + ") = "
            + provider.getDescription(Locale.getDefault()) + " / "
            + Arrays.toString(provider.getFileSuffixes()));

      // either decoding or extension suffice for our purposes
      if (!canDec && !hasExt)
        continue;

      if (filter != null && !(filter.isInstance(provider)))
        continue;

      // prefer imageio ext reader for tif
      if (Arrays.asList(provider.getFileSuffixes()).contains("tif")
          && provider.getPluginClassName().startsWith("it.geosolutions."))
        affirmed_readers.add(0, provider);
      else
        affirmed_readers.add(provider);
    }
    return affirmed_readers;
  }

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

  static public List<Object> listValidReaders(URI uri) throws IOException {
    List l = listValidImageIOReaders(uri, null);
    l.addAll(listValidJAICodecs(uri));
    return l;
  }

  static protected Object createInput(URI uri) throws IOException {
    // InputStream is = CompressedFile.openFile(uri);
    // ImageInputStream iis = new MemoryCacheImageInputStream(is);
    Object input;
    if (CompressedFile.isArchive(uri) || CompressedFile.isCompressed(uri)) {
      InputStream src_is;
      src_is = CompressedFile.openFile(uri);
      src_is = new BufferedInputStream(src_is);
      input = src_is;
    } else {
      // create a File object, native loaders like ecw, mrsid seem to insist on
      // it
      // error was:
      // "Unable to create a valid ImageInputStream for the provided input:"
      // took me two days to debug this.. pfffhhhh
      // if you find this workaround because of the full error string above,
      // that was intentional
      // please send your praises to edgar AT soldin DOT de, would love to hear
      // from you
      input = new File(uri);
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

}