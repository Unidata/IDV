

def makeNavigatedImage (d,ulLat,ulLon,lrLat,lrLon):
  """This takes a image data object and a lat/lon bounding box
     and adds a lat/lon domain to the data. Use it in conjunction with a formula:
  """
  from visad import Linear2DSet 
  from visad import RealTupleType
  ulLat=float(ulLat)
  ulLon=float(ulLon)
  lrLat=float(lrLat)
  lrLon=float(lrLon)
  domain = d.getDomainSet()
  newDomain = Linear2DSet(RealTupleType.SpatialEarth2DTuple,ulLon,lrLon,domain.getX().getLength(),ulLat,lrLat,domain.getY().getLength())
  return GridUtil.setSpatialDomain(d, newDomain)



def combineRGB(red, green, blue):
  """ combine 3 images as an RGB image """
  red=GridUtil.setParamType(red,makeRealType("redimage"), 0)
  green=GridUtil.setParamType(green,makeRealType("greenimage"), 0)
  blue=GridUtil.setParamType(blue,makeRealType("blueimage"), 0)
  return DerivedGridFactory.combineGrids((red,green,blue),1)


def combineABIRGB(chP64,chP86,chP47):
  """ GOES16/17 combine 3 images as an RGB image """
  green =  0.45*chP64 + 0.45*chP47 + 0.1*chP86
  red = GridUtil.setParamType(chP64,makeRealType("redimage"), 0)
  blue = GridUtil.setParamType(chP47,makeRealType("blueimage"), 0)
  grn = GridUtil.setParamType(green,makeRealType("greenimage"), 0)

  return DerivedGridFactory.combineGrids((red,grn,blue),1)
