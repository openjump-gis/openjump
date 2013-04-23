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
package org.openjump.core.ui.io.file;

public class Option {
  private String name;

  private String type;

  private boolean required;

  private Object defaultValue = null;

  public Option(final String name, final String type, final boolean required) {
    super();
    this.name = name;
    this.type = type;
    this.required = required;
  }
  
  public Option(final String name, final String type, final Object defaultValue, final boolean required) {
    super();
    this.name = name;
    this.type = type;
    this.defaultValue = defaultValue;
    this.required = required;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public Object getDefault() {
    return defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  /**
   * Overwrite equals(), because the Object.equals works wrong for an Option
   * instance. So we must compare each single value. This is especially
   * important if we have some Options stored in a List and do a List.remove().
   *
   * @param obj
   * @return true if both objects are equal.
   */
  public boolean equals(Object obj) {
	  boolean equal = false;
	  if (obj instanceof Option) {
		  Option option = (Option) obj;
		  equal = this.name.equals(option.getName()) && this.type.equals(option.getType()) && this.required == option.isRequired() && this.defaultValue == option.defaultValue;
	  }
	 return equal;
  }
}
