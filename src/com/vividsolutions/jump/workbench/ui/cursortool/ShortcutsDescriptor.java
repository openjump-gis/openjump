package com.vividsolutions.jump.workbench.ui.cursortool;

import java.util.Map;

public interface ShortcutsDescriptor {
  public String getName();
  public Map<QuasimodeTool.ModifierKeySpec, String> describeShortcuts();
}
