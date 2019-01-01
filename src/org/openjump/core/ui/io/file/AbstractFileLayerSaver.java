package org.openjump.core.ui.io.file;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.task.TaskMonitor;

abstract public class AbstractFileLayerSaver implements FileLayerSaver {

  private String description;
  private List<String> extensions;

  public AbstractFileLayerSaver(final String description,
      final List<String> extensions) {
      this.description = description;
      this.extensions = extensions;
  }
  
  @Override
  public Collection<String> getFileExtensions() {
    return Collections.unmodifiableList(extensions);
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  abstract public boolean write(TaskMonitor monitor, URI uri, Map<String, Object> options)
      throws Exception;

  @Override
  public List<Option> getOptionMetadata() {
    return new ArrayList();
  }

}
