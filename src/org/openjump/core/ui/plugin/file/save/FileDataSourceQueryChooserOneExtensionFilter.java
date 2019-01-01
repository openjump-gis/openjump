package org.openjump.core.ui.plugin.file.save;

import org.openjump.core.ui.io.file.FileNameExtensionFilter;

import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;

public class FileDataSourceQueryChooserOneExtensionFilter extends
    FileNameExtensionFilter {

  private FileDataSourceQueryChooser chooser;

  public FileDataSourceQueryChooserOneExtensionFilter(
      FileDataSourceQueryChooser chooser) {
    super(chooser.getDescription(),
        new String[] { chooser.getExtensions() != null
            && chooser.getExtensions().length > 0 ? chooser.getExtensions()[0]
            : "" });
    this.chooser = chooser;
  }

  public FileDataSourceQueryChooser getFileDataSourceQueryChooser() {
    return chooser;
  }
}
