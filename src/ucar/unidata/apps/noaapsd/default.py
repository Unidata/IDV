"""
NOAA/ESRL/PSD Jython functions
"""

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

def unmixIntervals(grid,cumTime=6,accum=True):
  """ Unmix fields which are on mixed intervals.  Assumes that every other timestep is
      twice the value of the previous. cumTime is 6 for 3,6 hour mixed.  12 would be
      for 6/12 mixed intervals.
  """
  from ucar.visad.data import CalendarDateTime as cdt
  from java.util import TimeZone as tz
  ugrid = grid.clone()
  timeSet = GridUtil.getTimeSet(grid)
  timeArray = cdt.timeSetToArray(timeSet)
  for time in range(len(timeArray)):
     if (time > 0):
        prevhour = int(timeArray[time-1].formattedString("HH",tz.getTimeZone("GMT")))
        nowhour = int(timeArray[time].formattedString("HH",tz.getTimeZone("GMT")))
        if (nowhour%cumTime == 0 and prevhour%cumTime != 0):
          if (accum):
             tsgrid = sub(grid.getSample(time),grid.getSample(time-1))
          else:
              tsgrid = sub(2*grid.getSample(time),grid.getSample(time-1))
          ugrid.setSample(time,tsgrid)         
  return ugrid

def makeIR(OLR):
  """ Make a simulated IR Temperature field from an OLR field using 
      the Steffan-Boltzmann law inversely
  """
  import math
  a = OLR/5.670367e-8
  b = pow(a,.25)
  return newUnit(b,"IRtemp","K")
  return b
