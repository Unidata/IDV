from visad.python.JPythonMethods import *
# A collection of Utilities for Mapes IDV Collection
#Author: Suvarchal Kumar Cheedela, suvarchal.kumar@rsmas.miami.edu
############################TIME UTILS############################################
def getSamplesAtTimes(grid,year=None,season=None,mon=None,day=None,hour=None,min=None,sec=None,ms=None):
  """ Samples a grid at specified time periods, multiple arguments can be used in complex sampling
      eg.., using hour = 5 would return all samples corresponding  to 5 am, further specifing year = 2008
      would give samples at 5am in year 2008
  """
  from visad import RealType
  from visad import Gridded1DDoubleSet
  from visad import FieldImpl
  from visad import FunctionType
  from visad import VisADException
  if (str(mon)!="None" and str(season)!="None"):
     raise VisADException("One of Month or Season can be used, not both")
  timeSet=GridUtil.getTimeSet(grid)
  indices=getSampleTimeIndices(grid,year,season,mon,day,hour,min,sec,ms)
  timevals=timeSet.getSamples()[0]
  subsetTimeValues=[timevals[i] for i in indices]
  newTimes=Gridded1DDoubleSet(RealType.Time,[subsetTimeValues],len(subsetTimeValues),None,timeSet.getSetUnits(),None)
  subsetgrid = FieldImpl(FunctionType(RealType.Time, grid.getSample(0).getType()), newTimes)
  for i in range(len(subsetTimeValues)):
     subsetgrid.setSample(i,grid[indices[i]])
  return subsetgrid
def getSampleTimeIndices(grid,year=None,season=None,mon=None,day=None,hour=None,min=None,sec=None,ms=None):
  """ A Helper function to get indices a grid at specified time periods, multiple arguments can be used in
      complex sampling. This function returns list of indices in grid.
  """
  from visad import VisADException
  searchformat=""
  searchstring=""
  if (str(mon)!="None" and str(season)!="None"):
     raise VisADException("One of Month or Season can be used, not both")
  if (str(year)!="None" and len(str(year))==4):
    searchformat=searchformat+"yyyy"
    searchstring=searchstring+str(year)
  if (str(mon)!="None" and int(mon) in ([range(1,13)]) and len(str(mon))<=2):
    searchformat=searchformat+"MM"
    if (len(str(mon))<2):
      searchstring=searchstring+"0"+str(mon)
    else:
      searchstring=searchstring+str(mon)

  if (str(day)!="None"):
    searchformat=searchformat+"dd"
    searchstring=searchstring+str(day)
  if (str(hour)!="None"):
    searchformat=searchformat+"HH"
    searchstring=searchstring+str(hour)
  if (str(min)!="None"):
    searchformat=searchformat+"mm"
    searchstring=searchstring+str(min)
  if (str(sec)!="None"):
    searchformat=searchformat+"ss"
    searchstring=searchstring+str(sec)
  if (str(ms)!="None"):
    searchformat=searchformat+"ms"
    searchstring=searchstring+str(ms)

  if (str(season)!="None"):
    seasons=("djf","jfm","mam","jja","son","ond","jjas")
    seasmons=((12,1,2),(1,2,3),(3,4,5),(6,7,8),(9,10,11),(10,11,12),(6,7,8,9))
    if (str(season).lower() in seasons):
      montimes=getSampleTimesInFormat(grid,"MM")
      alltimes=getSampleTimesInFormat(grid,searchformat.strip())
      seasonlist=[list(seasons)]
      seasonsearch=seasmons[list(seasons).index(str(season).lower())]
      matchindices=[i for i,t,m in zip(range(len(alltimes)),alltimes,montimes) if t==searchstring and int(m) in seasonsearch]
    else:
      raise VisADException("Season "+str(season)+" not found")
  else:
     alltimes=getSampleTimesInFormat(grid,searchformat.strip())
     matchindices=[i for i,t in enumerate(alltimes) if t==searchstring]
  return matchindices
def getSampleTimesInFormat(grid,timeformat,timezone="UTC",outformat="string"):
  """ A Helper function to return times of a grid in specified format as a list.
  """
  from ucar.visad.data import CalendarDateTime
  from visad import DateTime
  from ucar.visad.UtcDate import formatUtcDate
  from visad import VisADException
  from java.util import TimeZone
  dateTimes = CalendarDateTime.timeSetToArray(GridUtil.getTimeSet(grid))
  TIMEZONE=TimeZone.getTimeZone(timezone)
  temp=[]
  for i in range(grid.getDomainSet().getLength()):
    if (str(outformat).lower() in ("string","str")):
      temp.append(str(dateTimes[i].formattedString(timeformat,TIMEZONE)))
    elif (str(outformat).lower() in ("float","flt")):
      temp.append(float(dateTimes[i].formattedString(timeformat,TIMEZONE)))
    elif (str(outformat).lower() in ("int","integer")):
      temp.append(Integer(dateTimes[i].formattedString(timeformat,TIMEZONE)))
    else:
      raise VisADException("Unrecognized output format")
  return temp
