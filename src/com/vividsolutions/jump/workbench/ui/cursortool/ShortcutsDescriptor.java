package com.vividsolutions.jump.workbench.ui.cursortool;

import java.util.Map;

public interface ShortcutsDescriptor {
  String getName();
  Map<QuasimodeTool.ModifierKeySpec, String> describeShortcuts();
}
