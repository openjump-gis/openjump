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
package com.vividsolutions.jump.workbench.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vividsolutions.jump.util.Blackboard;

/**
 * Registry for DataSourceQueryChoosers which plug-ins may add to when the
 * JUMP Workbench starts up.
 */
public class DataSourceQueryChooserManager {
    private ArrayList loadDataSourceQueryChoosers = new ArrayList();
    public List getLoadDataSourceQueryChoosers() {
        return Collections.unmodifiableList(loadDataSourceQueryChoosers);
    }
    public DataSourceQueryChooserManager addLoadDataSourceQueryChooser(DataSourceQueryChooser chooser) {
        loadDataSourceQueryChoosers.add(chooser);
        return this;
    }    
    private ArrayList saveDataSourceQueryChoosers = new ArrayList();
    public List getSaveDataSourceQueryChoosers() {
            return Collections.unmodifiableList(saveDataSourceQueryChoosers);
        }
    public DataSourceQueryChooserManager addSaveDataSourceQueryChooser(DataSourceQueryChooser chooser) {
        saveDataSourceQueryChoosers.add(chooser);
        return this;
    }    
    /**
     * @param blackboard typically the Workbench blackboard
     */
    public static DataSourceQueryChooserManager get(Blackboard blackboard) {
        return (DataSourceQueryChooserManager) blackboard.get(DataSourceQueryChooserManager.class.getName() + " - INSTANCE", new DataSourceQueryChooserManager());
    }
}
