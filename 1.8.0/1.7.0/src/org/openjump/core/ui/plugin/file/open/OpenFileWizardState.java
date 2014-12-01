/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.plugin.file.open;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.Option;
import org.openjump.core.ui.plugin.file.InstallDummyReaderPlugIn.DummyDataSource;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;

public class OpenFileWizardState {
  public static final String KEY = OpenFileWizardState.class.getName();

  private String currentPanel;

  private FileLayerLoader fileLayerLoader;
  
  public static FileLayerLoader IGNORELOADER = new DataSourceFileLayerLoader(
    JUMPWorkbench.getInstance().getContext(),
    DummyDataSource.class, 
    I18N.get(OpenFileWizardState.class.getName()+".dummy-loader-description"),
    Arrays.asList(new String[]{"*"}));

  private Map<String, Set<FileLayerLoader>> extensionLoaderMap = new HashMap<String, Set<FileLayerLoader>>();

  private Map<URI, FileLayerLoader> fileLoaderMap = new HashMap<URI, FileLayerLoader>();

  private Map<FileLayerLoader, Set<URI>> fileLoaderFiles = new HashMap<FileLayerLoader, Set<URI>>();

  private Map<String, Set<URI>> multiLoaderFiles = new TreeMap<String, Set<URI>>();

  private Map<URI, Map<String, Object>> fileOptions = new HashMap<URI, Map<String, Object>>();

  private ErrorHandler errorHandler;