def createTimeMeans(grid,meanType="None"):
  """ Create time mean of a grid at periods specified by type.
      meanType can be yearly, monthly, daily, hourly, minutes, seconds
  """
  from visad import Real
  from visad import Gridded1DDoubleSet
  from visad import FieldImpl
  from visad import FunctionType
  from visad import RealType
  from visad import VisADException

  if (str(meanType).lower() in ("year","yr","years","yearly")):
     searchFormat="yyyy"
  elif (str(meanType).lower() in ("mon","month","months","monthly")):
     searchFormat="MM"
  elif (str(meanType).lower() in ("day","d","days","daily")):
     searchFormat="dd"
  elif (str(meanType).lower() in ("hr","hour","hours","hourly")):
     searchFormat="hh"
  elif (str(meanType).lower() in ("m","min","minute","minutes","minutely")):
     searchFormat="mm"
  elif (str(meanType).lower() in ("s","sec","second","seconds")):
     searchFormat="ss"
  else:
       raise VisADException("Unrecognized time mean type, use yearly or monthly etc")
  alltimes=getSampleTimesInFormat(grid,searchFormat)
  timeSet=GridUtil.getTimeSet(grid)
  timeset=GridUtil.getTimeSet(grid).getSamples()[0]
  timevalues=[i for i in timeset]
  oldtime=alltimes[0]
  temptime=0
  count=0
  newtimelist=[]
  for currt,tv,i in zip(alltimes,timevalues,range(len(alltimes))):
  #values are always accumulated next time
     if currt==oldtime:
      #takes care of multiple times,first time,last time
      temptime=temptime+tv
      count=count+1
      if(i==(len(alltimes)-1)):
        newtimeval=temptime/count
        newtimelist.append(newtimeval)
     else:
     #prev values are accumulated join to list
      newtimeval=temptime/count
      newtimelist.append(newtimeval)
      count=1
      temptime=tv
      oldtime=currt
      if(i==(len(alltimes)-1)):
        newtimelist.append(temptime)
  #create new time set
  newTimes=Gridded1DDoubleSet(RealType.Time,[newtimelist],len(newtimelist),None,timeSet.getSetUnits(),None)
  newdatalist=FieldImpl(FunctionType(RealType.Time, grid.getSample(0).getType()), newTimes)
  timindices=range(len(newtimelist))
  oldtime=alltimes[0]
  tempdata=grid.getSample(0).multiply(Real(0.0))
  count=0
  newind=0
  for currt,i in zip(alltimes,range(len(alltimes))):
  #values are always accumulated next time
     if currt==oldtime:
      #takes care of multiple times,first time,last time
      tempdata=tempdata.add(grid.getSample(i))
      count=count+1
      if(i==(len(alltimes)-1)):
        newdatalist.setSample(newind,tempdata.divide(Real(count)))
        newind=newind+1
     else:
     #prev values are accumulated join to list
      newdatalist.setSample(newind,tempdata.divide(Real(count)))
      newind=newind+1
      count=1
      tempdata=grid.getSample(i)
      oldtime=currt
      if(i==(len(alltimes)-1)):
        newdatalist.setSample(newind,tempdata.divide(Real(count)))
  newParamName="Time Mean "+str(Util.cleanTypeName(GridUtil.getParamType(grid)))
  return newName(newdatalist,newParamName)
def ddt(grid,timegradunit):
  """ compute tendency (time derivative) using forward difference,
      units of returned grid are units of grid per timegradient unit
      timegradient unit can be month, day, hour, minute, seconds

  """
  from visad import Real
  from visad import FunctionType
  from visad import FieldImpl
  from visad import RealType
  from visad import Gridded1DDoubleSet
  from ucar.visad.data import CalendarDateTime
  from visad import CommonUnit
  from visad import VisADException
  if (GridUtil.isTimeSequence(grid)==1):
    newTimeValues= []
    timediffs=[]
    ts = GridUtil.getTimeSet(grid)
    if (str(timegradunit).lower() in ("mon","month","months")):
       timefactor=86400.0*30
       timegradunit="month"
    elif (str(timegradunit).lower() in ("day","d","days")):
       timefactor=86400.0
       timegradunit="day"
    elif (str(timegradunit).lower() in ("hr","hour","hours")):
       timefactor=3600.0
       timegradunit="hr"
    elif (str(timegradunit).lower() in ("m","min","minute","minutes")):
       timefactor=60.0
       timegradunit="min"
    elif (str(timegradunit).lower() in ("s","sec","second","seconds")):
       timefactor=1.0
       timegradunit="s"
    else:
       raise VisADException("Requested time gradient unit is ambigious,use month,day,hour etc")
    for i in range(grid.getDomainSet().getLength()-1):
       newTimeValues.append((ts[i].getValue()+ts[i+1].getValue())/2)
       prevtime=float(ts[i].getValue(CommonUnit.secondsSinceTheEpoch))
       nexttime=float(ts[i+1].getValue(CommonUnit.secondsSinceTheEpoch))
       timediffs.append((nexttime-prevtime)/timefactor)
    newTimes=Gridded1DDoubleSet(RealType.Time,[newTimeValues],len(newTimeValues),None,ts.getSetUnits(),None)
    ddtgrid = FieldImpl(FunctionType(RealType.Time, grid.getSample(0).getType()), newTimes)
    for i in range(grid.getDomainSet().getLength()-1):
       diff=(grid.getSample(i+1)-grid.getSample(i)).divide(Real(timediffs[i]))
       ddtgrid.setSample(i,diff)
    unitname=str(GridUtil.getParamType(grid).getComponent(0).getDefaultUnit())
    print("["+unitname+"]/"+str(timegradunit))
    newunit = Util.parseUnit("("+unitname+")/"+str(timegradunit))
    newType = Util.makeRealType("ddt of "+getVarName(grid), newunit)
  else:
    raise VisADException("Well, this data is not a time series, hard to do a time derivative!")
  return GridUtil.setParamType(ddtgrid,newType,0)

def anomalyFromTimeMeans(grid,meanType="None"):
  """ Returns deviation from time means, timemean can be monthly, daily etc..
      eg.., meanType="day" will return deviation of each step from its
      corresponding daily mean.
  """
  from visad import Real
  from visad import Gridded1DDoubleSet
  from visad import FieldImpl
  from visad import FunctionType
  from visad import RealType
  from visad.Data import NEAREST_NEIGHBOR
  from visad.Data import NO_ERRORS
  from visad import VisADException
  timeMean=createTimeMeans(grid,meanType)
  grid.subtract(timeMean)
  if (str(meanType).lower() in ("mon","month","months","montly")):
     searchFormat="MM"
  elif (str(meanType).lower() in ("day","d","days","daily")):
     searchFormat="dd"
  elif (str(meanType).lower() in ("hr","hour","hours","hourly")):
     searchFormat="hh"
  elif (str(meanType).lower() in ("m","min","minute","minutes","minutely")):
     searchFormat="mm"
  elif (str(meanType).lower() in ("s","sec","second","seconds")):
     searchFormat="ss"
  else:
       raise VisADException("Unrecognized time mean type, use yearly or monthly etc")
  return grid.subtract(timeMean,NEAREST_NEIGHBOR,NO_ERRORS)
