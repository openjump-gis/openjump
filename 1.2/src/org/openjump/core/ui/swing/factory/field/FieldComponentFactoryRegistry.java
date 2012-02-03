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
package org.openjump.core.ui.swing.factory.field;

import java.util.HashMap;
import java.util.Map;

import org.openjump.swing.factory.field.FieldComponentFactory;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;

public class FieldComponentFactoryRegistry {
  public static final String KEY = FieldComponentFactoryRegistry.class.getName();

  public static void setFactory(final WorkbenchContext context,
    final String type, final FieldComponentFactory factory) {
    Blackboard blackboard = context.getBlackboard();
    Map<String, FieldComponentFactory> fields = getFields(blackboard);
    fields.put(type, factory);
  }

  public static FieldComponentFactory getFactory(
    final WorkbenchContext context, final String type) {
    Blackboard blackboard = context.getBlackboard();
    Map<String, FieldComponentFactory> fields = getFields(blackboard);
    return fields.get(type);
  }

  private static Map<String, FieldComponentFactory> getFields(
    Blackboard blackboard) {
    Map<String, FieldComponentFactory> fields = (Map<String, FieldComponentFactory>)blackboard.get(KEY);
    if (fields == null) {
      fields = new HashMap<String, FieldComponentFactory>();
      blackboard.put(KEY, fields);
    }
    return fields;
  }
}
