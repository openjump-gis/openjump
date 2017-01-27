package org.openjump.core.ui.plugin.file;

  public class PathPrefixes {
    private String oldPrefix = "";

    private String newPrefix = "";

    public PathPrefixes(String oldPrefix, String newPrefix) {
      this.oldPrefix = oldPrefix;
      this.newPrefix = newPrefix;
    }

    public String getOldPrefix() {
      return oldPrefix;
    }

    public String getNewPrefix() {
      return newPrefix;
    }
  }
