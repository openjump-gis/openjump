# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# if you are going to use execfile("thisFile") from startup.py
# use _ prefix for local scope to avoid conflict with other modules
from org.openjump.util.python.JUMP_GIS_Framework import *
import org.openjump.util.python.ModifyGeometry as ModifyGeometry
import com.vividsolutions.jump.workbench.ui.GeometryEditor as GeometryEditor

def unionSelected(event):  #TODO: support undo better
    layers = getLayersWithSelectedItems()
    if layers.size() == 0:
        warnUser("Nothing Selected")
        return
    for layer in layers:
        if not layer.isEditable():
            warnUser("Noneditable layer(s) selected")
            return
    #get the union of all selected features
    selectedFeatures = getSelectedFeatures()
    unionGeometry = selectedFeatures[0].geometry
    otherFeatures = list(selectedFeatures)[1:]  #[1:] is skip first
    for feature in otherFeatures:
        unionGeometry = unionGeometry.union(feature.geometry)
    #set the geometry of the first feature of the first layer to the union        
    ModGeo = ModifyGeometry("Union Selected Features")
    selectedFeatures = featuresOnLayer(layers[0])
    ModGeo.addChangeGeometryTransaction(layers[0], selectedFeatures[0], unionGeometry)
    #remove all other features leaving only the union
    del selectedFeatures[0] #remove the first feature of the first layer from the list
    ModGeo.addRemoveFeaturesTransaction(layers[0], selectedFeatures)
    del layers[0]
    for layer in layers:
        selectedFeatures = featuresOnLayer(layer)
        ModGeo.addRemoveFeaturesTransaction(layer, selectedFeatures)
    ModGeo.commitTransactions()
    repaint()
    
        