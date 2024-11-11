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

def flowVector(field):
  """ Make a vector from flow direction"""
  a=newName(sin(field),"a")
  b=newName(cos(field),"b")
  return DerivedGridFactory.createFlowVectors(a,b)

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

def horizontalAdvection(param, vector):
  """ horizontal advection """
  u = DerivedGridFactory.getUComponent(vector)
  v = DerivedGridFactory.getVComponent(vector)
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

def writeGridToXlsAtPolygon(grid,filename='grid.xls', filename1='point.xgrf'):
    """Write out the grid data to an excel spreadsheet"""
    GridUtil.writeGridAtPolygonToXls(grid,filename, filename1)
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

def substituteMissing(data, newValue):
    """change values in data  between low/high to newvalue """
    from java.lang import Float
    newData = data.clone();
    if (GridUtil.isTimeSequence(newData)):
        for t in range(newData.getDomainSet().getLength()):
            rangeObject = newData.getSample(t)
            values = rangeObject.getFloats(0);
            for i in range(len(values)):
               for j in range(len(values[0])):
		           if Float.isNaN(values[i][j]):
		               values[i][j] = newValue;
            rangeObject.setSamples(values,1);
    else:
        rangeObject = newData;
        values = rangeObject.getFloats(0);
        for i in range(len(values)):
          for j in range(len(values[0])):
             if Float.isNaN(values[i][j]):
                values[i][i] = newValue;
        rangeObject.setSamples(values,1);
    return newData;

def substituteWithMissing(data, missingValue):
    """change values in data  between low/high to newvalue """
    from java.lang import Float
    newData = data.clone();
    if (GridUtil.isTimeSequence(newData)):
        for t in range(newData.getDomainSet().getLength()):
            rangeObject = newData.getSample(t)
            values = rangeObject.getFloats(0);
            for i in range(len(values)):
                for j in range(len(values[0])):
		            if (values[i][j] == missingValue):
		               values[i][j] = float("NaN");
            rangeObject.setSamples(values,1);
    else:
        rangeObject = newData;
        values = rangeObject.getFloats(0);
        for i in range(len(values)):
            for j in range(len(values[0])):
                if (values[i][j] == missingValue):
                    values[i][i] = float("NaN");
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

def setLevel(grid, level, unit):
  return GridUtil.addLevelToGrid(grid, float(level), unit)

def thetaSurface(grid, theta0):
  return DerivedGridFactory.extractGridOverThetaTopoSurface(grid, float(theta0))

def thetaSurfaceA(grid, grid1, theta0):
  return DerivedGridFactory.extractGridOverThetaTopoSurface(grid, grid1, float(theta0))

def thetaSurfaceV(gridt, gridu, gridv, theta0):
  return DerivedGridFactory.extractUVGridOverThetaTopoSurface(gridt, gridu, gridv, float(theta0))

def thetaSurfaceV(gridt, griduv, theta0):
  return DerivedGridFactory.extractVectorGridOverThetaTopoSurface(gridt, griduv, float(theta0))

def thetaSurfaceADV(gridt, griduv, other, theta0):
  return DerivedGridFactory.extractGridADVOverThetaTopoSurface(gridt, griduv, other,float(theta0))

def heatIndex(gridtemp, gridrh):
  return DerivedGridFactory.createHeatIndex(gridtemp, gridrh)

def timeStepAccumulatedPrecip(grid):
  return DerivedGridFactory.timeStepAccumulatedPrecip(grid)

def virtualTemperature(p, t, dp):
  return DerivedGridFactory.createVirtualTemperature(p, t, dp)

def virtualPotentialTemperature(p, t, dp):
  return DerivedGridFactory.createVirtualPotentialTemperature(p, t, dp)

def medianFilter(grid, user_missingValue=None, window_lenx=10, window_leny=10):
  """ calculate median filter, need to replace the missingValue if it is not NaN
  """
  if user_missingValue == None or user_missingValue =="":
    return GridUtil.medianFilter(grid, window_lenx, window_leny)
  else:
    grid0 = substituteWithMissing(grid, missingValue)
    return GridUtil.medianFilter(grid0, window_lenx, window_leny)

def classifier(grid, classifierStr, outFileName):
  """ classifierStr is a string of a set of classifier info with format:
        "low1 high1 value1; low2 high2 value2; low3 high3 value3;..."
  """
  return GridUtil.classifier(grid, classifierStr, outFileName)

def applyFunctionOverGrid2D(grid, function, statThreshold):
  """  apply spatial Max, Min, Average, Percentile over 2D grid:
        function: "max", "max", "average", "percentile"
        statThreshold is string format numerical value
  """
  return GridMath.applyFunctionOverGrid2D(grid, function, statThreshold)

