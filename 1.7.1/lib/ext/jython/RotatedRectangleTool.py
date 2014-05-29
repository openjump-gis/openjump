# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
import com.vividsolutions.jts.geom.Coordinate as Coordinate
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool.FinalDrawListener as FinalDrawListener
from com.vividsolutions.jts.algorithm.CGAlgorithms import * 
import org.openjump.core.geomutils.GeoUtils as GeoUtils


class ToolListener(FinalDrawListener):
    def finalDraw(self, event):
        p = event.coords
        dist = distancePointLinePerpendicular(p[2], p[0], p[1])
        toLeft = computeOrientation(p[1], p[0], p[2]) == LEFT
        p[2] = GeoUtils.perpendicularVector(p[1], p[0], dist, toLeft)
        p.add(GeoUtils.vectorAdd(p[2], GeoUtils.vectorBetween(p[1], p[0]) ))
        #p.add(CoordUtils.add(p[2], CoordUtils.subtract(p[1], p[0]) )) #jts way
        p.add(Coordinate(p[0]))
        width = p[0].distance(p[1])
        length = p[1].distance(p[2])
        panel = event.wc.layerViewPanel
        statMsg = "[" + panel.format(width) + ", " + panel.format(length) + "]"
        event.statusMessage = statMsg
