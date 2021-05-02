# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool.FinalDrawListener as FinalDrawListener
import org.locationtech.jts.geom.GeometryFactory as GeometryFactory
import org.locationtech.jts.util.GeometricShapeFactory as GeometricShapeFactory
import org.locationtech.jts.geom.Coordinate as Coordinate

class ToolListener(FinalDrawListener):
    def finalDraw(self, event):
        p = event.coords
        width = abs(p[0].x - p[1].x)
        height = abs(p[0].y - p[1].y)
        geo = GeometryFactory()
        shapeFac = GeometricShapeFactory(geo)
        lowLeft = Coordinate()
        lowLeft.x = min(p[0].x, p[1].x)
        lowLeft.y = min(p[0].y ,p[1].y)
        shapeFac.setBase(lowLeft)
        shapeFac.setWidth(width)
        shapeFac.setHeight(height)
        shapeFac.setNumPoints(48)
        pa = shapeFac.createCircle().coordinates
        p.clear()
        for pt in pa:
            p.add(pt)
        panel = event.wc.layerViewPanel
        statMsg = "[" + panel.format(width) + ", " + panel.format(height) + "]"
        event.statusMessage = statMsg