def gridNormalDistribution(grid,user_mean=None,user_std=None):
  """ Returns a grid with values sampled from normal distrubuition(
      user_mean,user_std). user_units can be change units of returned grid.
      This also serves as a template code for creating grids sampled from
      different distributions.
  """
  from visad import FlatField
  import random
  import edu.wisc.ssec.mcidasv.data.hydra.Statistics

  def fillNormal(gridFF,user_mean,user_std):
      mean = user_mean
      std = user_std
      if user_mean == None or user_mean =="":
        statistics = GridMath.statisticsFF(gridFF)
        mean = (statistics.mean()).getValue()
        std = (statistics.standardDeviation()).getValue()
      else:
        mean = float(user_mean)
        std = float(user_std)
      tempFF=FlatField(gridFF.getType(),gridFF.getDomainSet())
      vals=[random.gauss(mean,std) for itm in range(len(gridFF.getFloats()[0]))]
      tempFF.setSamples([vals])
      return tempFF
  if GridUtil.isTimeSequence(grid):
      uniformG=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
          uniformG.setSample(j,fillNormal(grid.getSample(j),user_mean,user_std))
      return uniformG
  else:
      return fillNormal(grid,user_mean,user_std)

def gridUniformDistribution(grid,user_min=None,user_max=None):
  """ Returns a grid with values sampled from uniform distrubuition(
        user_min,user_max). user_units can be change units of returned grid.
        This also serves as a template code for creating grids sampled from
        different distributions.
  """
  from visad import FlatField
  import random
  import edu.wisc.ssec.mcidasv.data.hydra.Statistics

  def fillUniform(gridFF,user_min,user_max):
      import random
      min = user_min
      max = user_max

        #also fun the return by input grid
      if user_min == None or user_min =="":
          statistics = GridMath.statisticsFF(gridFF)
          min = (statistics.min()).getValue()
          max = (statistics.max()).getValue()
      else:
          min = float(user_min)
          max = float(user_max)
      tempFF=FlatField(gridFF.getType(),gridFF.getDomainSet())#put units here
      vals=[random.uniform(min,max) for itm in range(len(gridFF.getFloats()[0]))]
      tempFF.setSamples([vals])
      return tempFF

  if GridUtil.isTimeSequence(grid):
      uniformG=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
            uniformG.setSample(j,fillUniform(grid.getSample(j),user_min,user_max))
      return uniformG
  else:
      return fillUniform(grid,user_min,user_max)

def gridStandardScaler(grid,user_mean=None,user_std=None):
  """ StandardScaler standardizes grid values by removing the mean and scaling to
      variance using statistics on the samples to improve the performance and
      convergence of machine learning models, particularly those sensitive to
      feature scales. It is sensitive to outliers, and the features may scale
      differently from each other in the presence of outliers.
  """
  from visad import FlatField
  import random
  import edu.wisc.ssec.mcidasv.data.hydra.Statistics

  def fillStandardScaler(gridFF,user_mean,user_std):
      import random
      mean = user_mean
      std = user_std

        #also fun the return by input grid
      if user_mean == None or user_mean =="":
          statistics = GridMath.statisticsFF(gridFF)
          mean = (statistics.mean()).getValue()
          std = (statistics.standardDeviation()).getValue()
      else:
          mean = float(user_mean)
          std = float(user_std)
      tempFF=FlatField(gridFF.getType(),gridFF.getDomainSet())#put units here
      values = gridFF.getFloats()[0]
      vals=[(values[itm] - mean)/std for itm in range(len(gridFF.getFloats()[0]))]

      tempFF.setSamples([vals])
      return tempFF

  if GridUtil.isTimeSequence(grid):
      standardScaler=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
            standardScaler.setSample(j,fillStandardScaler(grid.getSample(j),user_mean,user_std))
      return standardScaler
  else:
      return fillStandardScaler(grid,user_mean,user_std)

def gridMinMaxScaler(grid,user_min,user_max):
  """ Rescale the grid values individually to a common range [user_min, user_max] linearly using statistics and
      it is also known as min-max normalization. It doesn’t reduce the effect of outliers, but it linearly scales
      them down into a fixed range, where the largest occurring data point corresponds to the maximum value
      and the smallest one corresponds to the minimum value.
  """
  from visad import FlatField
  import random
  import edu.wisc.ssec.mcidasv.data.hydra.Statistics

  def fillMinMaxScaler(gridFF,user_min,user_max):
      import random

      statistics = GridMath.statisticsFF(gridFF)
      min = (statistics.min()).getValue()
      max = (statistics.max()).getValue()
        #also fun the return by input grid
      if user_min == None or user_min =="":
          user_max = max
          user_min = min
      else:
          user_min = float(user_min)
          user_max = float(user_max)
      tempFF=FlatField(gridFF.getType(),gridFF.getDomainSet())#put units here
      values = gridFF.getFloats()[0]
      vals=[(user_max-user_min)*(values[itm]-min)/(max-min) + user_min for itm in range(len(gridFF.getFloats()[0]))]
      tempFF.setSamples([vals])
      #print "values = " , values[0]
      #print "min = ", min
      #print "man = ", max
      #print "vals = " , vals[0]
      return tempFF

  if GridUtil.isTimeSequence(grid):
      scalerG=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
            scalerG.setSample(j,fillMinMaxScaler(grid.getSample(j),user_min,user_max))
      return scalerG
  else:
      return fillMinMaxScaler(grid,user_min,user_max)