def getTimeDict(grid):
  """ A helper function to return timestamps of grid as dictionary of years, months etc.
  """
  from ucar.visad.data import CalendarDateTime
  from visad import DateTime
  from ucar.visad.UtcDate import formatUtcDate
  if (GridUtil.isTimeSequence(grid)==1):
    dateTimes = CalendarDateTime.timeSetToArray(grid.getDomainSet())
    YYYY=[]
    MM=[]
    dd=[]
    DD=[]
    mm=[]
    hh=[]
    ss=[]
    YYYYMMdd=[]
    HHmmss=[]
    for i in range(ds.getDomainSet().getLength()):
      print formatUtcDate(dateTimes[i],"DD",DateTime.DEFAULT_TIMEZONE)
      YYYY.append(str(dateTimes[i].formattedString("yyyy",DateTime.DEFAULT_TIMEZONE)))
      MM.append(str(dateTimes[i].formattedString("MM",DateTime.DEFAULT_TIMEZONE)))
      dd.append(str(dateTimes[i].formattedString("dd",DateTime.DEFAULT_TIMEZONE)))
      DD.append(str(dateTimes[i].formattedString("DD",DateTime.DEFAULT_TIMEZONE)))
      hh.append(str(dateTimes[i].formattedString("HH",DateTime.DEFAULT_TIMEZONE)))
      mm.append(str(dateTimes[i].formattedString("mm",DateTime.DEFAULT_TIMEZONE)))
      ss.append(str(dateTimes[i].formattedString("ss",DateTime.DEFAULT_TIMEZONE)))
      YYYYMMdd.append(str(dateTimes[i].formattedString("YYYYMMdd",DateTime.DEFAULT_TIMEZONE)))
      HHmmss.append(str(dateTimes[i].formattedString("HHmmss",DateTime.DEFAULT_TIMEZONE)))
    timeDict=dict([('YYYYMMdd',YYYYMMdd),('HHmmss',HHmmss),('YYYY', YYYY),('MM',MM),('dd',dd),('DD',DD),('mm',mm),('ss',ss)])
  else:
    raise VisADException("This grid is not a time sequence")
  return timeDict


def setValuestoGridAverage(variable,avgvariable):
  """ Set all values at each grid in a grid by spatial average,
      currently the average is not area weighted
  """
  from ucar.unidata.util.Misc import getAverage
  ts=GridUtil.getTimeSet(variable)
  newGrid=variable.clone()
  for i in range(ts.getLength()):
    avg=getAverage(avgvariable.getSample(i).getFloats()[0])
    newGrid.setSample(i,replace(variable.getSample(i),avg))
  return newGrid

def correlationwith1d(variable,variable1d):
  """ Computes time correlation at each grid point with 1d variable supplied.
  """
  yvar=setValuestoGridAverage(variable,variable1d)
  corr1d=correlation(variable,yvar)
  return corr1d

def correlation(xvar,yvar):
  """ Computes time correlation at each grid point in xvar with corresponding grid
      in yvar.
  """
  if(GridUtil.isTimeSequence(xvar) and GridUtil.isTimeSequence(yvar) and GridUtil.getTimeSet(xvar).getLength() == GridUtil.getTimeSet(yvar).getLength()):
    xavg=averageOverTime(xvar,makeTimes=0)
    yavg=averageOverTime(yvar,makeTimes=0)
    xdev=xvar-xavg
    ydev=yvar-yavg
    xydevsumbyn=sumOverTime(xdev.multiply(ydev))/(GridUtil.getTimeSet(xdev).getLength()-1)
    xstddev=sumOverTime(xdev**2,makeTimes=0)/(GridUtil.getTimeSet(xdev).getLength()-1)
    ystddev=sumOverTime(ydev**2,makeTimes=0)/(GridUtil.getTimeSet(ydev).getLength()-1)
  else:
    raise VisADException("Number of timesteps for correlation should be same")
  return noUnit(xydevsumbyn)/noUnit((xstddev**0.5)*(ystddev**0.5))
#######################################AREA UTILS###################################################
def areaWeights(grid):
  """ Computes area weights of a grid and returns the grid with
      weights at each grid.
  """
  from visad import Real
  areaW=createAreaField(grid)
  areaSum=sum(areaW.getValues()[0])
  return areaW.divide(Real(areaSum))
def xyAreaAverage(grid):
  """ Computes Area Average of a grid and returns a grid with area
      averaged value at all grid points.
  """
  oldtype=GridUtil.getParamType(grid)
  xyAavg=xsum(ysum(grid*areaWeights(grid)))
  return GridUtil.setParamType(xyAavg,oldtype,0)
def deviationXY(grid):
  """ Computes deviation from grid grid area average value of a grid
      and returns a grid with deviation from the area averaged value.
  """
  return sub(grid,xyAreaAverage(grid))
def anomalyFromTimeMeans(grid):
  """ Computes deviation from time mean at each grid point.
  """
  avggrid=averageOverTime(grid,1)
  return  grid.subtract(avggrid)
def deviationXYT(grid):
  """ Computes deviation from time and spatial mean at each grid point.
  """
  return deviationXY(anomalyFromTimeMeans(grid))
def computeGridAreaAverage(variable):
  from ucar.unidata.util.Misc import getAverage
  from visad import Real
  #print GridUtil.isTimeSequence(variable)
  areaW=createAreaField(variable)
  sumareaW=sum(areaW.getValues()[0])
  areaW=areaW.divide(Real(sumareaW))
  test=variable.getSample(0).multiply(areaW)
  aavg=sum(test.getValues()[0])/len(test.getValues()[0])
  return aavg
