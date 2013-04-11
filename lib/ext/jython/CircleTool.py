# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
#import com.vividsolutions.jts.geom.Coordinate as Coordinate
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool.FinalDrawListener as FinalDrawListener
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool.FeedbackListener as FeedbackListener
import org.openjump.core.geomutils.Arc as Arc

def _calculateCircle(p, pixelSize=0.1):
    arc = Arc(p[1], p[0], 360)
    arc.arcTolerance = pixelSize
    pa = arc.coordinates
    p.clear()
    for pt in pa:
        p.add(pt)

class ToolListenerFeedback(FeedbackListener):
    def feedbackDraw(self, event):
        p = event.coords
        radius = p[0].distance(p[1])
        panel = event.wc.layerViewPanel
        s = panel.viewport.envelopeInModelCoordinates.width / panel.width
        _calculateCircle(event.coords, pixelSize=s/2)
        statMsg = "Radius = " + panel.format(radius)
        event.statusMessage = statMsg
        
class ToolListenerFinal(FinalDrawListener):
    def finalDraw(self, event):
        p = event.coords
        radius = p[0].distance(p[1])
        panel = event.wc.layerViewPanel
        s = panel.viewport.envelopeInModelCoordinates.width / panel.width
        #s = radius / panel.width  #uncomment for fixed number of points
        _calculateCircle(event.coords, pixelSize=s/2)
        statMsg = "Radius = " + panel.format(radius)
        event.statusMessage = statMsg
