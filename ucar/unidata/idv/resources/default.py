
import java

import sys;
sys.add_package('visad');
sys.add_package('visad.python');

from visad.python.JPythonMethods import *

def makeVector(a,b):
  from ucar.unidata.data.grid.DerivedGridFactory import *
  return DerivedGridFactory.createFlowVectors(a,b)

def makeFlowField(a,b,c):
  from ucar.unidata.data.grid.DerivedGridFactory import *
  return DerivedGridFactory.createFlowVectors(a,b,c)


def extractPressureFromNWPGrid(fieldimpl):
  """get pressure coordinate grid from a VisAD FieldImpl 
     (a time series of one or more FlatFields);
     user must be sure input is a suitable FlatField. """
  #import methods from
  from ucar.unidata.data.grid.DerivedGridFactory import *
  #import FlatField and FieldImpl class
  from visad.FlatField import *
  from visad.FieldImpl import *
  ff = fieldimpl.getSample(0)
  return DerivedGridFactory.createPressureGridFromDomain(ff)


def extractLatitudeFromNWPGrid(fieldimpl):
  """get the latitude coordinate grid from a 3D VisAD FieldImpl 
     (a time series of one or more FlatFields);
     user must be sure input is a suitable FlatField. """
  #import methods from
  from ucar.unidata.data.grid.DerivedGridFactory import *
  from ucar.unidata.data.grid.GridUtil import *
  #import FlatField and FieldImpl class
  from visad.FieldImpl import *
  ff = DerivedGridFactory.getLatitudeGrid(fieldimpl)
  #ff2 = GridUtil.sliceAtLevel(ff, l000.0)
  return ff


def getNthTimeGrid(fieldimpl, Nth):
  """get Nth grid in time series fieldimpl, a VisAD FieldImpl;
     user must be sure input is a suitable data field. 
     returns a FlatField. 
     Nth is an integer, >=0, <= max index of grid time series. """
  #import FlatField and FieldImpl class
  from visad.FlatField import *
  from visad.FieldImpl import *
  # print causes error : print " get sample N = "+N
  #dumpTypes(fieldimpl)  #this does print to console ok
  #trap bad values of N
  N = int(Nth)
  if N<0 :
    N = 0
  ff = fieldimpl.getSample(N)
  return ff


def getSliceAtLevel(fieldimpl, level) :
  """ extract a 2D horizontal slice from a 3D grid at "Level."
      level is a real number; must be appropriate for the grid.
      param fieldimpl is a VisAD FieldImpl which may have
      one or more time steps.  """
  #import methods from
  from ucar.unidata.data.grid.GridUtil import *
  from visad.FieldImpl import *
  level = float(level)
  ff = sliceAtLevel(fieldimpl, level)
  return ff


# map -  map line set
# topo - topography dataset
def make3DMap(map, topo):
  import ucar.unidata.data.grid.DerivedGridFactory as dm
  b = dm.create2DTopography(topo, topo)
  c = b.resample(map)
  return c


#This takes a image data object and a lat/lon bounding box
#and adds a lat/lon domain to the data
#Use it in conjunction with a formula:
#makeNavigatedImage(image,user_ulLat,user_ulLon,user_lrLat,user_lrLon)
def makeNavigatedImage (d,ulLat,ulLon,lrLat,lrLon):
  from ucar.unidata.data.grid.GridUtil import *
  from visad import Linear2DSet 
  from visad import RealTupleType
  ulLat=float(ulLat)
  ulLon=float(ulLon)
  lrLat=float(lrLat)
  lrLon=float(lrLon)
  domain = d.getDomainSet()
  newDomain = Linear2DSet(RealTupleType.SpatialEarth2DTuple,ulLon,lrLon,domain.getX().getLength(),ulLat,lrLat,domain.getY().getLength())
  return setSpatialDomain(d, newDomain)


import sys;
sys.add_package('visad');
sys.add_package('visad.data.units');
def getSliceAtAltitude(fieldimpl, alt, unit="m") :
  """ extract a 2D horizontal slice from a 3D grid at the given altitude
      level is a real number; if unit is supplied, it must
      be compatible with meters (ft, fathoms, etc)
      param fieldimpl is a VisAD FieldImpl which may have
      one or more time steps.  """
  #import methods from
  import ucar.unidata.data.grid.GridUtil as gu
  from visad import RealType
  from visad import Real
  import visad.data.units.Parser as parser
  alt = float(alt)
  unit = parser.parse(str(unit))
  altitude = Real(RealType.Altitude, alt, unit)
  ff = gu.sliceAtLevel(fieldimpl, altitude)
  return ff


# wrapper for calculating layer difference
def layerDiff(grid, top, bottom):
   from ucar.unidata.data.grid.DerivedGridFactory import *
   return createLayerDifference(grid, top, bottom)

#change units from geopotential meters to meters
def getAltitude(z):
   import ucar.visad.quantities.GeopotentialAltitude as ga
   import ucar.visad.quantities.Gravity as gr
   import ucar.unidata.data.grid.GridUtil as gu
   zUnit = gu.getParamType(z).getRealComponents()[0].getDefaultUnit()
   if zUnit.equals(ga.getGeopotentialMeter()):
      z = z.divide(gr.newReal())
   return z