def rebin(grid,newGrid):
  """ Rebin or regrid a grid based on coordinates of newGrid using bilinear
      interpolation
  """
  from visad import Data
  return GridUtil.resampleGrid(grid,GridUtil.getSpatialDomain(newGrid),Data.WEIGHTED_AVERAGE)

################################VERTICAL UTILS###########################################
def getLevels(grid):
  """ A helper function to get levels values inside a grid as a list.
  """
  from visad import VisADException
  if GridUtil.is3D(GridUtil.getSpatialDomain(grid)):
     leveldim=GridUtil.getSpatialDomain(grid).getManifoldDimension()
     levels=remove_duplicates(GridUtil.getSpatialDomain(grid).getSamples()[leveldim-1])
  else:
     raise VisADException("No Vertical Levels Found")
  return levels
def getAbsCDiff(levels):
  from visad import VisADException
  cdiff=levels[:]
  for i,lev in zip(range(len(levels)),levels):
     if i==0 :
         cdiff[i]=abs(levels[i+1]-levels[i])/1.0
     else:
         if i==len(levels)-1:
             cdiff[i]=abs(levels[i-1]-levels[i])/1.0
         else:
             cdiff[i]=abs(levels[i+1]-levels[i-1])/2.0
  return cdiff
def verticalWeightedAvg(grid):
  """ Computes a vertical coordinate weighted average of a 3D grid.
  """
  from visad import Real
  levels=getLevels(grid)
  dlevels=getAbsCDiff(levels)
  vavg=GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid,levels[len(levels)-1])).multiply(Real(0.0))
  for level,dlevel in zip(levels,dlevels):
    vavg=vavg+GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, float(level)),0).multiply(Real(dlevel/sum(dlevels)))
  return vavg
def verticalIntegral(grid):
  """ Computes a vertical coordinate integral of a 3D grid.
  """
  from visad import Real
  levels=getLevels(grid)
  dlevels=getAbsCDiff(levels)
  vavg=GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid,levels[len(levels)-1])).multiply(Real(0.0))
  for level,dlevel in zip(levels,dlevels):
    vavg=vavg+GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, float(level)),0).multiply(Real(dlevel))
  return vavg

def pverticalIntegral(grid):
  """ Computes a vertical coordinate integral of a 3D grid/gravity
     integral( grid dp/g)
  """
  from visad import Real
  levels=getLevels(grid)
  dlevels=getAbsCDiff(levels)
  vavg=GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid,levels[len(levels)-1])).multiply(Real(0.0))
  for level,dlevel in zip(levels,dlevels):
    vavg=vavg+GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, float(level)),0).multiply(Real(dlevel/9.8))
  return vavg

def ddz(grid):
  """ Computes a vertical coordinate derivative of grid specifed.
  """
  from visad import VisADException
  #doesnt work well to raise exception for case when one level is present,
  #in that case does derivative for longitude
  if (GridUtil.is3D(GridUtil.getSpatialDomain(grid))):
    leveldim=GridUtil.getSpatialDomain(grid).getManifoldDimension()
  else:
    raise VisADException("Not a 3D Spatial Grid")
  return GridMath.partial(grid,(leveldim-1))

def smooth3d(grid,smooth_fn,smooth_val=None):
    """ Returns a smoothend field given IDV smoothers like "CIRC","RECT","GWFS"
        and corresponding smoothing values.
        For smoothers like SMS9 no smoothing value is required.
        Returns original field if function doesnt exist or invalid smoothing value.
    """
    from visad import FlatField
    smooth_fn=str(smooth_fn).upper()
    if smooth_val:
        smooth_val=int(smooth_val)
    def smoothFF(grid_sample,smooth_fn,smooth_val=None):
        from visad import FlatField
        levels=getLevels(grid_sample)
        tempFF=FlatField(grid_sample.getType(),grid_sample.getDomainSet())

        dims=range(len(grid_sample.getDomainSet().getLengths()))
        leveldim=GridUtil.getSpatialDomain(grid_sample).getManifoldDimension()
        dims.pop(leveldim-1)
        latlonlen=1
        for i in dims:
            latlonlen=latlonlen*len(set(grid_sample.getDomainSet().getDoubles()[i]))
        vals=grid_sample.getValues()[0]

        levsF=grid_sample.getDomainSet().getDoubles()[2] #must be manifold dimension
        for level in levels:
            levslice=GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid_sample,level))
            if smooth_val:
                smooth_levslice=GridUtil.smooth(levslice,smooth_fn,int(smooth_val))
            else:
                smooth_levslice=GridUtil.smooth(levslice,smooth_fn)
            ind=levsF.index(level)
            vals[ind:ind+latlonlen]=smooth_levslice.getValues()[0]
        tempFF.setSamples([vals])
        return tempFF
    if len(GridUtil.getSpatialDomain(grid).getLengths())<3:
        if smooth_val:
            return GridUtil.smooth(grid,smooth_fn,smooth_val)
        else:
            return GridUtil.smooth(grid,smooth_fn)
    elif GridUtil.isTimeSequence(grid):
        tempFI=grid.clone()
        for i in range(len(grid)):
             tempFI.setSample(i,smoothFF(grid.getSample(i),smooth_fn,smooth_val))
        return tempFI
    else:
        return smoothFF(grid,smooth_fn,smooth_val)
