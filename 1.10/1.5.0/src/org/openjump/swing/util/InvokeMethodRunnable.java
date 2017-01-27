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
package org.openjump.swing.util;

import java.lang.reflect.Method;

public class InvokeMethodRunnable implements Runnable {

  private Object object;

  private Method method;

  private Object[] parameters;

  public InvokeMethodRunnable(final Object object, final String methodName) {
    this(object, methodName, new Object[0]);
  }

  public InvokeMethodRunnable(final Object object, final String methodName,
    final Object[] parameters) {

    this.object = object;
    this.parameters = parameters;
    Class clazz = object.getClass();
    try {
      Class[] types = new Class[parameters.length];
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals(methodName)) {
          if (method.getParameterTypes().length == parameters.length) {
            this.method = method;
          }
        }
      }
      if (method == null) {
      System.err.println(this.method + methodName);
      }
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public void run() {
    try {
      method.invoke(object, parameters);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
