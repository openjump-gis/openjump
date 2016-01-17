/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */

package com.vividsolutions.jump.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.task.TaskMonitor;

/**
 * File-related utility functions.
 */
public class FileUtil {

  public static final String I18NPREFIX = FileUtil.class.getName() + ".";

  /**
   * Gets the content of filename as a list of lines.
   *
   * @param filename
   *          name of the input file
   * @return a list of strings representing the file's lines
   * @throws IOException
   */
  public static List getContents(String filename) throws IOException {
    return getContents(new FileInputStream(filename));
  }

  /**
   * Gets the content of filename as a list of lines.
   *
   * @param filename
   *          name of the input file
   * @param encoding
   *          charset to use to decode filename
   * @return a list of strings representing the file's lines
   * @throws IOException
   */
  public static List getContents(String filename, String encoding)
      throws IOException {
    return getContents(new FileInputStream(filename), encoding);
  }

  /**
   * Gets the content a compressed file passed as an URI.
   *
   * @param uri
   *          uri of the input resource
   * @return a list of strings representing the compressed file's lines
   * @throws IOException
   */
  public static List getContents(URI uri) throws IOException {
    return getContents(CompressedFile.openFile(uri));
  }

  /**
   * Gets the content of an inputSteam as a list of lines.
   * 
   * @param inputStream
   * @return a list of lines
   * @throws IOException
   */
  public static List getContents(InputStream inputStream) throws IOException {
    return getContents(inputStream, Charset.defaultCharset().name());
  }

  /**
   * Gets the content of an inputSteam as a list of lines. inputStream is
   * decoded with the specified charset.
   * 
   * @param inputStream
   * @return a list of lines
   * @throws IOException
   */
  public static List getContents(InputStream inputStream, String encoding)
      throws IOException {
    ArrayList contents = new ArrayList();
    InputStreamReader inputStreamReader = new InputStreamReader(inputStream,
        encoding);
    try {
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      try {
        String line = bufferedReader.readLine();
        while (line != null) {
          contents.add(line);
          line = bufferedReader.readLine();
        }
      } finally {
        bufferedReader.close();
      }
    } finally {
      inputStreamReader.close();
    }
    return contents;
  }

  /**
   * Saves the String to a file with the given filename.
   * 
   * @param filename
   *          the pathname of the file to create (or overwrite)
   * @param contents
   *          the data to save
   * @throws IOException
   *           if an I/O error occurs.
   */
  public static void setContents(String filename, String contents)
      throws IOException {
    setContents(filename, contents, Charset.defaultCharset().name());
  }

  /**
   * Saves the List of Strings to a file with the given filename.
   * 
   * @param filename
   *          the pathname of the file to create (or overwrite)
   * @param lines
   *          the Strings to save as lines in the file
   * @throws IOException
   *           if an I/O error occurs.
   */
  public static void setContents(String filename, List lines)
      throws IOException {
    setContents(filename, lines, Charset.defaultCharset().name());
  }

  /**
   * Saves lines into a file named filename, using encoding charset.
   *
   * @param filename
   *          the pathname of the file to create (or overwrite)
   * @param contents
   *          the data to save
   * @throws IOException
   *           if an I/O error occurs.
   */
  public static void setContents(String filename, String contents,
      String encoding) throws IOException {
    List<String> lines = new ArrayList<String>();
    lines.add(contents);
    setContents(filename, lines, encoding);
  }

