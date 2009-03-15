# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool as DrawCustomTool
from com.vividsolutions.jts.algorithm.CGAlgorithms import * 
import com.vividsolutions.jts.geom.Coordinate as Coordinate
import com.vividsolutions.jump.geom.CoordUtil as CU
import org.openjump.core.geomutils.GeoUtils as GeoUtils
import javax.swing as swing
import java.util.ArrayList as ArrayList
import com.vividsolutions.jump.geom.Angle as Angle
import org.openjump.core.geomutils.Arc as Arc
#from org.openjump.util.python.JUMP_GIS_Framework import showMessage
#from org.openjump.util.python.JUMP_GIS_Framework import warnUser

_label1 = None
_edit1 = None
_button1 = None
_panel = None
_toolbox = None
_laneWidths = []
_doBackup = 0
_skip = []
_radius = 0
_isActive = 0

def _backup(event):
    global _doBackup
    _doBackup = 1

def _roadDraw(p, final=0):
    global _edit1, _laneWidths, _doBackup, _skip, _radius, _roadCenterLine
    roadEdgeLeft = ArrayList()
    roadEdgeRight = ArrayList()
    _roadCenterLine = ArrayList()
    try:
        laneWidth = float(_edit1.text)
    except:
        laneWidth = p[0].distance(p[1])
    roadWidth = laneWidth * 2.0
    if p.size() == 3:
        _laneWidths = [laneWidth, laneWidth]
    _edit1.text = "%f" % laneWidth    
    p.remove(0)
    if len(_laneWidths) < p.size():
        _laneWidths.append(laneWidth)
    else:
        _laneWidths[-1] = laneWidth
    #handle the backup button
    for skip in _skip: p.remove(skip)
    skip = 0
    if _doBackup:
        skip = p.size() - 2  #index of prior point
        _skip.append(skip)   #we'll need it next time we get p[]
        _doBackup = 0
        if skip > 1:         #lane width skips can be handled now
            _laneWidths = _laneWidths[:skip] + _laneWidths[skip+1:]
    if skip > 1:
        p.remove(skip)
    left = 1
    right = 0
    #crank in the initial road rectangle left and right edges
    roadEdgeLeft.add(GeoUtils.perpendicularVector(p[0], p[1], _laneWidths[0], left))
    roadEdgeRight.add(GeoUtils.perpendicularVector(p[0], p[1], _laneWidths[0], left))
    roadEdgeRight.add(GeoUtils.perpendicularVector(p[0], p[1], _laneWidths[0], right))
    roadEdgeLeft.add(GeoUtils.perpendicularVector(p[1], p[0], _laneWidths[1], right))
    roadEdgeRight.add(GeoUtils.perpendicularVector(p[1], p[0], _laneWidths[1], left))
    _roadCenterLine.add(p[0])
    _roadCenterLine.add(p[1])
    #calculate the left and right edges of the rest of the road
    currPos = 1
    endPos = p.size() - 1 #the last point in the array is the curr mouse pos adjusted by constraints
    perp1 = GeoUtils.perpendicularVector(p[1], p[0], roadWidth, right) 
    while currPos < endPos:
        midPt = CU.add( p[currPos], CU.divide(CU.subtract(p[currPos + 1], p[currPos]), 2))
        perp2 = GeoUtils.perpendicularVector(midPt, p[currPos], roadWidth, right)
        center = GeoUtils.getIntersection(p[currPos], perp1, midPt, perp2)
        if center.z == 0.0:  #check for parallel vectors
            radius = center.distance(p[currPos])
            d = p[currPos].distance(midPt)
            curved = radius / d < 50  #compare to tangent threshold
        else: curved = 0
        if curved: #construct a circular curve
            circularArc = (_laneWidths[currPos] == _laneWidths[currPos + 1])
            radius = center.distance(p[currPos])
            toLeft = computeOrientation(p[currPos], perp1, p[currPos + 1]) == LEFT
            basePerp = GeoUtils.perpendicularVector(p[currPos], perp1, roadWidth, toLeft)
            toLeft = computeOrientation(p[currPos], basePerp, p[currPos + 1]) == LEFT 
            radians = Angle.angleBetween(center, p[currPos], p[currPos + 1])
            
            if circularArc:
                #left edge
                if toLeft:
                    angle = -Angle.toDegrees(radians)
                    ratio = (radius - _laneWidths[currPos]) / radius
                else:
                    angle = Angle.toDegrees(radians)
                    ratio = (radius + _laneWidths[currPos]) / radius
                arc = Arc(center, p[currPos], angle)
                arcPts = arc.getCoordinates()
                for pt in arcPts:
                    _roadCenterLine.add(pt)
                start = CU.add(center, CU.multiply(ratio, CU.subtract(p[currPos], center)))
                arc = Arc(center, start, angle)
                arcPts = arc.getCoordinates()
                for pt in arcPts:
                    roadEdgeLeft.add(pt)
                #right edge 
                if toLeft:
                    ratio = (radius + _laneWidths[currPos]) / radius
                else:
                    ratio = (radius - _laneWidths[currPos]) / radius
                start = CU.add(center, CU.multiply(ratio, CU.subtract(p[currPos], center)))
                arc = Arc(center, start, angle)
                arcPts = arc.getCoordinates()
                for pt in arcPts:
                    roadEdgeRight.add(pt)
                perp1.x = center.x
                perp1.y = center.y
                _radius = radius - _laneWidths[currPos]
            else: #smooth tangent non-circular curve
                #left edge
                if toLeft:
                    angle = -Angle.toDegrees(radians)
                else:
                    angle = Angle.toDegrees(radians)
                start = p[currPos]
                arc = Arc(center, start, angle)
                arcPts = arc.getCoordinates()
                for pt in arcPts:
                    _roadCenterLine.add(pt)
                inc = (_laneWidths[currPos + 1] - _laneWidths[currPos]) / (arcPts.size() - 1)
                i = 0
                for pt in arcPts:
                    if toLeft:
                        ratio = (radius - _laneWidths[currPos]  - (i * inc)) / radius
                    else:
                        ratio = (radius +  _laneWidths[currPos] + (i * inc)) / radius
                    roadEdgeLeft.add(CU.add(center, CU.multiply(ratio, CU.subtract(pt, center))))
                    i += 1
                #right edge
                i = 0
                for pt in arcPts:
                    if toLeft:
                        ratio = (radius + _laneWidths[currPos] + (i * inc)) / radius
                    else:
                        ratio = (radius - _laneWidths[currPos] - (i * inc)) / radius
                    roadEdgeRight.add(CU.add(center, CU.multiply(ratio, CU.subtract(pt, center))))
                    i += 1
                perp1.x = center.x
                perp1.y = center.y
                _radius = radius - _laneWidths[currPos]
        else: #construct straight section of road
            roadEdgeLeft.add(GeoUtils.perpendicularVector(p[currPos + 1], p[currPos], _laneWidths[currPos + 1], right))
            roadEdgeRight.add(GeoUtils.perpendicularVector(p[currPos + 1], p[currPos], _laneWidths[currPos + 1], left))
            perp1 = GeoUtils.perpendicularVector(p[currPos + 1], p[currPos], _laneWidths[currPos + 1], right)
            _roadCenterLine.add(p[currPos + 1])
            _radius = 0
        currPos += 1
    #if final:   #uncomment to display click point feedback for debugging
    p.clear()    #clear the click point array and replace with road edges
    for pt in roadEdgeLeft:
        p.add(pt)
    #trace right return path back to start
    i = roadEdgeRight.size() - 1
    while i >= 0:
        p.add(roadEdgeRight[i])
        i -= 1
        
