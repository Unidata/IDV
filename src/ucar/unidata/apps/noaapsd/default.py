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

