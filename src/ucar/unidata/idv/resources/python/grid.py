""" This is the doc for the grid module """

def makeVector(a,b):
  """ Make a vector from 2 components
  <div class=jython>
      makeVector(a,b) = [a,b]
  </div>
  """
  return DerivedGridFactory.createFlowVectors(a,b)

def makeFlowField(a,b,c):
  """ Make a vector from 3 components
  <div class=jython>
      makeVector(a,b,c) = [a,b,c]
  </div>
  """
  return DerivedGridFactory.createFlowVectors(a,b,c)


def extractPressureFromNWPGrid(fieldimpl):
  """Get the pressure coordinate from a time series grid and
     return a grid of the pressure at all points.  Input
     grid must have pressure or height (which is converted
     to pressure in the standard atmosphere).
     User must be sure input is a suitable FlatField.
  """
  ff = fieldimpl.getSample(0)
  return DerivedGridFactory.createPressureGridFromDomain(ff)


def extractLatitudeFromNWPGrid(fieldimpl):
  """Get the latitude coordinate from a grid.  Return a grid
  of the latitudes at each point.
  """
  ff = DerivedGridFactory.createLatitudeGrid(fieldimpl)
  return ff

def extractLongitudeFromNWPGrid(fieldimpl):
  """Get the longitude coordinate from a grid.  Return a grid
  of the longitudes at each point.
  """
  ff = DerivedGridFactory.createLongitudeGrid(fieldimpl)
  return ff


def getNthTimeGrid(fieldimpl, Nth):
  """Get the Nth grid in time series of grids;
     User must be sure input is a suitable data field.
     returns a single time.
     Nth is an integer, >=0, <= max index of grid time series. """
  #trap bad values of N
  N = int(Nth)
  if N<0 :
    N = 0
  ff = fieldimpl.getSample(N)
  return ff


def getSliceAtLevel(fieldimpl, level) :
  """Extract a 2D horizontal slice from a 3D grid at "Level."
     level is a real number; must be appropriate for the grid.
     param fieldimpl is a grid which may have one or more time steps.  """
  level = float(level)
  ff = GridUtil.sliceAtLevel(fieldimpl, level)
  return ff



def getSliceAtAltitude(fieldimpl, alt, unit="m") :
  """ Extract a 2D horizontal slice from a 3D grid at the given altitude;
      level is a real number; if unit is supplied, it must
      be compatible with meters (ft, fathoms, etc)
      param fieldimpl is a grid which may have
      one or more time steps.  """
  #import methods from
  from visad import RealType
  from visad import Real
  alt = float(alt)
  unit = Util.parseUnit(unit)
  altitude = Real(RealType.Altitude, alt, unit)
  ff = GridUtil.sliceAtLevel(fieldimpl, altitude)
  return ff



def layerAverage(grid, top, bottom, unit=None):
   """ Wrapper for calculating layer average """
   return DerivedGridFactory.createLayerAverage(grid, top, bottom, unit)

def layerDiff(grid, top, bottom, unit=None):
   """ Wrapper for calculating layer difference """
   return DerivedGridFactory.createLayerDifference(grid, top, bottom, unit)

def getAltitude(z):
   """ Change units from geopotential meters to meters """
   import ucar.visad.quantities.GeopotentialAltitude as ga
   import ucar.visad.quantities.Gravity as gr
   zUnit = GridUtil.getParamType(z).getRealComponents()[0].getDefaultUnit()
   if zUnit.equals(ga.getGeopotentialMeter()):
      z = z.divide(gr.newReal())
   return z


def windShear(u, v, z, top, bottom, unit=None):
   """ calculate the wind shear between discrete layers
   <div class=jython>
   shear = sqrt((u(top)-u(bottom))^2 + (v(top)-v(bottom))^2)/zdiff</pre>
   </div>
   """
   udiff = layerDiff(u, top, bottom, unit)
   vdiff = layerDiff(v, top, bottom, unit)
   zdiff = layerDiff(z, top, bottom, unit)