#calculate the wind shear between discrete layers
# shear = sqrt((u(top)-u(bottom))^2 + (v(top)-v(bottom))^2)/zdiff
def windShear(u, v, z, top, bottom):
   import ucar.unidata.data.grid.GridUtil as gu
   udiff = layerDiff(u, top, bottom)
   vdiff = layerDiff(v, top, bottom)
   zdiff = layerDiff(z, top, bottom)
#  adjust to altitude if units are gpm
   zdiff = getAltitude(zdiff)
   windDiff = sqrt(udiff*udiff + vdiff*vdiff)
   return windDiff/zdiff

#calculate the u and v layer difference and return as vector
def windShearVector(u, v, top, bottom):
   import ucar.unidata.data.grid.DerivedGridFactory as dgf
   udiff = layerDiff(u, top, bottom)
   vdiff = layerDiff(v, top, bottom)
   return makeVector(udiff, vdiff)

# display gridded data on a new domain
def resampleGrid(oldGrid, gridwithNewDomain):
   import ucar.unidata.data.grid.GridUtil as gu
   newLocs = gu.getSpatialDomain(gridwithNewDomain)
   return gu.resampleGrid(oldGrid, newLocs)

# true wind vectors
def makeTrueVector(u, v):
  from ucar.unidata.data.grid.DerivedGridFactory import *
  return DerivedGridFactory.createTrueWindVectors(u,v)

# horizontal advection
def horizontalAdvection(param, u, v):
  from ucar.unidata.data.grid.DerivedGridFactory import *
  return DerivedGridFactory.createHorizontalAdvection(param,u,v)

# horizontal flux divergence
def horizontalAdvection(param, u, v):
  from ucar.unidata.data.grid.DerivedGridFactory import *
  return DerivedGridFactory.createHorizontalFluxDivergence(param,u,v)

# combine several fields together
def combineFields(*a):
  from ucar.unidata.data.grid.DerivedGridFactory import *
  return DerivedGridFactory.combineGrids(a)

# combine 3 images as an RGB image
def combineRGB(red, green, blue):
  from ucar.unidata.data.grid.GridUtil import *
  red=setParamType(red,makeRealType("redimage"), 0)
  green=setParamType(green,makeRealType("greenimage"), 0)
  blue=setParamType(blue,makeRealType("blueimage"), 0)
  return DerivedGridFactory.combineGrids((red,green,blue),1)

# set the name and unit on a grid
def newUnit(field, varname, unitname):
  from ucar.visad import Util
  from ucar.unidata.data.grid import GridUtil
  newunit = Util.parseUnit(unitname)
  newType = Util.makeRealType(varname, newunit)
  return GridUtil.setParamType(field, newType,0)

# make a 2D slice from a 3D slice
def make2D(slice):
  from ucar.unidata.data.grid import GridUtil
  return GridUtil.make2DGridFromSlice(slice)




#Average the values in each time step
def averageOverTime(field,makeTimes = 0):
    import ucar.unidata.data.grid.GridUtil as gu
    if (gu.isTimeSequence(field)==0):
        return field;
    cnt = 0;
    domainSet = field.getDomainSet()
    current = None;
    for t in range(domainSet.getLength()):
        cnt=cnt+1
        rangeValue = field.getSample(t)
        if(current == None):
            current = rangeValue.clone();
        else:
            current = current+rangeValue;
    if(cnt == 0):
        return None;
    current = current/cnt;
    if(makeTimes):
        return Util.makeTimeField(current, GridUtil.getDateTimeList(field))
    return current


#
def makeFloatArray(rows,cols,value):
    import ucar.unidata.data.grid.GridUtil as gu
    return gu.makeFloatArray(rows,cols,value);


def applyToRange(function,data):
    import ucar.unidata.data.grid.GridUtil as gu
    newData = data.clone()
    f = function +'(rangeValue)'
    if (gu.isTimeSequence(newData)):
        for t in range(newData.getDomainSet().getLength()):
            rangeValue = newData.getSample(t)
            result = eval(f)
            newData.setSample(t,result,0)
    else:
        rangeValue = newData
        newData = eval(f)
    return newData


def applyToRangeValues(function,data):
    import ucar.unidata.data.grid.GridUtil as gu
    newData = data.clone()
    f = function +'(values,step=step,rangeObject=rangeObject,field=field)'
    step=0
    if (gu.isTimeSequence(newData)):
        for t in range(newData.getDomainSet().getLength()):
            rangeObject = newData.getSample(t)
            values = rangeObject.getFloats(0)
            values = eval(f)
            rangeObject.setSamples(values,1)
            step= step+1
    else:
        rangeObject = newData
        values = rangeObject.getFloats(0)
        values = eval(f)
        rangeObject.setSamples(values,1)
    return newData



def changeRange(d):
##   return   applyToRange('testApplyToRange',d);
   return   applyToRangeValues('testApplyToRange2',d);


def testApplyToRange(d,**args):
    r = d.getFloats(0)
    total = 0
    for i in xrange(len(r[0])):
        total= total+r[0][i]
    avg = total/len(r[0])
    for i in xrange(len(r[0])):
        if(r[0][i]<avg):
            r[0][i] = 0;
    d.setSamples(r)
    return d

def testApplyToRange2(r,**args):
    keys = args.keys()
    total = 0
    for i in xrange(len(r[0])):
        total= total+r[0][i]
    avg = total/len(r[0])
    for i in xrange(len(r[0])):
        r[0][i] = avg-r[0][i];
    return r


