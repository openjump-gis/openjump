# This script is run the first time the jython toolbox is opened
# The toolbox Tool Menu and the Tool Bar have already been added
# Example tools are defined here in order of simple to complex
import org.openjump.util.python.pythonexampleclasses.DrawCustomTool as DrawCustomTool
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil as FeatureDrawingUtil 
import javax.swing as swing
import java.io.File as File

featureDrawingUtil = FeatureDrawingUtil(toolbox.getContext())
toolMenu = toolbox.JMenuBar.getMenu(0)
sep = File.separator # / for linux and \ for windows

toolbox.centerPanel.components[0].hide() #comment out to initially show
#install menu items
def showConsole(event): toolbox.centerPanel.components[0].show(); toolbox.pack()
toolMenu.add(swing.JMenuItem("Show Console", actionPerformed=showConsole, icon = swing.ImageIcon(startuppath + "images" + sep + "console_show.png")))
def hideConsole(event): toolbox.centerPanel.components[0].hide(); toolbox.pack()
toolMenu.add(swing.JMenuItem("Hide Console", actionPerformed=hideConsole, icon = swing.ImageIcon(startuppath + "images" + sep + "console_hide.png")))
import UnionSelected  #too much code to inline.  use module
toolMenu.add(swing.JMenuItem("Union Selected", actionPerformed=UnionSelected.unionSelected, icon = swing.ImageIcon(startuppath + "images" + sep + "features_merge.png")))
import AlignSelected
alignMenu = swing.JMenu("Align Selected", icon = swing.ImageIcon(startuppath + "images" + sep + "shape_align_left.png"))
alignLeftMenu = swing.JMenuItem("Left", actionPerformed=AlignSelected.alignLeft, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_align_left.png"))
alignRightMenu = swing.JMenuItem("Right", actionPerformed=AlignSelected.alignRight, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_align_right.png"))
alignTopMenu = swing.JMenuItem("Top", actionPerformed=AlignSelected.alignTop, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_align_top.png"))
alignBottomMenu = swing.JMenuItem("Bottom", actionPerformed=AlignSelected.alignBottom, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_align_bottom.png"))
alignVerticalMenu = swing.JMenuItem("Center Vertical", actionPerformed=AlignSelected.alignVertical, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_center_vertical.png"))
alignHorizontalMenu = swing.JMenuItem("Center Horizontal", actionPerformed=AlignSelected.alignHorizontal, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_center_horizontal.png"))
alignMenu.add(alignLeftMenu)
alignMenu.add(alignTopMenu)
alignMenu.add(alignRightMenu)
alignMenu.add(alignBottomMenu)
alignMenu.add(alignVerticalMenu)
alignMenu.add(alignHorizontalMenu)
toolMenu.add(alignMenu)
import DistributeSelected
distributeMenu = swing.JMenu("Distribute Selected", icon = swing.ImageIcon(startuppath + "images" + sep + "shape_distribute_vertical.png"))
distributeVerticalMenu = swing.JMenuItem("Vertical", actionPerformed=DistributeSelected.distributeVertical, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_distribute_vertical.png"))
distributeHorizontalMenu = swing.JMenuItem("Horizontal", actionPerformed=DistributeSelected.distributeHorizontal, icon = swing.ImageIcon(startuppath + "images" + sep + "shape_distribute_horizontal.png"))
distributeMenu.add(distributeVerticalMenu)
distributeMenu.add(distributeHorizontalMenu)
toolMenu.add(distributeMenu)

#add a new panel with a label and edit text area for tool input
panel = swing.JPanel()
edit1 = swing.JTextField(preferredSize=(100,20))
label1 = swing.JLabel("Tool Input:")
panel.add("West", label1)
panel.add("East",edit1)
toolbox.centerPanel.add("South",panel)
toolbox.pack()
#toolbox.centerPanel.components[2].hide()  #uncomment to hide panel1

#install toolbox custom cursor tools
#tools can be defined in java only - as in this orphaned JUMP Note tool
import com.vividsolutions.jump.workbench.ui.cursortool.NoteTool as NoteTool
toolbox.add(NoteTool())  #surprise!  Deselect before changing tools to avoid bug
#Custom Tools can be defined with only a constructor
lineTool = DrawCustomTool(featureDrawingUtil, maxClicks = 2, toolName = "Line Tool", geometryType = "linestring")
toolbox.add(featureDrawingUtil.prepare(lineTool, 1))
#this one creates multipoints.  Double-Click or Right-click to stop
multiPointTool = DrawCustomTool(featureDrawingUtil, minClicks = 2, maxClicks = 99, toolName = "MultiPoint Tool", geometryType = "POINT", icon = swing.ImageIcon(startuppath +"images" + sep + "DrawPoint.gif"))
toolbox.add(featureDrawingUtil.prepare(multiPointTool, 1))

#Custom Tools can be defined with only an inline function and constructor
import org.openjump.core.geomutils.GeoUtils as GeoUtils  #imports are executable and can go anywhere
#jython statements with the same indention are considered a block
def corner(event):    #this event handler won't fire until fireClicks points
    p = event.coords  #set p to the current array of coordinates
    dist = p[2].distance(GeoUtils.getClosestPointOnLine(p[2], p[0], p[1]))
    toLeft = not GeoUtils.pointToRight(p[2], p[1], p[0])
    p[2] = GeoUtils.perpendicularVector(p[1], p[0], dist, toLeft)
toCorner = DrawCustomTool(featureDrawingUtil, finalDraw=corner, \
            minClicks = 3, maxClicks = 3, toolName = "Corner Tool", \
            icon = swing.ImageIcon(startuppath + "images" + sep + "DrawCorner.gif"), \
            fireClicks = 2, geometryType = "linestring")
toolbox.add(featureDrawingUtil.prepare(toCorner, 1))

#Complex Custom Tools can be defined in a class module and imported
rotRectTool = DrawCustomTool(featureDrawingUtil, minClicks = 3, maxClicks = 3, toolName = "Rotated Rectangle Tool", icon = swing.ImageIcon(startuppath + "images" + sep + "DrawRotRect.gif"), geometryType = "polygon");
import RotatedRectangleTool
# this tool has a feedback shape that is the same as the final geometry
rotRectTool.setFinalGeoListener(RotatedRectangleTool.ToolListener())
toolbox.add(featureDrawingUtil.prepare(rotRectTool, 1))

featureDrawingUtil = FeatureDrawingUtil(toolbox.getContext())
arcTool = DrawCustomTool(featureDrawingUtil, minClicks = 3, maxClicks = 3, toolName = "Arc Tool", icon = swing.ImageIcon(startuppath + "images" + sep + "DrawArc.gif"), geometryType = "LINESTRING");
import ArcTool
# this tool has a feedback shape that is different from the final geometry
arcTool.setFeedbackListener(ArcTool.ToolListenerFeedback())
arcTool.setFinalGeoListener(ArcTool.ToolListenerFinal())
toolbox.add(featureDrawingUtil.prepare(arcTool, 1))

circleTool = DrawCustomTool(featureDrawingUtil, minClicks = 2, maxClicks = 2, fireClicks = 1, toolName = "Tangent Circle Tool", icon = swing.ImageIcon(startuppath + "images" + sep + "DrawCircle.gif"), geometryType = "POLYGON");
import CircleTool
circleTool.setFeedbackListener(CircleTool.ToolListenerFeedback())
circleTool.setFinalGeoListener(CircleTool.ToolListenerFinal())
toolbox.add(featureDrawingUtil.prepare(circleTool, 1))

roadTool = DrawCustomTool(featureDrawingUtil, minClicks = 3, maxClicks = 99, fireClicks = 2, toolName = "Road Arc Tool", icon = swing.ImageIcon(startuppath + "images" + sep + "DrawRoad.gif"), geometryType = "POLYGON", strokeWidth = 3);
import RoadTool
roadListenerFinal = RoadTool.ToolListenerFinal()
roadListenerFinal.setGUI(label1, edit1, panel, toolbox)
roadTool.setFinalGeoListener(RoadTool.ToolListenerFinal())
roadTool.setFeedbackListener(RoadTool.ToolListenerFeedback())
# this tool has Activation and DeActivation event listeners that fire when tool icons are clicked
roadTool.setDeActivationListener(RoadTool.ToolListenerDeActivation())
roadTool.setActivationListener(RoadTool.ToolListenerActivation())
toolbox.add(featureDrawingUtil.prepare(roadTool, 1))

ovalTool = DrawCustomTool(featureDrawingUtil, fireClicks = 1, minClicks = 2, maxClicks = 2, toolName = "Oval Tool", icon = swing.ImageIcon(startuppath + "images" + sep + "DrawOval.gif"), geometryType = "polygon");
import OvalTool
ovalTool.setFinalGeoListener(OvalTool.ToolListener())
toolbox.add(featureDrawingUtil.prepare(ovalTool, 1))

try:   #you should make a UserStartup.py to add your own tools
    execfile(startuppath + "UserStartup.py")  #execfile targets share namespace
except:   #errors are assumed to be file missing
    pass  #do nothing
