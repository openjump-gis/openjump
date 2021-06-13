
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

package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jump.I18N;


/**
 * Creators of Layers are not bound to use these category names, but they are
 * the standard ones.
 */
public class StandardCategoryNames {
    static final public String WORKING = I18N.getInstance().get("model.StandardCategoryNames.working");
    static final public String REFERENCE = I18N.getInstance().get("model.StandardCategoryNames.reference");
    static final public String SUBJECT = I18N.getInstance().get("model.StandardCategoryNames.subject");
    static final public String RESULT = I18N.getInstance().get("model.StandardCategoryNames.result");
    static final public String RESULT_REFERENCE = I18N.getInstance().get("model.StandardCategoryNames.result-reference");
    static final public String RESULT_SUBJECT = I18N.getInstance().get("model.StandardCategoryNames.result-subject");
    static final public String QA = I18N.getInstance().get("model.StandardCategoryNames.qa");
    static final public String SYSTEM = I18N.getInstance().get("model.StandardCategoryNames.system");
}
