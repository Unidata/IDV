

def makeNavigatedImage (d,ulLat,ulLon,lrLat,lrLon):
  """This takes a image data object and a lat/lon bounding box
     and adds a lat/lon domain to the data. Use it in conjunction with a formula:
     makeNavigatedImage(image,user_ulLat,user_ulLon,user_lrLat,user_lrLon)"""
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