class ToolListenerFeedback(DrawCustomTool.FeedbackListener):
    def feedbackDraw(self, event):
        global _radius
        _roadDraw(event.coords, final=0)
        for pt in _roadCenterLine:
            event.coords.add(pt)
        
        panel = event.wc.layerViewPanel
        statMsg = "Inside radius " + panel.format(_radius)
        event.statusMessage = statMsg
    
class ToolListenerFinal(DrawCustomTool.FinalDrawListener):
    def finalDraw(self, event):
        global _label1, _edit1, _button1, _laneWidths, _skip, _radius
        _roadDraw(event.coords, final=1)
        _edit1.text = ""
        _laneWidths = []
        _skip = []
    def setGUI(self, label1, edit1, panel, toolbox):
        global _label1, _edit1, _panel, _toolbox
        _label1 = label1
        _edit1 = edit1
        _panel = panel
        _toolbox = toolbox
        _radius = 0

class ToolListenerDeActivation(DrawCustomTool.DeActivationListener):
    def handleDeActivation(self, event):
        global _label1, _edit1, _panel, _button1, _toolbox, _isActive
        _label1.text = "Tool input:"
        _edit1.text = ""
        _button1.actionPerformed = None
        _panel.remove(_button1)
        _isActive = 0
        _toolbox.pack()

class ToolListenerActivation(DrawCustomTool.ActivationListener):
    def handleActivation(self, event):
        global _label1, _button1, _doBackup, _isActive, _toolbox      
        _label1.text = "Lane Width"
        if not _isActive:
            _button1 = swing.JButton("Backup")
            _panel.add("East",_button1)
            _button1.actionPerformed = _backup
            _toolbox.pack()
        _isActive = 1
        _doBackup = 0
  