class Interpolate(object):
     def __init__(self, x_list, y_list,Strict=False):
        if Strict==0: #and any([y - x <= 0 for x, y in zip(x_list, x_list[1:])]):
            [x_list,y_list]=self.find_monotonic_segment(x_list,y_list)
        elif any([y - x <= 0 for x, y in zip(x_list, x_list[1:])]):
            x_list=[float('NaN')]
            y_list=[float('NaN')]
        x_list = self.x_list = map(float, x_list)
        y_list = self.y_list = map(float, y_list)
        intervals = zip(x_list, x_list[1:], y_list, y_list[1:])
        self.slopes = [(y2 - y1)/(x2 - x1) for x1, x2, y1, y2 in intervals]
     def find_monotonic_segment(self,x_list,y_list):
        if any([y - x <= 0 for x, y in zip(x_list, x_list[1:])]):
           for i in range(len(x_list)-1):
              if x_list[i] > x_list[i+1]:
                  return self.find_monotonic_segment(x_list[i+1:],y_list[i+1:])
        else:
           return [x_list,y_list]
     def __call__(self, x):
        from bisect import bisect_left
        if x <= self.x_list[0]:
           return float("NaN")
        elif x >= self.x_list[-1]:
           return float("NaN")
        else:
           i = bisect_left(self.x_list, x) - 1
           return self.y_list[i] + self.slopes[i] * (x - self.x_list[i])
###############################MISC UTILS###############################################
def createNewUnit(field,unit,multiplyfactor=1.0):
  """ creates a new unit that cannot be changed by IDV, eg..change units of
      precipitation from kg/m2s-1 to mm/day
  """
  from visad import BaseUnit
  BaseUnit.addBaseUnit(unit,unit)
  rt = GridUtil.getParamType(field).getRealComponents()[0]
  newtype= Util.makeRealType(rt.getName(),BaseUnit.unitNameToUnit(unit) )
  field=field*multiplyfactor
  return GridUtil.setParamType(field,newtype,0)

def makeTimeComposite(variable,avgvariable,minvalue,maxvalue):
  """ Make a time composite of grid supplied(variable) between min max ranges
      of a 1d avg variable supplied.
  """
  from visad import FunctionType
  from visad import FieldImpl
  from visad import RealType
  from visad import Gridded1DDoubleSet
  from visad import VisADException
  from ucar.unidata.util.Misc import getAverage
  timeSet = GridUtil.getTimeSet(avgvariable)
  newTimeIndexList = java.util.ArrayList();
  newTimeValues= []
  for i in range(timeSet.getLength()):
    avg=getAverage(avgvariable.getSample(i).getFloats()[0])
    if  minvalue < avg <=maxvalue:
      newTimeIndexList.add(Integer(i))
      newTimeValues.append(timeSet[i].getValue())
  print(len(newTimeIndexList))
  if (len(newTimeIndexList) <  1):
    raise VisADException("No Matches found to make a time composite")
  newTimes=Gridded1DDoubleSet(RealType.Time,[newTimeValues],len(newTimeValues),None,timeSet.getSetUnits(),None)
  compvariable = FieldImpl(FunctionType(RealType.Time, variable.getSample(0).getType()), newTimes)
  for i in range(len(newTimeValues)):
     compvariable.setSample(i,variable[newTimeIndexList[i]])
  return compvariable


def makeTimeCompositeWindow(avgvariable,variable,minvalue,maxvalue,minwindow,maxwindow):
  """ Make a time composite of grid supplied(variable) between min max ranges and time windows
      of a 1d avg variable supplied.
  """
  from visad import FunctionType
  from visad import FieldImpl
  from visad import RealType
  from visad import Gridded1DDoubleSet
  from visad import VisADException
  from ucar.unidata.util.Misc import getAverage
  timeSet = GridUtil.getTimeSet(avgvariable)
  newTimeIndexList = java.util.ArrayList();
  newTimeValues= []
  for i in range(timeSet.getLength()):
    avg=getAverage(avgvariable.getSample(i).getFloats()[0])
    if  minvalue < avg <=maxvalue:
      for j in range(minwindow,0,-1):
        if (len(newTimeValues)==0):
            prevTime=0
        else:
            prevTime=newTimeValues[-1]
        if (i-j >=0 and timeSet[i-j].getValue() > prevTime):
          newTimeIndexList.add(Integer(i-j))
          newTimeValues.append(timeSet[i-j].getValue())
      newTimeIndexList.add(Integer(i))
      newTimeValues.append(timeSet[i].getValue())
      for j in range(1,maxwindow+1):
        if (i+j < timeSet.getLength()):
          newTimeIndexList.add(Integer(i+j))
          newTimeValues.append(timeSet[i+j].getValue())
  if (len(newTimeIndexList) <  1):
    raise VisADException("No Matches found to make a time composite")
  newTimes=Gridded1DDoubleSet(RealType.Time,[newTimeValues],len(newTimeValues),None,timeSet.getSetUnits(),None)
  compvariable = FieldImpl(FunctionType(RealType.Time, variable.getSample(0).getType()), newTimes)
  for i in range(len(newTimeValues)):
     compvariable.setSample(i,variable[newTimeIndexList[i]])
  return compvariable
def remove_duplicates(values):
  """ A helper function to remove dumplicates, from a list, not necessary
      easier to change list to a set and backwards.
  """
  output = []
  seen = set()
  for value in values:
     if value not in seen:
       output.append(value)
       seen.add(value)
  return output
def isEnsembleGrid(grid):
  """ A helper function to check if grid is ensemble type or not
       Checks both (time,ensemble) and(ensemble...) grids
  """
  #GridUtil.getSequenceType(grid).toString().lower()=="ensemble"
  from visad import RealType
  enscheck1=(Util.getDomainSet(grid).getType().getDomain().getComponent(0).toString().lower()=="ensemble")
  enscheck2=(Util.getDomainSet(grid.getSample(0)).getType().getDomain().getComponent(0).toString().lower()=="ensemble")
  return (enscheck1 or enscheck2)
def getVarName(grid):
  """ A helper function to get raw variable name in side a grid"""
  return str(Util.cleanTypeName(GridUtil.getParamType(grid).getComponent(0).getName()))
def getRawTimes(grid):
  """ A helper function to get all times inside a grid, returns a list of times
  """
  times=[]
  for i in range(grid.getDomainSet().getLength()):
    times.append(grid.getDomainSet()[i].getValue())
  return times