#  adjust to altitude if units are gpm
   zdiff = getAltitude(zdiff)
   windDiff = sqrt(udiff*udiff + vdiff*vdiff)
   return windDiff/zdiff


def windShearVector(u, v, top, bottom, unit=None):
   """ calculate the u and v layer difference and return as vector
   """
   udiff = layerDiff(u, top, bottom, unit)
   vdiff = layerDiff(v, top, bottom, unit)
   return makeVector(udiff, vdiff)

def resampleGrid(oldGrid, gridwithNewDomain):
   """ display gridded data on a new domain
   """
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

def newName(field, varname, copy = 0):
  """ create a new field with a new parameter name """
  return GridUtil.setParamType(field, varname, copy)

def newUnit(field, varname, unitname):
  """ set the name and unit on a grid """
  newunit = Util.parseUnit(unitname)
  newType = Util.makeRealType(varname, newunit)
  return GridUtil.setParamType(field, newType,0)

def noUnit(field):
  """ remove the units from a grid """
  import visad
  from visad import CommonUnit
  newunit = CommonUnit.promiscuous
  rt = GridUtil.getParamType(field).getRealComponents()[0]
  newType = Util.makeRealType(rt.getName(), visad.CommonUnit.promiscuous)
  return GridUtil.setParamType(field, newType,0)

def make2D(slice):
  """Make a 2D slice from a 3D slice at a single level """
  return GridUtil.make2DGridFromSlice(slice)


def sumOverTime(field,makeTimes=0):
    """Take the sum of the values in each time step
    If makeTimes is true (1) then we return a field mapping all of the times
    to the average. Else we just return the sum """
    return GridMath.sumOverTime(field,makeTimes);

def minOverTime(field,makeTimes=0):
    """Take the min of the values in each time step
    If makeTimes is true (1) then we return a field mapping all of the times
    to the average. Else we just return the min """
    return GridMath.minOverTime(field,makeTimes);

def maxOverTime(field,makeTimes=0):
    """Take the max of the values in each time step
    If makeTimes is true (1) then we return a field mapping all of the times
    to the average. Else we just return the max """
    return GridMath.maxOverTime(field,makeTimes);

def averageOverTime(field,makeTimes=0):
    """Average the values in each time step
    If makeTimes is true (1) then we return a field mapping all of the times
    to the average. Else we just return the average """
    return GridMath.averageOverTime(field,makeTimes);


def differenceFromBaseTime(field):
    """set the value of each time step N:
    D(N)= D(N) - D(0)
    """
    return GridMath.differenceFromBaseTime(field);

def sumFromBaseTime(field):
    """set the value of each time step N:
    D(N)= D(N) + D(0)
    """
    return GridMath.sumFromBaseTime(field);


def timeStepDifference(field,offset=-1):
    """set the value of each time step N:
    D(N)= D(N) - D(N+offset)
    where offset should be negative
    """
    offset = int(offset)
    return GridMath.timeStepDifference(field,offset);


def timeStepSum(field,offset=-1):
    """set the value of each time step N:
    D(N)= D(N) + D(N+offset)
    where offset should be negative
    """
    offset = int(offset)
    return GridMath.timeStepSum(field,offset);