  /**
   * Saves lines into a file named filename, using encoding charset.
   *
   * @param filename
   *          the pathname of the file to create (or overwrite)
   * @param lines
   *          the data to save
   * @throws IOException
   *           if an I/O error occurs.
   */
  public static void setContents(String filename, List<String> lines,
      String encoding) throws IOException {
    OutputStreamWriter osw = null;
    try {
      osw = new OutputStreamWriter(new FileOutputStream(filename), encoding);
      String lineSep = System.getProperty("line.separator");
      for (Iterator<String> it = lines.iterator(); it.hasNext();) {
        osw.write(it.next());
        if (it.hasNext()) {
          osw.write(lineSep);
        }
      }
    } finally {
      if (osw != null) {
        try {
          osw.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public static void zip(Collection files, File zipFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(zipFile);
    try {
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      try {
        ZipOutputStream zos = new ZipOutputStream(bos);
        try {
          for (Iterator i = files.iterator(); i.hasNext();) {
            File file = (File) i.next();
            zos.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fis = new FileInputStream(file);
            try {
              BufferedInputStream bis = new BufferedInputStream(fis);
              try {
                while (true) {
                  int j = bis.read();
                  if (j == -1) {
                    break;
                  }
                  zos.write(j);
                }
              } finally {
                bis.close();
              }
            } finally {
              fis.close();
              zos.closeEntry();
            }
          }
        } finally {
          zos.close();
        }
      } finally {
        bos.close();
      }
    } finally {
      fos.close();
    }
  }

  public static String getExtension(String s) {
    String ext = "";
    int i = s.lastIndexOf('.');

    if ((i > 0) && (i < (s.length() - 1))) {
      ext = s.substring(i + 1).toLowerCase();
    }

    return ext;
  }

  public static String getExtension(File f) {
    String s = f.getName();
    return getExtension(s);
  }

  public static File addExtensionIfNone(File file, String extension) {
    if (getExtension(file).length() > 0) {
      return file;
    }
    String path = file.getAbsolutePath();
    if (!path.endsWith(".")) {
      path += ".";
    }
    path += extension;
    return new File(path);
  }

  public static File removeExtensionIfAny(File file) {
    int i = file.getName().lastIndexOf('.');
    if (i > 0) {
      String path = file.getAbsolutePath();
      path = path.substring(0, path.length() - file.getName().length() + i);
      return new File(path);
    } else
      return file;
  }

  public static String getFileNameFromLayerName(String layerName) {
    return layerName.replaceAll("[/\\\\\\?%\\*:\\|\"<> ]+", "_")
        .replaceAll("^_", "").replaceAll("_$", "");
  }

  public static void close(Closeable is) {
    try {
      if (is instanceof Closeable)
        is.close();
    } catch (IOException e) {
    }
  }

  public static final String PREFIX = "openjump";
  public static final String SUFFIX = ".tmp";

  /**
   * utility method to copy an inputstream to a temp file for further processing
   * NOTE: prefix, suffix, monitor are optional and may be {@link <code>null</code>}
   * 
   * @param in
   * @param prefix
   *          - default "openjump"
   * @param suffix
   *          - default ".tmp"
   * @param monitor
   * @return
   * @throws IOException
   */
  public static File copyInputStreamToTempFile(InputStream in, String prefix,
      String suffix, TaskMonitor monitor) throws IOException {
    final File tempFile = File.createTempFile(prefix != null ? prefix : PREFIX,
        suffix != null ? suffix : SUFFIX);
    tempFile.deleteOnExit();
    // Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(
        tempFile));

    long counter = 0;
    int r = 0;
    byte[] b = new byte[1024];
    long startCounter = counter;
    long stopCounter = counter;
    long startTime = System.currentTimeMillis();
    long stopTime = 0;
    int i = 0;
    float speed = 0;
    while ((r = in.read(b)) != -1) {
      counter += r;

      i += 1;
      if (i % 1023 == 0) {
        stopTime = System.currentTimeMillis();
        stopCounter = counter;
        speed = (stopCounter - startCounter) / 1024f / 1024
            / ((stopTime - startTime) / 1000f);
      }

      // only report if monitor is set
      if (monitor != null) {
        monitor.report(I18N.getMessage(
            I18NPREFIX + "receiving-{0}-MiB-(at-{1}-{2})",
            String.format("%.2f", counter / 1024f / 1024),
            String.format("%.2f", speed < 1 ? speed * 1024 : speed),
            speed < 1 ? I18N.getMessage(I18NPREFIX + "KiB/s") : I18N
                .getMessage(I18NPREFIX + "MiB/s")));
      }
      fout.write(b, 0, r);
      if (i >= 1023) {
        startTime = System.currentTimeMillis();
        startCounter = counter + 1;
        i = 0;
      }
    }
    in.close();
    fout.close();
    return tempFile;
  }
}