def getSimilarInd(inparray,value):
  """ A helper function to get all indices of an array where it matches given value
  """
  outind=[i for i,j in enumerate(inparray) if j==value]
  return outind
############################CDO###################################
def cdo(variable,user_cdo_options):
  """ Does a cdo operation on grid supplied and returns a grid.
      The function outsources the grid to CDOs (code.zmaw.de/cdo) user option decides what
      operation can be done.
      evalues in cdo as cdo user_cdo_options variable_written_to_temp_file.nc output_temp_file.nc
  """
  import java
  from java.util import Random
  import sys
  import commands
  from visad import VisADException
  rand = Random()

  file_prefix=rand.nextInt(9999)
  file_inp=str(file_prefix)+"_inp.nc"

  exportGridToNetcdf(variable,file_inp)

  timeunit=GridUtil.getTimeSet(variable).getSetUnits()[0]
  if (str(timeunit).split()[0]=="s"):
    newtimeunit=str(timeunit).replace("s","seconds",1)
    nco_status=commands.getstatusoutput("ncatted -a units,time,m,c,"+"\""+newtimeunit+"\""+" "+file_inp)
    if (nco_status[0]):
        raise VisADException(nco_status[1])

  file_out=str(file_prefix)+"_out.nc"
  cdo_status=commands.getstatusoutput("cdo "+user_cdo_options+" "+file_inp+" "+file_out)
  if (cdo_status[0]):
    raise VisADException(cdo_status[1])

  dstemp = makeDataSource(file_out)
  varout=getData(dstemp.getName())
  return varout
def cdo_bandpass(variable,minday,maxday):
   """ Do a band pass filter in time using CDOs and FFT. Before the filter
       is applied data is detrended and feb 29, if exists will be deleted.
       minday and maxday argument needs to be in units of days.
       Note: Also needs ncatted from NCO's also to be on your path.
   """
   from visad import Real
   if (float(minday) > float(maxday) ):
     temp=float(minday)
     minday=float(maxday)
     maxday=temp
   freqmin=float(365)/float(maxday)
   freqmax=float(365)/float(minday)
   user_options="bandpass,"+str(freqmin)+","+str(freqmax)+" -del29feb -detrend"
   filteredvariable=cdo(variable,user_options)
   return filteredvariable
def cdo_lowpass(variable,maxday):
   """ Do a low pass filter in time using CDOs.Before the filter
       is applied data is detrended and feb 29, if exists will be deleted.
       maxday argument needs to be in units of days.
       Note: Also needs ncatted from NCO's also to be on your path.
   """
   from visad import Real
   freqmin=float(365)/float(maxday)
   user_options="lowpass,"+str(freqmin)+" -del29feb -detrend"
   filteredvariable=cdo(variable,user_options)
   return filteredvariable
def cdo_highpass(variable,minday):
   """ Do a high pass filter in time using CDOs.Before the filter
       is applied data is detrended and feb 29, if exists will be deleted.
       maxday argument needs to be in units of days.
       Note: Also needs ncatted from NCO's also to be on your path.
   """
   from visad import Real
   freqmax=float(365)/float(minday)
   user_options="highpass,"+str(freqmax)+" -del29feb -detrend"
   filteredvariable=cdo(variable,user_options)
   return filteredvariable
def cdo_timecor(variable1,variable2):
   """ Computes a correlation in time at each grid point using CDOs.
       Note: Also needs ncatted from NCO's also to be on your path.
   """
   #resampleGrid(variable1,variable2)
   corr=cdo2(variable1,variable2,"timcor")
   return corr
def cdo_timecovar(variable1,variable2):
   """ Computes a covariance in time at each grid point using CDOs.
       Note: Also needs ncatted from NCO's also to be on your path.
   """
   #resampleGrid()
   corr=cdo2(variable1,variable2,"timcovar")
   return corr
def cdoSubGrid(variable,user_nlon,user_nlat):
  """ Computes a subgrid difference between a fine resolution and
      coarsened resolution grid (from regriding the
      original grid to user_nlon and user_nlat) using CDOs.Works
      best for regular grids.
      Note: Also needs ncatted from NCO's also to be on your path.
  """
  import java
  from java.util import Random
  import sys
  import commands
  import os
  try:
  	os.remove(idv.getObjectStore().getJythonCacheDir()+"/Lib/threading.py")
  except:
	pass
  from visad import VisADException
  rand = Random()

  file_prefix=rand.nextInt(9999)
  file_inp=str(file_prefix)+"_inp.nc"

  exportGridToNetcdf(variable,file_inp)

  timeunit=GridUtil.getTimeSet(variable).getSetUnits()[0]
  if (str(timeunit).split()[0]=="s"):
    newtimeunit=str(timeunit).replace("s","seconds",1)
    nco_status=commands.getstatusoutput("ncatted -a units,time,m,c,"+"\""+newtimeunit+"\""+" "+file_inp)
    if (nco_status[0]):
        raise VisADException(nco_status[1])

  file_out=str(file_prefix)+"_out.nc"
  cdo_command="cdo sub "+file_inp+" -remapnn,"+file_inp+" -remapcon,r"+user_nlon+"x"+user_nlat+" "+file_inp+" "+file_out
  cdo_status=commands.getstatusoutput(cdo_command)
  if (cdo_status[0]):
    raise VisADException(cdo_status[1])

  dstemp = makeDataSource(file_out)
  varout=getData(dstemp.getName())
  return varout
##################################################################################################################
def MSE(gridA,gridB):
  """ A function to calculate mean square error or difference between two  grids
  """
  diff=gridA.subtract(gridB)
  mse=diff.multiply(diff)
  return mse
def MAE(gridA,gridB):
  """ A function to calculate mean absolute error or difference between two grids
  """
  gridC=sampleAtTimesofGridB(gridA,gridB)
  diff=gridA.subtract(gridC)
  return diff.abs()
