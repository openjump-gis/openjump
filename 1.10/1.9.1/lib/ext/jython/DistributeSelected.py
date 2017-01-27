# Copyright (C) 2006 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# if you are going to use execfile("thisFile") from startup.py
# use _ prefix for local scope to avoid conflict with other modules

import com.vividsolutions.jts.geom.Coordinate as Coordinate
import com.vividsolutions.jts.geom.CoordinateFilter as CoordinateFilter
import com.vividsolutions.jump.geom.CoordUtil as CoordUtil
import com.vividsolutions.jump.feature.Feature as Feature
import com.vividsolutions.jts.geom.Geometry as Geometry
import com.vividsolutions.jts.geom.Envelope as Envelope
import org.openjump.util.python.ModifyGeometry as ModifyGeometry
from org.openjump.util.python.JUMP_GIS_Framework import *

vertical = 1
horizontal = 2

_xDisp = 0.0
_yDisp = 0.0
_typeDistribution = vertical

def distributeVertical(event):
    global _typeDistribution 
    _typeDistribution = vertical
    distributeSelected()
    
def distributeHorizontal(event):
    global _typeDistribution 
    _typeDistribution = horizontal
    distributeSelected()
    
def distributeSelected():
    global _xDisp, _yDisp
    warnMsg = ""
    
    layers = getLayersWithSelectedItems()
    if layers.size() == 0:
        warnUser("Nothing Selected")
        return
        
    #get the expanded envelope of all selected features
    #also get the sum of all the individual envelopes
    totalEnvelope = Envelope()
    usedWidth = 0
    usedHeight = 0
    featureList = []
    for layer in layers:
        selectedFeatures = featuresOnLayer(layer)
        for feature in selectedFeatures:
            indivEnv = feature.getGeometry().getEnvelopeInternal()
            usedWidth = usedWidth + indivEnv.getWidth()
            usedHeight = usedHeight + indivEnv.getHeight()
            totalEnvelope.expandToInclude(indivEnv)
            featureTuple = (feature, indivEnv, layer)
            featureList.append(featureTuple)
            
    if len(featureList) < 3:
        return
    name = "Distribute Features "
    if _typeDistribution == vertical:
        name = name + "Vertical"
    else: #_typeDistribution == horizontal:
        name = name + "Horizontal"
    distributeGeo = ModifyGeometry(name)
    cf = CoordFilter()
    
    if _typeDistribution == vertical: 
        verticalSpacing = (totalEnvelope.getHeight() - usedHeight) / (len(featureList) - 1)        
        newPos = totalEnvelope.getMinY()
        while len(featureList) > 0:
            #find the bottom most feature
            index = 0
            minPos = totalEnvelope.getMaxY()
        
            for featureTuple in featureList:
                pos = featureTuple[1].getMinY()
                if pos < minPos:
                    currIndex = index
                    minPos = pos
                index = index + 1
            
            currTuple = featureList[currIndex]
            feature = currTuple[0]
            geo = feature.getGeometry().clone()
            _xDisp = 0
            _yDisp = newPos - currTuple[1].getMinY()
            if (not currTuple[2].isEditable()) and (_yDisp <> 0.0):
                warnMsg = "Noneditable layer selected"
            else:
                geo.apply(cf) 
                distributeGeo.addChangeGeometryTransaction(currTuple[2], currTuple[0], geo)
            newPos = newPos + currTuple[1].getHeight() + verticalSpacing
            del featureList[currIndex]
            
    else: #_typeDistribution == horizontal:   
        horizontalSpacing = (totalEnvelope.getWidth() - usedWidth) / (len(featureList) - 1)        
        newPos = totalEnvelope.getMinX()
        while len(featureList) > 0:
            #find the left most feature
            index = 0
            minPos = totalEnvelope.getMaxX()
        
            for featureTuple in featureList:
                pos = featureTuple[1].getMinX()
                if pos < minPos:
                    currIndex = index
                    minPos = pos
                index = index + 1
            
            currTuple = featureList[currIndex]
            feature = currTuple[0]
            geo = feature.getGeometry().clone()
            _xDisp = newPos - currTuple[1].getMinX()
            _yDisp = 0
            if (not currTuple[2].isEditable()) and (_xDisp <> 0.0):
                warnMsg = "Noneditable layer selected"
            else:
                geo.apply(cf) 
                distributeGeo.addChangeGeometryTransaction(currTuple[2], currTuple[0], geo)
            newPos = newPos + currTuple[1].getWidth() + horizontalSpacing
            del featureList[currIndex]
        
    distributeGeo.commitTransactions()
    if len(warnMsg) > 0:
        warnUser(warnMsg)    
    repaint()

class CoordFilter(CoordinateFilter):
    def filter(self, coordinate):
        global _xDisp, _yDisp
        displacement = Coordinate(_xDisp, _yDisp)
        coordinate.setCoordinate(CoordUtil.add(coordinate, displacement))
