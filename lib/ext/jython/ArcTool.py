# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License

# this tool demonstrates how to draw a feedback shape 
# that is different from the final shape
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool.FinalDrawListener as FinalDrawListener
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool.FeedbackListener as FeedbackListener
from com.vividsolutions.jts.algorithm.CGAlgorithms import * 
import com.vividsolutions.jump.geom.Angle as Angle
import org.openjump.core.geomutils.Arc as Arc

_wasClockwise = 1  #make global to retain value on each invocation
_fullAngle = 0.0   #_ will make it global to module

def _calculateArc(self, p, finalize=0):
    global _wasClockwise, _fullAngle  #allow access to global variables
    wasInFirstHalfPlane = _fullAngle >= -90.0 and _fullAngle <= 90.0
    radians = Angle.angleBetween(p[0], p[1], p[2])
    _fullAngle = Angle.toDegrees(radians)  #0 to +180
    isClockwise = computeOrientation(p[0], p[1], p[2]) == CLOCKWISE
    if _fullAngle <= 90.0 and wasInFirstHalfPlane: 
        _wasClockwise = isClockwise
    if not wasInFirstHalfPlane: 
        if _wasClockwise != isClockwise: _fullAngle = 360 - _fullAngle
    if not _wasClockwise: _fullAngle = -_fullAngle
    arc = Arc(p[0], p[1], _fullAngle)
    pa = arc.coordinates
    if finalize: p.clear()
    else: p.remove(2)  #delete point 2 leaving 0 and 1
    for pt in pa:
        p.add(pt)
    if not finalize: p.add(p[0])
    return _fullAngle

class ToolListenerFeedback(FeedbackListener):
    def feedbackDraw(self, event):
        p = event.coords
        radius = p[0].distance(p[1])
        degrees = _calculateArc(self, p, finalize=0)
        panel = event.wc.layerViewPanel
        statMsg = "Radius = " + panel.format(radius) + ", Angle = " + panel.format(degrees)
        event.statusMessage = statMsg

class ToolListenerFinal(FinalDrawListener):
    def finalDraw(self, event):
        _calculateArc(self, event.coords, finalize=1)
