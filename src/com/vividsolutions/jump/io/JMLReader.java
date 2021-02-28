
/*
 * JMLReader.java
 *
 * Created on July 12, 2002, 3:00 PM
 */
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.io;


/**
 * JMLReader is a {@link JUMPReader} specialized to read JML.
 *
 * <p>
 * This is a simple class that passes the work off to the {@link GMLReader } class which
 * already has support for auto-generating a JML input template 
 * (see  {@link GMLInputTemplate}).
 * </p>
 *
 * <p>
 *   DataProperties for the JMLReader load(DataProperties) interface:<br><br>
 * </p>
 *
 *  <table style="border-collapse: collapse;" summary="">
 *  <tr>
 *    <th style="border: 1px solid #999; padding: 4px;">Parameter</th>
 *    <th style="border: 1px solid #999; padding: 4px;">Meaning</th>
 *  </tr>
 *  <tr>
 *    <td style="border: 1px solid #999; padding: 4px;">File or DefaultValue</td>
 *    <td style="border: 1px solid #999; padding: 4px;">File name for the input JML file</td>
 *  </tr>
 *  <tr>
 *    <td style="border: 1px solid #999; padding: 4px;">CompressedFile</td>
 *    <td style="border: 1px solid #999; padding: 4px;">
 *      File name (a .zip or .gz) with a .jml/.xml/.gml inside (specified by
 *      File)
 *    </td>
 *  </tr>
 *  <tr>
 *    <td style="border: 1px solid #999; padding: 4px;">CompressedFileTemplate</td>
 *    <td style="border: 1px solid #999; padding: 4px;">
 *      File name (.zip or .gz) with the input template in (specified by
 *      InputTemplateFile)
 *    </td>
 *  </tr>
 * </table>
 * <br>
 */
public class JMLReader extends GMLReader { }
