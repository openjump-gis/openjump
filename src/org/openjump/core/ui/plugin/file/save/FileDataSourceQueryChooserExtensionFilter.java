package org.openjump.core.ui.plugin.file.save;

import org.openjump.core.ui.io.file.FileNameExtensionFilter;

import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;

public class FileDataSourceQueryChooserExtensionFilter extends FileNameExtensionFilter{

  private FileDataSourceQueryChooser chooser;

  public FileDataSourceQueryChooserExtensionFilter(FileDataSourceQueryChooser chooser) {
    super(chooser.getDescription(), chooser.getExtensions());
    this.chooser = chooser;
  }

  public FileDataSourceQueryChooser getFileDataSourceQueryChooser() {
    return chooser;
  }
}