def oldaverageOverTime(field,makeTimes = 0):
    """@deprecated Average the values in each time step
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
        if(current is None):
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
    """ Apply the function name to each timestep of the data """
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
    """ Apply the function name to each value in each timestep of the data """
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

def mergeTimeSequences(g):
  """ Merge a set of time sequences of grids/images into a single time sequence.
      All grids/images must have the same parameter name """
  from visad import FunctionType, FieldImpl, Gridded1DDoubleSet, Set, Unit
  domain = getDomainSet(g[0])
  newDomain = domain
  dt = getDomainType(g[0])
  rt = getRangeType(g[0])
  for i in range(1,len(g)):
     newDomain = newDomain.merge1DSets(getDomainSet(g[i]))
  ft=FunctionType(dt, rt)
  fld=FieldImpl(ft,newDomain)
  for i in range(len(g)):
     oldDomain = getDomainSet(g[i])
     values = oldDomain.getDoubles(1)
     values = Unit.convertTuple(values, oldDomain.getSetUnits(), newDomain.getSetUnits())
     index = newDomain.doubleToIndex(values)
     for j in range(len(index)):
        if (index[j] >= 0):
           fld.setSample(index[j], g[i].getSample(j), 0)
  return fld


def writeGridToXls(grid,filename='grid.xls'):
    """Write out the grid data to an excel spreadsheet"""
    GridUtil.writeGridToXls(grid,filename)
    return grid

def exportGridToNetcdf(grid,filename='grid.nc'):
    """Write out the grid data to CF compliant netCDF file"""
    GridUtil.exportGridToNetcdf(grid,filename)
    return grid

def makeTopographyFromField(grid):
    """make a topography out of a grid """
    c = newUnit(grid, "topo", "m")
    return DerivedGridFactory.create2DTopography(grid,c)

def substitute(data, low, high, newValue):
    """change values in data  between low/high to newvalue """
    newData = data.clone();
    if (GridUtil.isTimeSequence(newData)):
        for t in range(newData.getDomainSet().getLength()):
            rangeObject = newData.getSample(t)
            values = rangeObject.getFloats(0);
            for i in range(len(values)):
               for j in range(len(values[0])):
		           if values[i][j]>=low:
		             if values[i][j]<=high:  values[i][j] = newValue;
            rangeObject.setSamples(values,1);
    else:
        rangeObject = newData;
        values = rangeObject.getFloats(0);
        for i in range(len(values)):
          for j in range(len(values[0])):
             if values[i][j]>=low:
                if values[i][j]<=high: values[0][i] = newValue;
        rangeObject.setSamples(values,1);
    return newData;

def maskGrid(grid, mask, value=0,resample=0):
    """mask one grid by the values in the other.  value is the masking value"""
    return DerivedGridFactory.mask(grid, mask, value, resample)

def GP2Z(gp):
    """Convert Geopotential (GP) to Height (Z)"""
    return gp/DerivedGridFactory.GRAVITY


def uvFromWindDir(dir):
    """creating the vector field using the wind direction"""
    u = newName(sin(dir),"U")
    v = newName(cos(dir),"V")
    return makeVector(u,v)

def runave(grid, nave=3, option=0):
    """generate a running average:
    <div class=jython>
    Where:<br>
          grid = grid to average<br>
          nave - number of steps to average over<br>
          option - option for unsmoothed end points<br>
                   0 - set to missing<br>
                   1 - use symmetry<br>
                  -1 - assume cyclic <br>
    </div>
    """
    steps = int(nave)
    opt = int(option)
    return GridMath.timeRunningAverage(grid,steps,opt)

def wgt_runave(grid, wgts, option=0):
    """generate a weighted running average:
    <div class=jython>
    Where:<br>
          grid = grid to average<br>
          wgts - comma separated list of weights<br>
          option - option for unsmoothed end points<br>
                   0 - set to missing<br>
                   1 - use symmetry<br>
                  -1 - assume cyclic <br>
    </div>
    """
    from ucar.unidata.util import Misc
    weights = Misc.parseFloats(wgts)
    opt = int(option)
    return GridMath.timeWeightedRunningAverage(grid,weights,opt)

def lonFlip(grid):
    """ Flip the longitudes in a grid from -180-180 to 0-360 (or vice-versa).
        Only works for cyclic rectilinear grids.
    """
    return GridUtil.lonFlip(grid)

def makeFlowTraj(u,v,w,s,s0):
  """Get the u,v,w and scalar variable s, s0 from a grid.  Return grid trajectroy.
  """
  ff = GridTrajectory.createTrajectoryGrid(u,v,w,s,s0)
  return ff

def make2DFlowTraj(u,v,s,s0):
  """Get the 2D u,v and scalar variable s, s0 from a grid.  Return grid trajectroy.
  """
  ff = GridTrajectory.createTrajectoryGrid(u,v,s,s0)
  return ff