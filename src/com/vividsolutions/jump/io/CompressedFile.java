/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive
 * GUI for visualizing and manipulating spatial features with geometry
 * and attributes.
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

package com.vividsolutions.jump.io;

import static com.vividsolutions.jump.util.FileUtil.close;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.Logger;

/**
 * Utility class for dealing with compressed files.
 * @see #isCompressed(URI)
 * 
 * @author dblasby
 * @author ede
 */
// TODO :I18N
public class CompressedFile {

  /** Creates a new CompressedFile */
  public CompressedFile() {
  }

  /**
   * Searches through the .zip file looking for a file with the given extension.
   * Returns null if it doesn't find one.
   * 
   * @deprecated only used by very old data readers which only deliver the first file in zip file [ede 05.2012]
   */
  public static String getInternalZipFnameByExtension(String extension,
      String compressedFile) throws Exception {
    // zip file
    String inside_zip_extension;
    InputStream IS_low = new FileInputStream(compressedFile);
    ZipInputStream fr_high = new ZipInputStream(IS_low);

    // need to find the correct file within the .zip file
    ZipEntry entry;

    entry = fr_high.getNextEntry();

    while (entry != null) {
      inside_zip_extension = entry.getName().substring(
          entry.getName().length() - extension.length());

      if (inside_zip_extension.compareToIgnoreCase(extension) == 0) {
        return (entry.getName());
      }

      entry = fr_high.getNextEntry();
    }

    return null;
  }

  public static List<URI> listEntries(File file) throws Exception {
    List<URI> entries = new ArrayList<URI>();

    // tar[.gz,.bz...] (un)compressed archive files
    if (CompressedFile.isTar(file.getName())) {
      InputStream is = CompressedFile.openFile(file.getAbsolutePath(), null);
      TarArchiveEntry entry;
      TarArchiveInputStream tis = new TarArchiveInputStream(is);
      while ((entry = tis.getNextTarEntry()) != null) {
        if (!entry.isDirectory()) {
          URI entryUri = UriUtil.createZipUri(file, entry.getName());
          entries.add(entryUri);
        }
      }
      close(tis);
    }
    
    // 7zip compressed files
    else if (CompressedFile.isSevenZ(file.getName())) {
      // System.out.println(file.getName());
      SevenZFile sevenZFile = new SevenZFile(file);
      SevenZArchiveEntry entry;
      while ((entry = sevenZFile.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          URI entryUri = UriUtil.createZipUri(file, entry.getName());
          entries.add(entryUri);
        }
      }
      close(sevenZFile);
    }
    
    // all other archive files
    else {

      InputStream is = new BufferedInputStream(new FileInputStream(file));
      
      // try if we are a compressed tar
      CompressorInputStream cis = null;
      try {
        cis = new CompressorStreamFactory().createCompressorInputStream(is);
      } catch (CompressorException e) {
      }
      
      ArchiveInputStream in = new ArchiveStreamFactory()
          .createArchiveInputStream(cis != null ? cis : is);
      ArchiveEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        if (!entry.isDirectory() && in.canReadEntryData(entry)) {
          URI entryUri = UriUtil.createZipUri(file, entry.getName());
          entries.add(entryUri);
        }
      }
      close(in);
      close(cis);
      close(is);
    }

