# Copyright (C) 2005 Integrated Systems Analysts, Inc.
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# if you are going to use execfile("thisFile") from startup.py
# use _ prefix for local scope to avoid conflict with other modules\
# Useage: will clone label attribute for first layer in selection 
from org.openjump.util.python.JUMP_GIS_Framework import *

def setASHSLabel(event):  #TODO: support undo better
    layers = getSelectedLayers()
    if layers.size() < 2:
        warnUser("Select more than 1 layer")
        return
    attribute1 = layers[0].labelStyle.attribute
    for layer in layers:
        layer.visible = 1
        labelStyle = layer.labelStyle
        labelStyle.attribute = attribute1
        labelStyle.scaling = 1
        labelStyle.enabled = 1
    repaint()

toolMenu.add(swing.JMenuItem("Clone Label Style", actionPerformed=setASHSLabel))