  public OpenFileWizardState(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public FileLayerLoader getFileLoader() {
    return fileLayerLoader;
  }

  public void setupFileLoaders(File[] files, FileLayerLoader fileLayerLoader) {
    Set<File> fileSet = new TreeSet<File>(Arrays.asList(files));
    multiLoaderFiles.clear();
    // explicit loader chosen
    if (fileLayerLoader != null) {
      fileLoaderMap.clear();
      for (File file : fileSet) {
        setFileLoader(file.toURI(), fileLayerLoader);
      }
    } 
    else {
      // Remove old entries in fileloadermap
      fileLoaderMap.clear();
//      for (Iterator<Entry<URI, FileLayerLoader>> iterator = fileLoaderMap.entrySet()
//        .iterator(); iterator.hasNext();) {
//        Entry<URI, FileLayerLoader> entry = iterator.next();
//        URI fileUri = entry.getKey();
//        File file;

//        if (fileUri.getScheme().equals("zip")) {
//          file = UriUtil.getZipFile(fileUri);
//        } else {
//          file = new File(fileUri);
//        }
//        
//        if (!fileSet.contains(file)) {
//          FileLayerLoader loader = entry.getValue();
//          fileLoaderFiles.get(loader);
//          Set<URI> loaderFiles = fileLoaderFiles.get(loader);
//          if (loaderFiles != null) {
//            loaderFiles.remove(fileUri);
//          }
//          iterator.remove();
//        }
//      }

      // manually add compressed files here
      for (File file : files) {
        // zip files
        if (CompressedFile.isZip(file.getName())) {
          try {
            ZipFile zipFile = new ZipFile(file);
            URI fileUri = file.toURI();
            Enumeration entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
              ZipArchiveEntry entry = (ZipArchiveEntry)entries.nextElement();
              if (!entry.isDirectory()) {
                URI entryUri = UriUtil.createZipUri(file, entry.getName());
                String entryExt = UriUtil.getFileExtension(entryUri);
                //System.out.println(entryUri+"<->"+entryExt);
                addFile(entryExt, entryUri);
              }
            }
          } catch (Exception e) {
            errorHandler.handleThrowable(e);
          }
        }
        // tar[.gz,.bz...] (un)compressed archive files
        else if (CompressedFile.isTar(file.getName())) {
          try {
            InputStream is = CompressedFile.openFile(file.getAbsolutePath(), null);
            TarArchiveEntry entry;
            TarArchiveInputStream tis = new TarArchiveInputStream(is);
            while ((entry = tis.getNextTarEntry()) != null) {
              if (!entry.isDirectory()) {
                URI entryUri = UriUtil.createZipUri(file, entry.getName());

                String entryExt = UriUtil.getFileExtension(entryUri);
                addFile(entryExt, entryUri);
              }
            }
            tis.close();
          } catch (Exception e) {
            errorHandler.handleThrowable(e);
          }
        }
        // 7zip compressed files
        else if (CompressedFile.isSevenZ(file.getName())) {
          try {
            //System.out.println(file.getName());
            SevenZFile sevenZFile = new SevenZFile(file);
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
              if (!entry.isDirectory()) {
                URI entryUri = UriUtil.createZipUri(file, entry.getName());

                String entryExt = UriUtil.getFileExtension(entryUri);
                addFile(entryExt, entryUri);
              }
            }
            sevenZFile.close();
          } catch (IOException e) {
            errorHandler.handleThrowable(e);
          }
        }
        // compressed files
        else if ( CompressedFile.hasCompressedFileExtension(file.getName()) ) {
          String[] parts = file.getName().split("\\.");
          if (parts.length>2)
            addFile(parts[parts.length-2], file.toURI());
        }
        // anything else is a plain data file
        else {
          URI fileUri = file.toURI();
          addFile(FileUtil.getExtension(file), fileUri);
        }
      }
    }
  }

  private void addFile(String extension, URI fileUrl) {
    Set<FileLayerLoader> loaders = getFileLoaders(extension);
    
    // try wildcard loaders, only if none of the other extension matched
    if (loaders.size() < 1) {
      loaders = getFileLoaders("*");
    }
    
    // this file only has one matching loader
    if (loaders.size() == 1) {
      FileLayerLoader loader = loaders.iterator().next();
      setFileLoader(fileUrl, loader);
    } 
    // oh no! :) we got multiple loaders to offer
    // save in a special map and fetch loaders later (again)
    else if (!loaders.isEmpty()) {
      Set<URI> extensionFiles = multiLoaderFiles.get(extension);
      if (extensionFiles == null) {
        extensionFiles = new TreeSet<URI>();
        multiLoaderFiles.put(extension, extensionFiles);
      }
      extensionFiles.add(fileUrl);
    }
  }

  public void setFileLoader(String extension, FileLayerLoader fileLayerLoader) {
    Set<URI> files = multiLoaderFiles.get(extension);
    for (URI file : files) {
      setFileLoader(file, fileLayerLoader);
    }
  }

  public void setFileLoader(URI file, FileLayerLoader fileLayerLoader) {
    FileLayerLoader oldFileLoader = fileLoaderMap.get(file);
    if (oldFileLoader != null) {
      Set<URI> files = fileLoaderFiles.get(oldFileLoader);
      if (files != null) {
        files.remove(file);
      }
    }

    fileLoaderMap.put(file, fileLayerLoader);
    Set<URI> files = fileLoaderFiles.get(fileLayerLoader);
    if (files == null) {
      files = new HashSet<URI>();
      fileLoaderFiles.put(fileLayerLoader, files);
    }
    files.add(file);
  }

  public void addFileLoader(final FileLayerLoader fileLayerLoader) {
    // loader for each ext in the extensionLoaderMap
    for (String extension : fileLayerLoader.getFileExtensions()) {
//      if (extension.equals("*"))
//      System.out.println("OFWS: add "+extension+"/"+fileLayerLoader);
      Set<FileLayerLoader> state_extensionLoaders = getFileLoaders(extension);
      state_extensionLoaders.add(fileLayerLoader);
    }
  }

  public Set<FileLayerLoader> getFileLoaders(String extension) {
    Set<FileLayerLoader> loaders = extensionLoaderMap.get(extension);
    //System.out.println(extensionLoaderMap);
    if (loaders == null) {
      loaders = new HashSet<FileLayerLoader>();
      extensionLoaderMap.put(extension, loaders);
    }
    
    return loaders;
  }

  public String getCurrentPanel() {
    return currentPanel;
  }

  public void setCurrentPanel(final String currentPanel) {
    this.currentPanel = currentPanel;
  }

  public String getNextPanel(final String currentPanel) {
    if (currentPanel.equals(SelectFilesPanel.KEY)) {
      if (fileLayerLoader == null && !multiLoaderFiles.isEmpty()) {
        return SelectFileLoaderPanel.class.getName();
      } 
      else {
        return getNextPanel(SelectFileLoaderPanel.class.getName());
      }
    } else if (currentPanel.equals(SelectFileLoaderPanel.KEY)) {
      for (Entry<FileLayerLoader, Set<URI>> entries : fileLoaderFiles.entrySet()) {
        FileLayerLoader fileLayerLoader = entries.getKey();
        if (!entries.getValue().isEmpty()) {
          if (!fileLayerLoader.getOptionMetadata().isEmpty()) {
            return SelectFileOptionsPanel.KEY;
          }
        }
      }
      return getNextPanel(SelectFileOptionsPanel.KEY);
    } else {
      return null;
    }
  }

  public Map<String, Set<FileLayerLoader>> getExtensionLoaderMap() {
    return extensionLoaderMap;
  }

  public void setExtensionLoaderMap(
    final Map<String, Set<FileLayerLoader>> extensionLoaderMap) {
    this.extensionLoaderMap = extensionLoaderMap;

  }

  public Map<String, Set<URI>> getMultiLoaderFiles() {
    return multiLoaderFiles;
  }

  public Map<URI, FileLayerLoader> getFileLoaders() {
    return fileLoaderMap;
  }

  public Map<FileLayerLoader, Set<URI>> getFileLoaderFiles() {
    return fileLoaderFiles;
  }

  public FileLayerLoader getFileLoader(URI file) {
    return fileLoaderMap.get(file);
  }

  public void setOption(final FileLayerLoader loader, final String label,
    final Object value) {
    Set<URI> files = fileLoaderFiles.get(loader);
    for (URI file : files) {
      setOption(file, label, value);
    }

  }

  public void setOption(URI file, String label, Object value) {
    Map<String, Object> options = getOptions(file);
    options.put(label, value);
  }

  public Map<String, Object> getOptions(URI file) {
    Map<String, Object> options = fileOptions.get(file);
    if (options == null) {
      options = new HashMap<String, Object>();
      fileOptions.put(file, options);
    }
    return options;
  }

  public boolean hasSelectedFiles() {
    return !fileLoaderFiles.isEmpty() || !multiLoaderFiles.isEmpty();
  }

  public boolean hasRequiredOptions() {
    for (Entry<FileLayerLoader, Set<URI>> entries : fileLoaderFiles.entrySet()) {
      FileLayerLoader fileLayerLoader = entries.getKey();
      Set<URI> files = entries.getValue();
      if (!files.isEmpty()) {
        List<Option> optionMetadata = fileLayerLoader.getOptionMetadata();
        for (URI file : files) {
          Map<String, Object> options = getOptions(file);
          for (Option option : optionMetadata) {
            if (option.isRequired()) {
              if (options.get(option.getName()) == null) {
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  public String getFileName(URI uri) {
    String path = uri.getPath();
    int slashIndex = path.lastIndexOf('/');
    if (slashIndex > -1) {
      return path.substring(slashIndex + 1);
    } else {
      return path;
    }
  }
}
