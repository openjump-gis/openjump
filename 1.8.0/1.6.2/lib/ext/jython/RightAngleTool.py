# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool.FinalDrawListener as FinalDrawListener
from com.vividsolutions.jts.algorithm.CGAlgorithms import * 
import org.openjump.core.geomutil.GeoUtils as GeoUtils

class ToolListener(FinalDrawListener):
    def finalDraw(self, event):
        p = event.coords
        dist = distancePointLinePerpendicular(p[2], p[0], p[1])
        toLeft = computeOrientation(p[0], p[1], p[2]) == LEFT
        p[2] = GeoUtils.perpendicularVector(p[1], p[0], dist, toLeft)
