""" This is the doc for the grid module """

def makeVector(a,b):
  return DerivedGridFactory.createFlowVectors(a,b)

def makeFlowField(a,b,c):
  return DerivedGridFactory.createFlowVectors(a,b,c)


def extractPressureFromNWPGrid(fieldimpl):
  """Get the pressure coordinate grid from a VisAD FieldImpl 
     (a time series of one or more FlatFields);
     user must be sure input is a suitable FlatField. """
  ff = fieldimpl.getSample(0)
  return DerivedGridFactory.createPressureGridFromDomain(ff)


def extractLatitudeFromNWPGrid(fieldimpl):
  """Get the latitude coordinate grid from a 3D VisAD FieldImpl 
     (a time series of one or more FlatFields);
     user must be sure input is a suitable FlatField. """
  ff = DerivedGridFactory.getLatitudeGrid(fieldimpl)
  #ff2 = GridUtil.sliceAtLevel(ff, l000.0)
  return ff



def getNthTimeGrid(fieldimpl, Nth):
  """Get the Nth grid in time series fieldimpl, a VisAD FieldImpl;
     user must be sure input is a suitable data field. 
     returns a FlatField. 
     Nth is an integer, >=0, <= max index of grid time series. """
  # print causes error : print " get sample N = "+N
  #dumpTypes(fieldimpl)  #this does print to console ok
  #trap bad values of N
  N = int(Nth)
  if N<0 :
    N = 0
  ff = fieldimpl.getSample(N)
  return ff


def getSliceAtLevel(fieldimpl, level) :
  """Extract a 2D horizontal slice from a 3D grid at "Level."
     level is a real number; must be appropriate for the grid.
     param fieldimpl is a VisAD FieldImpl which may have
     one or more time steps.  """
  level = float(level)
  ff = sliceAtLevel(fieldimpl, level)
  return ff



def getSliceAtAltitude(fieldimpl, alt, unit="m") :
  """ Extract a 2D horizontal slice from a 3D grid at the given altitude
      level is a real number; if unit is supplied, it must
      be compatible with meters (ft, fathoms, etc)
      param fieldimpl is a VisAD FieldImpl which may have
      one or more time steps.  """
  #import methods from
  from visad import RealType
  from visad import Real
  import visad.data.units.Parser as parser
  alt = float(alt)
  unit = parser.parse(str(unit))
  altitude = Real(RealType.Altitude, alt, unit)
  ff = GridUtil.sliceAtLevel(fieldimpl, altitude)
  return ff



def layerDiff(grid, top, bottom):
   """ Wrapper for calculating layer difference """
   return createLayerDifference(grid, top, bottom)

def getAltitude(z):
   """ Change units from geopotential meters to meters """
   import ucar.visad.quantities.GeopotentialAltitude as ga
   import ucar.visad.quantities.Gravity as gr
   zUnit = GridUtil.getParamType(z).getRealComponents()[0].getDefaultUnit()
   if zUnit.equals(ga.getGeopotentialMeter()):
      z = z.divide(gr.newReal())
   return z


def windShear(u, v, z, top, bottom):
   """ calculate the wind shear between discrete layers<pre>
   shear = sqrt((u(top)-u(bottom))^2 + (v(top)-v(bottom))^2)/zdiff</pre>"""
   udiff = layerDiff(u, top, bottom)
   vdiff = layerDiff(v, top, bottom)
   zdiff = layerDiff(z, top, bottom)
#  adjust to altitude if units are gpm
   zdiff = getAltitude(zdiff)
   windDiff = sqrt(udiff*udiff + vdiff*vdiff)
   return windDiff/zdiff


def windShearVector(u, v, top, bottom):
   """ calculate the u and v layer difference and return as vector """
   udiff = layerDiff(u, top, bottom)
   vdiff = layerDiff(v, top, bottom)
   return makeVector(udiff, vdiff)

def resampleGrid(oldGrid, gridwithNewDomain):
   """ display gridded data on a new domain """
   newLocs = GridUtil.getSpatialDomain(gridwithNewDomain)
   return GridUtil.resampleGrid(oldGrid, newLocs)

def makeTrueVector(u, v):
  """ true wind vectors """
  return DerivedGridFactory.createTrueWindVectors(u,v)


def horizontalAdvection(param, u, v):
  """ horizontal advection """
  return DerivedGridFactory.createHorizontalAdvection(param,u,v)

def horizontalDivergence(param, u, v):
  """ horizontal flux divergence """
  return DerivedGridFactory.createHorizontalFluxDivergence(param,u,v)

def combineFields(*a):
  """ combine several fields together """
  return DerivedGridFactory.combineGrids(a)


def newUnit(field, varname, unitname):
  """ set the name and unit on a grid """
  newunit = Util.parseUnit(unitname)
  newType = Util.makeRealType(varname, newunit)
  return GridUtil.setParamType(field, newType,0)

def make2D(slice):
  """Make a 2D slice from a 3D slice at a single level """
  return GridUtil.make2DGridFromSlice(slice)


def averageOverTime(field,makeTimes = 0):
    """Average the values in each time step
    If makeTimes is true (1) then we return a field mapping all of the times
    to the average. Else we just return the average """
    if (GridUtil.isTimeSequence(field)==0):
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


def applyToRange(function,data):
    newData = data.clone()
    f = function +'(rangeValue)'
    if (GridUtil.isTimeSequence(newData)):
        for t in range(newData.getDomainSet().getLength()):
            rangeValue = newData.getSample(t)
            result = eval(f)
            newData.setSample(t,result,0)
    else:
        rangeValue = newData
        newData = eval(f)
    return newData


def applyToRangeValues(function,data):
    newData = data.clone()
    f = function +'(values,step=step,rangeObject=rangeObject,field=field)'
    step=0
    if (GridUtil.isTimeSequence(newData)):
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




def makeTimeSequence(g):
  """ Merge a set of single time grids/images into a time sequence """
  from visad import FunctionType, FieldImpl, Gridded1DDoubleSet, QuickSort
  from jarray import array
  domain = getDomainSet(g[0])
  dt = getDomainType(g[0])
  v=[getDomain(g[i]).indexToDouble([0,])[0][0] for i in range(len(g))]
  va = array(v,'d')
  index = QuickSort.sort(va)
  ft=FunctionType(dt, getRangeType(g[0]))
  fld=FieldImpl(ft,Gridded1DDoubleSet.create(dt,va,None,domain.getSetUnits()[0],None))
  for i in range(len(g)):
     fld.setSample(i,g[index[i]].getSample(0),0)
  return fld

