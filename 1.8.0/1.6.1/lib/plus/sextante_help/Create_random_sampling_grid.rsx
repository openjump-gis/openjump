//polyg=vector
//numpoints=number
//output=output vector
//[Example scripts]=group
pts=spsample(polyg,numpoints,type="random")
output=SpatialPointsDataFrame(pts, as.data.frame(pts))
