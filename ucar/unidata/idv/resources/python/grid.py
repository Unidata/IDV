

def makeVector(a,b):
  return DerivedGridFactory.createFlowVectors(a,b)

def makeFlowField(a,b,c):
  return DerivedGridFactory.createFlowVectors(a,b,c)


def extractPressureFromNWPGrid(fieldimpl):
  """get pressure coordinate grid from a VisAD FieldImpl 
     (a time series of one or more FlatFields);
     user must be sure input is a suitable FlatField. """
  ff = fieldimpl.getSample(0)
  return DerivedGridFactory.createPressureGridFromDomain(ff)


def extractLatitudeFromNWPGrid(fieldimpl):
  """get the latitude coordinate grid from a 3D VisAD FieldImpl 
     (a time series of one or more FlatFields);
     user must be sure input is a suitable FlatField. """
  ff = DerivedGridFactory.getLatitudeGrid(fieldimpl)
  #ff2 = GridUtil.sliceAtLevel(ff, l000.0)
  return ff



def getNthTimeGrid(fieldimpl, Nth):
  """get Nth grid in time series fieldimpl, a VisAD FieldImpl;
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
  """extract a 2D horizontal slice from a 3D grid at "Level."
     level is a real number; must be appropriate for the grid.
     param fieldimpl is a VisAD FieldImpl which may have
     one or more time steps.  """
  level = float(level)
  ff = sliceAtLevel(fieldimpl, level)
  return ff



def getSliceAtAltitude(fieldimpl, alt, unit="m") :
  """ extract a 2D horizontal slice from a 3D grid at the given altitude
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
   """ wrapper for calculating layer difference """
   return createLayerDifference(grid, top, bottom)

def getAltitude(z):
   """ change units from geopotential meters to meters """
   import ucar.visad.quantities.GeopotentialAltitude as ga
   import ucar.visad.quantities.Gravity as gr
   zUnit = GridUtil.getParamType(z).getRealComponents()[0].getDefaultUnit()
   if zUnit.equals(ga.getGeopotentialMeter()):
      z = z.divide(gr.newReal())
   return z


def windShear(u, v, z, top, bottom):
   """ calculate the wind shear between discrete layers
   shear = sqrt((u(top)-u(bottom))^2 + (v(top)-v(bottom))^2)/zdiff """
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

def horizontalAdvection(param, u, v):
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
  """make a 2D slice from a 3D slice """
  return GridUtil.make2DGridFromSlice(slice)


def averageOverTime(field,makeTimes = 0):
    """Average the values in each time step
    If makeTimes is true then we return a field mapping all of the times
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



def changeRange(d):
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