    return entries;
  }
  
  
  public static InputStream openFile(String uri_string) throws URISyntaxException, IOException {
    return openFile(new URI(uri_string));
  }
  
  public static InputStream openFile(URI uri) throws IOException{
    return openFile( UriUtil.getZipFilePath(uri), UriUtil.getZipEntryName(uri) );
  }

  /**
   * Utility file open function - handles compressed and un-compressed files.
   * 
   * @param filePath
   *          name of the file to search for.
   * @param compressedEntry
   *          name of the compressed file.
   * 
   *          <p>
   *          If compressedEntry = null, opens a FileInputStream on filePath
   *          </p>
   * 
   *          <p>
   *          If filePath ends in ".zip" - opens the compressed Zip and
   *          looks for the file called compressedEntry
   *          </p>
   * 
   *          <p>
   *          If filePath ends in ".gz" - opens the compressed .gz file.
   *          </p>
   */
  public static InputStream openFile(String filePath, String compressedEntry)
      throws IOException {

    String extractMsg = compressedEntry != null ? " extract '" + compressedEntry
        + "'" : "";
    Logger.debug("Open '" + filePath + "'" + extractMsg);

    // check file accessibility beforehand
    File file = new File(filePath);
    if (!file.exists())
      throw new FileNotFoundException("Couldn't find file '" + filePath + "'.");
    if (!file.canRead())
      throw new IOException("Couldn't access file '" + filePath + "'.");

    // if no compressedEntry was given we are supposed to open a plain file
    // return fileinputstream or compressorinputstream,
    if (compressedEntry == null) {
      InputStream bis = new BufferedInputStream(new FileInputStream(filePath));

      // try if we are a plain compressed file
      CompressorInputStream cis = null;
      BufferedInputStream bcis = null;
      try {
        cis = new CompressorStreamFactory().createCompressorInputStream(bis);
        bcis = new BufferedInputStream(cis);
        return bcis;
      } catch (CompressorException e) {
        // ok, we are not ..lets return the plain fileinputstream
        close(bcis);
        close(cis);
        return bis;
      }
    }

    // load into memory workaround until commons compress 7zip learns streaming again 
    else if (compressedEntry != null && isSevenZ(filePath)) {

      SevenZFile sevenZFile = new SevenZFile(new File(filePath));
      try {
        SevenZArchiveEntry entry;
        while ((entry = sevenZFile.getNextEntry()) != null) {
          if (entry.getName().equals(compressedEntry)){
            // works only for files with INT MAX byte size ca. 2GB
            byte[] content = new byte[(int) entry.getSize()];
            sevenZFile.read(content);
            return new ByteArrayInputStream(content);
          }
        } 
      } finally {
        close(sevenZFile);
      }

      throw createArchiveFNFE(filePath, compressedEntry);
    }
    
    // generic method for archives w/o special needs (e.g. zip)
    else {
      // open the file as such, even it is compressed beforehand eg. tar.gz & such
      InputStream bis = openFile(filePath, null);

      ArchiveInputStream in = null;
      try {
        in = new ArchiveStreamFactory()
            .createArchiveInputStream(/*bcis != null ? bcis :*/ bis);
      } catch (ArchiveException e) {
          //close(bcis);
          close(bis);
          throw new IOException("Couldn't determine compressed file type for file '"
            + filePath + "' supposedly containing '"+compressedEntry+"'.", e);
      }

      ArchiveEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        if (entry.getName().equals(compressedEntry))
          return in;
      }

      // make sure to close the inputstream on failure to find entry
      close(in);
      throw createArchiveFNFE(filePath, compressedEntry);
    }
    
  }

  public static boolean isCompressed(URI uri) {
    String filepath = UriUtil.getFilePath(uri);
    return hasCompressedFileExtension(filepath);
  }

  public static boolean isArchive(URI uri) {
    String filepath = UriUtil.getFilePath(uri);
    return hasArchiveFileExtension(filepath);
  }

  public static boolean isCompressed(String filePath) {
    return hasCompressedFileExtension(filePath);
  }

  public static boolean isArchive(String filePath) {
    return hasArchiveFileExtension(filePath);
  }

  public static boolean isZip(String filePath) {
    return filePath.matches(".*\\.(?i:zip)");
  }

  public static boolean isTar(String filePath) {
    return filePath.matches("(?i).*\\.(tar|(tar\\.|t)(gz|bz2?|xz))");
  }

  public static boolean isGZip(String filePath) {
    return filePath.matches("(?i).*(?!\\.tar)\\.gz");
  }

  public static boolean isBZip(String filePath) {
    return filePath.matches("(?i).*(?!\\.tar)\\.bz2?");
  }

  public static boolean isXZ(String filePath) {
    return filePath.matches("(?i).*(?!\\.tar)\\.(xz)");
  }

  public static boolean isSevenZ(String filePath) {
    return filePath.matches("(?i).*(?!\\.tar)\\.(7z)");
  }

  // archives contain multiple items
  public static String[] getArchiveExtensions() {
    return  new String[]{ "zip", "tgz", "tar.gz", "tar.bz", "tar.bz2", "tbz", "tbz2", "txz", "tar.xz", "7z" };
  }
  // file is one plainly compressed file
  public static String[] getFileExtensions() {
    return  new String[]{ "gz", "bz", "bz2", "xz" };
  }
  
  public static boolean hasCompressedFileExtension(String filename) {
    return Arrays.asList(CompressedFile.getFileExtensions()).contains(
        FileUtil.getExtension(new File(filename)).toLowerCase());
  }
  
  public static boolean hasArchiveFileExtension(String filename) {
    for (String ext : getArchiveExtensions()) {
      if (filename.toLowerCase().endsWith(ext))
        return true;
    }
    return false;
  }
  
  public static String getTargetFileWithPath( URI uri ){
    String filepath = UriUtil.getFilePath(uri);
    String entry = UriUtil.getZipEntryName(uri);
    if (hasArchiveFileExtension(filepath)) {
      return entry;
    }
    return filepath;
  }

  public static URI replaceTargetFileWithPath( URI uri, String location){
    String filepath = UriUtil.getZipFilePath(uri);
    if (hasArchiveFileExtension(filepath)) {
      return UriUtil.createZipUri(filepath, location);
    }
    return UriUtil.createFileUri(location);
  }
  
  public static URI replaceTargetFileName( URI uri, String filename ){
    String oldpath = UriUtil.getPath(getTargetFileWithPath(uri));
    return replaceTargetFileWithPath( uri, oldpath + filename );
  }
  
  public static String createLayerName( URI uri ){
    String filename = UriUtil.getFileName(uri); 
    String layerName = UriUtil.getFileNameWithoutExtension(uri);
    // layername for archive members is "filename.ext (archive.ext)"
    if (CompressedFile.hasArchiveFileExtension(filename))
      layerName = UriUtil.getZipEntryName(uri) + " ("
          + filename + ")";
    // remove format extension for compressed files, but hint compressed file in braces
    else if (CompressedFile.hasCompressedFileExtension(filename)) {
      layerName = layerName.substring(0, layerName.lastIndexOf('.')) + " ("
          + filename + ")";
    }
    return layerName;
  }

  private static FileNotFoundException createArchiveFNFE( String archive, String entry ){
    return new FileNotFoundException("Couldn't find entry '" + entry + "' in compressed file: "
        + archive);
  }
}
