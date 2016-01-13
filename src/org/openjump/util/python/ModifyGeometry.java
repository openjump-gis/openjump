/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.util.python;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GeometryEditor;
import com.vividsolutions.jump.workbench.model.UndoableEditReceiver;

public class ModifyGeometry
{
	private static WorkbenchContext workbenchContext = null;
    private String name;
    private ArrayList transactions = new ArrayList();

    public ModifyGeometry(String name)
    { 
    	this.name = name;
    }
    
	public static void setWorkbenchContext(WorkbenchContext workContext)
	{
		workbenchContext = workContext;
	}
	
    public void commitTransactions()
    {
		LayerManager lm = workbenchContext.getLayerManager();
		if (lm != null)
		{
			UndoableEditReceiver uer = lm.getUndoableEditReceiver();
			if (uer != null)
			{
				uer.startReceiving();
				uer.reportNothingToUndoYet();
				EditTransaction.commit(transactions);
				uer.stopReceiving();
			}
		}
		else
		{
			EditTransaction.commit(transactions);
		}
    }
    
    public void addTransactionOnSelection(Layer layer) 
    {
        EditTransaction transaction =
            EditTransaction.createTransactionOnSelection(new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) 
            {
                for (Iterator j = selectedItems.iterator(); j.hasNext();) 
                {
                    Geometry item = (Geometry) j.next();
                    modify(item); //define this in jython class
                }

                return geometryWithSelectedItems;
            }
        }, workbenchContext.getLayerViewPanel(), workbenchContext.getLayerViewPanel().getContext(), name, layer, false, false);
        transactions.add(transaction);
    }

    public void modify(Geometry geometry) 
    {
    	//define this in jython class
    }
    
    public void addRemoveFeaturesTransaction(Layer layer, Collection features) 
    {
    	GeometryEditor geometryEditor = new GeometryEditor();
    	EditTransaction transaction = new EditTransaction(features, name, layer, false, true, workbenchContext.getLayerViewPanel().getContext());
    	for (Iterator i = features.iterator(); i.hasNext();)
    	{
    		Feature feature = (Feature) i.next();
    		Geometry g = transaction.getGeometry(feature);
        	g = geometryEditor.remove(g, g);
        	transaction.setGeometry(feature, g);
    	}
    	transactions.add(transaction);
    }

    public void addChangeGeometryTransaction(Layer layer, Feature feature, Geometry geometry) 
    {
    	ArrayList features = new ArrayList();
    	features.add(feature);
    	EditTransaction transaction = new EditTransaction(features, name, layer, false, true, workbenchContext.getLayerViewPanel().getContext());
    	transaction.setGeometry(feature, geometry);
    	transactions.add(transaction);    	
    }

}
