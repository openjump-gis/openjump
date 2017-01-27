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

left = 1
top = 2
right = 3
bottom = 4
vertical = 5
horizontal = 6

_xDisp = 0.0
_yDisp = 0.0
_leftAlign = 0.0
_rightAlign = 0.0
_topAlign = 0.0
_bottomAlign = 0.0 
_verticalAlign = 0.0
_horizontalAlign = 0.0
_typeAlignment = left
_layerEditable = 1
_warnMsg = ""

def alignLeft(event):
    global _typeAlignment 
    _typeAlignment = left
    alignSelected()
    
def alignTop(event):
    global _typeAlignment 
    _typeAlignment = top
    alignSelected()
    
def alignRight(event):
    global _typeAlignment 
    _typeAlignment = right
    alignSelected()
    
def alignBottom(event):
    global _typeAlignment 
    _typeAlignment = bottom
    alignSelected()
    
def alignVertical(event):
    global _typeAlignment 
    _typeAlignment = vertical
    alignSelected()
    
def alignHorizontal(event):
    global _typeAlignment 
    _typeAlignment = horizontal
    alignSelected()
    
def alignSelected():
    global _leftAlign, _rightAlign, _topAlign, _bottomAlign, _verticalAlign, _horizontalAlign, _layerEditable, _warnMsg 

    #get the expanded envelope of all selected features
    layers = getLayersWithSelectedItems()
    if layers.size() == 0:
        warnUser("Nothing Selected")
        return
    envelope = Envelope()
    for layer in layers:
        selectedFeatures = featuresOnLayer(layer)
        for feature in selectedFeatures:
            envelope.expandToInclude(feature.getGeometry().getEnvelopeInternal())

    _leftAlign = envelope.getMinX()
    _rightAlign = envelope.getMaxX()
    _topAlign = envelope.getMaxY()
    _bottomAlign = envelope.getMinY()
    _verticalAlign = envelope.centre().x
    _horizontalAlign = envelope.centre().y
    name = "Align Features "
    
    if _typeAlignment == left:
        name = name + "Left"
    elif _typeAlignment == top:
        name = name + "Top"
    elif _typeAlignment == right:
        name = name + "Right"
    elif _typeAlignment == bottom:
        name = name + "Bottom"
    elif _typeAlignment == vertical:
        name = name + "Vertical"
    else: #_typeAlignment == horizontal:
        name = name + "Horizontal"
    alignGeo = AlignGeometry(name)
            
    # loop through each layer and align selected geometries
    _warnMsg = ""
    
    for layer in layers:
        _layerEditable = layer.isEditable()
        alignGeo.addTransactionOnSelection(layer)
    if len(_warnMsg) > 0:
        warnUser(_warnMsg)
    alignGeo.commitTransactions()
    repaint()

class AlignGeometry(ModifyGeometry):
    def modify(self, geometry): #this is called from ModifyGeometry.addTransactionOnSelection
        global _xDisp, _yDisp, _leftAlign, _rightAlign, _topAlign, _bottomAlign, _verticalAlign, _horizontalAlign, _typeAlignment, _layerEditable, _warnMsg
        env = geometry.getEnvelopeInternal()

        if _typeAlignment == left:
            minX = geometry.getEnvelopeInternal().getMinX()
            _xDisp = _leftAlign - minX
            _yDisp = 0.0
        elif _typeAlignment == top:
            maxY = geometry.getEnvelopeInternal().getMaxY()
            _xDisp = 0.0
            _yDisp = _topAlign - maxY
        elif _typeAlignment == right:
            maxX = geometry.getEnvelopeInternal().getMaxX()
            _xDisp = _rightAlign - maxX
            _yDisp = 0.0
        elif _typeAlignment == bottom:
            minY = geometry.getEnvelopeInternal().getMinY()
            _xDisp = 0.0
            _yDisp = _bottomAlign - minY
        elif _typeAlignment == vertical:
            _xDisp = _verticalAlign - geometry.getCentroid().getX()
            _yDisp = 0.0
        else: #_typeAlignment == horizontal:
            _xDisp = 0.0
            _yDisp = _horizontalAlign - geometry.getCentroid().getY()
            
        if (_layerEditable == 0) and (_xDisp <> 0.0 or _yDisp <> 0.0):
            _warnMsg = "Noneditable layer selected"
        else:
          cf = CoordFilter()
          geometry.apply(cf)   

class CoordFilter(CoordinateFilter):
    def filter(self, coordinate):
        global _xDisp, _yDisp
        displacement = Coordinate(_xDisp, _yDisp)
        coordinate.setCoordinate(CoordUtil.add(coordinate, displacement))