def subsetGridTimes(gridA,gridB):
  """  A function to subset gridA by times of gridB.
       duplicate of another function subsetAtTimesofB
  """
  timeSet=GridUtil.getTimeSet(gridB)
  indices=getSampleTimeIndices(grid,year,season,mon,day,hour,min,sec,ms)
  timevals=timeSet.getSamples()[0]
  subsetTimeValues=[timevals[i] for i in indices]
  newTimes=Gridded1DDoubleSet(RealType.Time,[subsetTimeValues],len(subsetTimeValues),None,timeSet.getSetUnits(),None)
  subsetgrid = FieldImpl(FunctionType(RealType.Time, grid.getSample(0).getType()), newTimes)
  for i in range(len(subsetTimeValues)):
     subsetgrid.setSample(i,grid[indices[i]])
  return subsetgrid
def returnMatches(a,b):
  """ A helper function to to return all indices of list a that have matches in list b.
  """
  new_list = []
  for index,element in enumerate(a):
    if element in b:
        new_list.append(index)
  return new_list
def sampleAtTimesofGridB(gridA,gridB):
  """ A function to programatically sample gridA at times of gridB, similar to idv gui.
      one other application of this utility is when there are a lot of times where gui selection in
      IDV becomes tedious.
  """
  from visad import RealType
  from visad import Gridded1DDoubleSet
  from visad import FieldImpl
  from visad import FunctionType
  from visad import VisADException
  timeSetA=GridUtil.getTimeSet(gridA)
  timeSetB=GridUtil.getTimeSet(gridB)
  timesA=getSampleTimesInFormat(gridA,"yyyyMMddhhmm")
  timesB=getSampleTimesInFormat(gridB,"yyyyMMddhhmm")
  indicesA=returnMatches(timesA,timesB)
  timevalsA=timeSetA.getSamples()[0]
  subsetTimeValuesA=[timevalsA[i] for i in indicesA]
  newTimesA=Gridded1DDoubleSet(RealType.Time,[subsetTimeValuesA],len(subsetTimeValuesA),None,timeSetA.getSetUnits(),None)
  subsetgridA = FieldImpl(FunctionType(RealType.Time, gridA.getSample(0).getType()), newTimesA)
  for i in range(len(subsetTimeValuesA)):
     subsetgridA.setSample(i,gridA[indicesA[i]])
  return subsetgridA

def sampleAtTimesofGridB(gridA,gridB,matchformat="yyyyMMddhhmm"):
  """ A function to programatically sample gridA at times of gridB, similar to idv gui but the format
      decides what type type is picked.
      one other application of this utility is when there are a lot of times where gui selection in
      IDV becomes tedious.
  """
  from visad import RealType
  from visad import Gridded1DDoubleSet
  from visad import FieldImpl
  from visad import FunctionType
  from visad import VisADException
  timeSetA=GridUtil.getTimeSet(gridA)
  timeSetB=GridUtil.getTimeSet(gridB)
  timesA=getSampleTimesInFormat(gridA,str(matchformat))
  timesB=getSampleTimesInFormat(gridB,str(matchformat))
  indicesA=returnMatches(timesA,timesB)
  timevalsA=timeSetA.getSamples()[0]
  subsetTimeValuesA=[timevalsA[i] for i in indicesA]
  newTimesA=Gridded1DDoubleSet(RealType.Time,[subsetTimeValuesA],len(subsetTimeValuesA),None,timeSetA.getSetUnits(),None)
  subsetgridA = FieldImpl(FunctionType(RealType.Time, gridA.getSample(0).getType()), newTimesA)
  for i in range(len(subsetTimeValuesA)):
     subsetgridA.setSample(i,gridA[indicesA[i]])
  return subsetgridA


def cdo2(variable1,variable2,user_cdo_options):
  """ Does a cdo operation on 2 grids supplied and returns a grid.
      The function outsources the two grids to CDOs (code.zmaw.de/cdo) user option decides what
      operation can be done.
      evalues in cdo as cdo user_cdo_options variable1_written_to_temp_file.nc variable2_written.nc output_temp_file.nc
  """
  import java
  from java.util import Random
  import sys
  import commands
  from visad import VisADException
  rand = Random()

  file_prefix=rand.nextInt(9999)
  file_inp1=str(file_prefix)+"_inp1.nc"
  file_inp2=str(file_prefix)+"_inp2.nc"
  exportGridToNetcdf(variable1,file_inp1)
  exportGridToNetcdf(variable2,file_inp2)

  timeunit1=GridUtil.getTimeSet(variable1).getSetUnits()[0]
  timeunit2=GridUtil.getTimeSet(variable2).getSetUnits()[0]
  if (str(timeunit1).split()[0]=="s"):
    newtimeunit1=str(timeunit1).replace("s","seconds",1)
    nco_status=commands.getstatusoutput("ncatted -a units,time,m,c,"+"\""+newtimeunit1+"\""+" "+file_inp1)
    if (nco_status[0]):
        raise VisADException(nco_status[1])

  if (str(timeunit2).split()[0]=="s"):
    newtimeunit2=str(timeunit2).replace("s","seconds",1)
    nco_status=commands.getstatusoutput("ncatted -a units,time,m,c,"+"\""+newtimeunit2+"\""+" "+file_inp2)
    if (nco_status[0]):
        raise VisADException(nco_status[1])

  file_out=str(file_prefix)+"_out.nc"
  cdo_status=commands.getstatusoutput("cdo "+user_cdo_options+" "+file_inp1+" "+file_inp2+" "+file_out)
  if (cdo_status[0]):
    raise VisADException(cdo_status[1])

  dstemp = makeDataSource(file_out)
  varout=getData(dstemp.getName())
  return varout
#=============================Utils with additional Displays=====================================
def Moisture_Flux_Divergence(Q,U,V):
   """ Computes vertical integrated Moisture flux divergence and makes 1 display internally
       and returns a moisture flux divergence vector as output.
   """
   QU=verticalIntegral(Q.multiply(U))
   QV=verticalIntegral(Q.multiply(V))
   divergence=horizontalDivergence(Q, U, V)
   Idivergence=pverticalIntegral(divergence)
   createDisplay('planviewcolor',Idivergence,"Integrated Divergence")
   return makeTrueVector(QU, QV)