def gridQuantileTransform(grid):
  """ Rescale the grid values individually to a common range [user_min, user_max] linearly using statistics and
      it is also known as min-max normalization
  """
  from visad import FlatField

  def fillQuantileTransform(gridFF):

      tempFF=GridMath.quantileTransformerFF(gridFF)#put units here
      return tempFF

  if GridUtil.isTimeSequence(grid):
      scalerG=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
            scalerG.setSample(j,fillQuantileTransform(grid.getSample(j)))
      return scalerG
  else:
      return fillQuantileTransform(grid)

def gridPowerTransform(grid, user_lambda):
  """ Power transforms are a family of parametric, monotonic transformations that are applied
      to make data more Gaussian-like. This is useful for modeling issues related to
      heteroscedasticity (non-constant variance), or other situations where normality is desired.
      Currently, power_transform supports the Yeo-Johnson transform and Yeo-Johnson supports
      both positive or negative data..
  """
  from visad import FlatField

  def fillPowerTransform(gridFF, user_lambda):
      if user_lambda == None or user_lambda =="":
          data = gridFF.getFloats()[0]
          user_lambda = GridUtil.findOptimalLambda(data, -5, 5, 0.1)

      tempFF=GridMath.powerTransformerFF(gridFF, user_lambda)#put units here
      return tempFF

  if GridUtil.isTimeSequence(grid):
      scalerG=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
            scalerG.setSample(j,fillPowerTransform(grid.getSample(j), user_lambda))
      return scalerG
  else:
      return fillPowerTransform(grid, user_lambda)

def gridRobustScaler(grid):
  """ Rescale the grid values individually to a common range [user_min, user_max] linearly using statistics and
      it is also known as min-max normalization
      This Scaler removes the median and scales the data according to the quantile range
      (defaults to IQR: Interquartile Range). The IQR is the range between the 1st quartile (25th quantile) and
      the 3rd quartile (75th quantile).
      However, outliers can often influence the sample mean / variance in a negative way. In such cases,
      using the median and the interquartile range often give better results.
  """
  from visad import FlatField

  def fillRobustScaler(gridFF):
      tempFF=GridMath.robustScalerFF(gridFF)#put units here
      return tempFF

  if GridUtil.isTimeSequence(grid):
      scalerG=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
            scalerG.setSample(j,fillRobustScaler(grid.getSample(j)))
      return scalerG
  else:
      return fillRobustScaler(grid)

def gridNormalizer(grid, user_norm="Max"):
  """ Normalize samples individually to unit norm.
      Each sample (i.e. each row of the data matrix) with at least one non zero component
      is rescaled independently of other samples so that its norm (l1, l2 or inf) equals one.
  """
  from java.lang import Float
  from visad import FlatField
  import edu.wisc.ssec.mcidasv.data.hydra.Statistics

  def fillNormalizer(gridFF):
        #also fun the return by input grid
      tempFF=FlatField(gridFF.getType(),gridFF.getDomainSet())#put units here
      values = gridFF.getFloats()[0]
      if user_norm == "l1":
          norm = GridMath.calculateL1Norm(values)
          for itm in range(len(gridFF.getFloats()[0])):
              if not Float.isNaN(values[itm]):
                  values[itm] = values[itm]/norm
      elif user_norm == "l2":
          norm = GridMath.calculateL2Norm(values)
          for itm in range(len(gridFF.getFloats()[0])):
              if not Float.isNaN(values[itm]):
                  values[itm] = values[itm]/norm
      else:
          # vals=[(values[itm]-min)/(max-min) for itm in range(len(gridFF.getFloats()[0]))]
          statistics = GridMath.statisticsFF(gridFF)
          min = (statistics.min()).getValue()
          max = (statistics.max()).getValue()
          for itm in range(len(gridFF.getFloats()[0])):
              if not Float.isNaN(values[itm]):
                  values[itm] = (values[itm]-min)/(max-min)

      # tempFF.setSamples([vals])
      #print "values = " , values[0]

      tempFF.setSamples([values])
      #print "min = ", min
      #print "man = ", max
      #print "vals = " , vals[0]
      return tempFF

  if GridUtil.isTimeSequence(grid):
      scalerG=grid.clone()
      for j,time in enumerate(grid.domainEnumeration()):
            scalerG.setSample(j,fillNormalizer(grid.getSample(j)))
      return scalerG
  else:
      return fillNormalizer(grid)