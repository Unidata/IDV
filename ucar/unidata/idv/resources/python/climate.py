""" 
   This is the doc for the climate module.  These functions are useful
   for creating climate features from grids (means, anomalies, standard
   deviations).  Function names are based on NCL function names.
"""

def clmMon(grid):
  """ Create a monthly climatology from a grid of monthly data over a period
      of years. The number of timesteps must be a multiple of 12.
  """
  from visad import VisADException
  from visad import Gridded1DDoubleSet
  from visad import FunctionType
  from visad import FieldImpl
  from visad import RealType
  timeSet = GridUtil.getTimeSet(grid)
  numT = timeSet.getLength()
  if (numT%12 > 0):
    raise VisADException("Number of times must be a multiple of 12")
  numYears = numT/12
  days = [[0,31,59,90,120,151,181,212,243,273,304,334]]
  units = [Util.parseUnit("days since 0001-01-01 00:00")]
  newTimes = Gridded1DDoubleSet(RealType.Time, days, 12, None, units, None)
  climo = FieldImpl(FunctionType(RealType.Time, grid.getSample(0).getType()), newTimes) 
  for month in range (12):
     a = GridMath.applyFunctionOverTime(grid, GridUtil.FUNC_AVERAGE, month, 12, 0)
     climo.setSample(month, a, 0)
  return climo

def calcMonAnom(monthly, ltm, normalize=0):
  """ Calculate the monthly anomaly from a long term mean. 
      The number of timesteps in ltm must be 12 and the monthly
      data must start in January.
  """
  monAnom = monthly.clone()
  months = len(ltm)
  years = int(len(monthly)/months) +1
  for year in range(years):
    for month in range(months):
       index = year*months+month
       if index > len(monthly) - 1:
           break
       diff = sub(monthly[index],ltm[month])
       if normalize != 0:
           diff = sub(diff,xav(diff))
           diff = GridUtil.setParamType(diff, GridUtil.getParamType(monAnom))
       monAnom[index] = diff
  return monAnom
  
def stdMon(grid):
  """ Create monthly standard deviations from a grid of monthly data over a period
      of years. The number of timesteps must be a multiple of 12.
  """
  from visad import VisADException
  from visad import Gridded1DDoubleSet
  from visad import FunctionType
  from visad import FieldImpl
  from visad import RealType
  timeSet = GridUtil.getTimeSet(grid)
  numT = timeSet.getLength()
  if (numT%12 > 0):
    raise VisADException("Number of times must be a multiple of 12")
  numYears = numT/12
  days = [[0,31,59,90,120,151,181,212,243,273,304,334]]
  units = [Util.parseUnit("days since 0001-01-01 00:00")]
  newTimes = Gridded1DDoubleSet(RealType.Time, days, 12, None, units, None)
  stdev = FieldImpl(FunctionType(RealType.Time, grid.getSample(0).getType()), newTimes) 
  for month in range (12):
     a = GridMath.applyFunctionOverTime(grid, GridMath.FUNC_STDEV, month, 12, 0)
     stdev.setSample(month, a, 0)
  return stdev


