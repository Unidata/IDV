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
      The number of timesteps in ltm must be 12
  """
  from visad import VisADException
  monAnom = monthly.clone()
  months = len(ltm)
  if (not months == 12):
    raise VisADException("Number of months in ltm must be a 12")
  years = int(len(monthly)/months) +1
  startMonth = getStartMonth(GridUtil.getTimeSet(monthly))-1
  #print "Start month = " , startMonth
  index = 0
  for year in range(years):
    for month in range(12):
      if index > len(monthly) - 1:
        break
      thisMonth = (startMonth+month)%12
      #print thisMonth
      diff = sub(monthly[index],ltm[thisMonth])
      if normalize != 0:
        diff = sub(diff,xav(diff))
        diff = GridUtil.setParamType(diff, GridUtil.getParamType(monAnom))
      monAnom[index] = diff
      index = index + 1
  return monAnom

def getStartMonth(timeSet):
  """ Get the starting month number (1-12) from a timeset.
  """
  from visad.util import DataUtility as du
  from visad import DateTime
  r = du.getSample(timeSet, 0).getComponent(0)
  dt = DateTime(r)
  month = dt.formattedString("MM",DateTime.getFormatTimeZone())
  return int(month)

def stdMon(grid):
  """ Create monthly standard deviations from a grid of monthly data over 
      a period of years. The number of timesteps must be a multiple of 12.
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

def clmDay(grid, use366=1):
  """ Create a daily climatology from a grid of daily data over a period
      of years. 
  """
  return DerivedGridFactory.createDailyClimatology(grid, use366)

def calcDayAnom(daily, ltm, asPercent=0):
  """ Calculate the daily anomaly from a long term mean. <br>
      grid - daily values <br>
      ltm  - long term mean (climatology) <br>
      asPercent - if 1, return percentage of climatology normal (+/-) <br>
  """
  return DerivedGridFactory.calculateDailyAnomaly(daily, ltm, asPercent)