def Moisture_Flux_Divergence(Q,V):
   """ Computes vertical integrated Moisture flux divergence and makes 1 display internally
       and returns a moisture flux divergence vector as output.
       this function takes a derived field vector as input.
   """
   u=DerivedGridFactory.getUComponent(V)
   v=DerivedGridFactory.getVComponent(V)
   QU=verticalIntegral(Q.multiply(u))
   QV=verticalIntegral(Q.multiply(v))
   divergence=horizontalDivergence(Q, u, v)
   Idivergence=pverticalIntegral(divergence)
   createDisplay('planviewcolor',Idivergence,"Integrated Divergence")
   return makeTrueVector(QU, QV)

def zonal_filter_fft(grid,minwave,maxwave):
    """ Filters grid by zonal wavenumbers specified by minwave and maxwave.
        maxwave can be a large number to filter all smaller wavenumbers.
        for eg.., using minwave =1,10000, removes all variations zonally but mean
        using minwave=0 maxwave=0 removes zonal mean from the grid.
    """
    filtered_grid=grid.clone()
    def wavefilter(tempF,minwave,maxwave):
        if GridUtil.is3D(grid):
            raise VisADException('This implementation of filtering supports only 2d,eg.., lat-lon grids')

        tempFF=domainFactor(tempF,int(not(GridUtil.isLatLonOrder(tempF))))
        for j,lat in enumerate(tempFF.domainEnumeration()):
            fieldfft=fft(field(tempFF.evaluate(lat).getFloats()[0]))
            reals=fieldfft.getFloats()[0]
            imags=fieldfft.getFloats()[1]
            if maxwave==None or int(maxwave)>len(reals):
                maxwave=len(reals)-1

            for i in range(int(minwave),int(maxwave)+1):
                reals[i]=0.0
                imags[i]=0.0
            fieldfft.setSamples([reals,imags])
            fieldifft=ifft(fieldfft)
            tempFF[j].setSamples(GridUtil.getParam(fieldifft,0).getFloats())
        return tempFF.domainMultiply()

    if GridUtil.isTimeSequence(grid):
        for i in range(len(grid)):
            filtered_grid.setSample(i,wavefilter(grid.getSample(i),minwave,maxwave))
    elif grid.isFlatField():
        filtered_grid=wavefilter(grid,minwave,maxwave)
    else:
        raise VisADError('Not a valid 2d or 3d grid')
    return filtered_grid
def fillGridUniform(templategrid,user_min,user_max,user_units="default"):
    """ Returns a grid with values sampled from uniform distrubuition(
        user_min,user_max). user_units can be change units of returned grid.
        This also serves as a template code for creating grids sampled from
        different distributions.
    """

    minvalue=float(user_min)
    maxvalue=float(user_max)

    def fillUniform(gridFF,minvalue,maxvalue):
        import random
        tempFF=FlatField(gridFF.getType(),gridFF.getDomainSet())#put units here
        #also fun the return by input grid
        vals=[random.uniform(minvalue,maxvalue) for itm in range(len(gridFF.getFloats()[0]))]
        tempFF.setSamples([vals])
        return tempFF

    if GridUtil.isTimeSequence(templategrid):
        uniformG=templategrid.clone()
        for j,time in enumerate(templategrid.domainEnumeration()):
            uniformG.setSample(j,fillUniform(templategrid.getSample(j),minvalue,maxvalue))
        return uniformG
    else:
        return fillUniform(templategrid,minvalue,maxvalue)
def fillGridConstant(templategrid,user_value):
    """ Returns a grid with a constant value
        More of a helper function useful for debugging
    """
    value=float(user_value)
    newgrid=grid.clone()
    if GridUtil.isTimeSequence(grid):
        for i in range(len(grid)):
            newgrid.setSample(i,replace(grid.getSample(0),value))
    else:
        newgrid=replace(grid,Double(value))

    return newgrid
def fillGridNormal(templategrid,user_mean,user_std,user_units="default"):
    """ Returns a grid with values sampled from normal distrubuition(
        user_mean,user_std). user_units can be change units of returned grid.
        This also serves as a template code for creating grids sampled from
        different distributions.
    """

    minvalue=float(user_min)
    maxvalue=float(user_max)

    def fillNormal(gridFF,minvalue,maxvalue):
        import random
        tempFF=FlatField(gridFF.getType(),gridFF.getDomainSet())#put units here
        #also fun the return by input grid
        vals=[random.gauss(minvalue,maxvalue) for itm in range(len(gridFF.getFloats()[0]))]
        tempFF.setSamples([vals])
        return tempFF

    if GridUtil.isTimeSequence(templategrid):
        uniformG=templategrid.clone()
        for j,time in enumerate(templategrid.domainEnumeration()):
            uniformG.setSample(j,fillNormal(templategrid.getSample(j),minvalue,maxvalue))
        return uniformG
    else:
        return fillNormal(templategrid,minvalue,maxvalue)

def grid_modulo(grid,user_number):
    """ Returns modulo at every value of grid with number given"""
    from visad import FlatField
    number=int(user_number)
    def ff_modulo(grid_sample,number):
    	tempFF=FlatField(grid_sample.getType(),grid_sample.getDomainSet())
        vals=grid_sample.getValues()[0]
        tempFF.setSamples([map(lambda x:x%number,vals)])
        return tempFF
    if GridUtil.isTimeSequence(grid):
        tempFI=grid.clone()
    	for i in range(len(grid)):
             tempFI.setSample(i,ff_modulo(grid.getSample(i),number))
        return tempFI
    elif grid.isFlatField():
         return ff_modulo(grid,number)
    else:
        raise VisADError('Not a valid 2d or 3d grid')
