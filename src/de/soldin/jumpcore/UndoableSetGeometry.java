/**
 * Copyright 2011 Edgar Soldin
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.soldin.jumpcore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;

/**
 * The <code>UndoableSetGeometry</code> is a implementation of a 
 * {@link java.util.Collection}, as well as a {@link 
 * javax.swing.undo.AbstractUndoableEdit}. The purpose is to have 
 * an undoable swing component for modifying geometries.
 * <p>
 * With these capabilities joined it can act as a container for multiple
 * <code>UndoableSetGeometry</code> objects, which can be executed in 
 * a batch and as a single action.
 * </p>
 */
public class UndoableSetGeometry 
	extends AbstractUndoableEdit
	implements Collection
	{

	private static final long serialVersionUID = 1L;
	private Collection actions = new Vector();
	private String name;
	private Layer layer;
			
	private HashMap proposed_geoms = new HashMap();
	private HashMap original_geoms = new HashMap();	
	
	public void redo() {
		execute();
		super.redo();
	}

	public void undo() {
		unexecute();
		super.undo();
	}
	
	public String getPresentationName(){
		return getName();
	}
	
	public String getUndoPresentationName(){
		return getName();
	}
	
	public String getRedoPresentationName(){
		return getName();
	}

	public UndoableSetGeometry(Layer layer, String name) {
		this.layer = layer;
		this.name = name+" (Layer: "+layer.getName()+")";
	}

	public UndoableSetGeometry(String name) {
		this.name = name;
	}

	public void execute() {
		//System.out.print("UT:execute() "+this+" ");
		if (layer!=null){
				
			//List features = layer.getFeatureCollectionWrapper().getFeatures();
			ArrayList modifiedFeatures = new ArrayList();
			ArrayList modifiedFeaturesOldClones = new ArrayList();
			
			for (Iterator iter = proposed_geoms.keySet().iterator(); iter.hasNext();) {
				Feature feature = (Feature) iter.next();
				Geometry new_geom = (Geometry)proposed_geoms.get(feature);
				Geometry old_geom = feature.getGeometry();
				
				original_geoms.put(feature,old_geom);

				modifiedFeatures.add(feature);
				modifiedFeaturesOldClones.add(feature.clone());
				feature.setGeometry(new_geom);
			}
			
			refreshUI(modifiedFeatures,modifiedFeaturesOldClones);

		}else{
			//System.out.print("batch ");

			for (Iterator iter = this.iterator(); iter.hasNext();) {
				UndoableSetGeometry transformation = (UndoableSetGeometry) iter.next();
				transformation.execute();
			}

		}

	}

	public void unexecute() {
		//System.out.print("UT:unexecute() "+this+" ");
		if (layer!=null && original_geoms.size()>0){
	
			//List features = layer.getFeatureCollectionWrapper().getFeatures();
			ArrayList modifiedFeatures = new ArrayList();
			ArrayList modifiedFeaturesOldClones = new ArrayList();
			
			for (Iterator iter = original_geoms.keySet().iterator(); iter.hasNext();) {
				Feature feature = (Feature) iter.next();
				Geometry new_geom = (Geometry)original_geoms.get(feature);

				modifiedFeatures.add(feature);
				modifiedFeaturesOldClones.add(feature.clone());
				feature.setGeometry(new_geom);
				
				//original_geoms.remove(feature);				
			}
			original_geoms.clear();
						
			refreshUI(modifiedFeatures,modifiedFeaturesOldClones);
			
		}else{
			//System.out.print("batch ");	
			for (Iterator iter = this.iterator(); iter.hasNext();) {
				UndoableSetGeometry transformation = (UndoableSetGeometry) iter.next();
				transformation.unexecute();
			}
		}
		//System.out.println();

	}
	
	private void refreshUI(ArrayList modifiedFeatures, ArrayList modifiedFeaturesOldClones){
		if (this.layer!=null) {
			Layer.tryToInvalidateEnvelope(this.layer);
			// fire the appropriate event, so everybody gets notified
			if (!modifiedFeatures.isEmpty()) {
				this.layer.getLayerManager().fireGeometryModified(
					modifiedFeatures,
					this.layer,
					modifiedFeaturesOldClones);
			}
		}	
	}	
	
	public String getName() {
		String out = "";
		if (layer==null){
			for (Iterator iter = actions.iterator(); iter.hasNext();) {
				out += (out.length()>0?", ":"")+((UndoableSetGeometry)iter.next()).getName();
			}
		}
		return name+": "+out;
	}
	
	public void setGeom(Feature feature, Geometry geom){
		proposed_geoms.put(feature,geom);
	}
	
	public Geometry getGeom(Feature in_feature){
		//List features = layer.getFeatureCollectionWrapper().getFeatures();
		//Feature feature = (Feature)features.get(features.indexOf(in_feature));
		return (Geometry)in_feature.getGeometry().clone();
	}

// Start of implementation of the collection methods 
	
	public boolean add(UndoableSetGeometry t){
		return actions.add(t);
	}

	public int size() {
		return actions.size();
	}

	public void clear() {
		actions.clear();
	}

	public boolean isEmpty() {
		if (layer==null) {
			for (Iterator i = actions.iterator(); i.hasNext();) {
				Collection action = (Collection) i.next();
				if (!action.isEmpty()) return false;
			}
			return true;
		}
		else
			return proposed_geoms.isEmpty();
	}

	public Object[] toArray() {
		return actions.toArray();
	}

	public boolean add(Object o) {
		return actions.add(o);
	}

	public boolean contains(Object o) {
		return actions.contains(o);
	}

	public boolean remove(Object o) {
		return actions.remove(o);
	}

	public boolean addAll(Collection c) {
		return actions.addAll(c);
	}

	public boolean containsAll(Collection c) {
		return actions.containsAll(c);
	}

	public boolean removeAll(Collection c) {
		return actions.removeAll(c);
	}

	public boolean retainAll(Collection c) {
		return actions.retainAll(c);
	}


	public Iterator iterator() {
		return actions.iterator();
	}

	public Object[] toArray(Object[] a) {
		return actions.toArray(a);
	}

}
